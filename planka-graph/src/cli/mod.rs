/// SQL解析器模块
pub mod sql_parser;

/// 查询转换器模块
pub mod query_converter;

/// 结果处理器模块
pub mod result_processor;

// 重新导出主要的公共接口
pub use sql_parser::{SqlParser, ParsedSqlQuery, SqlQueryType};
pub use query_converter::QueryConverter;
pub use result_processor::ResultProcessor;

use crate::proto::pgraph::admin::{UpdateLogLevelRequest, DatabaseStatsRequest};
/**
 * pgraph CLI 模块 - 提供命令行客户端功能
 */
use crate::proto::{pgraph, CardQueryRequest, QueryScope, Yield, YieldedField};
use crate::raft::client::RaftClient;
use anyhow::{anyhow, Context, Result};
use clap::Parser;
use dialoguer::{Input, Password};
use prost::Message;
use rustyline::config::Config;
use rustyline::error::ReadlineError;
use rustyline::history::FileHistory;
use rustyline::Editor;
use std::collections::BTreeSet;
use std::fs;
use std::path::PathBuf;
use std::str::FromStr;
use std::sync::Arc;
use std::time::Duration;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpStream;
use tokio::sync::Mutex;
use tokio::time::timeout;
use uuid::Uuid;

// 定义CLI的命令行参数
#[derive(Parser, Debug)]
#[command(author, version, about = "pgraph CLI 客户端")]
pub struct Args {
    /// 服务器地址
    #[arg(short = 's', long = "server_addr")]
    pub server_addr: Option<String>,

    /// API服务地址（用于Raft集群管理）
    #[arg(long = "rpc-addr")]
    pub rpc_addr: Option<String>,

    /// 用户名
    #[arg(short = 'u', long = "username")]
    pub username: Option<String>,

    /// 密码
    #[arg(short = 'w', long = "password")]
    pub password: Option<String>,

}

pub struct pgraphClient {
    stream: Arc<Mutex<TcpStream>>,
    timeout: Duration,
    raft_client: RaftClient,
}

impl pgraphClient {
    pub async fn connect(addr: std::net::SocketAddr, rpc_addr: String) -> Result<Self> {
        let connect_timeout = Duration::from_secs(5);
        let stream = timeout(connect_timeout, TcpStream::connect(addr))
            .await
            .context("Connection timeout")?
            .context("Connection failed")?;

        // 初始化时随机指定leader
        let raft_client = RaftClient::new(1, rpc_addr.clone());

        Ok(Self {
            stream: Arc::new(Mutex::new(stream)),
            timeout: Duration::from_secs(30), // 默认30秒超时
            raft_client,
        })
    }

    async fn send_message(&self, payload: &[u8]) -> Result<Vec<u8>> {
        let mut stream = self.stream.lock().await;

        // 先发送4字节的消息长度前缀
        let payload_len = payload.len() as u32;
        let len_bytes = payload_len.to_be_bytes();
        stream.write_all(&len_bytes).await?;

        // 再发送消息体
        stream.write_all(payload).await?;
        stream.flush().await?;

        // 读取响应
        // 首先读取4字节的长度前缀
        let mut len_buf = [0u8; 4];
        timeout(self.timeout, stream.read_exact(&mut len_buf))
            .await
            .context("Reading response length timeout")?
            .context("Reading response length failed")?;

        let response_len = u32::from_be_bytes(len_buf) as usize;


        // 然后根据长度读取完整的响应
        let mut response_buf = vec![0u8; response_len];
        timeout(self.timeout, stream.read_exact(&mut response_buf))
            .await
            .context("Reading response body timeout")?
            .context("Reading response body failed")?;

        Ok(response_buf)
    }

    pub async fn authenticate(
        &mut self,
        username: Option<String>,
        password: Option<String>,
    ) -> Result<()> {
        // 不管是否提供了用户名和密码，都尝试进行认证
        // 如果未提供，服务器可能允许匿名访问或拒绝未认证连接
        let auth_request = pgraph::auth::AuthRequest {
            username: username.unwrap_or_default(),
            password: password.unwrap_or_default(),
        };

        let request = pgraph::request::Request {
            request_id: Uuid::new_v4().to_string(),
            request_type: Some(pgraph::request::request::RequestType::Auth(auth_request)),
        };

        let mut buf = Vec::new();
        request.encode(&mut buf)?;

        let response_buf = self.send_message(&buf).await?;
        let response = pgraph::response::Response::decode(&response_buf[..])?;

        if response.code != 200 {
            return Err(anyhow!("Authentication failed: {}", response.message));
        }

        if let Some(pgraph::response::response::ResponseType::AuthResponse(auth_response)) =
            response.response_type
        {
            if !auth_response.success {
                return Err(anyhow!("Authentication failed: {}", auth_response.message));
            }
        } else {
            return Err(anyhow!("Authentication response format error"));
        }

        Ok(())
    }

