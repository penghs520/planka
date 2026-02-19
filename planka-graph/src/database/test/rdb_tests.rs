use crate::database::database::Database;
use crate::database::model::{
    CardState, CardTypeId, ContainerId, Edge, EdgeDescriptor, EdgeDirection, EdgeProp, EdgeType, FieldId, NeighborQuery, NumberProp, VertexId, VertexQuery
};
use crate::database::test::test_utils::{new_test_vertex, TestDb};
use crate::database::transaction::Transaction;
/**
 * 数据库RocksDB实现测试模块
 *
 * 提供对RocksDB实现的各种功能测试，包括：
 * - 节点的创建与查询
 * - 边的创建与查询
 * - 各种复合查询条件测试
 * - 事务隔离性测试
 */

use std::collections::HashSet;

#[cfg(test)]
mod tests {
    use super::*;

    /**
     * 测试节点创建功能
     *
     * 测试场景：
     * 1. 创建一个新节点，验证创建成功
     * 2. 尝试重复创建同一节点，验证操作具有幂等性
     * 3. 验证节点能够被正确查询到
     * 4. 测试缓存机制，确保第二次查询使用了缓存
     *
     * 预期结果：
     * - 首次创建节点返回true
     * - 重复创建返回false
     * - 通过ID查询能够找到创建的节点
     * - 缓存机制正常工作
     */
    #[test]
    fn test_vertex_creation() {
        let test_db = TestDb::new();

        // 第一个事务：创建节点
        let mut txn = test_db.db.transaction();
        let mut vertex = new_test_vertex(1);

        assert!(txn.create_vertex(&mut vertex).unwrap());

        // 提交事务
        txn.commit().unwrap();

        // 重新创建事务
        let mut txn = test_db.db.transaction();

        // 重复创建节点，应该返回false表示幂等
        assert!(!txn.create_vertex(&mut vertex).unwrap());
        txn.commit().unwrap();

        // 第二个事务：验证节点存在
        let txn = test_db.db.transaction();
        let vertices = txn.get_specific_vertices(&vec![vertex.card_id]).unwrap();
        assert_eq!(vertices.len(), 1);

        // 第二次从缓存中读取
        let cached_vertices = txn.get_specific_vertices(&vec![vertex.card_id]).unwrap();
        assert_eq!(cached_vertices.len(), 1);
    }

    /**
     * 测试边创建及查询功能
     *
     * 测试场景：
     * 1. 创建两个节点
     * 2. 在两个节点之间创建一条边
     * 3. 验证正向查询能找到目标节点
     * 4. 验证错误方向查询无法找到目标节点
     * 5. 验证反向边查询能找到源节点
     *
     * 预期结果：
     * - 节点1通过正向查询能找到节点2
     * - 节点1通过错误方向查询找不到节点2
     * - 节点2通过反向查询能找到节点1
     * - 验证边存储的双向性
     */
    #[test]
    fn test_edge_creation_and_query() {
        let test_db = TestDb::new();

        // 准备：创建两个节点
        let mut txn = test_db.db.transaction();
        let mut vertex1 = new_test_vertex(1);
        let mut vertex2 = new_test_vertex(2);
        txn.create_vertex(&mut vertex1).unwrap();
        txn.create_vertex(&mut vertex2).unwrap();
        txn.commit().unwrap();

        // 第一个事务：创建边
        let mut txn = test_db.db.transaction();
        let edge = Edge::new(vertex1.card_id, EdgeType::new("FRIEND"), vertex2.card_id, Some(vec![]));
        let result = txn.create_edge(&edge);
        println!("创建边结果: {:?}", result);
        assert!(result.unwrap());
        txn.commit().unwrap();

        // 第二个事务：验证边存在
        let txn = test_db.db.transaction();

        // 测试正向查询
        let query = NeighborQuery {
            src_vertex_ids: vec![vertex1.card_id],
            edge_descriptor: EdgeDescriptor {
                t: EdgeType::new("FRIEND"),
                direction: EdgeDirection::Src,
            },
            dest_vertex_states: None,
        };
        println!("查询边关系: {:?}", query);
        let neighbor_vertex_ids = txn.query_neighbor_vertex_ids(&query).unwrap();
        println!("查询结果: {:?}", neighbor_vertex_ids);
        assert_eq!(neighbor_vertex_ids.len(), 1);
        assert_eq!(neighbor_vertex_ids[0], vertex2.card_id);

        // 测试错误方向查询
        let query = NeighborQuery {
            src_vertex_ids: vec![vertex1.card_id],
            edge_descriptor: EdgeDescriptor {
                t: EdgeType::new("FRIEND"),
                direction: EdgeDirection::Dest,
            },
            dest_vertex_states: None,
        };
        println!("查询边关系: {:?}", query);
        let neighbor_vertex_ids = txn.query_neighbor_vertex_ids(&query).unwrap();
        println!("查询结果: {:?}", neighbor_vertex_ids);
        assert_eq!(neighbor_vertex_ids.len(), 0);

        // 测试反向边查询
        let query = NeighborQuery {
            src_vertex_ids: vec![vertex2.card_id],
            edge_descriptor: EdgeDescriptor {
                t: EdgeType::new("FRIEND"),
                direction: EdgeDirection::Dest,
            },
            dest_vertex_states: None,
        };
        println!("查询边关系: {:?}", query);
        let neighbor_vertex_ids = txn.query_neighbor_vertex_ids(&query).unwrap();
        println!("查询结果: {:?}", neighbor_vertex_ids);
        assert_eq!(neighbor_vertex_ids.len(), 1);
        assert_eq!(neighbor_vertex_ids[0], vertex1.card_id);
    }

