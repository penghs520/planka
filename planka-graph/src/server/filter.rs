
use crate::database::model::{
    CardState, FieldValue, Identifier,
    TitleJointArea, Vertex, VertexId, VertexTitle,
};
use crate::database::transaction::Transaction;
use crate::proto::pgraph::common::Path;
use crate::proto::pgraph::query::{self, condition_node::NodeType, LogicOperator};
use crate::proto::Condition;
use crate::server::helper::{get_related_vertex_ids, get_related_vertices};
use crate::utils::pinyin_utils;
use std::collections::HashMap;
use std::sync::Arc;
use std::time::Instant;
use tracing::{debug, warn};

/// Path的扩展方法，提供缓存键生成功能
impl Path {
    /// 将Path转换为缓存键格式的字符串
    fn to_key(&self) -> String {
        if self.nodes.is_empty() {
            return String::new();
        }

        let mut key = String::new();
        for (i, node) in self.nodes.iter().enumerate() {
            if i > 0 {
                key.push_str("->");
            }
            key.push_str(&node.lt_id);
            key.push(':');
            key.push_str(&node.position);
        }
        key
    }
}

pub struct FilterContext {
    pub(crate) member_id: Option<VertexId>,
    pub(crate) parameters: HashMap<String, VertexId>,
}

pub struct ResolveCondition {
    pub(crate) condition: Condition,
}

impl ResolveCondition {
    /// 判断condition中是否存在条件
    pub fn has_clauses(&self) -> bool {
        self.condition.root.is_some()
    }
}

/// 用于临时缓存一次查询过程中需要用到的中间计算结果
struct EvaluateContext {
    refer_enum_cache: HashMap<String, Vec<Vec<String>>>,
}

/// 应用过滤条件（递归嵌套结构）
pub fn apply_filter_conditions<'a, T: Transaction<'a>>(
    vertices: &[Arc<Vertex>],
    filter_context: &Option<FilterContext>,
    condition: &Option<ResolveCondition>,
    txn: &T,
) -> Vec<Arc<Vertex>> {
    // 如果没有条件，直接返回所有节点
    if condition.is_none() || !condition.as_ref().unwrap().has_clauses() {
        return vertices.to_vec();
    }

    let condition = &condition.as_ref().unwrap().condition;
    let mut evaluate_context = EvaluateContext {
        refer_enum_cache: HashMap::with_capacity(64),
    };

    // 只有在debug级别启用时才计算时间
    let start_time = if tracing::enabled!(tracing::Level::DEBUG) {
        Some(Instant::now())
    } else {
        None
    };

    // 使用迭代器过滤并收集结果
    let res = vertices
        .iter()
        .filter(|vertex| {
            if let Some(root) = &condition.root {
                evaluate_node(vertex, root, txn, filter_context, &mut evaluate_context)
            } else {
                true
            }
        })
        .cloned()
        .collect();

    if let Some(start_time) = start_time {
        debug!(
            "filter vertices, cost: {:?}",
            Instant::now().duration_since(start_time)
        );
    }
    res
}

/// 递归评估条件节点
fn evaluate_node<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    node: &query::ConditionNode,
    txn: &T,
    context: &Option<FilterContext>,
    evaluate_context: &mut EvaluateContext,
) -> bool {
    match &node.node_type {
        Some(NodeType::Group(group)) => {
            evaluate_group(vertex, group, txn, context, evaluate_context)
        }
        Some(NodeType::Title(item)) => evaluate_title_condition(vertex, item, txn),
        Some(NodeType::Code(item)) => evaluate_code_condition(vertex, item),
        Some(NodeType::Text(item)) => evaluate_text_condition(vertex, item, txn),
        Some(NodeType::Number(item)) => evaluate_number_condition(vertex, item, txn),
        Some(NodeType::Date(item)) => evaluate_date_condition(vertex, item, txn),
        Some(NodeType::EnumItem(item)) => {
            evaluate_enum_condition(vertex, item, txn, context, evaluate_context)
        }
        Some(NodeType::Status(item)) => evaluate_status_condition(vertex, item, txn),
        Some(NodeType::State(item)) => evaluate_state_condition(vertex, item, txn),
        Some(NodeType::Link(item)) => evaluate_link_condition(vertex, item, txn, context),
        Some(NodeType::WebLink(item)) => evaluate_web_link_condition(vertex, item, txn),
        Some(NodeType::Keyword(item)) => evaluate_keyword_condition(vertex, item),
        Some(NodeType::CardType(item)) => evaluate_card_type_condition(vertex, item, txn),
        None => {
            warn!("Condition node type is empty");
            true
        }
    }
}

/// 评估条件组（递归）
fn evaluate_group<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    group: &query::ConditionGroup,
    txn: &T,
    context: &Option<FilterContext>,
    evaluate_context: &mut EvaluateContext,
) -> bool {
    match group.operator() {
        LogicOperator::And => group
            .children
            .iter()
            .all(|child| evaluate_node(vertex, child, txn, context, evaluate_context)),
        LogicOperator::Or => group
            .children
            .iter()
            .any(|child| evaluate_node(vertex, child, txn, context, evaluate_context)),
    }
}

// ============================================================================
// 标题条件评估
// ============================================================================

fn evaluate_title_condition<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    item: &query::TitleConditionItem,
    _txn: &T,
) -> bool {
    if let Some(operator) = &item.operator {
        if let Some(op_type) = &operator.operator_type {
            match op_type {
                query::title_operator::OperatorType::Contains(contains_op) => {
                    return vertex.title.name().contains(&contains_op.value);
                }
                query::title_operator::OperatorType::Equal(equal_op) => {
                    return vertex.title.name() == &equal_op.value;
                }
                query::title_operator::OperatorType::In(in_op) => {
                    return in_op.values.contains(vertex.title.name());
                }
            }
        }
    }
    warn!("Title condition has no matching operator");
    false
}

// ============================================================================
// 编号条件评估
// ============================================================================

fn evaluate_code_condition(vertex: &Arc<Vertex>, item: &query::CodeConditionItem) -> bool {
    vertex.code_in_org.to_string().contains(&item.value)
        || vertex
            .custom_code
            .as_ref()
            .map_or(false, |c| c.contains(&item.value))
}

// ============================================================================
// 卡片生命周期状态条件评估
// ============================================================================

fn evaluate_state_condition<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    item: &query::StateConditionItem,
    txn: &T,
) -> bool {
    if let Some(subject) = &item.subject {
        if let Some(path) = &subject.path {
            if !path.nodes.is_empty() {
                let related_vertices = get_related_vertices(vertex, path, txn);

                if related_vertices.is_empty() {
                    if let Some(operator) = &item.operator {
                        if let Some(op_type) = &operator.operator_type {
                            return match op_type {
                                query::state_operator::OperatorType::Equal(_) => false,
                                query::state_operator::OperatorType::NotEqual(_) => true,
                                query::state_operator::OperatorType::In(_) => false,
                                query::state_operator::OperatorType::NotIn(_) => true,
                            };
                        }
                    }
                    return false;
                }

                for related_vertex in &related_vertices {
                    if evaluate_state_on_vertex(related_vertex, item.operator.as_ref()) {
                        return true;
                    }
                }
                return false;
            }
        }
        evaluate_state_on_vertex(vertex, item.operator.as_ref())
    } else {
        evaluate_state_on_vertex(vertex, item.operator.as_ref())
    }
}

fn evaluate_state_on_vertex(
    vertex: &Arc<Vertex>,
    operator_opt: Option<&query::StateOperator>,
) -> bool {
    if let Some(operator) = operator_opt {
        if let Some(op_type) = &operator.operator_type {
            let state_str = match vertex.state {
                CardState::Active => "ACTIVE",
                CardState::Archived => "ARCHIVED",
                CardState::Discarded => "DISCARDED",
            };

            match op_type {
                query::state_operator::OperatorType::Equal(equal_op) => {
                    return state_str == equal_op.value;
                }
                query::state_operator::OperatorType::NotEqual(not_equal_op) => {
                    return state_str != not_equal_op.value;
                }
                query::state_operator::OperatorType::In(in_op) => {
                    return in_op.values.contains(&state_str.to_string());
                }
                query::state_operator::OperatorType::NotIn(not_in_op) => {
                    return !not_in_op.values.contains(&state_str.to_string());
                }
            }
        }
    }
    warn!("State condition has no matching operator");
    false
}

