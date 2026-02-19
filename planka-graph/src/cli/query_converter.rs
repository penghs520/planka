use crate::cli::sql_parser::{
    ParsedSqlQuery, SortDirection, SqlQueryType, WhereCondition, WhereOperator,
};
use crate::proto::pgraph::{
    common::{SortWay, CardState},
    query::{self, condition_node::NodeType, *},
    request::{request::RequestType, Request},
};
use anyhow::{anyhow, Result};
use std::collections::HashMap;
use uuid::Uuid;

/// 查询转换器，将解析的SQL转换为gRPC请求
pub struct QueryConverter;

impl QueryConverter {
    /// 将解析的SQL查询转换为CardQueryRequest
    pub fn convert_to_card_query(parsed_query: &ParsedSqlQuery) -> Result<CardQueryRequest> {
        let query_context = Some(QueryContext {
            org_id: None,
            member_id: None,
            parameters: HashMap::new(),
            consistent_read: false,
        });

        let query_scope = Self::build_query_scope(parsed_query)?;
        let condition = Self::build_condition(&parsed_query.where_conditions)?;
        let r#yield = Self::build_yield(&parsed_query.query_type)?;
        let sort_and_page = Self::build_sort_and_page(parsed_query)?;

        Ok(CardQueryRequest {
            query_context,
            query_scope: Some(query_scope),
            condition,
            r#yield: Some(r#yield),
            sort_and_page,
        })
    }

    /// 将解析的SQL查询转换为CardCountRequest
    pub fn convert_to_card_count(parsed_query: &ParsedSqlQuery) -> Result<CardCountRequest> {
        let query_context = Some(QueryContext {
            org_id: None,
            member_id: None,
            parameters: HashMap::new(),
            consistent_read: false,
        });

        let query_scope = Self::build_query_scope(parsed_query)?;
        let condition = Self::build_condition(&parsed_query.where_conditions)?;

        Ok(CardCountRequest {
            query_context,
            query_scope: Some(query_scope),
            condition,
        })
    }

    /// 构建查询范围
    fn build_query_scope(parsed_query: &ParsedSqlQuery) -> Result<QueryScope> {
        let mut query_scope = QueryScope {
            card_type_ids: vec![parsed_query.table_name.clone()],
            card_ids: vec![],
            container_ids: vec![],
            states: vec![],
        };

        for condition in &parsed_query.where_conditions {
            match condition.field.as_str() {
                "id" | "card_id" => match condition.operator {
                    WhereOperator::Equal => {
                        if let Some(value) = condition.values.first() {
                            match value.parse::<u64>() {
                                Ok(card_id) => query_scope.card_ids.push(card_id),
                                Err(_) => return Err(anyhow!("无效的card_id: {}", value)),
                            }
                        }
                    }
                    WhereOperator::In => {
                        for value in &condition.values {
                            match value.parse::<u64>() {
                                Ok(card_id) => query_scope.card_ids.push(card_id),
                                Err(_) => return Err(anyhow!("无效的card_id: {}", value)),
                            }
                        }
                    }
                    _ => {
                        return Err(anyhow!(
                            "id/card_id字段不支持{}操作符",
                            Self::operator_to_string(&condition.operator)
                        ));
                    }
                },
                "container_id" => match condition.operator {
                    WhereOperator::Equal => {
                        if let Some(value) = condition.values.first() {
                            query_scope.container_ids.push(value.clone());
                        }
                    }
                    WhereOperator::In => {
                        query_scope.container_ids.extend(condition.values.clone());
                    }
                    _ => {
                        return Err(anyhow!(
                            "container_id字段不支持{}操作符",
                            Self::operator_to_string(&condition.operator)
                        ));
                    }
                },
                "state" => {
                    let state_values = Self::parse_states(&condition.values)?;
                    match condition.operator {
                        WhereOperator::Equal => {
                            if let Some(state) = state_values.first() {
                                query_scope.states.push(*state);
                            }
                        }
                        WhereOperator::In => {
                            query_scope.states.extend(state_values);
                        }
                        _ => {
                            return Err(anyhow!(
                                "state字段不支持{}操作符",
                                Self::operator_to_string(&condition.operator)
                            ));
                        }
                    }
                }
                _ => {}
            }
        }

        Ok(query_scope)
    }

