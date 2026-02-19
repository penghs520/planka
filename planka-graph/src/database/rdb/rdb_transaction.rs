use crate::database::errors::{DbError, DbResult, ValidationError};
use crate::database::model::{
    CardId, CardState, CardTypeId, Edge, EdgeDescriptor, EdgeDirection, EdgeProp, EdgeType,
    NeighborQuery, Vertex, VertexId, VertexQuery,
};
use crate::database::transaction::Transaction;
use rocksdb::{OptimisticTransactionDB, Transaction as RocksTxn, WriteBatchWithTransaction};
use std::collections::HashMap;

use std::sync::Arc;
use std::time::Instant;
use tracing::{debug, warn};

use super::edge_manager::EdgeManager;
use super::memory::{EdgeDeltaItem, FxHashSet, InMemory, VertexFragment};
use super::vertex_manager::VertexManager;

/// RocksDB事务实现 - 处理图数据库的事务操作
pub struct RocksTransaction<'a> {
    /// RocksDB数据库引用
    db: &'a OptimisticTransactionDB,

    /// RocksDB内部事务
    inner_txn: RocksTxn<'a, OptimisticTransactionDB>,

    /// 写入批处理缓冲区
    write_batch: WriteBatchWithTransaction<true>,

    /// 节点管理器
    vertex_manager: VertexManager<'a>,

    /// 边管理器
    edge_manager: EdgeManager<'a>,

    /// 内存缓存引用
    in_memory: &'a InMemory,
}

impl<'a> RocksTransaction<'a> {
    /// 创建新的RocksDB事务实例
    ///
    /// # 参数
    /// * `db` - RocksDB数据库引用
    /// * `in_memory` - 内存缓存引用
    ///
    /// # 返回
    /// 新的RocksTransaction实例
    pub(super) fn new(db: &'a OptimisticTransactionDB, in_memory: &'a InMemory) -> Self {
        let txn = db.transaction();
        let write_batch = WriteBatchWithTransaction::new();
        RocksTransaction {
            db,
            inner_txn: txn,
            write_batch,
            vertex_manager: VertexManager::new(db),
            edge_manager: EdgeManager::new(db),
            in_memory,
        }
    }

    /// 根据卡片ID返回节点ID
    /// 注意：现在card_id就是vertex_id（u64类型），直接返回card_id即可
    ///
    /// # 参数
    /// * `card_id` - 卡片ID
    ///
    /// # 返回
    /// 节点ID（与card_id相同）
    pub(crate) fn return_vertex_id(&self, card_id: &CardId) -> DbResult<VertexId> {
        // card_id 就是 vertex_id，直接返回
        Ok(*card_id)
    }

    /// 检查节点状态是否满足查询条件
    fn check_vertex_state(&self, vertex_id: VertexId, states: &Option<Vec<CardState>>) -> bool {
        if let Some(states) = states {
            if states.is_empty() {
                // 如果状态集为空，只包含InProgress和Archived状态的节点
                if let Some(v) = self.in_memory.get_vertex_fragment(&vertex_id) {
                    v.card_state == CardState::Active || v.card_state == CardState::Archived
                } else if self.vertex_manager.delta.create.contains_key(&vertex_id) {
                    // 假设新创建的节点状态为InProgress
                    true
                } else {
                    false
                }
            } else {
                // 如果状态集不为空，检查目标节点状态是否在集合中
                if let Some(v) = self.in_memory.get_vertex_fragment(&vertex_id) {
                    states.contains(&v.card_state)
                } else if self.vertex_manager.delta.create.contains_key(&vertex_id) {
                    // 对于当前事务创建的节点，检查其状态
                    let vertex_fragment = self.vertex_manager.delta.create.get(&vertex_id).unwrap();
                    states.contains(&vertex_fragment.card_state)
                } else {
                    false
                }
            }
        } else {
            // 如果没有指定状态过滤，只包含InProgress和Archived状态的节点
            if let Some(v) = self.in_memory.get_vertex_fragment(&vertex_id) {
                v.card_state == CardState::Active || v.card_state == CardState::Archived
            } else if self.vertex_manager.delta.create.contains_key(&vertex_id) {
                // 假设新创建的节点状态为InProgress
                true
            } else {
                false
            }
        }
    }

