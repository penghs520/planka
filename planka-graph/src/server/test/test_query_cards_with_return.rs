#[cfg(test)]
mod tests {
    use crate::database::model::{CardState, Description, Identifier, StreamInfo, Vertex, VertexTitle, Edge, EdgeType};
    use crate::database::test::test_utils::TestDb;
    use crate::database::transaction::Transaction;
    use crate::proto::{
        CardQueryRequest, QueryContext, QueryScope, Condition, SortAndPage, Page,
        Yield, YieldedField, YieldedLink,
        condition_node::NodeType, ConditionNode, ConditionGroup, LogicOperator,
        TitleConditionItem, TitleOperator, title_operator, TitleContainsOperator,
        StateConditionItem, StateOperator, state_operator, StateEqualOperator,
    };
    use crate::server::query_cards;
    use std::collections::HashMap;
    use crate::database::database::Database;

    // 辅助函数：创建测试节点
fn create_test_vertex(
    card_id: u64,
    title: &str,
    card_type_id: &str,
    container_id: &str,
    state: CardState
) -> Vertex {
    Vertex {
        card_id,
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
        desc: Description {
            content: Some("描述".to_string()),
            changed: true,
        },

    }
}

#[test]
fn test_basic_query() {
    let test_db = TestDb::new();

    // 创建测试数据
    let mut txn = test_db.db.transaction();

    // 创建测试节点
    let mut vertex1 = create_test_vertex(1, "测试卡片1", "project", "container1", CardState::Active);
    let mut vertex2 = create_test_vertex(2, "需求卡片", "requirement", "container1", CardState::Archived);
    let mut vertex3 = create_test_vertex(3, "任务卡片", "task", "container2", CardState::Active);

    // 添加到数据库
    txn.create_vertex(&mut vertex1).unwrap();
    txn.create_vertex(&mut vertex2).unwrap();
    txn.create_vertex(&mut vertex3).unwrap();

    txn.commit().unwrap();

    // 创建一个简单的查询请求
    let request = CardQueryRequest {
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
        condition: None,
        r#yield: None,
        sort_and_page: None,
    };

    // 执行查询
    let response = query_cards(request, &test_db.db).unwrap();

    // 验证结果
    assert_eq!(response.cards.len(), 3);
    assert_eq!(response.count, 3);
    assert_eq!(response.total, 3);
}

#[test]
fn test_title_condition() {
    let test_db = TestDb::new();

    // 创建测试数据
    let mut txn = test_db.db.transaction();

    // 创建测试节点
    let mut vertex1 = create_test_vertex(1, "测试卡片1", "project", "container1", CardState::Active);
    let mut vertex2 = create_test_vertex(2, "需求卡片", "requirement", "container1", CardState::Archived);
    let mut vertex3 = create_test_vertex(3, "任务卡片", "task", "container2", CardState::Active);

    // 添加到数据库
    txn.create_vertex(&mut vertex1).unwrap();
    txn.create_vertex(&mut vertex2).unwrap();
    txn.create_vertex(&mut vertex3).unwrap();

    txn.commit().unwrap();

    // 使用新的递归条件结构创建标题条件查询
    let title_condition = ConditionNode {
        node_type: Some(NodeType::Title(TitleConditionItem {
            operator: Some(TitleOperator {
                operator_type: Some(title_operator::OperatorType::Contains(TitleContainsOperator {
                    value: "需求".to_string(),
                })),
            }),
        })),
    };

    let request = CardQueryRequest {
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
        condition: Some(Condition {
            root: Some(title_condition),
        }),
        r#yield: None,
        sort_and_page: None,
    };

    // 执行查询
    let response = query_cards(request, &test_db.db).unwrap();

    // 期望只返回包含"需求"的卡片
    assert_eq!(response.cards.len(), 1);
}

#[test]
fn test_state_condition() {
    let test_db = TestDb::new();

    // 创建测试数据
    let mut txn = test_db.db.transaction();

    // 创建测试节点
    let mut vertex1 = create_test_vertex(1, "测试卡片1", "project", "container1", CardState::Active);
    let mut vertex2 = create_test_vertex(2, "需求卡片", "requirement", "container1", CardState::Archived);
    let mut vertex3 = create_test_vertex(3, "任务卡片", "task", "container2", CardState::Active);

    // 添加到数据库
    txn.create_vertex(&mut vertex1).unwrap();
    txn.create_vertex(&mut vertex2).unwrap();
    txn.create_vertex(&mut vertex3).unwrap();

    txn.commit().unwrap();

    // 使用新的递归条件结构创建状态条件查询
    let state_condition = ConditionNode {
        node_type: Some(NodeType::State(StateConditionItem {
            subject: None,
            operator: Some(StateOperator {
                operator_type: Some(state_operator::OperatorType::Equal(StateEqualOperator {
                    value: "ARCHIVED".to_string(),
                })),
            }),
        })),
    };

    let request = CardQueryRequest {
        query_context: None,
        query_scope: Some(QueryScope {
            card_ids: vec![],
            card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
            container_ids: vec![],
            states: vec![],
        }),
        condition: Some(Condition {
            root: Some(state_condition),
        }),
        r#yield: None,
        sort_and_page: None,
    };

    // 执行查询
    let response = query_cards(request, &test_db.db).unwrap();

    // 验证结果 - 应该只返回已归档的卡片
    assert_eq!(response.cards.len(), 1);
    assert_eq!(response.cards[0].id, 2);
}

#[test]
fn test_logic_condition() {
    let test_db = TestDb::new();

    // 创建测试数据
    let mut txn = test_db.db.transaction();

    // 创建测试节点
    let mut vertex1 = create_test_vertex(1, "测试卡片1", "project", "container1", CardState::Active);
    let mut vertex2 = create_test_vertex(2, "需求卡片", "requirement", "container1", CardState::Archived);
    let mut vertex3 = create_test_vertex(3, "任务卡片", "task", "container2", CardState::Active);

    // 添加到数据库
    txn.create_vertex(&mut vertex1).unwrap();
    txn.create_vertex(&mut vertex2).unwrap();
    txn.create_vertex(&mut vertex3).unwrap();

    txn.commit().unwrap();

    // 使用新的递归条件结构: 标题包含"任务" AND 状态为活跃
    let condition_group = ConditionNode {
        node_type: Some(NodeType::Group(ConditionGroup {
            operator: LogicOperator::And as i32,
            children: vec![
                ConditionNode {
                    node_type: Some(NodeType::Title(TitleConditionItem {
                        operator: Some(TitleOperator {
                            operator_type: Some(title_operator::OperatorType::Contains(TitleContainsOperator {
                                value: "任务".to_string(),
                            })),
                        }),
                    })),
                },
                ConditionNode {
                    node_type: Some(NodeType::State(StateConditionItem {
                        subject: None,
                        operator: Some(StateOperator {
                            operator_type: Some(state_operator::OperatorType::Equal(StateEqualOperator {
                                value: "ACTIVE".to_string(),
                            })),
                        }),
                    })),
                },
            ],
        })),
    };

    let request = CardQueryRequest {
        query_context: None,
        query_scope: Some(QueryScope {
            card_ids: vec![],
            card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
            container_ids: vec![],
            states: vec![],
        }),
        condition: Some(Condition {
            root: Some(condition_group),
        }),
        r#yield: None,
        sort_and_page: None,
    };

    // 执行查询
    let response = query_cards(request, &test_db.db).unwrap();

    // 验证结果 - 应该只返回task类型且活跃的卡片
    assert_eq!(response.cards.len(), 1);
    assert_eq!(response.cards[0].type_id, "task");
}

#[test]
fn test_pagination() {
    let test_db = TestDb::new();

    // 创建测试数据
    let mut txn = test_db.db.transaction();

    // 创建测试节点
    let mut vertex1 = create_test_vertex(1, "测试卡片1", "project", "container1", CardState::Active);
    let mut vertex2 = create_test_vertex(2, "需求卡片", "requirement", "container1", CardState::Archived);
    let mut vertex3 = create_test_vertex(3, "任务卡片", "task", "container2", CardState::Active);

    // 添加到数据库
    txn.create_vertex(&mut vertex1).unwrap();
    txn.create_vertex(&mut vertex2).unwrap();
    txn.create_vertex(&mut vertex3).unwrap();

    txn.commit().unwrap();

    // 创建带分页的查询
    let request = CardQueryRequest {
        query_context: None,
        query_scope: Some(QueryScope {
            card_ids: vec![],
            card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
            container_ids: vec![],
            states: vec![],
        }),
        condition: None,
        r#yield: None,
        sort_and_page: Some(SortAndPage {
            sorts: vec![],
            page: Some(Page {
                page_num: 1,
                page_size: 2,
            }),
        }),
    };

    // 执行查询
    let response = query_cards(request, &test_db.db).unwrap();

    // 验证结果 - 应该只返回前2条记录
    assert_eq!(response.cards.len(), 2);
    assert_eq!(response.count, 2);
    assert_eq!(response.total, 3);
}

#[test]
fn test_second_page() {
    let test_db = TestDb::new();

    // 创建测试数据
    let mut txn = test_db.db.transaction();

    // 创建测试节点
    let mut vertex1 = create_test_vertex(1, "测试卡片1", "project", "container1", CardState::Active);
    let mut vertex2 = create_test_vertex(2, "需求卡片", "requirement", "container1", CardState::Archived);
    let mut vertex3 = create_test_vertex(3, "任务卡片", "task", "container2", CardState::Active);

    // 添加到数据库
    txn.create_vertex(&mut vertex1).unwrap();
    txn.create_vertex(&mut vertex2).unwrap();
    txn.create_vertex(&mut vertex3).unwrap();

    txn.commit().unwrap();

    // 创建第二页的查询
    let request = CardQueryRequest {
        query_context: None,
        query_scope: Some(QueryScope {
            card_ids: vec![],
            card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
            container_ids: vec![],
            states: vec![],
        }),
        condition: None,
        r#yield: None,
        sort_and_page: Some(SortAndPage {
            sorts: vec![],
            page: Some(Page {
                page_num: 2,
                page_size: 2,
            }),
        }),
    };

    // 执行查询
    let response = query_cards(request, &test_db.db).unwrap();

    // 验证结果 - 应该只返回第3条记录
    assert_eq!(response.cards.len(), 1);
    assert_eq!(response.count, 1);
    assert_eq!(response.total, 3);
}

#[test]
fn test_query_by_card_ids() {
    let test_db = TestDb::new();

    // 创建测试数据
    let mut txn = test_db.db.transaction();

    // 创建测试节点
    let mut vertex1 = create_test_vertex(1, "测试卡片1", "project", "container1", CardState::Active);
    let mut vertex2 = create_test_vertex(2, "需求卡片", "requirement", "container1", CardState::Archived);
    let mut vertex3 = create_test_vertex(3, "任务卡片", "task", "container2", CardState::Active);

    // 添加到数据库
    txn.create_vertex(&mut vertex1).unwrap();
    txn.create_vertex(&mut vertex2).unwrap();
    txn.create_vertex(&mut vertex3).unwrap();

    txn.commit().unwrap();

    // 首先测试查询单个卡片ID
    let request1 = CardQueryRequest {
        query_context: None,
        query_scope: Some(QueryScope {
            card_ids: vec![1],
            card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
            container_ids: vec![],
            states: vec![],
        }),
        condition: None,
        r#yield: None,
        sort_and_page: None,
    };

    // 执行查询
    let response1 = query_cards(request1, &test_db.db).unwrap();

    // 验证结果 - 至少要包含指定卡片
    assert!(response1.cards.iter().any(|c| c.id == 1));

    // 然后测试另一张卡片
    let request2 = CardQueryRequest {
        query_context: None,
        query_scope: Some(QueryScope {
            card_ids: vec![3],
            card_type_ids: vec!["project".to_string(), "requirement".to_string(), "task".to_string()],
            container_ids: vec![],
            states: vec![],
        }),
        condition: None,
        r#yield: None,
        sort_and_page: None,
    };

    // 执行查询
    let response2 = query_cards(request2, &test_db.db).unwrap();

    // 验证结果
    assert!(response2.cards.iter().any(|c| c.id == 3));
}

#[test]
fn test_return_configuration() {
    let test_db = TestDb::new();

    // 创建测试数据
    let mut txn = test_db.db.transaction();

    // 1. 创建主卡片
    let mut main_card = create_test_vertex(1, "主卡片", "task", "container1", CardState::Active);

    // 添加主卡的自定义字段
    let mut main_card_field_values = HashMap::new();
    main_card_field_values.insert(
        Identifier::new("main_card_field1"),
        crate::database::model::FieldValue::Text(
            crate::database::model::TextValue {
                text: "主卡字段1的值".to_string()
            }
        )
    );
    main_card_field_values.insert(
        Identifier::new("main_card_field2"),
        crate::database::model::FieldValue::Number(
            crate::database::model::NumberValue {
                number: 123.45
            }
        )
    );
    main_card.field_values = Some(main_card_field_values);
    main_card.desc = Description {
        content: Some("这是主卡片的详细描述，需要通过Return配置来获取".to_string()),
        changed: true,
    };

    // 2. 创建一级关联卡片
    let mut level1_card1 = create_test_vertex(2, "关联卡片1", "requirement", "container1", CardState::Active);
    let mut level1_card1_field_values = HashMap::new();
    level1_card1_field_values.insert(
        Identifier::new("level1_card1_field3"),
        crate::database::model::FieldValue::Text(
            crate::database::model::TextValue {
                text: "关联卡片1的字段值".to_string()
            }
        )
    );
    level1_card1.field_values = Some(level1_card1_field_values);
    level1_card1.desc = Description {
        content: Some("这是一级关联卡片1的描述".to_string()),
        changed: true,
    };

    // 3. 创建二级关联卡片
    let mut level2_card1 = create_test_vertex(3, "二级关联卡片2", "project", "container1", CardState::Active);
    level2_card1.desc = Description {
        content: Some("这是二级关联卡片2的描述".to_string()),
        changed: true,
    };

    // 添加到数据库
    txn.create_vertex(&mut main_card).unwrap();
    txn.create_vertex(&mut level1_card1).unwrap();
    txn.create_vertex(&mut level2_card1).unwrap();

    // 创建边关系：主卡片 <- 关联卡片1
    let edge1 = Edge::new(
        main_card.card_id,
        EdgeType::new("MAIA_CARD_LINK_TO_LEVEL1_CARD"),
        level1_card1.card_id,
        None
    );

    // 创建边关系：一级关联卡片1 <- 二级关联卡片1
    let edge2 = Edge::new(
        level1_card1.card_id,
        EdgeType::new("LEVEL1_CARD_LINK_TO_LEVEL2_CARD"),
        level2_card1.card_id,
        None
    );

    // 添加边关系
    txn.create_edge(&edge1).unwrap();
    txn.create_edge(&edge2).unwrap();

    txn.commit().unwrap();

    // 创建具有复杂Yield结构的查询请求
    let r#yield = Yield {
        yielded_field: Some(YieldedField {
            custom_fields: vec!["main_card_field1".to_string(), "main_card_field2".to_string()],
            contains_all_custom_field: false,
            contains_desc: true,
        }),
        yielded_links: vec![
            YieldedLink {
                fields_on_link: HashMap::new(),
                contains_discard: false,
                path_node: Some(crate::proto::PathNode {
                    lt_id: "MAIA_CARD_LINK_TO_LEVEL1_CARD".to_string(),
                    position: "Src".to_string(),
                }),
                yielded_field: Some(YieldedField {
                    custom_fields: vec!["level1_card1_field3".to_string()],
                    contains_all_custom_field: false,
                    contains_desc: true,
                }),
                next_yielded_link: vec![],
            },
        ],
    };

