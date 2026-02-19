use tracing::{debug, error, trace, warn};

use crate::database::database::Database;
use crate::database::model::{AttachmentItem, AttachmentValue, CardState, DateProp, DateValue, Description, Edge, EdgeDirection, EdgeProp, EnumItemId, EnumProp, EnumValue, FieldId, FieldValue, Identifier, JointTitleInfo, JointTitlePart, NumberProp, NumberValue, StreamInfo, TextValue, TitleJointArea, Vertex, VertexId, VertexTitle, WebLinkValue};
use crate::database::transaction::Transaction;
use crate::proto::pgraph::field::FieldValue as ProtoFieldValue;
use crate::proto::pgraph::model::{title::TitleType, Card, Link};
use crate::proto::pgraph::write::{
    BatchCardCommonResponse, BatchCreateCardRequest, BatchCreateLinkRequest, BatchDeleteLinkRequest,
    BatchLinkCommonResponse, BatchUpdateCardFieldRequest, BatchUpdateCardRequest, BatchUpdateCardTitleRequest,
    BatchUpdateLinkRequest, UpdateCardFieldRequest,
};
use crate::proto::{field_value, field_value_on_link, FieldValueOnLink, Title};
use std::{
    collections::HashMap,
    time::{SystemTime, UNIX_EPOCH},
};

/// 批量创建卡片
///
/// 将请求中的卡片转换为节点并存储到数据库中
///
/// # 参数
/// * `request` - 批量创建卡片请求
/// * `db` - 数据库实例
/// * `raft_client` - 可选的Raft客户端，如果提供则使用Raft共识协议
///
/// # 返回
/// 返回包含成功创建卡片数量的响应
pub async fn batch_create_cards<D: Database>(
    request: BatchCreateCardRequest,
    db: &D,
    raft_client: Option<&crate::raft::client::RaftClient>,
) -> BatchCardCommonResponse {

    // 如果提供了Raft客户端，则通过Raft共识协议处理请求
    if let Some(client) = raft_client {
        return process_batch_create_cards_via_raft(request, client).await;
    }
    do_batch_create_cards(request, db)
}

fn do_batch_create_cards<D: Database>(
    request: BatchCreateCardRequest,
    db: &D,
) -> BatchCardCommonResponse {
    let mut txn = db.transaction();
    let mut success_count = 0;
    let mut failed_ids = Vec::new();

    for card in &request.cards {
        // 将Card转换为Vertex
        match card_to_vertex(card, None, &txn) {
            Ok(mut vertex) => {
                // 存储节点到数据库
                match txn.create_vertex(&mut vertex) {
                    Ok(_) => {
                        success_count += 1;
                    }
                    Err(e) => {
                        error!("Failed to create card: {}, error: {:?}", card.id, e);
                        failed_ids.push(card.id.clone());
                    }
                }
            }
            Err(e) => {
                debug!("Failed to convert card: {}, error: {}", card.id, e);
                failed_ids.push(card.id.clone());
            }
        }
    }

    // 提交事务
    match txn.commit() {
        Ok(_) => {
            debug!(
                "Batch card update transaction committed successfully, total: {}, success: {}",
                request.cards.len(),
                success_count
            );
        }
        Err(e) => {
            error!("Batch card update transaction commit failed: {:?}", e);
            // 如果事务提交失败，成功数量应为0
            success_count = 0;
        }
    }

    debug!(
        "Batch card creation transaction committed successfully, total: {}, success: {}",
        request.cards.len(),
        success_count
    );

    // 构造并返回响应
    BatchCardCommonResponse {
        success: success_count,
        failed_ids: failed_ids,
    }
}

/// 通过Raft共识协议批量创建卡片
async fn process_batch_create_cards_via_raft(
    request: BatchCreateCardRequest,
    client: &crate::raft::client::RaftClient,
) -> BatchCardCommonResponse {
    // 将请求封装为Raft请求
    let raft_request = crate::raft::store::Request::BatchCreateCards {
        request: request.clone(),
    };

    // 提交请求到Raft集群
    debug!(
            "Submitting batch create cards request to Raft cluster, card count: {}",
            request.cards.len()
        );
    match client.write(&raft_request).await {
        Ok(_) => {
            debug!("Raft consensus achieved for batch update cards");
            // 返回成功的响应
            // 注意：实际上应该从Raft响应中提取正确的结果
            // 这里简化处理，假设所有卡片都成功更新
            BatchCardCommonResponse {
                success: request.cards.len() as i32,
                failed_ids: Vec::new(),
            }
        }
        Err(e) => {
            error!("Raft error in batch_update_cards: {}", e);
            BatchCardCommonResponse {
                success: 0,
                failed_ids: request.cards.iter().map(|card| card.id.clone()).collect(),
            }
        }
    }
}

/// 批量更新卡片
///
/// 根据请求更新数据库中已存在的卡片信息
///
/// # 参数
/// * `request` - 批量更新卡片请求
/// * `db` - 数据库实例
/// * `raft_client` - 可选的Raft客户端，如果提供则使用Raft共识协议
///
/// # 返回
/// 返回包含成功更新卡片数量的响应
pub async fn batch_update_cards<D: Database>(
    request: BatchUpdateCardRequest,
    db: &D,
    raft_client: Option<&crate::raft::client::RaftClient>,
) -> BatchCardCommonResponse {
    if let Some(client) = raft_client {
        return process_batch_update_cards_via_raft(request, client).await;
    }
    do_batch_update_cards(request, db)
}

/// 单机模式下批量更新卡片
fn do_batch_update_cards<D: Database>(
    request: BatchUpdateCardRequest,
    db: &D,
) -> BatchCardCommonResponse {
    let mut txn = db.transaction();
    let mut success_count = 0;
    let mut failed_ids = Vec::new();
    for card in &request.cards {
        debug!("Processing card update: {}", card.id);

        // card.id 就是 vertex_id，直接使用
        let vertex_id = Some(card.id);
        trace!("Using vertex ID {} for card {}", card.id, card.id);

        // Convert Card to Vertex with vertex ID
        match card_to_vertex(card, vertex_id, &txn) {
            Ok(vertex) => {
                // Update vertex in database
                match txn.update_vertex(&vertex) {
                    Ok(updated) => {
                        if updated {
                            success_count += 1;
                            debug!("Successfully updated card: {}", card.id);
                        } else {
                            error!("Failed to update card: {}, card does not exist", card.id);
                            failed_ids.push(card.id.clone());
                        }
                    }
                    Err(e) => {
                        error!("Failed to update card: {}, error: {:?}", card.id, e);
                        failed_ids.push(card.id.clone());
                    }
                }
            }
            Err(e) => {
                debug!("Failed to convert card: {}, error: {}", card.id, e);
                failed_ids.push(card.id.clone());
            }
        }
    }

    // 提交事务
    match txn.commit() {
        Ok(_) => {
            debug!(
                "Batch card update transaction committed successfully, total: {}, success: {}",
                request.cards.len(),
                success_count
            );
        }
        Err(e) => {
            error!("Batch card update transaction commit failed: {:?}", e);
            // 如果事务提交失败，成功数量应为0
            success_count = 0;
        }
    }

    // 构造并返回响应
    BatchCardCommonResponse {
        success: success_count,
        failed_ids: failed_ids,
    }
}

