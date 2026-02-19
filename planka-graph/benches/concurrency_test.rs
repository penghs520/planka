use anyhow::Result;
use criterion::{black_box, criterion_group, criterion_main, Criterion};
use prost::Message;
use rand::Rng;
use std::collections::{HashMap, VecDeque};
use std::sync::{Arc, Barrier, Mutex};
use std::thread;
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use tempfile::tempdir;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpStream;
use tokio::runtime::Runtime;
use tokio::sync::{Mutex as AsyncMutex, RwLock};
use uuid::Uuid;

use pgraph::config::ServerConfig;
use pgraph::database::database::Database;
use pgraph::database::model::*;
use pgraph::database::rdb::rdb::RocksDatabase;
use pgraph::database::transaction::Transaction;
use pgraph::proto::pgraph::{
    common::ValueUnitState,
    model::{title::TitleType, Card, Link, PureTitle, Title},
    query::{CardQueryRequest, QueryScope},
    write::{BatchCreateCardRequest, BatchCreateLinkRequest, BatchUpdateCardRequest},
};
use pgraph::proto::{request::RequestType, Request, Response};
use pgraph::server::start_server;

/// 全局递增ID计数器，用于生成唯一的卡片ID
static NEXT_CARD_ID: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1000000);

/// 性能统计结构
#[derive(Debug, Clone)]
pub struct PerformanceStats {
    pub total_requests: u64,
    pub successful_requests: u64,
    pub failed_requests: u64,
    pub total_duration: Duration,
    pub min_latency: Duration,
    pub max_latency: Duration,
    pub avg_latency: Duration,
    pub p95_latency: Duration,
    pub p99_latency: Duration,
    pub qps: f64,
    // 读写请求分别统计
    pub read_requests: u64,
    pub write_requests: u64,
    pub read_qps: f64,
    pub write_qps: f64,
    pub read_avg_latency: Duration,
    pub write_avg_latency: Duration,
}

impl PerformanceStats {
    pub fn new() -> Self {
        Self {
            total_requests: 0,
            successful_requests: 0,
            failed_requests: 0,
            total_duration: Duration::new(0, 0),
            min_latency: Duration::from_secs(u64::MAX),
            max_latency: Duration::new(0, 0),
            avg_latency: Duration::new(0, 0),
            p95_latency: Duration::new(0, 0),
            p99_latency: Duration::new(0, 0),
            qps: 0.0,
            read_requests: 0,
            write_requests: 0,
            read_qps: 0.0,
            write_qps: 0.0,
            read_avg_latency: Duration::new(0, 0),
            write_avg_latency: Duration::new(0, 0),
        }
    }

    pub fn calculate_from_latencies(
        &mut self,
        latencies: &mut Vec<Duration>,
        total_duration: Duration,
    ) {
        self.total_requests = latencies.len() as u64;
        self.total_duration = total_duration;

        if latencies.is_empty() {
            return;
        }

        // 排序延迟数据以计算百分位数
        latencies.sort();

        self.min_latency = latencies[0];
        self.max_latency = latencies[latencies.len() - 1];

        // 计算平均延迟
        let sum: Duration = latencies.iter().sum();
        self.avg_latency = sum / latencies.len() as u32;

        // 计算P95和P99延迟
        let p95_index = (latencies.len() as f64 * 0.95) as usize;
        let p99_index = (latencies.len() as f64 * 0.99) as usize;

        self.p95_latency = latencies[std::cmp::min(p95_index, latencies.len() - 1)];
        self.p99_latency = latencies[std::cmp::min(p99_index, latencies.len() - 1)];

        // 计算QPS
        if total_duration.as_secs_f64() > 0.0 {
            self.qps = self.total_requests as f64 / total_duration.as_secs_f64();
        }
    }