    pub async fn run_cli(&mut self) -> Result<()> {
        println!("pgraph CLI Commands:");
        println!("  get <key>  - Get a single card by ID");
        println!("  mget <key1> <key2> ... - Batch get cards by multiple IDs");
        println!("  SELECT ... - Execute SQL query (SELECT/COUNT operations)");
        println!("  init-cluster - Initialize Raft cluster");
        println!("  add-learner <node_id> <rpc_addr> - Add learner node");
        println!("  change-membership <node_id1,node_id2,...> - Change cluster membership");
        println!("  cluster-status - Get cluster status");
        println!("  trigger-snapshot - Manually trigger snapshot creation");
        println!("  log-level <level> - Update log level, supports simple format (trace/debug/info/warn/error) or complex format (pgraph=debug,tokio=info)");
        println!("  db-stats [--details] - Show database statistics (vertices, edges, cache, memory usage)");
        println!("  exit       - Exit program");
        println!("  help       - Show help information");
        println!("  help select - Show detailed SQL query syntax");
        println!();
        println!("Quick start: try 'SELECT * FROM `your_card_type_id`' or 'help select' for detailed SQL syntax");

        // 创建rustyline编辑器并配置
        let config = Config::builder()
            .history_ignore_space(true)
            .completion_type(rustyline::CompletionType::List)
            .build();
        let mut rl = Editor::<(), FileHistory>::with_config(config)?;

        // 设置历史文件
        let history_dir = dirs::home_dir()
            .unwrap_or_else(|| PathBuf::from("."))
            .join(".pgraph");
        fs::create_dir_all(&history_dir).context("Unable to create history directory")?;
        let history_path = history_dir.join("history.txt");

        // 尝试加载历史记录
        if rl.load_history(&history_path).is_err() {}

        loop {
            // 使用rustyline读取输入，支持上下键历史记录和编辑
            let readline = rl.readline("pgraph> ");
            match readline {
                Ok(line) => {
                    let input = line.trim();
                    if input.is_empty() {
                        continue;
                    }

                    // 将命令添加到历史记录
                    rl.add_history_entry(input)?;

                    let parts: Vec<&str> = input.split_whitespace().collect();
                    match parts[0] {
                        "get" => {
                            if parts.len() != 2 {
                                println!("Usage: get <key>");
                                continue;
                            }
                            match self.handle_get(parts[1]).await {
                                Ok(_) => (),
                                Err(e) => println!("Error: {}", e),
                            }
                        }
                        "mget" => {
                            if parts.len() < 2 {
                                println!("Usage: mget <key1> <key2> ...");
                                continue;
                            }
                            let keys: Vec<&str> = parts[1..].to_vec();
                            match self.handle_mget(&keys).await {
                                Ok(_) => (),
                                Err(e) => println!("Error: {}", e),
                            }
                        }

                        "init-cluster" => match self.handle_init_cluster().await {
                            Ok(_) => (),
                            Err(e) => println!("Error: {}", e),
                        },
                        "add-learner" => {
                            if parts.len() != 3 {
                                println!("Usage: add-learner <node_id> <rpc_addr>");
                                continue;
                            }
                            let node_id = match parts[1].parse::<u64>() {
                                Ok(id) => id,
                                Err(_) => {
                                    println!("Error: Invalid node ID");
                                    continue;
                                }
                            };
                            match self.handle_add_learner(node_id, parts[2]).await {
                                Ok(_) => (),
                                Err(e) => println!("Error: {}", e),
                            }
                        }
                        "change-membership" => {
                            if parts.len() != 2 {
                                println!("Usage: change-membership <node_id1,node_id2,...>");
                                continue;
                            }
                            let node_ids_result: Result<BTreeSet<u64>> = parts[1]
                                .split(',')
                                .map(|s| s.parse::<u64>().map_err(|_| anyhow!("Invalid node ID")))
                                .collect();

                            match node_ids_result {
                                Ok(node_ids) => {
                                    match self.handle_change_membership(node_ids).await {
                                        Ok(_) => (),
                                        Err(e) => println!("Error: {}", e),
                                    }
                                }
                                Err(e) => println!("Error: {}", e),
                            }
                        }
                        "cluster-status" => match self.handle_cluster_status().await {
                            Ok(_) => (),
                            Err(e) => println!("Error: {}", e),
                        },
                        "trigger-snapshot" => match self.handle_trigger_snapshot().await {
                            Ok(_) => (),
                            Err(e) => println!("Error: {}", e),
                        },
                        "log-level" => {
                            if parts.len() != 2 {
                                println!("Usage: log-level <level>");
                                println!("Supports simple format: trace, debug, info, warn, error");
                                println!("Supports complex format: pgraph=debug,tokio=info,tonic=error,info");
                                continue;
                            }
                            match self.handle_update_log_level(parts[1]).await {
                                Ok(_) => (),
                                Err(e) => println!("Error: {}", e),
                            }
                        },
                        "db-stats" => {
                            let include_details = parts.len() > 1 && parts[1] == "--details";
                            match self.handle_database_stats(include_details).await {
                                Ok(_) => (),
                                Err(e) => println!("Error: {}", e),
                            }
                        },
                        "help" => {
                            if parts.len() > 1 && parts[1] == "select" {
                                self.show_select_help();
                            } else {
                                println!("pgraph CLI Commands:");
                                println!("  get <key>  - Get a single card by ID");
                                println!("  mget <key1> <key2> ... - Batch get cards by multiple IDs");
                                println!("  SELECT ... - Execute SQL query (SELECT/COUNT operations)");
                                println!("  init-cluster - Initialize Raft cluster");
                                println!(
                                    "  add-learner <node_id> <rpc_addr> - Add learner node"
                                );
                                println!("  change-membership <node_id1,node_id2,...> - Change cluster membership");
                                println!("  cluster-status - Get cluster status");
                                println!("  trigger-snapshot - Manually trigger snapshot creation");
                                println!("  log-level <level> - Update log level, supports simple format (trace/debug/info/warn/error) or complex format (pgraph=debug,tokio=info)");
                                println!("  db-stats [--details] - Show database statistics (vertices, edges, cache, memory usage)");
                                println!("  exit       - Exit program");
                                println!("  help       - Show help information");
                                println!("  help select - Show detailed SQL query syntax");
                                println!();
                                println!("Quick SQL Examples:");
                                println!("  SELECT * FROM `（卡片类型ID）`");
                                println!("  SELECT id, title FROM `（卡片类型ID）` WHERE state = 'InProgress'");
                                println!("  SELECT COUNT(*) FROM `（卡片类型ID）` WHERE container_id = 'container1'");
                                println!("  SELECT * FROM `（卡片类型ID）` WHERE title CONTAINS 'test' ORDER BY created DESC LIMIT 10");
                            }
                        }
                        "exit" => {
                            println!("Bye!");
                            break;
                        }
                        _ => {
                            // 检查是否是以SELECT开头的SQL语句
                            let input_upper = input.to_uppercase();
                            if input_upper.starts_with("SELECT") {
                                match self.handle_query(input).await {
                                    Ok(_) => (),
                                    Err(e) => println!("Error: {}", e),
                                }
                            } else {
                                println!("Unknown command: {}, type 'help' to see available commands", parts[0]);
                            }
                        }
                    }
                }
                Err(ReadlineError::Interrupted) => {
                    println!("Bye!");
                    break;
                }
                Err(ReadlineError::Eof) => {
                    println!("Bye!");
                    break;
                }
                Err(err) => {
                    println!("Input reading error: {}", err);
                    break;
                }
            }
        }

        // 保存历史记录
        rl.save_history(&history_path)?;

        Ok(())
    }

