#[cfg(test)]
mod tests {
    use crate::database::database::Database;
    use crate::database::model::{CardId, CardState, DateValue, Description, Edge, EdgeType, EnumValue, FieldValue, Identifier, NumberValue, StreamInfo, TextValue, Vertex, VertexTitle};
    use crate::database::transaction::Transaction;
    use crate::proto::pgraph::common::{Path, PathNode};
    use crate::proto::pgraph::query::{
        condition_node::NodeType, title_operator, text_operator, number_operator,
        date_operator, enum_operator, state_operator, link_operator, card_type_operator,
        CardQueryRequest, Condition, ConditionNode, ConditionGroup, LogicOperator,
        TitleConditionItem, TitleOperator, TitleContainsOperator, TitleEqualOperator,
        TextConditionItem, TextSubject, TextOperator, TextContainsOperator, TextEqualOperator,
        TextStartsWithOperator, TextEndsWithOperator,
        NumberConditionItem, NumberSubject, NumberOperator, NumberGreaterThanOperator,
        NumberBetweenOperator, NumberEqualOperator,
        DateConditionItem, DateSubject, DateOperator, DateAfterOperator, DateBetweenOperator,
        EnumConditionItem, EnumSubject, EnumOperator, EnumEqualOperator,
        StateConditionItem, StateSubject, StateOperator, StateEqualOperator, StateInOperator,
        CardTypeConditionItem, CardTypeSubject, CardTypeOperator, CardTypeEqualOperator, CardTypeInOperator,
        LinkConditionItem, LinkSubject, LinkOperator, LinkIsNullOperator, LinkIsNotNullOperator,
        LinkEqualOperator, SpecialLink,
        QueryContext, QueryScope,
    };
    use crate::proto::EnumStaticValues;
    use crate::server::query_cards;
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
        let mut txn = test_db.db.transaction();

        // 创建测试节点
        let mut vertex1 = create_test_vertex(
            1, "测试卡片1", "project", "container1", CardState::Active,
        );
        let mut field_values1 = HashMap::new();
        field_values1.insert(
            Identifier::new("priority"),
            FieldValue::Enum(EnumValue { items: vec![Identifier::new("high")] }),
        );
        field_values1.insert(
            Identifier::new("score"),
            FieldValue::Number(NumberValue { number: 85.5 }),
        );
        field_values1.insert(
            Identifier::new("date_field"),
            FieldValue::Date(DateValue { timestamp: 1630000000000 }),
        );
        field_values1.insert(
            Identifier::new("custom_text"),
            FieldValue::Text(TextValue { text: "测试文本值".to_string() }),
        );
        vertex1.field_values = Some(field_values1);

        let mut vertex2 = create_test_vertex(
            2,  "需求卡片", "requirement", "container1", CardState::Archived,
        );
        let mut field_values2 = HashMap::new();
        field_values2.insert(
            Identifier::new("priority"),
            FieldValue::Enum(EnumValue { items: vec![Identifier::new("medium")] }),
        );
        field_values2.insert(
            Identifier::new("score"),
            FieldValue::Number(NumberValue { number: 70.0 }),
        );
        field_values2.insert(
            Identifier::new("date_field"),
            FieldValue::Date(DateValue { timestamp: 1640000000000 }),
        );
        field_values2.insert(
            Identifier::new("custom_text"),
            FieldValue::Text(TextValue { text: "需求文本字段".to_string() }),
        );
        vertex2.field_values = Some(field_values2);

        let mut vertex3 = create_test_vertex(
            3, "任务卡片", "task", "container2", CardState::Active,
        );
        let mut field_values3 = HashMap::new();
        field_values3.insert(
            Identifier::new("priority"),
            FieldValue::Enum(EnumValue { items: vec![Identifier::new("low")] }),
        );
        field_values3.insert(
            Identifier::new("score"),
            FieldValue::Number(NumberValue { number: 50.0 }),
        );
        field_values3.insert(
            Identifier::new("date_field"),
            FieldValue::Date(DateValue { timestamp: 1650000000000 }),
        );
        field_values3.insert(
            Identifier::new("custom_text"),
            FieldValue::Text(TextValue { text: "任务描述".to_string() }),
        );
        vertex3.field_values = Some(field_values3);

