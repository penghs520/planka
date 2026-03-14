use super::memory::{FxHashMap, VertexFragment};
use crate::{
    database::{
        errors::DbResult,
        model::{Vertex, VertexId},
    },
    utils::{self, build},
};
use rocksdb::{ColumnFamilyRef, OptimisticTransactionDB, WriteBatchWithTransaction};
use std::collections::HashMap;

/// 节点管理器 - 负责节点数据的底层存储和检索
pub(super) struct VertexManager<'a> {
    /// RocksDB数据库引用
    db: &'a OptimisticTransactionDB,

    /// 节点数据的列族引用
    vertex_cf: ColumnFamilyRef<'a>,

    /// 节点描述的列族引用
    vertex_desc_cf: ColumnFamilyRef<'a>,

    /// 节点索引的列族引用
    vertex_index_cf: ColumnFamilyRef<'a>,

    /// 增量变更记录 - 当事务提交成功时将更新到内存
    pub delta: VertexDelta,
}

/// 增量更新结构 - 事务提交时需要更新到内存的变化
///
/// 跟踪事务中发生的变化，以便在提交时高效地更新内存缓存
pub(super) struct VertexDelta {
    /// 已删除的节点 - 映射节点ID到卡片ID和节点部分信息
    pub delete: FxHashMap<VertexId, VertexFragment>,

    /// 新创建的节点 - 映射节点ID到节点部分信息
    pub create: FxHashMap<VertexId, VertexFragment>,

    /// 已更新的节点 - 映射节点ID到更新后的节点部分信息
    pub update: FxHashMap<VertexId, VertexFragment>,
}

impl Default for VertexDelta {
    fn default() -> Self {
        Self {
            delete: FxHashMap::default(),
            create: FxHashMap::default(),
            update: FxHashMap::default(),
        }
    }
}

impl<'a> VertexManager<'a> {
    pub(super) fn new(db: &OptimisticTransactionDB) -> VertexManager<'_> {
        VertexManager {
            db,
            vertex_cf: db.cf_handle("vertex_cf").unwrap(),
            vertex_desc_cf: db.cf_handle("vertex_desc_cf").unwrap(),
            vertex_index_cf: db.cf_handle("vertex_index_cf").unwrap(),
            delta: VertexDelta {
                delete: Default::default(),
                create: Default::default(),
                update: Default::default(),
            },
        }
    }

    fn key(&self, id: &VertexId) -> Vec<u8> {
        build(&[utils::Component::VertexId(id)])
    }

    pub(super) fn exists(&self, id: &VertexId) -> DbResult<bool> {
        Ok(self.db.get_cf(&self.vertex_cf, self.key(id))?.is_some())
    }

    pub(super) fn create(
        &mut self,
        batch: &mut WriteBatchWithTransaction<true>,
        vertex: &Vertex,
    ) -> DbResult<bool> {
        let key = self.key(&vertex.card_id);

        //节点和节点属性存储
        batch.put_cf(&self.vertex_cf, &key, bincode::serialize(&vertex)?);

        //卡片类型索引是否需要？ 不需要，都在内存中维护

        //卡片描述存储
        if vertex.desc.changed {
            if vertex.desc.content.is_some() {
                let desc = vertex.desc.content.as_ref().unwrap();
                batch.put_cf(
                    &self.vertex_desc_cf,
                    &key,
                    bincode::serialize(desc)?,
                );
            }
        }

        self.delta.create.insert(
            vertex.card_id,
            VertexFragment::new(
                vertex.card_type_id,
                vertex.container_id,
                vertex.state,
                vertex.card_id,
                vertex.stream_info.stream_id,
                vertex.stream_info.status_id,
            ),
        );

        Ok(true)
    }

    /// 更新节点信息
    ///
    /// 根据提供的节点对象更新节点的信息
    pub(super) fn update(
        &mut self,
        batch: &mut WriteBatchWithTransaction<true>,
        vertex: &Vertex,
    ) -> DbResult<()> {
        let key = self.key(&vertex.card_id);

        // 更新节点数据
        batch.put_cf(&self.vertex_cf, &key, bincode::serialize(&vertex)?);

        // 更新节点描述信息
        if vertex.desc.changed {
            if vertex.desc.content.is_none() {
                batch.delete_cf(&self.vertex_desc_cf, &key);
            } else {
                batch.put_cf(
                    &self.vertex_desc_cf,
                    &key,
                    bincode::serialize(&vertex.desc.content.as_ref().unwrap())?,
                );
            }
        }

        Ok(())
    }

    /// 删除节点
    ///
    /// 根据节点ID删除节点及其相关数据
    pub(super) fn delete(
        &mut self,
        batch: &mut WriteBatchWithTransaction<true>,
        vertex_id: &VertexId,
    ) -> DbResult<()> {
        let key = self.key(vertex_id);

        // 删除节点数据
        batch.delete_cf(&self.vertex_cf, &key);

        // 删除节点描述
        batch.delete_cf(&self.vertex_desc_cf, &key);

        // 删除节点索引 (如果有的话)
        batch.delete_cf(&self.vertex_index_cf, &key);

        Ok(())
    }

    pub(super) fn get_specific_vertices(
        &self,
        vertex_ids: &Vec<VertexId>,
    ) -> DbResult<Vec<Vertex>> {
        // VertexId 现在是 u64，需要 8 字节
        let ids_bytes: Vec<[u8; 8]> = vertex_ids.iter().map(|x| x.to_be_bytes()).collect();
        // 存储拥有所有权的字节数组
        let vec = self
            .db
            .batched_multi_get_cf(self.vertex_cf, &ids_bytes, false);
        let mut result = Vec::with_capacity(vec.len());
        for (_, v) in vec.iter().enumerate() {
            if let Ok(Some(value)) = v {
                if let Ok(vertex) = bincode::deserialize::<Vertex>(&value) {
                    result.push(vertex);
                }
            }
        }
        Ok(result)
    }

    pub(super) fn get_vertex_by_id(&self, vid: &VertexId) -> Option<Vertex> {
        // 存储拥有所有权的字节数组
        let res = self.db.get_cf(self.vertex_cf, vid.to_be_bytes());
        match res {
            Ok(Some(v)) => {
                if let Ok(vertex) = bincode::deserialize::<Vertex>(&v) {
                    Some(vertex)
                } else {
                    None
                }
            }
            _ => None,
        }
    }

    /// 获取卡片描述
    ///
    /// 根据节点ID列表批量获取卡片描述信息
    ///
    /// # 参数
    /// * `vertex_ids` - 节点ID列表
    ///
    /// # 返回
    /// 节点ID到描述信息的映射
    pub(super) fn get_card_descriptions(
        &self,
        vertex_ids: &Vec<VertexId>,
    ) -> DbResult<HashMap<VertexId, Option<String>>> {
        let mut result = HashMap::with_capacity(vertex_ids.len());

        // 批量获取节点描述
        for &vertex_id in vertex_ids {
            let key = self.key(&vertex_id);
            if let Ok(Some(desc_value)) = self.db.get_cf(&self.vertex_desc_cf, &key) {
                if let Ok(desc) = bincode::deserialize::<String>(&desc_value) {
                    result.insert(vertex_id, Some(desc));
                } else {
                    // 反序列化失败
                    result.insert(vertex_id, None);
                }
            } else {
                // 描述不存在
                result.insert(vertex_id, None);
            }
        }

        Ok(result)
    }
}