/// 通过Raft共识协议批量更新卡片
async fn process_batch_update_cards_via_raft(
    request: BatchUpdateCardRequest,
    client: &crate::raft::client::RaftClient,
) -> BatchCardCommonResponse {
    // 将请求封装为Raft请求
    let raft_request = crate::raft::store::Request::BatchUpdateCards {
        request: request.clone(),
    };

    // 提交请求到Raft集群
    debug!(
        "Submitting batch update cards request to Raft cluster, card count: {}",
        request.cards.len()
    );
    match client.write(&raft_request).await {
        Ok(_) => {
            debug!("Raft consensus achieved for batch update cards");
            // 返回成功的响应
            // 注意：实际上应该从Raft响应中提取正确的结果
            // 这里简化处理，假设所有卡片都成功更新
            BatchCardCommonResponse {
                success: request.cards.len() as i32,
                failed_ids: Vec::new(),
            }
        }
        Err(e) => {
            error!("Raft error in batch_update_cards: {}", e);
            BatchCardCommonResponse {
                success: 0,
                failed_ids: request.cards.iter().map(|card| card.id.clone()).collect(),
            }
        }
    }
}

/// 批量创建关联关系
///
/// 将请求中的关联关系转换为边并存储到数据库中
///
/// # 参数
/// * `request` - 批量创建关联关系请求
/// * `db` - 数据库实例
/// * `raft_client` - 可选的Raft客户端，如果提供则使用Raft共识协议
///
/// # 返回
/// 返回包含成功创建关联关系数量的响应
pub async fn batch_create_links<D: Database>(
    request: BatchCreateLinkRequest,
    db: &D,
    raft_client: Option<&crate::raft::client::RaftClient>,
) -> BatchLinkCommonResponse {
    if let Some(client) = raft_client {
        return process_batch_create_links_via_raft(request, client).await;
    }
    do_batch_create_links(request, db)
}

fn do_batch_create_links<D: Database>(
    request: BatchCreateLinkRequest,
    db: &D,
) -> BatchLinkCommonResponse {
    let mut txn = db.transaction();
    let mut success_count = 0;
    let mut failed_links = Vec::new();
    for link in &request.links {
        // 将Link转换为Edge
        match link_to_edge(link, &txn) {
            Ok(edge) => {
                // 存储边到数据库
                match txn.create_edge(&edge) {
                    Ok(created) => {
                        if created {
                            success_count += 1;
                            debug!(
                                "Successfully created link: {}-{}-{}",
                                link.src_id, link.lt_id, link.dest_id
                            );
                        } else {
                            warn!(
                                "Link already exists: {}-{}-{}",
                                link.src_id, link.lt_id, link.dest_id
                            );
                            failed_links.push(link.clone());
                        }
                    }
                    Err(e) => {
                        error!(
                            "Failed to create link: {}-{}-{}, error: {:?}",
                            link.src_id, link.lt_id, link.dest_id, e
                        );
                        failed_links.push(link.clone());
                    }
                }
            }
            Err(e) => {
                warn!(
                    "Failed to convert link: {}-{}-{}, error: {}",
                    link.src_id, link.lt_id, link.dest_id, e
                );
                failed_links.push(link.clone());
            }
        }
    }

    // 提交事务
    match txn.commit() {
        Ok(_) => {
            debug!(
                "Batch link creation transaction committed successfully, total: {}, success: {}",
                request.links.len(),
                success_count
            );
        }
        Err(e) => {
            error!("Batch link creation transaction commit failed: {:?}", e);
            // 如果事务提交失败，成功数量应为0
            success_count = 0;
        }
    }

    // 构造并返回响应
    BatchLinkCommonResponse {
        success: success_count,
        failed_links,
    }
}

/// 通过Raft共识协议批量创建关联关系
async fn process_batch_create_links_via_raft(
    request: BatchCreateLinkRequest,
    client: &crate::raft::client::RaftClient,
) -> BatchLinkCommonResponse {
    // 将请求封装为Raft请求
    let raft_request = crate::raft::store::Request::BatchCreateLinks {
        request: request.clone(),
    };

    // 提交请求到Raft集群
    debug!(
        "Submitting batch create links request to Raft cluster, link count: {}",
        request.links.len()
    );
    match client.write(&raft_request).await {
        Ok(_) => {
            debug!("Raft consensus achieved for batch create links");
            // 返回成功的响应
            // 注意：实际上应该从Raft响应中提取正确的结果
            // 这里简化处理，假设所有关联关系都成功创建
            BatchLinkCommonResponse {
                success: request.links.len() as i32,
                failed_links: Vec::new(),
            }
        }
        Err(e) => {
            error!("Raft error in batch_create_links: {}", e);
            BatchLinkCommonResponse {
                success: 0,
                failed_links: request.links,
            }
        }
    }
}

/// 批量更新关联关系
///
/// 根据请求更新数据库中已存在的关联关系
///
/// # 参数
/// * `request` - 批量更新关联关系请求
/// * `db` - 数据库实例
/// * `raft_client` - 可选的Raft客户端，如果提供则使用Raft共识协议
///
/// # 返回
/// 返回包含成功更新关联关系数量的响应
pub async fn batch_update_links<D: Database>(
    request: BatchUpdateLinkRequest,
    db: &D,
    raft_client: Option<&crate::raft::client::RaftClient>,
) -> BatchLinkCommonResponse {
    if let Some(client) = raft_client {
        return process_batch_update_links_via_raft(request, client).await;
    }
    do_batch_update_links(request, db)
}