    pub fn calculate_from_typed_latencies(
        &mut self,
        read_latencies: &mut Vec<Duration>,
        write_latencies: &mut Vec<Duration>,
        total_duration: Duration,
    ) {
        self.read_requests = read_latencies.len() as u64;
        self.write_requests = write_latencies.len() as u64;
        self.total_requests = self.read_requests + self.write_requests;
        self.total_duration = total_duration;

        // 合并所有延迟数据用于总体统计
        let mut all_latencies = Vec::new();
        all_latencies.extend_from_slice(read_latencies);
        all_latencies.extend_from_slice(write_latencies);

        if all_latencies.is_empty() {
            return;
        }

        // 排序延迟数据以计算百分位数
        all_latencies.sort();

        self.min_latency = all_latencies[0];
        self.max_latency = all_latencies[all_latencies.len() - 1];

        // 计算总体平均延迟
        let sum: Duration = all_latencies.iter().sum();
        self.avg_latency = sum / all_latencies.len() as u32;

        // 计算P95和P99延迟
        let p95_index = (all_latencies.len() as f64 * 0.95) as usize;
        let p99_index = (all_latencies.len() as f64 * 0.99) as usize;

        self.p95_latency = all_latencies[std::cmp::min(p95_index, all_latencies.len() - 1)];
        self.p99_latency = all_latencies[std::cmp::min(p99_index, all_latencies.len() - 1)];

        // 计算读请求平均延迟
        if !read_latencies.is_empty() {
            let read_sum: Duration = read_latencies.iter().sum();
            self.read_avg_latency = read_sum / read_latencies.len() as u32;
        }

        // 计算写请求平均延迟
        if !write_latencies.is_empty() {
            let write_sum: Duration = write_latencies.iter().sum();
            self.write_avg_latency = write_sum / write_latencies.len() as u32;
        }

        // 计算QPS
        if total_duration.as_secs_f64() > 0.0 {
            self.qps = self.total_requests as f64 / total_duration.as_secs_f64();
            self.read_qps = self.read_requests as f64 / total_duration.as_secs_f64();
            self.write_qps = self.write_requests as f64 / total_duration.as_secs_f64();
        }
    }
}

/// 测试配置
#[derive(Clone)]
pub struct TestConfig {
    pub node_count: u32,
    pub edge_count: u32,
    pub concurrent_threads: usize,
    pub test_duration_secs: u64,
    pub write_ratio: f64,
    pub server_port: u16,
}

impl Default for TestConfig {
    fn default() -> Self {
        Self {
            node_count: 10_000,      // 节点数量
            edge_count: 100_000,     // 边数量
            concurrent_threads: 100, // 并发线程
            test_duration_secs: 30,  // 测试时长
            write_ratio: 0.1,        // 写请求比例
            server_port: 18080,      // 服务器端口
        }
    }
}

/// TCP客户端
pub struct TcpClient {
    stream: TcpStream,
    server_addr: String,
}

impl TcpClient {
    /// 连接到服务器
    pub async fn connect(server_addr: String) -> Result<Self> {
        let stream = TcpStream::connect(&server_addr).await?;
        Ok(Self {
            stream,
            server_addr,
        })
    }

    /// 发送请求并接收响应
    pub async fn send_request(&mut self, request: Request) -> Result<Response> {
        // 序列化请求
        let mut buffer = Vec::new();
        request.encode(&mut buffer)?;

        // 发送长度前缀（4字节）
        let len = buffer.len() as u32;
        self.stream.write_all(&len.to_be_bytes()).await?;

        // 发送请求数据
        self.stream.write_all(&buffer).await?;
        self.stream.flush().await?;

        // 读取响应长度
        let mut len_buf = [0u8; 4];
        self.stream.read_exact(&mut len_buf).await?;
        let response_len = u32::from_be_bytes(len_buf) as usize;

        // 读取响应数据
        let mut response_buf = vec![0u8; response_len];
        self.stream.read_exact(&mut response_buf).await?;

        // 反序列化响应
        let response = Response::decode(&response_buf[..])?;
        Ok(response)
    }

    /// 重连
    pub async fn reconnect(&mut self) -> Result<()> {
        self.stream = TcpStream::connect(&self.server_addr).await?;
        Ok(())
    }
}

/// TCP连接池
pub struct TcpConnectionPool {
    server_addr: String,
    available_connections: Arc<AsyncMutex<VecDeque<TcpClient>>>,
    max_connections: usize,
    current_connections: Arc<AsyncMutex<usize>>,
}