    /// 构建查询条件（递归嵌套结构）
    fn build_condition(where_conditions: &[WhereCondition]) -> Result<Option<Condition>> {
        let mut condition_nodes = vec![];

        for condition in where_conditions {
            // 跳过已经在query_scope中处理的字段
            match condition.field.as_str() {
                "id" | "card_id" | "container_id" | "state" => continue,
                _ => {}
            }

            let node = Self::build_condition_node(condition)?;
            condition_nodes.push(node);
        }

        if condition_nodes.is_empty() {
            Ok(None)
        } else if condition_nodes.len() == 1 {
            Ok(Some(Condition {
                root: Some(condition_nodes.remove(0)),
            }))
        } else {
            // 多个条件用 AND 组合
            Ok(Some(Condition {
                root: Some(ConditionNode {
                    node_type: Some(NodeType::Group(ConditionGroup {
                        operator: LogicOperator::And as i32,
                        children: condition_nodes,
                    })),
                }),
            }))
        }
    }

    /// 构建单个条件节点
    fn build_condition_node(condition: &WhereCondition) -> Result<ConditionNode> {
        let node_type = match condition.field.as_str() {
            "title" => {
                let operator = match condition.operator {
                    WhereOperator::Contains => {
                        if let Some(value) = condition.values.first() {
                            TitleOperator {
                                operator_type: Some(title_operator::OperatorType::Contains(
                                    TitleContainsOperator {
                                        value: value.clone(),
                                    },
                                )),
                            }
                        } else {
                            return Err(anyhow!("title CONTAINS操作符需要值"));
                        }
                    }
                    WhereOperator::Equal => {
                        if let Some(value) = condition.values.first() {
                            TitleOperator {
                                operator_type: Some(title_operator::OperatorType::Equal(
                                    TitleEqualOperator {
                                        value: value.clone(),
                                    },
                                )),
                            }
                        } else {
                            return Err(anyhow!("title =操作符需要值"));
                        }
                    }
                    WhereOperator::In => TitleOperator {
                        operator_type: Some(title_operator::OperatorType::In(TitleInOperator {
                            values: condition.values.clone(),
                        })),
                    },
                    WhereOperator::GreaterThan | WhereOperator::LessThan => {
                        return Err(anyhow!(
                            "title字段不支持{}操作符",
                            Self::operator_to_string(&condition.operator)
                        ));
                    }
                };

                NodeType::Title(TitleConditionItem {
                    operator: Some(operator),
                })
            }
            "code" => {
                if condition.operator != WhereOperator::Contains {
                    return Err(anyhow!("code字段只支持CONTAINS操作符"));
                }

                if let Some(value) = condition.values.first() {
                    NodeType::Code(CodeConditionItem {
                        value: value.clone(),
                    })
                } else {
                    return Err(anyhow!("code字段CONTAINS操作符需要值"));
                }
            }
            "desc" | "keyword" => {
                if condition.operator != WhereOperator::Contains {
                    return Err(anyhow!("desc字段只支持CONTAINS操作符"));
                }

                if let Some(value) = condition.values.first() {
                    NodeType::Keyword(KeywordConditionItem {
                        value: value.clone(),
                    })
                } else {
                    return Err(anyhow!("desc字段CONTAINS操作符需要值"));
                }
            }
            "status_id" => Self::build_status_condition_node(condition)?,
            "created" | "updated" | "archived_at" | "discarded_at" => {
                Self::build_date_condition_node(condition)?
            }
            "card_type_id" | "type_id" => Self::build_card_type_condition_node(condition)?,
            _ => {
                return Err(anyhow!("不支持的字段: {}", condition.field));
            }
        };

        Ok(ConditionNode {
            node_type: Some(node_type),
        })
    }

