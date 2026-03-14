/**
 * 内存缓存模块 - 提供高效的内存中数据结构
 *
 * 该模块定义了各种内存缓存和数据结构，用于加速图数据库的查询操作
 * 并减少对底层存储的访问，提高整体性能
 */
use std::{
    collections::{HashMap, HashSet},
    hash::BuildHasherDefault,
    sync::Arc,
};

use dashmap::DashMap;
use foyer::{Cache as FoyerCache, CacheBuilder};
use fxhash::{FxBuildHasher, FxHasher};
use smallvec::SmallVec;

use crate::database::model::{
    CardId, CardState, CardTypeId, ContainerId, EdgeDescriptor, EdgeProp, StatusId, StreamId,
    Vertex, VertexId,
};

/// 定义使用FxHasher的DashMap类型，提高哈希性能
type FxDashMap<K, V> = DashMap<K, V, BuildHasherDefault<FxHasher>>;

/// 定义使用FxHasher的HashMap类型，提高哈希性能
pub(super) type FxHashMap<K, V> = HashMap<K, V, BuildHasherDefault<FxHasher>>;

/// 定义使用FxHasher的HashSet类型，提高哈希性能
pub type FxHashSet<T> = HashSet<T, BuildHasherDefault<FxHasher>>;

/// 内存数据结构 - 包含多种缓存和索引
///
/// 为了提高性能，该结构在内存中维护多种索引和缓存，
/// 使常见的查询操作不必总是访问磁盘数据库
pub(super) struct InMemory {
    /// 节点类型索引 - 全量缓存每种节点类型包含的卡片ID
    card_type_index: FxDashMap<CardTypeId, FxHashSet<CardId>>,

    /// 全量的节点碎片缓存 - 只缓存节点的部分信息以减少内存压力
    /// 使用CardId作为键（CardId = VertexId = u64）
    vertex_fragment_cache: FxDashMap<CardId, VertexFragment>,

    /// 热点节点的完整信息缓存 - 使用LRU策略
    vertex_lru_cache: FoyerCache<CardId, Arc<Vertex>, BuildHasherDefault<FxHasher>>,

    /// 热点卡片描述缓存 - 使用LRU策略，避免频繁读取大文本字段
    desc_lru_cache: FoyerCache<CardId, Arc<String>, BuildHasherDefault<FxHasher>>,

    /// 边关系缓存 - 全量缓存所有边关系
    /// 使用CardId作为键
    edge_cache: FxDashMap<EdgeDescriptor, FxHashMap<CardId, SmallVec<[CardId; 4]>>>,

    /// 全量的边属性缓存 - 只有很少数量的边有属性，所以这里全量缓存
    edge_props_cache: FxDashMap<String, Vec<EdgeProp>>,
}

#[derive(Debug)]
pub(super) struct EdgeDeltaItem {
    /// 源节点ID
    pub src_vertex_id: VertexId,

    /// 边描述符（类型和方向）
    pub edge_descriptor: EdgeDescriptor,

    /// 目标节点ID
    pub dest_vertex_id: VertexId,

    /// 边属性
    pub props: Option<Vec<EdgeProp>>,
}

/// 节点碎片 - 全量缓存节点的核心属性，避免过多属性造成内存负担
#[derive(Debug, Eq, PartialEq, Clone)]
pub(super) struct VertexFragment {
    /// 卡片ID
    pub card_id: CardId,

    /// 节点使用类型ID
    pub card_type_id: CardTypeId,

    /// 容器ID
    pub container_id: ContainerId,

    /// 节点状态
    pub card_state: CardState,

    /// 价值流ID
    pub stream_id: StreamId,

    /// 状态ID
    pub status_id: StatusId,
}

impl VertexFragment {
    pub fn new(
        card_type_id: CardTypeId,
        container_id: ContainerId,
        card_state: CardState,
        card_id: CardId,
        stream_id: StreamId,
        status_id: StatusId,
    ) -> Self {
        Self {
            card_type_id,
            card_state,
            container_id,
            card_id,
            stream_id,
            status_id,
        }
    }
}

