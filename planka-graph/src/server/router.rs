use log::trace;
use prost::Message;
use std::sync::Arc;
use std::time::Instant;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::{TcpStream};
use tokio::sync::{broadcast, RwLock};
use tracing::{debug, error, info, span, warn, Instrument, Level};

use crate::config::ServerConfig;
use crate::database::database::Database;
use crate::database::rdb::rdb::RocksDatabase;
use crate::logger;
use crate::proto::{
    request, response,
    pgraph::{
        admin::{UpdateLogLevelRequest, DatabaseStatsRequest},
        auth::AuthResponse,
        linkquery::{LinkFetchRequest, LinkQueryRequest},
        query::{CardCountByGroupRequest, CardCountRequest, CardIdQueryRequest, CardQueryRequest, QueryCardTitlesRequest},
        write::{
            BatchCreateCardRequest, BatchCreateLinkRequest, BatchDeleteLinkRequest,
            BatchUpdateCardFieldRequest, BatchUpdateCardRequest, BatchUpdateCardTitleRequest,
            BatchUpdateLinkRequest,
        },
    },
    Request, Response,
};

use super::{
    batch_create_cards, batch_create_links, batch_delete_links,
    batch_update_card_titles, batch_update_cards, batch_update_links, fetch_links, query_card_titles, query_cards,
    query_links,
};

/// 向客户端发送响应
async fn send_response(stream: &mut TcpStream, response: Response) -> Result<(), std::io::Error> {
    let mut encoded_response = Vec::new();
    response.encode(&mut encoded_response).unwrap();

    let response_size = encoded_response.len();
    debug!("Sending response, size: {} bytes", response_size);

    // 发送4字节的消息长度
    let size_bytes = (response_size as u32).to_be_bytes();
    stream.write_all(&size_bytes).await?;

    // 大型响应可能需要分块发送
    const CHUNK_SIZE: usize = 8192;

    if response_size > CHUNK_SIZE * 4 {
        // 对于大型响应，分块发送以避免缓冲区溢出
        trace!(
            "Large response ({} bytes), sending in chunks",
            response_size
        );

        for chunk_start in (0..response_size).step_by(CHUNK_SIZE) {
            let chunk_end = std::cmp::min(chunk_start + CHUNK_SIZE, response_size);
            let chunk = &encoded_response[chunk_start..chunk_end];
            stream.write_all(chunk).await?;
            stream.flush().await?;
            //每4个块或者最后一个块flush一次
            if (chunk_start + CHUNK_SIZE) % (CHUNK_SIZE * 4) == 0 || chunk_start + CHUNK_SIZE == response_size {
                stream.flush().await?;
            }
        }    

        trace!("Large response sending complete");
        Ok(())
    } else {
        // 小型响应直接发送
        stream.write_all(&encoded_response).await?;
        stream.flush().await
    }
}

/// 向客户端发送错误响应
async fn send_error_response(stream: &mut TcpStream, code: i32, request_id: String, message: &str) {
    let response = Response {
        code,
        request_id,
        message: message.to_string(),
        response_type: None,
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send error response: {}", e);
    }
}