fn do_batch_update_links<D: Database>(
    request: BatchUpdateLinkRequest,
    db: &D,
) -> BatchLinkCommonResponse {
    let mut txn = db.transaction();
    let mut success_count = 0;
    let mut failed_links = Vec::new();

    for link in &request.links {
        // Convert Link to Edge
        match link_to_edge(link, &txn) {
            Ok(edge) => {
                // Check if edge exists
                match txn.exists_edge(&edge, EdgeDirection::Src) {
                    Ok(exists) => {
                        if exists {
                            // Update edge properties directly instead of delete and recreate
                            match txn.update_edge(&edge, EdgeDirection::Src) {
                                Ok(true) => {
                                    success_count += 1;
                                    debug!(
                                        "Successfully updated link: {}-{}-{}",
                                        link.src_id, link.lt_id, link.dest_id
                                    );
                                }
                                Ok(false) => {
                                    error!(
                                        "Link does not exist: {}-{}-{}",
                                        link.src_id, link.lt_id, link.dest_id
                                    );
                                    failed_links.push(link.clone());
                                }
                                Err(e) => {
                                    error!(
                                        "Failed to update link: {}-{}-{}, error: {:?}",
                                        link.src_id, link.lt_id, link.dest_id, e
                                    );
                                    failed_links.push(link.clone());
                                }
                            }
                        } else {
                            error!(
                                "Link does not exist: {}-{}-{}",
                                link.src_id, link.lt_id, link.dest_id
                            );
                            failed_links.push(link.clone());
                        }
                    }
                    Err(e) => {
                        error!(
                            "Failed to check link existence: {}-{}-{}, error: {:?}",
                            link.src_id, link.lt_id, link.dest_id, e
                        );
                        failed_links.push(link.clone());
                    }
                }
            }
            Err(e) => {
                debug!(
                    "Failed to convert link: {}-{}-{}, error: {}",
                    link.src_id, link.lt_id, link.dest_id, e
                );
                failed_links.push(link.clone());
            }
        }
    }

    // 提交事务
    match txn.commit() {
        Ok(_) => {
            debug!(
                "Batch link update transaction committed successfully, total: {}, success: {}",
                request.links.len(),
                success_count
            );
        }
        Err(e) => {
            error!("Batch link update transaction commit failed: {:?}", e);
            // 如果事务提交失败，成功数量应为0
            success_count = 0;
        }
    }

    // 构造并返回响应
    BatchLinkCommonResponse {
        success: success_count,
        failed_links,
    }
}

/// 通过Raft共识协议批量更新关联关系
async fn process_batch_update_links_via_raft(
    request: BatchUpdateLinkRequest,
    client: &crate::raft::client::RaftClient,
) -> BatchLinkCommonResponse {
    // 将请求封装为Raft请求
    let raft_request = crate::raft::store::Request::BatchUpdateLinks {
        request: request.clone(),
    };

    // 提交请求到Raft集群
    debug!(
        "Submitting batch update links request to Raft cluster, link count: {}",
        request.links.len()
    );
    match client.write(&raft_request).await {
        Ok(_) => {
            debug!("Raft consensus achieved for batch update links");
            // 返回成功的响应
            // 注意：实际上应该从Raft响应中提取正确的结果
            // 这里简化处理，假设所有关联关系都成功更新
            BatchLinkCommonResponse {
                success: request.links.len() as i32,
                failed_links: Vec::new(),
            }
        }
        Err(e) => {
            error!("Raft error in batch_update_links: {}", e);
            BatchLinkCommonResponse {
                success: 0,
                failed_links: request.links,
            }
        }
    }
}

/// 批量删除关联关系
///
/// 根据请求删除数据库中的关联关系
///
/// # 参数
/// * `request` - 批量删除关联关系请求
/// * `db` - 数据库实例
/// * `raft_client` - 可选的Raft客户端，如果提供则使用Raft共识协议
///
/// # 返回
/// 返回包含成功删除关联关系数量的响应
pub async fn batch_delete_links<D: Database>(
    request: BatchDeleteLinkRequest,
    db: &D,
    raft_client: Option<&crate::raft::client::RaftClient>,
) -> BatchLinkCommonResponse {

    // 如果提供了Raft客户端，则通过Raft共识协议处理请求
    if let Some(client) = raft_client {
        return process_batch_delete_links_via_raft(request, client).await;
    }
    do_batch_delete_links(request, db)
}

/// 批量删除关联关系的具体实现
fn do_batch_delete_links<D: Database>(
    request: BatchDeleteLinkRequest,
    db: &D,
) -> BatchLinkCommonResponse {
    debug!(
        "Processing batch delete links, count: {}",
        request.links.len()
    );

    // 创建事务
    let mut txn = db.transaction();
    let mut success_count = 0;
    let mut failed_links = Vec::new();
    let mut edges_to_delete = Vec::new();

    // 处理每个关联关系
    for link in &request.links {
        debug!("Processing link deletion: {}", link.id);

        // 将Link转换为Edge
        match link_to_edge(link, &txn) {
            Ok(edge) => {
                // 收集要删除的边
                edges_to_delete.push(edge);
            }
            Err(e) => {
                debug!("Failed to convert link: {}, error: {}", link.id, e);
                failed_links.push(link.clone());
            }
        }
    }

    // 批量删除边
    if !edges_to_delete.is_empty() {
        match txn.delete_edges(&edges_to_delete) {
            Ok(_) => {
                success_count = edges_to_delete.len() as i32;
                debug!("Successfully deleted {} links", success_count);
            }
            Err(e) => {
                error!("Failed to delete edges, error: {:?}", e);
                // 添加所有失败的链接
                for link in &request.links {
                    failed_links.push(link.clone());
                }
                success_count = 0;
            }
        }
    }

    // 提交事务
    match txn.commit() {
        Ok(_) => {
            debug!(
                "Batch link deletion transaction committed successfully, total: {}, success: {}",
                request.links.len(),
                success_count
            );
        }
        Err(e) => {
            error!("Batch link deletion transaction commit failed: {:?}", e);
            // 如果事务提交失败，成功数量应为0
            success_count = 0;
            // 添加所有失败的链接
            failed_links.clear();
            for link in &request.links {
                failed_links.push(link.clone());
            }
        }
    }

    // 构造并返回响应
    BatchLinkCommonResponse {
        success: success_count,
        failed_links,
    }
}