impl InMemory {
    /// 清空所有内存缓存和索引
    ///
    /// 在从checkpoint恢复时调用，清除当前的内存状态
    pub fn clear_all(&self) {
        tracing::info!("Clearing all memory caches and indices");
        self.card_type_index.clear();
        self.vertex_fragment_cache.clear();
        self.vertex_lru_cache.clear();
        self.desc_lru_cache.clear();
        self.edge_cache.clear();
        self.edge_props_cache.clear();
        tracing::info!("All memory caches and indices cleared");
    }

    /// 创建新的InMemory实例
    pub fn new(
        card_type_index_capacity: usize,
        vertex_fragment_cache_capacity: usize,
        vertex_lru_capacity: u64,
        edge_delta_item_capacity: usize,
    ) -> Self {
        Self {
            card_type_index: FxDashMap::with_capacity_and_hasher(
                card_type_index_capacity,
                BuildHasherDefault::<FxHasher>::default(),
            ),
            vertex_fragment_cache: FxDashMap::with_capacity_and_hasher(
                vertex_fragment_cache_capacity,
                BuildHasherDefault::<FxHasher>::default(),
            ),
            vertex_lru_cache: CacheBuilder::new(vertex_lru_capacity as usize)
                .with_hash_builder(FxBuildHasher::default())
                .build(),
            desc_lru_cache: CacheBuilder::new(50000)
                .with_hash_builder(FxBuildHasher::default())
                .build(),
            edge_cache: FxDashMap::with_capacity_and_hasher(
                edge_delta_item_capacity,
                BuildHasherDefault::<FxHasher>::default(),
            ),
            edge_props_cache: FxDashMap::with_hasher(BuildHasherDefault::<FxHasher>::default()),
        }
    }

    /// 使用闭包处理指定卡片类型的节点ID集合，避免克隆
    ///
    /// # 参数
    /// * `card_type_id` - 卡片类型ID
    /// * `f` - 处理节点ID集合的闭包
    ///
    /// # 返回
    /// 如果找到指定的卡片类型，返回闭包的执行结果，否则返回None
    pub fn with_vertex_ids_by_card_type_id<F, R>(&self, card_type_id: &CardTypeId, f: F) -> Option<R>
    where
        F: FnOnce(&FxHashSet<VertexId>) -> R,
    {
        self.card_type_index.get(card_type_id).map(|entry| f(&entry))
    }

    /// 从card_type索引中移除指定的节点ID
    ///
    /// # 参数
    /// * `card_type_id` - 卡片类型ID
    /// * `vertex_id` - 要移除的节点ID
    ///
    /// # 返回
    /// 如果成功移除返回true，否则返回false
    pub fn remove_vertex_from_card_type_index(&self, card_type_id: &CardTypeId, vertex_id: &VertexId) -> bool {
        if let Some(mut set) = self.card_type_index.get_mut(card_type_id) {
            set.remove(vertex_id)
        } else {
            false
        }
    }

    /// 向card_type索引中添加节点ID
    ///
    /// # 参数
    /// * `card_type_id` - 卡片类型ID
    /// * `vertex_id` - 要添加的节点ID
    pub fn add_vertex_to_card_type_index(&self, card_type_id: &CardTypeId, vertex_id: &VertexId) {
        self.card_type_index
            .entry(*card_type_id)
            .or_insert_with(FxHashSet::default)
            .insert(*vertex_id);
    }

    /// 批量向card_type索引中添加节点ID
    ///
    /// # 参数
    /// * `card_type_id` - 卡片类型ID
    /// * `vertex_ids` - 要添加的节点ID列表
    pub fn add_vertices_to_card_type_index(&self, card_type_id: &CardTypeId, vertex_ids: &[VertexId]) {
        let mut set = self
            .card_type_index
            .entry(*card_type_id)
            .or_insert_with(FxHashSet::default);
        for &vertex_id in vertex_ids {
            set.insert(vertex_id);
        }
    }

