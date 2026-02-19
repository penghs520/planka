use serde::{Deserialize, Serialize};
use serde_yaml;
use std::fs;
use std::net::IpAddr;
use std::path::Path;
use std::str::FromStr;

/// 日志滚动配置结构体
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct LogRotation {
    /// 保留的最大日志文件数量
    pub max_files: Option<u32>,

    /// 日志滚动的小时点（0-23），如设置为0则在每天0点滚动
    pub rotation_hour: Option<u8>,
}

impl Default for LogRotation {
    fn default() -> Self {
        Self {
            max_files: Some(10),
            rotation_hour: Some(0),
        }
    }
}

/// 服务器配置结构体
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ServerConfig { 
    /// 服务器监听地址
    pub listen_address: String,
    
    /// 服务器监听端口
    pub listen_port: u16,
    
    /// 允许连接的客户端地址列表
    pub allowed_clients: Option<Vec<String>>,
    
    /// 数据库目录路径 TODO 改成私有
    pub db_path: String,

    /// 快照目录请配置到共享目录下，各服务共享快照文件，否则Follower将无法使用快照进行恢复
    pub db_snapshot_path: String,

    /// 最大保存地快照文件份数，默认：5份
    pub max_snapshot_files_to_keep: Option<u64>,

    /// 数据库缓存大小（MB）
    pub db_cache_size_mb: Option<u64>,
    
    /// RocksDB写入缓冲区大小（MB）
    pub db_write_buffer_size_mb: Option<u64>,
    
    /// RocksDB最大打开文件数量
    pub db_max_open_files: Option<i32>,
    
    /// RocksDB并行压缩线程数
    pub db_max_background_jobs: Option<i32>,
    
    /// 节点LRU缓存大小（条目数）
    pub db_vertex_lru_cache_size: Option<u64>,
    
    /// 日志级别配置，支持简单级别名称或复杂的过滤器格式
    /// 
    /// 简单格式：
    /// - "trace", "debug", "info", "warn", "error"：设置全局日志级别
    /// 
    /// 复杂格式（RUST_LOG格式）：
    /// - `crate_name=level`：为特定库设置日志级别
    /// - `path::to::module=level`：为特定模块设置日志级别
    /// - `level`：设置全局默认日志级别
    /// 
    /// 常用示例：
    /// - `pgraph=debug,info`：pgraph库使用DEBUG级别，其他库使用INFO级别
    /// - `pgraph=debug,pgraph::database=trace,info`：pgraph库使用DEBUG级别，database模块使用TRACE级别，其他库使用INFO级别
    /// - `debug`：所有库都使用DEBUG级别
    pub log_level: Option<String>,
    
    /// 日志滚动配置
    pub log_rotation: Option<LogRotation>,
    
    /// 认证配置
    pub authentication: Option<Authentication>,

    /// 线程池大小
    pub thread_pool_size: Option<usize>,
    
    /// Raft集群配置
    pub cluster_config: Option<RaftConfig>,
}

/// Raft集群配置
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct RaftConfig {
    /// Raft节点ID
    pub node_id: u64,

    /// 本节点的RPC服务地址，用于节点间通信
    pub rpc_addr: String,

    /// 保留多少个已经包含在快照中的日志条目，默认1000
    pub max_in_snapshot_log_to_keep: Option<u64>,

    /// 快照触发策略，当日志条目达到指定数量时触发快照
    /// 格式：数字（如10000）表示当日志条目达到该数量时触发快照
    /// 默认值：10000
    pub snapshot_policy_logs_threshold: Option<u64>,

    /// 心跳间隔(毫秒)
    pub heartbeat_interval: Option<u64>,

    /// 选举超时最小值(毫秒)
    pub election_timeout_min: Option<u64>,
    
    /// 选举超时最大值(毫秒)
    pub election_timeout_max: Option<u64>,

}

impl Default for RaftConfig {
    fn default() -> Self {
        Self {
            node_id: 1,
            rpc_addr: "127.0.0.1:13897".to_owned(),
            max_in_snapshot_log_to_keep: None,
            snapshot_policy_logs_threshold: Some(5000),
            heartbeat_interval: Some(500),
            election_timeout_min: Some(1500),
            election_timeout_max: Some(3000),
        }
    }
}

/// Raft节点地址信息
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct NodeAddress {
    /// 节点的RPC服务地址
    pub rpc_addr: String,
}