    /**
     * 测试多个边的创建和查询
     *
     * 测试场景：
     * 1. 创建三个节点
     * 2. 从节点1创建两条边，分别连接到节点2和节点3
     * 3. 验证节点1的邻居查询能够找到节点2和节点3
     *
     * 预期结果：
     * - 邻居查询能找到两个目标节点
     * - 结果集包含预期的两个节点ID
     * - 验证一对多关系的正确存储和查询
     */
    #[test]
    fn test_multiple_edges() {
        let test_db = TestDb::new();

        // 准备：创建三个节点
        let mut txn = test_db.db.transaction();
        let mut vertex1 = new_test_vertex(1);
        let mut vertex2 = new_test_vertex(2);
        let mut vertex3 = new_test_vertex(3);

        txn.create_vertex(&mut vertex1).unwrap();
        txn.create_vertex(&mut vertex2).unwrap();
        txn.create_vertex(&mut vertex3).unwrap();
        txn.commit().unwrap();

        // 创建边：vertex1 -> vertex2, vertex1 -> vertex3
        let mut txn = test_db.db.transaction();
        let edge1 = Edge::new(vertex1.card_id, EdgeType::new("FRIEND"), vertex2.card_id, Some(vec![]));
        let edge2 = Edge::new(vertex1.card_id, EdgeType::new("FRIEND"), vertex3.card_id, Some(vec![]));

        txn.create_edge(&edge1).unwrap();
        txn.create_edge(&edge2).unwrap();
        txn.commit().unwrap();

        // 验证多个邻接节点
        let txn = test_db.db.transaction();
        let neighbor_vertex_ids = txn
            .query_neighbor_vertex_ids(&NeighborQuery {
                src_vertex_ids: vec![vertex1.card_id],
                edge_descriptor: EdgeDescriptor {
                    t: EdgeType::new("FRIEND"),
                    direction: EdgeDirection::Src,
                },
                dest_vertex_states: None,
            })
            .unwrap();

        assert_eq!(neighbor_vertex_ids.len(), 2);

        // 结果应该包含vertex2和vertex3
        let result_set: HashSet<VertexId> = neighbor_vertex_ids.into_iter().collect();
        assert!(result_set.contains(&vertex2.card_id));
        assert!(result_set.contains(&vertex3.card_id));
    }

    /**
     * 测试节点查询功能
     *
     * 测试场景：
     * 1. 创建一个节点
     * 2. 测试基本节点类型查询
     * 3. 测试复合条件查询（类型+容器+状态）
     * 4. 测试不存在的卡片类型
     * 5. 测试不存在的容器
     * 6. 测试不匹配的卡片状态
     *
     * 预期结果：
     * - 基本查询和复合查询都能找到匹配的节点
     * - 不存在的条件查询返回空结果
     * - 验证查询条件的逻辑组合
     */
    #[test]
    fn test_vertex_query() {
        let test_db = TestDb::new();

        // 准备：创建节点
        let mut txn = test_db.db.transaction();
        let mut vertex = new_test_vertex(1);
        txn.create_vertex(&mut vertex).unwrap();
        txn.commit().unwrap();

        let txn = test_db.db.transaction();

        // 测试简单查询
        let query = VertexQuery {
            card_ids: None,
            vertex_ids: None,
            card_type_ids: vec![CardTypeId::new("default_card_type_id".to_string())],
            container_ids: None,
            states: None,
        };
        let result = txn.query_vertices(query).unwrap();
        assert_eq!(result.len(), 1);
        assert_eq!(result[0].card_id, vertex.card_id);
        assert_eq!(result[0].card_id, vertex.card_id);

        // 测试复合条件查询 - 应该匹配
        let query = VertexQuery {
            card_ids: None,
            vertex_ids: None,
            card_type_ids: vec![CardTypeId::new("default_card_type_id".to_string())],
            container_ids: Some(vec![ContainerId::new("default_container_id".to_string())]),
            states: Some(vec![CardState::Active]),
        };
        let result = txn.query_vertices(query).unwrap();
        assert_eq!(result.len(), 1);
        assert_eq!(result[0].card_id, vertex.card_id);
        assert_eq!(result[0].card_id, vertex.card_id);

        // 测试不存在的卡片类型
        let query = VertexQuery {
            card_ids: None,
            vertex_ids: None,
            card_type_ids: vec![CardTypeId::new("default_card_type_id_2".to_string())],
            container_ids: None,
            states: None,
        };
        let result = txn.query_vertices(query).unwrap();
        assert_eq!(result.len(), 0);

        // 测试不存在的容器
        let query = VertexQuery {
            card_ids: None,
            vertex_ids: None,
            card_type_ids: vec![CardTypeId::new("default_card_type_id".to_string())],
            container_ids: Some(vec![ContainerId::new("default_container_id_2".to_string())]),
            states: None,
        };
        let result = txn.query_vertices(query).unwrap();
        assert_eq!(result.len(), 0);

        // 测试不匹配的卡片状态
        let query = VertexQuery {
            card_ids: None,
            vertex_ids: None,
            card_type_ids: vec![CardTypeId::new("default_card_type_id".to_string())],
            container_ids: Some(vec![ContainerId::new("default_container_id".to_string())]),
            states: Some(vec![CardState::Archived]),
        };
        let result = txn.query_vertices(query).unwrap();
        assert_eq!(result.len(), 0);
    }