    /// 批量从card_type索引中移除节点ID
    ///
    /// # 参数
    /// * `card_type_id` - 卡片类型ID
    /// * `card_ids` - 要移除的卡片ID列表 (CardId = VertexId)
    pub fn remove_vertices_from_card_type_index(&self, card_type_id: &CardTypeId, card_ids: &[CardId]) {
        if let Some(mut set) = self.card_type_index.get_mut(card_type_id) {
            for &card_id in card_ids {
                set.remove(&card_id);
            }
        }
    }

    /// 从热点节点LRU缓存中获取节点
    ///
    /// # 参数
    /// * `vertex_id` - 节点ID
    ///
    /// # 返回
    /// 节点引用，如果不存在则返回None
    pub fn get_vertex_from_lru_cache(&self, vertex_id: &VertexId) -> Option<Arc<Vertex>> {
        self.vertex_lru_cache
            .get(vertex_id)
            .map(|entry| entry.value().clone())
    }

    /// 将节点插入到热点节点LRU缓存中
    ///
    /// # 参数
    /// * `vertex_id` - 节点ID
    /// * `vertex` - 节点数据
    pub fn insert_vertex_to_lru_cache(&self, vertex_id: VertexId, vertex: Arc<Vertex>) {
        self.vertex_lru_cache.insert(vertex_id, vertex);
    }

    /// 从热点节点LRU缓存中移除节点
    ///
    /// # 参数
    /// * `vertex_id` - 节点ID
    pub fn remove_vertex_from_lru_cache(&self, vertex_id: &VertexId) {
        self.vertex_lru_cache.remove(vertex_id);
    }

    /// 从描述LRU缓存中获取卡片描述
    ///
    /// # 参数
    /// * `vertex_id` - 节点ID
    ///
    /// # 返回
    /// 卡片描述，如果不存在则返回None
    pub fn get_card_description_from_cache(&self, vertex_id: &VertexId) -> Option<Arc<String>> {
        self.desc_lru_cache
            .get(vertex_id)
            .map(|entry| entry.value().clone())
    }

    /// 将卡片描述插入到描述LRU缓存中
    ///
    /// # 参数
    /// * `vertex_id` - 节点ID
    /// * `description` - 卡片描述
    pub fn insert_card_description_to_cache(&self, vertex_id: VertexId, description: Arc<String>) {
        self.desc_lru_cache.insert(vertex_id, description);
    }

    /// 从描述LRU缓存中移除卡片描述
    ///
    /// # 参数
    /// * `vertex_id` - 节点ID
    pub fn remove_card_description_from_cache(&self, vertex_id: &VertexId) {
        self.desc_lru_cache.remove(vertex_id);
    }