    /// 显示SQL查询命令的详细帮助信息
    fn show_select_help(&self) {
        println!("=== pgraph SQL 查询语法详细说明 ===");
        println!();
        
        println!("📝 基本语法:");
        println!("   SELECT [字段列表|*|COUNT(*)] FROM `表名` [WHERE 条件] [ORDER BY 字段 ASC|DESC] [LIMIT 数量]");
        println!();
        
        println!("💡 语法特性:");
        println!("  • 关键字大小写不敏感 (SELECT, FROM, WHERE, ORDER BY, LIMIT, AND, IN, CONTAINS 等)");
        println!("  • 支持单引号、双引号、反引号包裹字符串值和表名");
        println!("  • 支持多个 AND 条件组合");
        println!("  • 字段名和值严格校验");
        println!();
        
        println!("🔍 支持的查询类型:");
        println!("  • SELECT * FROM `card_type_id`                    - 查询所有字段");
        println!("  • SELECT 字段1, 字段2 FROM `card_type_id`         - 查询指定字段");
        println!("  • SELECT COUNT(*) FROM `card_type_id`             - 计数查询");
        println!();
        
        println!("📋 支持的字段:");
        println!("  内置字段:");
        println!("    • id, card_id          - 卡片ID (两者等价)");
        println!("    • org_id               - 组织ID");
        println!("    • card_type_id               - 卡片类型ID");
        println!("    • container_id         - 容器ID");
        println!("    • state                - 卡片状态 (支持值: InProgress, Archived, Abandon, Deleted)");
        println!("    • title                - 标题");
        println!("    • code, code_in_org    - 编码");
        println!("    • custom_code          - 自定义编码");
        println!("    • created, updated     - 创建/更新时间");
        println!("    • desc                 - 描述");
        println!("    • stream_id            - 流程ID");
        println!("    • step_id              - 步骤ID");
        println!("    • status_id            - 状态ID");
        println!("    • archived_at         - 归档日期");
        println!("    • discarded_at         - 放弃日期");
        println!("    • comment_date         - 评论日期");
        println!("    • discard_reason       - 放弃原因");
        println!("    • restore_reason       - 还原原因");
        println!();
        
        println!("🔎 WHERE 条件语法:");
        println!("  支持的操作符:");
        println!("    • =                    - 等于 (适用于: id, card_id, container_id, stream_id, step_id, status_id, state)");
        println!("    • IN (值1, 值2, ...)   - 包含 (适用于: id, card_id, container_id, stream_id, step_id, status_id, state)");
        println!("    • CONTAINS             - 包含文本 (适用于: title, desc, code)");
        println!("    • AND                  - 逻辑与，连接多个条件");
        println!();
        
        println!("  单条件示例:");
        println!("    • WHERE id = 'card123'");
        println!("    • WHERE card_id IN ('id1', 'id2', 'id3')");
        println!("    • WHERE state = 'InProgress'");
        println!("    • WHERE state IN ('InProgress', 'Archived', 'Abandon', 'Deleted')");
        println!("    • WHERE title CONTAINS '测试'");
        println!("    • WHERE desc CONTAINS '重要'");
        println!("    • WHERE code CONTAINS 'PROJ'");
        println!("    • WHERE container_id = 'container123'");
        println!("    • WHERE stream_id = 'stream456'");
        println!();
        
        println!("  多条件组合 (AND):");
        println!("    • WHERE state = 'InProgress' AND container_id = 'container1'");
        println!("    • WHERE title CONTAINS '项目' AND state IN ('InProgress', 'Archived')");
        println!("    • WHERE container_id = 'c1' AND code CONTAINS 'TEST' AND state = 'InProgress'");
        println!("    • WHERE id = 'card123' AND title CONTAINS '重要' AND state = 'InProgress'");
        println!();
        
        println!("📊 排序和分页:");
        println!("  ORDER BY 语法:");
        println!("    • ORDER BY created DESC         - 按创建时间降序");
        println!("    • ORDER BY title ASC            - 按标题升序");
        println!("    • ORDER BY updated DESC         - 按更新时间降序");
        println!("    • ORDER BY position ASC         - 按位置升序");
        println!();
        
        println!("  LIMIT 语法:");
        println!("    • LIMIT 10                      - 限制返回10条记录");
        println!("    • LIMIT 100                     - 限制返回100条记录");
        println!();
        
        println!("💡 完整示例:");
        println!("  1. 基本查询:");
        println!("     SELECT * FROM `123456`");
        println!("     query SELECT * FROM `123456`");
        println!();
        
        println!("  2. 条件查询:");
        println!("     SELECT id, title, state FROM `123456` WHERE state = 'InProgress'");
        println!("     SELECT * FROM `123456` WHERE container_id = 'project_a'");
        println!();
        
        println!("  3. 文本搜索:");
        println!("     SELECT * FROM `123456` WHERE title CONTAINS '需求'");
        println!("     SELECT * FROM `123456` WHERE desc CONTAINS '重要功能'");
        println!();
        
        println!("  4. 批量条件:");
        println!("     SELECT * FROM `123456` WHERE card_id IN ('card1', 'card2', 'card3')");
        println!("     SELECT * FROM `123456` WHERE state IN ('InProgress', 'Archived')");
        println!();
        
        println!("  5. 排序和分页:");
        println!("     SELECT * FROM `123456` ORDER BY created DESC LIMIT 20");
        println!("     SELECT * FROM `123456` WHERE state = 'InProgress' ORDER BY updated DESC LIMIT 10");
        println!();
        
        println!("  6. 计数查询:");
        println!("     SELECT COUNT(*) FROM `123456`");
        println!("     SELECT COUNT(*) FROM `123456` WHERE state = 'InProgress'");
        println!();
        
        println!("  7. 大小写不敏感示例:");
        println!("     select * from `123456` where state = 'InProgress' and container_id = 'project_a'");
        println!("     Select * From `123456` Where title Contains '需求' And state In ('InProgress', 'Archived')");
        println!("     SELECT * FROM `123456` where state = 'InProgress' AND title contains '重要' order by created desc limit 10");
        println!();
        
        println!("⚠️  注意事项:");
        println!("  • 表名必须用引号包围: `card_type_id` 或 'card_type_id' 或 \"card_type_id\"");
        println!("  • 字符串值必须用单引号或双引号包围: 'value' 或 \"value\"");
        println!("  • 字段名区分大小写，只支持预定义的内置字段");
        println!("  • 支持的状态值: 'InProgress', 'Archived', 'Abandon', 'Deleted'");
        println!("  • WHERE条件目前只支持AND连接，不支持OR");
        println!("  • 不支持的字段将返回错误信息");
        println!();
    }