// ============================================================================
// 卡片类型条件评估
// ============================================================================

fn evaluate_card_type_condition<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    item: &query::CardTypeConditionItem,
    txn: &T,
) -> bool {
    if let Some(subject) = &item.subject {
        if let Some(path) = &subject.path {
            if !path.nodes.is_empty() {
                let related_vertices = get_related_vertices(vertex, path, txn);

                if related_vertices.is_empty() {
                    if let Some(operator) = &item.operator {
                        if let Some(op_type) = &operator.operator_type {
                            return match op_type {
                                query::card_type_operator::OperatorType::Equal(_) => false,
                                query::card_type_operator::OperatorType::In(_) => false,
                            };
                        }
                    }
                    return false;
                }

                for related_vertex in &related_vertices {
                    if evaluate_card_type_on_vertex(related_vertex, item.operator.as_ref()) {
                        return true;
                    }
                }
                return false;
            }
        }
        evaluate_card_type_on_vertex(vertex, item.operator.as_ref())
    } else {
        evaluate_card_type_on_vertex(vertex, item.operator.as_ref())
    }
}

fn evaluate_card_type_on_vertex(vertex: &Arc<Vertex>, operator_opt: Option<&query::CardTypeOperator>) -> bool {
    if let Some(operator) = operator_opt {
        if let Some(op_type) = &operator.operator_type {
            match op_type {
                query::card_type_operator::OperatorType::Equal(equal_op) => {
                    vertex.card_type_id.to_string() == equal_op.value
                }
                query::card_type_operator::OperatorType::In(in_op) => {
                    in_op.values.contains(&vertex.card_type_id.to_string())
                }
            }
        } else {
            warn!("VUT condition operator type is empty");
            false
        }
    } else {
        warn!("VUT condition operator is empty");
        false
    }
}

// ============================================================================
// 文本条件评估
// ============================================================================

fn evaluate_text_condition<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    item: &query::TextConditionItem,
    txn: &T,
) -> bool {
    if let Some(subject) = &item.subject {
        if let Some(path) = &subject.path {
            if !path.nodes.is_empty() {
                let related_vertices = get_related_vertices(vertex, path, txn);

                if related_vertices.is_empty() {
                    if let Some(operator) = &item.operator {
                        if let Some(op_type) = &operator.operator_type {
                            return match op_type {
                                query::text_operator::OperatorType::IsBlank(_) => true,
                                query::text_operator::OperatorType::NotEqual(_) => true,
                                query::text_operator::OperatorType::NotContains(_) => true,
                                query::text_operator::OperatorType::IsNotBlank(_) => false,
                                _ => false,
                            };
                        }
                    }
                    return false;
                }

                for related_vertex in &related_vertices {
                    if evaluate_text_field_on_vertex(
                        related_vertex,
                        subject.field_id.as_str(),
                        item.operator.as_ref(),
                    ) {
                        return true;
                    }
                }
                return false;
            }
        }
        return evaluate_text_field_on_vertex(
            vertex,
            subject.field_id.as_str(),
            item.operator.as_ref(),
        );
    } else {
        warn!("Text condition subject is empty");
        return false;
    }
}

fn evaluate_text_field_on_vertex(
    vertex: &Arc<Vertex>,
    field_id: &str,
    operator_opt: Option<&query::TextOperator>,
) -> bool {
    if operator_opt.is_none() {
        warn!("Text condition operator is empty, field ID: {}", field_id);
        return false;
    }

    let operator = operator_opt.unwrap();
    if operator.operator_type.is_none() {
        warn!(
            "Text condition operator type is empty, field ID: {}",
            field_id
        );
        return false;
    }

    let op_type = operator.operator_type.as_ref().unwrap();

    let text_value = match vertex.field_values.as_ref().and_then(|values| {
        values
            .get(&Identifier::new(field_id))
            .and_then(|field_value| {
                if let FieldValue::Text(text_value) = field_value {
                    Some(text_value.text.as_str())
                } else {
                    None
                }
            })
    }) {
        Some(value) => value,
        None => {
            return match op_type {
                query::text_operator::OperatorType::IsBlank(_) => true,
                query::text_operator::OperatorType::NotEqual(_) => true,
                query::text_operator::OperatorType::NotContains(_) => true,
                _ => false,
            };
        }
    };

    match op_type {
        query::text_operator::OperatorType::Equal(equal_op) => text_value == equal_op.value,
        query::text_operator::OperatorType::NotEqual(not_equal_op) => {
            text_value != not_equal_op.value
        }
        query::text_operator::OperatorType::Contains(contains_op) => {
            text_value.contains(&contains_op.value)
        }
        query::text_operator::OperatorType::NotContains(not_contains_op) => {
            !text_value.contains(&not_contains_op.value)
        }
        query::text_operator::OperatorType::StartsWith(starts_with_op) => {
            text_value.starts_with(&starts_with_op.value)
        }
        query::text_operator::OperatorType::EndsWith(ends_with_op) => {
            text_value.ends_with(&ends_with_op.value)
        }
        query::text_operator::OperatorType::IsBlank(_) => text_value.is_empty(),
        query::text_operator::OperatorType::IsNotBlank(_) => !text_value.is_empty(),
    }
}

// ============================================================================
// 数字条件评估
// ============================================================================

fn evaluate_number_condition<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    item: &query::NumberConditionItem,
    txn: &T,
) -> bool {
    if let Some(subject) = &item.subject {
        if let Some(path) = &subject.path {
            if !path.nodes.is_empty() {
                let related_vertices = get_related_vertices(vertex, path, txn);

                if related_vertices.is_empty() {
                    if let Some(operator) = &item.operator {
                        if let Some(op_type) = &operator.operator_type {
                            return match op_type {
                                query::number_operator::OperatorType::IsNull(_) => true,
                                query::number_operator::OperatorType::NotEqual(_) => true,
                                query::number_operator::OperatorType::IsNotNull(_) => false,
                                _ => false,
                            };
                        }
                    }
                    return false;
                }

                for related_vertex in &related_vertices {
                    if evaluate_number_field_on_vertex(
                        related_vertex,
                        subject.field_id.as_str(),
                        item.operator.as_ref(),
                    ) {
                        return true;
                    }
                }
                return false;
            }
        }

        evaluate_number_field_on_vertex(
            vertex,
            subject.field_id.as_str(),
            item.operator.as_ref(),
        )
    } else {
        warn!("Number condition subject is empty");
        false
    }
}

fn evaluate_number_field_on_vertex(
    vertex: &Arc<Vertex>,
    field_id: &str,
    operator_opt: Option<&query::NumberOperator>,
) -> bool {
    if operator_opt.is_none() {
        warn!(
            "Number condition operator is empty, field ID: {}",
            field_id
        );
        return false;
    }

    let operator = operator_opt.unwrap();
    if operator.operator_type.is_none() {
        warn!(
            "Number condition operator type is empty, field ID: {}",
            field_id
        );
        return false;
    }

    let op_type = operator.operator_type.as_ref().unwrap();

    let num_value = match vertex.field_values.as_ref().and_then(|values| {
        values
            .get(&Identifier::new(field_id))
            .and_then(|field_value| {
                if let FieldValue::Number(number_value) = field_value {
                    Some(number_value.number)
                } else {
                    None
                }
            })
    }) {
        Some(value) => value,
        None => {
            return match op_type {
                query::number_operator::OperatorType::IsNull(_) => true,
                query::number_operator::OperatorType::NotEqual(_) => true,
                query::number_operator::OperatorType::NotBetween(_) => true,
                _ => false,
            };
        }
    };

    match op_type {
        query::number_operator::OperatorType::Equal(equal_op) => {
            (num_value - equal_op.value).abs() < std::f64::EPSILON
        }
        query::number_operator::OperatorType::NotEqual(not_equal_op) => {
            (num_value - not_equal_op.value).abs() >= std::f64::EPSILON
        }
        query::number_operator::OperatorType::GreaterThan(gt_op) => num_value > gt_op.value,
        query::number_operator::OperatorType::LessThan(lt_op) => num_value < lt_op.value,
        query::number_operator::OperatorType::GreaterThanOrEqual(gte_op) => {
            num_value >= gte_op.value
        }
        query::number_operator::OperatorType::LessThanOrEqual(lte_op) => num_value <= lte_op.value,
        query::number_operator::OperatorType::Between(between_op) => {
            num_value >= between_op.min_value && num_value <= between_op.max_value
        }
        query::number_operator::OperatorType::NotBetween(not_between_op) => {
            num_value < not_between_op.min_value || num_value > not_between_op.max_value
        }
        query::number_operator::OperatorType::IsNull(_) => false,
        query::number_operator::OperatorType::IsNotNull(_) => true,
    }
}