    /**
     * 测试事务隔离性 - 读提交级别
     *
     * 测试场景：
     * 1. 事务1创建节点但不提交
     * 2. 事务2尝试查询该节点，验证节点不可见
     * 3. 事务1提交后，事务3再次查询，验证节点可见
     *
     * 预期结果：
     * - 在事务1提交前，事务2无法查询到节点（读提交隔离级别）
     * - 在事务1提交后，事务3可以查询到节点
     * - 验证系统支持读提交隔离级别
     */
    #[test]
    fn test_transaction_isolation_read_committed() {
        let test_db = TestDb::new();

        // 事务1：创建节点
        let mut txn1 = test_db.db.transaction();
        let mut vertex = new_test_vertex(1);
        txn1.create_vertex(&mut vertex).unwrap();

        // 事务2：在事务1提交前进行查询
        let txn2 = test_db.db.transaction();

        // 根据card_type_id查询节点
        let card_type_id = CardTypeId::new("default_card_type_id".to_string());
        let query = VertexQuery {
            card_ids: None,
            vertex_ids: None,
            card_type_ids: vec![card_type_id.clone()],
            container_ids: None,
            states: None,
        };

        let vertices = txn2.query_vertices(query).unwrap();
        // 事务1未提交，查询结果应为空
        assert_eq!(vertices.len(), 0);

        // 提交事务1
        txn1.commit().unwrap();

        // 事务3：在事务1提交后进行查询
        let txn3 = test_db.db.transaction();

        let query = VertexQuery {
            card_ids: None,
            vertex_ids: None,
            card_type_ids: vec![card_type_id],
            container_ids: None,
            states: None,
        };

        let vertices = txn3.query_vertices(query).unwrap();
        // 事务1已提交，查询结果应包含创建的节点
        assert_eq!(vertices.len(), 1);
        assert_eq!(vertices[0].card_id, vertex.card_id);
    }

    /**
     * 测试事务隔离性 - 节点更新
     *
     * 测试场景：
     * 1. 创建一个带有描述的节点
     * 2. 事务1开始更新节点描述，但不提交
     * 3. 事务2尝试读取节点，验证仍看到原始描述
     * 4. 事务1提交更新
     * 5. 事务3读取节点，验证能看到更新后的描述
     *
     * 预期结果：
     * - 在更新事务提交前，其他事务仍看到原始节点数据
     * - 在更新事务提交后，新的事务能看到更新的数据
     * - 验证更新操作的事务隔离性
     */
    #[test]
    fn test_transaction_isolation_vertex_update() {
        let test_db = TestDb::new();

        // 准备：创建一个节点
        let mut txn = test_db.db.transaction();
        let mut vertex = new_test_vertex(1);
        vertex.custom_code = Some("原始卡片编号".to_string());
        txn.create_vertex(&mut vertex).unwrap();
        txn.commit().unwrap();

        // 事务1：更新节点描述，但先不提交
        let mut txn1 = test_db.db.transaction();

        // 注意：由于Vertex没有实现Clone，需要创建一个新的节点对象进行更新
        let mut updated_vertex = new_test_vertex(1);
        updated_vertex.custom_code = Some("更新后的卡片编号".to_string());

        println!("更新前节点的卡片编号: {:?}", updated_vertex.custom_code);
        let update_result = txn1.update_vertex(&updated_vertex);
        println!("更新结果: {:?}", update_result);

        // 事务2：在事务1提交前读取节点
        let txn2 = test_db.db.transaction();
        let vertices = txn2.get_specific_vertices(&vec![vertex.card_id]).unwrap();
        assert_eq!(vertices.len(), 1);
        // 在事务1提交前，读到的应该仍是原始描述
        println!("事务2读取到的卡片编号: {:?}", vertices[0].custom_code);
        assert_eq!(vertices[0].custom_code, Some("原始卡片编号".to_string()));

        // 提交事务1的更新
        txn1.commit().unwrap();

        // 事务3：在事务1提交后读取节点
        let txn3 = test_db.db.transaction();
        let vertices = txn3.get_specific_vertices(&vec![vertex.card_id]).unwrap();
        assert_eq!(vertices.len(), 1);
        // 在事务1提交后，应该能读到更新后的描述
        println!("事务3读取到的卡片编号: {:?}", vertices[0].custom_code);
        assert_eq!(vertices[0].custom_code, Some("更新后的卡片编号".to_string()));
    }

    /**
     * 测试事务隔离性 - 节点删除
     *
     * 测试场景：
     * 1. 创建一个节点
     * 2. 事务1开始删除节点，但不提交
     * 3. 事务2尝试读取节点，验证节点仍存在
     * 4. 事务1提交删除
     * 5. 事务3尝试读取节点，验证节点已不存在
     *
     * 预期结果：
     * - 在删除事务提交前，其他事务仍能查询到节点
     * - 在删除事务提交后，节点不再可见
     * - 验证删除操作的事务隔离性
     */
    #[test]
    fn test_transaction_isolation_vertex_delete() {
        let test_db = TestDb::new();

        // 准备：创建一个节点
        let mut txn = test_db.db.transaction();
        let mut vertex = new_test_vertex(1);
        txn.create_vertex(&mut vertex).unwrap();
        txn.commit().unwrap();

        // 事务1：删除节点，但先不提交
        let mut txn1 = test_db.db.transaction();
        txn1.delete_vertices(&vec![vertex.card_id]).unwrap();

        // 事务2：在事务1提交前读取节点
        let txn2 = test_db.db.transaction();
        let vertices = txn2.get_specific_vertices(&vec![vertex.card_id]).unwrap();
        // 事务1未提交，节点应该仍存在
        assert_eq!(vertices.len(), 1);

        // 提交事务1的删除
        txn1.commit().unwrap();

        // 事务3：在事务1提交后读取节点
        let txn3 = test_db.db.transaction();
        let vertices = txn3.get_specific_vertices(&vec![vertex.card_id]).unwrap();
        // 事务1已提交，节点应该已被删除
        assert_eq!(vertices.len(), 0);
    }