impl TcpConnectionPool {
    /// 创建新的连接池
    pub fn new(server_addr: String, max_connections: usize) -> Self {
        Self {
            server_addr,
            available_connections: Arc::new(AsyncMutex::new(VecDeque::new())),
            max_connections,
            current_connections: Arc::new(AsyncMutex::new(0)),
        }
    }

    /// 从连接池获取连接
    pub async fn get_connection(&self) -> Result<TcpClient> {
        // 最多重试50次，每次等待20ms
        for _retry in 0..50 {
            // 首先尝试从可用连接中获取
            {
                let mut available = self.available_connections.lock().await;
                if let Some(client) = available.pop_front() {
                    return Ok(client);
                }
            }

            // 如果没有可用连接，尝试创建新连接
            {
                let mut current = self.current_connections.lock().await;
                if *current < self.max_connections {
                    *current += 1;
                    drop(current); // 释放锁

                    match TcpClient::connect(self.server_addr.clone()).await {
                        Ok(client) => return Ok(client),
                        Err(e) => {
                            // 创建失败，减少计数
                            let mut current = self.current_connections.lock().await;
                            *current -= 1;
                            // 创建连接失败，但可以继续重试等待现有连接
                            eprintln!("创建新连接失败: {:?}, 将等待现有连接", e);
                        }
                    }
                }
            }

            // 等待一段时间后重试
            println!("tcp创建新连接失败，等待一段时间后重试");
            tokio::time::sleep(Duration::from_millis(20)).await;
        }

        Err(anyhow::anyhow!("连接池获取连接超时，已重试50次"))
    }

    /// 将连接返回到连接池
    pub async fn return_connection(&self, client: TcpClient) {
        let mut available = self.available_connections.lock().await;
        available.push_back(client);
    }

    /// 发送请求（自动管理连接）
    pub async fn send_request(&self, request: Request) -> Result<Response> {
        let mut client = self.get_connection().await?;

        match client.send_request(request).await {
            Ok(response) => {
                // 成功后返回连接到池中
                self.return_connection(client).await;
                Ok(response)
            }
            Err(e) => {
                // 失败时尝试重连
                if client.reconnect().await.is_ok() {
                    self.return_connection(client).await;
                } else {
                    // 重连失败，减少连接计数
                    let mut current = self.current_connections.lock().await;
                    *current -= 1;
                }
                Err(e)
            }
        }
    }
}

/// 测试数据生成器
pub struct TestDataGenerator {
    config: TestConfig,
    rt: Arc<Runtime>,
    connection_pool: Arc<TcpConnectionPool>,
}

impl TestDataGenerator {
    pub fn new(config: TestConfig, rt: Arc<Runtime>) -> Self {
        let server_addr = format!("127.0.0.1:{}", config.server_port);
        let connection_pool = Arc::new(TcpConnectionPool::new(server_addr, 500));

        Self {
            config,
            rt,
            connection_pool,
        }
    }

    /// 生成测试节点数据
    pub fn generate_nodes(&self) -> Result<()> {
        println!("开始生成 {} 个测试节点...", self.config.node_count);
        let start_time = Instant::now();

        let batch_size = 100; // 每批处理100个
        let mut current_batch = 0;

        self.rt.block_on(async {
            while current_batch < self.config.node_count {
                let batch_end = std::cmp::min(current_batch + batch_size, self.config.node_count);
                let mut batch_cards = Vec::new();

                for i in current_batch..batch_end {
                    let card = self.create_test_card(i + 1);
                    batch_cards.push(card);
                }

                // 使用连接池发送请求
                let request = Request {
                    request_id: Uuid::new_v4().to_string(),
                    request_type: Some(RequestType::BatchCreateCard(BatchCreateCardRequest {
                        cards: batch_cards,
                    })),
                };

                match self.connection_pool.send_request(request).await {
                    Ok(response) => {
                        if response.code != 200 {
                            eprintln!("批量创建节点失败: {}", response.message);
                        }
                    }
                    Err(e) => {
                        eprintln!("发送请求失败: {:?}", e);
                    }
                }

                current_batch = batch_end;

                if current_batch % 1000 == 0 {
                    let elapsed = start_time.elapsed();
                    let rate = current_batch as f64 / elapsed.as_secs_f64();
                    println!("已生成 {} 个节点，速度: {:.0} nodes/s", current_batch, rate);
                }
            }

            Ok::<(), anyhow::Error>(())
        })?;

        let elapsed = start_time.elapsed();
        let rate = self.config.node_count as f64 / elapsed.as_secs_f64();
        println!(
            "节点生成完成，总共 {} 个，耗时 {:?}，平均速度: {:.0} nodes/s",
            self.config.node_count, elapsed, rate
        );

        Ok(())
    }

