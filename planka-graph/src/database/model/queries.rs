use super::{CardId, CardState, CardTypeId, ContainerId, EdgeDescriptor, VertexId};
use std::collections::HashSet;

/**
 * 查询模块 - 定义图数据库的各种查询结构
 * 
 * 本模块包含节点查询、边查询和邻居查询等数据结构，
 * 用于在图数据库中执行高效的数据检索操作
 */

/// 节点查询结构 - 用于根据多种条件查询节点
///
/// 支持按节点类型、卡片ID、容器和状态进行过滤
#[derive(Debug)]
pub struct VertexQuery {

    /// 要查询的节点ID列表
    pub card_ids: Option<Vec<CardId>>,

    /// 要查询的节点ID列表，card_ids和vertex_ids不会同时存在
    pub vertex_ids: Option<HashSet<VertexId>>,

    /// 要查询的节点类型ID列表
    pub card_type_ids: Vec<CardTypeId>,
    
    /// 容器ID列表过滤（可选）
    /// 如果提供，只返回指定容器中的节点
    pub container_ids: Option<Vec<ContainerId>>,
    
    /// 节点状态过滤（可选）
    /// 如果提供，只返回指定状态的节点
    pub states: Option<Vec<CardState>>,
}

/// 邻居查询结构 - 用于查找与指定节点相连的邻居节点
///
/// 根据边描述符和源节点ID查询邻居
#[derive(Debug)]
pub struct NeighborQuery {
    /// 源节点ID列表 - 起始查询的节点
    pub src_vertex_ids: Vec<VertexId>,
    
    /// 边描述符 - 定义要遵循的关系类型和方向
    pub edge_descriptor: EdgeDescriptor,
    
    /// 目标节点状态过滤（可选）
    /// 默认只返回活跃和归档的节点，不包括已放弃的
    pub dest_vertex_states: Option<Vec<CardState>>,
}

/// 边查询结构 - 用于查询满足条件的边
///
/// 类似于邻居查询，但侧重于边本身而非目标节点
#[derive(Debug)]
pub struct EdgeQuery {
    /// 源节点ID列表 - 起始查询的节点
    pub src_vertex_ids: Vec<VertexId>,
    
    //TODO path - 未来可能支持路径查询
    
    /// 边描述符 - 定义要查询的关系类型和方向
    pub edge_descriptor: EdgeDescriptor,
    
    /// 目标节点状态过滤（可选）
    /// 默认只返回连接到活跃和归档节点的边，不包括已放弃的
    pub dest_vertex_states: Option<Vec<CardState>>,
}