/// 处理客户端认证
async fn handle_client_authentication(
    stream: &mut TcpStream,
    config: &ServerConfig,
) -> Result<bool, ()> {
    if let Some(auth) = &config.authentication {
        if auth.enabled {
            // 读取4字节的消息长度
            let mut len_buf = [0u8; 4];
            match stream.read_exact(&mut len_buf).await {
                Ok(_) => {
                    let msg_len = u32::from_be_bytes(len_buf) as usize;
                    debug!("Authentication request length: {} bytes", msg_len);

                    // 检查请求大小是否合理
                    const MAX_AUTH_SIZE: usize = 1024 * 1024; // 最大认证请求大小限制为1MB
                    if msg_len > MAX_AUTH_SIZE {
                        warn!(
                            "Authentication request exceeds maximum size limit {} bytes",
                            MAX_AUTH_SIZE
                        );
                        // 发送错误响应
                        let response = Response {
                            code: 413,
                            request_id: "unknown".to_string(),
                            message: format!(
                                "Authentication request too large: exceeded {} bytes limit",
                                MAX_AUTH_SIZE
                            ),
                            response_type: None,
                        };
                        let _ = send_response(stream, response).await;
                        return Err(());
                    }

                    // 读取消息体
                    let mut buffer = vec![0u8; msg_len];
                    match stream.read_exact(&mut buffer).await {
                        Ok(_) => {
                            debug!("Successfully read auth request of {} bytes", msg_len);

                            // 尝试解析认证请求
                            let request_result = Request::decode(&buffer[..]);
                            if let Ok(request) = request_result {
                                let request_id = request.request_id.clone();

                                // 检查是否是认证请求
                                if let Some(request::RequestType::Auth(auth_req)) =
                                    request.request_type
                                {
                                    let username = &auth_req.username;
                                    let password = &auth_req.password;

                                    debug!(
                                        "Received authentication request: username={}",
                                        username
                                    );

                                    // 验证用户名和密码
                                    if !config.authenticate_user(username, password) {
                                        warn!("Authentication failed for user {}", username);
                                        // 发送认证失败响应
                                        let auth_response = AuthResponse {
                            success: false,
                            message: "Authentication failed: Invalid username or password"
                                .to_string(),
                        };
                                        let response = Response {
                                            code: 401,
                                            request_id,
                                            message: "Authentication failed".to_string(),
                                            response_type: Some(
                                                response::ResponseType::AuthResponse(auth_response),
                                            ),
                                        };
                                        let _ = send_response(stream, response).await;
                                        return Err(());
                                    }

                                    // 认证成功
                                    info!("User {} authenticated successfully", username);

                                    // 发送认证成功响应
                                    let auth_response = AuthResponse {
                                        success: true,
                                        message: "Authentication successful".to_string(),
                                    };
                                    let response = Response {
                                        code: 200,
                                        request_id,
                                        message: "Authentication successful".to_string(),
                                        response_type: Some(response::ResponseType::AuthResponse(
                                            auth_response,
                                        )),
                                    };
                                    let _ = send_response(stream, response).await;
                                    return Ok(true);
                                } else {
                                    // 发送未认证错误
                                    warn!("First request is not an authentication request");
                                    let response = Response {
                                        code: 401,
                                        request_id,
                                        message: "Authentication required before sending requests"
                                            .to_string(),
                                        response_type: None,
                                    };
                                    let _ = send_response(stream, response).await;
                                    return Err(());
                                }
                            } else {
                                warn!("Failed to parse authentication request");
                                // 发送解析错误
                                let response = Response {
                                    code: 400,
                                    request_id: "unknown".to_string(),
                                    message: "Failed to parse authentication request".to_string(),
                                    response_type: None,
                                };
                                let _ = send_response(stream, response).await;
                                return Err(());
                            }
                        }
                        Err(e) => {
                            // 区分正常关闭和其他错误
                            if e.kind() == std::io::ErrorKind::UnexpectedEof {
                                info!("Client closed connection during authentication body read");
                            } else {
                                error!("Failed to read authentication body: {}", e);
                            }
                            return Err(());
                        }
                    }
                }
                Err(e) => {
                    // 区分正常关闭和其他错误
                    if e.kind() == std::io::ErrorKind::UnexpectedEof {
                        info!("Client closed connection during authentication length read");
                    } else {
                        error!("Failed to read authentication length: {}", e);
                    }
                    return Err(());
                }
            }
        }
    }

    // 如果不需要认证，发送一个认证成功响应
    let auth_response = AuthResponse {
        success: true,
        message: "Authentication successful".to_string(),
    };
    let response = Response {
        code: 200,
        request_id: "unknown".to_string(),
        message: "Authentication successful".to_string(),
        response_type: Some(response::ResponseType::AuthResponse(auth_response)),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send authentication response: {}", e);
        return Err(());
    }
    // 如果不需要认证，直接返回认证成功
    Ok(true)
}

