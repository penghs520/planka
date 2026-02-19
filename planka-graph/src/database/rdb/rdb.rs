/**
 * RocksDB实现模块 - 提供基于RocksDB的图数据库核心实现
 *
 * 该模块实现了Database和Transaction特征，使用RocksDB作为底层存储引擎
 * 同时结合内存缓存来提高性能，支持节点和边的CRUD操作
 */
use crate::database::database::Database;
use crate::database::model::{
    CardId, CardTypeId, EdgeDescriptor, EdgeDirection, EdgeProp, Vertex, VertexId,
};
use crate::utils;
use rocksdb::{ColumnFamilyDescriptor, OptimisticTransactionDB, Options};
use std::{collections::HashMap, path::Path, time::Instant};
use tracing::{info, warn};

use super::memory::{InMemory, VertexFragment, CacheStatistics};
use super::rdb_config::RocksDbConfig;
use super::rdb_transaction::RocksTransaction;

/// RocksDB数据库实现 - 基于RocksDB的图数据库实现
pub struct RocksDatabase {
    /// RocksDB数据库实例
    pub(crate) db: OptimisticTransactionDB,

    /// 内存缓存
    in_memory: InMemory,

    /// 数据库配置
    pub(crate) config: RocksDbConfig,
}

impl RocksDatabase {
    /// 创建一个新的RocksDB数据库实例
    ///
    /// # 参数
    /// * `path` - 数据库文件路径
    ///
    /// # 返回
    /// 数据库实例
    pub fn new<P: AsRef<Path>>(path: P) -> Self {
        let config = RocksDbConfig::new(path.as_ref().to_str().unwrap().to_string());
        Self::new_db_with_config(config)
    }

    /// 使用配置创建一个新的RocksDB数据库实例
    ///
    /// # 参数
    /// * `config` - 数据库配置
    ///
    /// # 返回
    /// 数据库实例
    pub fn new_db_with_config(config: RocksDbConfig) -> Self {
        // 创建默认配置
        let mut opts = Options::default();
        opts.create_if_missing(true);
        opts.create_missing_column_families(true);

        // 应用配置参数
        opts.set_max_open_files(config.max_open_files as i32);
        opts.set_max_background_jobs(config.max_background_jobs as i32);
        opts.set_write_buffer_size(config.write_buffer_size_mb as usize * 1024 * 1024);

        // 创建列族
        let cfs = vec![
            ColumnFamilyDescriptor::new("vertex_cf", Options::default()),
            ColumnFamilyDescriptor::new("vertex_desc_cf", Options::default()),
            ColumnFamilyDescriptor::new("vertex_index_cf", Options::default()),
            ColumnFamilyDescriptor::new("edge_cf", Options::default()),
        ];

        // 打开数据库
        let db = OptimisticTransactionDB::open_cf_descriptors(&opts, &config.path, cfs).unwrap();
        // 创建内存缓存结构
        let in_memory = InMemory::new(
            100,                          // card_type_index_capacity
            10000,                        // vertex_fragment_capacity
            config.vertex_lru_cache_size, // vertex_lru_capacity
            500,                          // edge_delta_item_capacity
        );

        let rocks_db = RocksDatabase {
            db,
            in_memory,
            config,
        };

        // 加载内存数据
        //rocks_db.load_memory_data();

        rocks_db
    }

