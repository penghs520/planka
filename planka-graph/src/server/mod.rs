use crate::config::ServerConfig;
use crate::database::rdb::rdb::RocksDatabase;
use crate::database::rdb::rdb_config::RocksDbConfig;
use std::io;
use std::sync::Arc;
use tokio::net::TcpListener;
use tokio::signal;
use tokio::sync::{broadcast, RwLock};
use tracing::{error, info, warn};

// 声明子模块
pub mod card_converter;
mod filter;
mod helper;
mod link_querys;
pub mod page;
pub mod querys;
mod router;
pub mod test;
pub mod writes;

// 重新导出函数，使其可用于其他模块
use crate::raft::start_raft_node;
pub use card_converter::convert_to_cards;
pub use card_converter::vertex_title_to_proto_title;
pub use card_converter::vertex_to_card_with_yield;
pub use link_querys::{fetch_links, query_links};
pub use page::SortPageProcessor;
pub use querys::{count_cards, count_cards_by_group, query_card_ids, query_card_titles, query_cards};
pub use writes::{
    batch_create_cards, batch_create_links, batch_delete_links, batch_update_card_field,
    batch_update_card_titles, batch_update_cards, batch_update_links,
};

// 启动TCP服务器，使用tokio异步处理连接，支持graceful shutdown
pub async fn start_server(
    db: Arc<RwLock<Arc<RocksDatabase>>>, 
    _db_config: RocksDbConfig,
    config: Arc<ServerConfig>
) -> io::Result<()> {
    let server_address = config.get_server_address();
    let server_address = server_address.as_str();
    let listener = TcpListener::bind(server_address).await?;

    // 创建shutdown信号广播通道
    let (shutdown_tx, _shutdown_rx) = broadcast::channel(1);

    // 初始化Raft（如果启用）
    let raft_client = if let Some(cluster_config) = &config.cluster_config {
        let node_id = cluster_config.node_id;
        info!("Running in cluster mode, node_id={}", node_id);

        // 创建Raft客户端（直接使用异步方式）
        let client = crate::raft::client::RaftClient::new(node_id, cluster_config.rpc_addr.clone());

        // 启动raft节点，需要手动初始化集群以及手动将leaner加入集群
        if let Err(e) = start_raft_server(
            node_id,
            cluster_config,
            Arc::clone(&db), // 直接传递数据库包装器
            &config,
        )
        .await
        {
            warn!("Raft node startup failed: {}", e);
        }

        Some(Arc::new(client))
    } else {
        info!("Running in standalone mode");
        db.read().await.load_memory_data();
        None
    };

    info!("Server started, listening on {}", server_address);

    // 使用tokio::select!来同时监听连接和shutdown信号
    loop {
        tokio::select! {
            // 监听新连接
            result = listener.accept() => {
                match result {
                    Ok((stream, _)) => {
                        // 设置TCP_NODELAY以禁用Nagle算法，减少延迟
                        if let Err(e) = stream.set_nodelay(true) {
                            warn!("Failed to set TCP_NODELAY: {}", e);
                        }

                        let db_clone = Arc::clone(&db);
                        let config_clone = Arc::clone(&config);
                        let raft_client_clone = raft_client.clone();
                        let shutdown_rx = shutdown_tx.subscribe();

                        // 使用tokio处理任务
                        tokio::spawn(async move {
                            let _ = router::handle_client_with_shutdown(
                                stream,
                                db_clone,
                                config_clone,
                                raft_client_clone,
                                shutdown_rx
                            ).await;
                        });
                    }
                    Err(e) => {
                        error!("Connection failed: {}", e);
                    }
                }
            }
            // 监听shutdown信号
            _ = shutdown_signal() => {
                info!("Received shutdown signal, starting graceful shutdown...");

                // 发送shutdown信号给所有连接处理任务
                let _ = shutdown_tx.send(());

                // 等待一小段时间让连接处理完成
                tokio::time::sleep(tokio::time::Duration::from_secs(2)).await;

                info!("Server shutdown complete");
                break;
            }
        }
    }

    Ok(())
}

// 监听shutdown信号（SIGTERM, SIGINT等）
async fn shutdown_signal() {
    let ctrl_c = async {
        signal::ctrl_c()
            .await
            .expect("failed to install Ctrl+C handler");
    };

    #[cfg(unix)]
    let terminate = async {
        signal::unix::signal(signal::unix::SignalKind::terminate())
            .expect("failed to install signal handler")
            .recv()
            .await;
    };

    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    tokio::select! {
        _ = ctrl_c => {
            info!("Received Ctrl+C signal");
        },
        _ = terminate => {
            info!("Received SIGTERM signal");
        },
    }
}

// 初始化Raft节点
async fn start_raft_server(
    node_id: u64,
    cluster_config: &crate::config::RaftConfig,
    db: Arc<RwLock<Arc<RocksDatabase>>>, // 更新参数类型
    config: &crate::config::ServerConfig,
) -> Result<(), String> {
    let rpc_addr = cluster_config.rpc_addr.clone();

    let db_clone = Arc::clone(&db);
    let cluster_config = cluster_config.clone();
    let server_config = config.clone();

    // 在一个单独的tokio任务中运行raft节点
    // 这是一个"fire and forget"模式 - 我们不等待它完成
    tokio::spawn(async move {
        if let Err(e) =
            start_raft_node(node_id, rpc_addr, db_clone, &cluster_config, &server_config).await
        {
            error!("Raft node startup failed: {:?}", e);
        }
    });

    info!("Raft node startup completed");

    Ok(())
}