    /// 处理事务中新创建的边
    fn process_created_edges(
        &self,
        query: &NeighborQuery,
        deleted_edges: &FxHashSet<(VertexId, VertexId)>,
    ) -> Vec<VertexId> {
        let mut result = Vec::new();
        for edge_delta_item in &self.edge_manager.delta.create {
            if edge_delta_item.edge_descriptor.t == query.edge_descriptor.t
                && edge_delta_item.edge_descriptor.direction == query.edge_descriptor.direction
                && query
                    .src_vertex_ids
                    .contains(&edge_delta_item.src_vertex_id)
            {
                // 如果之前标记为删除，则跳过
                if deleted_edges.contains(&(
                    edge_delta_item.src_vertex_id,
                    edge_delta_item.dest_vertex_id,
                )) {
                    continue;
                }

                if self
                    .check_vertex_state(edge_delta_item.dest_vertex_id, &query.dest_vertex_states)
                {
                    result.push(edge_delta_item.dest_vertex_id);
                }
            }
        }
        result
    }

    /// 处理内存缓存中的边
    fn process_cached_edges(
        &self,
        query: &NeighborQuery,
        deleted_edges: &FxHashSet<(VertexId, VertexId)>,
    ) -> Vec<VertexId> {
        let mut result = Vec::new();
        for src_vertex_id in &query.src_vertex_ids {
            self.in_memory
                .with_edge_targets(&query.edge_descriptor, src_vertex_id, |neighbors| {
                    for &dest_vertex_id in neighbors.iter() {
                        // 如果边已被删除，跳过
                        if deleted_edges.contains(&(*src_vertex_id, dest_vertex_id)) {
                            continue;
                        }

                        if self.check_vertex_state(dest_vertex_id, &query.dest_vertex_states) {
                            result.push(dest_vertex_id);
                        }
                    }
                });
        }
        result
    }

    /// 收集已删除的边
    fn collect_deleted_edges(&self, query: &NeighborQuery) -> FxHashSet<(VertexId, VertexId)> {
        let mut deleted_edges = FxHashSet::default();
        for edge_delta_item in &self.edge_manager.delta.delete {
            if edge_delta_item.edge_descriptor.t == query.edge_descriptor.t
                && edge_delta_item.edge_descriptor.direction == query.edge_descriptor.direction
                && query
                    .src_vertex_ids
                    .contains(&edge_delta_item.src_vertex_id)
            {
                deleted_edges.insert((
                    edge_delta_item.src_vertex_id,
                    edge_delta_item.dest_vertex_id,
                ));
            }
        }
        deleted_edges
    }

    /// 将本次事务的数据更新写入数据库
    /// id_counter 不再需要，因为 card_id 就是 vertex_id
    fn update_id_counter_and_write_db(&mut self) -> DbResult<()> {
        let write_batch =
            std::mem::replace(&mut self.write_batch, WriteBatchWithTransaction::new());
        self.db
            .write(write_batch)
            .map_err(|e| DbError::TransactionError(Box::new(e)))
    }