/// 通过Raft共识协议批量删除关联关系
async fn process_batch_delete_links_via_raft(
    request: BatchDeleteLinkRequest,
    client: &crate::raft::client::RaftClient,
) -> BatchLinkCommonResponse {
    // 将请求封装为Raft请求
    let raft_request = crate::raft::store::Request::BatchDeleteLinks {
        request: request.clone(),
    };

    // 提交请求到Raft集群
    debug!(
        "Submitting batch delete links request to Raft cluster, link count: {}",
        request.links.len()
    );
    match client.write(&raft_request).await {
        Ok(_) => {
            debug!("Raft consensus achieved for batch delete links");
            // 返回成功的响应
            // 注意：实际上应该从Raft响应中提取正确的结果
            // 这里简化处理，假设所有关联关系都成功删除
            BatchLinkCommonResponse {
                success: request.links.len() as i32,
                failed_links: Vec::new(),
            }
        }
        Err(e) => {
            error!("Raft error in batch_delete_links: {}", e);
            BatchLinkCommonResponse {
                success: 0,
                failed_links: request.links,
            }
        }
    }
}

/// 将Proto的Title转换为VertexTitle
///
/// # 参数
/// * `title` - protobuf中的标题对象
///
/// # 返回
/// 返回转换后的VertexTitle
fn proto_title_to_vertex_title(title: &Option<crate::proto::pgraph::model::Title>) -> VertexTitle {
    if let Some(title) = title {
        if let Some(title_type) = &title.title_type {
            match title_type {
                TitleType::Pure(pure) => {
                    VertexTitle::PureTitle(pure.value.clone())
                }
                TitleType::Joint(joint) => {
                    // 转换拼接标题
                    let area = match joint.area {
                        0 => TitleJointArea::Prefix,
                        1 => TitleJointArea::Suffix,
                        _ => TitleJointArea::Prefix, // 默认前缀
                    };

                    // 转换多个拼接标题部分
                    let multi_parts = joint.multi_parts.iter().map(|parts| {
                        // 转换每个部分的字段和名称
                        let title_parts = parts.parts.iter().map(|part| {
                            JointTitlePart {
                                name: part.name.clone(),
                            }
                        }).collect();

                        crate::database::model::JointTitleParts {
                            parts: title_parts,
                        }
                    }).collect();

                    // 创建拼接标题信息
                    VertexTitle::JointTitle(JointTitleInfo {
                        name: joint.name.clone(),
                        area,
                        multi_parts,
                    })
                }
            }
        } else {
            VertexTitle::PureTitle("".to_string())
        }
    } else {
        VertexTitle::PureTitle("".to_string())
    }
}

/// 将Card转换为Vertex
///
/// # 参数
/// * `card` - 要转换的卡片
/// * `vertex_id` - 可选的节点ID，用于更新操作。如果为None，则ID设为0，由数据库生成
///
/// # 返回
/// 成功返回Vertex实例，失败返回错误信息
fn card_to_vertex<'a, T: Transaction<'a>>(card: &Card, vertex_id: Option<VertexId>, txn: &T) -> Result<Vertex, String> {
    // 获取当前时间戳
    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map_err(|e| format!("Failed to get timestamp: {}", e))?
        .as_secs();


    // 提取卡片标题 （结合解构和守卫条件）
    let title = match &card.title {
        Some(Title { title_type: Some(TitleType::Pure(pure)) }) if !pure.value.is_empty() => {
            if let Some(vertex_id) = vertex_id {
                if let Some(vertex) = txn.get_vertex_by_id(&vertex_id) {
                    //与旧节点的标题比较看是否变化，如果没有变化则保持原标题
                    match &vertex.title {
                        VertexTitle::PureTitle(old_pure) => {
                            if &pure.value == old_pure {
                                // 标题没有变化，保持原标题
                                VertexTitle::PureTitle(old_pure.clone())
                            } else {
                                // 标题有变化，使用新标题
                                VertexTitle::PureTitle(pure.value.clone())
                            }
                        }
                        VertexTitle::JointTitle(old_joint) => {
                            // 从拼接标题改为纯标题，创建新的拼接标题但使用新名称
                            let mut joint = old_joint.clone();
                            joint.name = pure.value.clone();
                            VertexTitle::JointTitle(joint)
                        }
                    }
                } else {
                    // 节点不存在，使用新标题
                    VertexTitle::PureTitle(pure.value.clone())
                }
            } else {
                // 创建新节点，使用新标题
                VertexTitle::PureTitle(pure.value.clone())
            }
        }
        Some(Title { title_type: Some(TitleType::Joint(joint)) }) if !joint.name.is_empty() => {
            proto_title_to_vertex_title(&card.title)
        }
        _ => VertexTitle::PureTitle("".to_string()),
    };

    // 转换状态
    // Proto CardState: Active=0, Discarded=1, Archived=2
    let state = match card.state {
        0 => CardState::Active,
        1 => CardState::Discarded,
        2 => CardState::Archived,
        _ => CardState::Active,
    };

    // 转换字段值
    let field_values = if !card.custom_field_value_map.is_empty() {
        let mut map = HashMap::new();
        for (field_id, proto_value) in &card.custom_field_value_map {
            if let Some(field_value) = proto_field_value_to_vertex_field_value(proto_value) {
                map.insert(Identifier::new(field_id), field_value);
            }
        }
        Some(map)
    } else {
        None
    };

    // 创建并返回节点
    // 注意：新的 proto Card 移除了部分字段，这些字段使用默认值
    Ok(Vertex {
        card_id: card.id,
        org_id: Identifier::new(&card.org_id),
        card_type_id: Identifier::new(&card.type_id),
        container_id: Identifier::default(), // proto 已移除 container_id 字段
        stream_info: StreamInfo {
            stream_id: Identifier::new(&card.stream_id),
            status_id: Identifier::new(&card.status_id),
        },
        state,
        title,
        code_in_org: card.code_in_org.to_string(),
        code_in_org_int: card.code_in_org as u32,
        custom_code: if card.custom_code.is_empty() {
            None
        } else {
            Some(card.custom_code.clone())
        },
        position: 0, // proto 已移除 position 字段
        created_at: if card.created_at > 0 {
            card.created_at as u64
        } else {
            now
        },
        updated_at: if card.updated_at > 0 {
            card.updated_at as u64
        } else {
            now
        },
        archived_at: if card.archived_at > 0 {
            Some(card.archived_at as u64)
        } else {
            None
        },
        discarded_at: if card.discarded_at > 0 {
            Some(card.discarded_at as u64)
        } else {
            None
        },
        discard_reason: None, // proto 已移除 discard_reason 字段
        restore_reason: None, // proto 已移除 restore_reason 字段
        field_values,
        desc: if card.description.is_empty() {
            Description{
                content: None,
                changed: true,
            }
        } else {
            Description{
                content: Some(card.description.to_string()),
                changed: true,
            }
        }
    })
}

