use crate::database::model::{
    CardState, EdgeDescriptor, EdgeDirection, Identifier, NeighborQuery, Vertex, VertexId,
    VertexQuery,
};
use crate::database::transaction::Transaction;
use crate::proto::pgraph::query::{self, condition_node::NodeType};
use crate::proto::{Condition, QueryScope};
use crate::server::filter::{FilterContext, ResolveCondition};
use std::collections::HashSet;
use std::sync::Arc;
use std::time::Instant;
use tracing::{debug, warn};

pub fn resolve_condition<'a, T: Transaction<'a>>(
    query_scope: &QueryScope,
    filter_context: &Option<FilterContext>,
    condition: &Option<Condition>,
    txn: &T,
) -> (
    VertexQuery,
    Option<ResolveCondition>,
    bool, /*return_empty_directly*/
) {
    let mut vertex_query = build_vertex_query(query_scope);

    if condition.is_none() {
        return (vertex_query, None, false);
    }

    let mut new_condition = condition.as_ref().unwrap().clone();

    // 检查 vertex_query 中是否已存在 card_ids，如果存在则跳过优化
    let should_optimize = match &vertex_query.card_ids {
        Some(card_ids) if !card_ids.is_empty() => false,
        _ => true,
    };

    if should_optimize {
        let vertex_id_scope =
            extract_vertex_id_scope_from_condition(filter_context, &mut new_condition, txn);
        if let Some(vertex_id_scope) = vertex_id_scope {
            if vertex_id_scope.is_empty() {
                return (vertex_query, None, true);
            } else {
                // 将 HashSet<VertexId> 转换为 Vec<CardId> (VertexId = CardId)
                vertex_query.card_ids = Some(vertex_id_scope.into_iter().collect());
            }
        }
    }

    (
        vertex_query,
        Some(ResolveCondition {
            condition: new_condition,
        }),
        false,
    )
}

/// 从条件中提取顶点ID范围以进行查询优化
fn extract_vertex_id_scope_from_condition<'a, T: Transaction<'a>>(
    filter_context: &Option<FilterContext>,
    condition: &mut Condition,
    txn: &T,
) -> Option<HashSet<VertexId>> {
    let Some(root) = &condition.root else {
        return None;
    };

    // 递归搜索可优化的 LinkConditionItem
    find_optimizable_link_in_node(filter_context, root, txn)
}

/// 在节点树中递归查找可优化的关联条件
fn find_optimizable_link_in_node<'a, T: Transaction<'a>>(
    filter_context: &Option<FilterContext>,
    node: &query::ConditionNode,
    txn: &T,
) -> Option<HashSet<VertexId>> {
    match &node.node_type {
        Some(NodeType::Group(group)) => {
            // 在组内递归查找
            for child in &group.children {
                if let Some(result) = find_optimizable_link_in_node(filter_context, child, txn) {
                    return Some(result);
                }
            }
            None
        }
        Some(NodeType::Link(link_item)) => {
            get_vertex_id_scope_via_link_condition(filter_context, link_item, txn)
        }
        _ => None,
    }
}

/// 通过关联条件获取顶点ID范围
fn get_vertex_id_scope_via_link_condition<'a, T: Transaction<'a>>(
    filter_context: &Option<FilterContext>,
    link_item: &query::LinkConditionItem,
    txn: &T,
) -> Option<HashSet<VertexId>> {
    let Some(link_operator) = &link_item.operator else {
        return None;
    };
    let Some(link_subject) = &link_item.subject else {
        return None;
    };
    let Some(link_operator_type) = &link_operator.operator_type else {
        return None;
    };

    let ref_vertex_ids = match link_operator_type {
        query::link_operator::OperatorType::Equal(equal_op) => {
            if let Some(value) = &equal_op.value {
                match value {
                    query::link_equal_operator::Value::SpecialLink(special_link) => {
                        if !special_link.card_ids.is_empty() {
                            Some(special_link.card_ids.clone())
                        } else {
                            None
                        }
                    }
                    query::link_equal_operator::Value::ReferLink(refer_link) => {
                        if let Some(refer_on_type) = &refer_link.refer_on_type {
                            get_ref_on_vertex_ids(filter_context, refer_on_type, txn, false)
                        } else {
                            None
                        }
                    }
                }
            } else {
                None
            }
        }
        query::link_operator::OperatorType::In(in_op) => {
            if let Some(value) = &in_op.value {
                match value {
                    query::link_in_operator::Value::SpecialLink(special_link) => {
                        if !special_link.card_ids.is_empty() {
                            Some(special_link.card_ids.clone())
                        } else {
                            None
                        }
                    }
                    query::link_in_operator::Value::ReferLink(refer_link) => {
                        if let Some(refer_on_type) = &refer_link.refer_on_type {
                            get_ref_on_vertex_ids(filter_context, refer_on_type, txn, false)
                        } else {
                            None
                        }
                    }
                }
            } else {
                None
            }
        }
        _ => None,
    };

    if let Some(ref_vertex_ids) = &ref_vertex_ids {
        if let Some(subject_path) = &link_subject.path {
            if !subject_path.nodes.is_empty() {
                let reversed_path = reverse_path(subject_path);
                let mut subject_vertex_ids = HashSet::new();

                for start_vertex_id in ref_vertex_ids {
                    let source_ids =
                        get_related_vertex_ids(start_vertex_id, &reversed_path, txn, true);
                    if !source_ids.is_empty() {
                        subject_vertex_ids.extend(source_ids);
                    }
                }

                if !subject_vertex_ids.is_empty() {
                    return Some(subject_vertex_ids);
                }
                return None;
            }
        }

        let mut result = HashSet::with_capacity(ref_vertex_ids.len());
        result.extend(ref_vertex_ids);
        return Some(result);
    }

    None
}