    fn update_vertex_cache(&mut self) {
        // 获取所有更新
        let vertex_delta = std::mem::replace(&mut self.vertex_manager.delta, Default::default());

        // 按卡片类型分组收集需要处理的节点
        let mut delete_by_card_type: HashMap<CardTypeId, Vec<(VertexId, CardId)>> = HashMap::new();
        let mut create_by_card_type: HashMap<CardTypeId, Vec<(VertexId, VertexFragment)>> =
            HashMap::new();
        let mut update_by_card_type: HashMap<CardTypeId, Vec<(VertexId, VertexFragment)>> =
            HashMap::new();

        // 分组删除的节点
        for (vertex_id, patch) in vertex_delta.delete {
            delete_by_card_type
                .entry(patch.card_type_id)
                .or_insert_with(Vec::new)
                .push((vertex_id, patch.card_id));
        }

        // 分组新建的节点
        for (vertex_id, patch) in vertex_delta.create {
            create_by_card_type
                .entry(patch.card_type_id)
                .or_insert_with(Vec::new)
                .push((vertex_id, patch));
        }

        // 分组更新的节点
        for (vertex_id, current) in vertex_delta.update {
            if let Some(before) = self.in_memory.get_vertex_fragment(&vertex_id) {
                if before.card_type_id != current.card_type_id {
                    // 如果卡片类型发生变化，需要处理两个不同类型的索引
                    delete_by_card_type
                        .entry(before.card_type_id)
                        .or_insert_with(Vec::new)
                        .push((vertex_id, before.card_id.clone()));

                    create_by_card_type
                        .entry(current.card_type_id)
                        .or_insert_with(Vec::new)
                        .push((vertex_id, current));
                } else {
                    // 卡片类型没变，只需更新其他信息
                    update_by_card_type
                        .entry(current.card_type_id)
                        .or_insert_with(Vec::new)
                        .push((vertex_id, current));
                }
            }
            // 移除相关的LRU缓存
            self.in_memory.remove_vertex_from_lru_cache(&vertex_id);
            self.in_memory
                .remove_card_description_from_cache(&vertex_id);
        }

        // 处理更新（卡片类型相同的情况）
        for (_, vertices) in update_by_card_type {
            for (vertex_id, patch) in vertices {
                // 这些节点的卡片类型没变，只需更新其他属性
                // 更新节点缓存
                self.in_memory.insert_vertex_fragment(vertex_id, patch);
            }
        }

        // 批量处理删除   先处理删除，再处理创建，因为卡片类型切换的场景需要
        for (card_type_id, vertices) in delete_by_card_type {
            // 提取 card_ids (VertexId = CardId)
            let card_ids: Vec<CardId> = vertices.iter().map(|(vid, _)| *vid).collect();

            // 使用新的公共方法批量从card_type索引中移除节点
            self.in_memory
                .remove_vertices_from_card_type_index(&card_type_id, &card_ids);

            // 清理其他相关缓存
            for (vertex_id, _) in vertices {
                self.in_memory.remove_vertex_fragment(&vertex_id);
                // card_id == vertex_id，不再需要单独的映射
                self.in_memory.remove_vertex_from_lru_cache(&vertex_id);
                self.in_memory
                    .remove_card_description_from_cache(&vertex_id);
            }
        }

        // 批量处理创建
        for (card_type_id, vertices) in create_by_card_type {
            // 收集所有需要添加的节点ID
            let vertex_ids: Vec<VertexId> =
                vertices.iter().map(|(vertex_id, _)| *vertex_id).collect();

            // 使用新的公共方法批量添加到card_type索引中
            self.in_memory
                .add_vertices_to_card_type_index(&card_type_id, &vertex_ids);

            // 批量添加到其他缓存
            for (vertex_id, vertex_fragment) in vertices {
                // card_id == vertex_id，不再需要单独的映射
                self.in_memory
                    .insert_vertex_fragment(vertex_id, vertex_fragment);
            }
        }
    }