// ============================================================================
// 枚举条件评估
// ============================================================================

fn evaluate_enum_condition<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    item: &query::EnumConditionItem,
    txn: &T,
    context: &Option<FilterContext>,
    evaluate_context: &mut EvaluateContext,
) -> bool {
    if let Some(subject) = &item.subject {
        if let Some(path) = &subject.path {
            if !path.nodes.is_empty() {
                let related_vertices = get_related_vertices(vertex, path, txn);

                if related_vertices.is_empty() {
                    if let Some(operator) = &item.operator {
                        if let Some(op_type) = &operator.operator_type {
                            return match op_type {
                                query::enum_operator::OperatorType::IsNull(_) => true,
                                query::enum_operator::OperatorType::NotIn(_) => true,
                                query::enum_operator::OperatorType::NotEqual(_) => true,
                                query::enum_operator::OperatorType::IsNotNull(_) => false,
                                _ => false,
                            };
                        }
                    }
                    return false;
                }

                for related_vertex in &related_vertices {
                    if evaluate_enum_field_on_vertex(
                        related_vertex,
                        subject.field_id.as_str(),
                        item.operator.as_ref(),
                        txn,
                        context,
                        &mut evaluate_context.refer_enum_cache,
                    ) {
                        return true;
                    }
                }
                return false;
            }
        }

        evaluate_enum_field_on_vertex(
            vertex,
            subject.field_id.as_str(),
            item.operator.as_ref(),
            txn,
            context,
            &mut evaluate_context.refer_enum_cache,
        )
    } else {
        warn!("Enum condition subject is empty");
        false
    }
}

fn evaluate_enum_field_on_vertex<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    field_id: &str,
    operator_opt: Option<&query::EnumOperator>,
    txn: &T,
    context: &Option<FilterContext>,
    refer_enum_cache: &mut HashMap<String, Vec<Vec<String>>>,
) -> bool {
    if operator_opt.is_none() {
        warn!("Enum condition operator is empty, field ID: {}", field_id);
        return false;
    }

    let operator = operator_opt.unwrap();
    if operator.operator_type.is_none() {
        warn!(
            "Enum condition operator type is empty, field ID: {}",
            field_id
        );
        return false;
    }

    if let Some(field_values) = &vertex.field_values {
        if let Some(field_value) = field_values.get(&Identifier::new(field_id)) {
            return match field_value {
                FieldValue::Enum(enum_value) => {
                    let items_ref = &enum_value.items;

                    match &operator.operator_type.as_ref().unwrap() {
                        query::enum_operator::OperatorType::Equal(equal_op) => {
                            if let Some(value_type) = &equal_op.value_type {
                                match value_type {
                                    query::enum_equal_operator::ValueType::StaticValues(
                                        static_values,
                                    ) => {
                                        if static_values.values.len() != items_ref.len() {
                                            return false;
                                        }
                                        items_ref
                                            .iter()
                                            .all(|id| static_values.values.contains(id))
                                    }
                                    query::enum_equal_operator::ValueType::ReferEnum(refer_enum) => {
                                        let referred_values_list = get_refer_enum_values(
                                            refer_enum,
                                            vertex,
                                            txn,
                                            context,
                                            refer_enum_cache,
                                        );

                                        if !referred_values_list.is_empty() {
                                            referred_values_list.iter().any(|values| {
                                                if values.len() != items_ref.len() {
                                                    return false;
                                                }
                                                items_ref.iter().all(|id| values.contains(id))
                                            })
                                        } else {
                                            debug!("无法获取引用枚举值: {:?}", refer_enum);
                                            false
                                        }
                                    }
                                }
                            } else {
                                false
                            }
                        }
                        query::enum_operator::OperatorType::NotEqual(not_equal_op) => {
                            if let Some(value_type) = &not_equal_op.value_type {
                                match value_type {
                                    query::enum_not_equal_operator::ValueType::StaticValues(
                                        static_values,
                                    ) => {
                                        if static_values.values.len() != items_ref.len() {
                                            return true;
                                        }
                                        items_ref
                                            .iter()
                                            .any(|id| !static_values.values.contains(id))
                                    }
                                    query::enum_not_equal_operator::ValueType::ReferEnum(
                                        refer_enum,
                                    ) => {
                                        let referred_values_list = get_refer_enum_values(
                                            refer_enum,
                                            vertex,
                                            txn,
                                            context,
                                            refer_enum_cache,
                                        );
                                        if !referred_values_list.is_empty() {
                                            referred_values_list.iter().any(|values| {
                                                if values.len() != items_ref.len() {
                                                    return true;
                                                }
                                                items_ref.iter().any(|id| !values.contains(id))
                                            })
                                        } else {
                                            true
                                        }
                                    }
                                }
                            } else {
                                true
                            }
                        }
                        query::enum_operator::OperatorType::In(in_op) => {
                            if let Some(values_type) = &in_op.values_type {
                                match values_type {
                                    query::enum_in_operator::ValuesType::StaticValues(
                                        static_values,
                                    ) => items_ref
                                        .iter()
                                        .any(|id| static_values.values.contains(id)),
                                    query::enum_in_operator::ValuesType::ReferEnum(refer_enum) => {
                                        let referred_values_list = get_refer_enum_values(
                                            refer_enum,
                                            vertex,
                                            txn,
                                            context,
                                            refer_enum_cache,
                                        );
                                        if !referred_values_list.is_empty() {
                                            referred_values_list.iter().any(|values| {
                                                items_ref.iter().any(|id| values.contains(id))
                                            })
                                        } else {
                                            false
                                        }
                                    }
                                }
                            } else {
                                false
                            }
                        }
                        query::enum_operator::OperatorType::NotIn(not_in_op) => {
                            if let Some(values_type) = &not_in_op.values_type {
                                match values_type {
                                    query::enum_not_in_operator::ValuesType::StaticValues(
                                        static_values,
                                    ) => !items_ref
                                        .iter()
                                        .any(|id| static_values.values.contains(id)),
                                    query::enum_not_in_operator::ValuesType::ReferEnum(
                                        refer_enum,
                                    ) => {
                                        let referred_values_list = get_refer_enum_values(
                                            refer_enum,
                                            vertex,
                                            txn,
                                            context,
                                            refer_enum_cache,
                                        );
                                        if !referred_values_list.is_empty() {
                                            !referred_values_list.iter().any(|values| {
                                                items_ref.iter().any(|id| values.contains(id))
                                            })
                                        } else {
                                            true
                                        }
                                    }
                                }
                            } else {
                                true
                            }
                        }
                        query::enum_operator::OperatorType::IsNull(_) => items_ref.is_empty(),
                        query::enum_operator::OperatorType::IsNotNull(_) => !items_ref.is_empty(),
                    }
                }
                _ => {
                    warn!("Current field is not an enum type, field ID: {}", field_id);
                    false
                }
            };
        }
    }

    if let Some(op_type) = &operator.operator_type {
        return match op_type {
            query::enum_operator::OperatorType::IsNull(_) => true,
            query::enum_operator::OperatorType::NotIn(_) => true,
            query::enum_operator::OperatorType::NotEqual(_) => true,
            _ => false,
        };
    }

    false
}