    /// 获取节点补丁信息
    ///
    /// # 参数
    /// * `vertex_id` - 节点ID
    ///
    /// # 返回
    /// 节点补丁信息的引用，如果不存在则返回None
    pub fn get_vertex_fragment(
        &self,
        vertex_id: &VertexId,
    ) -> Option<dashmap::mapref::one::Ref<'_, VertexId, VertexFragment>> {
        self.vertex_fragment_cache.get(vertex_id)
    }

    /// 插入节点补丁信息
    ///
    /// # 参数
    /// * `vertex_id` - 节点ID
    /// * `fragment` - 节点补丁信息
    pub fn insert_vertex_fragment(&self, vertex_id: VertexId, fragment: VertexFragment) {
        self.vertex_fragment_cache.insert(vertex_id, fragment);
    }

    /// 移除节点补丁信息
    ///
    /// # 参数
    /// * `vertex_id` - 节点ID
    ///
    /// # 返回
    /// 被移除的节点补丁信息，如果不存在则返回None
    pub fn remove_vertex_fragment(
        &self,
        vertex_id: &VertexId,
    ) -> Option<(VertexId, VertexFragment)> {
        self.vertex_fragment_cache.remove(vertex_id)
    }

    /// 检查是否包含指定的节点补丁信息
    ///
    /// # 参数
    /// * `vertex_id` - 节点ID
    ///
    /// # 返回
    /// 如果包含返回true，否则返回false
    pub fn contains_vertex_fragment(&self, vertex_id: &VertexId) -> bool {
        self.vertex_fragment_cache.contains_key(vertex_id)
    }

    /// 根据边描述符和源节点ID查询目标节点列表
    ///
    /// # 参数
    /// * `edge_descriptor` - 边描述符
    /// * `src_vertex_id` - 源节点ID
    /// * `f` - 处理目标节点列表的闭包
    ///
    /// # 返回
    /// 如果找到目标节点列表，返回闭包的执行结果，否则返回None
    pub fn with_edge_targets<F, R>(
        &self,
        edge_descriptor: &EdgeDescriptor,
        src_vertex_id: &VertexId,
        f: F,
    ) -> Option<R>
    where
        F: FnOnce(&SmallVec<[VertexId; 4]>) -> R,
    {
        let src_map = self.edge_cache.get(edge_descriptor)?;
        let targets = src_map.get(src_vertex_id)?;
        Some(f(targets))
    }

    /// 检查边缓存中是否存在指定的边
    ///
    /// # 参数
    /// * `edge_descriptor` - 边描述符
    /// * `src_vertex_id` - 源节点ID
    /// * `dest_vertex_id` - 目标节点ID
    ///
    /// # 返回
    /// 如果边存在返回true，否则返回false
    pub fn edge_exists_in_cache(
        &self,
        edge_descriptor: &EdgeDescriptor,
        src_vertex_id: &VertexId,
        dest_vertex_id: &VertexId,
    ) -> bool {
        if let Some(src_map) = self.edge_cache.get(edge_descriptor) {
            if let Some(dest_vec) = src_map.get(src_vertex_id) {
                return dest_vec.contains(dest_vertex_id);
            }
        }
        false
    }

    /// 批量添加边到缓存 - 按边描述符分组处理，减少锁获取次数
    ///
    /// # 参数
    /// * `edges_by_descriptor` - 按边描述符分组的边列表
    pub fn batch_add_edges_to_cache(
        &self,
        edges_by_descriptor: &HashMap<EdgeDescriptor, Vec<(VertexId, VertexId)>>,
    ) {
        for (edge_descriptor, edges) in edges_by_descriptor {
            // 一次性获取或创建该边描述符的映射，然后批量处理所有边
            let mut entry = self
                .edge_cache
                .entry(*edge_descriptor)
                .or_insert_with(|| FxHashMap::default());

            for &(src_vertex_id, dest_vertex_id) in edges {
                match entry.get_mut(&src_vertex_id) {
                    Some(dest_vec) => {
                        if !dest_vec.contains(&dest_vertex_id) {
                            dest_vec.push(dest_vertex_id);
                        }
                    }
                    None => {
                        let mut vec = SmallVec::new();
                        vec.push(dest_vertex_id);
                        entry.insert(src_vertex_id, vec);
                    }
                }
            }
        }
    }

    /// 批量从缓存中移除边 - 按边描述符分组处理，减少锁获取次数
    ///
    /// # 参数
    /// * `edges_by_descriptor` - 按边描述符分组的边列表
    pub fn batch_remove_edges_from_cache(
        &self,
        edges_by_descriptor: &HashMap<EdgeDescriptor, Vec<(VertexId, VertexId)>>,
    ) {
        for (edge_descriptor, edges) in edges_by_descriptor {
            // 一次性获取该边描述符的可变映射，然后批量处理所有边
            if let Some(mut entry) = self.edge_cache.get_mut(edge_descriptor) {
                for &(src_vertex_id, dest_vertex_id) in edges {
                    if let Some(dest_vec) = entry.get_mut(&src_vertex_id) {
                        if let Some(pos) = dest_vec.iter().position(|&id| id == dest_vertex_id) {
                            dest_vec.remove(pos);
                        }
                    }
                }
            }
        }
    }

    /// 批量添加节点到内存缓存 - 按card_type_id分组处理，减少锁获取次数
    ///
    /// # 参数
    /// * `vertices_by_card_type` - 按节点类型ID分组的节点数据 (CardId = VertexId)
    pub fn batch_add_vertices_to_cache(
        &self,
        vertices_by_card_type: &HashMap<CardTypeId, Vec<(CardId, VertexFragment)>>,
    ) {
        // 批量添加到card_type索引
        for (card_type_id, vertices) in vertices_by_card_type {
            // 收集该card_type_id下的所有卡片ID (CardId = VertexId)
            let card_ids: Vec<CardId> = vertices.iter().map(|(card_id, _)| *card_id).collect();
            self.add_vertices_to_card_type_index(card_type_id, &card_ids);
        }

        // 批量添加节点补丁信息
        for vertices in vertices_by_card_type.values() {
            for (card_id, vertex_fragment) in vertices {
                self.insert_vertex_fragment(*card_id, vertex_fragment.clone());
            }
        }
    }

    /// 插入边属性到缓存
    ///
    /// # 参数
    /// * `edge_key` - 边属性的键
    /// * `props` - 边属性列表
    pub fn insert_edge_properties(&self, edge_key: String, props: Vec<EdgeProp>) {
        self.edge_props_cache.insert(edge_key, props);
    }

    /// 从缓存中移除边属性
    ///
    /// # 参数
    /// * `edge_key` - 边属性的键
    ///
    /// # 返回
    /// 被移除的边属性，如果不存在则返回None
    pub fn remove_edge_properties(&self, edge_key: &str) -> Option<(String, Vec<EdgeProp>)> {
        self.edge_props_cache.remove(edge_key)
    }

    /// 获取边属性
    ///
    /// # 参数
    /// * `edge_key` - 边属性的键
    ///
    /// # 返回
    /// 边属性的引用，如果不存在则返回None
    pub fn get_edge_properties(
        &self,
        edge_key: &str,
    ) -> Option<dashmap::mapref::one::Ref<'_, String, Vec<EdgeProp>>> {
        self.edge_props_cache.get(edge_key)
    }

    /// 批量插入边属性
    ///
    /// # 参数
    /// * `edge_props` - 边属性列表，每个元素是(edge_key, props)元组
    pub fn batch_insert_edge_properties(&self, edge_props: Vec<(String, Vec<EdgeProp>)>) {
        for (edge_key, props) in edge_props {
            self.edge_props_cache.insert(edge_key, props);
        }
    }

    /// 获取内存缓存统计信息
    ///
    /// # 返回
    /// 包含各种缓存大小的统计信息
    pub fn get_cache_stats(&self) -> CacheStatistics {
        CacheStatistics {
            vertex_lru_cache_size: self.vertex_lru_cache.usage(),
            desc_lru_cache_size: self.desc_lru_cache.usage(),
        }
    }

    /// 获取节点类型总数
    ///
    /// # 返回
    /// 节点类型数量
    pub fn get_vertex_types_count(&self) -> u32 {
        self.card_type_index.len() as u32
    }

    /// 获取边类型总数
    ///
    /// # 返回
    /// 边类型数量
    pub fn get_edge_types_count(&self) -> u32 {
        self.edge_cache.len() as u32
    }

    /// 获取节点总数
    ///
    /// # 返回
    /// 节点总数
    pub fn get_total_vertices_count(&self) -> u64 {
        self.card_type_index
            .iter()
            .map(|entry| entry.value().len() as u64)
            .sum()
    }

    /// 获取边总数（去重，两点确定一条边）
    ///
    /// # 返回
    /// 边总数
    pub fn get_total_edges_count(&self) -> u64 {
        let mut total_edges = 0u64;
        for entry in self.edge_cache.iter() {
            let edges_map = entry.value();
            for (_, edges_vec) in edges_map.iter() {
                total_edges += edges_vec.len() as u64;
            }
        }
        // 考虑方向，每条边在两个方向都存储，所以需要除以2
        total_edges / 2
    }


}

/// 缓存统计信息结构体
#[derive(Debug, Clone)]
pub struct CacheStatistics {
    pub vertex_lru_cache_size: usize,
    pub desc_lru_cache_size: usize,
}