/// 处理客户端连接，支持graceful shutdown
pub async fn handle_client_with_shutdown(
    mut stream: TcpStream,
    db: Arc<RwLock<Arc<RocksDatabase>>>,
    config: Arc<ServerConfig>,
    raft_client: Option<Arc<crate::raft::client::RaftClient>>,
    mut shutdown_rx: broadcast::Receiver<()>,
) {
    if let Some(ref client_addr) = stream.peer_addr().ok() {
        info!("Connection established from {}", client_addr);
    } else {
        warn!("Connection established from unknown client");
    }

    // 处理认证请求（如果需要）
    if let Some(ref auth) = config.authentication {
        if auth.enabled {
            if let Err(_) = handle_client_authentication(&mut stream, &config).await {
                return;
            }
        }
    }

    // 处理客户端请求
    loop {
        tokio::select! {
            // 监听shutdown信号
            _ = shutdown_rx.recv() => {
                info!("Received shutdown signal, closing client connection");
                break;
            }
            
            // 处理客户端请求
            result = read_and_process_request(&mut stream, &db, &raft_client) => {
                match result {
                    Ok(true) => continue, // 继续处理下一个请求
                    Ok(false) => break,   // 客户端正常关闭连接
                    Err(_) => break,      // 发生错误，关闭连接
                }
            }
        }
    }
}

/// 读取并处理单个请求
async fn read_and_process_request(
    stream: &mut TcpStream,
    db: &Arc<RwLock<Arc<RocksDatabase>>>,
    raft_client: &Option<Arc<crate::raft::client::RaftClient>>,
) -> Result<bool, std::io::Error> {
    // 读取4字节的消息长度
    let mut len_buf = [0u8; 4];
    match stream.read_exact(&mut len_buf).await {
        Ok(_) => {
            // 成功读取长度前缀
            let msg_len = u32::from_be_bytes(len_buf) as usize;
            debug!("Request length: {} bytes", msg_len);

            // 检查请求大小是否合理
            const MAX_REQUEST_SIZE: usize = 1024 * 1024 * 64; // 最大请求大小限制为64MB
            if msg_len > MAX_REQUEST_SIZE {
                error!(
                    "Request exceeds maximum size limit {} bytes",
                    MAX_REQUEST_SIZE
                );
                send_error_response(
                    stream,
                    413,
                    "unknown".to_string(),
                    &format!(
                        "Request too large: exceeded {} bytes limit",
                        MAX_REQUEST_SIZE
                    ),
                )
                .await;
                return Ok(true);
            }

            // 读取消息体
            let mut buffer = vec![0u8; msg_len];
            match stream.read_exact(&mut buffer).await {
                Ok(_) => {
                    debug!("Successfully read request of {} bytes", msg_len);

                    // 解析请求
                    match Request::decode(&buffer[..]) {
                        Ok(request) => {
                            // 处理请求
                            if let Some(request_type) = request.request_type {
                                let request_id = request.request_id.clone();
                                process_request(request_type, request_id, stream, db, raft_client.as_ref().map(|r| r.as_ref()))
                                    .instrument(span!(Level::INFO, "request", request_id = display(&request.request_id)))
                                    .await;
                            }
                            Ok(true)
                        }
                        Err(e) => {
                            error!(
                                "Failed to decode request: {}, buffer size:{}",
                                e,
                                buffer.len()
                            );
                            send_error_response(
                                stream,
                                400,
                                "unknown".to_string(),
                                &format!(
                                    "Failed to decode request: {}, buffer size:{}",
                                    e,
                                    buffer.len()
                                ),
                            )
                            .await;
                            Ok(true)
                        }
                    }
                }
                Err(e) => {
                    // 读取请求体出错，可能是连接关闭
                    if e.kind() == std::io::ErrorKind::UnexpectedEof {
                        info!("Client closed connection while reading request body");
                    } else {
                        error!("Error reading request body: {}", e);
                    }
                    Ok(false)
                }
            }
        }
        Err(e) => {
            // 读取长度前缀出错
            if e.kind() == std::io::ErrorKind::UnexpectedEof {
                // 这是正常的连接关闭，客户端主动断开
                info!("Client closed connection");
            } else {
                // 其他IO错误
                error!("Error reading request length: {}", e);
            }
            Ok(false)
        }
    }
}