    /**
     * 测试事务隔离性 - 边删除
     *
     * 测试场景：
     * 1. 创建两个节点和它们之间的边
     * 2. 事务1开始删除边，但不提交
     * 3. 事务2查询邻居，验证边仍存在
     * 4. 事务1提交删除
     * 5. 事务3查询邻居，验证边已不存在
     *
     * 预期结果：
     * - 在删除事务提交前，其他事务仍能查询到边关系
     * - 在删除事务提交后，边关系不再可见
     * - 验证边删除的事务隔离性
     */
    #[test]
    fn test_transaction_isolation_edge_delete() {
        let test_db = TestDb::new();

        // 准备：创建两个节点和一条边
        let mut txn = test_db.db.transaction();
        let mut vertex1 = new_test_vertex(1);
        let mut vertex2 = new_test_vertex(2);
        txn.create_vertex(&mut vertex1).unwrap();
        txn.create_vertex(&mut vertex2).unwrap();

        let edge = Edge::new(vertex1.card_id, EdgeType::new("FRIEND"), vertex2.card_id, Some(vec![]));
        txn.create_edge(&edge).unwrap();
        txn.commit().unwrap();

        // 事务1：删除边，但先不提交
        let mut txn1 = test_db.db.transaction();
        txn1.delete_edges_by_edge_descriptors(&vec![EdgeDescriptor {
            t: EdgeType::new("FRIEND"),
            direction: EdgeDirection::Src
        }]).unwrap();

        // 事务2：在事务1提交前查询邻居关系
        let txn2 = test_db.db.transaction();
        let query = NeighborQuery {
            src_vertex_ids: vec![vertex1.card_id],
            edge_descriptor: EdgeDescriptor {
                t: EdgeType::new("FRIEND"),
                direction: EdgeDirection::Src,
            },
            dest_vertex_states: None,
        };

        let neighbor_ids = txn2.query_neighbor_vertex_ids(&query).unwrap();
        // 事务1未提交，应该仍能查询到邻居关系
        assert_eq!(neighbor_ids.len(), 1);
        assert_eq!(neighbor_ids[0], vertex2.card_id);

        // 提交事务1的删除
        txn1.commit().unwrap();

        // 事务3：在事务1提交后查询邻居关系
        let txn3 = test_db.db.transaction();
        let neighbor_ids = txn3.query_neighbor_vertex_ids(&query).unwrap();
        // 事务1已提交，邻居关系应该已被删除
        assert_eq!(neighbor_ids.len(), 0);
    }