fn get_refer_enum_values<'a, T: Transaction<'a>>(
    refer_enum: &query::ReferEnum,
    context_vertex: &Arc<Vertex>,
    txn: &T,
    context: &Option<FilterContext>,
    refer_enum_cache: &mut HashMap<String, Vec<Vec<String>>>,
) -> Vec<Vec<String>> {
    let Some(filter_context) = context.as_ref() else {
        return vec![];
    };
    let cache_key = match &refer_enum.refer_on_type {
        Some(query::refer_enum::ReferOnType::ReferOnCurrentCard(_)) => String::new(),
        Some(query::refer_enum::ReferOnType::ReferOnParameterCard(param_card)) => {
            match filter_context.parameters.get(&param_card.parameter_card_type_id) {
                Some(id) => {
                    if let Some(path) = &param_card.path {
                        id.to_string() + &path.to_key() + &refer_enum.field_id
                    } else {
                        id.to_string() + &refer_enum.field_id
                    }
                }
                None => {
                    warn!(
                        "Cannot find parameter card ID: {}",
                        param_card.parameter_card_type_id
                    );
                    return Vec::new();
                }
            }
        }
        Some(query::refer_enum::ReferOnType::ReferOnMember(refer_member)) => {
            match filter_context.member_id {
                Some(id) => {
                    if let Some(path) = &refer_member.path {
                        id.to_string() + &path.to_key() + &refer_enum.field_id
                    } else {
                        id.to_string() + &refer_enum.field_id
                    }
                }
                None => {
                    warn!("Cannot find member card ID");
                    return Vec::new();
                }
            }
        }
        Some(query::refer_enum::ReferOnType::ReferOnContextualCard(contextual_card)) => {
            if let Some(path) = &contextual_card.path {
                contextual_card.contextual_card_id.to_string()
                    + &path.to_key()
                    + &refer_enum.field_id
            } else {
                contextual_card.contextual_card_id.to_string() + &refer_enum.field_id
            }
        }
        None => {
            warn!("Reference enum has no reference type specified");
            return Vec::new();
        }
    };

    let is_current_card_ref = matches!(
        &refer_enum.refer_on_type,
        Some(query::refer_enum::ReferOnType::ReferOnCurrentCard(_))
    );

    if !is_current_card_ref {
        if let Some(cached_result) = refer_enum_cache.get(&cache_key) {
            debug!("Using cached enum value, key: {}", cache_key);
            return cached_result.clone();
        }
    }

    let mut source_vertices = match &refer_enum.refer_on_type {
        Some(query::refer_enum::ReferOnType::ReferOnCurrentCard(current_card)) => {
            if let Some(path) = &current_card.path {
                if !path.nodes.is_empty() {
                    get_related_vertices(context_vertex, path, txn)
                } else {
                    vec![context_vertex.clone()]
                }
            } else {
                vec![context_vertex.clone()]
            }
        }
        Some(query::refer_enum::ReferOnType::ReferOnParameterCard(param_card)) => {
            match filter_context.parameters.get(&param_card.parameter_card_type_id) {
                Some(param_vertex_id) => {
                    if let Some(path) = &param_card.path {
                        if !path.nodes.is_empty() {
                            let related_ids =
                                get_related_vertex_ids(param_vertex_id, path, txn, false);
                            related_ids
                                .into_iter()
                                .filter_map(|id| {
                                    txn.get_specific_vertices(&vec![id])
                                        .ok()
                                        .and_then(|vertices| vertices.first().cloned())
                                })
                                .collect()
                        } else {
                            txn.get_specific_vertices(&vec![*param_vertex_id])
                                .ok()
                                .and_then(|vertices| vertices.first().cloned())
                                .map_or(vec![], |v| vec![v])
                        }
                    } else {
                        txn.get_specific_vertices(&vec![*param_vertex_id])
                            .ok()
                            .and_then(|vertices| vertices.first().cloned())
                            .map_or(vec![], |v| vec![v])
                    }
                }
                None => {
                    warn!(
                        "Cannot find parameter card ID: {}",
                        param_card.parameter_card_type_id
                    );
                    vec![]
                }
            }
        }
        Some(query::refer_enum::ReferOnType::ReferOnMember(member)) => {
            if let Some(path) = &member.path {
                if let Some(member_id) = filter_context.member_id {
                    if !path.nodes.is_empty() {
                        let related_ids = get_related_vertex_ids(&member_id, path, txn, false);
                        related_ids
                            .into_iter()
                            .filter_map(|id| {
                                txn.get_specific_vertices(&vec![id])
                                    .ok()
                                    .and_then(|vertices| vertices.first().cloned())
                            })
                            .collect()
                    } else {
                        txn.get_specific_vertices(&vec![member_id])
                            .ok()
                            .and_then(|vertices| vertices.first().cloned())
                            .map_or(vec![], |v| vec![v])
                    }
                } else {
                    warn!("Cannot find member card ID");
                    vec![]
                }
            } else if let Some(member_id) = filter_context.member_id {
                txn.get_specific_vertices(&vec![member_id])
                    .ok()
                    .and_then(|vertices| vertices.first().cloned())
                    .map_or(vec![], |v| vec![v])
            } else {
                warn!("Cannot find member card ID");
                vec![]
            }
        }
        Some(query::refer_enum::ReferOnType::ReferOnContextualCard(contextual_card)) => {
            if let Some(path) = &contextual_card.path {
                if !path.nodes.is_empty() {
                    let related_ids = get_related_vertex_ids(
                        &contextual_card.contextual_card_id,
                        path,
                        txn,
                        false,
                    );
                    related_ids
                        .into_iter()
                        .filter_map(|id| {
                            txn.get_specific_vertices(&vec![id])
                                .ok()
                                .and_then(|vertices| vertices.first().cloned())
                        })
                        .collect()
                } else {
                    txn.get_specific_vertices(&vec![contextual_card.contextual_card_id])
                        .ok()
                        .and_then(|vertices| vertices.first().cloned())
                        .map_or(vec![], |v| vec![v])
                }
            } else {
                txn.get_specific_vertices(&vec![contextual_card.contextual_card_id])
                    .ok()
                    .and_then(|vertices| vertices.first().cloned())
                    .map_or(vec![], |v| vec![v])
            }
        }
        None => {
            warn!("Reference enum has no reference type specified");
            vec![]
        }
    };

    source_vertices.dedup_by_key(|v: &mut Arc<Vertex>| v.card_id);

    let field_id = &refer_enum.field_id;
    let mut result = Vec::new();

    for vertex in source_vertices {
        if let Some(field_values) = &vertex.field_values {
            if let Some(field_value) = field_values.get(&Identifier::new(field_id)) {
                match field_value {
                    FieldValue::Enum(enum_value) => {
                        let values: Vec<String> =
                            enum_value.items.iter().map(|id| id.to_string()).collect();
                        if !values.is_empty() {
                            result.push(values);
                        }
                    }
                    _ => {
                        debug!("Referenced field is not an enum type: {}", field_id);
                    }
                }
            } else {
                debug!("Referenced field does not exist: {}", field_id);
            }
        }
    }

    if !is_current_card_ref {
        refer_enum_cache.insert(cache_key, result.clone());
    }

    result
}

// ============================================================================
// 日期条件评估
// ============================================================================

fn evaluate_date_condition<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    item: &query::DateConditionItem,
    txn: &T,
) -> bool {
    let normalized_op =
        get_normalized_date_operator(item.operator.as_ref(), Some(vertex), Some(txn));
    if let Some(subject) = &item.subject {
        if let Some(path) = &subject.path {
            if !path.nodes.is_empty() {
                let related_vertices = get_related_vertices(vertex, path, txn);

                if related_vertices.is_empty() {
                    if let Some(operator) = &item.operator {
                        if let Some(op_type) = &operator.operator_type {
                            return match op_type {
                                query::date_operator::OperatorType::IsNull(_) => true,
                                query::date_operator::OperatorType::NotEqual(_) => true,
                                query::date_operator::OperatorType::NotBetween(_) => true,
                                query::date_operator::OperatorType::IsNotNull(_) => false,
                                _ => false,
                            };
                        }
                    }
                    return false;
                }

                for related_vertex in &related_vertices {
                    if evaluate_date_on_vertex_with_normalized_op(
                        related_vertex,
                        subject.field_id.as_str(),
                        &normalized_op,
                    ) {
                        return true;
                    }
                }
                return false;
            }
        }

        evaluate_date_on_vertex_with_normalized_op(
            vertex,
            subject.field_id.as_str(),
            &normalized_op,
        )
    } else {
        warn!("Date condition subject is empty");
        false
    }
}

enum NormalizedDateOperator {
    Equal { day_start: i64, day_end: i64 },
    NotEqual { day_start: i64, day_end: i64 },
    Before { day_start: i64 },
    After { day_end: i64 },
    BeforeOrEqual { day_start: i64, day_end: i64 },
    AfterOrEqual { day_start: i64, day_end: i64 },
    Between { day_start: i64, day_end: i64 },
    NotBetween { day_start: i64, day_end: i64 },
    IsNull,
    IsNotNull,
    EqualToAny { normalized_values: Vec<(i64, i64)> },
    NotEqualToAll { normalized_values: Vec<(i64, i64)> },
}

