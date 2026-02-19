use criterion::{criterion_group, criterion_main, Criterion};
use rand::Rng;
use std::collections::HashMap;
use std::fs;
use pgraph::database::database::Database;
use pgraph::database::model::*;
use pgraph::database::rdb::rdb::RocksDatabase;
use pgraph::database::rdb::rdb_config::RocksDbConfig;
use pgraph::database::transaction::Transaction;

#[derive(Clone, Copy)]
enum NodeType {
    Hot,    // 热点节点 5%
    Normal, // 普通节点 75%
    Cold,   // 冷节点 20%
}

fn get_node_type(id: VertexId) -> NodeType {
    match id % 100 {
        0..=4 => NodeType::Hot,     // 5%
        5..=79 => NodeType::Normal, // 75%
        _ => NodeType::Cold,        // 20%
    }
}

fn create_test_vertices(
    db: &RocksDatabase,
    total_vertices: u32,
) -> Result<(), Box<dyn std::error::Error>> {
    let mut txn = db.transaction();
    let mut count = 0;

    for i in 0..total_vertices {
        let mut vertex = new_vertex(i, format!("card_{}", i));
        txn.create_vertex(&mut vertex)?;
        count += 1;

        // 每1000个节点提交一次
        if count % 1_000 == 0 {
            txn.commit()?;
            println!("commit {} vertices done", count);
            txn = db.transaction();
        }
    }
    txn.commit()?;
    println!("commit all vertices done, total vertices: {}", count);
    Ok(())
}

fn create_test_edges(
    db: &RocksDatabase,
    total_vertices: u32,
) -> Result<(), Box<dyn std::error::Error>> {
    let mut rng = rand::thread_rng();
    let mut txn = db.transaction();
    let mut edge_count = 0;

    // 为每个节点生成边
    for src_id in 1..total_vertices {
        let node_type = get_node_type(src_id);

        // 根据节点类型生成不同数量的边类型
        let edge_types_count = match node_type {
            NodeType::Hot => rng.gen_range(20..30),
            NodeType::Normal => rng.gen_range(5..10),
            NodeType::Cold => rng.gen_range(2..5),
        };

        let edge_types: Vec<EdgeType> = (0..edge_types_count)
            .map(|_| EdgeType::new(format!("edge_type_{}", rng.gen_range(0..300))))
            .collect();

        // 为每种边类型生成邻居节点
        for edge_type in edge_types {
            // 根据节点类型生成不同数量的邻居
            let neighbors_count = match node_type {
                NodeType::Hot => rng.gen_range(1..3),
                NodeType::Normal => rng.gen_range(1..2),
                NodeType::Cold => 1,
            };

            for _ in 0..neighbors_count {
                let dest_id = rng.gen_range(1..total_vertices);

                // 只有0.1%的边包含属性
                let edge_props = if rng.gen_bool(0.001) {
                    Some(vec![
                        EdgeProp::Number(NumberProp {
                            field_id: FieldId::new("weight"),
                            number: rng.gen_range(0.0..1000.0),
                        }),
                        EdgeProp::Number(NumberProp {
                            field_id: FieldId::new("timestamp"),
                            number: 1_694_000_000_000.0 + (rng.gen_range(0..86400) as f64) * 1000.0,
                        }),
                        EdgeProp::Number(NumberProp {
                            field_id: FieldId::new("priority"),
                            number: rng.gen_range(1.0..5.0),
                        }),
                    ])
                } else {
                    None
                };

                let edge = Edge::new(src_id, edge_type.clone(), dest_id, edge_props);
                txn.create_edge(&edge)?;
                edge_count += 1;

                // 每1000个边提交一次
                if edge_count % 1_000 == 0 {
                    txn.commit()?;
                    println!("commit {} edges done", edge_count);
                    txn = db.transaction();
                }
            }
        }
    }
    txn.commit()?;
    println!("commit all edges done, total edges: {}", edge_count);
    Ok(())
}

fn setup_test_data(
    db: &RocksDatabase,
    total_vertices: u32,
) -> Result<(), Box<dyn std::error::Error>> {
    println!("开始创建测试数据...");
    create_test_vertices(db, total_vertices)?;
    create_test_edges(db, total_vertices)?;
    println!("测试数据创建完成");
    Ok(())
}

fn bench_create_edge(c: &mut Criterion) {
    fs::remove_dir_all("/tmp/rdb_test").ok();
    let db = RocksDatabase::new_db_with_config(RocksDbConfig::new("/tmp/rdb_test".to_owned()));
    let mut txn = db.transaction();

    let mut vertex1 = new_vertex(1, "card_1".to_string());
    let mut vertex2 = new_vertex(2, "card_2".to_string());
    txn.create_vertex(&mut vertex1).unwrap();
    txn.create_vertex(&mut vertex2).unwrap();
    txn.commit().unwrap();

    let edge = Edge::new(
        vertex1.id,
        EdgeType::new("default_edge_type".to_string()),
        vertex2.id,
        Some(vec![
            EdgeProp::Number(NumberProp {
                field_id: FieldId::new("weight"),
                number: 42.0,
            }),
            EdgeProp::Number(NumberProp {
                field_id: FieldId::new("timestamp"),
                number: 1_694_000_000_000.0,
            }),
            EdgeProp::Number(NumberProp {
                field_id: FieldId::new("priority"),
                number: 1.0,
            }),
        ]),
    );

    c.bench_function("create_edge", |b| {
        b.iter(|| {
            let mut txn = db.transaction();
            txn.create_edge(&edge).unwrap();
            txn.commit().unwrap();
        })
    });
}