    /// 生成测试边数据
    pub fn generate_edges(&self) -> Result<()> {
        println!("开始生成 {} 条测试边...", self.config.edge_count);
        let start_time = Instant::now();

        let batch_size = 100; // 每批处理100条边
        let mut current_batch = 0;

        self.rt.block_on(async {
            while current_batch < self.config.edge_count {
                let batch_end = std::cmp::min(current_batch + batch_size, self.config.edge_count);
                let mut batch_links = Vec::new();

                for i in current_batch..batch_end {
                    let link = self.create_test_link(i + 1);
                    batch_links.push(link);
                }

                // 使用连接池发送请求
                let request = Request {
                    request_id: Uuid::new_v4().to_string(),
                    request_type: Some(RequestType::BatchCreateLink(BatchCreateLinkRequest {
                        links: batch_links,
                    })),
                };

                match self.connection_pool.send_request(request).await {
                    Ok(response) => {
                        if response.code != 200 {
                            eprintln!("批量创建边失败: {}", response.message);
                        }
                    }
                    Err(e) => {
                        eprintln!("发送边请求失败: {:?}", e);
                    }
                }

                current_batch = batch_end;

                if current_batch % 10000 == 0 {
                    let elapsed = start_time.elapsed();
                    let rate = current_batch as f64 / elapsed.as_secs_f64();
                    println!("已生成 {} 条边，速度: {:.0} edges/s", current_batch, rate);
                }
            }

            Ok::<(), anyhow::Error>(())
        })?;

        let elapsed = start_time.elapsed();
        let rate = self.config.edge_count as f64 / elapsed.as_secs_f64();
        println!(
            "边生成完成，总共 {} 条，耗时 {:?}，平均速度: {:.0} edges/s",
            self.config.edge_count, elapsed, rate
        );

        Ok(())
    }

    /// 创建测试卡片
    fn create_test_card(&self, id: u32) -> Card {
        let mut rng = rand::rng();
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs();

        // 随机分配卡片类型
        let card_types = vec!["task", "bug", "feature", "epic", "story"];
        let card_type = card_types[rng.random_range(0..card_types.len())];

        // 随机分配容器
        let container_id = format!("container_{}", rng.random_range(1..11));

        // 随机状态
        let state = match rng.random_range(0..10) {
            0 => ValueUnitState::Abandon as i32,
            1..=3 => ValueUnitState::InProgress as i32,
            _ => ValueUnitState::Archived as i32,
        };

        Card {
            id: format!("card_{}", id),
            org_id: "test_org".to_string(),
            card_type_id: card_type.to_string(),
            container_id,
            stream_id: "stream_1".to_string(),
            step_id: "step_1".to_string(),
            status_id: "status_1".to_string(),
            state,
            title: Some(Title {
                title_type: Some(TitleType::Pure(PureTitle {
                    value: format!("测试卡片 {}", id),
                })),
            }),
            code_in_org: id as i64,
            custom_code: String::new(),
            description: format!("测试卡片 {} 的描述", id),
            position: 0,
            created: now as i64,
            updated: now as i64,
            discarded_at: 0,
            archived_at: 0,
            comment_date: 0,
            discard_reason: String::new(),
            restore_reason: String::new(),
            tags: vec![],
            custom_field_value_map: HashMap::new(),
            derive_field_value_map: HashMap::new(),
            link_card_map: HashMap::new(),
        }
    }

