//
//
// #[cfg(test)]
// mod tests {
//     use std::collections::HashMap;
//     use crate::database::database::Database;
//     use crate::database::model::{CardId, CardState, Description, Edge, EdgeDescriptor, EdgeDirection, EdgeType, Identifier, NeighborQuery, StreamInfo, Vertex, VertexQuery, VertexTitle};
//     use crate::database::rdb::rdb::RocksDatabase;
//     use crate::database::rdb::rdb_config::RocksDbConfig;
//     use crate::database::transaction::Transaction;
//     use std::sync::atomic::{AtomicUsize, Ordering};
//     use std::sync::{Arc, Barrier};
//     use std::thread;
//     use std::time::Instant;
//
//     // 辅助函数：创建测试节点
//     fn create_test_vertex(
//         card_id: CardId,
//         title: &str,
//         card_type_id: &str,
//         container_id: &str,
//         state: CardState,
//     ) -> Vertex {
//         Vertex {
//             card_id: card_id,
//             org_id: Identifier::new("org1"),
//             card_type_id: Identifier::new(card_type_id),
//             container_id: Identifier::new(container_id),
//             stream_info: StreamInfo {
//                 stream_id: Identifier::new("stream1"),
//                 step_id: Identifier::new("step1"),
//                 status_id: Identifier::new("status1"),
//             },
//             state,
//             title: VertexTitle::PureTitle(title.to_string()),
//             code_in_org: card_id.to_string(),
//             code_in_org_int: card_id as u32,
//             custom_code: None,
//             position: 0,
//             created_at: 1000,
//             updated_at: 1100,
//             archived_at: None,
//             discarded_at: None,
//             discard_reason: None,
//             restore_reason: None,
//             field_values: Some(HashMap::new()),
//             desc: Description{
//                 content: Some("测试描述".to_string()),
//                 changed: true,
//             },
//             tags: None,
//             created_by: "test_user".to_string(),
//             updated_by: "test_user".to_string(),
//         }
//     }
//
//     /**
//      * 测试多线程并发创建卡片
//      *
//      * 测试场景：
//      * 1. 启动多个线程同时创建卡片
//      * 2. 每个线程创建一定数量的卡片
//      * 3. 验证所有卡片是否创建成功
//      *
//      * 预期结果：
//      * - 所有卡片都应该创建成功
//      * - 没有死锁或其他并发问题
//      */
//     #[test]
//     fn test_concurrent_card_creation() {
//         // 创建临时数据库
//         let db_path = format!("/tmp/pgraph_concurrent_test_{}", std::process::id());
//         let config = RocksDbConfig::new(db_path.clone());
//         let db = Arc::new(RocksDatabase::new_db_with_config(config));
//
//         let thread_count = 10;
//         let cards_per_thread = 100;
//         let total_cards = thread_count * cards_per_thread;
//
//         let counter = Arc::new(AtomicUsize::new(0));
//         let barrier = Arc::new(Barrier::new(thread_count));
//
//         let mut handles = vec![];
//
//         println!("开始测试并发创建卡片...");
//         let start_time = Instant::now();
//
//         for t in 0..thread_count {
//             let db_clone = Arc::clone(&db);
//             let counter_clone = Arc::clone(&counter);
//             let barrier_clone = Arc::clone(&barrier);
//
//             let handle = thread::spawn(move || {
//                 // 等待所有线程就绪
//                 barrier_clone.wait();
//
//                 let start_id = t * cards_per_thread;
//                 for i in 0..cards_per_thread {
//                     let card_id = format!("card_{}", start_id + i);
//                     let mut txn = db_clone.transaction();
//
//                     let mut vertex = create_test_vertex(
//                         0, // 由数据库自动分配ID
//                         &card_id,
//                         &format!("测试卡片 {}", start_id + i),
//                         "task",
//                         "container1",
//                         CardState::Active,
//                     );
//
//                     if let Ok(true) = txn.create_vertex(&mut vertex) {
//                         counter_clone.fetch_add(1, Ordering::SeqCst);
//                     }
//
//                     txn.commit().unwrap();
//                 }
//             });
//
//             handles.push(handle);
//         }
//
//         // 等待所有线程完成
//         for handle in handles {
//             handle.join().unwrap();
//         }
//
//         let elapsed = start_time.elapsed();
//         let created_cards = counter.load(Ordering::SeqCst);
//
//         println!("并发创建卡片测试完成");
//         println!("总耗时: {:?}", elapsed);
//         println!("成功创建卡片数: {}/{}", created_cards, total_cards);
//
//         // 验证结果
//         assert_eq!(created_cards, total_cards, "应该创建 {} 个卡片", total_cards);
//
//         {
//             // 查询验证 - 使用作用域确保txn在drop(db)前已经释放
//             let txn = db.transaction();
//             let query = VertexQuery {
//                 card_type_ids: vec![Identifier::new("task")],
//                 card_ids: None,
//                 container_ids: None,
//                 states: None,
//                 vertex_ids: None,
//             };
//
//             let vertices = txn.query_vertices(query).unwrap();
//             assert_eq!(vertices.len(), total_cards, "查询结果应包含所有创建的卡片");
//         }
//
//         // 清理数据库文件
//         drop(db);
//         std::fs::remove_dir_all(&db_path).ok();
//     }
//
//     /**
//      * 测试多线程并发修改卡片
//      *
//      * 测试场景：
//      * 1. 先创建若干卡片
//      * 2. 启动多个线程同时修改这些卡片
//      * 3. 验证修改是否成功且无冲突
//      *
//      * 预期结果：
//      * - 所有卡片都应该被成功修改
//      * - 没有数据不一致或丢失
//      */
//     #[test]
//     fn test_concurrent_card_update() {
//         // 创建临时数据库
//         let db_path = format!("/tmp/pgraph_concurrent_update_test_{}", std::process::id());
//         let config = RocksDbConfig::new(db_path.clone());
//         let db = Arc::new(RocksDatabase::new_db_with_config(config));
//
//         // 先创建测试卡片
//         let card_count = 50;
//         let mut card_ids = vec![];
//
//         {
//             let mut txn = db.transaction();
//             for i in 0..card_count {
//                 card_ids.push(i);
//
//                 let mut vertex = create_test_vertex(
//                     i,
//                     &format!("原始卡片 {}", i),
//                     "task",
//                     "container1",
//                     CardState::Active,
//                 );
//
//                 txn.create_vertex(&mut vertex).unwrap();
//             }
//             txn.commit().unwrap();
//         }
//
//         let thread_count = 10;
//         let updates_per_thread = 20;
//         let barrier = Arc::new(Barrier::new(thread_count));
//         let counter = Arc::new(AtomicUsize::new(0));
//
//         let mut handles = vec![];
//
//         println!("开始测试并发修改卡片...");
//         let start_time = Instant::now();
//
//         let card_ids_arc = Arc::new(card_ids);
//
//         for t in 0..thread_count {
//             let db_clone = Arc::clone(&db);
//             let counter_clone = Arc::clone(&counter);
//             let barrier_clone = Arc::clone(&barrier);
//             let card_ids_clone = Arc::clone(&card_ids_arc);
//
//             let handle = thread::spawn(move || {
//                 // 等待所有线程就绪
//                 barrier_clone.wait();
//
//                 for i in 0..updates_per_thread {
//                     // 每个线程随机选择卡片进行修改
//                     let index = (t * i) % card_count;
//                     let card_id = &card_ids_clone[index];
//
//                     let mut txn = db_clone.transaction();
//
//                     // 先通过卡片ID查询获取节点ID
//                     if let Some(vertex_id) = txn.get_vertex_id_by_card_id(card_id) {
//                         // 获取完整的节点信息
//                         if let Ok(vertices) = txn.get_specific_vertices(&vec![vertex_id]) {
//                             if let Some(vertex_arc) = vertices.first() {
//                                 // 创建一个新的节点进行修改，而不是克隆
//                                 let mut updated_vertex = create_test_vertex(
//                                     vertex_id,
//                                     card_id,
//                                     &format!("修改后卡片 {} by thread {}", index, t),
//                                     &vertex_arc.card_type_id.0,
//                                     &vertex_arc.container_id.0,
//                                     CardState::Archived // 修改状态
//                                 );
//
//                                 // 保留原始节点的一些字段
//                                 updated_vertex.org_id = vertex_arc.org_id.clone();
//                                 // 创建新的StreamInfo而不是调用clone
//                                 updated_vertex.stream_info = StreamInfo {
//                                     stream_id: vertex_arc.stream_info.stream_id.clone(),
//                                     step_id: vertex_arc.stream_info.step_id.clone(),
//                                     status_id: vertex_arc.stream_info.status_id.clone(),
//                                 };
//                                 updated_vertex.code_in_org = vertex_arc.code_in_org.clone();
//                                 updated_vertex.custom_code = vertex_arc.custom_code.clone();
//                                 updated_vertex.position = vertex_arc.position;
//                                 updated_vertex.created_at = vertex_arc.created_at;
//                                 updated_vertex.updated_at = vertex_arc.updated_at;
//                                 updated_vertex.field_values = vertex_arc.field_values.clone();
//
//                                 if let Ok(true) = txn.update_vertex(&updated_vertex) {
//                                     counter_clone.fetch_add(1, Ordering::SeqCst);
//                                 }
//
//                                 txn.commit().unwrap();
//                             }
//                         }
//                     }
//                 }
//             });
//
//             handles.push(handle);
//         }
//
//         // 等待所有线程完成
//         for handle in handles {
//             handle.join().unwrap();
//         }
//
//         let elapsed = start_time.elapsed();
//         let updated_cards = counter.load(Ordering::SeqCst);
//
//         println!("并发修改卡片测试完成");
//         println!("总耗时: {:?}", elapsed);
//         println!("成功修改卡片次数: {}", updated_cards);
//
//         // 验证结果
//         assert!(updated_cards > 0, "应该至少修改了一些卡片");
//
//         {
//             // 查询验证 - 使用作用域确保txn在drop(db)前已经释放
//             let txn = db.transaction();
//             let query = VertexQuery {
//                 card_type_ids: vec![Identifier::new("task")],
//                 card_ids: None,
//                 container_ids: None,
//                 states: Some(vec![CardState::Archived]),
//                 vertex_ids: None,
//             };
//
//             let vertices = txn.query_vertices(query).unwrap();
//             assert!(vertices.len() > 0, "应该至少有一些卡片被修改为归档状态");
//         }
//
//         // 清理数据库文件
//         drop(db);
//         std::fs::remove_dir_all(&db_path).ok();
//     }
//
//     /**
//      * 测试多线程并发创建边关系
//      *
//      * 测试场景：
//      * 1. 先创建一定数量的卡片节点
//      * 2. 启动多个线程同时创建卡片之间的关联关系
//      * 3. 验证关联关系是否创建成功
//      *
//      * 预期结果：
//      * - 所有关联关系都应该创建成功
//      * - 没有死锁或数据不一致问题
//      */
//     #[test]
//     fn test_concurrent_edge_creation() {
//         // 创建临时数据库
//         let db_path = format!("/tmp/pgraph_concurrent_edge_test_{}", std::process::id());
//         let config = RocksDbConfig::new(db_path.clone());
//         let db = Arc::new(RocksDatabase::new_db_with_config(config));
//
//         // 创建测试节点
//         let node_count = 100;
//         let mut node_ids = vec![];
//
//         {
//             let mut txn = db.transaction();
//             for i in 0..node_count {
//                 let card_id = format!("edge_node_{}", i);
//
//                 let mut vertex = create_test_vertex(
//                     0,
//                     &card_id,
//                     &format!("节点 {}", i),
//                     "task",
//                     "container1",
//                     CardState::Active,
//                 );
//
//                 txn.create_vertex(&mut vertex).unwrap();
//                 node_ids.push(vertex.id);
//             }
//             txn.commit().unwrap();
//         }
//
//         let thread_count = 10;
//         let edges_per_thread = 100;
//         let total_edges = thread_count * edges_per_thread;
//
//         let counter = Arc::new(AtomicUsize::new(0));
//         let barrier = Arc::new(Barrier::new(thread_count));
//
//         let mut handles = vec![];
//
//         println!("开始测试并发创建边关系...");
//         let start_time = Instant::now();
//
//         let node_ids_arc = Arc::new(node_ids);
//
//         for t in 0..thread_count {
//             let db_clone = Arc::clone(&db);
//             let counter_clone = Arc::clone(&counter);
//             let barrier_clone = Arc::clone(&barrier);
//             let node_ids_clone = Arc::clone(&node_ids_arc);
//
//             let handle = thread::spawn(move || {
//                 // 等待所有线程就绪
//                 barrier_clone.wait();
//
//                 for i in 0..edges_per_thread {
//                     // 选择源节点和目标节点
//                     let src_index = (t * i) % node_count;
//                     let dest_index = (src_index + 1) % node_count;
//
//                     let src_id = node_ids_clone[src_index];
//                     let dest_id = node_ids_clone[dest_index];
//
//                     let mut txn = db_clone.transaction();
//
//                     // 创建边
//                     let edge = Edge::new(
//                         src_id,
//                         EdgeType::new(&format!("DEPENDS_ON_{}", i % 5)), // 5种不同的边类型
//                         dest_id,
//                         None,
//                     );
//
//                     if let Ok(true) = txn.create_edge(&edge) {
//                         counter_clone.fetch_add(1, Ordering::SeqCst);
//                     }
//
//                     txn.commit().unwrap();
//                 }
//             });
//
//             handles.push(handle);
//         }
//
//         // 等待所有线程完成
//         for handle in handles {
//             handle.join().unwrap();
//         }
//
//         let elapsed = start_time.elapsed();
//         let created_edges = counter.load(Ordering::SeqCst);
//
//         println!("并发创建边关系测试完成");
//         println!("总耗时: {:?}", elapsed);
//         println!("成功创建边关系数: {}/{}", created_edges, total_edges);
//
//         // 验证结果 - 由于可能有重复边创建尝试，实际创建数可能少于尝试数
//         assert!(created_edges > 0, "应该至少创建了一些边关系");
//
//         // 清理数据库文件
//         drop(db);
//         std::fs::remove_dir_all(&db_path).ok();
//     }
//
//     /**
//      * 测试多线程并发查询
//      *
//      * 测试场景：
//      * 1. 先创建测试数据（节点和边）
//      * 2. 启动多个线程同时进行不同类型的查询
//      * 3. 验证查询结果的正确性和性能
//      *
//      * 预期结果：
//      * - 所有查询都应该正确返回结果
//      * - 没有死锁或性能严重下降的情况
//      */
//     #[test]
//     fn test_concurrent_queries() {
//         // 创建临时数据库
//         let db_path = format!("/tmp/pgraph_concurrent_query_test_{}", std::process::id());
//         let config = RocksDbConfig::new(db_path.clone());
//         let db = Arc::new(RocksDatabase::new_db_with_config(config));
//
//         // 创建测试数据
//         let node_count = 100;
//         let edge_types = vec!["DEPENDS_ON", "BLOCKS", "RELATES_TO"];
//
//         {
//             let mut txn = db.transaction();
//
//             // 创建节点
//             for i in 0..node_count {
//                 let card_type_id = match i % 3 {
//                     0 => "task",
//                     1 => "project",
//                     _ => "requirement",
//                 };
//
//                 let container_id = match i % 2 {
//                     0 => "container1",
//                     _ => "container2",
//                 };
//
//                 let state = match i % 4 {
//                     0 => CardState::Active,
//                     1 => CardState::Discarded,
//                     2 => CardState::Archived,
//                     _ => CardState::Archived,
//                 };
//
//                 let mut vertex = create_test_vertex(
//                     0,
//                     &format!("query_node_{}", i),
//                     &format!("查询节点 {}", i),
//                     card_type_id,
//                     container_id,
//                     state,
//                 );
//
//                 txn.create_vertex(&mut vertex).unwrap();
//
//                 // 创建与前面节点的边关系
//                 if i > 0 {
//                     for j in 0..3 {
//                         if i % (j + 2) == 0 {
//                             let prev_id = (i - 1) as u32; // 修复类型问题
//                             let edge = Edge::new(
//                                 vertex.id,
//                                 EdgeType::new(edge_types[j % edge_types.len()]),
//                                 prev_id,
//                                 None,
//                             );
//                             txn.create_edge(&edge).unwrap();
//                         }
//                     }
//                 }
//             }
//
//             txn.commit().unwrap();
//         }
//
//         let thread_count = 20;
//         let queries_per_thread = 50;
//
//         let barrier = Arc::new(Barrier::new(thread_count));
//         let counter = Arc::new(AtomicUsize::new(0));
//
//         let mut handles = vec![];
//
//         println!("开始测试并发查询...");
//         let start_time = Instant::now();
//
//         // 为每个线程创建一个edge_types的副本，避免所有权问题
//         for t in 0..thread_count {
//             let db_clone = Arc::clone(&db);
//             let counter_clone = Arc::clone(&counter);
//             let barrier_clone = Arc::clone(&barrier);
//             // 为每个线程克隆一份edge_types
//             let edge_types_clone = edge_types.clone();
//
//             let handle = thread::spawn(move || {
//                 // 等待所有线程就绪
//                 barrier_clone.wait();
//
//                 let mut successful_queries = 0;
//
//                 for i in 0..queries_per_thread {
//                     let query_type = (t + i) % 4; // 4种不同类型的查询
//
//                     let txn = db_clone.transaction();
//                     let mut query_success = false;
//
//                     match query_type {
//                         0 => {
//                             // 查询特定类型的节点
//                             let card_type_id = match i % 3 {
//                                 0 => "task",
//                                 1 => "project",
//                                 _ => "requirement",
//                             };
//
//                             let query = VertexQuery {
//                                 card_type_ids: vec![Identifier::new(card_type_id)],
//                                 card_ids: None,
//                                 container_ids: None,
//                                 states: None,
//                                 vertex_ids: None,
//                             };
//
//                             if let Ok(vertices) = txn.query_vertices(query) {
//                                 if !vertices.is_empty() {
//                                     query_success = true;
//                                 }
//                             }
//                         },
//                         1 => {
//                             // 查询特定状态的节点
//                             let state = if i % 2 == 0 {
//                                 CardState::Active
//                             } else {
//                                 CardState::Archived
//                             };
//
//                             let query = VertexQuery {
//                                 card_type_ids: vec![
//                                     Identifier::new("task"),
//                                     Identifier::new("project"),
//                                     Identifier::new("requirement"),
//                                 ],
//                                 card_ids: None,
//                                 container_ids: None,
//                                 states: Some(vec![state]),
//                                 vertex_ids: None,
//                             };
//
//                             if let Ok(vertices) = txn.query_vertices(query) {
//                                 if !vertices.is_empty() {
//                                     query_success = true;
//                                 }
//                             }
//                         },
//                         2 => {
//                             // 查询特定容器的节点
//                             let container_id = if i % 2 == 0 {
//                                 "container1"
//                             } else {
//                                 "container2"
//                             };
//
//                             let query = VertexQuery {
//                                 card_type_ids: vec![
//                                     Identifier::new("task"),
//                                     Identifier::new("project"),
//                                     Identifier::new("requirement"),
//                                 ],
//                                 card_ids: None,
//                                 container_ids: Some(vec![Identifier::new(container_id)]),
//                                 states: None,
//                                 vertex_ids: None,
//                             };
//
//                             if let Ok(vertices) = txn.query_vertices(query) {
//                                 if !vertices.is_empty() {
//                                     query_success = true;
//                                 }
//                             }
//                         },
//                         3 => {
//                             // 查询节点的关联关系
//                             if let Ok(vertices) = txn.query_vertices(VertexQuery {
//                                 card_type_ids: vec![Identifier::new("task")],
//                                 card_ids: None,
//                                 container_ids: None,
//                                 states: None,
//                                 vertex_ids: None,
//                             }) {
//                                 if !vertices.is_empty() && vertices.len() > 1 {
//                                     let vertex_id = vertices[i % vertices.len()].id;
//
//                                     let edge_type = edge_types_clone[i % edge_types_clone.len()];
//                                     let edge_descriptor = EdgeDescriptor {
//                                         t: EdgeType::new(edge_type),
//                                         direction: EdgeDirection::Src,
//                                     };
//
//                                     let neighbor_query = NeighborQuery {
//                                         src_vertex_ids: vec![vertex_id],
//                                         edge_descriptor,
//                                         dest_vertex_states: None,
//                                     };
//
//                                     if let Ok(_) = txn.query_neighbor_vertex_ids(&neighbor_query) {
//                                         query_success = true;
//                                     }
//                                 }
//                             }
//                         },
//                         _ => unreachable!(),
//                     }
//
//                     if query_success {
//                         successful_queries += 1;
//                     }
//                 }
//
//                 counter_clone.fetch_add(successful_queries, Ordering::SeqCst);
//             });
//
//             handles.push(handle);
//         }
//
//         // 等待所有线程完成
//         for handle in handles {
//             handle.join().unwrap();
//         }
//
//         let elapsed = start_time.elapsed();
//         let successful_queries = counter.load(Ordering::SeqCst);
//         let total_queries = thread_count * queries_per_thread;
//
//         println!("并发查询测试完成");
//         println!("总耗时: {:?}", elapsed);
//         println!("成功查询次数: {}/{}", successful_queries, total_queries);
//         println!("每秒查询数: {:.2}", successful_queries as f64 / elapsed.as_secs_f64());
//
//         // 验证结果
//         assert!(successful_queries > 0, "应该至少有一些成功的查询");
//
//         // 清理数据库文件
//         drop(db);
//         std::fs::remove_dir_all(&db_path).ok();
//     }
//
//     /**
//      * 测试混合并发操作
//      *
//      * 测试场景：
//      * 1. 同时进行创建、修改、删除和查询操作
//      * 2. 验证数据库在混合负载下的表现
//      *
//      * 预期结果：
//      * - 所有操作都能成功完成
//      * - 没有死锁或数据不一致问题
//      */
//     #[test]
//     fn test_mixed_concurrent_operations() {
//         // 创建临时数据库
//         let db_path = format!("/tmp/pgraph_concurrent_mixed_test_{}", std::process::id());
//         let config = RocksDbConfig::new(db_path.clone());
//         let db = Arc::new(RocksDatabase::new_db_with_config(config));
//
//         // 预先创建一些节点
//         let initial_nodes = 50;
//         let mut node_ids = vec![];
//
//         {
//             let mut txn = db.transaction();
//             for i in 0..initial_nodes {
//                 let card_id = format!("mixed_node_{}", i);
//
//                 let mut vertex = create_test_vertex(
//                     0,
//                     &card_id,
//                     &format!("混合测试节点 {}", i),
//                     "task",
//                     "container1",
//                     CardState::Active,
//                 );
//
//                 txn.create_vertex(&mut vertex).unwrap();
//                 node_ids.push(vertex.id);
//             }
//             txn.commit().unwrap();
//         }
//
//         let thread_count = 20;
//         let operations_per_thread = 50;
//
//         let barrier = Arc::new(Barrier::new(thread_count));
//         let success_counter = Arc::new(AtomicUsize::new(0));
//         let node_ids_arc = Arc::new(node_ids);
//
//         let mut handles = vec![];
//
//         println!("开始测试混合并发操作...");
//         let start_time = Instant::now();
//
//         for t in 0..thread_count {
//             let db_clone = Arc::clone(&db);
//             let counter_clone = Arc::clone(&success_counter);
//             let barrier_clone = Arc::clone(&barrier);
//             let node_ids_clone = Arc::clone(&node_ids_arc);
//
//             let handle = thread::spawn(move || {
//                 // 等待所有线程就绪
//                 barrier_clone.wait();
//
//                 let mut successful_ops = 0;
//
//                 for i in 0..operations_per_thread {
//                     let op_type = (t + i) % 4; // 4种不同类型的操作
//
//                     match op_type {
//                         0 => {
//                             // 创建新节点
//                             let mut txn = db_clone.transaction();
//                             let card_id = format!("new_mixed_node_{}_{}", t, i);
//
//                             let mut vertex = create_test_vertex(
//                                 0,
//                                 &card_id,
//                                 &format!("新建混合节点 {}-{}", t, i),
//                                 "task",
//                                 "container1",
//                                 CardState::Active,
//                             );
//
//                             if let Ok(true) = txn.create_vertex(&mut vertex) {
//                                 txn.commit().unwrap();
//                                 successful_ops += 1;
//                             } else {
//                                 txn.rollback().unwrap();
//                             }
//                         },
//                         1 => {
//                             // 修改现有节点
//                             if !node_ids_clone.is_empty() {
//                                 let index = (t * i) % node_ids_clone.len();
//                                 let vertex_id = node_ids_clone[index];
//
//                                 let mut txn = db_clone.transaction();
//
//                                 if let Ok(vertices) = txn.get_specific_vertices(&vec![vertex_id]) {
//                                     if let Some(vertex_arc) = vertices.first() {
//                                         // 创建一个新的Vertex实例
//                                         let mut updated_vertex = create_test_vertex(
//                                             vertex_id,
//                                             &vertex_arc.card_id,
//                                             &format!("修改后混合节点 {}-{}", t, i),
//                                             &vertex_arc.card_type_id.0,
//                                             &vertex_arc.container_id.0,
//                                             vertex_arc.state,
//                                         );
//
//                                         // 保留原始节点的一些字段
//                                         updated_vertex.org_id = vertex_arc.org_id.clone();
//                                         // 创建新的StreamInfo而不是调用clone
//                                         updated_vertex.stream_info = StreamInfo {
//                                             stream_id: vertex_arc.stream_info.stream_id.clone(),
//                                             step_id: vertex_arc.stream_info.step_id.clone(),
//                                             status_id: vertex_arc.stream_info.status_id.clone(),
//                                         };
//                                         updated_vertex.code_in_org = vertex_arc.code_in_org.clone();
//                                         updated_vertex.custom_code = vertex_arc.custom_code.clone();
//                                         updated_vertex.position = vertex_arc.position;
//                                         updated_vertex.created_at = vertex_arc.created_at;
//                                         updated_vertex.updated_at = vertex_arc.updated_at;
//                                         updated_vertex.field_values = vertex_arc.field_values.clone();
//
//                                         if let Ok(true) = txn.update_vertex(&updated_vertex) {
//                                             txn.commit().unwrap();
//                                             successful_ops += 1;
//                                         } else {
//                                             txn.rollback().unwrap();
//                                         }
//                                     }
//                                 }
//                             }
//                         },
//                         2 => {
//                             // 创建边关系
//                             if node_ids_clone.len() >= 2 {
//                                 let src_index = (t * i) % node_ids_clone.len();
//                                 let dest_index = (src_index + 1) % node_ids_clone.len();
//
//                                 let src_id = node_ids_clone[src_index];
//                                 let dest_id = node_ids_clone[dest_index];
//
//                                 let mut txn = db_clone.transaction();
//
//                                 let edge = Edge::new(
//                                     src_id,
//                                     EdgeType::new("MIXED_RELATION"),
//                                     dest_id,
//                                     None,
//                                 );
//
//                                 if let Ok(true) = txn.create_edge(&edge) {
//                                     txn.commit().unwrap();
//                                     successful_ops += 1;
//                                 } else {
//                                     txn.rollback().unwrap();
//                                 }
//                             }
//                         },
//                         3 => {
//                             // 查询操作
//                             let txn = db_clone.transaction();
//
//                             let query = VertexQuery {
//                                 card_type_ids: vec![Identifier::new("task")],
//                                 card_ids: None,
//                                 container_ids: None,
//                                 states: None,
//                                 vertex_ids: None,
//                             };
//
//                             if let Ok(vertices) = txn.query_vertices(query) {
//                                 if !vertices.is_empty() {
//                                     successful_ops += 1;
//                                 }
//                             }
//                         },
//                         _ => unreachable!(),
//                     }
//                 }
//
//                 counter_clone.fetch_add(successful_ops, Ordering::SeqCst);
//             });
//
//             handles.push(handle);
//         }
//
//         // 等待所有线程完成
//         for handle in handles {
//             handle.join().unwrap();
//         }
//
//         let elapsed = start_time.elapsed();
//         let successful_ops = success_counter.load(Ordering::SeqCst);
//         let total_ops = thread_count * operations_per_thread;
//
//         println!("混合并发操作测试完成");
//         println!("总耗时: {:?}", elapsed);
//         println!("成功操作次数: {}/{}", successful_ops, total_ops);
//         println!("每秒操作数: {:.2}", successful_ops as f64 / elapsed.as_secs_f64());
//
//         // 验证结果
//         assert!(successful_ops > 0, "应该至少有一些成功的操作");
//
//         {
//             // 进行最终验证 - 使用作用域确保txn在drop(db)前已经释放
//             let txn = db.transaction();
//             let query = VertexQuery {
//                 card_type_ids: vec![Identifier::new("task")],
//                 card_ids: None,
//                 container_ids: None,
//                 states: None,
//                 vertex_ids: None,
//             };
//
//             if let Ok(vertices) = txn.query_vertices(query) {
//                 println!("最终数据库中的节点数: {}", vertices.len());
//                 assert!(vertices.len() >= initial_nodes, "最终节点数应该至少等于初始节点数");
//             }
//         }
//
//         // 清理数据库文件
//         drop(db);
//         std::fs::remove_dir_all(&db_path).ok();
//     }
// }