    /// 构建状态条件节点
    fn build_status_condition_node(condition: &WhereCondition) -> Result<NodeType> {
        let operator = match condition.operator {
            WhereOperator::Equal => {
                if let Some(value) = condition.values.first() {
                    StatusOperator {
                        operator_type: Some(status_operator::OperatorType::Equal(
                            StatusEqualOperator {
                                stream_id: String::new(),
                                status_id: value.clone(),
                            },
                        )),
                    }
                } else {
                    return Err(anyhow!("status =操作符需要值"));
                }
            }
            WhereOperator::In => {
                return Err(anyhow!("status_id字段暂不支持IN操作符，请使用=操作符"));
            }
            _ => {
                return Err(anyhow!(
                    "status字段不支持{}操作符",
                    Self::operator_to_string(&condition.operator)
                ));
            }
        };

        Ok(NodeType::Status(StatusConditionItem {
            subject: Some(StatusSubject { path: None }),
            operator: Some(operator),
            status_orders: vec![],
        }))
    }

    /// 构建卡片类型条件节点
    fn build_card_type_condition_node(condition: &WhereCondition) -> Result<NodeType> {
        let operator = match condition.operator {
            WhereOperator::Equal => {
                if let Some(value) = condition.values.first() {
                    CardTypeOperator {
                        operator_type: Some(card_type_operator::OperatorType::Equal(CardTypeEqualOperator {
                            value: value.clone(),
                        })),
                    }
                } else {
                    return Err(anyhow!("card_type_id =操作符需要值"));
                }
            }
            WhereOperator::In => CardTypeOperator {
                operator_type: Some(card_type_operator::OperatorType::In(CardTypeInOperator {
                    values: condition.values.clone(),
                })),
            },
            _ => {
                return Err(anyhow!(
                    "card_type_id字段不支持{}操作符",
                    Self::operator_to_string(&condition.operator)
                ));
            }
        };

        Ok(NodeType::CardType(CardTypeConditionItem {
            subject: Some(CardTypeSubject { path: None }),
            operator: Some(operator),
        }))
    }

    /// 构建日期条件节点
    fn build_date_condition_node(condition: &WhereCondition) -> Result<NodeType> {
        if condition.values.is_empty() {
            return Err(anyhow!("日期字段查询需要提供值"));
        }

        let date_value = &condition.values[0];
        let timestamp = Self::parse_date_string(date_value)?;

        let date_subject = DateSubject {
            path: None,
            field_id: Self::get_date_field_id(&condition.field),
            name: Self::get_date_field_name(&condition.field),
        };

        let date_operator = match condition.operator {
            WhereOperator::Equal => DateOperator {
                operator_type: Some(query::date_operator::OperatorType::Equal(
                    DateEqualOperator {
                        value_type: Some(query::date_equal_operator::ValueType::StaticValue(
                            timestamp,
                        )),
                    },
                )),
            },
            WhereOperator::GreaterThan => DateOperator {
                operator_type: Some(query::date_operator::OperatorType::After(DateAfterOperator {
                    value_type: Some(query::date_after_operator::ValueType::StaticValue(
                        timestamp,
                    )),
                })),
            },
            WhereOperator::LessThan => DateOperator {
                operator_type: Some(query::date_operator::OperatorType::Before(
                    DateBeforeOperator {
                        value_type: Some(query::date_before_operator::ValueType::StaticValue(
                            timestamp,
                        )),
                    },
                )),
            },
            _ => {
                return Err(anyhow!(
                    "日期字段不支持{}操作符",
                    Self::operator_to_string(&condition.operator)
                ));
            }
        };

        Ok(NodeType::Date(DateConditionItem {
            subject: Some(date_subject),
            operator: Some(date_operator),
            time_precision: "DAY".to_string(),
        }))
    }

    /// 构建返回定义
    fn build_yield(query_type: &SqlQueryType) -> Result<Yield> {
        let yielded_field = match query_type {
            SqlQueryType::Select { fields, .. } => {
                let contains_desc =
                    fields.contains(&"desc".to_string()) || fields.contains(&"*".to_string());
                let contains_all_custom_field = fields.contains(&"*".to_string());

                YieldedField {
                    custom_fields: if contains_all_custom_field {
                        vec![]
                    } else {
                        fields
                            .iter()
                            .filter(|f| !Self::is_builtin_field(f))
                            .cloned()
                            .collect()
                    },
                    contains_all_custom_field,
                    contains_desc,
                }
            }
            SqlQueryType::Count => YieldedField {
                custom_fields: vec![],
                contains_all_custom_field: false,
                contains_desc: false,
            },
        };

        Ok(Yield {
            yielded_field: Some(yielded_field),
            yielded_links: vec![],
        })
    }