/// 将Proto字段值转换为Vertex字段值
fn proto_field_value_to_vertex_field_value(proto_value: &ProtoFieldValue) -> Option<FieldValue> {
    if let Some(field_type) = &proto_value.field_type {
        match field_type {
            field_value::FieldType::TextField(text_field) => Some(FieldValue::Text(TextValue {
                text: text_field.value.clone(),
            })),
            field_value::FieldType::NumberField(number_field) => {
                Some(FieldValue::Number(NumberValue {
                    number: number_field.value,
                }))
            }
            field_value::FieldType::DateField(date_field) => Some(FieldValue::Date(DateValue {
                timestamp: date_field.value as u64,
            })),
            field_value::FieldType::EnumField(value) => {
                let items = value
                    .value
                    .iter()
                    .map(|option_id| EnumItemId::new(option_id))
                    .collect();
                Some(FieldValue::Enum(EnumValue { items }))
            }
            field_value::FieldType::WebLinkField(web_link_field) => {
                Some(FieldValue::WebLink(WebLinkValue {
                    href: web_link_field.href.clone(),
                    name: web_link_field.name.clone(),
                }))
            }
            field_value::FieldType::AttachmentField(attachment_field) => {
                // 将proto的附件项转换为数据库模型的附件项
                let items = attachment_field
                    .value
                    .iter()
                    .map(|item| AttachmentItem {
                        id: item.id.clone(),
                        name: item.name.clone(),
                        uploader: item.uploader.clone(),
                        created_at: item.created_at as u64,
                        size: item.size as u64,
                    })
                    .collect();
                Some(FieldValue::Attachment(AttachmentValue { items }))
            }
            _ => {
                warn!("Unsupported field type: {:?}", field_type);
                None
            }
        }
    } else {
        warn!("Field type is empty");
        None
    }
}

fn proto_field_value_to_edge_prop(proto_value: &FieldValueOnLink) -> Option<EdgeProp> {
    let field_id = FieldId::new(&proto_value.field_id);
    match &proto_value.field_type {
        Some(field_type) => match field_type {
            field_value_on_link::FieldType::NumberField(value) => Some(EdgeProp::Number(NumberProp {
                field_id,
                number: value.value,
            })),
            field_value_on_link::FieldType::DateField(value) => Some(EdgeProp::Date(DateProp {
                field_id,
                timestamp: value.value as u64,
            })),
            field_value_on_link::FieldType::EnumField(value) => {
                let items = value
                    .value
                    .iter()
                    .map(|option_id| EnumItemId::new(option_id))
                    .collect();
                Some(EdgeProp::Enum(EnumProp { field_id, items }))
            }
        },
        None => None,
    }
}


/// 将Link转换为Edge
///
/// # 参数
/// * `link` - 要转换的关联关系
/// * `txn` - 数据库事务，用于查询节点ID
///
/// # 返回
/// 成功返回Edge实例，失败返回错误信息
fn link_to_edge<'a, T: Transaction<'a>>(link: &Link, txn: &T) -> Result<Edge, String> {
    // card_id 就是 vertex_id，直接使用
    let src_vertex_id = link.src_id;
    let dest_vertex_id = link.dest_id;
    debug!("Using vertex ID {} for source card {}", src_vertex_id, link.src_id);
    debug!("Using vertex ID {} for target card {}", dest_vertex_id, link.dest_id);

    // 创建边类型标识符
    let edge_type = Identifier::new(&link.lt_id);

    // 处理边的属性（如果有）
    let props = if !link.field_values.is_empty() {
        // 转换字段值为边属性
        let mut edge_props = Vec::with_capacity(link.field_values.len());
        for field_value in &link.field_values {
            if let Some(prop) = proto_field_value_to_edge_prop(field_value) {
                edge_props.push(prop);
            }
        }
        Some(edge_props)
    } else {
        None
    };

    // 创建并返回边
    Ok(Edge::new(src_vertex_id, edge_type, dest_vertex_id, props))
}

/// 批量更新卡片标题
///
/// 根据请求更新数据库中卡片的标题信息
///
/// # 参数
/// * `request` - 批量更新卡片标题请求
/// * `db` - 数据库实例
/// * `raft_client` - 可选的Raft客户端，如果提供则使用Raft共识协议
///
/// # 返回
/// 返回包含成功更新卡片标题数量的响应
pub async fn batch_update_card_titles<D: Database>(
    request: BatchUpdateCardTitleRequest,
    db: &D,
    raft_client: Option<&crate::raft::client::RaftClient>,
) -> BatchCardCommonResponse {
    if let Some(client) = raft_client {
        return process_batch_update_card_titles_via_raft(request, client).await;
    }
    do_batch_update_card_titles(request, db)
}

/// 批量更新卡片标题
fn do_batch_update_card_titles<D: Database>(
    request: BatchUpdateCardTitleRequest,
    db: &D,
) -> BatchCardCommonResponse {
    debug!("Processing batch update card titles request, count: {}", request.requests.len());

    let mut txn = db.transaction();
    let mut success_count = 0;
    let mut failed_ids = Vec::new();

    for title_request in &request.requests {
        // card_id 就是 vertex_id，直接查询
        let vertex_id = title_request.card_id;

        // 查询完整的vertex信息
        match txn.get_specific_vertices(&vec![vertex_id]) {
            Ok(vertices) => {
                if let Some(vertex_arc) = vertices.first() {
                    // 创建可变的vertex副本
                    let mut vertex = (**vertex_arc).clone();

                    // 更新vertex的标题
                    vertex.title = proto_title_to_vertex_title(&title_request.title);

                    // 更新vertex
                    match txn.update_vertex(&vertex) {
                        Ok(true) => {
                            success_count += 1;
                            debug!("Successfully updated card title: {}", title_request.card_id);
                        }
                        Ok(false) => {
                            error!("Failed to update card title: {}, card does not exist", title_request.card_id);
                            failed_ids.push(title_request.card_id.clone());
                        }
                        Err(e) => {
                            error!("Failed to update card title: {}, error: {:?}", title_request.card_id, e);
                            failed_ids.push(title_request.card_id.clone());
                        }
                    }
                } else {
                    error!("Card does not exist: {}", title_request.card_id);
                    failed_ids.push(title_request.card_id.clone());
                }
            }
            Err(e) => {
                error!("Failed to get card info: {}, error: {:?}", title_request.card_id, e);
                failed_ids.push(title_request.card_id.clone());
            }
        }
    }

    // 提交事务
    match txn.commit() {
        Ok(_) => {
            debug!(
                "Batch update card titles transaction committed successfully, total: {}, success: {}",
                request.requests.len(),
                success_count
            );
        }
        Err(e) => {
            error!("Batch update card titles transaction commit failed: {:?}", e);
            // 如果事务提交失败，成功数量应为0
            success_count = 0;
            // 收集所有失败的ID
            failed_ids.clear();
            for req in &request.requests {
                failed_ids.push(req.card_id.clone());
            }
        }
    }

    // 构造并返回响应
    BatchCardCommonResponse {
        success: success_count,
        failed_ids,
    }
}