/// 处理单个请求
async fn process_request(
    request_type: request::RequestType,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RwLock<Arc<RocksDatabase>>>,
    raft_client: Option<&crate::raft::client::RaftClient>,
) {
    // 获取当前数据库实例
    let db_instance = {
        let db_read = db.read().await;
        Arc::clone(&*db_read)
    };
    
    // 根据请求类型分发
    match request_type {
        request::RequestType::CardQuery(req) => {
            handle_card_query_request(
                req,
                request_id,
                stream,
                &db_instance,
            )
            .await;
        }
        request::RequestType::CardCount(req) => {
            handle_card_count_request(
                req,
                request_id,
                stream,
                &db_instance,
            )
            .await;
        }
        request::RequestType::CardIdQuery(req) => {
            handle_card_id_query_request(
                req,
                request_id,
                stream,
                &db_instance,
            )
            .await;
        }
        request::RequestType::QueryCardTitles(req) => {
            handle_query_card_titles_request(
                req,
                request_id,
                stream,
                &db_instance,
            )
            .await;
        }
        request::RequestType::BatchCreateCard(req) => {
            handle_batch_create_card_request(
                req,
                request_id,
                stream,
                &db_instance,
                raft_client,
            )
            .await;
        }
        request::RequestType::BatchUpdateCard(req) => {
            handle_batch_update_card_request(
                req,
                request_id,
                stream,
                &db_instance,
                raft_client,
            )
            .await;
        }
        request::RequestType::BatchCreateLink(req) => {
            handle_batch_create_link_request(
                req,
                request_id,
                stream,
                &db_instance,
                raft_client,
            )
            .await;
        }
        request::RequestType::BatchUpdateLink(req) => {
            handle_batch_update_link_request(
                req,
                request_id,
                stream,
                &db_instance,
                raft_client,
            )
            .await;
        }
        request::RequestType::BatchDeleteLink(req) => {
            handle_batch_delete_link_request(
                req,
                request_id,
                stream,
                &db_instance,
                raft_client,
            )
            .await;
        }
        request::RequestType::BatchUpdateCardTitle(req) => {
            handle_batch_update_card_title_request(
                req,
                request_id,
                stream,
                &db_instance,
                raft_client,
            )
            .await;
        }
        request::RequestType::BatchUpdateCardFields(req) => {
            handle_batch_update_card_field_request(
                req,
                request_id,
                stream,
                &db_instance,
                raft_client,
            )
            .await;
        }
        request::RequestType::LinkQuery(req) => {
            handle_link_query_request(
                req,
                request_id,
                stream,
                &db_instance,
            )
            .await;
        }
        request::RequestType::LinkFetch(req) => {
            handle_link_fetch_request(
                req,
                request_id,
                stream,
                &db_instance,
            )
            .await;
        }
        request::RequestType::UpdateLogLevel(req) => {
            handle_update_log_level_request(
                req,
                request_id,
                stream,
            )
            .await;
        }
        request::RequestType::CardCountByGroup(req) => {
            handle_card_count_by_group_request(
                req,
                request_id,
                stream,
                &db_instance,
            )
            .await;
        }
        request::RequestType::DatabaseStats(req) => {
            handle_database_stats_request(
                req,
                request_id,
                stream,
                &db_instance,
            )
            .await;
        }
        _ => {
            debug!("Unsupported request type");
            send_error_response(
                stream,
                400,
                request_id,
                "Unsupported request type",
            )
            .await;
        }
    }
}