    /// 判断是否是内置字段
    fn is_builtin_field(field: &str) -> bool {
        matches!(
            field,
            "id" | "card_id"
                | "org_id"
                | "card_type_id"
                | "type_id"
                | "container_id"
                | "state"
                | "created"
                | "updated"
                | "title"
                | "code"
                | "code_in_org"
                | "custom_code"
                | "desc"
                | "stream_id"
                | "status_id"
                | "archived_at"
                | "discarded_at"
                | "created_by"
                | "updated_by"
                | "*"
        )
    }

    /// 构建排序和分页
    fn build_sort_and_page(parsed_query: &ParsedSqlQuery) -> Result<Option<SortAndPage>> {
        let mut sorts = vec![];

        if let Some(order_by) = &parsed_query.order_by {
            let sort_field = Self::build_sort_field(&order_by.field)?;
            let sort_way = match order_by.direction {
                SortDirection::Asc => SortWay::Asc as i32,
                SortDirection::Desc => SortWay::Desc as i32,
            };

            sorts.push(Sort {
                sort_field: Some(sort_field),
                sort_way,
            });
        }

        let page = if let Some(limit) = parsed_query.limit {
            Some(Page {
                page_num: 1,
                page_size: limit,
            })
        } else {
            None
        };

        if sorts.is_empty() && page.is_none() {
            Ok(None)
        } else {
            Ok(Some(SortAndPage { page, sorts }))
        }
    }

    /// 构建排序字段
    fn build_sort_field(field_name: &str) -> Result<SortField> {
        let field_type = match field_name {
            "created" | "updated" | "archived_at" | "discarded_at" | "title" | "card_type_id"
            | "type_id" | "container_id" => sort_field::FieldType::InnerField(SortInnerField {
                field_id: field_name.to_string(),
            }),
            "code" | "custom_code" | "code_in_org" => {
                sort_field::FieldType::InnerField(SortInnerField {
                    field_id: "code".to_string(),
                })
            }
            _ => {
                return Err(anyhow!("不支持对字段{}进行排序", field_name));
            }
        };

        Ok(SortField {
            path: None,
            field_type: Some(field_type),
        })
    }

    /// 解析状态字符串为枚举值
    fn parse_states(state_strings: &[String]) -> Result<Vec<i32>> {
        let mut states = vec![];

        for state_str in state_strings {
            let state = match state_str.as_str() {
                "Active" | "ACTIVE" => CardState::Active as i32,
                "Archived" | "ARCHIVED" => CardState::Archived as i32,
                "Abandon" | "DISCARDED" => CardState::Discarded as i32,
                _ => {
                    return Err(anyhow!(
                        "无效的状态值: {}，支持的值: Active/ACTIVE, Archived/ARCHIVED, Abandon/DISCARDED",
                        state_str
                    ));
                }
            };
            states.push(state);
        }

        Ok(states)
    }