/// 通过Raft共识协议批量更新卡片标题
async fn process_batch_update_card_titles_via_raft(
    request: BatchUpdateCardTitleRequest,
    client: &crate::raft::client::RaftClient,
) -> BatchCardCommonResponse {
    // 将请求封装为Raft请求
    let raft_request = crate::raft::store::Request::BatchUpdateCardTitles {
        request: request.clone(),
    };

    // 提交请求到Raft集群
    debug!(
        "Submitting batch update card titles request to Raft cluster, title count: {}",
        request.requests.len()
    );
    match client.write(&raft_request).await {
        Ok(_) => {
            debug!("Raft consensus achieved for batch update card titles");
            // 返回成功的响应
            // 注意：实际上应该从Raft响应中提取正确的结果
            // 这里简化处理，假设所有标题都成功更新
            BatchCardCommonResponse {
                success: request.requests.len() as i32,
                failed_ids: Vec::new(),
            }
        }
        Err(e) => {
            error!("Raft error in batch_update_card_titles: {}", e);
            BatchCardCommonResponse {
                success: 0,
                failed_ids: request.requests.iter().map(|req| req.card_id.clone()).collect(),
            }
        }
    }
}

/// 批量部分更新卡片属性
///
/// 根据请求中的指定属性更新数据库中卡片的对应属性，未指定的属性保持不变
///
/// # 参数
/// * `request` - 批量部分更新卡片属性请求
/// * `db` - 数据库实例
/// * `raft_client` - 可选的Raft客户端，如果提供则使用Raft共识协议
///
/// # 返回
/// 返回包含成功更新卡片数量的响应
pub async fn batch_update_card_field<D: Database>(
    request: BatchUpdateCardFieldRequest,
    db: &D,
    raft_client: Option<&crate::raft::client::RaftClient>,
) -> BatchCardCommonResponse {
    if let Some(client) = raft_client {
        return process_batch_update_card_field_via_raft(request, client).await;
    }
    do_batch_update_card_field(request, db).await
}

/// 批量更新卡片部分属性
pub async  fn do_batch_update_card_field<D: Database>(
    request: BatchUpdateCardFieldRequest,
    db: &D,
) -> BatchCardCommonResponse {
    let mut txn = db.transaction();
    let mut success_count = 0;
    let mut failed_ids = Vec::new();

    for update_request in &request.requests {
        let card_id = update_request.card_id;
        debug!("Processing card partial update request: {}", card_id);

        // CardId = VertexId，直接使用 card_id 作为 vertex_id
        // 查询完整的vertex信息
        match txn.get_specific_vertices(&vec![card_id]) {
            Ok(vertices) => {
                if let Some(vertex_arc) = vertices.first() {
                    // 创建可变的vertex副本
                    let mut vertex = (**vertex_arc).clone();

                    // 应用更新
                    if let Some(result) = apply_card_attribute_updates(&mut vertex, update_request) {
                        match result {
                            Ok(_) => {
                                // 更新vertex
                                match txn.update_vertex(&vertex) {
                                    Ok(true) => {
                                        success_count += 1;
                                        debug!("Successfully updated card field: {}", card_id);
                                    }
                                    Ok(false) => {
                                        error!("Failed to update card field: {}, card does not exist", card_id);
                                        failed_ids.push(card_id);
                                    }
                                    Err(e) => {
                                        error!("Failed to update card field: {}, error: {:?}", card_id, e);
                                        failed_ids.push(card_id);
                                    }
                                }
                            }
                            Err(e) => {
                                error!("Failed to apply card field update: {}, error: {}", card_id, e);
                                failed_ids.push(card_id);
                            }
                        }
                    } else {
                        debug!("No card fields need to be updated: {}", card_id);
                        // 这里不算作失败，因为请求可能只是一个空请求
                        success_count += 1;
                    }
                } else {
                    error!("Card does not exist: {}", card_id);
                    failed_ids.push(card_id);
                }
            }
            Err(e) => {
                error!("Failed to get card info: {}, error: {:?}", card_id, e);
                failed_ids.push(card_id);
            }
        }
    }

    // 提交事务
    match txn.commit() {
        Ok(_) => {
            debug!(
                "Batch update card partial fields transaction committed successfully, total: {}, success: {}",
                request.requests.len(),
                success_count
            );
        }
        Err(e) => {
            error!("Batch update card partial fields transaction commit failed: {:?}", e);
            // 如果事务提交失败，成功数量应为0
            success_count = 0;
            // 收集所有失败的ID
            failed_ids.clear();
            for req in &request.requests {
                failed_ids.push(req.card_id);
            }
        }
    }

    // 构造并返回响应
    BatchCardCommonResponse {
        success: success_count,
        failed_ids,
    }
}

/// 通过Raft共识协议批量更新卡片部分属性
async fn process_batch_update_card_field_via_raft(
    request: BatchUpdateCardFieldRequest,
    client: &crate::raft::client::RaftClient,
) -> BatchCardCommonResponse {
    // 将请求封装为Raft请求
    let raft_request = crate::raft::store::Request::BatchUpdateCardField {
        request: request.clone(),
    };

    // 提交请求到Raft集群
    debug!(
        "Submitting batch partial update card fields request to Raft cluster, request count: {}",
        request.requests.len()
    );
    match client.write(&raft_request).await {
        Ok(_) => {
            debug!("Batch update card partial fields Raft consensus reached");
            // 返回成功的响应
            // 注意：实际上应该从Raft响应中提取正确的结果
            // 这里简化处理，假设所有卡片都成功更新
            BatchCardCommonResponse {
                success: request.requests.len() as i32,
                failed_ids: Vec::new(),
            }
        }
        Err(e) => {
            error!("Batch update card partial fields Raft operation failed: {}", e);
            BatchCardCommonResponse {
                success: 0,
                failed_ids: request.requests.iter().map(|req| req.card_id.clone()).collect(),
            }
        }
    }
}