    /**
     * 测试查询邻居边功能
     *
     * 测试场景：
     * 1. 创建两个节点
     * 2. 创建一条带属性的边连接这两个节点，携带属性
     * 3. 使用 query_neighbor_edges 查询边，验证返回的边信息
     * 4. 测试反向查询
     * 5. 验证反向边属性为空（初始状态）
     * 6. 测试更新正向边属性，验证正向查询能获取更新后的属性
     * 7. 测试尝试更新反向边属性，验证该操作会成功（因为两点之间的双向边在物理中同时存在）
     * 8. 测试创建反向边（作为新的正向边）来设置反向关系的不同属性
     * 9. 测试删除边
     * 10. 测试查询不存在的边，正向查询返回空，反向查询返回空
     *
     * 预期结果：
     * - 正向查询返回正确的边，包含正确的属性
     * - 反向查询也能正确找到边，但反向边不存储属性（属性为空）
     * - 更新正向边属性后，正向查询能获取更新后的属性，反向边属性保持为空
     * - 尝试通过update_edge更新反向边属性会成功，因为两点之间的双向边在物理中同时存在
     * - 可以通过创建反向方向的新边（使用不同的边类型）来设置反向关系的属性
     * - 删除边后，无法再查询到该边关系，正向查询和反向查询都返回空
     * - 查询不存在的边类型时，返回空结果
     */
    #[test]
    fn test_query_neighbor_edges_with_props() {
        let test_db = TestDb::new();

        // 第一个事务：创建节点和边
        let mut txn = test_db.db.transaction();

        // 创建节点
        let mut vertex1 = new_test_vertex(0);
        let mut vertex2 = new_test_vertex(2);

        // 创建节点
        assert!(txn.create_vertex(&mut vertex1).unwrap());
        assert!(txn.create_vertex(&mut vertex2).unwrap());

        // 创建一条带属性的边
        let edge_props = Some(vec![
            EdgeProp::Number(NumberProp {
                field_id: FieldId::new("weight"),
                number: 123.45
            })
        ]);

        let edge = Edge::new(
            vertex1.card_id,
            EdgeType::new("DEPENDS_ON"),
            vertex2.card_id,
            edge_props.clone()
        );

        // 创建边并提交事务
        assert!(txn.create_edge(&edge).unwrap());
        txn.commit().unwrap();

        // 第二个事务：使用 query_neighbor_edges 查询
        let txn = test_db.db.transaction();

        // 测试正向查询
        let query = NeighborQuery {
            src_vertex_ids: vec![vertex1.card_id],
            edge_descriptor: EdgeDescriptor {
                t: EdgeType::new("DEPENDS_ON"),
                direction: EdgeDirection::Src,
            },
            dest_vertex_states: None,
        };

        let edges = txn.query_neighbor_edges_with_props(&query).unwrap();
        assert_eq!(edges.len(), 1, "应该找到一条边");

        let found_edge = &edges[0];
        assert_eq!(found_edge.src_id, vertex1.card_id, "源节点ID应匹配");
        assert_eq!(found_edge.dest_id, vertex2.card_id, "目标节点ID应匹配");
        assert_eq!(found_edge.t, EdgeType::new("DEPENDS_ON"), "边类型应匹配");

        // 验证边属性
        if let Some(props) = &found_edge.props {
            assert_eq!(props.len(), 1, "应该有一个属性");
            match &props[0] {
                EdgeProp::Number(number_prop) => {
                    assert_eq!(number_prop.field_id, FieldId::new("weight"));
                    assert_eq!(number_prop.number, 123.45);
                },
                _ => panic!("属性应该是数值类型"),
            }
        } else {
            panic!("边属性不应为空");
        }

        // 测试反向查询
        let reverse_query = NeighborQuery {
            src_vertex_ids: vec![vertex2.card_id],
            edge_descriptor: EdgeDescriptor {
                t: EdgeType::new("DEPENDS_ON"),
                direction: EdgeDirection::Dest,
            },
            dest_vertex_states: None,
        };

        let reverse_edges = txn.query_neighbor_edges(&reverse_query).unwrap();
        assert_eq!(reverse_edges.len(), 1, "应该找到一条反向边");

        let reverse_edge = &reverse_edges[0];
        assert_eq!(reverse_edge.src_id, vertex2.card_id, "反向边的源节点ID应匹配");
        assert_eq!(reverse_edge.dest_id, vertex1.card_id, "反向边的目标节点ID应匹配");

        // 初始状态：打印正向边和反向边的属性
        println!("=== 初始状态 ===");
        println!("正向边属性: {:?}", found_edge.props);
        println!("反向边属性: {:?}", reverse_edge.props);

        // 验证反向边属性为空，因为在存储模型中只有正向边存储属性
        assert!(reverse_edge.props.is_none(), "反向边不应该存储属性");

        // 提交事务后创建新事务，测试更新边属性
        txn.commit().unwrap();

        // 更新正向边属性
        let mut txn = test_db.db.transaction();
        let updated_props = Some(vec![
            EdgeProp::Number(NumberProp {
                field_id: FieldId::new("weight"),
                number: 456.78
            })
        ]);

        let updated_edge = Edge::new(
            vertex1.card_id,
            EdgeType::new("DEPENDS_ON"),
            vertex2.card_id,
            updated_props.clone()
        );

        println!("正在更新正向边属性...");
        let update_result = txn.update_edge(&updated_edge, EdgeDirection::Src);
        println!("更新正向边结果: {:?}", update_result);
        assert!(update_result.unwrap());
        txn.commit().unwrap();

        // 验证更新后的正向边属性
        let txn = test_db.db.transaction();
        let edges = txn.query_neighbor_edges_with_props(&query).unwrap();
        assert_eq!(edges.len(), 1, "应该找到一条边");

        let found_edge = &edges[0];
        println!("=== 更新正向边后 ===");
        println!("更新后的正向边属性: {:?}", found_edge.props);

        // 验证更新后的边属性
        if let Some(props) = &found_edge.props {
            assert_eq!(props.len(), 1, "应该有一个属性");
            match &props[0] {
                EdgeProp::Number(number_prop) => {
                    assert_eq!(number_prop.field_id, FieldId::new("weight"));
                    assert_eq!(number_prop.number, 456.78);
                },
                _ => panic!("属性应该是数值类型"),
            }
        } else {
            panic!("边属性不应为空");
        }

        // 验证反向边的属性仍为空，不受正向边更新的影响
        let reverse_edges = txn.query_neighbor_edges_with_props(&reverse_query).unwrap();
        assert_eq!(reverse_edges.len(), 1, "应该找到一条反向边");

        let reverse_edge = &reverse_edges[0];
        println!("更新正向边后的反向边属性: {:?}", reverse_edge.props);

        // 反向边属性应该始终为空
        assert!(reverse_edge.props.is_none(), "反向边不应该存储属性");

        txn.commit().unwrap();

        // 尝试通过update_edge更新反向边属性
        let mut txn = test_db.db.transaction();

        let rev_updated_props = Some(vec![
            EdgeProp::Number(NumberProp {
                field_id: FieldId::new("weight"),
                number: 999.99
            })
        ]);

        let rev_updated_edge = Edge::new(
            vertex2.card_id,
            EdgeType::new("DEPENDS_ON"),
            vertex1.card_id,
            rev_updated_props.clone()
        );

        println!("正在尝试通过update_edge更新反向边属性...");
        let rev_update_result = txn.update_edge(&rev_updated_edge, EdgeDirection::Dest);
        assert!(rev_update_result.unwrap());
        txn.commit().unwrap();

        // 验证反向边属性被更新
        let txn = test_db.db.transaction();
        let reverse_edges = txn.query_neighbor_edges_with_props(&reverse_query).unwrap();
        assert_eq!(reverse_edges.len(), 1, "应该找到一条反向边");

        let reverse_edge = &reverse_edges[0];
        println!("尝试更新后的反向边属性: {:?}", reverse_edge.props);
        assert!(reverse_edge.props.is_some(), "反向边属性应该被更新");

        // 验证反向边属性被更新
        if let Some(props) = &reverse_edge.props {
            assert_eq!(props.len(), 1, "应该有一个属性");
            match &props[0] {
                EdgeProp::Number(number_prop) => {
                    assert_eq!(number_prop.field_id, FieldId::new("weight"));
                    assert_eq!(number_prop.number, 999.99);
                },
                _ => panic!("属性应该是数值类型"),
            }
        } else {
            panic!("反向边属性不应为空");
        }

        // 验证正向边属性不受反向边更新尝试的影响
        let edges = txn.query_neighbor_edges_with_props(&query).unwrap();
        assert_eq!(edges.len(), 1, "应该找到一条正向边");

        let found_edge = &edges[0];
        println!("尝试更新反向边后的正向边属性: {:?}", found_edge.props);

        // 正向边的属性应该保持不变
        if let Some(props) = &found_edge.props {
            assert_eq!(props.len(), 1, "应该有一个属性");
            match &props[0] {
                EdgeProp::Number(number_prop) => {
                    assert_eq!(number_prop.field_id, FieldId::new("weight"));
                    assert_eq!(number_prop.number, 456.78);
                },
                _ => panic!("属性应该是数值类型"),
            }
        } else {
            panic!("边属性不应为空");
        }

        // 通过create_edge创建反向关系（作为新的正向边）
        txn.commit().unwrap();
        let mut txn = test_db.db.transaction();

        // 创建从vertex2到vertex1的新边（与原来的边方向相反）
        let reverse_edge_props = Some(vec![
            EdgeProp::Number(NumberProp {
                field_id: FieldId::new("weight"),
                number: 888.88
            })
        ]);

        let new_reverse_edge = Edge::new(
            vertex2.card_id,
            EdgeType::new("DEPENDS_ON_REVERSE"), // 使用不同的边类型
            vertex1.card_id,
            reverse_edge_props.clone()
        );

        println!("正在创建新的反向关系边...");
        let create_reverse_result = txn.create_edge(&new_reverse_edge);
        println!("创建反向关系边结果: {:?}", create_reverse_result);
        assert!(create_reverse_result.unwrap());
        txn.commit().unwrap();

        // 验证新创建的反向关系边
        let txn = test_db.db.transaction();

        // 查询从vertex2到vertex1的边
        let new_reverse_query = NeighborQuery {
            src_vertex_ids: vec![vertex2.card_id],
            edge_descriptor: EdgeDescriptor {
                t: EdgeType::new("DEPENDS_ON_REVERSE"),
                direction: EdgeDirection::Src,
            },
            dest_vertex_states: None,
        };

        let new_reverse_edges = txn.query_neighbor_edges_with_props(&new_reverse_query).unwrap();
        assert_eq!(new_reverse_edges.len(), 1, "应该找到一条新的反向关系边");

        let new_reverse_edge = &new_reverse_edges[0];
        println!("新创建的反向关系边属性: {:?}", new_reverse_edge.props);

        // 验证新创建的反向关系边的属性
        if let Some(props) = &new_reverse_edge.props {
            assert_eq!(props.len(), 1, "应该有一个属性");
            match &props[0] {
                EdgeProp::Number(number_prop) => {
                    assert_eq!(number_prop.field_id, FieldId::new("weight"));
                    assert_eq!(number_prop.number, 888.88);
                },
                _ => panic!("属性应该是数值类型"),
            }
        } else {
            panic!("新创建的反向关系边属性不应为空");
        }

        // 测试删除边
        txn.commit().unwrap();
        let mut txn = test_db.db.transaction();

        // 删除第一条边
        println!("=== 删除原边 ===");
        let specific_edge = Edge::new(
            vertex1.card_id,
            EdgeType::new("DEPENDS_ON"),
            vertex2.card_id,
            None
        );
        println!("尝试删除特定边: {:?}", specific_edge);

        txn.delete_edges_by_edge_descriptors(&vec![EdgeDescriptor {
            t: EdgeType::new("DEPENDS_ON"),
            direction: EdgeDirection::Src
        }]).unwrap();

        // 删除新创建的反向关系边
        println!("=== 删除新创建的反向关系边 ===");
        let specific_reverse_edge = Edge::new(
            vertex2.card_id,
            EdgeType::new("DEPENDS_ON_REVERSE"),
            vertex1.card_id,
            None
        );
        println!("尝试删除反向关系边: {:?}", specific_reverse_edge);

        txn.delete_edges_by_edge_descriptors(&vec![EdgeDescriptor {
            t: EdgeType::new("DEPENDS_ON_REVERSE"),
            direction: EdgeDirection::Src
        }]).unwrap();

        txn.commit().unwrap();

        // 验证边已被删除
        let txn = test_db.db.transaction();

        // 正向查询应返回空
        let edges = txn.query_neighbor_edges_with_props(&query).unwrap();
        println!("删除后的正向查询结果数量: {}", edges.len());
        assert_eq!(edges.len(), 0, "正向边应该已被删除");

        // 反向查询也应返回空
        let reverse_edges = txn.query_neighbor_edges_with_props(&reverse_query).unwrap();
        println!("删除后的反向查询结果数量: {}", reverse_edges.len());
        assert_eq!(reverse_edges.len(), 0, "反向边也应该已被删除");

        // 验证新创建的反向关系边已被删除
        let new_reverse_edges = txn.query_neighbor_edges_with_props(&new_reverse_query).unwrap();
        println!("删除后的新反向关系边查询结果数量: {}", new_reverse_edges.len());
        assert_eq!(new_reverse_edges.len(), 0, "新创建的反向关系边应该已被删除");

        // 测试查询不存在的边类型
        let non_exist_query = NeighborQuery {
            src_vertex_ids: vec![vertex1.card_id],
            edge_descriptor: EdgeDescriptor {
                t: EdgeType::new("NON_EXIST_TYPE"),
                direction: EdgeDirection::Src,
            },
            dest_vertex_states: None,
        };

        let edges = txn.query_neighbor_edges_with_props(&non_exist_query).unwrap();
        assert_eq!(edges.len(), 0, "不应找到不存在的边类型");
    }