    /// 创建测试链接
    fn create_test_link(&self, id: u32) -> Link {
        let mut rng = rand::rng();

        // 随机选择源和目标节点
        let src_id = format!("card_{}", rng.random_range(1..=self.config.node_count));
        let dest_id = format!("card_{}", rng.random_range(1..=self.config.node_count));

        // 关联关系类型
        let link_types = vec![
            "depends_on",
            "blocks",
            "relates_to",
            "parent_of",
            "child_of",
        ];
        let lt_id = link_types[rng.random_range(0..link_types.len())];

        Link {
            id: format!("link_{}", id),
            lt_id: lt_id.to_string(),
            src_id,
            dest_id,
            field_values: vec![], // 简化测试，不添加字段值
        }
    }
}

/// 并发数据库操作测试客户端
pub struct ConcurrentDatabaseClient {
    config: TestConfig,
    connection_pool: Arc<TcpConnectionPool>,
}

impl ConcurrentDatabaseClient {
    pub fn new(config: TestConfig) -> Self {
        let server_addr = format!("127.0.0.1:{}", config.server_port);
        let connection_pool = Arc::new(TcpConnectionPool::new(server_addr, 500));

        Self {
            config,
            connection_pool,
        }
    }

    /// 运行并发测试
    pub fn run_concurrent_test(&self) -> Result<PerformanceStats> {
        println!("开始运行并发数据库操作测试...");
        println!(
            "配置: {} 个并发线程，测试时长 {} 秒，写请求比例 {:.1}%",
            self.config.concurrent_threads,
            self.config.test_duration_secs,
            self.config.write_ratio * 100.0
        );

        // 创建共享的 Tokio 运行时
        let rt = Arc::new(Runtime::new()?);
        let barrier = Arc::new(Barrier::new(self.config.concurrent_threads));
        let stop_flag = Arc::new(Mutex::new(false));
        let read_latencies = Arc::new(Mutex::new(Vec::new()));
        let write_latencies = Arc::new(Mutex::new(Vec::new()));
        let mut handles = Vec::new();

        let start_time = Instant::now();

        // 启动并发线程
        for thread_id in 0..self.config.concurrent_threads {
            let barrier = Arc::clone(&barrier);
            let stop_flag = Arc::clone(&stop_flag);
            let read_latencies = Arc::clone(&read_latencies);
            let write_latencies = Arc::clone(&write_latencies);
            let config = self.config.clone();
            let connection_pool = Arc::clone(&self.connection_pool);
            let rt = Arc::clone(&rt); // 共享运行时

            let handle = thread::spawn(move || {
                // 使用共享的 tokio 运行时
                rt.block_on(async {
                    Self::run_client_thread(
                        thread_id,
                        barrier,
                        stop_flag,
                        read_latencies,
                        write_latencies,
                        config,
                        connection_pool,
                    )
                    .await
                });
            });
            handles.push(handle);
        }

        // 等待指定时间后停止测试
        thread::sleep(Duration::from_secs(self.config.test_duration_secs));
        *stop_flag.lock().unwrap() = true;

        // 等待所有线程完成
        for handle in handles {
            handle.join().unwrap();
        }

        let total_duration = start_time.elapsed();

        // 计算统计信息
        let mut read_latency_data = read_latencies.lock().unwrap().clone();
        let mut write_latency_data = write_latencies.lock().unwrap().clone();
        let mut stats = PerformanceStats::new();
        stats.calculate_from_typed_latencies(
            &mut read_latency_data,
            &mut write_latency_data,
            total_duration,
        );
        stats.successful_requests = stats.total_requests;

        Ok(stats)
    }