    /// 更新边相关的缓存 - 使用批量处理优化性能
    fn update_edge_delta_item(&mut self) {
        let edge_delta = std::mem::replace(&mut self.edge_manager.delta, Default::default());

        // 按边描述符分组处理删除的边
        if !edge_delta.delete.is_empty() {
            let mut delete_groups: HashMap<EdgeDescriptor, Vec<(VertexId, VertexId)>> =
                HashMap::new();
            let mut delete_props_keys = Vec::new();

            for edge in edge_delta.delete {
                // 分组边关系
                delete_groups
                    .entry(edge.edge_descriptor)
                    .or_insert_with(Vec::new)
                    .push((edge.src_vertex_id, edge.dest_vertex_id));

                // 收集需要删除的边属性key
                let edge_key = format!(
                    "{}:{}:{}",
                    edge.src_vertex_id,
                    edge.edge_descriptor.t.as_str(),
                    edge.dest_vertex_id
                );
                delete_props_keys.push(edge_key);
            }

            // 批量删除边关系
            self.in_memory.batch_remove_edges_from_cache(&delete_groups);

            // 批量删除边属性
            for key in delete_props_keys {
                self.in_memory.remove_edge_properties(&key);
            }
        }

        // 按边描述符分组处理新创建的边
        if !edge_delta.create.is_empty() {
            let mut create_groups: HashMap<EdgeDescriptor, Vec<(VertexId, VertexId)>> =
                HashMap::new();
            let mut create_props = Vec::new();

            for edge in edge_delta.create {
                // 分组边关系
                create_groups
                    .entry(edge.edge_descriptor)
                    .or_insert_with(Vec::new)
                    .push((edge.src_vertex_id, edge.dest_vertex_id));

                // 收集需要添加的边属性
                if let Some(props) = edge.props {
                    let edge_key = format!(
                        "{}:{}:{}",
                        edge.src_vertex_id,
                        edge.edge_descriptor.t.as_str(),
                        edge.dest_vertex_id
                    );
                    create_props.push((edge_key, props));
                }
            }

            // 批量添加边关系
            self.in_memory.batch_add_edges_to_cache(&create_groups);

            // 批量添加边属性
            if !create_props.is_empty() {
                self.in_memory.batch_insert_edge_properties(create_props);
            }
        }

        // 处理更新的边（主要是属性更新）
        for edge in edge_delta.update {
            self.update_edge_in_cache(&edge);
        }
    }

    /// 更新缓存中的边信息
    fn update_edge_in_cache(&mut self, edge: &EdgeDeltaItem) {
        let edge_key = format!(
            "{}:{}:{}",
            edge.src_vertex_id,
            edge.edge_descriptor.t.as_str(),
            edge.dest_vertex_id
        );
        if let Some(props) = &edge.props {
            self.in_memory
                .insert_edge_properties(edge_key, props.clone());
        } else {
            self.in_memory.remove_edge_properties(&edge_key);
        }
    }
}