    #[test]
    fn test_concurrent_different_edges_creation_and_deletion_10_times(){
        for _ in 0..10 {
            test_concurrent_different_edges_creation_and_deletion();
        }
    }

    ///测试多线程多个事务同一个边类型下的边的创建，验证最终的边结果不会丢失
    ///接着再测试多线程多个事务对边进行逐个的删除，验证最终所有的边都被删除
    #[test]
    fn test_concurrent_different_edges_creation_and_deletion(){
        use std::sync::Arc;
        use std::thread;

        // 测试参数配置
        const A_NODE_COUNT: usize = 1000;  // A节点数量
        const THREAD_COUNT: usize = 20;    // 线程数量
        const EDGES_PER_THREAD: usize = A_NODE_COUNT / THREAD_COUNT;  // 每个线程处理的边数量

        let test_db = Arc::new(TestDb::new());

        // 1. 创建1000个A节点
        let mut txn = test_db.db.transaction();
        let mut a_vertices = Vec::new();

        for i in 0..A_NODE_COUNT {
            let mut vertex = new_test_vertex(i as u64);
            txn.create_vertex(&mut vertex).unwrap();
            a_vertices.push(vertex.card_id);
        }

        // 2. 创建1个B节点
        let mut b_vertex = new_test_vertex(A_NODE_COUNT as u64);
        txn.create_vertex(&mut b_vertex).unwrap();
        let b_vertex_id = b_vertex.card_id;

        txn.commit().unwrap();

        // 3. 开启20个线程，每个线程负责创建50条边
        let mut handles = Vec::new();

        for thread_id in 0..THREAD_COUNT {
            let db_clone: Arc<TestDb> = Arc::clone(&test_db);
            let start = thread_id * EDGES_PER_THREAD;
            let end = (thread_id + 1) * EDGES_PER_THREAD;
            let a_vertices_clone = a_vertices.clone();

            let handle = thread::spawn(move || {
                println!("线程 {} 开始创建边，范围: {}..{}", thread_id, start, end);

                for i in start..end {
                    let mut txn = db_clone.db.transaction();
                    let a_vertex_id = a_vertices_clone[i];

                    // 创建从B节点到A节点的边
                    let edge = Edge::new(
                        b_vertex_id,
                        EdgeType::new("CONNECTS_TO"),
                        a_vertex_id,
                        Some(vec![
                            EdgeProp::Number(NumberProp {
                                field_id: FieldId::new("thread_id"),
                                number: thread_id as f64
                            })
                        ])
                    );

                    match txn.create_edge(&edge) {
                        Ok(true) => {
                            println!("线程 {} 成功创建边: B -> A[{}]", thread_id, i);
                        },
                        Ok(false) => {
                            println!("线程 {} 边已存在: B -> A[{}]", thread_id, i);
                        },
                        Err(e) => {
                            println!("线程 {} 创建边失败: B -> A[{}], 错误: {:?}", thread_id, i, e);
                        }
                    }

                    match txn.commit() {
                        Ok(_) => {
                            println!("线程 {} 提交成功，创建了 1 条边", thread_id);
                        },
                        Err(e) => {
                            println!("线程 {} 提交失败: {:?}", thread_id, e);
                        }
                    }
                }

                println!("线程 {} 完成，处理了 {} 条边", thread_id, EDGES_PER_THREAD);
            });

            handles.push(handle);
        }

        // 等待所有线程完成
        for handle in handles {
            match handle.join() {
                Ok(_) => {},
                Err(e) => println!("线程执行出错: {:?}", e),
            }
        }

        println!("所有线程完成，预期创建了 {} 条边", A_NODE_COUNT);

        // 4. 最终验证B节点与每一个A节点都存在关系
        let txn = test_db.db.transaction();

        // 查询B节点的所有邻居
        let query = NeighborQuery {
            src_vertex_ids: vec![b_vertex_id],
            edge_descriptor: EdgeDescriptor {
                t: EdgeType::new("CONNECTS_TO"),
                direction: EdgeDirection::Src,
            },
            dest_vertex_states: None,
        };

        let neighbor_ids = txn.query_neighbor_vertex_ids(&query).unwrap();
        println!("B节点的邻居数量: {}", neighbor_ids.len());

        // 验证找到了A_NODE_COUNT个邻居
        assert_eq!(neighbor_ids.len(), A_NODE_COUNT, "B节点应该与所有{}个A节点都有连接", A_NODE_COUNT);

        // 验证每个A节点都在邻居列表中
        let neighbor_set: HashSet<VertexId> = neighbor_ids.into_iter().collect();
        for (i, &a_vertex_id) in a_vertices.iter().enumerate() {
            assert!(
                neighbor_set.contains(&a_vertex_id),
                "A节点 {} (ID: {:?}) 应该在B节点的邻居列表中",
                i,
                a_vertex_id
            );
        }

        // 额外验证：查询带属性的边，确保属性也被正确存储
        let edges = txn.query_neighbor_edges_with_props(&query).unwrap();
        assert_eq!(edges.len(), A_NODE_COUNT, "应该找到{}条带属性的边", A_NODE_COUNT);

        // 验证边属性中的线程ID分布
        let mut thread_counts = vec![0; THREAD_COUNT];
        for edge in &edges {
            if let Some(props) = &edge.props {
                for prop in props {
                    if let EdgeProp::Number(number_prop) = prop {
                        if number_prop.field_id == FieldId::new("thread_id") {
                            let thread_id = number_prop.number as usize;
                            if thread_id < THREAD_COUNT {
                                thread_counts[thread_id] += 1;
                            }
                        }
                    }
                }
            }
        }

        println!("各线程创建的边数量分布: {:?}", thread_counts);

        // 验证每个线程都创建了预期数量的边
        for (thread_id, &count) in thread_counts.iter().enumerate() {
            assert_eq!(
                count, EDGES_PER_THREAD,
                "线程 {} 应该创建了{}条边，实际创建了 {} 条",
                thread_id, EDGES_PER_THREAD, count
            );
        }

        println!("并发边创建测试通过！所有{}条边都被正确创建和存储，{}个线程并发执行。", A_NODE_COUNT, THREAD_COUNT);
        
        txn.commit().unwrap();

        // ==================== 开始多线程并发删除边测试 ====================
        println!("\n=== 开始多线程并发删除边测试 ===");

        // 5. 开启20个线程，每个线程负责删除50条边
        let mut delete_handles = Vec::new();

        for thread_id in 0..THREAD_COUNT {
            let db_clone: Arc<TestDb> = Arc::clone(&test_db);
            let start = thread_id * EDGES_PER_THREAD;
            let end = (thread_id + 1) * EDGES_PER_THREAD;
            let a_vertices_clone = a_vertices.clone();

            let handle = thread::spawn(move || {
                println!("删除线程 {} 开始删除边，范围: {}..{}", thread_id, start, end);

                for i in start..end {
                    let mut txn = db_clone.db.transaction();
                    let a_vertex_id = a_vertices_clone[i];

                    // 创建要删除的边对象
                    let edge = Edge::new(
                        b_vertex_id,
                        EdgeType::new("CONNECTS_TO"),
                        a_vertex_id,
                        None // 删除时不需要属性
                    );

                    match txn.delete_edge(&edge) {
                        Ok(_) => {
                            println!("删除线程 {} 成功删除边: B -> A[{}]", thread_id, i);
                        },
                        Err(e) => {
                            println!("删除线程 {} 删除边失败: B -> A[{}], 错误: {:?}", thread_id, i, e);
                        }
                    }

                    match txn.commit() {
                        Ok(_) => {
                            println!("删除线程 {} 提交成功，删除了 1 条边", thread_id);
                        },
                        Err(e) => {
                            println!("删除线程 {} 提交失败: {:?}", thread_id, e);
                        }
                    }
                }

                println!("删除线程 {} 完成，处理了 {} 条边", thread_id, EDGES_PER_THREAD);
            });

            delete_handles.push(handle);
        }

        // 等待所有删除线程完成
        for handle in delete_handles {
            match handle.join() {
                Ok(_) => {},
                Err(e) => println!("删除线程执行出错: {:?}", e),
            }
        }

        println!("所有删除线程完成，预期删除了 {} 条边", A_NODE_COUNT);

        // 6. 最终验证所有边都被删除
        let txn = test_db.db.transaction();

        // 再次查询B节点的所有邻居
        let final_neighbor_ids = txn.query_neighbor_vertex_ids(&query).unwrap();
        println!("删除后B节点的邻居数量: {}", final_neighbor_ids.len());

        // 验证所有边都被删除，B节点应该没有任何邻居
        assert_eq!(final_neighbor_ids.len(), 0, "删除后B节点不应该有任何邻居连接");

        // 额外验证：查询带属性的边，应该为空
        let final_edges = txn.query_neighbor_edges_with_props(&query).unwrap();
        assert_eq!(final_edges.len(), 0, "删除后应该找不到任何边");

        // 验证反向查询也为空
        for &a_vertex_id in &a_vertices {
            let reverse_query = NeighborQuery {
                src_vertex_ids: vec![a_vertex_id],
                edge_descriptor: EdgeDescriptor {
                    t: EdgeType::new("CONNECTS_TO"),
                    direction: EdgeDirection::Dest,
                },
                dest_vertex_states: None,
            };

            let reverse_neighbors = txn.query_neighbor_vertex_ids(&reverse_query).unwrap();
            assert_eq!(reverse_neighbors.len(), 0, "A节点 {:?} 的反向连接也应该被删除", a_vertex_id);
        }

        println!("并发边删除测试通过！所有{}条边都被正确删除，{}个线程并发执行。", A_NODE_COUNT, THREAD_COUNT);
        println!("完整的并发边创建和删除测试通过！");
    }

}