    /// 客户端线程执行函数
    async fn run_client_thread(
        thread_id: usize,
        barrier: Arc<Barrier>,
        stop_flag: Arc<Mutex<bool>>,
        read_latencies: Arc<Mutex<Vec<Duration>>>,
        write_latencies: Arc<Mutex<Vec<Duration>>>,
        config: TestConfig,
        connection_pool: Arc<TcpConnectionPool>,
    ) {
        // 等待所有线程准备就绪
        barrier.wait();

        let mut rng = rand::thread_rng();
        let mut thread_read_latencies = Vec::new();
        let mut thread_write_latencies = Vec::new();

        while !*stop_flag.lock().unwrap() {
            let start = Instant::now();

            // 决定是读请求还是写请求
            let is_write = rng.gen::<f64>() < config.write_ratio;

            match Self::execute_database_operation(&connection_pool, is_write, &mut rng).await {
                Ok(_) => {
                    let latency = start.elapsed();
                    if is_write {
                        thread_write_latencies.push(latency);
                    } else {
                        thread_read_latencies.push(latency);
                    }
                }
                Err(e) => {
                    // 检查是否是因为运行时关闭导致的错误
                    let error_msg = format!("{:?}", e);
                    if error_msg.contains("shutdown") || error_msg.contains("Tokio") {
                        // 如果是运行时关闭错误，直接退出循环
                        break;
                    }
                    eprintln!("线程 {} 操作失败: {:?}", thread_id, e);
                }
            }

            // 检查停止标志，如果设置了就立即退出
            if *stop_flag.lock().unwrap() {
                break;
            }

            // 小延迟避免过度占用CPU
            tokio::time::sleep(Duration::from_millis(1)).await;
        }

        // 将线程的延迟数据合并到全局统计中
        if let Ok(mut global_read_latencies) = read_latencies.lock() {
            global_read_latencies.extend(thread_read_latencies);
        }
        if let Ok(mut global_write_latencies) = write_latencies.lock() {
            global_write_latencies.extend(thread_write_latencies);
        }
        println!("线程 {} 完成", thread_id);
    }

    /// 执行数据库操作
    async fn execute_database_operation(
        connection_pool: &Arc<TcpConnectionPool>,
        is_write: bool,
        rng: &mut rand::rngs::ThreadRng,
    ) -> Result<()> {
        if is_write {
            Self::execute_write_operation(connection_pool, rng).await?;
        } else {
            Self::execute_read_operation(connection_pool, rng).await?;
        }

        Ok(())
    }

    /// 执行写操作
    async fn execute_write_operation(
        connection_pool: &Arc<TcpConnectionPool>,
        rng: &mut rand::rngs::ThreadRng,
    ) -> Result<()> {
        match rng.random_range(0..10) {
            //10%创建，90%更新
            0 => {
                // 创建新卡片
                let id = NEXT_CARD_ID.fetch_add(1, std::sync::atomic::Ordering::Relaxed);
                let card = Self::create_random_card(id as u32, rng);

                let request = Request {
                    request_id: Uuid::new_v4().to_string(),
                    request_type: Some(RequestType::BatchCreateCard(BatchCreateCardRequest {
                        cards: vec![card],
                    })),
                };

                let _response = connection_pool.send_request(request).await?;
            }
            _ => {
                // 更新卡片
                let id = rng.random_range(1..10000);
                let mut card = Self::create_random_card(id, rng);
                card.id = format!("card_{}", id); // 使用已有的卡片ID
                card.description = format!("更新的描述 {}", rng.random::<u32>());

                let request = Request {
                    request_id: Uuid::new_v4().to_string(),
                    request_type: Some(RequestType::BatchUpdateCard(BatchUpdateCardRequest {
                        cards: vec![card],
                    })),
                };

                let _response = connection_pool.send_request(request).await?;
            }
        }

        Ok(())
    }

    /// 执行读操作
    async fn execute_read_operation(
        connection_pool: &Arc<TcpConnectionPool>,
        rng: &mut rand::rngs::ThreadRng,
    ) -> Result<()> {
        // 70%的请求使用card_ids查询，30%使用card_type_ids查询
        if rng.random_range(0..100) < 70 {
            // 查询特定的卡片ID（1~10个）
            let card_count = rng.random_range(1..=10);
            let mut card_ids = Vec::new();

            for _ in 0..card_count {
                // 从已生成的测试节点中随机选择
                let card_id = format!("card_{}", rng.random_range(1..=10000));
                card_ids.push(card_id);
            }

            let request = Request {
                request_id: Uuid::new_v4().to_string(),
                request_type: Some(RequestType::CardQuery(CardQueryRequest {
                    query_context: None,
                    query_scope: Some(QueryScope {
                        card_type_ids: vec![], // card_ids查询时card_type_ids为空
                        card_ids,
                        container_ids: vec![],
                        states: vec![],
                    }),
                    condition: None,
                    rtn: None,
                    sort_and_page: None,
                })),
            };

            let _response = connection_pool.send_request(request).await?;
        } else {
            // 查询特定卡片类型
            let card_types = vec!["task", "bug", "feature"];
            let card_type = card_types[rng.random_range(0..card_types.len())];

            let request = Request {
                request_id: Uuid::new_v4().to_string(),
                request_type: Some(RequestType::CardQuery(CardQueryRequest {
                    query_context: None,
                    query_scope: Some(QueryScope {
                        card_type_ids: vec![card_type.to_string()],
                        card_ids: vec![], // card_type_ids查询时card_ids为空
                        container_ids: vec![],
                        states: vec![],
                    }),
                    condition: None,
                    rtn: None,
                    sort_and_page: None,
                })),
            };

            let _response = connection_pool.send_request(request).await?;
        }

        Ok(())
    }

