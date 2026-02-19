use crate::database::errors::{DbError, DbResult};
use crate::database::model::{BulkInsertItem, CardId, Edge, EdgeDescriptor, EdgeDirection, NeighborQuery, Vertex, VertexId, VertexQuery, EdgeProp};
use std::collections::HashMap;
use std::sync::Arc;

/// 事务接口 - 定义单个事务的操作集合
///
/// 提供对节点和边的创建、查询、删除等操作
pub trait Transaction<'a> {
    /// 创建一个节点
    ///
    /// 返回是否成功创建（true表示创建成功，false表示已存在）
    fn create_vertex(&mut self, vertex: &mut Vertex) -> DbResult<bool>;

    /// 检查节点是否存在
    ///
    /// 根据节点ID查询节点是否存在
    fn vertex_exists(&self, vertex_id: &VertexId) -> DbResult<bool>;

    /// 检查边是否存在
    ///
    /// 查询指定的边是否已存在于数据库中
    fn exists_edge(&self, edge: &Edge, direction: EdgeDirection) -> DbResult<bool>;

    /// 创建一条边
    ///
    /// 如果创建成功，返回true，如果边已经存在，返回false
    /// 如果边两侧的节点不存在，返回错误
    fn create_edge(&mut self, edge: &Edge) -> DbResult<bool>;

    /// 更新节点
    ///
    /// 根据提供的节点对象更新节点的属性
    /// 返回是否成功更新（true表示更新成功，false表示节点不存在）
    fn update_vertex(&mut self, vertex: &Vertex) -> DbResult<bool>;

    /// 更新边
    /// 需要指定方向，因为边在物理上存在两个方向
    /// 根据提供的边对象更新边的属性
    /// 返回是否成功更新（true表示更新成功，false表示边不存在）
    fn update_edge(&mut self, edge: &Edge, direction: EdgeDirection) -> DbResult<bool>;

    /// 批量插入节点和边
    ///
    /// 默认实现通过循环调用单个插入方法实现，
    /// 具体实现可以覆盖此方法以提供更高效的批量操作
    fn bulk_insert(&mut self, items: Vec<BulkInsertItem>) -> DbResult<()> {
        for item in items {
            match item {
                BulkInsertItem::Vertex(mut vertex) => {
                    self.create_vertex(&mut vertex)?;
                }
                BulkInsertItem::Edge(edge) => {
                    self.create_edge(&edge)?;
                }
            }
        }
        Ok(())
    }

    /// 删除指定的节点集合
    ///
    /// 根据节点ID列表删除对应的节点
    fn delete_vertices(&mut self, vertices: &Vec<VertexId>) -> DbResult<()>;

    /// 获取指定的节点集合
    ///
    /// 根据节点ID列表获取节点详情
    fn get_specific_vertices(&self, ids: &Vec<VertexId>) -> DbResult<Vec<Arc<Vertex>>>;

    fn get_vertex_by_id(&self, ids: &VertexId) -> Option<Arc<Vertex>>;
    fn get_vertex_by_id_directly(&self, ids: &VertexId) -> Option<Arc<Vertex>>;
    /// 根据查询条件获取节点
    ///
    /// 使用VertexQuery对象定义查询条件
    fn query_vertices(&self, query: VertexQuery) -> DbResult<Vec<Arc<Vertex>>>;

    /// 删除指定的边集合
    ///
    /// 根据边描述符列表删除对应的边
    fn delete_edges_by_edge_descriptors(&mut self, edge_descriptors: &Vec<EdgeDescriptor>) -> DbResult<()>;

    /// 删除指定的边
    ///
    /// 根据具体的边对象列表删除对应的边，而不是根据边描述符删除所有匹配的边
    fn delete_edges(&mut self, edges: &Vec<Edge>) -> DbResult<()> {
        // 默认实现是逐个删除边
        for edge in edges {
            self.delete_edge(edge)?;
        }
        Ok(())
    }

    /// 删除单条边
    ///
    /// 删除指定的一条边
    fn delete_edge(&mut self, edge: &Edge) -> DbResult<()>;

    /// 根据节点ID，边类型和方向查询边
    ///
    /// 返回与指定节点相连的特定类型和方向的所有边，不包含边属性
    ///
    /// # 参数
    /// * `vertex_id` - 节点ID
    /// * `descriptor` - 边描述符（包含边类型和方向）
    ///
    /// # 返回
    /// 符合条件的边的迭代器
    fn query_neighbor_edges(
        &self,
        query: &NeighborQuery,
    ) -> DbResult<Vec<Edge>>;

    /// 查询邻居节点的边，并返回边属性
    ///
    /// 根据查询条件获取与指定节点相连的邻居节点的边，并返回边属性
    ///
    /// # 参数
    /// * `query` - 查询条件
    ///
    /// # 返回
    fn query_neighbor_edges_with_props(
        &self,
        query: &NeighborQuery,
    ) -> DbResult<Vec<Edge>>;

    /// 查询邻居节点的ID
    ///
    /// 根据查询条件获取与指定节点相连的邻居ID
    fn query_neighbor_vertex_ids(&self, query: &NeighborQuery) -> DbResult<Vec<VertexId>>;

    /// 查询邻居节点
    ///
    /// 根据查询条件获取与指定节点相连的邻居详情
    fn query_neighbor_vertices(&self, query: &NeighborQuery) -> DbResult<Vec<Arc<Vertex>>> {
        let vertex_ids = self.query_neighbor_vertex_ids(query)?;
        if vertex_ids.is_empty() {
            return Ok(vec![]);
        }
        self.get_specific_vertices(&vertex_ids)
    }

    /// 获取边的属性
    ///
    /// 根据源节点ID、边类型和目标节点ID获取边的属性
    ///
    /// # 参数
    /// * `src_vertex_id` - 源节点ID
    /// * `edge_type` - 边类型
    /// * `dest_vertex_id` - 目标节点ID
    ///
    /// # 返回
    /// 如果边存在属性则返回Some(Vec<EdgeProp>)，否则返回None
    fn get_edge_properties(
        &self,
        src_vertex_id: &VertexId,
        edge_type: &super::model::EdgeType,
        dest_vertex_id: &VertexId,
    ) -> Option<Vec<EdgeProp>>;

    /// 同步数据到磁盘
    ///
    /// 确保所有修改都已写入持久化存储
    /// 默认实现返回Unsupported错误，具体实现应覆盖此方法
    fn sync(&self) -> DbResult<()> {
        Err(DbError::Unsupported)
    }

    /// 提交事务
    ///
    /// 确认所有事务中的修改并应用到数据库
    fn commit(self) -> DbResult<()>;

    /// 回滚事务
    ///
    /// 撤销事务中的所有修改
    fn rollback(self) -> DbResult<()>;

    /// 获取节点总数
    ///
    /// 返回数据库中的节点数量
    fn get_vertex_count(&self) -> u64;

    /// 检查卡片是否存在
    ///
    /// # 参数
    /// * `card_id` - 卡片ID (u64)
    ///
    /// # 返回
    /// 如果卡片存在返回true，否则返回false
    fn card_exists(&self, card_id: CardId) -> bool;

    /// 批量获取卡片描述信息
    ///
    /// 根据节点ID列表获取对应卡片的描述信息
    /// 如果描述信息不存在，则该节点ID对应的描述为None
    ///
    /// # 参数
    /// * `vertex_ids` - 节点ID列表
    ///
    /// # 返回
    /// 节点ID到描述信息的映射
    fn get_card_descriptions(&self, vertex_ids: &Vec<VertexId>) -> HashMap<VertexId, Option<Arc<String>>>;
    fn get_card_description(&self, vertex_ids: &VertexId) -> Option<Arc<String>>;

    /// 检查是否需要包含卡片描述
    ///
    /// 根据Yield结构判断是否需要包含卡片描述字段
    ///
    /// # 参数
    /// * `r#yield` - 返回结构配置
    ///
    /// # 返回
    /// 如果需要包含描述则返回true，否则返回false
    fn should_include_description(&self, r#yield: Option<&crate::proto::pgraph::query::Yield>) -> bool {
        if let Some(yield_conf) = r#yield {
            if let Some(yielded_field) = &yield_conf.yielded_field {
                // 检查是否直接包含描述字段
                if yielded_field.contains_desc {
                    return true;
                }

                // 检查是否返回所有自定义字段（包括描述）
                if yielded_field.contains_all_custom_field {
                    return true;
                }
            }
        }

        // 默认不包含描述
        false
    }
}