    /// 处理SQL查询命令
    async fn handle_query(&mut self, sql: &str) -> Result<()> {
        println!("执行SQL查询: {}", sql);
        
        // 解析SQL
        let parsed_query = match SqlParser::parse(sql) {
            Ok(query) => query,
            Err(e) => {
                println!("SQL解析失败: {}", e);
                return Ok(());
            }
        };

        // 转换为gRPC请求
        let request = match QueryConverter::create_request(&parsed_query) {
            Ok(req) => req,
            Err(e) => {
                println!("查询转换失败: {}", e);
                return Ok(());
            }
        };

        // 序列化请求
        let mut buf = Vec::new();
        request.encode(&mut buf)?;

        // 发送请求
        let response_buf = self.send_message(&buf).await?;
        let response = pgraph::response::Response::decode(&response_buf[..])?;

        if response.code != 200 {
            println!("查询失败: {}", response.message);
            return Ok(());
        }

        // 处理响应
        match &parsed_query.query_type {
            SqlQueryType::Count => {
                if let Some(pgraph::response::response::ResponseType::CardCountResponse(count_response)) =
                    response.response_type
                {
                    ResultProcessor::process_card_count_response(&count_response)?;
                } else {
                    println!("响应格式错误: 期望CardCountResponse");
                }
            }
            SqlQueryType::Select { .. } => {
                if let Some(pgraph::response::response::ResponseType::CardQueryResponse(query_response)) =
                    response.response_type
                {
                    ResultProcessor::process_card_query_response(&query_response, &parsed_query)?;
                } else {
                    println!("响应格式错误: 期望CardQueryResponse");
                }
            }
        }

        Ok(())
    }