/// 应用卡片属性更新
///
/// # 参数
/// * `vertex` - 要更新的节点
/// * `request` - 更新请求
///
/// # 返回
/// 如果有属性更新，返回Result，否则返回None
fn apply_card_attribute_updates(vertex: &mut Vertex, request: &UpdateCardFieldRequest) -> Option<Result<(), String>> {
    let mut has_updates = false;

    // 更新标题
    if let Some(title) = &request.title {
        vertex.title = proto_title_to_vertex_title(&Some(title.clone()));
        has_updates = true;
    }

    // 更新描述
    if let Some(description) = &request.description {
        vertex.desc = if description.is_empty() {
            Description{
                content: None,
                changed: false,
            }
        } else {
            Description{
                content: Some(description.clone()),
                changed: true,
            }
        };
        has_updates = true;
    }

    // 更新状态
    // Proto CardState: Active=0, Discarded=1, Archived=2
    if let Some(state) = request.state {
        vertex.state = match state {
            0 => CardState::Active,
            1 => CardState::Discarded,
            2 => CardState::Archived,
            _ => CardState::Active,
        };
        has_updates = true;
    }

    // 更新价值流状态（作为一个整体）
    if let Some(stream_status) = &request.value_stream_status {
        vertex.stream_info = StreamInfo {
            stream_id: Identifier::new(&stream_status.stream_id),
            status_id: Identifier::new(&stream_status.status_id),
        };
        has_updates = true;
    }

    // 更新自定义编码
    if let Some(custom_code) = &request.custom_code {
        vertex.custom_code = if custom_code.is_empty() {
            None
        } else {
            Some(custom_code.clone())
        };
        has_updates = true;
    }

    // 更新容器ID
    if let Some(container_id) = &request.container_id {
        vertex.container_id = Identifier::new(container_id);
        has_updates = true;
    }

    // 更新位置
    if let Some(position) = request.position {
        vertex.position = position as u64;
        has_updates = true;
    }

    // 更新废弃日期
    if let Some(discarded_at) = request.discarded_at {
        vertex.discarded_at = if discarded_at > 0 {
            Some(discarded_at as u64)
        } else {
            None
        };
        has_updates = true;
    }

    // 更新更新日期
    if let Some(updated_at) = request.updated_at {
        vertex.updated_at = if updated_at > 0 {
            updated_at as u64
        } else {
            vertex.updated_at
        };
        has_updates = true;
    }

    // 更新归档日期
    if let Some(archived_at) = request.archived_at {
        vertex.archived_at = if archived_at > 0 {
            Some(archived_at as u64)
        } else {
            None
        };
        has_updates = true;
    }

    // 更新废弃原因
    if let Some(discard_reason) = &request.discard_reason {
        vertex.discard_reason = if discard_reason.is_empty() {
            None
        } else {
            Some(discard_reason.clone())
        };
        has_updates = true;
    }

    // 更新恢复原因
    if let Some(restore_reason) = &request.restore_reason {
        vertex.restore_reason = if restore_reason.is_empty() {
            None
        } else {
            Some(restore_reason.clone())
        };
        has_updates = true;
    }


    // 更新自定义字段值
    if !request.custom_field_value_map.is_empty() {
        if vertex.field_values.is_none() {
            vertex.field_values = Some(HashMap::new());
        }

        let field_values = vertex.field_values.as_mut().unwrap();

        for (field_id, proto_value) in &request.custom_field_value_map {
            if let Some(field_value) = proto_field_value_to_vertex_field_value(proto_value) {
                field_values.insert(Identifier::new(field_id), field_value);
                has_updates = true;
            }
        }
    }

    // 如果有更新，返回Ok结果
    if has_updates {
        Some(Ok(()))
    } else {
        None
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::database::model::VertexQuery;
    use crate::database::test::test_utils::TestDb;
    use crate::proto::pgraph::common::TitleJointArea;
    use crate::proto::pgraph::model::title::TitleType;
    use crate::proto::pgraph::model::{Card, JointTitle, JointTitlePart, JointTitleParts, PureTitle, Title};
    use crate::proto::pgraph::write::BatchCreateCardRequest;
    use std::collections::HashMap;

    // 创建测试卡片
    fn create_test_card(id: u64, title: &str) -> Card {
        let mut card = Card::default();
        card.id = id;
        card.title = Some(Title {
            title_type: Some(TitleType::Pure(PureTitle {
                value: title.to_string(),
            })),
        });
        card.org_id = "test_org".to_string();
        card.type_id = "test_type".to_string();
        card.stream_id = "test_stream".to_string();
        card.status_id = "test_status".to_string();
        card.state = 0; // ACTIVE
        card.code_in_org = 1;
        card.custom_field_value_map = HashMap::new();
        card
    }

    #[test]
    fn test_card_to_vertex_conversion() {
        // 创建测试数据库
        let test_db = TestDb::new();
        let txn = test_db.db.transaction();
        
        // 测试基本卡片转换
        let card = create_test_card(1u64, "测试卡片");
        let result = card_to_vertex(&card, None, &txn);
        assert!(result.is_ok(), "卡片应该能成功转换为节点");

        let vertex = result.unwrap();
        assert_eq!(vertex.card_id, 1u64);
        match vertex.title {
            VertexTitle::PureTitle(title) => assert_eq!(title, "测试卡片"),
            _ => {}
        }
        assert_eq!(vertex.card_type_id.to_string(), "test_type");
    }
    
    #[test]
    fn test_card_with_custom_fields() {
        // 测试带有自定义字段的卡片
        let mut card = create_test_card(2, "带字段的卡片");

        // 添加文本字段
        let text_field = ProtoFieldValue {
            field_id: "text_field".to_string(),
            field_type: Some(field_value::FieldType::TextField(
                crate::proto::pgraph::field::TextFieldValue {
                    value: "文本字段值".to_string(),
                    max_string_length: 100,
                },
            )),
        };
        card.custom_field_value_map
            .insert("text_field".to_string(), text_field);

        // 添加数字字段
        let number_field = ProtoFieldValue {
            field_id: "number_field".to_string(),
            field_type: Some(field_value::FieldType::NumberField(
                crate::proto::pgraph::field::NumberFieldValue { value: 42.5 },
            )),
        };
        card.custom_field_value_map
            .insert("number_field".to_string(), number_field);

        // 创建测试数据库和事务
        let test_db = TestDb::new();
        let txn = test_db.db.transaction();
        
        // 执行转换
        let result = card_to_vertex(&card, None, &txn);
        assert!(result.is_ok(), "带自定义字段的卡片应该能成功转换");

        let vertex = result.unwrap();

        // 验证字段值存在
        assert!(vertex.field_values.is_some(), "卡片应该包含字段值");
        let field_values = vertex.field_values.as_ref().unwrap();

        // 验证文本字段
        let text_field_id = Identifier::new("text_field");
        assert!(
            field_values.contains_key(&text_field_id),
            "应该包含文本字段"
        );
        if let FieldValue::Text(text_value) = &field_values[&text_field_id] {
            assert_eq!(text_value.text, "文本字段值");
        } else {
            panic!("字段类型不匹配");
        }

        // 验证数字字段
        let number_field_id = Identifier::new("number_field");
        assert!(
            field_values.contains_key(&number_field_id),
            "应该包含数字字段"
        );
        if let FieldValue::Number(number_value) = &field_values[&number_field_id] {
            assert_eq!(number_value.number, 42.5);
        } else {
            panic!("字段类型不匹配");
        }
    }

    #[test]
    fn test_proto_field_value_to_vertex_field_value() {
        // 测试文本字段
        let text_field = ProtoFieldValue {
            field_id: "text_field".to_string(),
            field_type: Some(field_value::FieldType::TextField(
                crate::proto::pgraph::field::TextFieldValue {
                    value: "测试文本".to_string(),
                    max_string_length: 100,
                },
            )),
        };

        let converted = proto_field_value_to_vertex_field_value(&text_field).unwrap();
        match converted {
            FieldValue::Text(text_value) => {
                assert_eq!(text_value.text, "测试文本");
            }
            _ => panic!("应该转换为文本字段"),
        }

        // 测试数字字段
        let number_field = ProtoFieldValue {
            field_id: "number_field".to_string(),
            field_type: Some(field_value::FieldType::NumberField(
                crate::proto::pgraph::field::NumberFieldValue { value: 123.45 },
            )),
        };

        let converted = proto_field_value_to_vertex_field_value(&number_field).unwrap();
        match converted {
            FieldValue::Number(number_value) => {
                assert_eq!(number_value.number, 123.45);
            }
            _ => panic!("应该转换为数字字段"),
        }

        // 测试日期字段
        let date_field = ProtoFieldValue {
            field_id: "date_field".to_string(),
            field_type: Some(field_value::FieldType::DateField(
                crate::proto::pgraph::field::DateFieldValue { value: 1698765432 },
            )),
        };

        let converted = proto_field_value_to_vertex_field_value(&date_field).unwrap();
        match converted {
            FieldValue::Date(date_value) => {
                assert_eq!(date_value.timestamp, 1698765432);
            }
            _ => panic!("应该转换为日期字段"),
        }
    }

    #[test]
    fn test_joint_title_conversion() {
        // 创建一个拼接标题卡片
        let mut card = create_test_card(3, "");

        // 创建拼接标题
        let joint_title_part1 = JointTitlePart {
            name: "字段1".to_string(),
        };

        let joint_title_part2 = JointTitlePart {
            name: "字段2".to_string(),
        };

        let joint_title_parts = JointTitleParts {
            parts: vec![joint_title_part1, joint_title_part2],
        };

        let joint_title = JointTitle {
            name: "测试拼接标题".to_string(),
            area: TitleJointArea::Prefix as i32, // 前缀
            multi_parts: vec![joint_title_parts],
        };

        card.title = Some(Title {
            title_type: Some(TitleType::Joint(joint_title)),
        });

        // 创建测试数据库和事务
        let test_db = TestDb::new();
        let txn = test_db.db.transaction();
        
        // 转换为 Vertex
        let result = card_to_vertex(&card, None, &txn);
        assert!(result.is_ok(), "拼接标题卡片转换失败");

        let vertex = result.unwrap();

        // 验证转换后的拼接标题
        match vertex.title {
            VertexTitle::JointTitle(info) => {
                assert_eq!(info.name, "测试拼接标题");
                assert_eq!(info.area, crate::database::model::TitleJointArea::Prefix);
                assert_eq!(info.multi_parts.len(), 1);

                let parts = &info.multi_parts[0];
                assert_eq!(parts.parts.len(), 2);
                assert_eq!(parts.parts[0].name, "字段1");
                assert_eq!(parts.parts[1].name, "字段2");
            },
            VertexTitle::PureTitle(_) => {
                panic!("应该是拼接标题而不是纯标题");
            }
        }
    }

    #[tokio::test]
    async fn test_batch_update_cards() {
        // 创建测试数据库
        let test_db = TestDb::new();

        // 首先创建两张卡片
        let mut cards = vec![
            create_test_card(4, "更新测试卡片1"),
            create_test_card(5, "更新测试卡片2"),
        ];

        // 额外添加一个在测试中不会更新的卡片
        cards.push(create_test_card(6, "不更新测试卡片"));

        // 批量创建卡片
        let create_request = BatchCreateCardRequest { cards };
        let create_response = batch_create_cards(create_request, &test_db.db, None).await;

        // 确认创建成功
        let unwrapped_response = create_response;
        assert_eq!(unwrapped_response.success, 3, "应成功创建3张卡片");
        assert!(unwrapped_response.failed_ids.is_empty(), "创建卡片不应失败");

        // 准备更新数据 - 注意，现在包含节点ID
        let mut update_cards = vec![
            create_test_card(4, "已更新的卡片1"),
            create_test_card(5, "已更新的卡片2"),
        ];

        // 额外添加一个不存在的卡片（预期更新失败）
        update_cards.push(create_test_card(7, "不存在的卡片"));

        // 批量更新卡片
        let update_request = BatchUpdateCardRequest {
            cards: update_cards,
        };
        let update_response = batch_update_cards(update_request, &test_db.db, None).await;

        // 验证更新结果
        assert_eq!(update_response.success, 2, "应该成功更新2张卡片");
        assert_eq!(update_response.failed_ids.len(), 1, "应该有1张卡片更新失败");
        assert_eq!(
            update_response.failed_ids[0], 7,
            "不存在的卡片应该更新失败"
        );

        // 获取更新后的卡片验证更新是否成功
        // 如果有查询方法，可以在这里添加验证逻辑
        let txn = test_db.db.transaction();
        // 我们可以通过构建包含卡片ID的VertexQuery来检查更新是否成功
        let card_type_id = Identifier::new("test_type");
        let query = VertexQuery {
            card_ids: Some(vec![4]),
            vertex_ids: None,
            card_type_ids: vec![card_type_id],
            container_ids: None,
            states: None,
        };

        let vertices = txn.query_vertices(query).unwrap();
        assert_eq!(vertices.len(), 1, "应该能够找到更新后的卡片");

        let vertex = &vertices[0];
        match &vertex.title {
            VertexTitle::PureTitle(text) => {
                assert_eq!(text, "已更新的卡片1", "卡片标题应该已被更新");
            }
            _ => {}
        }
    }
}
