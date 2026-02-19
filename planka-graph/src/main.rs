use clap::Parser;
use num_cpus;
use serde_yaml;
/**
 * pgraph - 图数据库应用程序入口点
 *
 */

use std::fs;
use std::path::Path;
use std::sync::Arc;
use tokio::sync::RwLock;
use tracing::{debug, error, info};
use pgraph::config::ServerConfig;
use pgraph::database::rdb::rdb::RocksDatabase;
use pgraph::database::rdb::rdb_config::RocksDbConfig;
use pgraph::logger;
use pgraph::server;

#[derive(Parser, Debug)]
#[command(author, version, about = "pgraph Database")]
struct Cli {
    /// 配置文件路径
    #[arg(short, long)]
    config: Option<String>,
}

/// 主函数：启动图数据库服务器
fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();
    
    // 确定配置文件路径
    let config_path = cli.config.as_deref().unwrap_or("./pgraph.conf");
    
    // 加载配置
    info!("Loading config file: {}", config_path);
    let config = match ServerConfig::load(config_path) {
        Ok(config) => {
            info!("Configuration loaded successfully");
            Arc::new(config)
        },
        Err(e) => {
            error!("Failed to load configuration: {}. Using default configuration", e);
            Arc::new(ServerConfig::default())
        }
    };
    
    // 初始化日志系统
    logger::init_logger(&config);
    
    // 从配置中获取线程池大小，如未设置则使用CPU核心数的2倍
    let default_threads = num_cpus::get() * 2;
    let thread_pool_size = config.thread_pool_size.unwrap_or(default_threads);
    info!("Configured tokio worker threads: {} (CPU cores: {})", thread_pool_size, num_cpus::get());
    
    // 创建优化的tokio运行时 - 使用配置的线程池大小
    let runtime = tokio::runtime::Builder::new_multi_thread()
        .worker_threads(thread_pool_size)
        .enable_all()
        .build()?;
    
    // 在自定义运行时上运行服务器
    runtime.block_on(async {
        run_server(config).await
    })
}

/// 启动pgraph服务器
async fn run_server(config: Arc<ServerConfig>) -> Result<(), Box<dyn std::error::Error>> {
    // 记录详细配置信息
    if let Ok(yaml) = serde_yaml::to_string(&*config) {
        debug!("Complete configuration:\n{}", yaml);
    } else {
        debug!("Complete configuration: {:?}", &config);
    }

    // 确保数据库目录存在
    let db_data_path = config.get_db_data_path();
    let db_path = Path::new(&db_data_path);
    if !db_path.exists() {
        info!("Creating database directory: {}", db_data_path);
        match fs::create_dir_all(db_path) {
            Ok(_) => info!("Database directory created successfully"),
            Err(e) => error!("Failed to create database directory: {}", e)
        }
    } 
    
    // 创建新的数据库实例
    let mut db_config = RocksDbConfig::new(db_data_path);
    
    // 设置RocksDB配置参数
    if let Some(size) = config.db_cache_size_mb {
        db_config.cache_size_mb = size;
    }
    
    if let Some(size) = config.db_write_buffer_size_mb {
        db_config.write_buffer_size_mb = size;
    }
    
    if let Some(files) = config.db_max_open_files {
        db_config.max_open_files = files;
    }
    
    if let Some(jobs) = config.db_max_background_jobs {
        db_config.max_background_jobs = jobs;
    }
    
    if let Some(size) = config.db_vertex_lru_cache_size {
        db_config.vertex_lru_cache_size = size;
    } else {
        db_config.vertex_lru_cache_size = 100_0000;
    }
    let db = RocksDatabase::new_db_with_config(db_config.clone());
    
    // 使用RwLock包装数据库实例，支持动态替换
    let db_wrapper = Arc::new(RwLock::new(Arc::new(db)));
    info!("Database instance created successfully");
        
    // 启动服务
    server::start_server(db_wrapper, db_config, Arc::clone(&config)).await?;
    
    Ok(())
}