impl Default for ServerConfig {
    fn default() -> Self {
        Self {
            db_path: "/tmp/rdb_test".to_owned(),
            db_snapshot_path: "/tmp/rdb_test/snapshots".to_string(),
            listen_address: "127.0.0.1".to_owned(),
            listen_port: 8081,
            allowed_clients: Some(vec!["127.0.0.1".to_owned()]),
            db_cache_size_mb: Some(1024),
            db_write_buffer_size_mb: Some(128),
            db_max_open_files: Some(64),
            db_max_background_jobs: Some(4),
            db_vertex_lru_cache_size: Some(100_0000),
            log_level: Some("info".to_owned()),
            log_rotation: Some(LogRotation::default()),
            authentication: None,
            thread_pool_size: None,
            cluster_config: None,
            max_snapshot_files_to_keep: Some(5),
        }
    }
}

impl ServerConfig {
    /// 从文件加载配置
    pub fn load(file_path: &str) -> Result<Self, String> {
        // 检查文件是否存在
        if !Path::new(file_path).exists() {
            // 如果文件不存在，创建默认配置文件
            let default_config = Self::default();
            Self::save_to_file(&default_config, file_path)?;
            return Ok(default_config);
        }
        
        // 从文件加载配置
        let contents = fs::read_to_string(file_path)
            .map_err(|e| format!("Failed to read config file: {}", e))?;
            
        // 根据文件扩展名选择解析器
        let config = if file_path.ends_with(".yml") || file_path.ends_with(".yaml") || file_path.ends_with(".conf") {
            serde_yaml::from_str(&contents)
                .map_err(|e| format!("Failed to parse YAML config: {}", e))?
        } else {
            serde_json::from_str(&contents)
                .map_err(|e| format!("Failed to parse JSON config: {}", e))?
        };
        
        Ok(config)
    }
    
    /// 保存配置到文件
    pub fn save_to_file(&self, file_path: &str) -> Result<(), String> {
        // 根据文件扩展名选择序列化器
        let contents = if file_path.ends_with(".yml") || file_path.ends_with(".yaml") || file_path.ends_with(".conf") {
            serde_yaml::to_string(self)
                .map_err(|e| format!("Failed to serialize config to YAML: {}", e))?
        } else {
            serde_json::to_string_pretty(self)
                .map_err(|e| format!("Failed to serialize config to JSON: {}", e))?
        };
        
        fs::write(file_path, contents)
            .map_err(|e| format!("Failed to write config file: {}", e))?;
            
        Ok(())
    }
    
    /// 获取完整的服务器地址（IP:端口）
    pub fn get_server_address(&self) -> String {
        format!("{}:{}", self.listen_address, self.listen_port)
    }
    
    /// 检查客户端地址是否被允许连接
    pub fn is_client_allowed(&self, client_addr: &str) -> bool {
        // 如果没有设置允许的客户端列表，则允许所有连接
        if let Some(allowed) = &self.allowed_clients {
            if allowed.is_empty() {
                return true;
            }
            
            // 尝试解析客户端地址
            if let Ok(_) = IpAddr::from_str(client_addr) {
                for allowed_addr in allowed {
                    // 允许精确匹配
                    if allowed_addr == client_addr {
                        return true;
                    }
                    
                    // 允许CIDR格式匹配（这里简化为"0.0.0.0"表示允许所有）
                    if allowed_addr == "0.0.0.0" {
                        return true;
                    }
                }
                
                return false;
            }
        }
        
        // 默认允许连接
        true
    }
    
    /// 验证用户名和密码
    pub fn authenticate_user(&self, username: &str, password: &str) -> bool {
        if let Some(auth) = &self.authentication {
            if !auth.enabled {
                // 认证未启用，总是返回成功
                return true;
            }
            
            // 查找并验证用户
            for user in &auth.users {
                if user.username == username && user.password == password {
                    return true;
                }
            }
            
            // 没有找到匹配的用户
            return false;
        }
        
        // 无认证配置，总是返回成功
        true
    }

    pub fn get_db_data_path(&self) -> String {
        format!("{}/data", self.db_path)
    }

    pub fn get_db_log_path(&self) -> String {
        format!("{}/logs", self.db_path)
    }

    pub fn get_snapshot_path(&self) -> String {
        self.db_snapshot_path.clone()
    }

    pub fn get_raft_log_path(&self) -> String {
        format!("{}/rafts", self.db_path)
    }

    pub fn get_max_snapshot_files_to_keep(&self) -> u64  {
        if let Some(max) = self.max_snapshot_files_to_keep {
            return max;
        }
        5
    }
}

/// 认证配置结构体
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Authentication {
    /// 是否启用认证
    pub enabled: bool,
    
    /// 用户列表
    pub users: Vec<User>,
}

/// 用户信息结构体
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct User {
    /// 用户名
    pub username: String,
    
    /// 密码
    pub password: String,
} 