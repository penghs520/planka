use crate::database::rdb::rdb::RocksDatabase;
use crate::database::rdb::rdb_config::RocksDbConfig;
use std::fmt::Debug;
use std::io::Cursor;
use std::path::Path;
use std::sync::Arc;
use std::time::{Instant, SystemTime, UNIX_EPOCH};
use std::{thread, time};
use tokio::sync::RwLock;

use crate::raft::logstore::log_store::RocksLogStore;
use crate::raft::types::{
    ErrorSubject, LogId, Snapshot, SnapshotData, SnapshotMeta, StorageError,
    StoredMembership,
};
use crate::raft::TypeConfig;
use futures::StreamExt;
use openraft::storage::RaftStateMachine;
use openraft::storage::EntryResponder;
use openraft::EntryPayload;
use openraft::error::ErrorSource;
use openraft::ErrorVerb;
use openraft::OptionalSend;
use openraft::RaftSnapshotBuilder;
use openraft::impls::BoxedErrorSource;
use openraft::SnapshotId;
use rocksdb::ColumnFamilyDescriptor;
use rocksdb::Options;
use rocksdb::DB;
use rocksdb::{ColumnFamily, Error};
use serde::Deserialize;
use serde::Serialize;
use tracing::{info, warn};

/**
 * 定义与Raft节点交互的请求类型
 * 在这个示例中，只实现了Set请求，用于写入键值对
 * 你可以根据需要添加更多的请求类型
 */
#[derive(Serialize, Deserialize, Debug, Clone)]
pub enum Request {
    // 批量创建卡片请求
    BatchCreateCards {
        request: crate::proto::pgraph::write::BatchCreateCardRequest,
    },
    // 批量更新卡片请求
    BatchUpdateCards {
        request: crate::proto::pgraph::write::BatchUpdateCardRequest,
    },
    // 批量更新卡片标题请求
    BatchUpdateCardTitles {
        request: crate::proto::pgraph::write::BatchUpdateCardTitleRequest,
    },
    // 批量部分更新卡片属性请求
    BatchUpdateCardField {
        request: crate::proto::pgraph::write::BatchUpdateCardFieldRequest,
    },
    // 批量创建关联关系请求
    BatchCreateLinks {
        request: crate::proto::pgraph::write::BatchCreateLinkRequest,
    },
    // 批量更新关联关系请求
    BatchUpdateLinks {
        request: crate::proto::pgraph::write::BatchUpdateLinkRequest,
    },
    // 批量删除关联关系请求
    BatchDeleteLinks {
        request: crate::proto::pgraph::write::BatchDeleteLinkRequest,
    },
}

impl std::fmt::Display for Request {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Request::BatchCreateCards { request } => {
                write!(f, "BatchCreateCards(count={})", request.cards.len())
            }
            Request::BatchUpdateCards { request } => {
                write!(f, "BatchUpdateCards(count={})", request.cards.len())
            }
            Request::BatchUpdateCardTitles { request } => {
                write!(f, "BatchUpdateCardTitles(count={})", request.requests.len())
            }
            Request::BatchUpdateCardField { request } => {
                write!(f, "BatchUpdateCardField(count={})", request.requests.len())
            }
            Request::BatchCreateLinks { request } => {
                write!(f, "BatchCreateLinks(count={})", request.links.len())
            }
            Request::BatchUpdateLinks { request } => {
                write!(f, "BatchUpdateLinks(count={})", request.links.len())
            }
            Request::BatchDeleteLinks { request } => {
                write!(f, "BatchDeleteLinks(count={})", request.links.len())
            }
        }
    }
}

/**
 * 定义期望从节点读取数据的响应类型
 * 在这个示例中，响应包含一个可选的字符串值
 */
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct Response {
    pub value: Option<String>,
}

// 存储的快照结构体，包含元数据和状态机数据
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct StoredSnapshot {
    pub meta: SnapshotMeta,

    /// 快照数据
    pub data: Vec<u8>,
}

// 图数据库快照元数据，包含checkpoint信息
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct GraphDatabaseSnapshot {
    pub meta: SnapshotMeta,

    /// RocksDB checkpoint路径
    pub snapshot_path: String,

    /// 快照创建时间戳
    pub timestamp: u64,
}