    /// 操作符转字符串（用于错误信息）
    fn operator_to_string(operator: &WhereOperator) -> &'static str {
        match operator {
            WhereOperator::Equal => "=",
            WhereOperator::In => "IN",
            WhereOperator::Contains => "CONTAINS",
            WhereOperator::GreaterThan => ">",
            WhereOperator::LessThan => "<",
        }
    }

    /// 创建完整的Request
    pub fn create_request(parsed_query: &ParsedSqlQuery) -> Result<Request> {
        let request_type = match &parsed_query.query_type {
            SqlQueryType::Count => {
                let card_count_request = Self::convert_to_card_count(parsed_query)?;
                RequestType::CardCount(card_count_request)
            }
            SqlQueryType::Select { .. } => {
                let card_query_request = Self::convert_to_card_query(parsed_query)?;
                RequestType::CardQuery(card_query_request)
            }
        };

        Ok(Request {
            request_id: Uuid::new_v4().to_string(),
            request_type: Some(request_type),
        })
    }

    /// 获取日期字段的ID
    fn get_date_field_id(field_name: &str) -> String {
        match field_name {
            "created" => "created".to_string(),
            "updated" => "updated".to_string(),
            "archived_at" => "archived_at".to_string(),
            "discarded_at" => "discarded_at".to_string(),
            _ => field_name.to_string(),
        }
    }

    /// 获取日期字段的显示名称
    fn get_date_field_name(field_name: &str) -> String {
        match field_name {
            "created" => "创建时间".to_string(),
            "updated" => "更新时间".to_string(),
            "archived_at" => "归档时间".to_string(),
            "discarded_at" => "放弃时间".to_string(),
            _ => field_name.to_string(),
        }
    }

    /// 解析日期字符串为时间戳
    fn parse_date_string(date_str: &str) -> Result<i64> {
        use chrono::{DateTime, NaiveDate, NaiveDateTime, Utc};

        let date_str = date_str.trim();

        if let Ok(naive_date) = NaiveDate::parse_from_str(date_str, "%Y-%m-%d") {
            let naive_datetime = naive_date
                .and_hms_opt(0, 0, 0)
                .ok_or_else(|| anyhow!("无法创建有效的日期时间"))?;
            let datetime_utc = DateTime::<Utc>::from_naive_utc_and_offset(naive_datetime, Utc);
            return Ok(datetime_utc.timestamp_millis());
        }

        if let Ok(naive_datetime) = NaiveDateTime::parse_from_str(date_str, "%Y-%m-%d %H:%M:%S") {
            let datetime_utc = DateTime::<Utc>::from_naive_utc_and_offset(naive_datetime, Utc);
            return Ok(datetime_utc.timestamp_millis());
        }

        if let Ok(naive_datetime) = NaiveDateTime::parse_from_str(date_str, "%Y-%m-%d %H:%M") {
            let datetime_utc = DateTime::<Utc>::from_naive_utc_and_offset(naive_datetime, Utc);
            return Ok(datetime_utc.timestamp_millis());
        }

        if let Ok(timestamp) = date_str.parse::<i64>() {
            if timestamp > 1_000_000_000_000 {
                return Ok(timestamp);
            } else if timestamp > 1_000_000_000 {
                return Ok(timestamp * 1000);
            }
        }

        Err(anyhow!(
            "不支持的日期格式: '{}'。支持的格式: YYYY-MM-DD, YYYY-MM-DD HH:MM:SS, YYYY-MM-DD HH:MM, 或时间戳",
            date_str
        ))
    }
}

// #[cfg(test)]
// mod tests {
//     use super::*;
//     use crate::cli::sql_parser::SqlParser;
//
//     #[test]
//     fn test_convert_simple_query() {
//         let sql = "SELECT * FROM `123456` WHERE id = 'test123'";
//         let parsed = SqlParser::parse(sql).unwrap();
//         let request = QueryConverter::create_request(&parsed).unwrap();
//
//         match request.request_type.unwrap() {
//             RequestType::CardQuery(card_query) => {
//                 assert!(card_query.query_scope.is_some());
//                 let scope = card_query.query_scope.unwrap();
//                 assert_eq!(scope.card_type_ids, vec!["123456"]);
//                 assert_eq!(scope.card_ids, vec!["test123"]);
//             }
//             _ => panic!("Expected CardQuery request type"),
//         }
//     }
//
//     #[test]
//     fn test_convert_count_query() {
//         let sql = "SELECT COUNT(*) FROM `123456`";
//         let parsed = SqlParser::parse(sql).unwrap();
//         let request = QueryConverter::create_request(&parsed).unwrap();
//
//         match request.request_type.unwrap() {
//             RequestType::CardCount(_) => {}
//             _ => panic!("Expected CardCount request type"),
//         }
//     }
//
//     #[test]
//     fn test_convert_with_state_condition() {
//         let sql = "SELECT * FROM `123456` WHERE state IN ('InProgress', 'Archived')";
//         let parsed = SqlParser::parse(sql).unwrap();
//         let request = QueryConverter::create_request(&parsed).unwrap();
//
//         match request.request_type.unwrap() {
//             RequestType::CardQuery(card_query) => {
//                 let scope = card_query.query_scope.unwrap();
//                 assert_eq!(scope.states.len(), 2);
//                 assert!(scope.states.contains(&(CardState::Active as i32)));
//                 assert!(scope.states.contains(&(CardState::Archived as i32)));
//             }
//             _ => panic!("Expected CardQuery request type"),
//         }
//     }
// }