fn get_refer_date_values<'a, T: Transaction<'a>>(
    refer_date: &query::ReferDate,
    context_vertex: &Arc<Vertex>,
    txn: &T,
) -> Vec<i64> {
    let mut source_vertices = match &refer_date.refer_on_type {
        Some(query::refer_date::ReferOnType::ReferOnCurrentCard(current_card)) => {
            if let Some(path) = &current_card.path {
                if !path.nodes.is_empty() {
                    get_related_vertices(context_vertex, path, txn)
                } else {
                    vec![context_vertex.clone()]
                }
            } else {
                vec![context_vertex.clone()]
            }
        }
        Some(query::refer_date::ReferOnType::ReferOnParameterCard(param_card)) => {
            debug!("Need to get card from query parameters: {:?}", param_card);
            vec![]
        }
        Some(query::refer_date::ReferOnType::ReferOnMember(member)) => {
            debug!("Need to get member related date: {:?}", member);
            vec![]
        }
        Some(query::refer_date::ReferOnType::ReferOnContextualCard(contextual_card)) => {
            debug!(
                "Need to get card from contextual card: {:?}",
                contextual_card
            );
            vec![]
        }
        None => {
            warn!("Reference date has no reference type specified");
            vec![]
        }
    };

    source_vertices.sort_by_key(|v| v.card_id);
    source_vertices.dedup_by_key(|v| v.card_id);

    if source_vertices.is_empty() {
        return vec![];
    }

    let field_id = &refer_date.field_id;
    let mut result = Vec::new();

    for vertex in source_vertices {
        let timestamp = match field_id.as_str() {
            "created" => Some(vertex.created_at as i64 * 1000),
            "updated" => Some(vertex.updated_at as i64 * 1000),
            "discarded_at" => vertex.discarded_at.map(|v| v as i64 * 1000),
            "archived_at" => vertex.archived_at.map(|v| v as i64 * 1000),
            _ => {
                if let Some(field_values) = &vertex.field_values {
                    if let Some(field_value) = field_values.get(&Identifier::new(field_id)) {
                        match field_value {
                            FieldValue::Date(date_value) => Some(date_value.timestamp as i64),
                            _ => {
                                debug!("Referenced field is not a date type: {}", field_id);
                                None
                            }
                        }
                    } else {
                        debug!("Referenced field does not exist: {}", field_id);
                        None
                    }
                } else {
                    None
                }
            }
        };

        if let Some(ts) = timestamp {
            result.push(ts);
        }
    }

    result
}

fn get_normalized_date_operator<'a, T: Transaction<'a>>(
    operator_opt: Option<&query::DateOperator>,
    context_vertex: Option<&Arc<Vertex>>,
    txn: Option<&T>,
) -> NormalizedDateOperator {
    if operator_opt.is_none() {
        return NormalizedDateOperator::IsNull;
    }

    let operator = operator_opt.unwrap();
    if operator.operator_type.is_none() {
        return NormalizedDateOperator::IsNull;
    }

    match &operator.operator_type.as_ref().unwrap() {
        query::date_operator::OperatorType::Equal(equal_op) => {
            if let Some(value_type) = &equal_op.value_type {
                match value_type {
                    query::date_equal_operator::ValueType::StaticValue(timestamp) => {
                        let day_start = normalize_timestamp(*timestamp, false);
                        let day_end = normalize_timestamp(*timestamp, true);
                        NormalizedDateOperator::Equal { day_start, day_end }
                    }
                    query::date_equal_operator::ValueType::ReferDate(refer_date) => {
                        if let (Some(vertex), Some(transaction)) = (context_vertex, txn) {
                            let timestamps = get_refer_date_values(refer_date, vertex, transaction);
                            if !timestamps.is_empty() {
                                NormalizedDateOperator::EqualToAny {
                                    normalized_values: timestamps
                                        .iter()
                                        .map(|ts| {
                                            (
                                                normalize_timestamp(*ts, false),
                                                normalize_timestamp(*ts, true),
                                            )
                                        })
                                        .collect(),
                                }
                            } else {
                                debug!("无法获取引用日期值: {:?}", refer_date);
                                NormalizedDateOperator::IsNull
                            }
                        } else {
                            debug!("缺少处理引用日期所需的上下文: {:?}", refer_date);
                            NormalizedDateOperator::IsNull
                        }
                    }
                }
            } else {
                NormalizedDateOperator::IsNull
            }
        }
        query::date_operator::OperatorType::NotEqual(not_equal_op) => {
            if let Some(value_type) = &not_equal_op.value_type {
                match value_type {
                    query::date_not_equal_operator::ValueType::StaticValue(timestamp) => {
                        let day_start = normalize_timestamp(*timestamp, false);
                        let day_end = normalize_timestamp(*timestamp, true);
                        NormalizedDateOperator::NotEqual { day_start, day_end }
                    }
                    query::date_not_equal_operator::ValueType::ReferDate(refer_date) => {
                        if let (Some(vertex), Some(transaction)) = (context_vertex, txn) {
                            let timestamps = get_refer_date_values(refer_date, vertex, transaction);
                            if !timestamps.is_empty() {
                                NormalizedDateOperator::NotEqualToAll {
                                    normalized_values: timestamps
                                        .iter()
                                        .map(|ts| {
                                            (
                                                normalize_timestamp(*ts, false),
                                                normalize_timestamp(*ts, true),
                                            )
                                        })
                                        .collect(),
                                }
                            } else {
                                debug!("Cannot get referenced date value: {:?}", refer_date);
                                NormalizedDateOperator::IsNull
                            }
                        } else {
                            debug!(
                                "Missing context for processing referenced date: {:?}",
                                refer_date
                            );
                            NormalizedDateOperator::IsNull
                        }
                    }
                }
            } else {
                NormalizedDateOperator::IsNull
            }
        }
        query::date_operator::OperatorType::Before(before_op) => {
            if let Some(value_type) = &before_op.value_type {
                match value_type {
                    query::date_before_operator::ValueType::StaticValue(timestamp) => {
                        NormalizedDateOperator::Before {
                            day_start: normalize_timestamp(*timestamp, false),
                        }
                    }
                    query::date_before_operator::ValueType::ReferDate(refer_date) => {
                        if let (Some(vertex), Some(transaction)) = (context_vertex, txn) {
                            let timestamps = get_refer_date_values(refer_date, vertex, transaction);
                            if !timestamps.is_empty() {
                                let min_timestamp = *timestamps.iter().min().unwrap_or(&0);
                                let day_start = normalize_timestamp(min_timestamp, false);
                                NormalizedDateOperator::Before { day_start }
                            } else {
                                debug!("Cannot get referenced date value: {:?}", refer_date);
                                NormalizedDateOperator::IsNull
                            }
                        } else {
                            debug!(
                                "Missing context for processing referenced date: {:?}",
                                refer_date
                            );
                            NormalizedDateOperator::IsNull
                        }
                    }
                }
            } else {
                NormalizedDateOperator::IsNull
            }
        }
        query::date_operator::OperatorType::After(after_op) => {
            if let Some(value_type) = &after_op.value_type {
                match value_type {
                    query::date_after_operator::ValueType::StaticValue(timestamp) => {
                        NormalizedDateOperator::After {
                            day_end: normalize_timestamp(*timestamp, true),
                        }
                    }
                    query::date_after_operator::ValueType::ReferDate(refer_date) => {
                        if let (Some(vertex), Some(transaction)) = (context_vertex, txn) {
                            let timestamps = get_refer_date_values(refer_date, vertex, transaction);
                            if !timestamps.is_empty() {
                                let max_timestamp = *timestamps.iter().max().unwrap_or(&0);
                                let day_end = normalize_timestamp(max_timestamp, true);
                                NormalizedDateOperator::After { day_end }
                            } else {
                                debug!("Cannot get referenced date value: {:?}", refer_date);
                                NormalizedDateOperator::IsNull
                            }
                        } else {
                            debug!(
                                "Missing context for processing referenced date: {:?}",
                                refer_date
                            );
                            NormalizedDateOperator::IsNull
                        }
                    }
                }
            } else {
                NormalizedDateOperator::IsNull
            }
        }
        query::date_operator::OperatorType::BeforeOrEqual(before_or_equal_op) => {
            if let Some(value_type) = &before_or_equal_op.value_type {
                match value_type {
                    query::date_before_or_equal_operator::ValueType::StaticValue(timestamp) => {
                        NormalizedDateOperator::BeforeOrEqual {
                            day_start: normalize_timestamp(*timestamp, false),
                            day_end: normalize_timestamp(*timestamp, true),
                        }
                    }
                    query::date_before_or_equal_operator::ValueType::ReferDate(refer_date) => {
                        if let (Some(vertex), Some(transaction)) = (context_vertex, txn) {
                            let timestamps = get_refer_date_values(refer_date, vertex, transaction);
                            if !timestamps.is_empty() {
                                let min_timestamp = *timestamps.iter().min().unwrap_or(&0);
                                let day_start = normalize_timestamp(min_timestamp, false);
                                let day_end = normalize_timestamp(min_timestamp, true);
                                NormalizedDateOperator::BeforeOrEqual { day_start, day_end }
                            } else {
                                debug!("Cannot get referenced date value: {:?}", refer_date);
                                NormalizedDateOperator::IsNull
                            }
                        } else {
                            debug!(
                                "Missing context for processing referenced date: {:?}",
                                refer_date
                            );
                            NormalizedDateOperator::IsNull
                        }
                    }
                }
            } else {
                NormalizedDateOperator::IsNull
            }
        }
        query::date_operator::OperatorType::AfterOrEqual(after_or_equal_op) => {
            if let Some(value_type) = &after_or_equal_op.value_type {
                match value_type {
                    query::date_after_or_equal_operator::ValueType::StaticValue(timestamp) => {
                        let day_start = normalize_timestamp(*timestamp, false);
                        let day_end = normalize_timestamp(*timestamp, true);
                        NormalizedDateOperator::AfterOrEqual { day_start, day_end }
                    }
                    query::date_after_or_equal_operator::ValueType::ReferDate(refer_date) => {
                        if let (Some(vertex), Some(transaction)) = (context_vertex, txn) {
                            let timestamps = get_refer_date_values(refer_date, vertex, transaction);
                            if !timestamps.is_empty() {
                                let max_timestamp = *timestamps.iter().max().unwrap_or(&0);
                                let day_start = normalize_timestamp(max_timestamp, true);
                                let day_end = normalize_timestamp(max_timestamp, true);
                                NormalizedDateOperator::AfterOrEqual { day_start, day_end }
                            } else {
                                debug!("Cannot get referenced date value: {:?}", refer_date);
                                NormalizedDateOperator::IsNull
                            }
                        } else {
                            debug!(
                                "Missing context for processing referenced date: {:?}",
                                refer_date
                            );
                            NormalizedDateOperator::IsNull
                        }
                    }
                }
            } else {
                NormalizedDateOperator::IsNull
            }
        }
        query::date_operator::OperatorType::Between(between_op) => {
            let day_start = normalize_timestamp(between_op.static_start_value, false);
            let day_end = normalize_timestamp(between_op.static_end_value, true);
            NormalizedDateOperator::Between { day_start, day_end }
        }
        query::date_operator::OperatorType::NotBetween(not_between_op) => {
            let day_start = normalize_timestamp(not_between_op.static_start_value, false);
            let day_end = normalize_timestamp(not_between_op.static_end_value, true);
            NormalizedDateOperator::NotBetween { day_start, day_end }
        }
        query::date_operator::OperatorType::IsNull(_) => NormalizedDateOperator::IsNull,
        query::date_operator::OperatorType::IsNotNull(_) => NormalizedDateOperator::IsNotNull,
    }
}