// 状态机存储结构体
#[derive(Clone)]
pub struct StateMachineStore {
    pub data: StateMachineData,

    /// 用于持久化日志、快照等raft数据的rocksdb数据库实例
    raft_db: Arc<DB>,

    /// 存储图数据的rocksdb数据库实例 - 直接引用主数据库包装器
    graph_db: Arc<RwLock<Arc<RocksDatabase>>>,

    /// 图数据库配置 - 用于重建数据库实例
    db_config: RocksDbConfig,

    /// 存储快照数据的基础目录
    snapshot_path: String,

    /// 最大保留快照文件数量
    max_snapshot_files: u64,
}

// 状态机数据结构体
#[derive(Debug, Clone)]
pub struct StateMachineData {
    /// 当前快照ID（使用时间戳，应该全局唯一）
    pub current_snapshot_id: Option<SnapshotId>,

    // 最后应用的日志ID
    pub last_applied_log_id: Option<LogId>,

    // 最后的成员配置
    pub last_membership: StoredMembership,
}

// 实现状态机的快照构建器
impl RaftSnapshotBuilder<TypeConfig> for StateMachineStore {
    // 构建快照 - 基于RocksDB checkpoint机制
    async fn build_snapshot(&mut self) -> Result<Snapshot, std::io::Error> {
        let last_applied_log = self.data.last_applied_log_id;
        let last_membership = self.data.last_membership.clone();

        // 生成快照ID
        let snapshot_id: SnapshotId = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs()
            .to_string();

        // 创建快照元数据
        let meta = SnapshotMeta {
            last_log_id: last_applied_log,
            last_membership,
            snapshot_id,
        };

        // 获取快照基础路径
        let snapshot_base_path = self.snapshot_path.clone();

        let _ = self.flush_graph_db().await;

        // 获取数据库写锁，阻塞客户端的读写请求
        let graph_db = self.graph_db.write().await;

        // 创建RocksDB checkpoint
        let snapshot_path = {
            graph_db
                .create_checkpoint(&snapshot_base_path)
                .map_err(|e| std::io::Error::new(std::io::ErrorKind::Other, e.to_string()))?
        };

        // 向snapshot_path目录下写入一个snapshot_meta.json文件，包含当前快照的元数据
        let meta_file_path = Path::new(&snapshot_path).join("snapshot_meta.json");
        let meta_json = serde_json::to_string_pretty(&meta)
            .map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e))?;
        std::fs::write(&meta_file_path, meta_json)?;

        // 创建图数据库快照
        let graph_snapshot = GraphDatabaseSnapshot {
            meta: meta.clone(),
            snapshot_path: snapshot_path.clone(),
            timestamp: SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_secs(),
        };

        // 序列化图数据库快照数据
        let snapshot_data = serde_json::to_vec(&graph_snapshot)
            .map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e))?;

        let snapshot = StoredSnapshot {
            meta: meta.clone(),
            data: snapshot_data.clone(),
        };

        // 保存快照数据
        self.save_current_snapshot(snapshot)
            .map_err(|e| std::io::Error::new(std::io::ErrorKind::Other, e.to_string()))?;

        // 清理旧快照文件
        if let Err(e) = self.clean_old_snapshots() {
            warn!("Failed to clean old snapshots: {}", e);
        }

        info!("Created database snapshot: checkpoint={}", snapshot_path,);

        Ok(Snapshot {
            meta,
            snapshot: Cursor::new(snapshot_data),
        })
    }
}

impl StateMachineStore {
    // 创建新的状态机存储
    pub async fn new(
        db: Arc<DB>,
        graph_db: Arc<RwLock<Arc<RocksDatabase>>>,
        db_config: RocksDbConfig,
        snapshot_path: String,
        max_snapshot_files: u64,
    ) -> Result<StateMachineStore, StorageError> {
        let mut sm = Self {
            data: StateMachineData {
                last_applied_log_id: None,
                current_snapshot_id: None,
                last_membership: Default::default(),
            },
            raft_db: db,
            graph_db: Arc::clone(&graph_db),
            db_config,
            snapshot_path,
            max_snapshot_files,
        };
        // 尝试加载现有快照
        let snapshot = sm.get_current_snapshot_()?;
        if let Some(snap) = snapshot {
            sm.update_state_machine_(snap).await?;
        } else {
            info!("No snapshot found, load data from origin data directory");
            graph_db.read().await.load_memory_data();
        }
        Ok(sm)
    }

