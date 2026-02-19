use crate::cli::sql_parser::{ParsedSqlQuery, SqlQueryType};
use crate::proto::pgraph::{
    model::Card,
    query::{CardCountResponse, CardQueryResponse},
};
use anyhow::Result;

/// 结果处理器，负责处理查询结果和字段裁剪
pub struct ResultProcessor;

impl ResultProcessor {
    /// 处理CardQueryResponse，根据SQL查询进行字段裁剪和格式化
    pub fn process_card_query_response(
        response: &CardQueryResponse,
        parsed_query: &ParsedSqlQuery,
    ) -> Result<()> {
        match &parsed_query.query_type {
            SqlQueryType::Select { fields, .. } => {
                if response.cards.is_empty() {
                    println!("没有找到匹配的卡片");
                    return Ok(());
                }

                // 如果是SELECT *，显示完整的卡片信息
                if fields.contains(&"*".to_string()) {
                    Self::display_full_cards(&response.cards);
                } else {
                    // 否则只显示指定的字段
                    Self::display_selected_fields(&response.cards, fields)?;
                }

                // 显示统计信息
                println!("\n找到 {} 张卡片，总共 {} 张", response.count, response.total);
            }
            SqlQueryType::Count => {
                // 这种情况不应该发生，因为COUNT查询应该使用CardCountResponse
                println!("查询结果数量: {}", response.total);
            }
        }

        Ok(())
    }

    /// 处理CardCountResponse
    pub fn process_card_count_response(response: &CardCountResponse) -> Result<()> {
        println!("COUNT(*): {}", response.count);
        Ok(())
    }

    /// 显示完整的卡片信息 - 直接序列化为JSON展示
    fn display_full_cards(cards: &[Card]) {
        println!("\n=== 查询结果 ===");
        for (index, card) in cards.iter().enumerate() {
            println!("\n{}. 卡片详情:", index + 1);
            
            // 将Card序列化为JSON格式展示
            match Self::card_to_json_pretty(card) {
                Ok(json_str) => println!("{}", json_str),
                Err(e) => {
                    println!("序列化卡片失败: {}", e);
                    // 如果序列化失败，显示基本信息
                    println!("   ID: {}", card.id);
                    println!("   组织ID: {}", card.org_id);
                    println!("   卡片类型ID: {}", card.type_id);
                }
            }
        }
    }

    /// 将Card转换为格式化的JSON字符串
    fn card_to_json_pretty(card: &Card) -> Result<String> {
        match serde_json::to_string_pretty(&card) {
            Ok(json_str) => Ok(json_str),
            Err(e) => Err(anyhow::anyhow!("JSON格式化失败: {}", e)),
        }
    }

    /// 显示指定字段的卡片信息
    fn display_selected_fields(cards: &[Card], fields: &[String]) -> Result<()> {
        println!("\n=== 查询结果 ===");
        
        // 显示表头
        let header: Vec<String> = fields.iter().map(|f| Self::get_field_display_name(f)).collect();
        println!("{}", header.join("\t"));
        println!("{}", "-".repeat(header.join("\t").len()));
        
        // 显示数据行
        for card in cards {
            let row_values: Result<Vec<String>> = fields.iter()
                .map(|field| Self::extract_field_value(card, field))
                .collect();
            
            match row_values {
                Ok(values) => println!("{}", values.join("\t")),
                Err(e) => println!("提取字段值失败: {}", e),
            }
        }

        Ok(())
    }

    /// 提取卡片中指定字段的值
    fn extract_field_value(card: &Card, field_name: &str) -> Result<String> {
        let value = match field_name {
            // 基础ID字段
            "id" | "card_id" => card.id.to_string(),
            "org_id" => card.org_id.clone(),
            "type_id" => card.type_id.clone(),

            // 状态字段
            "state" => Self::format_state(card.state),

            // 时间字段
            "created" => Self::format_timestamp(card.created_at),
            "updated" => Self::format_timestamp(card.updated_at),
            "archived_at" => {
                if card.archived_at > 0 {
                    Self::format_timestamp(card.archived_at)
                } else {
                    "".to_string()
                }
            }
            "discarded_at" => {
                if card.discarded_at > 0 {
                    Self::format_timestamp(card.discarded_at)
                } else {
                    "".to_string()
                }
            }

            // 编码字段
            "code" | "code_in_org" => card.code_in_org.to_string(),
            "custom_code" => card.custom_code.clone(),

            // 标题字段
            "title" => {
                if let Some(title) = &card.title {
                    Self::format_title(title)
                } else {
                    "".to_string()
                }
            }

            // 描述字段
            "desc" | "description" => card.description.clone(),

            // 流程相关字段
            "stream_id" => card.stream_id.clone(),
            "status_id" => card.status_id.clone(),
            _ => {
                return Err(anyhow::anyhow!("不支持的字段: {}", field_name))
            }
        };

        Ok(value)
    }

    /// 获取字段的显示名称
    fn get_field_display_name(field_name: &str) -> String {
        match field_name {
            // 基础ID字段
            "id" | "card_id" => "卡片ID".to_string(),
            "org_id" => "组织ID".to_string(),
            "type_id" => "卡片类型ID".to_string(),

            // 状态字段
            "state" => "状态".to_string(),

            // 时间字段
            "created" => "创建时间".to_string(),
            "updated" => "更新时间".to_string(),
            "archived_at" => "归档时间".to_string(),
            "discarded_at" => "丢弃时间".to_string(),

            // 编码字段
            "code" | "code_in_org" => "编码".to_string(),
            "custom_code" => "自定义编码".to_string(),

            // 标题字段
            "title" => "标题".to_string(),

            // 描述字段
            "desc" | "description" => "描述".to_string(),

            // 流程相关字段
            "stream_id" => "价值流ID".to_string(),
            "status_id" => "状态ID".to_string(),

            // 审计字段
            "created_by" => "创建人".to_string(),
            "updated_by" => "更新人".to_string(),

            // 自定义字段或未知字段
            _ => field_name.to_string(),
        }
    }

    /// 格式化标题
    fn format_title(title: &crate::proto::pgraph::model::Title) -> String {
        if let Some(title_type) = &title.title_type {
            match title_type {
                crate::proto::pgraph::model::title::TitleType::Pure(pure) => {
                    pure.value.clone()
                }
                crate::proto::pgraph::model::title::TitleType::Joint(joint) => {
                    joint.name.clone()
                }
            }
        } else {
            "[无标题]".to_string()
        }
    }

    /// 格式化状态
    /// Proto CardState: ACTIVE=0, ARCHIVED=1, DISCARDED=2
    fn format_state(state_value: i32) -> String {
        use crate::proto::pgraph::common::CardState;
        match CardState::try_from(state_value) {
            Ok(CardState::Active) => "活跃".to_string(),
            Ok(CardState::Archived) => "已归档".to_string(),
            Ok(CardState::Discarded) => "已丢弃".to_string(),
            Err(_) => format!("未知状态({})", state_value),
        }
    }

    /// 格式化时间戳
    fn format_timestamp(timestamp: i64) -> String {
        if timestamp <= 0 {
            return "".to_string();
        }

        // 将毫秒时间戳转换为秒
        let secs = if timestamp > 1_000_000_000_000 {
            timestamp / 1000
        } else {
            timestamp
        };

        match chrono::DateTime::from_timestamp(secs, 0) {
            Some(dt) => dt.format("%Y-%m-%d %H:%M:%S").to_string(),
            None => timestamp.to_string(),
        }
    }

}