    /// 创建随机卡片
    fn create_random_card(id: u32, rng: &mut rand::rngs::ThreadRng) -> Card {
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs();
        let card_types = vec![
            "task", "bug", "feature", "card_type1", "card_type2", "card_type3", "card_type4", "card_type5", "card_type6", "card_type7",
            "card_type8", "card_type9", "card_type10", "card_type11", "card_type12", "card_type13", "card_type14", "card_type15", "card_type16", "card_type17",
            "card_type18", "card_type19", "card_type20", "card_type21", "card_type22", "card_type23", "card_type24", "card_type25", "card_type26", "card_type27",
        ];
        let card_type = card_types[rng.random_range(0..card_types.len())];

        let container_id = format!("container_{}", rng.random_range(1..11));

        let state = match rng.random_range(0..4) {
            0 => ValueUnitState::Abandon as i32,
            1 => ValueUnitState::InProgress as i32,
            2 => ValueUnitState::Archived as i32,
            _ => ValueUnitState::Deleted as i32,
        };

        Card {
            id: format!("card_{}", id),
            org_id: "test_org".to_string(),
            card_type_id: card_type.to_string(),
            container_id,
            stream_id: "stream_1".to_string(),
            step_id: "step_1".to_string(),
            status_id: "status_1".to_string(),
            state,
            title: Some(Title {
                title_type: Some(TitleType::Pure(PureTitle {
                    value: format!("随机卡片 {}", id),
                })),
            }),
            code_in_org: id as i64,
            custom_code: String::new(),
            description: format!("随机卡片描述 {}", id),
            position: 0,
            created: now as i64,
            updated: now as i64,
            archived_at: 0,
            discarded_at: 0,
            comment_date: 0,
            discard_reason: String::new(),
            restore_reason: String::new(),
            tags: vec![],
            custom_field_value_map: HashMap::new(),
            derive_field_value_map: HashMap::new(),
            link_card_map: HashMap::new(),
        }
    }
}

/// 启动测试服务器
async fn start_test_server(db: Arc<RocksDatabase>, config: TestConfig) -> Result<()> {
    let server_config = ServerConfig {
        listen_address: "127.0.0.1".to_string(),
        listen_port: config.server_port,
        allowed_clients: None,
        db_path: "/tmp/test_db".to_string(),
        db_cache_size_mb: Some(1024),
        db_write_buffer_size_mb: Some(128),
        db_max_open_files: Some(64),
        db_max_background_jobs: Some(4),
        db_vertex_lru_cache_size: Some(100_000),
        log_level: Some("info".to_string()),
        log_rotation: None,
        authentication: None,
        thread_pool_size: None,
        cluster_config: None,
    };

    let db_config = pgraph::database::rdb::rdb_config::RocksDbConfig::default();
    let db_wrapped = Arc::new(RwLock::new(db));
    start_server(db_wrapped, db_config, Arc::new(server_config)).await?;
    Ok(())
}