    // 根据快照更新状态机 - 支持图数据库checkpoint恢复
    async fn update_state_machine_(
        &mut self,
        snapshot: StoredSnapshot,
    ) -> Result<(), StorageError> {
        // 反序列化为图数据库快照
        let graph_snapshot = serde_json::from_slice::<GraphDatabaseSnapshot>(&snapshot.data)
            .map_err(|e| {
                StorageError::read_snapshot(
                    Some(snapshot.meta.signature()),
                    BoxedErrorSource::from_string(format!("Invalid graph database snapshot format: {}", e)),
                )
            })?;

        info!("update state machine use snapshot: {}", graph_snapshot.snapshot_path);

        // 检查快照文件是否存在
        let snapshot_path = Path::new(&graph_snapshot.snapshot_path);
        self.check_snapshot_file_exists(&snapshot_path)
            .map_err(|e| {
                StorageError::read_snapshot(
                    Some(snapshot.meta.signature()),
                    BoxedErrorSource::from_string(format!("Failed to restore from snapshot: {}", e)),
                )
            })?;

        // 首先释放旧的数据库实例，确保文件锁被释放
        {
            let mut graph_db_write = self.graph_db.write().await;

            // 刷新旧实例
            info!("Releasing old database instance for restoration...");

            // 创建一个虚拟实例替换当前实例，释放文件锁
            let dummy_config = RocksDbConfig::new("/tmp/dummy_db".to_string());
            let dummy_db = Arc::new(RocksDatabase::new_db_with_config(dummy_config));
            let _ = std::mem::replace(&mut *graph_db_write, dummy_db);

            info!("Old database instance has been released");

            let current_db_path = Path::new(&self.db_config.path);
            // 重启新的数据库实例
            // 删除目标目录（如果存在）  TODO 是否要备份
            if current_db_path.exists() {
                std::fs::remove_dir_all(current_db_path).map_err(|e| {
                    StorageError::read_snapshot(
                        Some(snapshot.meta.signature()),
                        BoxedErrorSource::from_string(format!("Failed to restore from checkpoint: {}", e)),
                    )
                })?;
                info!(
                    "Old database file has been removed:{}",
                    current_db_path.display()
                );
            }

            // 复制snapshot文件到数据库目录下
            self.copy_dir_recursively(snapshot_path, current_db_path)
                .map_err(|e| {
                    StorageError::read_snapshot(
                        Some(snapshot.meta.signature()),
                        BoxedErrorSource::from_string(format!("Failed to restore from checkpoint: {}", e)),
                    )
                })?;

            // 创建新的数据库实例
            let config = RocksDbConfig::new(current_db_path.to_string_lossy().to_string());
            let restored_db = Arc::new(RocksDatabase::new_db_with_config(config));
            restored_db.load_memory_data();
            let _ = std::mem::replace(&mut *graph_db_write, restored_db);
            // 方式2（报错）：graph_db_write.db = restored_db.db;//cannot assign to data in an `Arc`
            //方式3：*graph_db_write = Arc::new(restored_db);
            // 更新状态
            self.data.current_snapshot_id = Some(snapshot.meta.snapshot_id.clone());
            self.data.last_applied_log_id = snapshot.meta.last_log_id;
            self.data.last_membership = snapshot.meta.last_membership.clone();
        } // 自动释放锁

        info!(
            "Successfully restored database state from snapshot {}:",
            graph_snapshot.snapshot_path
        );

        Ok(())
    }

    fn check_snapshot_file_exists<P: AsRef<Path>>(
        &self,
        snapshot_path: P,
    ) -> Result<(), Box<dyn std::error::Error>> {
        // 轮询直到source目录存在，10秒内如果还不存在则返回错误
        let timeout_duration = time::Duration::from_secs(10);
        let poll_interval = time::Duration::from_millis(100);
        let start_time = Instant::now();
        let checkpoint_path = snapshot_path.as_ref();

        loop {
            if checkpoint_path.exists() && checkpoint_path.is_dir() {
                info!("Snapshot directory found: {}", checkpoint_path.display());
                break Ok(());
            }

            // 检查是否超时
            if start_time.elapsed() >= timeout_duration {
                return Err(format!(
                    "Timeout waiting for snapshot directory to exist: {:?}. Waited for {} seconds.",
                    checkpoint_path,
                    timeout_duration.as_secs()
                )
                .into());
            }

            // 等待一小段时间后再次检查
            thread::sleep(poll_interval);
            tracing::debug!(
                "Waiting for snapshot directory: {:?}, elapsed: {:?}",
                checkpoint_path,
                start_time.elapsed()
            );
        }
    }