impl<'a> Transaction<'a> for RocksTransaction<'a> {
    fn create_vertex(&mut self, vertex: &mut Vertex) -> DbResult<bool> {
        // card_id 就是 vertex_id，无需额外赋值
        if self.vertex_exists(&vertex.card_id)? {
            return Ok(false);
        }
        self.vertex_manager.create(&mut self.write_batch, vertex)
    }

    fn vertex_exists(&self, vertex_id: &VertexId) -> DbResult<bool> {
        // 首先检查当前事务中是否创建了该节点
        if self.vertex_manager.delta.create.contains_key(vertex_id) {
            return Ok(true);
        }
        // 然后检查内存缓存
        if self.in_memory.contains_vertex_fragment(vertex_id) {
            Ok(true)
        } else {
            Ok(false)
        }
        // 不用检查数据库
        //self.vertex_manager.exists(vertex_id)
    }

    fn exists_edge(&self, edge: &Edge, direction: EdgeDirection) -> DbResult<bool> {
        let edge_descriptor = EdgeDescriptor {
            t: edge.t.clone(),
            direction,
        };

        // 1. 首先检查当前事务的增量变更（delta.create）
        // 这一步很重要，因为在同一事务中创建的边还没有提交到全局内存缓存
        for edge_delta_item in &self.edge_manager.delta.create {
            if edge_delta_item.edge_descriptor.t == edge_descriptor.t
                && edge_delta_item.edge_descriptor.direction == edge_descriptor.direction
                && edge_delta_item.src_vertex_id == edge.src_id
                && edge_delta_item.dest_vertex_id == edge.dest_id
            {
                return Ok(true);
            }
        }

        // 2. 然后检查全局内存缓存（edge_delta_item）
        if self
            .in_memory
            .edge_exists_in_cache(&edge_descriptor, &edge.src_id, &edge.dest_id)
        {
            return Ok(true);
        }

        Ok(false)
    }

    fn create_edge(&mut self, edge: &Edge) -> DbResult<bool> {
        //如果边已经存在，返回false
        if self.exists_edge(edge, EdgeDirection::Src)? {
            return Ok(false);
        }
        //校验两侧的节点是否存在，如果不存在，返回错误
        if !self.vertex_exists(&edge.src_id)? {
            return Err(DbError::VertexNotExists(edge.src_id));
        }
        if !self.vertex_exists(&edge.dest_id)? {
            return Err(DbError::VertexNotExists(edge.dest_id));
        }

        // 创建边 - EdgeManager.create方法会同时处理正向和反向边的缓存更新
        self.edge_manager.create(&mut self.write_batch, edge)
    }

    fn update_vertex(&mut self, vertex: &Vertex) -> DbResult<bool> {
        // 先检查节点是否存在
        if !self.vertex_exists(&vertex.card_id)? {
            return Ok(false);
        }

        // 更新节点信息
        self.vertex_manager.update(&mut self.write_batch, vertex)?;

        // 记录更新信息到内存缓存
        self.vertex_manager.delta.update.insert(
            vertex.card_id,
            VertexFragment::new(
                vertex.card_type_id,
                vertex.container_id,
                vertex.state,
                vertex.card_id.clone(),
                vertex.stream_info.stream_id,
                vertex.stream_info.status_id,
            ),
        );

        Ok(true)
    }

    fn update_edge(&mut self, edge: &Edge, direction: EdgeDirection) -> DbResult<bool> {
        // 检查边是否存在
        if !self.exists_edge(edge, direction)? {
            return Ok(false);
        }

        // 更新边信息
        self.edge_manager.update(&mut self.write_batch, edge)
    }

    fn delete_vertices(&mut self, vertices: &Vec<VertexId>) -> DbResult<()> {
        for vertex_id in vertices {
            // 检查节点是否存在
            if !self.vertex_exists(vertex_id)? {
                continue; // 如果节点不存在，则跳过
            }

            // 获取节点的完整信息，以便在缓存中更新
            let vertices = self.get_specific_vertices(&vec![*vertex_id])?;
            if let Some(vertex) = vertices.first() {
                // 记录删除信息到内存缓存
                self.vertex_manager.delta.delete.insert(
                    *vertex_id,
                    VertexFragment::new(
                        vertex.card_type_id,
                        vertex.container_id,
                        vertex.state,
                        vertex.card_id.clone(),
                        vertex.stream_info.stream_id,
                        vertex.stream_info.status_id,
                    ),
                );
            }

            // 从数据库中删除节点
            self.vertex_manager
                .delete(&mut self.write_batch, vertex_id)?;
        }

        Ok(())
    }

    fn delete_edges_by_edge_descriptors(
        &mut self,
        edge_descriptors: &Vec<EdgeDescriptor>,
    ) -> DbResult<()> {
        for edge_descriptor in edge_descriptors {
            // 获取符合边描述符的所有边
            let edges = self.edge_manager.get_edges_by_descriptor(edge_descriptor)?;

            // 删除每一条边
            for edge in edges {
                self.edge_manager.delete(&mut self.write_batch, &edge)?;
            }
        }

        Ok(())
    }

    fn get_vertex_by_id(&self, vid: &VertexId) -> Option<Arc<Vertex>> {
        if let Some(v) = self.in_memory.get_vertex_from_lru_cache(vid) {
            Some(v)
        } else {
            let vertex = self.vertex_manager.get_vertex_by_id(&vid)?;
            let arc = Arc::new(vertex);
            //回填缓存
            self.in_memory.insert_vertex_to_lru_cache(*vid, arc.clone());
            Some(arc)
        }
    }

    fn get_vertex_by_id_directly(&self, vid: &VertexId) -> Option<Arc<Vertex>> {
        let vertex = self.vertex_manager.get_vertex_by_id(&vid)?;
        let arc = Arc::new(vertex);
        //回填缓存
        self.in_memory.insert_vertex_to_lru_cache(*vid, arc.clone());
        Some(arc)
    }

    fn get_specific_vertices(&self, ids: &Vec<VertexId>) -> DbResult<Vec<Arc<Vertex>>> {
        if ids.is_empty() {
            warn!("get_specific_vertices return empty because given ids is empty");
            return Ok(vec![]);
        }
        //先从缓存中获取，如果缓存里没有，再查db，然后回填缓存
        let mut result = Vec::with_capacity(ids.len());
        let mut no_hit_ids = if ids.len() > 1024 {
            Vec::with_capacity(ids.len() / 3) //如果数量太大，我们假设有1/3未命中缓存
        } else {
            Vec::with_capacity(ids.len())
        };
        for vid in ids {
            if let Some(v) = self.in_memory.get_vertex_from_lru_cache(vid) {
                result.push(v);
            } else {
                no_hit_ids.push(*vid);
            }
        }
        if !no_hit_ids.is_empty() {
            let vertices = self.vertex_manager.get_specific_vertices(&no_hit_ids)?;
            if !vertices.is_empty() {
                for v in vertices {
                    let vid = v.card_id;
                    let v_arc = Arc::new(v);
                    result.push(Arc::clone(&v_arc));
                    //回填缓存
                    self.in_memory
                        .insert_vertex_to_lru_cache(vid, v_arc.clone());
                }
            }
        }
        Ok(result)
    }

    fn query_vertices(&self, query: VertexQuery) -> DbResult<Vec<Arc<Vertex>>> {
        let start_time = if tracing::enabled!(tracing::Level::DEBUG) {
            Some(Instant::now())
        } else {
            None
        };
        let has_id_scope = query.card_ids.is_some() && !query.card_ids.as_ref().unwrap().is_empty()
            || query.card_ids.is_some() && !query.card_ids.as_ref().unwrap().is_empty();

        // 验证查询参数
        if query.card_type_ids.is_empty() && (!has_id_scope) {
            return Err(DbError::ValidationError(ValidationError::EmptyInput(
                "both card_type_ids and card_ids/vertex_ids cannot be empty at the same time".to_string(),
            )));
        }

        let mut vertex_ids: Vec<VertexId> = Vec::new();
        let need_card_type_filter = !query.card_type_ids.is_empty() && has_id_scope;

        // 优先使用ID
        // card_ids 就是 vertex_ids，直接使用
        if let Some(card_ids) = &query.card_ids {
            vertex_ids.extend(card_ids);
        } else {
            // 只有在没有指定卡片ID的情况下，才使用卡片类型ID查询
            for card_type_id in &query.card_type_ids {
                // 使用闭包处理节点ID集合，避免克隆整个集合
                self.in_memory.with_vertex_ids_by_card_type_id(card_type_id, |ids| {
                    vertex_ids.extend(ids);
                });
            }
        }

        // 如果没有找到任何节点ID，直接返回空结果
        if vertex_ids.is_empty() {
            return Ok(Vec::new());
        }

        if let Some(start_time) = start_time {
            let current_time = Instant::now();
            debug!(
                "query {} vertex ids before filter, cost: {:?}",
                vertex_ids.len(),
                current_time.duration_since(start_time)
            );
        }

        let result = vertex_ids
            .iter()
            .map(|vid| {
                let opt = self.in_memory.get_vertex_from_lru_cache(vid);
                if let Some(v_arc) = opt {
                    // 卡片类型过滤（如果是通过card_type_id进行查询的，不需要再过滤卡片类型）
                    let card_type_match = if need_card_type_filter {
                        query.card_type_ids.contains(&v_arc.card_type_id)
                    } else {
                        true
                    };
                    if !card_type_match {
                        return None;
                    }
                    // 容器ID过滤
                    let container_match = match &query.container_ids {
                        Some(container_ids) => {
                            container_ids.is_empty() || container_ids.contains(&v_arc.container_id)
                        }
                        None => true,
                    };
                    if !container_match {
                        return None;
                    }
                    // 卡片状态过滤
                    let state_match = match &query.states {
                        Some(states) => {
                            if states.is_empty() {
                                true // 状态集为空时，不过滤
                            } else {
                                // 如果状态集不为空，检查节点状态是否在集合中
                                states.contains(&v_arc.state)
                            }
                        }
                        None => true, // 未指定状态时，不过滤
                    };
                    if !state_match {
                        return None;
                    }
                    return Some(v_arc);
                } else {
                    if let Some(v) = self.in_memory.get_vertex_fragment(&vid) {
                        // 卡片类型过滤（如果是通过card_type_id进行查询的，不需要再过滤卡片类型）
                        let card_type_match = if need_card_type_filter {
                            query.card_type_ids.contains(&v.card_type_id)
                        } else {
                            true
                        };

                        if !card_type_match {
                            return None;
                        }

                        // 容器ID过滤
                        let container_match = match &query.container_ids {
                            Some(container_ids) => {
                                container_ids.is_empty() || container_ids.contains(&v.container_id)
                            }
                            None => true,
                        };
                        if !container_match {
                            return None;
                        }

                        // 卡片状态过滤
                        let state_match = match &query.states {
                            Some(states) => {
                                if states.is_empty() {
                                    true // 状态集为空时，不过滤
                                } else {
                                    // 如果状态集不为空，检查节点状态是否在集合中
                                    states.contains(&v.card_state)
                                }
                            }
                            None => true, // 未指定状态时，不过滤
                        };
                        if !state_match {
                            return None;
                        }
                        return self.get_vertex_by_id_directly(vid);
                    }
                };
                return None;
            })
            .filter(|v| v.is_some())
            .map(|v| v.unwrap())
            .collect();
        if let Some(start_time) = start_time {
            let current_time = Instant::now();
            debug!(
                "query {} vertex, cost: {:?}",
                vertex_ids.len(),
                current_time.duration_since(start_time)
            );
        }
        Ok(result)
    }

    fn query_neighbor_vertex_ids(&self, query: &NeighborQuery) -> DbResult<Vec<VertexId>> {
        if query.src_vertex_ids.is_empty() {
            return Ok(Vec::with_capacity(0));
        }

        // 收集已删除的边
        let deleted_edges = self.collect_deleted_edges(query);

        // 处理事务中新创建的边
        let mut result = self.process_created_edges(query, &deleted_edges);

        // 处理内存缓存中的边
        result.extend(self.process_cached_edges(query, &deleted_edges));

        // 去重，确保结果中没有重复的节点ID
        result.dedup();

        Ok(result)
    }

    fn query_neighbor_edges(&self, query: &NeighborQuery) -> DbResult<Vec<Edge>> {
        // 查询邻居节点ID列表，这里可以根据卡片周期状态对邻居节点进行过滤
        let vertex_ids = self.query_neighbor_vertex_ids(query)?;

        // 如果没有邻居节点，返回空集合
        if vertex_ids.is_empty() {
            return Ok(Vec::new());
        }

        // 构建边结构
        let mut edges = Vec::with_capacity(vertex_ids.len());

        for &dest_vertex_id in &vertex_ids {
            // 获取所有源节点
            for &src_vertex_id in &query.src_vertex_ids {
                // 创建边对象，不包含属性
                let edge = Edge::new(src_vertex_id, query.edge_descriptor.t, dest_vertex_id, None);
                edges.push(edge);
            }
        }

        Ok(edges)
    }

    fn query_neighbor_edges_with_props(&self, query: &NeighborQuery) -> DbResult<Vec<Edge>> {
        // 查询邻居节点ID列表，这里可以根据卡片周期状态对邻居节点进行过滤
        let vertex_ids = self.query_neighbor_vertex_ids(query)?;

        // 如果没有邻居节点，返回空集合
        if vertex_ids.is_empty() {
            return Ok(Vec::new());
        }

        // 构建边结构
        let mut edges = Vec::with_capacity(vertex_ids.len());

        for &dest_vertex_id in &vertex_ids {
            // 获取所有源节点
            for &src_vertex_id in &query.src_vertex_ids {
                // 检查边是否存在（应该存在，因为是从query_neighbor_vertex_ids获取的）
                // 直接从内存缓存获取边属性
                let edge_key = format!(
                    "{}:{}:{}",
                    src_vertex_id,
                    query.edge_descriptor.t.as_str(),
                    dest_vertex_id
                );

                // 获取边属性（如果有）
                let props =
                    if let Some(cached_props) = self.in_memory.get_edge_properties(&edge_key) {
                        Some(cached_props.clone())
                    } else {
                        None
                    };

                // 创建边对象，包含属性
                let edge = Edge::new(
                    src_vertex_id,
                    query.edge_descriptor.t,
                    dest_vertex_id,
                    props,
                );

                edges.push(edge);
            }
        }

        Ok(edges)
    }

    fn commit(mut self) -> DbResult<()> {
        // 更新 ID 计数器，以及将本次事务的数据更新写入数据库
        self.update_id_counter_and_write_db()?;

        // 更新节点缓存
        self.update_vertex_cache();

        // 更新边缓存
        self.update_edge_delta_item();

        Ok(())
    }

    fn rollback(self) -> DbResult<()> {
        match self.inner_txn.rollback() {
            Ok(_) => Ok(()),
            Err(err) => Err(DbError::TransactionError(Box::new(err))),
        }
    }

    fn get_vertex_count(&self) -> u64 {
        todo!()
    }

    fn card_exists(&self, card_id: CardId) -> bool {
        // CardId = VertexId，直接检查节点碎片缓存
        self.in_memory.contains_vertex_fragment(&card_id)
    }

    fn get_card_descriptions(
        &self,
        vertex_ids: &Vec<VertexId>,
    ) -> HashMap<VertexId, Option<Arc<String>>> {
        let mut result = HashMap::with_capacity(vertex_ids.len());
        let mut no_hit_ids = Vec::new();

        // 首先检查LRU缓存中是否已有描述
        for &vertex_id in vertex_ids {
            if let Some(desc) = self.in_memory.get_card_description_from_cache(&vertex_id) {
                // 已在缓存中，直接使用
                result.insert(vertex_id, Some(desc.clone()));
            } else {
                // 不在缓存中，加入待查询列表
                no_hit_ids.push(vertex_id);
            }
        }

        if no_hit_ids.is_empty() {
            // 全部从缓存中获取到，直接返回
            return result;
        }

        // 使用vertex_manager获取卡片描述
        if let Ok(descriptions) = self.vertex_manager.get_card_descriptions(&no_hit_ids) {
            for (vertex_id, desc_opt) in descriptions {
                if let Some(desc) = &desc_opt {
                    let desc_arc = Arc::new(desc.clone());
                    // 更新缓存
                    self.in_memory
                        .insert_card_description_to_cache(vertex_id, desc_arc.clone());
                    result.insert(vertex_id, Some(desc_arc));
                }
            }
        }

        // 确保所有的节点ID都有结果
        for &vertex_id in vertex_ids {
            if !result.contains_key(&vertex_id) {
                result.insert(vertex_id, None);
            }
        }

        result
    }

    fn get_card_description(&self, vertex_id: &VertexId) -> Option<Arc<String>> {
        if let Some(desc) = self.in_memory.get_card_description_from_cache(&vertex_id) {
            // 已在缓存中，直接返回
            return Some(desc);
        } else {
            // 不在缓存中，加入待查询列表
            if let Ok(descriptions) = self.vertex_manager.get_card_descriptions(&vec![*vertex_id]) {
                for (vertex_id, desc_opt) in descriptions {
                    if let Some(desc) = &desc_opt {
                        let desc_arc = Arc::new(desc.clone());
                        // 更新缓存
                        self.in_memory
                            .insert_card_description_to_cache(vertex_id, desc_arc.clone());
                        return Some(desc_arc);
                    }
                }
            }
        }
        None
    }

    fn delete_edge(&mut self, edge: &Edge) -> DbResult<()> {
        // 调用边管理器的delete方法删除指定的边
        self.edge_manager.delete(&mut self.write_batch, edge)?;
        Ok(())
    }

    fn get_edge_properties(
        &self,
        src_vertex_id: &VertexId,
        edge_type: &EdgeType,
        dest_vertex_id: &VertexId,
    ) -> Option<Vec<EdgeProp>> {
        // 构建边属性键
        let edge_key = format!(
            "{}:{}:{}",
            src_vertex_id,
            edge_type.as_str(),
            dest_vertex_id
        );

        // 从内存缓存中获取边属性
        if let Some(cached_props) = self.in_memory.get_edge_properties(&edge_key) {
            Some(cached_props.clone())
        } else {
            None
        }
    }
}