    let request = CardQueryRequest {
        query_context: Some(QueryContext {
            member_id: Some("member1".to_string()),
            org_id: Some("org1".to_string()),
            parameters: HashMap::new(),
            consistent_read: false,
        }),
        query_scope: Some(QueryScope {
            card_ids: vec![1],
            card_type_ids: vec![],
            container_ids: vec![],
            states: vec![],
        }),
        condition: None,
        r#yield: Some(r#yield),
        sort_and_page: None,
    };

    // 执行查询
    let response = query_cards(request, &test_db.db).unwrap();

    // 验证结果
    assert_eq!(response.cards.len(), 1, "应该只返回一张主卡片");

    let main_card = &response.cards[0];

    // 验证主卡片的自定义字段是否正确填充
    assert!(main_card.custom_field_value_map.contains_key("main_card_field1"), "应该包含field1");
    assert!(main_card.custom_field_value_map.contains_key("main_card_field2"), "应该包含field2");

    // 验证描述是否正确填充
    assert_eq!(main_card.description, "这是主卡片的详细描述，需要通过Return配置来获取", "主卡片描述应该被正确填充");

    // 验证关联卡片 key 格式为 "lt_id:position"
    assert!(main_card.link_card_map.contains_key("MAIA_CARD_LINK_TO_LEVEL1_CARD:Src"), "应该包含MAIA_CARD_LINK_TO_LEVEL1_CARD:Src关联");