fn evaluate_date_on_vertex_with_normalized_op(
    vertex: &Arc<Vertex>,
    field_id: &str,
    normalized_op: &NormalizedDateOperator,
) -> bool {
    let timestamp = match field_id {
        "created" => vertex.created_at as i64,
        "updated" => vertex.updated_at as i64,
        "abandonDate" => vertex.discarded_at.unwrap_or(0) as i64,
        "archivedDate" => vertex.archived_at.unwrap_or(0) as i64,
        _ => {
            if let Some(field_values) = &vertex.field_values {
                if let Some(field_value) = field_values.get(&Identifier::new(field_id)) {
                    match field_value {
                        FieldValue::Date(date_value) => date_value.timestamp as i64,
                        _ => {
                            warn!("Current field is not a date type, field ID: {}", field_id);
                            0
                        }
                    }
                } else {
                    0
                }
            } else {
                0
            }
        }
    };

    if timestamp == 0 {
        return match normalized_op {
            NormalizedDateOperator::IsNull => true,
            NormalizedDateOperator::NotEqual { .. } => true,
            NormalizedDateOperator::NotBetween { .. } => true,
            _ => false,
        };
    }

    match normalized_op {
        NormalizedDateOperator::Equal { day_start, day_end } => {
            timestamp >= *day_start && timestamp <= *day_end
        }
        NormalizedDateOperator::NotEqual { day_start, day_end } => {
            timestamp < *day_start || timestamp > *day_end
        }
        NormalizedDateOperator::Before { day_start } => timestamp < *day_start,
        NormalizedDateOperator::After { day_end } => timestamp > *day_end,
        NormalizedDateOperator::BeforeOrEqual { day_start, day_end } => {
            timestamp <= *day_start || (timestamp >= *day_start && timestamp <= *day_end)
        }
        NormalizedDateOperator::AfterOrEqual { day_start, day_end } => {
            timestamp >= *day_end || (timestamp >= *day_start && timestamp <= *day_end)
        }
        NormalizedDateOperator::Between { day_start, day_end } => {
            timestamp >= *day_start && timestamp <= *day_end
        }
        NormalizedDateOperator::NotBetween { day_start, day_end } => {
            timestamp < *day_start || timestamp > *day_end
        }
        NormalizedDateOperator::IsNull => timestamp == 0,
        NormalizedDateOperator::IsNotNull => timestamp != 0,
        NormalizedDateOperator::EqualToAny { normalized_values } => normalized_values
            .iter()
            .any(|(value, day_end)| timestamp >= *value && timestamp <= *day_end),
        NormalizedDateOperator::NotEqualToAll { normalized_values } => normalized_values
            .iter()
            .all(|(value, day_end)| timestamp < *value || timestamp > *day_end),
    }
}

// ============================================================================
// 价值流状态条件评估
// ============================================================================

fn evaluate_status_condition<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    item: &query::StatusConditionItem,
    txn: &T,
) -> bool {
    if let Some(subject) = &item.subject {
        if let Some(path) = &subject.path {
            if !path.nodes.is_empty() {
                let related_vertices = get_related_vertices(vertex, path, txn);

                if related_vertices.is_empty() {
                    if let Some(operator) = &item.operator {
                        if let Some(op_type) = &operator.operator_type {
                            return match op_type {
                                query::status_operator::OperatorType::Equal(_) => false,
                                query::status_operator::OperatorType::NotEqual(_) => true,
                                query::status_operator::OperatorType::In(_) => false,
                                query::status_operator::OperatorType::NotIn(_) => true,
                            };
                        }
                    }
                    return false;
                }

                for related_vertex in &related_vertices {
                    if evaluate_status_on_vertex(related_vertex, item.operator.as_ref()) {
                        return true;
                    }
                }
                return false;
            }
        }
        return evaluate_status_on_vertex(vertex, item.operator.as_ref());
    } else {
        return evaluate_status_on_vertex(vertex, item.operator.as_ref());
    }
}