        txn.create_vertex(&mut vertex1).unwrap();
        txn.create_vertex(&mut vertex2).unwrap();
        txn.create_vertex(&mut vertex3).unwrap();

        // 创建边关系用于测试Link条件
        let edge = Edge::new(
            vertex1.card_id,
            EdgeType::new("DEPENDS_ON"),
            vertex2.card_id,
            None
        );
        txn.create_edge(&edge).unwrap();

        txn.commit().unwrap();

        (test_db, vec![vertex1, vertex2, vertex3])
    }

    fn create_query_request(condition: Option<Condition>) -> CardQueryRequest {
        CardQueryRequest {
            query_context: Some(QueryContext {
                member_id: Some("member1".to_string()),
                org_id: Some("org1".to_string()),
                parameters: HashMap::new(),
                consistent_read: false,
            }),
            query_scope: Some(QueryScope {
                card_ids: vec![],
                card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
                container_ids: vec![],
                states: vec![],
            }),
            condition,
            r#yield: None,
            sort_and_page: None,
        }
    }

    // ==================== 标题条件测试 ====================

    #[test]
    fn test_title_contains() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Title(TitleConditionItem {
                    operator: Some(TitleOperator {
                        operator_type: Some(title_operator::OperatorType::Contains(
                            TitleContainsOperator { value: "需求".to_string() }
                        )),
                    }),
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        assert_eq!(response.cards.len(), 1);
        assert_eq!(response.cards[0].id, 2);
    }

    #[test]
    fn test_title_equals() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Title(TitleConditionItem {
                    operator: Some(TitleOperator {
                        operator_type: Some(title_operator::OperatorType::Equal(
                            TitleEqualOperator { value: "任务卡片".to_string() }
                        )),
                    }),
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        assert_eq!(response.cards.len(), 1);
        assert_eq!(response.cards[0].id, 3);
    }

    // ==================== 文本条件测试 ====================

    #[test]
    fn test_text_contains() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Text(TextConditionItem {
                    subject: Some(TextSubject {
                        path: None,
                        field_id: "custom_text".to_string(),
                        name: "自定义文本".to_string(),
                    }),
                    operator: Some(TextOperator {
                        operator_type: Some(text_operator::OperatorType::Contains(
                            TextContainsOperator { value: "需求".to_string() }
                        )),
                    }),
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        assert_eq!(response.cards.len(), 1);
        assert_eq!(response.cards[0].id, 2);
    }

    // ==================== 状态条件测试 ====================

    #[test]
    fn test_state_equals() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::State(StateConditionItem {
                    subject: None,
                    operator: Some(StateOperator {
                        operator_type: Some(state_operator::OperatorType::Equal(
                            StateEqualOperator { value: "ARCHIVED".to_string() }
                        )),
                    }),
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        assert_eq!(response.cards.len(), 1);
        assert_eq!(response.cards[0].id, 2);
    }

    #[test]
    fn test_state_in() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::State(StateConditionItem {
                    subject: None,
                    operator: Some(StateOperator {
                        operator_type: Some(state_operator::OperatorType::In(
                            StateInOperator { values: vec!["ACTIVE".to_string(), "ARCHIVED".to_string()] }
                        )),
                    }),
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        assert_eq!(response.cards.len(), 3);
    }

    // ==================== 卡片类型条件测试 ====================

    #[test]
    fn test_card_type_equals() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::CardType(CardTypeConditionItem {
                    subject: Some(CardTypeSubject { path: None }),
                    operator: Some(CardTypeOperator {
                        operator_type: Some(card_type_operator::OperatorType::Equal(
                            CardTypeEqualOperator { value: "project".to_string() }
                        )),
                    }),
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        assert_eq!(response.cards.len(), 1);
        assert_eq!(response.cards[0].id, 1);
    }

    // ==================== 数字条件测试 ====================

    #[test]
    fn test_number_greater_than() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Number(NumberConditionItem {
                    subject: Some(NumberSubject {
                        path: None,
                        field_id: "score".to_string(),
                        name: "分数".to_string(),
                    }),
                    operator: Some(NumberOperator {
                        operator_type: Some(number_operator::OperatorType::GreaterThan(
                            NumberGreaterThanOperator { value: 75.0 }
                        )),
                    }),
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        assert_eq!(response.cards.len(), 1);
        assert_eq!(response.cards[0].id, 1);
    }

    #[test]
    fn test_number_between() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Number(NumberConditionItem {
                    subject: Some(NumberSubject {
                        path: None,
                        field_id: "score".to_string(),
                        name: "分数".to_string(),
                    }),
                    operator: Some(NumberOperator {
                        operator_type: Some(number_operator::OperatorType::Between(
                            NumberBetweenOperator { min_value: 60.0, max_value: 80.0 }
                        )),
                    }),
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        assert_eq!(response.cards.len(), 1);
        assert_eq!(response.cards[0].id, 2);
    }

    // ==================== 枚举条件测试 ====================

    #[test]
    fn test_enum_equals() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::EnumItem(EnumConditionItem {
                    subject: Some(EnumSubject {
                        path: None,
                        field_id: "priority".to_string(),
                        name: "优先级".to_string(),
                    }),
                    operator: Some(EnumOperator {
                        operator_type: Some(enum_operator::OperatorType::Equal(
                            EnumEqualOperator {
                                value_type: Some(crate::proto::pgraph::query::enum_equal_operator::ValueType::StaticValues(
                                    EnumStaticValues { values: vec!["high".to_string()] }
                                )),
                            }
                        )),
                    }),
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        assert_eq!(response.cards.len(), 1);
        assert_eq!(response.cards[0].id, 1);
    }

    // ==================== 日期条件测试 ====================

    #[test]
    fn test_date_after() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Date(DateConditionItem {
                    subject: Some(DateSubject {
                        path: None,
                        field_id: "date_field".to_string(),
                        name: "日期字段".to_string(),
                    }),
                    operator: Some(DateOperator {
                        operator_type: Some(date_operator::OperatorType::After(
                            DateAfterOperator {
                                value_type: Some(crate::proto::pgraph::query::date_after_operator::ValueType::StaticValue(1645000000000)),
                            }
                        )),
                    }),
                    time_precision: "DAY".to_string(),
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        assert_eq!(response.cards.len(), 1);
        assert_eq!(response.cards[0].id, 3);
    }

    // ==================== 逻辑组合条件测试 ====================

    #[test]
    fn test_condition_and() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Group(ConditionGroup {
                    operator: LogicOperator::And as i32,
                    children: vec![
                        ConditionNode {
                            node_type: Some(NodeType::Title(TitleConditionItem {
                                operator: Some(TitleOperator {
                                    operator_type: Some(title_operator::OperatorType::Contains(
                                        TitleContainsOperator { value: "卡片".to_string() }
                                    )),
                                }),
                            })),
                        },
                        ConditionNode {
                            node_type: Some(NodeType::State(StateConditionItem {
                                subject: None,
                                operator: Some(StateOperator {
                                    operator_type: Some(state_operator::OperatorType::Equal(
                                        StateEqualOperator { value: "ACTIVE".to_string() }
                                    )),
                                }),
                            })),
                        },
                    ],
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        // 标题包含"卡片"且状态为活跃的：card1, card3
        assert_eq!(response.cards.len(), 2);
    }

    #[test]
    fn test_condition_or() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Group(ConditionGroup {
                    operator: LogicOperator::Or as i32,
                    children: vec![
                        ConditionNode {
                            node_type: Some(NodeType::CardType(CardTypeConditionItem {
                                subject: Some(CardTypeSubject { path: None }),
                                operator: Some(CardTypeOperator {
                                    operator_type: Some(card_type_operator::OperatorType::Equal(
                                        CardTypeEqualOperator { value: "project".to_string() }
                                    )),
                                }),
                            })),
                        },
                        ConditionNode {
                            node_type: Some(NodeType::CardType(CardTypeConditionItem {
                                subject: Some(CardTypeSubject { path: None }),
                                operator: Some(CardTypeOperator {
                                    operator_type: Some(card_type_operator::OperatorType::Equal(
                                        CardTypeEqualOperator { value: "task".to_string() }
                                    )),
                                }),
                            })),
                        },
                    ],
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        // 类型为project或task的：card1, card3
        assert_eq!(response.cards.len(), 2);
    }

    #[test]
    fn test_condition_nested() {
        let (test_db, _) = create_test_db();

        // 条件: (类型=project OR 类型=task) AND 状态=ACTIVE
        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Group(ConditionGroup {
                    operator: LogicOperator::And as i32,
                    children: vec![
                        ConditionNode {
                            node_type: Some(NodeType::Group(ConditionGroup {
                                operator: LogicOperator::Or as i32,
                                children: vec![
                                    ConditionNode {
                                        node_type: Some(NodeType::CardType(CardTypeConditionItem {
                                            subject: Some(CardTypeSubject { path: None }),
                                            operator: Some(CardTypeOperator {
                                                operator_type: Some(card_type_operator::OperatorType::Equal(
                                                    CardTypeEqualOperator { value: "project".to_string() }
                                                )),
                                            }),
                                        })),
                                    },
                                    ConditionNode {
                                        node_type: Some(NodeType::CardType(CardTypeConditionItem {
                                            subject: Some(CardTypeSubject { path: None }),
                                            operator: Some(CardTypeOperator {
                                                operator_type: Some(card_type_operator::OperatorType::Equal(
                                                    CardTypeEqualOperator { value: "task".to_string() }
                                                )),
                                            }),
                                        })),
                                    },
                                ],
                            })),
                        },
                        ConditionNode {
                            node_type: Some(NodeType::State(StateConditionItem {
                                subject: None,
                                operator: Some(StateOperator {
                                    operator_type: Some(state_operator::OperatorType::Equal(
                                        StateEqualOperator { value: "ACTIVE".to_string() }
                                    )),
                                }),
                            })),
                        },
                    ],
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        // (project OR task) AND ACTIVE = card1, card3
        assert_eq!(response.cards.len(), 2);
    }

    // ==================== 空条件测试 ====================

    #[test]
    fn test_empty_condition() {
        let (test_db, _) = create_test_db();

        let request = create_query_request(None);
        let response = query_cards(request, &test_db.db).unwrap();

        // 没有条件，返回所有卡片
        assert_eq!(response.cards.len(), 3);
    }

    // ==================== 关联条件测试 ====================

    #[test]
    fn test_link_is_not_null() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Link(LinkConditionItem {
                    subject: Some(LinkSubject {
                        path: Some(Path {
                            nodes: vec![PathNode {
                                lt_id: "DEPENDS_ON".to_string(),
                                position: "Src".to_string(),
                            }],
                        }),
                    }),
                    operator: Some(LinkOperator {
                        operator_type: Some(link_operator::OperatorType::IsNotNull(
                            LinkIsNotNullOperator {}
                        )),
                    }),
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        // card1 有一个 DEPENDS_ON 关联到 card2
        assert_eq!(response.cards.len(), 1);
        assert_eq!(response.cards[0].id, 1);
    }

    #[test]
    fn test_link_equal_special() {
        let (test_db, _) = create_test_db();

        let condition = Condition {
            root: Some(ConditionNode {
                node_type: Some(NodeType::Link(LinkConditionItem {
                    subject: Some(LinkSubject {
                        path: Some(Path {
                            nodes: vec![PathNode {
                                lt_id: "DEPENDS_ON".to_string(),
                                position: "Src".to_string(),
                            }],
                        }),
                    }),
                    operator: Some(LinkOperator {
                        operator_type: Some(link_operator::OperatorType::Equal(
                            LinkEqualOperator {
                                value: Some(crate::proto::pgraph::query::link_equal_operator::Value::SpecialLink(
                                    SpecialLink {
                                        card_ids: vec![2],
                                        contains_discard: false,
                                    }
                                )),
                            }
                        )),
                    }),
                })),
            }),
        };

        let request = create_query_request(Some(condition));
        let response = query_cards(request, &test_db.db).unwrap();

        assert_eq!(response.cards.len(), 1);
        assert_eq!(response.cards[0].id, 1);
    }
}