fn bench_query_neighbor_vertex_ids(c: &mut Criterion) {
    //fs::remove_dir_all("/tmp/rdb_test").ok();
    let db = RocksDatabase::new_db_with_config(RocksDbConfig::new("/tmp/rdb_test".to_owned()));
    // 创建测试数据
    //setup_test_data(&db, 5_000_000).unwrap();

    // 从随机的节点查询其邻接节点
    c.bench_function("query_neighbor_vertex_ids", |b| {
        let mut rng = rand::thread_rng();
        b.iter(|| {
            let src_id = rng.gen_range(1..1_000_000);
            // 随机选择一个边类型进行查询（0-300）
            let edge_type = EdgeType::new(format!("edge_type_{}", rng.gen_range(0..300)));
            let query = NeighborQuery {
                src_vertex_ids: vec![src_id],
                edge_descriptor: EdgeDescriptor {
                    t: edge_type,
                    direction: EdgeDirection::Src,
                },
                dest_vertex_states: None,
            };
            let txn = db.transaction();
            txn.query_neighbor_vertex_ids(&query).unwrap();
        })
    });
}

criterion_group!(benches, bench_create_edge, bench_query_neighbor_vertex_ids);
criterion_main!(benches);

fn new_vertex(id: VertexId, card_id: CardId) -> Vertex {
    let mut rng = rand::thread_rng();
    let node_type = get_node_type(id);

    // 根据节点类型生成不同数量的属性
    let field_count = match node_type {
        NodeType::Hot => rng.gen_range(30..50),
        NodeType::Normal => rng.gen_range(5..10),
        NodeType::Cold => rng.gen_range(2..5),
    };

    let mut field_values = HashMap::new();
    for i in 0..field_count {
        let field_id = FieldId::new(format!("field_{}", i));
        match rng.gen_range(0..4) {
            0 => field_values.insert(
                field_id,
                FieldValue::Text(TextValue {
                    text: format!("text_value_{}", i),
                }),
            ),
            1 => field_values.insert(
                field_id,
                FieldValue::Number(NumberValue {
                    number: rng.gen_range(0.0..1000.0),
                }),
            ),
            2 => field_values.insert(
                field_id,
                FieldValue::Date(DateValue {
                    timestamp: 1_694_000_000_000 + rng.gen_range(0..86400) * 1000,
                }),
            ),
            _ => field_values.insert(
                field_id,
                FieldValue::Enum(EnumValue {
                    items: vec![EnumItemId::new(format!(
                        "enum_item_{}",
                        rng.gen_range(1..5)
                    ))],
                }),
            ),
        };
    }

    // 随机选择卡片类型（0-49）
    let card_type_id = CardTypeId::new(format!("card_type_{}", rng.gen_range(0..50)));
    // 随机选择容器ID（0-9）
    let container_id = ContainerId::new(format!("container_{}", rng.gen_range(0..10)));
    // 随机选择价值流类型（0-29）
    let stream_id = StreamId::new(format!("stream_{}", rng.gen_range(0..30)));

    // 判断是否是特殊价值流（少数价值流拥有更多阶段和状态）
    let is_special_stream = rng.gen_bool(0.1); // 10%的概率是特殊价值流
    let (step_count, status_count) = if is_special_stream {
        (6, 15) // 特殊价值流：6个阶段，15个状态
    } else {
        (3, 3) // 普通价值流：3个阶段，3个状态
    };

    // 随机选择阶段和状态
    let step_id = StepId::new(format!("step_{}", rng.gen_range(0..step_count)));
    let status_id = StatusId::new(format!("status_{}", rng.gen_range(0..status_count)));

    Vertex {
        id,
        card_id,
        org_id: Identifier::new("default_org_id".to_string()),
        card_type_id,
        container_id,
        stream_info: StreamInfo {
            stream_id,
            status_id,
        },
        state: CardState::Active,
        title: VertexTitle::PureTitle("default_name".to_string()),
        code_in_org: "1".to_string(),
        code_in_org_int: 1,
        custom_code: Some("default_code".to_string()),
        position: 1,
        created_at: 1_694_000_000_000,
        updated_at: 1_694_000_000_000,
        archived_at: Some(1_694_000_000_000),
        discarded_at: Some(1_694_000_000_000),
        discard_reason: Some("default_reason".to_string()),
        restore_reason: Some("default_reason".to_string()),
        field_values: Some(field_values),
        desc: Description {
            content: Some("default_description".to_string()),
            changed: false,
        }
    }
}