    /// 递归复制目录
    fn copy_dir_recursively(
        &self,
        src: &Path,
        dst: &Path,
    ) -> Result<(), Box<dyn std::error::Error>> {
        std::fs::create_dir_all(dst)?;

        for entry in std::fs::read_dir(src)? {
            let entry = entry?;
            let src_path = entry.path();
            let dst_path = dst.join(entry.file_name());

            if src_path.is_dir() {
                self.copy_dir_recursively(&src_path, &dst_path)?;
            } else {
                std::fs::copy(&src_path, &dst_path)?;
            }
        }

        Ok(())
    }

    // 获取当前快照
    fn get_current_snapshot_(&self) -> StorageResult<Option<StoredSnapshot>> {
        // 尝试从快照目录中读取最新快照的元数据
        match self.get_latest_snapshot_from_directory() {
            Ok(Some(snapshot)) => Ok(Some(snapshot)),
            Ok(None) => {
                Ok(None)
            }
            Err(e) => {
                warn!("Failed to read snapshot from directory: {}", e);
                Ok(None)
            }
        }
    }

    /// 从快照目录中获取最新的快照:安装目录名的后缀时间戳排序取最新
    fn get_latest_snapshot_from_directory(&self) -> Result<Option<StoredSnapshot>, Box<dyn std::error::Error>> {
        let snapshot_dir = Path::new(&self.snapshot_path);
        
        // 如果快照目录不存在，返回 None
        if !snapshot_dir.exists() {
            return Ok(None);
        }

        // 读取快照目录中的所有条目，找到最新的快照目录
        let mut entries: Vec<_> = std::fs::read_dir(snapshot_dir)?
            .filter_map(|entry| {
                let entry = entry.ok()?;
                let path = entry.path();
                
                // 只处理目录（快照是以目录形式存储的）
                if path.is_dir() {
                    // 检查是否包含 snapshot_meta.json 文件
                    let meta_file_path = path.join("snapshot_meta.json");
                    if meta_file_path.exists() {
                        return Some(path);
                    }
                    warn!("snapshot_meta.json not found in {}", path.display());
                }
                None
            })
            .collect();

        // 如果没有找到任何快照目录，返回 None
        if entries.is_empty() {
            return Ok(None);
        }

        // 按目录名称中的时间戳排序，最新的在最后
        entries.sort_by(|a, b| {
            let extract_timestamp = |path: &Path| -> u64 {
                path.file_name()
                    .and_then(|name| name.to_str())
                    .and_then(|name| {
                        // 找到最后一个下划线，提取后面的时间戳部分
                        name.rfind('_')
                            .map(|pos| &name[pos + 1..])
                            .and_then(|timestamp_str| timestamp_str.parse::<u64>().ok())
                    })
                    .unwrap_or(0)
            };
            
            let timestamp_a = extract_timestamp(a);
            let timestamp_b = extract_timestamp(b);
            timestamp_a.cmp(&timestamp_b)
        });

        // 获取最新的快照目录（最后一个）
        let latest_snapshot_path = &entries[entries.len() - 1];
        info!("latest snapshot path: {}", latest_snapshot_path.display());
        let meta_file_path = latest_snapshot_path.join("snapshot_meta.json");

        // 读取并解析 snapshot_meta.json 文件
        let meta_content = std::fs::read_to_string(&meta_file_path)?;
        let meta: SnapshotMeta = serde_json::from_str(&meta_content)?;

        // 创建 GraphDatabaseSnapshot
        let graph_snapshot = GraphDatabaseSnapshot {
            meta: meta.clone(),
            snapshot_path: latest_snapshot_path.to_string_lossy().to_string(),
            timestamp: SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_secs(),
        };

        // 序列化图数据库快照数据
        let snapshot_data = serde_json::to_vec(&graph_snapshot)?;

        Ok(Some(StoredSnapshot {
            meta,
            data: snapshot_data,
        }))
    }