fn evaluate_status_on_vertex(
    vertex: &Arc<Vertex>,
    operator_opt: Option<&query::StatusOperator>,
) -> bool {
    if let Some(operator) = operator_opt {
        if let Some(op_type) = &operator.operator_type {
            match op_type {
                query::status_operator::OperatorType::Equal(equal_op) => {
                    vertex.stream_info.stream_id.as_str() == equal_op.stream_id
                        && vertex.stream_info.status_id.as_str() == equal_op.status_id
                }
                query::status_operator::OperatorType::NotEqual(not_equal_op) => {
                    vertex.stream_info.stream_id.as_str() != not_equal_op.stream_id
                        || vertex.stream_info.status_id.as_str() != not_equal_op.status_id
                }
                query::status_operator::OperatorType::In(in_op) => {
                    vertex.stream_info.stream_id.as_str() == in_op.stream_id
                        && in_op.values.contains(&vertex.stream_info.status_id)
                }
                query::status_operator::OperatorType::NotIn(not_in_op) => {
                    vertex.stream_info.stream_id.as_str() != not_in_op.stream_id
                        || !not_in_op.values.contains(&vertex.stream_info.status_id)
                }
            }
        } else {
            warn!("Status condition operator type is empty");
            false
        }
    } else {
        warn!("Status condition operator is empty");
        false
    }
}

// ============================================================================
// 关联卡片条件评估
// ============================================================================

fn evaluate_link_condition<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    item: &query::LinkConditionItem,
    txn: &T,
    context: &Option<FilterContext>,
) -> bool {
    if item.subject.is_none() {
        warn!("Link condition subject is empty");
        return false;
    }
    if item.operator.is_none() {
        warn!("Link condition operator is empty");
        return false;
    }
    if item.operator.as_ref().unwrap().operator_type.is_none() {
        warn!("Link condition operator type is empty");
        return false;
    }

    let subject = item.subject.as_ref().unwrap();
    let operator = item.operator.as_ref().unwrap();
    let operator_type = operator.operator_type.as_ref().unwrap();

    if let Some(subject_path) = &subject.path {
        let mut subject_vertex_ids = vec![vertex.card_id];
        if !subject_path.nodes.is_empty() {
            subject_vertex_ids = get_related_vertex_ids(&vertex.card_id, subject_path, txn, false);
        }
        if subject_vertex_ids.is_empty() {
            return match operator_type {
                query::link_operator::OperatorType::IsNull(_) => true,
                query::link_operator::OperatorType::IsNotNull(_) => false,
                query::link_operator::OperatorType::NotEqual(_)
                | query::link_operator::OperatorType::NotIn(_) => true,
                _ => false,
            };
        } else {
            return match operator_type {
                query::link_operator::OperatorType::IsNull(_) => false,
                query::link_operator::OperatorType::IsNotNull(_) => true,
                query::link_operator::OperatorType::Equal(_)
                | query::link_operator::OperatorType::In(_) => {
                    subject_vertex_ids.iter().any(|subject_vertex_id| {
                        evaluate_link_on_vertex(
                            subject_vertex_id,
                            vertex,
                            item.operator.as_ref(),
                            txn,
                            context,
                        )
                    })
                }
                query::link_operator::OperatorType::NotEqual(_)
                | query::link_operator::OperatorType::NotIn(_) => {
                    subject_vertex_ids.iter().all(|subject_vertex_id| {
                        evaluate_link_on_vertex(
                            subject_vertex_id,
                            vertex,
                            item.operator.as_ref(),
                            txn,
                            context,
                        )
                    })
                }
            };
        }
    }

    evaluate_link_on_vertex(&vertex.card_id, vertex, item.operator.as_ref(), txn, context)
}

fn evaluate_link_on_vertex<'a, T: Transaction<'a>>(
    subject_vertex_id: &VertexId,
    current_vertex: &Arc<Vertex>,
    operator_opt: Option<&query::LinkOperator>,
    txn: &T,
    context: &Option<FilterContext>,
) -> bool {
    if operator_opt.is_none() {
        warn!("Link condition operator is empty");
        return false;
    }

    let operator = operator_opt.unwrap();
    if operator.operator_type.is_none() {
        warn!("Link condition operator type is empty");
        return false;
    }

    match operator.operator_type.as_ref().unwrap() {
        query::link_operator::OperatorType::Equal(equal_op) => {
            evaluate_link_equal(subject_vertex_id, current_vertex, equal_op, txn, context)
        }
        query::link_operator::OperatorType::NotEqual(not_equal_op) => evaluate_link_not_equal(
            subject_vertex_id,
            current_vertex,
            not_equal_op,
            txn,
            context,
        ),
        query::link_operator::OperatorType::In(in_op) => {
            evaluate_link_in(subject_vertex_id, current_vertex, in_op, txn, context)
        }
        query::link_operator::OperatorType::NotIn(not_in_op) => {
            evaluate_link_not_in(subject_vertex_id, current_vertex, not_in_op, txn, context)
        }
        _ => false,
    }
}

fn evaluate_link_equal<'a, T: Transaction<'a>>(
    subject_vertex_id: &VertexId,
    current_vertex: &Arc<Vertex>,
    equal_op: &query::LinkEqualOperator,
    txn: &T,
    context: &Option<FilterContext>,
) -> bool {
    if let Some(value) = &equal_op.value {
        match value {
            query::link_equal_operator::Value::SpecialLink(special_link) => {
                if !special_link.card_ids.is_empty() {
                    return special_link.card_ids.contains(subject_vertex_id);
                }
            }
            query::link_equal_operator::Value::ReferLink(refer_link) => {
                if let Some(refer_type) = &refer_link.refer_on_type {
                    let ref_vertex_id =
                        get_reference_vertex_id(&current_vertex.card_id, refer_type, context);

                    if ref_vertex_id.is_none() {
                        return false;
                    }
                    return process_path_and_compare(
                        subject_vertex_id,
                        refer_type,
                        &ref_vertex_id.unwrap(),
                        txn,
                        false,
                    );
                }
            }
        }
    }
    false
}

fn evaluate_link_not_equal<'a, T: Transaction<'a>>(
    subject_vertex_id: &VertexId,
    current_vertex: &Arc<Vertex>,
    not_equal_op: &query::LinkNotEqualOperator,
    txn: &T,
    context: &Option<FilterContext>,
) -> bool {
    if let Some(value) = &not_equal_op.value {
        match value {
            query::link_not_equal_operator::Value::SpecialLink(special_link) => {
                if !special_link.card_ids.is_empty() {
                    return !special_link.card_ids.contains(subject_vertex_id);
                }
                return true;
            }
            query::link_not_equal_operator::Value::ReferLink(refer_link) => {
                if let Some(refer_type) = &refer_link.refer_on_type {
                    let ref_vertex_id =
                        get_reference_vertex_id(&current_vertex.card_id, refer_type, context);

                    if ref_vertex_id.is_none() {
                        return true;
                    }
                    return process_path_and_compare(
                        subject_vertex_id,
                        refer_type,
                        &ref_vertex_id.unwrap(),
                        txn,
                        true,
                    );
                }
            }
        }
    }
    true
}

fn evaluate_link_in<'a, T: Transaction<'a>>(
    subject_vertex_id: &VertexId,
    vertex: &Arc<Vertex>,
    in_op: &query::LinkInOperator,
    txn: &T,
    context: &Option<FilterContext>,
) -> bool {
    if let Some(value) = &in_op.value {
        match value {
            query::link_in_operator::Value::SpecialLink(special_link) => {
                if !special_link.card_ids.is_empty() {
                    return special_link.card_ids.contains(subject_vertex_id);
                }
            }
            query::link_in_operator::Value::ReferLink(refer_link) => {
                if let Some(refer_type) = &refer_link.refer_on_type {
                    let ref_vertex_id = get_reference_vertex_id(&vertex.card_id, refer_type, context);
                    if ref_vertex_id.is_none() {
                        return false;
                    }
                    return process_path_and_compare(
                        subject_vertex_id,
                        refer_type,
                        &ref_vertex_id.unwrap(),
                        txn,
                        false,
                    );
                }
            }
        }
    }
    false
}

fn evaluate_link_not_in<'a, T: Transaction<'a>>(
    subject_vertex_id: &VertexId,
    vertex: &Arc<Vertex>,
    not_in_op: &query::LinkNotInOperator,
    txn: &T,
    context: &Option<FilterContext>,
) -> bool {
    if let Some(value) = &not_in_op.value {
        match value {
            query::link_not_in_operator::Value::SpecialLink(special_link) => {
                if !special_link.card_ids.is_empty() {
                    return !special_link.card_ids.contains(subject_vertex_id);
                }
                return true;
            }
            query::link_not_in_operator::Value::ReferLink(refer_link) => {
                if let Some(refer_type) = &refer_link.refer_on_type {
                    let ref_vertex_id = get_reference_vertex_id(&vertex.card_id, refer_type, context);
                    if ref_vertex_id.is_none() {
                        return true;
                    }
                    return process_path_and_compare(
                        subject_vertex_id,
                        refer_type,
                        &ref_vertex_id.unwrap(),
                        txn,
                        true,
                    );
                }
                return true;
            }
        }
    }
    true
}

