use criterion::{criterion_group, criterion_main, Criterion};
use std::fs;
use tokio::runtime::Runtime;
use pgraph::database::database::Database;
use pgraph::database::model::Identifier;
use pgraph::database::model::StatusId;
use pgraph::database::model::StepId;
use pgraph::database::model::StreamId;
use pgraph::database::model::{CardId, CardState, CardTypeId, ContainerId, Description, StreamInfo, Vertex, VertexQuery, VertexTitle};
use pgraph::database::rdb::rdb::RocksDatabase;
use pgraph::database::rdb::rdb_config::RocksDbConfig;
use pgraph::database::transaction::Transaction;

//基线：8us
fn create_vertex_bench(c: &mut Criterion) {
    // Ensure the runtime is created outside the benchmark loop
    let rt = Runtime::new().unwrap();

    // Clean up before starting the benchmark
    fs::remove_dir_all("/tmp/rdb_test").ok(); // Use .ok() to ignore errors if the directory doesn't exist

    let db = RocksDatabase::new_db_with_config(RocksDbConfig::new("/tmp/rdb_test".to_owned()));
    let mut i = 0;

    c.bench_function("create_vertex_bench", |b| {
        b.iter(|| {
            i += 1;
            let mut v = new_vertex(i.to_string());

            // Use the runtime to block on the async function
            rt.block_on(async {
                let mut txn = db.transaction();
                txn.create_vertex(&mut v).unwrap();
                txn.commit().unwrap();
            });
        });
    });
}

//基线：18~19us
fn get_vertex_bench(c: &mut Criterion) {
    // Ensure the runtime is created outside the benchmark loop
    let rt = Runtime::new().unwrap();

    // Clean up before starting the benchmark
    fs::remove_dir_all("/tmp/rdb_test").ok(); // Use .ok() to ignore errors if the directory doesn't exist
    let db = RocksDatabase::new_db_with_config(RocksDbConfig::new("/tmp/rdb_test".to_owned()));

    //创建200w节点
    let mut vids = Vec::with_capacity(200_0000);
    for i in 0..400_0000 {
        let mut v = new_vertex(i.to_string());
        rt.block_on(async {
            let mut txn = db.transaction();
            txn.create_vertex(&mut v).unwrap();
            vids.push(v.id);
            txn.commit().unwrap();
        });
    }
    c.bench_function("get_vertex_bench", |b| {
        b.iter(|| {
            rt.block_on(async {
                let vid = vids.pop().unwrap();
                let mut txn = db.transaction();
                txn.get_specific_vertices(&vec![vid]).unwrap()
            });
        });
    });
}

//使用 future cache：2us
//使用 sync cache:  [1.6909 µs 1.7212 µs 1.7556 µs]  [1.6687 µs 1.6783 µs 1.6908 µs]
fn get_vertex_hit_cache_bench(c: &mut Criterion) {
    // Ensure the runtime is created outside the benchmark loop
    let rt = Runtime::new().unwrap();

    // Clean up before starting the benchmark
    fs::remove_dir_all("/tmp/rdb_test").ok(); // Use .ok() to ignore errors if the directory doesn't exist
    let db = RocksDatabase::new_db_with_config(RocksDbConfig::new("/tmp/rdb_test".to_owned()));

    //创建200w节点
    for i in 0..200_0000 {
        let mut v = new_vertex(i.to_string());
        rt.block_on(async {
            let mut txn = db.transaction();
            txn.create_vertex(&mut v).unwrap();
            txn.commit().unwrap();
        });
    }
    c.bench_function("get_vertex_hit_cache_bench", |b| {
        b.iter(|| {
            rt.block_on(async {
                let mut txn = db.transaction();
                //永远查询id为100的节点，这样直接命中了缓存
                txn.get_specific_vertices(&vec![100]).unwrap()
            });
        });
    });
}