// 处理CardQueryRequest
async fn handle_card_query_request(
    request: CardQueryRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
) {
    debug!("Received CardQueryRequest: request={:?}", request);
    let start_time = Instant::now();

    // 使用 query_cards 函数执行查询
    let query_response = match query_cards(request, &**db) {
        Ok(response) => response,
        Err(e) => {
            error!("Failed to execute card query: {:?}", e);
            send_error_response(stream, 400, request_id, &format!("Query failed: {}", e)).await;
            return;
        }
    };

    let return_count = query_response.cards.len();

    let elapsed = start_time.elapsed();
    debug!(
            "Query returned {} card records before send_response, cost: {:?}",
            return_count, elapsed
        );

    // 构建统一的响应对象
    let response = Response {
        code: 200,
        request_id,
        message: "Query successful".to_string(),
        response_type: Some(response::ResponseType::CardQueryResponse(query_response)),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send card query response: {}", e);
    } else {
        let elapsed = start_time.elapsed();
        debug!(
            "Query returned {} card records after send_response, cost: {:?}",
            return_count, elapsed
        );
    }
}

// 处理CardCountRequest
async fn handle_card_count_request(
    request: CardCountRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
) {
    debug!("Received CardCountRequest: request={:?}", request);
    let start_time = Instant::now();

    // 执行卡片计数查询
    let count_response = match crate::server::count_cards(request, &**db) {
        Ok(response) => response,
        Err(e) => {
            error!("Failed to execute card count query: {:?}", e);
            send_error_response(
                stream,
                400,
                request_id,
                &format!("Count query failed: {}", e),
            )
            .await;
            return;
        }
    };

    let count = count_response.count;
    // 构建统一的响应对象
    let response = Response {
        code: 200,
        request_id,
        message: "Count query successful".to_string(),
        response_type: Some(response::ResponseType::CardCountResponse(count_response)),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send count response: {}", e);
    } else {
        let elapsed = start_time.elapsed();
        debug!(
            "Count query returned result: {}, cost: {:?}",
            count, elapsed
        );
    }
}

// 处理QueryCardTitlesRequest
async fn handle_query_card_titles_request(
    request: QueryCardTitlesRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
) {
    debug!("Received QueryCardTitlesRequest: card_ids count={}", request.card_ids.len());
    let start_time = Instant::now();

    // 执行查询卡片标题
    let query_response = match query_card_titles(request, &**db) {
        Ok(response) => response,
        Err(e) => {
            error!("Query card titles failed: {:?}", e);
            send_error_response(
                stream,
                500,
                request_id,
                &format!("Query card titles failed: {:?}", e),
            )
            .await;
            return;
        }
    };

    let titles_count = query_response.titles.len();

    // 构建响应
    let response = Response {
        code: 200,
        request_id,
        message: "Query card titles completed".to_string(),
        response_type: Some(response::ResponseType::QueryCardTitlesResponse(query_response)),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send query card titles response: {}", e);
    } else {
        let elapsed = start_time.elapsed();
        debug!(
            "Query card titles returned {} titles, cost: {:?}",
            titles_count, elapsed
        );
    }
}

/// 处理卡片批量创建请求
async fn handle_batch_create_card_request(
    request: BatchCreateCardRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
    raft_client: Option<&crate::raft::client::RaftClient>,
) {
    debug!("Received BatchCreateCardRequest: request={:?}", request);
    let start_time = Instant::now();

    // 执行批量创建卡片
    let response = batch_create_cards(request, &**db, raft_client).await;

    let elapsed = start_time.elapsed();

    debug!(
        "Batch create card request completed in {}ms, success: {}, failure: {}",
        elapsed.as_millis(),
        response.success,
        response.failed_ids.len()
    );

    let proto_response = Response {
        code: 200,
        request_id,
        message: "Batch create card operation completed".to_string(),
        response_type: Some(response::ResponseType::BatchCardCommonResponse(response)),
    };

    if let Err(e) = send_response(stream, proto_response).await {
        error!("Failed to send response: {}", e);
    }
}