/// 打印性能测试报告
fn print_performance_report(stats: &PerformanceStats, config: &TestConfig) {
    println!("\n=== pgraph 高并发性能测试报告（通过TCP连接）===");
    println!("测试配置:");
    println!("  - 节点数量: {}", config.node_count);
    println!("  - 并发线程数: {}", config.concurrent_threads);
    println!("  - 测试时长: {} 秒", config.test_duration_secs);
    println!("  - 写请求比例: {:.1}%", config.write_ratio * 100.0);
    println!("  - 服务器端口: {}", config.server_port);

    println!("\n性能指标:");
    println!("  - 总请求数: {}", stats.total_requests);
    println!("  - 成功请求数: {}", stats.successful_requests);
    println!("  - 失败请求数: {}", stats.failed_requests);
    println!(
        "  - 总测试时长: {:.2} 秒",
        stats.total_duration.as_secs_f64()
    );

    println!("\n延迟统计:");
    println!(
        "  - 平均延迟: {:.2} ms",
        stats.avg_latency.as_secs_f64() * 1000.0
    );
    println!(
        "  - 最小延迟: {:.2} ms",
        stats.min_latency.as_secs_f64() * 1000.0
    );
    println!(
        "  - 最大延迟: {:.2} ms",
        stats.max_latency.as_secs_f64() * 1000.0
    );
    println!(
        "  - P95 延迟: {:.2} ms",
        stats.p95_latency.as_secs_f64() * 1000.0
    );
    println!(
        "  - P99 延迟: {:.2} ms",
        stats.p99_latency.as_secs_f64() * 1000.0
    );

    println!("\n吞吐量:");
    println!("  - 总 QPS: {:.2}", stats.qps);
    println!("  - 读请求 QPS: {:.2}", stats.read_qps);
    println!("  - 写请求 QPS: {:.2}", stats.write_qps);

    println!("\n读写请求分析:");
    println!(
        "  - 读请求数量: {} ({:.1}%)",
        stats.read_requests,
        (stats.read_requests as f64 / stats.total_requests as f64) * 100.0
    );
    println!(
        "  - 写请求数量: {} ({:.1}%)",
        stats.write_requests,
        (stats.write_requests as f64 / stats.total_requests as f64) * 100.0
    );
    println!(
        "  - 读请求平均延迟: {:.2} ms",
        stats.read_avg_latency.as_secs_f64() * 1000.0
    );
    println!(
        "  - 写请求平均延迟: {:.2} ms",
        stats.write_avg_latency.as_secs_f64() * 1000.0
    );

    println!("\n=== 测试报告结束 ===\n");
}

/// 高并发测试主函数
fn run_high_concurrency_test() -> Result<()> {
    let config = TestConfig::default();

    // 创建临时数据库
    let temp_dir = tempdir()?;
    let db_path = temp_dir.path().join("test_db");
    let db = Arc::new(RocksDatabase::new(db_path.to_str().unwrap()));

    println!("=== 开始高并发性能测试（通过TCP连接）===");

    // 创建tokio运行时
    let rt = Arc::new(Runtime::new()?);
    let rt_clone = Arc::clone(&rt);
    let db_clone = Arc::clone(&db);
    let config_clone = config.clone();

    // 在后台启动服务器
    let server_handle = rt.spawn(async move {
        if let Err(e) = start_test_server(db_clone, config_clone).await {
            eprintln!("服务器启动失败: {:?}", e);
        }
    });

    // 等待服务器启动
    thread::sleep(Duration::from_secs(2));

    // 生成测试数据
    println!("1. 生成测试数据...");
    let data_generator = TestDataGenerator::new(config.clone(), rt_clone);
    data_generator.generate_nodes()?;
    data_generator.generate_edges()?;

    // 运行并发测试
    println!("2. 执行并发数据库操作测试...");
    let client = ConcurrentDatabaseClient::new(config.clone());
    let stats = client.run_concurrent_test()?;

    // 打印测试报告
    print_performance_report(&stats, &config);

    // 停止服务器
    server_handle.abort();

    Ok(())
}

/// Criterion基准测试函数
fn bench_high_concurrency(c: &mut Criterion) {
    c.bench_function("high_concurrency_tcp_test", |b| {
        b.iter(|| {
            black_box(run_high_concurrency_test().unwrap());
        });
    });
}

criterion_group! {
    name = benches;
    config = Criterion::default()
        .sample_size(3)
        .measurement_time(Duration::from_secs(90))
        .warm_up_time(Duration::from_secs(10));
    targets = bench_high_concurrency
}

criterion_main!(benches);

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_high_concurrency() {
        if let Err(e) = run_high_concurrency_test() {
            eprintln!("高并发测试失败: {:?}", e);
            panic!("测试失败");
        }
    }
}
