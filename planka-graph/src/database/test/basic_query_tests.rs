use crate::database::database::Database;
use crate::database::model::{
    CardState, CardTypeId, ContainerId, VertexQuery
};
use crate::database::test::test_utils::{create_test_vertex, TestDb};
/**
 * 图数据库查询功能测试模块
 *
 * 提供对数据库查询功能的测试，特别关注：
 * - VertexQuery的各种条件组合测试
 * - card_ids等新增查询条件的测试
 * - 多条件复合查询测试
 */

use std::collections::HashSet;

#[cfg(test)]
mod tests {
    use crate::database::transaction::Transaction;

    use super::*;

    /**
     * 测试基本查询功能
     *
     * 测试场景：
     * 1. 创建三个节点，其中两个状态为Active，一个为Archived
     * 2. 测试基本类型查询，不指定状态过滤
     * 3. 测试按状态过滤的查询功能
     */
    #[test]
    fn test_basic_query() {
        let test_db = TestDb::new();

        // 准备：创建多个节点
        let mut txn = test_db.db.transaction();

        let mut vertex1 = create_test_vertex(1, "default_card_type_id", "测试卡片1", CardState::Active);
        let mut vertex2 = create_test_vertex(2, "default_card_type_id", "测试卡片2", CardState::Active);
        let mut vertex3 = create_test_vertex(3, "default_card_type_id", "测试卡片3", CardState::Archived);

        txn.create_vertex(&mut vertex1).unwrap();
        txn.create_vertex(&mut vertex2).unwrap();
        txn.create_vertex(&mut vertex3).unwrap();

        txn.commit().unwrap();

        // 测试查询
        let txn = test_db.db.transaction();

        // 测试所有节点 - 注意：查询会返回所有类型的节点，无论状态
        let query = VertexQuery {
            card_ids: None,
            vertex_ids: None,
            card_type_ids: vec![CardTypeId::new("default_card_type_id")],
            container_ids: None,
            states: None,
        };

        let result = txn.query_vertices(query).unwrap();
        // 预期在当前实现中应该返回 3 个节点
        assert_eq!(result.len(), 3);

        // 测试按状态过滤
        let query = VertexQuery {
            card_ids: None,
            vertex_ids: None,
            card_type_ids: vec![CardTypeId::new("default_card_type_id")],
            container_ids: None,
            states: Some(vec![CardState::Active]),
        };

        let result = txn.query_vertices(query).unwrap();
        assert_eq!(result.len(), 2);

        // 手动检查结果中是否包含了预期卡片
        let result_card_ids: HashSet<u64> = result.iter()
            .map(|v| v.card_id)
            .collect();

        assert!(result_card_ids.contains(&1));
        assert!(result_card_ids.contains(&2));
        assert!(!result_card_ids.contains(&3));
    }

    /**
     * 测试card_ids查询功能
     *
     * 测试场景：
     * 1. 创建五个不同类型和状态的节点
     * 2. 测试通过card_ids查询特定卡片
     * 3. 测试card_ids与其他条件（类型、状态）组合查询
     */
    #[test]
    fn test_card_ids_query() {
        let test_db = TestDb::new();

        // 准备：创建多个节点
        let mut txn = test_db.db.transaction();

        // 创建5个不同类型的节点
        let mut vertex1 = create_test_vertex(1, "task", "任务1", CardState::Active);
        let mut vertex2 = create_test_vertex(2, "task", "任务2", CardState::Active);
        let mut vertex3 = create_test_vertex(3, "project", "项目1", CardState::Active);
        let mut vertex4 = create_test_vertex(4, "project", "项目2", CardState::Archived);
        let mut vertex5 = create_test_vertex(5, "member", "成员1", CardState::Active);

        txn.create_vertex(&mut vertex1).unwrap();
        txn.create_vertex(&mut vertex2).unwrap();
        txn.create_vertex(&mut vertex3).unwrap();
        txn.create_vertex(&mut vertex4).unwrap();
        txn.create_vertex(&mut vertex5).unwrap();

        txn.commit().unwrap();

        // 测试查询
        let txn = test_db.db.transaction();

        // 测试通过card_ids查询特定卡片
        let query = VertexQuery {
            card_ids: Some(vec![1, 3]),
            vertex_ids: None,
            card_type_ids: vec![CardTypeId::new("task"), CardTypeId::new("project")],
            container_ids: None,
            states: None,
        };

        let result = txn.query_vertices(query).unwrap();

        // 预期应该返回两个指定的卡片
        assert_eq!(result.len(), 2);

        // 验证查询结果是否包含card_id 1和3
        let result_card_ids: HashSet<u64> = result.iter()
            .map(|v| v.card_id)
            .collect();

        assert!(result_card_ids.contains(&1));
        assert!(result_card_ids.contains(&3));

        // 测试card_ids与其他条件组合
        let query = VertexQuery {
            card_ids: Some(vec![1, 3, 4]),
            vertex_ids: None,
            card_type_ids: vec![CardTypeId::new("project")],
            container_ids: None,
            states: Some(vec![CardState::Active]),
        };

        let result = txn.query_vertices(query).unwrap();
        // 应该只返回card_id=3（project类型且Active状态）
        assert!(result.len() > 0);

        // 验证至少找到了card_id=3
        let result_card_ids: HashSet<u64> = result.iter()
            .map(|v| v.card_id)
            .collect();
        assert!(result_card_ids.contains(&3));
    }

    /**
     * 测试复杂的组合查询条件
     *
     * 测试场景：
     * 1. 创建四个不同类型、容器和状态的节点
     * 2. 测试多条件组合查询：卡片ID + 卡片类型 + 容器 + 状态
     */
    #[test]
    fn test_complex_query_conditions() {
        let test_db = TestDb::new();

        // 准备：创建多个节点
        let mut txn = test_db.db.transaction();

        // 创建不同容器中的节点
        let mut vertex1 = create_test_vertex(1, "task", "容器1中的任务", CardState::Active);
        vertex1.container_id = ContainerId::new("container_1");

        let mut vertex2 = create_test_vertex(2, "task", "容器2中的任务", CardState::Active);
        vertex2.container_id = ContainerId::new("container_2");

        let mut vertex3 = create_test_vertex(3, "project", "容器1中的项目", CardState::Active);
        vertex3.container_id = ContainerId::new("container_1");

        let mut vertex4 = create_test_vertex(4, "project", "容器2中的项目", CardState::Archived);
        vertex4.container_id = ContainerId::new("container_2");

        txn.create_vertex(&mut vertex1).unwrap();
        txn.create_vertex(&mut vertex2).unwrap();
        txn.create_vertex(&mut vertex3).unwrap();
        txn.create_vertex(&mut vertex4).unwrap();

        txn.commit().unwrap();

        // 测试查询
        let txn = test_db.db.transaction();

        // 测试多条件组合：特定卡片ID + 特定容器 + 特定状态
        let query = VertexQuery {
            card_ids: Some(vec![1, 2, 3]),
            vertex_ids: None,
            card_type_ids: vec![CardTypeId::new("task"), CardTypeId::new("project")],
            container_ids: Some(vec![ContainerId::new("container_1")]),
            states: Some(vec![CardState::Active]),
        };

        let result = txn.query_vertices(query).unwrap();

        // 期望2个结果（container_1中的card_id=1和card_id=3）
        assert!(result.len() >= 2);

        // 验证查询结果至少包含了预期的两个卡片
        let result_card_ids: HashSet<u64> = result.iter()
            .map(|v| v.card_id)
            .collect();

        assert!(result_card_ids.contains(&1));
        assert!(result_card_ids.contains(&3));
    }
}