    async fn handle_get(&mut self, key: &str) -> Result<()> {
        let card_id: u64 = key.parse().map_err(|e| anyhow::anyhow!("Invalid card_id: {}", e))?;
        let query_request = CardQueryRequest {
            query_context: None,
            query_scope: Some(QueryScope {
                card_type_ids: vec![],
                card_ids: vec![card_id],
                container_ids: vec![],
                states: vec![],
            }),
            condition: None,
            r#yield: Some(Yield {
                yielded_field: Some({
                    YieldedField {
                        custom_fields: vec![],
                        contains_all_custom_field: true,
                        contains_desc: false,
                    }
                }),
                yielded_links: vec![],
            }),
            sort_and_page: None,
        };

        let request = pgraph::request::Request {
            request_id: Uuid::new_v4().to_string(),
            request_type: Some(pgraph::request::request::RequestType::CardQuery(
                query_request,
            )),
        };

        let mut buf = Vec::new();
        request.encode(&mut buf)?;

        let response_buf = self.send_message(&buf).await?;
        let response = pgraph::response::Response::decode(&response_buf[..])?;

        if response.code != 200 {
            return Err(anyhow!("Query failed: {}", response.message));
        }

        if let Some(pgraph::response::response::ResponseType::CardQueryResponse(query_response)) =
            response.response_type
        {
            if !query_response.cards.is_empty() {
                println!("Card information:");
                println!("{:#?}", query_response.cards[0]);
            } else {
                println!("Card with ID {} not found", key);
            }
        } else {
            return Err(anyhow!("Query response format error"));
        }

        Ok(())
    }

    async fn handle_mget(&mut self, keys: &[&str]) -> Result<()> {
        let card_ids: Vec<u64> = keys
            .iter()
            .map(|k| k.parse::<u64>())
            .collect::<std::result::Result<Vec<_>, _>>()
            .map_err(|e| anyhow::anyhow!("Invalid card_id: {}", e))?;
        let query_request = CardQueryRequest {
            query_context: None,
            query_scope: Some(QueryScope {
                card_type_ids: vec![],
                card_ids,
                container_ids: vec![],
                states: vec![],
            }),
            condition: None,
            r#yield: Some(Yield {
                yielded_field: Some({
                    YieldedField {
                        custom_fields: vec![],
                        contains_all_custom_field: true,
                        contains_desc: false,
                    }
                }),
                yielded_links: vec![],
            }),
            sort_and_page: None,
        };

        let request = pgraph::request::Request {
            request_id: Uuid::new_v4().to_string(),
            request_type: Some(pgraph::request::request::RequestType::CardQuery(
                query_request,
            )),
        };

        let mut buf = Vec::new();
        request.encode(&mut buf)?;

        let response_buf = self.send_message(&buf).await?;
        let response = pgraph::response::Response::decode(&response_buf[..])?;

        if response.code != 200 {
            return Err(anyhow!("Batch query failed: {}", response.message));
        }

        if let Some(pgraph::response::response::ResponseType::CardQueryResponse(query_response)) =
            response.response_type
        {
            if !query_response.cards.is_empty() {
                println!("Found {} cards:", query_response.cards.len());
                for (i, card) in query_response.cards.iter().enumerate() {
                    // 获取标题，处理可能是None的情况
                    let title_display = match &card.title {
                        Some(title) => match &title.title_type {
                            Some(pgraph::model::title::TitleType::Pure(pure)) => &pure.value,
                            Some(pgraph::model::title::TitleType::Joint(joint)) => &joint.name,
                            None => "[No Title]",
                        },
                        None => "[No Title]",
                    };

                    println!("{}. ID: {}, Title: {}", i + 1, card.id, title_display);
                    println!("   Details: {:#?}\n", card);
                }
            } else {
                println!("No cards found with the specified IDs");
            }
        } else {
            return Err(anyhow!("Query response format error"));
        }

        Ok(())
    }

    // 初始化集群
    async fn handle_init_cluster(&self) -> Result<()> {
        println!("Initializing Raft cluster...");
        match self.raft_client.init().await {
            Ok(_) => {
                println!("Cluster initialization successful");
                Ok(())
            }
            Err(e) => {
                println!("Cluster initialization failed!: {}", e);
                Err(anyhow!("Cluster initialization failed!: {}", e))
            }
        }
    }

    // 添加学习者节点
    async fn handle_add_learner(&self, node_id: u64, rpc_addr: &str) -> Result<()> {
        println!(
            "Adding learner node {} {}...",
            node_id, rpc_addr
        );

        match self
            .raft_client
            .add_learner((node_id, rpc_addr.to_string()))
            .await
        {
            Ok(_) => {
                println!("Successfully added learner node {}", node_id);
                Ok(())
            }
            Err(e) => {
                println!("Failed to add learner node: {}", e);
                Err(anyhow!("Failed to add learner node: {}", e))
            }
        }
    }

    // 变更集群成员
    async fn handle_change_membership(&self, node_ids: BTreeSet<u64>) -> Result<()> {
        println!("Changing cluster membership to: {:?}...", node_ids);
        // 获取当前集群状态以检查Leader
        match self.raft_client.metrics().await {
            Ok(metrics) => {
                if let Some(leader_id) = metrics.current_leader {
                    if !node_ids.contains(&leader_id) {
                        return Err(anyhow!("Warning: New member list does not contain current Leader node {}, operation rejected for cluster stability", leader_id));
                    }
                } else {
                    println!("Warning: Current cluster has no Leader, please proceed with caution for membership changes");
                }
            }
            Err(e) => {
                println!("Warning: Unable to get cluster status: {}, will proceed with membership change", e);
            }
        }

        match self.raft_client.change_membership(&node_ids).await {
            Ok(_) => {
                println!("Cluster membership change successful");
                Ok(())
            }
            Err(e) => {
                println!("Cluster membership change failed: {}", e);
                Err(anyhow!("Cluster membership change failed: {}", e))
            }
        }
    }

    // 获取集群状态
    async fn handle_cluster_status(&self) -> Result<()> {
        match self.raft_client.metrics().await {
            Ok(metrics) => {
                println!("Cluster status: {}", metrics);
                if let Some(leader_id) = metrics.current_leader {
                    println!("Current Leader: {:?}", leader_id);
                } else {
                    println!("Currently no Leader");
                }
                Ok(())
            }
            Err(e) => {
                println!("Failed to get cluster status: {}", e);
                Err(anyhow!("Failed to get cluster status: {}", e))
            }
        }
    }

    // 触发快照
    async fn handle_trigger_snapshot(&self) -> Result<()> {
        println!("Triggering snapshot creation...");
        match self.raft_client.trigger_snapshot().await {
            Ok(_) => {
                println!("Snapshot creation triggered successfully");
                Ok(())
            }
            Err(e) => {
                println!("Failed to trigger snapshot creation: {}", e);
                Err(anyhow!("Failed to trigger snapshot creation: {}", e))
            }
        }
    }

    // 添加更新日志级别的方法
    async fn handle_update_log_level(&mut self, level: &str) -> Result<()> {
        println!("Updating log level to: {}", level);

        let update_request = UpdateLogLevelRequest {
            log_level: level.to_string(),
        };

        let request = pgraph::request::Request {
            request_id: Uuid::new_v4().to_string(),
            request_type: Some(pgraph::request::request::RequestType::UpdateLogLevel(update_request)),
        };

        let mut buf = Vec::new();
        request.encode(&mut buf)?;

        let response_buf = self.send_message(&buf).await?;
        let response = pgraph::response::Response::decode(&response_buf[..])?;

        if response.code != 200 {
            return Err(anyhow!("Failed to update log level: {}", response.message));
        }

        if let Some(pgraph::response::response::ResponseType::UpdateLogLevelResponse(update_response)) =
            response.response_type
        {
            if update_response.success {
                println!("Log level update successful: {}", update_response.log_level);
                println!("Details: {}", update_response.message);
            } else {
                return Err(anyhow!("Failed to update log level: {}", update_response.message));
            }
        } else {
            return Err(anyhow!("Update log level response format error"));
        }

        Ok(())
    }

    // 添加获取数据库统计信息的方法
    async fn handle_database_stats(&mut self, include_details: bool) -> Result<()> {
        println!("Retrieving database statistics...");

        let start_time = std::time::Instant::now();

        let stats_request = DatabaseStatsRequest {
            include_cache_details: Some(include_details),
        };

        let request = pgraph::request::Request {
            request_id: Uuid::new_v4().to_string(),
            request_type: Some(pgraph::request::request::RequestType::DatabaseStats(stats_request)),
        };

        let mut buf = Vec::new();
        request.encode(&mut buf)?;

        let response_buf = self.send_message(&buf).await?;
        let response = pgraph::response::Response::decode(&response_buf[..])?;

        let elapsed = start_time.elapsed();

        if response.code != 200 {
            return Err(anyhow!("Failed to get database stats: {}", response.message));
        }

        if let Some(pgraph::response::response::ResponseType::DatabaseStatsResponse(stats_response)) =
            response.response_type
        {
            self.display_database_stats(&stats_response, include_details, elapsed);
        } else {
            return Err(anyhow!("Database stats response format error"));
        }

        Ok(())
    }

    // 显示数据库统计信息
    fn display_database_stats(&self, stats: &pgraph::admin::DatabaseStatsResponse, include_details: bool, elapsed: std::time::Duration) {
        println!("\n=== Database Statistics ===");
        
        // 基础统计信息
        println!("\n📊 Data Statistics:");
        println!("  Total Vertices: {}", stats.total_vertices);
        println!("  Total Edges: {}", stats.total_edges);
        println!("  Total Vertex Types: {}", stats.vertex_type_size);
        println!("  Total Edge Types: {}", stats.edge_type_size);

        // 缓存统计信息
        if let Some(cache_stats) = &stats.cache_stats {
            println!("\n🗄️  Cache Statistics:");
            println!("  Vertex LRU Cache Entries: {}", cache_stats.vertex_lru_cache_size);
            println!("  Description LRU Cache Entries: {}", cache_stats.desc_lru_cache_size);
        }



        // RocksDB详细统计信息（仅在请求详细信息时显示）
        if include_details {
            if let Some(rocksdb_stats) = &stats.rocksdb_stats {
                println!("\n🗃️  RocksDB Statistics:");
                println!("{}", rocksdb_stats);
            }
            // 显示查询耗时
            if elapsed.as_millis() > 0 {
                println!("\n⏱️ Execution Time: {:.2} ms", elapsed.as_secs_f64() * 1000.0);
            } else {
                println!("\n⏱️ Execution Time: {:.2} μs", elapsed.as_nanos() as f64 / 1000.0);
        }
        } else {
            // 显示查询耗时
            if elapsed.as_millis() > 0 {
                println!("\n⏱️ Execution Time: {:.2} ms", elapsed.as_secs_f64() * 1000.0);
            } else {
                println!("\n⏱️ Execution Time: {:.2} μs", elapsed.as_nanos() as f64 / 1000.0);
            }
            println!("\n💡 Tip: Use 'db-stats --details' to get detailed RocksDB statistics");
        }

        println!();
    }

    // 格式化字节数为可读格式
    fn format_bytes(bytes: u64) -> String {
        const UNITS: &[&str] = &["B", "KB", "MB", "GB", "TB"];
        let mut size = bytes as f64;
        let mut unit_index = 0;

        while size >= 1024.0 && unit_index < UNITS.len() - 1 {
            size /= 1024.0;
            unit_index += 1;
        }

        if unit_index == 0 {
            format!("{} {}", bytes, UNITS[unit_index])
        } else {
            format!("{:.2} {}", size, UNITS[unit_index])
        }
    }
}