    // 获取一级关联卡片
    let linked_cards = &main_card.link_card_map["MAIA_CARD_LINK_TO_LEVEL1_CARD:Src"].cards;
    assert_eq!(linked_cards.len(), 1, "应该有1个一级关联卡片");

    let linked_card = &linked_cards[0];

    // 验证关联卡片的自定义字段是否正确填充
    assert!(linked_card.custom_field_value_map.contains_key("level1_card1_field3"), "一级关联卡片1应该包含level1_card1_field3");

    // 验证关联卡片的描述是否正确填充
    assert_eq!(linked_card.description, "这是一级关联卡片1的描述", "关联卡片描述应该被正确填充");
}

#[test]
fn test_return_configuration_minimalist() {
    let test_db = TestDb::new();

    // 创建测试数据
    let mut txn = test_db.db.transaction();

    // 创建主卡片，带有自定义字段值
    let mut main_card = create_test_vertex(1, "主卡片", "task", "container1", CardState::Active);

    // 添加自定义字段
    let mut field_values = HashMap::new();
    field_values.insert(
        Identifier::new("field1"),
        crate::database::model::FieldValue::Text(
            crate::database::model::TextValue {
                text: "字段1的值".to_string()
            }
        )
    );
    field_values.insert(
        Identifier::new("field2"),
        crate::database::model::FieldValue::Number(
            crate::database::model::NumberValue {
                number: 123.45
            }
        )
    );
    main_card.field_values = Some(field_values);
    main_card.desc = Description {
        content: Some("这是主卡片的详细描述，正常不应该被返回".to_string()),
        changed: true,
    };

    // 创建关联卡片
    let mut linked_card = create_test_vertex(2, "关联卡片", "requirement", "container1", CardState::Active);
    let mut field_values = HashMap::new();
    field_values.insert(
        Identifier::new("field3"),
        crate::database::model::FieldValue::Text(
            crate::database::model::TextValue {
                text: "关联卡片的字段值".to_string()
            }
        )
    );
    linked_card.field_values = Some(field_values);
    linked_card.desc = Description {
        content: Some("这是关联卡片的描述".to_string()),
        changed: true,
    };

    // 添加到数据库
    txn.create_vertex(&mut main_card).unwrap();
    txn.create_vertex(&mut linked_card).unwrap();

    // 创建边关系：主卡片 <- 关联卡片
    let edge = Edge::new(
        main_card.card_id,
        EdgeType::new("DEPENDS_ON"),
        linked_card.card_id,
        None
    );

    // 添加边关系
    txn.create_edge(&edge).unwrap();

    txn.commit().unwrap();

    // 测试场景1：空Yield结构
    let request1 = CardQueryRequest {
        query_context: None,
        query_scope: Some(QueryScope {
            card_ids: vec![1],
            card_type_ids: vec![],
            container_ids: vec![],
            states: vec![],
        }),
        condition: None,
        r#yield: None,
        sort_and_page: None,
    };

    // 执行查询
    let response1 = query_cards(request1, &test_db.db).unwrap();

    // 验证结果
    assert_eq!(response1.cards.len(), 1, "应该只返回一张主卡片");

    let main_card = &response1.cards[0];

    // 验证主卡片的属性是否正确填充 - 不应包含任何自定义字段值
    assert!(main_card.custom_field_value_map.is_empty(), "不应返回自定义字段");

    // 验证描述是否为空
    assert!(main_card.description.is_empty(), "不应返回描述信息");

    // 验证关联卡片是否为空
    assert!(main_card.link_card_map.is_empty(), "不应返回关联卡片");

    // 测试场景2：明确指定不返回描述和关联卡，只返回特定字段
    let request2 = CardQueryRequest {
        query_context: None,
        query_scope: Some(QueryScope {
            card_ids: vec![1],
            card_type_ids: vec![],
            container_ids: vec![],
            states: vec![],
        }),
        condition: None,
        r#yield: Some(Yield {
            yielded_field: Some(YieldedField {
                custom_fields: vec!["field1".to_string()],
                contains_all_custom_field: false,
                contains_desc: false,
            }),
            yielded_links: vec![],
        }),
        sort_and_page: None,
    };

    // 执行查询
    let response2 = query_cards(request2, &test_db.db).unwrap();

    // 验证结果
    assert_eq!(response2.cards.len(), 1, "应该只返回一张主卡片");

    let main_card = &response2.cards[0];

    // 验证主卡片的属性是否正确填充 - 只包含field1，不包含field2
    assert!(main_card.custom_field_value_map.contains_key("field1"), "应该包含field1");
    assert!(!main_card.custom_field_value_map.contains_key("field2"), "不应包含field2");

    // 验证描述是否为空
    assert!(main_card.description.is_empty(), "不应返回描述信息");

    // 验证关联卡片是否为空
    assert!(main_card.link_card_map.is_empty(), "不应返回关联卡片");
}
} // mod tests