/// 处理卡片批量更新请求
async fn handle_batch_update_card_request(
    request: BatchUpdateCardRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
    raft_client: Option<&crate::raft::client::RaftClient>,
) {
    let start = Instant::now();
    debug!("Processing batch update card request");

    let response = batch_update_cards(request, db.as_ref(), raft_client).await;
    let elapsed = start.elapsed();

    debug!(
        "Batch update card request completed in {}ms, success: {}, failure: {}",
        elapsed.as_millis(),
        response.success,
        response.failed_ids.len()
    );

    let proto_response = Response {
        code: 200,
        request_id,
        message: "Batch update card operation completed".to_string(),
        response_type: Some(response::ResponseType::BatchCardCommonResponse(response)),
    };

    if let Err(e) = send_response(stream, proto_response).await {
        error!("Failed to send response: {}", e);
    }
}

// 处理BatchCreateLinkRequest
async fn handle_batch_create_link_request(
    request: BatchCreateLinkRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
    raft_client: Option<&crate::raft::client::RaftClient>,
) {
    debug!(
        "Received BatchCreateLinkRequest: links quantity={}",
        request.links.len()
    );
    let start_time = Instant::now();

    // 使用 batch_create_links 函数执行批量创建关联关系
    let batch_response = batch_create_links(request, &**db, raft_client).await;

    let success = batch_response.success;

    // 构建统一的响应对象
    let response = Response {
        code: 200,
        request_id,
        message: "Batch link creation successful".to_string(),
        response_type: Some(response::ResponseType::BatchLinkCommonResponse(
            batch_response,
        )),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send batch link creation response: {}", e);
    } else {
        let elapsed = start_time.elapsed();
        debug!(
            "BatchCreateLink processed {} links successfully, cost: {:?}",
            success, elapsed
        );
    }
}

// 处理BatchUpdateLinkRequest
async fn handle_batch_update_link_request(
    request: BatchUpdateLinkRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
    raft_client: Option<&crate::raft::client::RaftClient>,
) {
    debug!(
        "Received BatchUpdateLinkRequest: links quantity={}",
        request.links.len()
    );
    let start_time = Instant::now();

    // 使用 batch_update_links 函数执行批量更新关联关系
    let batch_response = batch_update_links(request, &**db, raft_client).await;

    let success = batch_response.success;

    // 构建统一的响应对象
    let response = Response {
        code: 200,
        request_id,
        message: "Batch link update successful".to_string(),
        response_type: Some(response::ResponseType::BatchLinkCommonResponse(
            batch_response,
        )),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send batch link update response: {}", e);
    } else {
        let elapsed = start_time.elapsed();
        debug!(
            "BatchUpdateLink processed {} links successfully, cost: {:?}",
            success, elapsed
        );
    }
}

// 处理BatchDeleteLinkRequest
async fn handle_batch_delete_link_request(
    request: BatchDeleteLinkRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
    raft_client: Option<&crate::raft::client::RaftClient>,
) {
    debug!(
        "Received BatchDeleteLinkRequest: links quantity={}",
        request.links.len()
    );
    let start_time = Instant::now();

    // 使用 batch_delete_links 函数执行批量删除关联关系
    let batch_response = batch_delete_links(request, &**db, raft_client).await;

    let success = batch_response.success;

    // 构建统一的响应对象
    let response = Response {
        code: 200,
        request_id,
        message: "Batch link deletion successful".to_string(),
        response_type: Some(response::ResponseType::BatchLinkCommonResponse(
            batch_response,
        )),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send batch link deletion response: {}", e);
    } else {
        let elapsed = start_time.elapsed();
        debug!(
            "BatchDeleteLink processed {} links successfully, cost: {:?}",
            success, elapsed
        );
    }
}

// 处理LinkQueryRequest
async fn handle_link_query_request(
    request: LinkQueryRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
) {
    debug!("Received LinkQueryRequest: request={:?}", request);
    let start_time = Instant::now();

    // 创建数据库事务
    let txn = db.transaction();

    // 使用 query_links 函数执行查询
    let query_response = query_links(request, &txn);

    let links_count = query_response.links.len();
    debug!("Query returned {} link records", links_count);

    // 构建统一的响应对象
    let response = Response {
        code: 200,
        request_id,
        message: "Link query successful".to_string(),
        response_type: Some(response::ResponseType::LinkQueryResponse(query_response)),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send link query response: {}", e);
    } else {
        let elapsed = start_time.elapsed();
        debug!(
            "LinkQuery returned {} links, cost: {:?}",
            links_count, elapsed
        );
    }
}

