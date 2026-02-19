#![allow(clippy::uninlined_format_args)]
#![deny(unused_qualifications)]

pub mod app;
pub mod client;
mod logstore;
mod network;
pub mod store;

//#[path = "../../utils/declare_types.rs"]
pub mod types;
use crate::config::RaftConfig;
use crate::raft::app::pgraphRaftApp;
use crate::raft::network::api::write;
use tokio::sync::RwLock;
use crate::raft::network::management::{
    add_learner, change_membership, init, metrics, trigger_snapshot,
};
use crate::raft::network::raft_service::{append, snapshot, vote};
use crate::raft::network::pgraphRaftNetwork;
use crate::raft::store::{new_storage, Request, Response};
use actix_web::web::Data;
use actix_web::{error, web, HttpResponse};
use actix_web::{middleware, App, HttpServer};
use openraft::Config;
use serde::Serialize;
use std::fmt::Display;
use std::sync::Arc;
use std::time::Duration;
use tracing::error;
use crate::database::rdb::rdb::RocksDatabase;

// 定义节点ID类型为u64
pub type NodeId = u64;

// 定义自定义JSON错误响应结构
#[derive(Serialize)]
struct JsonErrorResponse {
    code: u16,
    message: String,
}

// 节点信息结构体，包含RPC地址和API地址
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize, PartialEq, Eq, Default)]
pub struct Node {
    pub rpc_addr: String, // RPC服务地址，用于节点间通信
}

impl Display for Node {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "Node {{ rpc_addr: {} }}", self.rpc_addr)
    }
}

// 声明Raft所需的类型配置
// D = Request：定义写入请求的数据类型
// R = Response：定义响应的数据类型
// Node = Node：定义节点的数据类型
openraft::declare_raft_types!(
    pub TypeConfig:
        D = Request,
        R = Response,
        Node = Node,
);

// 启动Raft节点的函数
pub async fn start_raft_node(
    node_id: NodeId,                                         // 节点ID
    rpc_addr: String,                                        // RPC服务地址
    graph_db: Arc<RwLock<Arc<RocksDatabase>>>, // 图数据库包装器
    cluster_config: &RaftConfig,
    server_config: &crate::config::ServerConfig,
) -> std::io::Result<()> {
    // 创建Raft实例的配置
    let mut config = Config::default();

    //保留多少个已经包含在快照中的日志条目，默认1000
    if let Some(max_in_snapshot) = cluster_config.max_in_snapshot_log_to_keep {
        config.max_in_snapshot_log_to_keep = max_in_snapshot;
    }

    // 设置快照触发策略，当日志条目达到指定数量时触发快照
    if let Some(threshold) = cluster_config.snapshot_policy_logs_threshold {
        config.snapshot_policy = openraft::SnapshotPolicy::LogsSinceLast(threshold);
    } else {
        config.snapshot_policy = openraft::SnapshotPolicy::LogsSinceLast(10000);
    }

    // 如果提供了 RaftConfig，则使用其中的配置值
    if let Some(heartbeat_interval) = cluster_config.heartbeat_interval {
        config.heartbeat_interval = heartbeat_interval;
    }
    if let Some(timeout_min) = cluster_config.election_timeout_min {
        config.election_timeout_min = timeout_min;
    }
    if let Some(timeout_max) = cluster_config.election_timeout_max {
        config.election_timeout_max = timeout_max;
    }

    let config = Arc::new(config.validate().unwrap());

    // 创建存储实例（日志存储和状态机存储）
    let db_config = {
        let graph_db_read = graph_db.read().await;
        graph_db_read.config.clone()
    };
    let (log_store, state_machine_store) = new_storage(
        server_config.get_raft_log_path(),
        server_config.get_snapshot_path(),
        graph_db, // 直接传递主数据库包装器
        db_config,
        server_config.get_max_snapshot_files_to_keep(),
    )
    .await;

    // 创建网络层，用于节点间通信
    let network = pgraphRaftNetwork::new();

    // 创建本地Raft实例
    let raft = openraft::Raft::new(
        node_id,
        config.clone(),
        network,
        log_store,
        state_machine_store,
    )
    .await
    .unwrap();

    // 创建应用实例
    let app = Data::new(pgraphRaftApp {
        node_id: node_id,
        rpc_addr: rpc_addr.clone(),
        raft,
        config,
    });

    let server = HttpServer::new(move || {
        App::new()
            .wrap(middleware::Compress::default())
            .app_data(app.clone())
            // 配置JSON请求体大小限制，增加到64MB，并添加错误处理
            .app_data(
                web::JsonConfig::default()
                    .limit(64 * 1024 * 1024)
                    .error_handler(json_error_handler),
            )
            .service(init)
            .service(add_learner)
            .service(change_membership)
            .service(metrics)
            .service(trigger_snapshot)
            // raft internal RPC
            .service(append)
            .service(snapshot)
            .service(vote)
            // application API
            .service(write)
    })
    .keep_alive(Duration::from_secs(5))
    .client_request_timeout(Duration::from_secs(30));

    tracing::info!("Raft Server listening on: {}", rpc_addr);

    let x = server.bind(rpc_addr)?;

    x.run().await?;

    Ok(())
}