fn get_reference_vertex_id(
    current_vertex_id: &VertexId,
    refer_on_type: &query::refer_link::ReferOnType,
    context: &Option<FilterContext>,
) -> Option<VertexId> {
    match refer_on_type {
        query::refer_link::ReferOnType::ReferOnCurrentCard(_) => {
            if context.is_some() {
                Some(*current_vertex_id)
            } else {
                None
            }
        }
        query::refer_link::ReferOnType::ReferOnParameterCard(param_ref) => {
            if let Some(ctx) = context {
                ctx.parameters.get(&param_ref.parameter_card_type_id).cloned()
            } else {
                None
            }
        }
        query::refer_link::ReferOnType::ReferOnMember(_) => {
            if let Some(ctx) = context {
                ctx.member_id
            } else {
                None
            }
        }
        query::refer_link::ReferOnType::ReferOnContextualCard(refer_on_contextual_card) => {
            Some(refer_on_contextual_card.contextual_card_id)
        }
    }
}

fn process_path_and_compare<'a, T: Transaction<'a>>(
    subject_card_id: &VertexId,
    refer_type: &query::refer_link::ReferOnType,
    ref_vertex_id: &VertexId,
    txn: &T,
    is_not_equal: bool,
) -> bool {
    let path_opt = match refer_type {
        query::refer_link::ReferOnType::ReferOnCurrentCard(current_card) => {
            current_card.path.as_ref()
        }
        query::refer_link::ReferOnType::ReferOnParameterCard(param_ref) => param_ref.path.as_ref(),
        query::refer_link::ReferOnType::ReferOnMember(member) => member.path.as_ref(),
        query::refer_link::ReferOnType::ReferOnContextualCard(ctx_card) => ctx_card.path.as_ref(),
    };

    if let Some(path) = path_opt {
        if !path.nodes.is_empty() {
            let related_vertex_ids = get_related_vertex_ids(ref_vertex_id, path, txn, false);
            if is_not_equal {
                return related_vertex_ids.is_empty()
                    || !related_vertex_ids.contains(subject_card_id);
            } else {
                return related_vertex_ids.contains(subject_card_id);
            };
        };
    }

    if is_not_equal {
        subject_card_id != ref_vertex_id
    } else {
        subject_card_id == ref_vertex_id
    }
}

// ============================================================================
// 网址链接条件评估
// ============================================================================

fn evaluate_web_link_condition<'a, T: Transaction<'a>>(
    vertex: &Arc<Vertex>,
    item: &query::WebLinkConditionItem,
    txn: &T,
) -> bool {
    if let Some(subject) = &item.subject {
        if let Some(path) = &subject.path {
            if !path.nodes.is_empty() {
                let related_vertices = get_related_vertices(vertex, path, txn);

                if related_vertices.is_empty() {
                    if let Some(operator) = &item.operator {
                        if let Some(op_type) = &operator.operator_type {
                            return match op_type {
                                query::web_link_operator::OperatorType::IsNull(_) => true,
                                query::web_link_operator::OperatorType::NotContains(_) => true,
                                query::web_link_operator::OperatorType::IsNotNull(_) => false,
                                _ => false,
                            };
                        }
                    }
                    return false;
                }

                for related_vertex in &related_vertices {
                    if evaluate_web_link_on_vertex(
                        related_vertex,
                        subject.field_id.as_str(),
                        item.operator.as_ref(),
                    ) {
                        return true;
                    }
                }
                return false;
            }
        }

        evaluate_web_link_on_vertex(
            vertex,
            subject.field_id.as_str(),
            item.operator.as_ref(),
        )
    } else {
        warn!("Web link condition subject is empty");
        false
    }
}

fn evaluate_web_link_on_vertex(
    vertex: &Arc<Vertex>,
    field_id: &str,
    operator_opt: Option<&query::WebLinkOperator>,
) -> bool {
    if let Some(operator) = operator_opt {
        if let Some(op_type) = &operator.operator_type {
            if let Some(field_values) = &vertex.field_values {
                if let Some(field_value) = field_values.get(&Identifier::new(field_id)) {
                    if let FieldValue::WebLink(web_link_value) = field_value {
                        let href = &web_link_value.href;
                        let name = &web_link_value.name;

                        return match op_type {
                            query::web_link_operator::OperatorType::Equal(equal_op) => {
                                href == &equal_op.value || name == &equal_op.value
                            }
                            query::web_link_operator::OperatorType::Contains(contains_op) => {
                                href.contains(&contains_op.value)
                                    || name.contains(&contains_op.value)
                            }
                            query::web_link_operator::OperatorType::NotContains(
                                not_contains_op,
                            ) => {
                                !href.contains(&not_contains_op.value)
                                    && !name.contains(&not_contains_op.value)
                            }
                            query::web_link_operator::OperatorType::IsNull(_) => {
                                href.is_empty() && name.is_empty()
                            }
                            query::web_link_operator::OperatorType::IsNotNull(_) => {
                                !href.is_empty() || !name.is_empty()
                            }
                        };
                    } else {
                        warn!("Field is not a web link type, field ID: {}", field_id);
                    }
                }
            }

            return match op_type {
                query::web_link_operator::OperatorType::IsNull(_) => true,
                query::web_link_operator::OperatorType::Equal(_) => false,
                query::web_link_operator::OperatorType::Contains(_) => false,
                query::web_link_operator::OperatorType::NotContains(_) => true,
                query::web_link_operator::OperatorType::IsNotNull(_) => false,
            };
        } else {
            warn!("Web link condition operator type is empty");
            false
        }
    } else {
        warn!(
            "Web link condition operator is empty, field ID: {}",
            field_id
        );
        false
    }
}

// ============================================================================
// 关键词条件评估
// ============================================================================

fn evaluate_keyword_condition(
    vertex: &Arc<Vertex>,
    item: &query::KeywordConditionItem,
) -> bool {
    if item.value.is_empty() {
        warn!("Keyword condition value is empty");
        return false;
    }

    let keyword = &item.value;

    if vertex.code_in_org.contains(keyword) {
        return true;
    }

    if let Some(custom_code) = &vertex.custom_code {
        if custom_code.contains(keyword) {
            return true;
        }
    }

    match &vertex.title {
        VertexTitle::PureTitle(title) => {
            if pinyin_utils::is_pinyin_match(title, keyword) {
                return true;
            }
        }
        VertexTitle::JointTitle(joint_info) => {
            let mut full_title = String::new();
            match joint_info.area {
                TitleJointArea::Prefix => {
                    for parts in &joint_info.multi_parts {
                        for part in &parts.parts {
                            full_title.push_str(&part.name);
                        }
                    }
                    full_title.push_str(&joint_info.name);
                }
                TitleJointArea::Suffix => {
                    full_title.push_str(&joint_info.name);
                    for parts in &joint_info.multi_parts {
                        for part in &parts.parts {
                            full_title.push_str(&part.name);
                        }
                    }
                }
            }
            if pinyin_utils::is_pinyin_match(&full_title, keyword) {
                return true;
            }
        }
    }

    false
}

// ============================================================================
// 工具函数
// ============================================================================

const MILLIS_PER_DAY: i64 = 24 * 60 * 60 * 1000;
const DAY_END_OFFSET: i64 = MILLIS_PER_DAY - 1;

/// 现在我们只支持天级精度，并考虑东八区时区
///
/// - 如果 is_end_of_day=false, 返回当天开始时间 (00:00:00.000)
/// - 如果 is_end_of_day=true, 返回当天结束时间 (23:59:59.999)
fn normalize_timestamp(timestamp: i64, is_end_of_day: bool) -> i64 {
    const TIMEZONE_OFFSET_MS: i64 = 8 * 60 * 60 * 1000;

    let timezone_adjusted = timestamp + TIMEZONE_OFFSET_MS;
    let day_start = (timezone_adjusted / MILLIS_PER_DAY) * MILLIS_PER_DAY - TIMEZONE_OFFSET_MS;

    if is_end_of_day {
        day_start + DAY_END_OFFSET
    } else {
        day_start
    }
}