// 处理LinkFetchRequest
async fn handle_link_fetch_request(
    request: LinkFetchRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
) {
    debug!("Received LinkFetchRequest: request={:?}", request);
    let start_time = Instant::now();

    // 创建数据库事务
    let txn = db.transaction();

    // 使用 fetch_links 函数执行查询
    let query_response = fetch_links(request, &txn);

    let links_count = query_response.links.len();
    debug!("Query returned {} link records", links_count);

    // 构建统一的响应对象
    let response = Response {
        code: 200,
        request_id,
        message: "Link fetch successful".to_string(),
        response_type: Some(response::ResponseType::LinkQueryResponse(query_response)),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send link fetch response: {}", e);
    } else {
        let elapsed = start_time.elapsed();
        debug!(
            "LinkFetch returned {} links, cost: {:?}",
            links_count, elapsed
        );
    }
}

// 处理CardIdQueryRequest
async fn handle_card_id_query_request(
    request: CardIdQueryRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
) {
    debug!("Received CardIdQueryRequest: request={:?}", request);
    let start_time = Instant::now();

    // 执行卡片ID查询
    let query_response = match crate::server::query_card_ids(request, &**db) {
        Ok(response) => response,
        Err(e) => {
            error!("Failed to execute card ID query: {:?}", e);
            send_error_response(stream, 400, request_id, &format!("ID query failed: {}", e)).await;
            return;
        }
    };

    let ids_count = query_response.ids.len();
    debug!("ID query returned {} card IDs", ids_count);

    // 构建统一的响应对象
    let response = Response {
        code: 200,
        request_id,
        message: "ID query successful".to_string(),
        response_type: Some(response::ResponseType::QueryIdsResponse(query_response)),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send ID query response: {}", e);
    } else {
        let elapsed = start_time.elapsed();
        debug!(
            "CardIdQuery returned {} card IDs, cost: {:?}",
            ids_count, elapsed
        );
    }
}

/// 处理批量更新卡片标题请求
async fn handle_batch_update_card_title_request(
    request: BatchUpdateCardTitleRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
    raft_client: Option<&crate::raft::client::RaftClient>,
) {
    let start = Instant::now();
    debug!(
        "Processing batch update card title request with {} items",
        request.requests.len()
    );

    let response = batch_update_card_titles(request, db.as_ref(), raft_client).await;
    let elapsed = start.elapsed();

    debug!(
        "Batch update card title request completed in {}ms, success: {}, failure: {}",
        elapsed.as_millis(),
        response.success,
        response.failed_ids.len()
    );

    let proto_response = Response {
        code: 200,
        request_id,
        message: "Batch update card title operation completed".to_string(),
        response_type: Some(response::ResponseType::BatchCardCommonResponse(response)),
    };

    if let Err(e) = send_response(stream, proto_response).await {
        error!("Failed to send batch update card title response: {}", e);
    }
}

/// 处理批量更新卡片属性请求
async fn handle_batch_update_card_field_request(
    request: BatchUpdateCardFieldRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
    raft_client: Option<&crate::raft::client::RaftClient>,
) {
    let start = Instant::now();
    debug!(
        "Processing batch update card Field request with {} items",
        request.requests.len()
    );

    // 使用 batch_update_card_field 函数执行批量部分更新卡片属性
    let response =
        crate::server::writes::batch_update_card_field(request, db.as_ref(), raft_client).await;
    let elapsed = start.elapsed();

    debug!(
        "Batch update card Field request completed in {}ms, success: {}, failure: {}",
        elapsed.as_millis(),
        response.success,
        response.failed_ids.len()
    );

    let proto_response = Response {
        code: 200,
        request_id,
        message: "Batch update card Field operation completed".to_string(),
        response_type: Some(response::ResponseType::BatchCardCommonResponse(response)),
    };

    if let Err(e) = send_response(stream, proto_response).await {
        error!("Failed to send batch update card Field response: {}", e);
    }
}