fn create_vertex_for_query(db: &RocksDatabase) {
    //创建500w节点，每5w个节点分配一个卡片类型和容器id
    //每10个节点中有6个节点是卡片状态为Archived，3个节点是卡片状态为InProgress，1个节点是卡片状态为Abandoned

    let mut card_type_id = CardTypeId::new("card_type_1".to_string());
    let mut container_id = ContainerId::new("container_1".to_string());
    let mut txn = db.transaction();
    for i in 0..5_000_000 {
        let mut v = new_vertex(format!("card_{}", i));
        if i % 50_000 == 0 {
            card_type_id = CardTypeId::new(format!("card_type_{}", i));
            container_id = ContainerId::new(format!("container_{}", i));
        }
        v.card_type_id = card_type_id;
        v.container_id = container_id;
        v.state = match i % 10 {
            0 => CardState::Discarded,
            1 | 2 | 3 => CardState::Active,
            _ => CardState::Archived,
        };
        txn.create_vertex(&mut v).unwrap();
        //每1000个节点提交一次
        if i % 1000 == 0 {
            txn.commit().unwrap();
            txn = db.transaction();
        }
    }
    txn.commit().unwrap();
}

//根据卡片类型查询的基准测试
fn get_vertex_by_card_type_bench(c: &mut Criterion) {
    fs::remove_dir_all("/tmp/rdb_test").ok();
    let db = RocksDatabase::new_db_with_config(RocksDbConfig::new("/tmp/rdb_test".to_owned()));
    create_vertex_for_query(&db);

    let rt = Runtime::new().unwrap();

    //开始基准测试
    c.bench_function("get_vertex_by_card_type_bench", |b| {
        b.iter(|| {
            rt.block_on(async {
                let txn = db.transaction();
                let query = VertexQuery {
                    card_ids: None,
                    vertex_ids: None,
                    card_type_ids: vec![CardTypeId::new("card_type_0".to_string())],
                    container_ids: None,
                    states: None,
                };
                txn.query_vertices(query).unwrap()
            });
        });
    });
}

//根据卡片类型 + 容器 + 卡片周期查询的基准测试
fn get_vertex_by_card_type_and_container_and_state_bench(c: &mut Criterion) {
    fs::remove_dir_all("/tmp/rdb_test").ok();
    let db = RocksDatabase::new_db_with_config(RocksDbConfig::new("/tmp/rdb_test".to_owned()));
    create_vertex_for_query(&db);

    let rt = Runtime::new().unwrap();

    c.bench_function("get_vertex_by_card_type_and_container_and_state_bench", |b| {
        b.iter(|| {
            rt.block_on(async {
                let txn = db.transaction();
                let query = VertexQuery {
                    card_ids: None,
                    vertex_ids: None,
                    card_type_ids: vec![CardTypeId::new("card_type_0".to_string())],
                    container_ids: Some(vec![ContainerId::new("container_0".to_string())]),
                    states: Some(vec![CardState::Active]),
                };
                txn.query_vertices(query).unwrap()
            });
        });
    });
}


fn new_vertex(card_id: CardId) -> Vertex {
    Vertex {
        card_id,
        org_id: Identifier::new("default_org_id".to_string()),
        card_type_id: CardTypeId::new("default_card_type_id".to_string()),
        container_id: ContainerId::new("default_container_id".to_string()),
        stream_info: StreamInfo {
            stream_id: StreamId::new("default_stream_id".to_string()),
            status_id: StatusId::new("default_status_id".to_string()),
        },
        state: CardState::Active,
        title: VertexTitle::PureTitle("".to_string()),
        code_in_org: "0".to_string(),
        code_in_org_int: 0,
        custom_code: None,
        position: 0,
        created_at: 0,
        updated_at: 0,
        archived_at: None,
        discarded_at: None,
        discard_reason: None,
        restore_reason: None,
        field_values: None,
        desc: Description {
            content: None,
            changed: false,
        }
    }
}

criterion_group!(
    vertex_bench,
    create_vertex_bench,
    get_vertex_bench,
    get_vertex_hit_cache_bench,
    get_vertex_by_card_type_bench,
    get_vertex_by_card_type_and_container_and_state_bench
);
criterion_main!(vertex_bench); // cargo bench --bench xxx
