#[cfg(test)]
mod tests {
    use crate::database::database::Database;
    use crate::database::model::{CardId, CardState, Description, EnumValue, FieldValue, Identifier, NumberValue, StreamInfo, TextValue, Vertex, VertexTitle};
    use crate::database::transaction::Transaction;
    use crate::proto::pgraph::query::{
        condition_node::NodeType, Condition, ConditionGroup, ConditionNode, LogicOperator,
        QueryScope, TextConditionItem, TextContainsOperator, TextOperator, TextSubject,
        CardTypeConditionItem, CardTypeEqualOperator, CardTypeOperator, CardTypeSubject,
    };
    use crate::proto::CardCountRequest;
    use crate::server::count_cards;

    use crate::database::test::test_utils::TestDb;
    use std::collections::HashMap;

    // 辅助函数：创建测试节点
    fn create_test_vertex(
        card_id: CardId,
        title: &str,
        card_type_id: &str,
        container_id: &str,
        state: CardState,
    ) -> Vertex {
        Vertex {
            card_id: card_id,
            org_id: Identifier::new("org1"),
            card_type_id: Identifier::new(card_type_id),
            container_id: Identifier::new(container_id),
            stream_info: StreamInfo {
                stream_id: Identifier::new("stream1"),
                status_id: Identifier::new("status1"),
            },
            state,
            title: VertexTitle::PureTitle(title.to_string()),
            code_in_org: card_id.to_string(),
            code_in_org_int: card_id as u32,
            custom_code: None,
            position: 0,
            created_at: 1000,
            updated_at: 1100,
            archived_at: None,
            discarded_at: None,
            discard_reason: None,
            restore_reason: None,
            field_values: Some(HashMap::new()),
            desc: Description{
                content: Some("描述".to_string()),
                changed: true,
            }
        }
    }

    // 创建测试数据库和卡片
    fn create_test_db() -> (TestDb, Vec<Vertex>) {
        let test_db = TestDb::new();

        // 创建测试数据
        let mut txn = test_db.db.transaction();

        // 创建测试节点
        let mut vertex1 = create_test_vertex(
            1,
            "测试卡片1",
            "project",
            "container1",
            CardState::Active,
        );
        // 添加自定义字段到卡片1
        let mut field_values1 = HashMap::new();
        field_values1.insert(
            Identifier::new("priority"),
            FieldValue::Enum(EnumValue {
                items: vec![Identifier::new("high")],
            }),
        );
        field_values1.insert(
            Identifier::new("score"),
            FieldValue::Number(NumberValue { number: 85.5 }),
        );
        field_values1.insert(
            Identifier::new("custom_text"),
            FieldValue::Text(TextValue {
                text: "测试文本值".to_string(),
            }),
        );
        vertex1.field_values = Some(field_values1);

        let mut vertex2 = create_test_vertex(
            2,
            "需求卡片",
            "requirement",
            "container1",
            CardState::Archived,
        );
        // 添加自定义字段到卡片2
        let mut field_values2 = HashMap::new();
        field_values2.insert(
            Identifier::new("priority"),
            FieldValue::Enum(EnumValue {
                items: vec![Identifier::new("medium")],
            }),
        );
        field_values2.insert(
            Identifier::new("score"),
            FieldValue::Number(NumberValue { number: 70.0 }),
        );
        field_values2.insert(
            Identifier::new("custom_text"),
            FieldValue::Text(TextValue {
                text: "需求文本字段".to_string(),
            }),
        );
        vertex2.field_values = Some(field_values2);

        let mut vertex3 = create_test_vertex(
            3,
            "任务卡片",
            "task",
            "container2",
            CardState::Active,
        );
        // 添加自定义字段到卡片3
        let mut field_values3 = HashMap::new();
        field_values3.insert(
            Identifier::new("priority"),
            FieldValue::Enum(EnumValue {
                items: vec![Identifier::new("low")],
            }),
        );
        field_values3.insert(
            Identifier::new("score"),
            FieldValue::Number(NumberValue { number: 60.0 }),
        );
        field_values3.insert(
            Identifier::new("custom_text"),
            FieldValue::Text(TextValue {
                text: "任务文本字段".to_string(),
            }),
        );
        vertex3.field_values = Some(field_values3);

        // 添加到数据库
        txn.create_vertex(&mut vertex1).unwrap();
        txn.create_vertex(&mut vertex2).unwrap();
        txn.create_vertex(&mut vertex3).unwrap();

        txn.commit().unwrap();

        (test_db, vec![vertex1, vertex2, vertex3])
    }