/// 处理更新日志级别请求
async fn handle_update_log_level_request(
    request: UpdateLogLevelRequest,
    request_id: String,
    stream: &mut TcpStream,
) {
    let start = Instant::now();
    info!("Processing update log level request: {}", request.log_level);

    // 尝试更新日志级别
    let result = logger::update_log_level(&request.log_level);

    // 构建响应
    let (code, message, success) = match result {
        Ok(msg) => (200, msg, true),
        Err(err) => (400, err, false),
    };

    // 创建日志级别更新响应
    let update_log_level_response = crate::proto::pgraph::admin::UpdateLogLevelResponse {
        success,
        log_level: request.log_level.clone(),
        message,
    };

    // 发送响应
    let response = Response {
        code,
        request_id,
        message: if success { 
            "Log level updated successfully".into() 
        } else { 
            "Failed to update log level".into() 
        },
        response_type: Some(response::ResponseType::UpdateLogLevelResponse(
            update_log_level_response,
        )),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send log level update response: {}", e);
    }

    let elapsed = start.elapsed();
    info!("Update log level request processing completed, duration: {:?}", elapsed);
}

// 处理CardCountByGroupRequest
async fn handle_card_count_by_group_request(
    request: CardCountByGroupRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
) {
    debug!("Received CardCountByGroupRequest: request={:?}", request);
    let start_time = Instant::now();

    // 执行按分组计数查询
    let group_count_response = match crate::server::count_cards_by_group(request, &**db) {
        Ok(response) => response,
        Err(e) => {
            error!("Failed to execute card count by group query: {:?}", e);
            send_error_response(
                stream,
                400,
                request_id,
                &format!("Count by group query failed: {}", e),
            )
            .await;
            return;
        }
    };

    // 构建统一的响应对象
    let response = Response {
        code: 200,
        request_id,
        message: "Count by group query successful".to_string(),
        response_type: Some(response::ResponseType::CardCountByGroupResponse(group_count_response)),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send count by group response: {}", e);
    } else {
        let elapsed = start_time.elapsed();
        debug!(
            "Count by group query returned result with groups, cost: {:?}",
            elapsed
        );
    }
}

// 处理数据库统计请求
async fn handle_database_stats_request(
    request: DatabaseStatsRequest,
    request_id: String,
    stream: &mut TcpStream,
    db: &Arc<RocksDatabase>,
) {
    debug!("Received DatabaseStatsRequest: request={:?}", request);
    let start_time = Instant::now();

    // 获取数据库统计信息
    let include_cache_details = request.include_cache_details.unwrap_or(false);
    let db_stats = db.get_database_stats(include_cache_details);

    // 构建protobuf响应
    let stats_response = crate::proto::pgraph::admin::DatabaseStatsResponse {
        total_vertices: db_stats.total_vertices,
        total_edges: db_stats.total_edges,
        vertex_type_size: db_stats.vertex_types_count,
        edge_type_size: db_stats.edge_types_count,
        cache_stats: Some(crate::proto::pgraph::admin::CacheStats {
            vertex_lru_cache_size: db_stats.cache_stats.vertex_lru_cache_size as u64,
            desc_lru_cache_size: db_stats.cache_stats.desc_lru_cache_size as u64,
        }),

        rocksdb_stats: db_stats.rocksdb_stats,
    };

    // 构建统一的响应对象
    let response = Response {
        code: 200,
        request_id,
        message: "Database stats retrieved successfully".to_string(),
        response_type: Some(response::ResponseType::DatabaseStatsResponse(stats_response)),
    };

    if let Err(e) = send_response(stream, response).await {
        error!("Failed to send database stats response: {}", e);
    } else {
        let elapsed = start_time.elapsed();
        debug!(
            "Database stats query successful, total_vertices: {}, total_edges: {}, cost: {:?}",
            db_stats.total_vertices,
            db_stats.total_edges,
            elapsed
        );
    }
}
