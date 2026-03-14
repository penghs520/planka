/**
 * RocksDB配置模块 - 提供RocksDB数据库的配置结构和默认实现
 */

/// RocksDB数据库配置结构体
#[derive(Clone, Debug)]
pub struct RocksDbConfig {
    /// 数据库文件存储路径
    pub path: String,
    
    /// 缓存大小（MB）
    pub cache_size_mb: u64,
    
    /// 写入缓冲区大小（MB）
    pub write_buffer_size_mb: u64,
    
    /// 最大打开文件数量
    pub max_open_files: i32,
    
    /// 并行压缩线程数
    pub max_background_jobs: i32,

    /// 节点LRU缓存大小（条目数, 默认100000）
    pub vertex_lru_cache_size: u64,
}

impl Default for RocksDbConfig {
    fn default() -> Self {
        Self {
            path: "/tmp/rdb".to_owned(),
            cache_size_mb: 1024,
            write_buffer_size_mb: 128,
            max_open_files: 64,
            max_background_jobs: 4,
            vertex_lru_cache_size: 10000,
        }
    }
}

impl RocksDbConfig {
    /// 创建新的配置实例，指定必要的路径
    pub fn new(path: String) -> Self {
        let mut config = Self::default();
        config.path = path;
        config
    }
} 