/// 获取引用卡片ID
fn get_ref_on_vertex_ids<'a, T: Transaction<'a>>(
    filter_context: &Option<FilterContext>,
    refer_on_type: &query::refer_link::ReferOnType,
    txn: &T,
    last_level_contains_discard: bool,
) -> Option<Vec<VertexId>> {
    match refer_on_type {
        query::refer_link::ReferOnType::ReferOnCurrentCard(_) => None,
        query::refer_link::ReferOnType::ReferOnParameterCard(ref_on_parameter_card) => {
            if let Some(ctx) = filter_context {
                if let Some(parameter_vertex_id) = ctx
                    .parameters
                    .get(&ref_on_parameter_card.parameter_card_type_id)
                {
                    if let Some(path) = ref_on_parameter_card.path.as_ref() {
                        if !path.nodes.is_empty() {
                            let ref_vertex_ids = get_related_vertex_ids(
                                parameter_vertex_id,
                                path,
                                txn,
                                last_level_contains_discard,
                            );
                            Some(ref_vertex_ids)
                        } else {
                            Some(vec![*parameter_vertex_id])
                        }
                    } else {
                        Some(vec![*parameter_vertex_id])
                    }
                } else {
                    None
                }
            } else {
                None
            }
        }
        query::refer_link::ReferOnType::ReferOnMember(ref_on_member) => {
            if let Some(ctx) = filter_context {
                if let Some(member_id) = ctx.member_id {
                    if let Some(path) = ref_on_member.path.as_ref() {
                        if !path.nodes.is_empty() {
                            let ref_vertex_ids = get_related_vertex_ids(
                                &member_id,
                                path,
                                txn,
                                last_level_contains_discard,
                            );
                            Some(ref_vertex_ids)
                        } else {
                            Some(vec![member_id])
                        }
                    } else {
                        Some(vec![member_id])
                    }
                } else {
                    None
                }
            } else {
                None
            }
        }
        query::refer_link::ReferOnType::ReferOnContextualCard(refer_on_contextual_card) => {
            if filter_context.is_some() {
                if let Some(path) = refer_on_contextual_card.path.as_ref() {
                    if !path.nodes.is_empty() {
                        let ref_vertex_ids = get_related_vertex_ids(
                            &refer_on_contextual_card.contextual_card_id,
                            path,
                            txn,
                            last_level_contains_discard,
                        );
                        Some(ref_vertex_ids)
                    } else {
                        Some(vec![refer_on_contextual_card.contextual_card_id])
                    }
                } else {
                    Some(vec![refer_on_contextual_card.contextual_card_id])
                }
            } else {
                None
            }
        }
    }
}

/// last_level_contains_discard: 只有最后一级包含丢弃节点
pub fn get_related_vertex_ids<'a, T: Transaction<'a>>(
    vertex: &VertexId,
    path: &crate::proto::pgraph::common::Path,
    txn: &T,
    last_level_contains_discard: bool,
) -> Vec<VertexId> {
    if path.nodes.is_empty() {
        debug!("Path is empty, returning empty list");
        return Vec::new();
    }

    let mut current_vertex_ids = vec![*vertex];

    for (index, node_def) in path.nodes.iter().enumerate() {
        let mut next_vertex_ids = Vec::new();

        for current_vertex_id in &current_vertex_ids {
            let edge_type = Identifier::new(&node_def.lt_id);
            let direction = match node_def.position.as_str() {
                "Src" => EdgeDirection::Src,
                "Dest" => EdgeDirection::Dest,
                _ => {
                    debug!("Unrecognized direction: {}, skipping", node_def.position);
                    continue;
                }
            };

            let edge_descriptor = EdgeDescriptor::new(edge_type, direction);
            let query = NeighborQuery {
                src_vertex_ids: vec![*current_vertex_id],
                edge_descriptor,
                dest_vertex_states: if index == path.nodes.len() - 1 && last_level_contains_discard
                {
                    Some(vec![
                        CardState::Active,
                        CardState::Archived,
                        CardState::Discarded,
                    ])
                } else {
                    None
                },
            };

            match txn.query_neighbor_vertex_ids(&query) {
                Ok(neighbor_vertex_ids) => {
                    if !neighbor_vertex_ids.is_empty() {
                        next_vertex_ids.extend(neighbor_vertex_ids);
                    }
                }
                Err(err) => {
                    warn!("Failed to query neighbor nodes: {:?}", err);
                    return Vec::new();
                }
            }
        }

        if next_vertex_ids.is_empty() {
            return Vec::new();
        }

        current_vertex_ids = next_vertex_ids;
    }
    current_vertex_ids
}