    // 设置当前快照
    fn save_current_snapshot(&self, snap: StoredSnapshot) -> StorageResult<()> {
        self.raft_db
            .put_cf(
                self.cf_store(),
                b"snapshot",
                serde_json::to_vec(&snap).unwrap().as_slice(),
            )
            .map_err(|e| StorageError::write_snapshot(Some(snap.meta.signature()), BoxedErrorSource::from_error(&e)))?;
        self.flush_raft_db(
            ErrorSubject::Snapshot(Some(snap.meta.signature())),
            ErrorVerb::Write,
        )?;
        Ok(())
    }

    // 刷新数据到磁盘
    fn flush_raft_db(&self, subject: ErrorSubject, verb: ErrorVerb) -> Result<(), StorageError> {
        self.raft_db
            .flush_wal(true)
            .map_err(|e| StorageError::new(subject, verb, BoxedErrorSource::from_error(&e)))?;
        Ok(())
    }

    async fn flush_graph_db(&self) -> Result<(), Error> {
        let graph_db = self.graph_db.read().await;
        self.flush_graph_db_inner(&*graph_db)
    }

    fn flush_graph_db_inner(&self, graph_db: &RocksDatabase) -> Result<(), Error> {
        graph_db.db.flush_wal(true)
    }

    // 获取存储列族
    fn cf_store(&self) -> &ColumnFamily {
        self.raft_db.cf_handle("store").unwrap()
    }

    /// 清理旧快照文件，保留最新的max_snapshot_files个快照
    pub fn clean_old_snapshots(&self) -> Result<(), std::io::Error> {
        let snapshot_dir = Path::new(&self.snapshot_path);
        
        // 如果快照目录不存在，直接返回
        if !snapshot_dir.exists() {
            return Ok(());
        }

        // 读取快照目录中的所有条目
        let mut entries: Vec<_> = std::fs::read_dir(snapshot_dir)?
            .filter_map(|entry| {
                let entry = entry.ok()?;
                let path = entry.path();
                
                // 只处理目录（快照是以目录形式存储的）
                if path.is_dir() {
                    return Some(path);
                }
                None
            })
            .collect();

        // 如果快照数量没有超过限制，不需要清理
        if entries.len() <= self.max_snapshot_files as usize {
            return Ok(());
        }

        // 按目录名称中的时间戳数值排序，最新的在最后
        entries.sort_by(|a, b| {
            let extract_timestamp = |path: &Path| -> u64 {
                path.file_name()
                    .and_then(|name| name.to_str())
                    .and_then(|name| {
                        // 找到最后一个下划线，提取后面的时间戳部分
                        name.rfind('_')
                            .map(|pos| &name[pos + 1..])
                            .and_then(|timestamp_str| timestamp_str.parse::<u64>().ok())
                    })
                    .unwrap_or(0)
            };
            
            let timestamp_a = extract_timestamp(a);
            let timestamp_b = extract_timestamp(b);
            timestamp_a.cmp(&timestamp_b)
        });

        // 删除超出限制的旧快照（排序后的前面部分是旧的快照）
        let to_delete = entries.len() - self.max_snapshot_files as usize;
        for path in entries.iter().take(to_delete) {
            if let Err(e) = std::fs::remove_dir_all(path) {
                warn!(
                    "Failed to remove old snapshot directory {:?}: {}",
                    path,
                    e
                );
            } else {
                info!("Removed old snapshot directory: {:?}", path);
            }
        }

        Ok(())
    }
}

// 实现Raft状态机接口
impl RaftStateMachine<TypeConfig> for StateMachineStore {
    type SnapshotBuilder = Self;

    // 获取已应用的状态
    async fn applied_state(&mut self) -> Result<(Option<LogId>, StoredMembership), std::io::Error> {
        Ok((
            self.data.last_applied_log_id,
            self.data.last_membership.clone(),
        ))
    }