// 定义JSON请求体错误处理函数
fn json_error_handler(err: error::JsonPayloadError, req: &actix_web::HttpRequest) -> error::Error {
    let error_msg = match err {
        error::JsonPayloadError::Overflow { limit } => {
            format!("Request body too large, exceeds size limit {}B", limit)
        }
        error::JsonPayloadError::OverflowKnownLength { length, limit } => {
            format!("Request body too large {}B, exceeds size limit {}B", length, limit)
        }
        _ => format!("JSON parsing error: {}", err),
    };

    // 打印请求路径和错误信息
    error!("JSON error on path {}: {}", req.path(), error_msg);

    let resp = HttpResponse::BadRequest().json(JsonErrorResponse {
        code: 400,
        message: error_msg,
    });

    error::InternalError::from_response(err, resp).into()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::database::test::test_utils::TestDb;
    use crate::raft::logstore::log_store::RocksLogStore;
    use crate::raft::store::StateMachineStore;
    use openraft::testing::log::{StoreBuilder, Suite};
    use rocksdb::{ColumnFamilyDescriptor, Options, DB};
    use std::mem::ManuallyDrop;
    use openraft::StorageError;

    struct Builder {}

    impl StoreBuilder<TypeConfig, RocksLogStore<TypeConfig>, StateMachineStore> for Builder {
        async fn build(
            &self,
        ) -> Result<
            ((), RocksLogStore<TypeConfig>, StateMachineStore),
            StorageError<TypeConfig>,
        > {
            // 创建RocksDB选项
            let mut db_opts = Options::default();
            db_opts.create_missing_column_families(true);
            db_opts.create_if_missing(true);

            // 创建列族描述符
            let store = ColumnFamilyDescriptor::new("store", Options::default());
            let meta = ColumnFamilyDescriptor::new("meta", Options::default());
            let logs = ColumnFamilyDescriptor::new("logs", Options::default());

            let test_id = rand::random::<u64>();
            let raft_data_path = format!("/tmp/pgraph/raft-data-{}", test_id);
            let snapshots_path = format!("/tmp/pgraph/snapshots-{}", test_id);

            // 清理可能存在的旧测试目录
            std::fs::remove_dir_all(&raft_data_path).ok();
            std::fs::remove_dir_all(&snapshots_path).ok();

            // 打开RocksDB数据库
            let db =
                DB::open_cf_descriptors(&db_opts, raft_data_path, vec![store, meta, logs]).unwrap();
            let db = Arc::new(db);

            let test_db = ManuallyDrop::new(TestDb::new());
            // 使用 TestDb 中已创建的数据库实例，避免 Drop trait 导致的移动问题
            let graph_db = unsafe {
                Arc::new(std::ptr::read(&test_db.db))
            };

            let log_store: RocksLogStore<TypeConfig> = RocksLogStore::new(Arc::clone(&db));
            let db_config = graph_db.config.clone();
            let graph_db_wrapper = Arc::new(RwLock::new(graph_db));
            let sm = StateMachineStore::new(
                Arc::clone(&db),
                graph_db_wrapper,
                db_config,
                snapshots_path,
                5, // 测试环境默认保留5个快照
            ).await.map_err(|e| StorageError::read_state_machine(&e))?;
            Ok(((), log_store, sm))
        }
    }

    #[tokio::test]
    pub async fn test_store() -> Result<(), StorageError<TypeConfig>> {
        Suite::test_all(Builder {}).await?;
        Ok(())
    }
}
