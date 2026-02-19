use super::{Edge, Vertex};

/**
 * 批量插入模块 - 提供批量操作的数据结构
 * 
 * 该模块定义了批量插入操作中可以使用的数据项类型，
 * 支持在一次操作中插入多个节点和边
 */


/// 批量插入项枚举 - 表示批量插入操作中的单个项
///
/// 可以是节点或边，允许在同一批操作中混合插入不同类型的数据
#[derive(Debug)]
pub enum BulkInsertItem {
    /// 节点数据项
    Vertex(Vertex),
    
    /// 边数据项
    Edge(Edge),
}