    /// 加载内存数据 - 从RocksDB加载数据到内存缓存
    ///
    /// 该方法在数据库启动时调用，用于初始化内存缓存。
    /// 按照以下顺序加载数据：
    /// 1. 加载节点数据（包括节点类型索引和节点缓存）
    /// 2. 加载边数据（包括边关系和边属性）
    /// 3. 加载全局计数器
    pub fn load_memory_data(&self) {
        info!("Loading data...");
        let start = Instant::now();
        // 清除数据
        self.in_memory.clear_all();
        // id_counter 不再需要，因为 card_id 就是 vertex_id
        // 保留这段代码以备将来可能的向后兼容需求

        // 加载节点数据 - 使用批量处理优化性能
        let vertex_cf = self.db.cf_handle("vertex_cf").unwrap();
        let iter = self
            .db
            .iterator_cf(&vertex_cf, rocksdb::IteratorMode::Start);

        let mut vertex_cont = 0;
        let mut vertices_by_card_type: HashMap<CardTypeId, Vec<(CardId, VertexFragment)>> =
            HashMap::with_capacity(1024);

        // 第一阶段：收集所有节点数据，按card_type_id分组
        for item in iter {
            if let Ok((_, value)) = item {
                if let Ok(vertex) = bincode::deserialize::<Vertex>(&value) {
                    vertex_cont += 1;

                    let card_id = vertex.card_id;
                    info!("Vertex {} loaded {:?}", card_id, vertex.state);
                    let vertex_fragment = VertexFragment::new(
                        vertex.card_type_id,
                        vertex.container_id,
                        vertex.state,
                        card_id,
                        vertex.stream_info.stream_id,
                        vertex.stream_info.status_id,
                    );

                    // 按card_type_id分组收集节点数据 (CardId = VertexId)
                    vertices_by_card_type
                        .entry(vertex.card_type_id)
                        .or_insert_with(Vec::new)
                        .push((card_id, vertex_fragment));
                }
            }
        }

        // 第二阶段：批量添加节点到缓存
        if !vertices_by_card_type.is_empty() {
            self.in_memory.batch_add_vertices_to_cache(&vertices_by_card_type);
        }
        info!("Vertex loading completed, loaded {} vertices", vertex_cont);

        // 加载边数据和边属性 - 使用批量处理优化性能
        let edge_cf = self.db.cf_handle("edge_cf").unwrap();
        let iter = self.db.iterator_cf(&edge_cf, rocksdb::IteratorMode::Start);

        let mut edge_cont = 0;
        let mut edges_by_descriptor: HashMap<EdgeDescriptor, Vec<(VertexId, VertexId)>> =
            HashMap::with_capacity(256);
        let mut edge_props = Vec::with_capacity(1024);

        // 第一阶段：收集所有边数据，按边描述符分组
        for item in iter {
            if let Ok((key, value)) = item {
                edge_cont += 1;
                use std::io::Cursor;
                let mut cursor = Cursor::new(&key);

                // 从key中解析边信息
                let edge_type = utils::read_identifier(&mut cursor);
                let direction = utils::read_edge_direction(&mut cursor);
                let src_id = utils::read_vertex_id(&mut cursor);
                let dest_id = utils::read_vertex_id(&mut cursor);

                // 按边描述符分组收集边关系
                let edge_descriptor = EdgeDescriptor {
                    t: edge_type,
                    direction,
                };

                edges_by_descriptor
                    .entry(edge_descriptor)
                    .or_insert_with(Vec::new)
                    .push((src_id, dest_id));

                // 如果是正向边且有属性值，则收集边属性
                if direction == EdgeDirection::Src && !value.is_empty() {
                    if let Ok(props) = bincode::deserialize::<Vec<EdgeProp>>(&value) {
                        let edge_key = format!("{}:{}:{}", src_id, edge_type.as_str(), dest_id);
                        edge_props.push((edge_key, props));
                    } else {
                        warn!("Failed to deserialize edge properties");
                    }
                }
            }
        }

        if !edges_by_descriptor.is_empty() {
            self.in_memory
                .batch_add_edges_to_cache(&edges_by_descriptor);
        }

        if !edge_props.is_empty() {
            self.in_memory.batch_insert_edge_properties(edge_props);
        }
        info!("Edge loading completed, loaded {} edges", edge_cont);
        info!("Loading data completed in {:?}", start.elapsed());
    }

    /// 创建数据库checkpoint快照
    ///
    /// # 参数
    /// * `checkpoint_path` - checkpoint目标路径
    ///
    /// # 返回
    /// 创建的checkpoint路径和数据库统计信息
    pub fn create_checkpoint<P: AsRef<Path>>(
        &self,
        checkpoint_path: P,
    ) -> Result<String, Box<dyn std::error::Error>> {
        use std::time::{SystemTime, UNIX_EPOCH};

        let timestamp = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs();

        // 创建checkpoint base目录
        std::fs::create_dir_all(&checkpoint_path)?;

        let checkpoint_dir = checkpoint_path
            .as_ref()
            .join(format!("snapshot_{}", timestamp));

        // 如果目录已存在，先删除它
        if checkpoint_dir.exists() {
            std::fs::remove_dir_all(&checkpoint_dir)?;
        }

        // 创建RocksDB checkpoint
        let checkpoint = rocksdb::checkpoint::Checkpoint::new(&self.db)?;
        checkpoint.create_checkpoint(&checkpoint_dir)?;

        info!(
            "Created database snapshot by rocksdb checkpoint at: {}",
            checkpoint_dir.display()
        );
        Ok(checkpoint_dir.to_string_lossy().to_string())
    }

    /// 获取数据库路径
    pub fn get_db_path(&self) -> String {
        self.config.path.clone()
    }

    /// 获取数据库统计信息
    ///
    /// # 参数
    /// * `include_cache_details` - 是否包含详细缓存信息
    ///
    /// # 返回
    /// 数据库统计信息
    pub fn get_database_stats(&self, include_cache_details: bool) -> DatabaseStatsInfo {
        let cache_stats = self.in_memory.get_cache_stats();
        
        let total_vertices = self.in_memory.get_total_vertices_count();
        let total_edges = self.in_memory.get_total_edges_count();
        let vertex_types_count = self.in_memory.get_vertex_types_count();
        let edge_types_count = self.in_memory.get_edge_types_count();

        // 获取RocksDB统计信息（可选）
        let rocksdb_stats = if include_cache_details {
            // 获取RocksDB的内部统计信息
            match self.db.property_value("rocksdb.stats") {
                Ok(Some(stats)) => Some(stats),
                _ => Some("RocksDB stats not available".to_string()),
            }
        } else {
            None
        };

        DatabaseStatsInfo {
            total_vertices,
            total_edges,
            vertex_types_count,
            edge_types_count,
            cache_stats,
            rocksdb_stats,
        }
    }

}


/// 实现Database特征 - 提供事务创建功能
impl Database for RocksDatabase {
    type Transaction<'a> = RocksTransaction<'a>;

    fn transaction(&self) -> Self::Transaction<'_> {
        RocksTransaction::new(&self.db, &self.in_memory)
    }
}

/// 数据库统计信息结构体
#[derive(Debug, Clone)]
pub struct DatabaseStatsInfo {
    pub total_vertices: u64,
    pub total_edges: u64,
    pub vertex_types_count: u32,
    pub edge_types_count: u32,
    pub cache_stats: CacheStatistics,
    pub rocksdb_stats: Option<String>,
}