// CLI入口函数
pub async fn start_cli() -> Result<()> {
    let args = Args::parse();

    println!("Welcome to pgraph CLI!");
    println!("--------------------");

    // 获取服务器地址
    let server_addr = match args.server_addr {
        Some(h) => h,
        None => Input::new()
            .with_prompt("Please enter server address")
            .default("127.0.0.1:7009".to_string())
            .interact_text()
            .context("Failed to read server address")?,
    };

    // 获取RPC地址（用于Raft集群管理）
    let rpc_addr = match args.rpc_addr {
        Some(a) => a,
        None => Input::new()
            .with_prompt("Please enter RPC address for cluster node internal communication")
            .default("127.0.0.1:13897".to_string())
            .interact_text()
            .context("Please enter RPC address for cluster node internal communication")?,
    };

    // 获取用户名
    let username = match args.username {
        Some(u) => Some(u),
        None => {
            let input: String = Input::new()
                .with_prompt("Please enter username (optional, press Enter to skip)")
                .default("pgraph".to_string())
                .allow_empty(true)
                .interact_text()
                .context("Failed to read username")?;
            if input.is_empty() {
                None
            } else {
                Some(input)
            }
        }
    };

    // 如果提供了用户名，则获取密码
    let password = match (args.password, username.as_ref()) {
        (Some(p), _) => Some(p),
        (None, Some(_)) => {
            let input = Password::new()
                .with_prompt("Please enter password")
                .interact()
                .context("Failed to read password")?;
            Some(input)
        }
        (None, None) => None,
    };

    // 构建服务器地址
    let parts: Vec<&str> = server_addr.split(':').collect();

    let addr = std::net::SocketAddr::from_str(&format!("{}:{}", parts[0], parts[1]))
        .context("Server address format error")?;

    println!("\nConnecting to {}...", addr);

    // 创建并连接客户端
    let mut client = pgraphClient::connect(addr, rpc_addr)
        .await
        .context("Failed to connect to server")?;

    // 认证
    if let Err(e) = client.authenticate(username, password).await {
        eprintln!("Authentication failed: {}", e);
        println!("Bye!");
        return Ok(());
    }

    println!("Connection successful!\n");

    // 运行CLI
    if let Err(e) = client.run_cli().await {
        eprintln!("CLI error: {}", e);
    }

    Ok(())
}