    #[test]
    fn test_count_all_cards() {
        let (test_db, _) = create_test_db();

        // 创建一个简单的计数请求，查询所有卡片
        // 需要提供至少一个card_type_id
        let query_scope = QueryScope {
            card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
            card_ids: vec![],
            container_ids: vec![],
            states: vec![],
        };

        let count_request = CardCountRequest {
            query_scope: Some(query_scope),
            condition: None,
            query_context: None,
        };

        // 执行计数查询
        let count_response = count_cards(count_request, &test_db.db).unwrap();

        // 验证结果
        assert_eq!(count_response.count, 3);
    }

    #[test]
    fn test_count_cards_by_type() {
        let (test_db, _) = create_test_db();

        // 创建一个带有类型条件的计数请求，使用新的 ConditionNode 结构
        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::CardType(CardTypeConditionItem {
                    subject: Some(CardTypeSubject { path: None }),
                    operator: Some(CardTypeOperator {
                        operator_type: Some(
                            crate::proto::pgraph::query::card_type_operator::OperatorType::Equal(
                                CardTypeEqualOperator {
                                    value: "project".to_string(),
                                },
                            ),
                        ),
                    }),
                })),
            }),
        };

        // 需要提供至少一个card_type_id
        let query_scope = QueryScope {
            card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
            card_ids: vec![],
            container_ids: vec![],
            states: vec![],
        };

        let count_request = CardCountRequest {
            query_scope: Some(query_scope),
            condition: Some(condition),
            query_context: None,
        };

        // 执行计数查询
        let count_response = count_cards(count_request, &test_db.db).unwrap();

        // 验证结果
        assert_eq!(count_response.count, 1);
    }

    #[test]
    fn test_count_cards_by_query_scope() {
        let (test_db, _) = create_test_db();

        // 创建一个带有查询范围的计数请求
        let query_scope = QueryScope {
            card_type_ids: vec!["project".to_string(), "task".to_string()],
            card_ids: vec![],
            container_ids: vec![],
            states: vec![0], // 只计数活跃的卡片
        };

        let count_request = CardCountRequest {
            query_scope: Some(query_scope),
            condition: None,
            query_context: None,
        };

        // 执行计数查询
        let count_response = count_cards(count_request, &test_db.db).unwrap();

        // 验证结果 (project类型活跃的卡片1个 + task类型活跃的卡片1个)
        assert_eq!(count_response.count, 2);
    }

    #[test]
    fn test_count_cards_with_complex_condition() {
        let (test_db, _) = create_test_db();

        // 创建一个复杂的条件计数请求
        // 使用VUT条件 AND 文本字段条件
        let card_type_node = ConditionNode {
            node_type: Some(NodeType::CardType(CardTypeConditionItem {
                subject: Some(CardTypeSubject { path: None }),
                operator: Some(CardTypeOperator {
                    operator_type: Some(
                        crate::proto::pgraph::query::card_type_operator::OperatorType::Equal(
                            CardTypeEqualOperator {
                                value: "task".to_string(),
                            },
                        ),
                    ),
                }),
            })),
        };

        let text_node = ConditionNode {
            node_type: Some(NodeType::Text(TextConditionItem {
                subject: Some(TextSubject {
                    path: None,
                    field_id: "custom_text".to_string(),
                    name: "自定义文本".to_string(),
                }),
                operator: Some(TextOperator {
                    operator_type: Some(
                        crate::proto::pgraph::query::text_operator::OperatorType::Contains(
                            TextContainsOperator {
                                value: "任务".to_string(),
                            },
                        ),
                    ),
                }),
            })),
        };

        // 创建 AND 条件组
        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Group(ConditionGroup {
                    operator: LogicOperator::And as i32,
                    children: vec![card_type_node, text_node],
                })),
            }),
        };

        // 需要提供至少一个card_type_id
        let query_scope = QueryScope {
            card_type_ids: vec!["task".to_string()],
            card_ids: vec![],
            container_ids: vec![],
            states: vec![],
        };

        let count_request = CardCountRequest {
            query_scope: Some(query_scope),
            condition: Some(condition),
            query_context: None,
        };

        // 执行计数查询
        let count_response = count_cards(count_request, &test_db.db).unwrap();

        // 验证结果
        assert_eq!(count_response.count, 1);
    }

    #[test]
    fn test_count_cards_with_or_condition() {
        let (test_db, _) = create_test_db();

        // 创建一个带有OR条件组的计数请求
        let card_type_project_node = ConditionNode {
            node_type: Some(NodeType::CardType(CardTypeConditionItem {
                subject: Some(CardTypeSubject { path: None }),
                operator: Some(CardTypeOperator {
                    operator_type: Some(
                        crate::proto::pgraph::query::card_type_operator::OperatorType::Equal(
                            CardTypeEqualOperator {
                                value: "project".to_string(),
                            },
                        ),
                    ),
                }),
            })),
        };

        let card_type_requirement_node = ConditionNode {
            node_type: Some(NodeType::CardType(CardTypeConditionItem {
                subject: Some(CardTypeSubject { path: None }),
                operator: Some(CardTypeOperator {
                    operator_type: Some(
                        crate::proto::pgraph::query::card_type_operator::OperatorType::Equal(
                            CardTypeEqualOperator {
                                value: "requirement".to_string(),
                            },
                        ),
                    ),
                }),
            })),
        };

        // 创建OR逻辑组
        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Group(ConditionGroup {
                    operator: LogicOperator::Or as i32,
                    children: vec![card_type_project_node, card_type_requirement_node],
                })),
            }),
        };

        // 需要提供至少一个card_type_id
        let query_scope = QueryScope {
            card_type_ids: vec!["project".to_string(), "requirement".to_string()],
            card_ids: vec![],
            container_ids: vec![],
            states: vec![],
        };

        let count_request = CardCountRequest {
            query_scope: Some(query_scope),
            condition: Some(condition),
            query_context: None,
        };

        // 执行计数查询
        let count_response = count_cards(count_request, &test_db.db).unwrap();

        // 验证结果 (project类型1个 + requirement类型1个)
        assert_eq!(count_response.count, 2);
    }

    #[test]
    fn test_count_cards_with_empty_result() {
        let (test_db, _) = create_test_db();

        // 创建一个不匹配任何卡片的计数请求
        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::CardType(CardTypeConditionItem {
                    subject: Some(CardTypeSubject { path: None }),
                    operator: Some(CardTypeOperator {
                        operator_type: Some(
                            crate::proto::pgraph::query::card_type_operator::OperatorType::Equal(
                                CardTypeEqualOperator {
                                    value: "不存在的类型".to_string(),
                                },
                            ),
                        ),
                    }),
                })),
            }),
        };

        // 需要提供至少一个card_type_id或卡片ID
        let query_scope = QueryScope {
            card_type_ids: vec!["不存在的类型".to_string()],
            card_ids: vec![],
            container_ids: vec![],
            states: vec![],
        };

        let count_request = CardCountRequest {
            query_scope: Some(query_scope),
            condition: Some(condition),
            query_context: None,
        };

        // 执行计数查询
        let count_response = count_cards(count_request, &test_db.db).unwrap();

        // 验证结果
        assert_eq!(count_response.count, 0);
    }
}