    // 应用日志条目到状态机
    async fn apply<Strm>(&mut self, mut entries: Strm) -> Result<(), std::io::Error>
    where
        Strm: futures::Stream<Item = Result<EntryResponder<TypeConfig>, std::io::Error>> + Unpin + OptionalSend,
    {
        while let Some(entry_result) = entries.next().await {
            let (entry, responder) = entry_result?;
            let last_applied_log_id = Some(entry.log_id);

            // 根据日志条目类型进行处理，计算响应
            let response = match &entry.payload {
                EntryPayload::Blank => {
                    // 空日志条目，无需处理
                    Response { value: None }
                }

                EntryPayload::Normal(req) => match req {
                    // 处理批量创建卡片请求
                    Request::BatchCreateCards { request } => {
                        // 在这里调用实际的处理函数，并封装结果
                        tracing::debug!(
                            "Applying batch create cards request via Raft, card count: {}",
                            request.cards.len()
                        );

                        // 实际调用图数据库的批量创建卡片方法
                        let resp = {
                            let graph_db = self.graph_db.read().await;
                            crate::server::writes::batch_create_cards(
                                request.clone(),
                                &**graph_db,
                                None,
                            )
                            .await
                        };
                        Response {
                            value: Some(format!(
                                "Processed batch create cards request, success count: {}",
                                resp.success
                            )),
                        }
                    }

                    // 处理批量更新卡片请求
                    Request::BatchUpdateCards { request } => {
                        tracing::debug!(
                            "Applying batch update cards request via Raft, card count: {}",
                            request.cards.len()
                        );

                        // 实际调用图数据库的批量更新卡片方法
                        let resp = {
                            let graph_db = self.graph_db.read().await;
                            crate::server::writes::batch_update_cards(
                                request.clone(),
                                &**graph_db,
                                None,
                            )
                            .await
                        };
                        Response {
                            value: Some(format!(
                                "Processed batch update cards request, success count: {}",
                                resp.success
                            )),
                        }
                    }

                    // 处理批量更新卡片标题请求
                    Request::BatchUpdateCardTitles { request } => {
                        tracing::debug!(
                            "Applying batch update card titles request via Raft, title count: {}",
                            request.requests.len()
                        );

                        // 实际调用图数据库的批量更新卡片标题方法
                        let resp = {
                            let graph_db = self.graph_db.read().await;
                            crate::server::writes::batch_update_card_titles(
                                request.clone(),
                                &**graph_db,
                                None,
                            )
                            .await
                        };
                        Response {
                            value: Some(format!(
                                "Processed batch update card titles request, success count: {}",
                                resp.success
                            )),
                        }
                    }

                    // 处理批量部分更新卡片属性请求
                    Request::BatchUpdateCardField { request } => {
                        tracing::debug!(
                            "Applying batch update card field request via Raft, request count: {}",
                            request.requests.len()
                        );

                        // 实际调用图数据库的批量更新卡片属性方法
                        let resp = {
                            let graph_db = self.graph_db.read().await;
                            crate::server::writes::do_batch_update_card_field(
                                request.clone(),
                                &**graph_db,
                            )
                            .await
                        };
                        Response {
                            value: Some(format!(
                                "Processed batch update card field request, success count: {}",
                                resp.success
                            )),
                        }
                    }

                    // 处理批量创建关联关系请求
                    Request::BatchCreateLinks { request } => {
                        tracing::debug!(
                            "Applying batch create links request via Raft, link count: {}",
                            request.links.len()
                        );

                        // 实际调用图数据库的批量创建关联关系方法
                        let resp = {
                            let graph_db = self.graph_db.read().await;
                            crate::server::writes::batch_create_links(
                                request.clone(),
                                &**graph_db,
                                None,
                            )
                            .await
                        };
                        Response {
                            value: Some(format!(
                                "Processed batch create links request, success count: {}",
                                resp.success
                            )),
                        }
                    }

                    // 处理批量更新关联关系请求
                    Request::BatchUpdateLinks { request } => {
                        tracing::debug!(
                            "Applying batch update links request via Raft, link count: {}",
                            request.links.len()
                        );

                        // 实际调用图数据库的批量更新关联关系方法
                        let resp = {
                            let graph_db = self.graph_db.read().await;
                            crate::server::writes::batch_update_links(
                                request.clone(),
                                &**graph_db,
                                None,
                            )
                            .await
                        };
                        Response {
                            value: Some(format!(
                                "Processed batch update links request, success count: {}",
                                resp.success
                            )),
                        }
                    }

                    // 处理批量删除关联关系请求
                    Request::BatchDeleteLinks { request } => {
                        tracing::debug!(
                            "Applying batch delete links request via Raft, link count: {}",
                            request.links.len()
                        );

                        // 实际调用图数据库的批量删除关联关系方法
                        let resp = {
                            let graph_db = self.graph_db.read().await;
                            crate::server::writes::batch_delete_links(
                                request.clone(),
                                &**graph_db,
                                None,
                            )
                            .await
                        };
                        Response {
                            value: Some(format!(
                                "Processed batch delete links request, success count: {}",
                                resp.success
                            )),
                        }
                    }
                },

                EntryPayload::Membership(membership) => {
                    self.data.last_membership =
                        StoredMembership::new(Some(entry.log_id), membership.clone());
                    Response { value: None }
                }
            };

            // 发送响应给等待的客户端（仅在leader上有responder）
            if let Some(r) = responder {
                r.send(response);
            }

            // 更新最后应用的日志ID
            self.data.last_applied_log_id = last_applied_log_id;
        }

        Ok(())
    }