/// 根据路径获取关联节点
pub fn get_related_vertices<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    path: &crate::proto::pgraph::common::Path,
    txn: &T,
) -> Vec<Arc<Vertex>> {
    if path.nodes.is_empty() {
        debug!("path is empty, return empty list");
        return Vec::new();
    }
    let start_time = Instant::now();

    let mut current_vertex_ids = vec![vertex.card_id];

    for (_, node_def) in path.nodes.iter().enumerate() {
        let mut next_vertex_ids = Vec::new();

        for current_vertex_id in &current_vertex_ids {
            let edge_type = Identifier::new(&node_def.lt_id);
            let direction = match node_def.position.as_str() {
                "Src" => EdgeDirection::Src,
                "Dest" => EdgeDirection::Dest,
                _ => {
                    debug!("Unrecognized direction: {}, skip", node_def.position);
                    continue;
                }
            };

            let edge_descriptor = EdgeDescriptor::new(edge_type, direction);
            let query = NeighborQuery {
                src_vertex_ids: vec![*current_vertex_id],
                edge_descriptor,
                dest_vertex_states: None,
            };

            match txn.query_neighbor_vertex_ids(&query) {
                Ok(neighbor_vertex_ids) => {
                    if !neighbor_vertex_ids.is_empty() {
                        next_vertex_ids.extend(neighbor_vertex_ids);
                    }
                }
                Err(err) => {
                    warn!("Failed to query neighbor nodes: {:?}", err);
                    return Vec::new();
                }
            }
        }

        if next_vertex_ids.is_empty() {
            return Vec::new();
        }

        current_vertex_ids = next_vertex_ids;
    }

    let result = txn
        .get_specific_vertices(&current_vertex_ids)
        .unwrap_or_else(|_| Vec::new());

    let end_time = Instant::now();
    let duration = end_time.duration_since(start_time);
    debug!("Get associated nodes duration: {:?}", duration);

    result
}

/// 构建基础查询条件
fn build_vertex_query(query_scope: &QueryScope) -> VertexQuery {
    let mut base_query = VertexQuery {
        card_ids: None,
        vertex_ids: None,
        card_type_ids: Vec::new(),
        container_ids: None,
        states: None,
    };

    if !query_scope.card_type_ids.is_empty() {
        base_query.card_type_ids = query_scope
            .card_type_ids
            .iter()
            .map(|id| Identifier::new(id))
            .collect();
    }

    if !query_scope.card_ids.is_empty() {
        base_query.card_ids = Some(query_scope.card_ids.clone());
    }

    if !query_scope.container_ids.is_empty() {
        base_query.container_ids = Some(
            query_scope
                .container_ids
                .iter()
                .map(|id| Identifier::new(id))
                .collect(),
        );
    }

    if !query_scope.states.is_empty() {
        let states: Vec<CardState> = query_scope
            .states
            .iter()
            .map(|&state_num| match state_num {
                0 => CardState::Active,
                1 => CardState::Discarded,
                2 => CardState::Archived,
                _ => {
                    warn!(
                        "Unknown state code: {}, using default value Active",
                        state_num
                    );
                    CardState::Active
                }
            })
            .collect();
        base_query.states = Some(states);
    }
    base_query
}

/// 反转path，将目标到源的路径转换为源到目标的路径
fn reverse_path(path: &crate::proto::pgraph::common::Path) -> crate::proto::pgraph::common::Path {
    let mut reversed_path = crate::proto::pgraph::common::Path {
        nodes: Vec::with_capacity(path.nodes.len()),
    };

    for node in path.nodes.iter().rev() {
        let mut reversed_node = node.clone();
        if node.position == "Src" {
            reversed_node.position = "Dest".to_string();
        } else if node.position == "Dest" {
            reversed_node.position = "Src".to_string();
        }
        reversed_path.nodes.push(reversed_node);
    }

    reversed_path
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_reverse_path() {
        let original_path = crate::proto::pgraph::common::Path {
            nodes: vec![
                crate::proto::pgraph::common::PathNode {
                    lt_id: "link1".to_string(),
                    position: "Src".to_string(),
                },
                crate::proto::pgraph::common::PathNode {
                    lt_id: "link2".to_string(),
                    position: "Dest".to_string(),
                },
            ],
        };

        let reversed = reverse_path(&original_path);

        assert_eq!(reversed.nodes.len(), 2);
        assert_eq!(reversed.nodes[0].lt_id, "link2");
        assert_eq!(reversed.nodes[0].position, "Src");
        assert_eq!(reversed.nodes[1].lt_id, "link1");
        assert_eq!(reversed.nodes[1].position, "Dest");
    }
}
