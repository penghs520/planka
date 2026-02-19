#[cfg(test)]
mod tests {
    use crate::database::database::Database;
    use crate::database::model::{CardId, CardState, Description, EnumValue, FieldValue, Identifier, NumberValue, StreamInfo, TextValue, Vertex, VertexQuery, VertexTitle};
    use crate::database::transaction::Transaction;
    use crate::proto::pgraph::query::{
        condition_node::NodeType, CardIdQueryRequest, Condition, ConditionGroup, ConditionNode,
        LogicOperator, QueryScope, TextConditionItem, TextContainsOperator, TextOperator,
        TextSubject, CardTypeConditionItem, CardTypeEqualOperator, CardTypeOperator, CardTypeSubject,
    };
    use crate::server::query_card_ids;

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
    fn test_query_all_card_ids() {
        let (test_db, _) = create_test_db();

        // 创建一个简单的ID查询请求，查询所有卡片ID
        // 需要提供至少一个card_type_id
        let query_scope = QueryScope {
            card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
            card_ids: vec![],
            container_ids: vec![],
            states: vec![],
        };

        let query_request = CardIdQueryRequest {
            query_scope: Some(query_scope),
            condition: None,
            query_context: None,
        };

        // 执行ID查询
        let query_response = query_card_ids(query_request, &test_db.db).unwrap();

        // 验证结果 - 应该返回所有3个卡片ID
        assert_eq!(query_response.ids.len(), 3);
        assert!(query_response.ids.contains(&1u64));
        assert!(query_response.ids.contains(&2u64));
        assert!(query_response.ids.contains(&3u64));
    }

    #[test]
    fn test_query_card_ids_by_type() {
        let (test_db, _) = create_test_db();

        // 创建一个按类型查询的请求
        let query_scope = QueryScope {
            card_type_ids: vec!["project".to_string()],
            card_ids: vec![],
            container_ids: vec![],
            states: vec![],
        };

        let query_request = CardIdQueryRequest {
            query_scope: Some(query_scope),
            condition: None,
            query_context: None,
        };

        // 执行ID查询
        let query_response = query_card_ids(query_request, &test_db.db).unwrap();

        // 验证结果 - 应该只返回project类型的卡片ID
        assert_eq!(query_response.ids.len(), 1);
        assert!(query_response.ids.contains(&1u64));
    }

    #[test]
    fn test_query_card_ids_by_state() {
        let (test_db, _) = create_test_db();

        // 调试 - 列出所有节点状态
        let txn = test_db.db.transaction();
        let base_query = VertexQuery {
            card_ids: None,
            vertex_ids: None,
            card_type_ids: vec![Identifier::new("project"), Identifier::new("requirement"), Identifier::new("task")],
            container_ids: None,
            states: None,
        };
        let all_vertices = txn.query_vertices(base_query).unwrap();
        println!("调试 - 所有节点状态：");
        for v in &all_vertices {
            println!("卡片ID: {}, 状态: {:?}", v.card_id, v.state);
        }

        // 创建一个按状态查询的请求，只查询存档状态的卡片
        let query_scope = QueryScope {
            card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
            card_ids: vec![],
            container_ids: vec![],
            states: vec![2], // 2表示Archived状态
        };

        let query_request = CardIdQueryRequest {
            query_scope: Some(query_scope),
            condition: None,
            query_context: None,
        };

        // 输出query_request用于调试
        println!("查询请求: {:?}", query_request);

        // 执行ID查询
        let query_response = query_card_ids(query_request, &test_db.db).unwrap();

        // 输出结果进行调试
        println!("查询结果包含 {} 个ID:", query_response.ids.len());
        for id in &query_response.ids {
            println!("ID: {}", id);
        }

        // 找到存档状态的卡片进行验证
        let archived_cards = all_vertices.iter()
            .filter(|v| matches!(v.state, CardState::Archived))
            .collect::<Vec<_>>();

        println!("存档状态的卡片IDs:");
        for card in &archived_cards {
            println!("存档卡片: {}", card.card_id);
        }

        // 验证结果 - 应该只返回Archived状态的卡片ID
        assert_eq!(query_response.ids.len(), archived_cards.len());

        // 检查每个存档卡片是否在结果中
        for card in archived_cards {
            assert!(query_response.ids.contains(&card.card_id));
        }
    }

    #[test]
    fn test_query_card_ids_with_condition() {
        let (test_db, _) = create_test_db();

        // 创建一个带有文本条件的查询，查找custom_text字段包含"任务"的卡片
        let query_scope = QueryScope {
            card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
            card_ids: vec![],
            container_ids: vec![],
            states: vec![],
        };

        let condition = Condition {
            root: Some(ConditionNode {
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
            }),
        };

        let query_request = CardIdQueryRequest {
            query_scope: Some(query_scope),
            condition: Some(condition),
            query_context: None,
        };

        // 执行ID查询
        let query_response = query_card_ids(query_request, &test_db.db).unwrap();

        // 验证结果 - 应该只返回custom_text包含"任务"的卡片ID
        assert_eq!(query_response.ids.len(), 1);
        assert!(query_response.ids.contains(&3u64));
    }

    #[test]
    fn test_query_card_ids_with_or_condition() {
        let (test_db, _) = create_test_db();

        // 创建一个带有OR条件的查询，查找类型为project或requirement的卡片
        let query_scope = QueryScope {
            card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
            card_ids: vec![],
            container_ids: vec![],
            states: vec![],
        };

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

        let query_request = CardIdQueryRequest {
            query_scope: Some(query_scope),
            condition: Some(condition),
            query_context: None,
        };

        // 执行ID查询
        let query_response = query_card_ids(query_request, &test_db.db).unwrap();

        // 验证结果 - 应该返回project和requirement类型的卡片ID
        assert_eq!(query_response.ids.len(), 2);
        assert!(query_response.ids.contains(&1u64)); // project
        assert!(query_response.ids.contains(&2u64)); // requirement
    }
}