    // 获取快照构建器
    async fn get_snapshot_builder(&mut self) -> Self::SnapshotBuilder {
        self.clone()
    }

    // 开始接收快照
    async fn begin_receiving_snapshot(&mut self) -> Result<Cursor<Vec<u8>>, std::io::Error> {
        Ok(Cursor::new(Vec::new()))
    }

    // follower安装快照到状态机
    async fn install_snapshot(
        &mut self,
        meta: &SnapshotMeta,
        snapshot: SnapshotData,
    ) -> Result<(), std::io::Error> {
        let new_snapshot = StoredSnapshot {
            meta: meta.clone(),
            data: snapshot.into_inner(),
        };
        info!("installing snapshot {:?}", new_snapshot.meta);

        // 使用快照更新状态机
        self.update_state_machine_(new_snapshot.clone()).await
            .map_err(|e| std::io::Error::new(std::io::ErrorKind::Other, e.to_string()))?;

        // 保存快照
        self.save_current_snapshot(new_snapshot)
            .map_err(|e| std::io::Error::new(std::io::ErrorKind::Other, e.to_string()))?;

        Ok(())
    }

    // 获取当前快照
    async fn get_current_snapshot(&mut self) -> Result<Option<Snapshot>, std::io::Error> {
        let x = self.get_current_snapshot_()
            .map_err(|e| std::io::Error::new(std::io::ErrorKind::Other, e.to_string()))?;
        Ok(x.map(|s| Snapshot {
            meta: s.meta.clone(),
            snapshot: Cursor::new(s.data.clone()),
        }))
    }
}

type StorageResult<T> = Result<T, StorageError>;

/// 检查并从checkpoint恢复数据库
///
/// 在数据库启动前调用，检查是否有需要应用的checkpoint恢复
pub async fn check_and_restore_from_checkpoint<P: AsRef<Path>>(
    _db_path: P,
) -> Result<(), Box<dyn std::error::Error>> {
    //TODO 最近的一次快照路径

    Ok(())
}

// 创建新的存储实例（日志存储和状态机存储）
pub(crate) async fn new_storage<P: AsRef<Path>>(
    raft_data_path: P,
    db_snapshot_path: String,
    graph_db: Arc<RwLock<Arc<RocksDatabase>>>,
    db_config: RocksDbConfig,
    max_snapshot_files: u64,
) -> (RocksLogStore<TypeConfig>, StateMachineStore) {
    // 创建RocksDB选项
    let mut db_opts = Options::default();
    db_opts.create_missing_column_families(true);
    db_opts.create_if_missing(true);

    // 创建列族描述符
    let store = ColumnFamilyDescriptor::new("store", Options::default());
    let meta = ColumnFamilyDescriptor::new("meta", Options::default());
    let logs = ColumnFamilyDescriptor::new("logs", Options::default());

    // 打开RocksDB数据库
    let db = DB::open_cf_descriptors(&db_opts, raft_data_path, vec![store, meta, logs]).unwrap();
    let db = Arc::new(db);

    // 创建日志存储和状态机存储
    let log_store = RocksLogStore::new(db.clone());
    let sm_store = StateMachineStore::new(db, graph_db, db_config, db_snapshot_path, max_snapshot_files)
        .await
        .unwrap();

    (log_store, sm_store)
}
