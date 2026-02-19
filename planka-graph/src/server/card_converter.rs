// card_converter.rs
// 处理卡片转换相关的功能

use crate::database::model::{
    CardState, EdgeDescriptor, EdgeDirection, EdgeType, Identifier, NeighborQuery, Vertex,
    VertexId, VertexTitle,
};
use crate::database::transaction::Transaction;
use crate::proto::pgraph::model::{Card, CardList};
use crate::proto::YieldedLink;
use std::collections::HashMap;
use std::sync::Arc;

/// 按需填充卡片的自定义属性、衍生属性、关联卡片、卡片描述
pub fn convert_to_cards<'a, T: Transaction<'a>>(
    vertices: &[Arc<Vertex>],
    txn: &T,
    r#yield: Option<&crate::proto::pgraph::query::Yield>,
) -> Vec<Card> {
    // 检查是否需要包含卡片描述
    let include_description = txn.should_include_description(r#yield);

    // 转换节点为卡片
    let mut cards = vertices
        .iter()
        .map(|v| vertex_to_card_with_yield(v, r#yield))
        .collect::<Vec<Card>>();

    // 根据Return结构中的描述决定填充卡片描述
    if include_description && !vertices.is_empty() {
        let vertex_ids: Vec<VertexId> = vertices.iter().map(|v| v.card_id).collect();
        let descriptions = txn.get_card_descriptions(&vertex_ids);

        // 根据顺序填充描述信息
        for (i, vertex) in vertices.iter().enumerate() {
            if let Some(desc_opt) = descriptions.get(&vertex.card_id) {
                if let Some(desc) = desc_opt {
                    // 更新卡片描述
                    cards[i].description = desc.to_string();
                }
            }
        }
    }

    // 处理关联卡片
    if let Some(yield_conf) = r#yield {
        if !yield_conf.yielded_links.is_empty() {
            fill_linked_cards(&mut cards, vertices, txn, &yield_conf.yielded_links);
        }
    }

    cards
}

/// 填充关联卡片及其属性
fn fill_linked_cards<'a, T: Transaction<'a>>(
    source_cards: &mut [Card],
    source_vertices: &[Arc<Vertex>],
    txn: &T,
    yielded_links: &Vec<YieldedLink>,
) {
    for (card_idx, vertex) in source_vertices.iter().enumerate() {
        // 处理每个需要返回的关联
        for yielded_link in yielded_links {
            do_fill_linked_cards(vertex, yielded_link, txn, &mut source_cards[card_idx]);
        }
    }
}

/// 处理路径节点，获取关联卡片
fn do_fill_linked_cards<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    yielded_link: &YieldedLink,
    txn: &T,
    source_card: &mut Card,
) {
    let Some(path_node) = &yielded_link.path_node else {
        return;
    };

    // 创建边类型
    let edge_type = EdgeType::new(&path_node.lt_id);

    // 确定边的方向
    let direction = if path_node.position == "Src" {
        EdgeDirection::Src
    } else {
        EdgeDirection::Dest
    };

    // 创建边描述符
    let descriptor = EdgeDescriptor::new(edge_type, direction);

    let dest_vertex_states = if yielded_link.contains_discard {
        Some(vec![
            CardState::Active,
            CardState::Archived,
            CardState::Discarded,
        ])
    } else {
        Some(vec![CardState::Active, CardState::Archived])
    };

    // 构建邻居查询参数，排除丢弃状态的节点
    let query = NeighborQuery {
        src_vertex_ids: vec![vertex.card_id],
        edge_descriptor: descriptor,
        dest_vertex_states,
    };

    // 查询邻居节点
    if let Ok(neighbor_vertices) = txn.query_neighbor_vertices(&query) {
        if neighbor_vertices.is_empty() {
            return;
        }

        // 创建关联卡片集合
        let mut card_list = CardList { cards: Vec::new() };

        let yielded_field = &yielded_link.yielded_field;

        // 处理每个邻居节点
        for neighbor in &neighbor_vertices {
            // 根据当前层级的YieldedField配置创建返回配置
            let r#yield = if let Some(yf) = yielded_field {
                crate::proto::pgraph::query::Yield {
                    yielded_field: Some(yf.clone()),
                    yielded_links: Vec::new(),
                }
            } else {
                crate::proto::pgraph::query::Yield {
                    yielded_field: None,
                    yielded_links: Vec::new(),
                }
            };

            // 处理节点转换为卡片，对每个节点都应用相应的返回配置
            let mut linked_card = vertex_to_card_with_yield(neighbor, Some(&r#yield));

            // 如果当前层级需要返回描述，则填充描述字段
            if yielded_field.is_some() && yielded_field.as_ref().unwrap().contains_desc {
                if let Some(desc) = txn.get_card_description(&neighbor.card_id) {
                    linked_card.description = desc.to_string();
                }
            }

            // 注意: derive_field_value_map 已从 proto Card 中移除
            // 边属性（fields_on_link）暂不处理

            // 添加到卡片列表
            card_list.cards.push(linked_card);
        }

        // 递归处理下一级节点 - 在添加到card_list之后进行
        for (card_idx, neighbor) in neighbor_vertices.iter().enumerate() {
            for next in yielded_link.next_yielded_link.iter() {
                do_fill_linked_cards(neighbor, next, txn, &mut card_list.cards[card_idx]);
            }
        }

        // 将关联卡片添加到映射中
        if !card_list.cards.is_empty() {
            let mut lt_id = path_node.lt_id.to_string();
            lt_id.push_str(":");
            lt_id.push_str(&path_node.position);
            source_card.link_card_map.insert(lt_id, card_list);
        }
    }
}

/// 将Vertex的字段值转换为Proto的字段值
fn vertex_field_value_to_proto_field_value(
    field_id: &Identifier,
    field_value: &crate::database::model::FieldValue,
) -> Option<crate::proto::pgraph::field::FieldValue> {
    use crate::proto::pgraph::field::field_value::FieldType;
    use crate::proto::pgraph::field::*;

    let field_type = match field_value {
        crate::database::model::FieldValue::Text(text_value) => {
            // 文本字段
            let text_field = TextFieldValue {
                value: text_value.text.clone(),
                max_string_length: 0, // 可以根据需要设置
            };
            Some(FieldType::TextField(text_field))
        }
        crate::database::model::FieldValue::Number(number_value) => {
            // 数值字段
            let number_field = NumberFieldValue {
                value: number_value.number,
            };
            Some(FieldType::NumberField(number_field))
        }
        crate::database::model::FieldValue::Date(date_value) => {
            // 日期字段
            let date_field = DateFieldValue {
                value: date_value.timestamp as i64,
            };
            Some(FieldType::DateField(date_field))
        }
        crate::database::model::FieldValue::Enum(enum_value) => {
            // 枚举字段：直接存储选项 ID 列表
            let option_ids: Vec<String> = enum_value
                .items
                .iter()
                .map(|item_id| item_id.to_string())
                .collect();

            let enum_field = EnumFieldValue { value: option_ids };
            Some(FieldType::EnumField(enum_field))
        }
        crate::database::model::FieldValue::WebLink(web_link_value) => {
            // 网址链接字段
            let web_link_field = WebLinkFieldValue {
                href: web_link_value.href.clone(),
                name: web_link_value.name.clone(),
            };
            Some(FieldType::WebLinkField(web_link_field))
        }
        crate::database::model::FieldValue::Attachment(attachment_value) => {
            // 附件字段
            let attachment_items: Vec<AttachmentItem> = attachment_value
                .items
                .iter()
                .map(|item| AttachmentItem {
                    id: item.id.clone(),
                    name: item.name.clone(),
                    uploader: item.uploader.clone(),
                    created_at: item.created_at as i64,
                    size: item.size as i64,
                })
                .collect();

            let attachment_field = AttachmentFieldValue {
                value: attachment_items,
            };
            Some(FieldType::AttachmentField(attachment_field))
        }
    };

    // 构建并返回FieldValue对象
    if let Some(field_type) = field_type {
        Some(FieldValue {
            field_id: field_id.to_string(),
            field_type: Some(field_type),
        })
    } else {
        None
    }
}

/// 将Vertex的title转换为proto Title
pub fn vertex_title_to_proto_title(vertex_title: &VertexTitle) -> crate::proto::pgraph::model::Title {
    use crate::proto::pgraph::common::TitleJointArea;
    use crate::proto::pgraph::model::{
        title, JointTitle, JointTitlePart, JointTitleParts, PureTitle, Title,
    };

    match vertex_title {
        VertexTitle::PureTitle(content) => {
            let pure_title = PureTitle {
                value: content.clone(),
            };
            let title_type = title::TitleType::Pure(pure_title);
            Title {
                title_type: Some(title_type),
            }
        }
        VertexTitle::JointTitle(joint_info) => {
            // 转换拼接标题区域类型
            let proto_area = match joint_info.area {
                crate::database::model::TitleJointArea::Prefix => TitleJointArea::Prefix,
                crate::database::model::TitleJointArea::Suffix => TitleJointArea::Suffix,
            };

            // 转换多个拼接标题部分
            let multi_parts = joint_info
                .multi_parts
                .iter()
                .map(|parts| {
                    // 转换每个部分
                    let proto_parts = parts
                        .parts
                        .iter()
                        .map(|part| JointTitlePart {
                            name: part.name.clone(),
                        })
                        .collect();

                    JointTitleParts { parts: proto_parts }
                })
                .collect();

            // 创建拼接标题
            let joint_title = JointTitle {
                name: joint_info.name.clone(),
                area: proto_area as i32,
                multi_parts,
            };

            let title_type = title::TitleType::Joint(joint_title);
            Title {
                title_type: Some(title_type),
            }
        }
    }
}

/// 将Vertex转换为Card，根据Yield结构决定填充哪些自定义属性
pub fn vertex_to_card_with_yield(
    vertex: &Arc<Vertex>,
    r#yield: Option<&crate::proto::pgraph::query::Yield>,
) -> Card {
    // 创建Title
    let title = Some(vertex_title_to_proto_title(&vertex.title));

    // 创建自定义字段值映射
    let mut custom_field_value_map = HashMap::new();

    // 只有在存在Yield和字段值时才处理字段值
    if let (Some(yield_conf), Some(field_values)) = (r#yield, &vertex.field_values) {
        if let Some(yielded_field) = &yield_conf.yielded_field {
            // 如果all字段为true，返回所有字段值
            if yielded_field.contains_all_custom_field {
                for (field_id, field_value) in field_values {
                    if let Some(proto_field_value) =
                        vertex_field_value_to_proto_field_value(field_id, field_value)
                    {
                        custom_field_value_map.insert(field_id.to_string(), proto_field_value);
                    }
                }
            } else {
                // 否则只返回指定的字段
                // 获取需要返回的自定义字段ID列表
                for field_id_str in &yielded_field.custom_fields {
                    let id_obj = Identifier::new(field_id_str);
                    if let Some(field_value) = field_values.get(&id_obj) {
                        if let Some(proto_field_value) =
                            vertex_field_value_to_proto_field_value(&id_obj, field_value)
                        {
                            custom_field_value_map.insert(field_id_str.clone(), proto_field_value);
                        }
                    }
                }
            }
            // 卡片描述不再这里填充
        }
    }

    // 将数据库状态映射到Proto状态
    // 数据库: Active, Archived, Discarded
    // Proto CardState: Active=0, Archived=1, Discarded=2
    let proto_state = match vertex.state {
        CardState::Active => crate::proto::pgraph::common::CardState::Active as i32,
        CardState::Archived => crate::proto::pgraph::common::CardState::Archived as i32,
        CardState::Discarded => crate::proto::pgraph::common::CardState::Discarded as i32,
    };

    Card {
        id: vertex.card_id,
        title,
        org_id: vertex.org_id.to_string(),
        code_in_org: vertex
            .code_in_org
            .parse::<i64>()
            .expect("code_in_org is not a number"),
        custom_code: vertex.custom_code.clone().unwrap_or_default(),
        type_id: vertex.card_type_id.to_string(),
        state: proto_state,
        created_at: vertex.created_at as i64,
        updated_at: vertex.updated_at as i64,
        discarded_at: vertex.discarded_at.unwrap_or(0) as i64,
        archived_at: vertex.archived_at.unwrap_or(0) as i64,
        description: String::new(), // 初始化为空字符串，稍后在convert_to_cards函数中根据Return配置填充
        stream_id: vertex.stream_info.stream_id.to_string(),
        status_id: vertex.stream_info.status_id.to_string(),
        custom_field_value_map,
        link_card_map: HashMap::new(),
    }
}
