use crate::database::errors::DbResult;
use crate::database::model::{CardState, Edge, EdgeDescriptor, EdgeDirection, Identifier, NeighborQuery, VertexId};
use crate::database::transaction::Transaction;
use crate::proto::pgraph::linkquery::{LinkFetchRequest, LinkQueryRequest, LinkQueryResponse};
use crate::proto::pgraph::model::Link;
use tracing::{debug, error, info};

/// 从给定的卡片id出发，沿着给定的关联关系+方向，查询关联关系
pub fn query_links<'a, T: Transaction<'a>>(
    request: LinkQueryRequest,
    txn: &'a T,
) -> LinkQueryResponse {
    debug!(
        "Executing link query, Card IDs: {:?}, Link types: {:?}",
        request.card_ids, request.lt_with_position
    );

    let mut all_links = Vec::new();

    // card_ids 就是 vertex_ids，无需转换
    let src_vertex_ids: Vec<VertexId> = request.card_ids.clone();

    for lt_with_position in &request.lt_with_position {
        // 确定边的方向
        let direction = match lt_with_position.position {
            1 => EdgeDirection::Dest, // 目标向
            _ => EdgeDirection::Src,  // 默认为源向
        };

        let query = NeighborQuery {
            src_vertex_ids: src_vertex_ids.clone(),
            edge_descriptor: EdgeDescriptor {
                t: Identifier::new(&lt_with_position.lt_id),
                direction,
            },
            dest_vertex_states: Some(vec![CardState::Active, CardState::Archived]),
        };

        // 查询边
        let edges = txn.query_neighbor_edges(&query);

        // 将边转换为链接
        let links = edges_to_links(edges, direction, txn);
        all_links.extend(links);
    }

    info!("Link query completed, found {} links", all_links.len());

    // 构建并返回响应
    LinkQueryResponse { links: all_links }
}

/// 根据 srcId + ltId + destId 三元组查找关系
pub fn fetch_links<'a, T: Transaction<'a>>(
    request: LinkFetchRequest,
    txn: &'a T,
) -> LinkQueryResponse {
    debug!(
        "Executing link fetch, Link IDs: {:?}",
        request.link_keys
    );

    let mut all_links = Vec::new();

    for link_key in &request.link_keys {
        // card_id 现在是 u64，无需解析
        let src_id = link_key.src_id;
        let lt_id = &link_key.lt_id;

        // 构建边描述符
        let edge_descriptor = EdgeDescriptor {
            t: Identifier::new(lt_id),
            direction: EdgeDirection::Src,
        };

        // 查询边
        let edges = txn.query_neighbor_edges(&NeighborQuery {
            src_vertex_ids: vec![src_id],
            edge_descriptor,
            dest_vertex_states: Some(vec![CardState::Active, CardState::Archived]),
        });

        // 将边转换为链接 TODO direction
        let links = edges_to_links(edges,EdgeDirection::Src, txn);
        all_links.extend(links);
    }

    info!("Link fetch completed, found {} links", all_links.len());

    // 构建并返回响应
    LinkQueryResponse { links: all_links }
}

/// 将Edge转换为Link
fn edge_to_link<'a, T: Transaction<'a>>(edge: Edge, direction: EdgeDirection, _txn: &'a T) -> Option<Link> {
    // 现在 vertex_id 就是 card_id，无需转换
    let mut src_card_id = edge.src_id;
    let mut dest_card_id = edge.dest_id;

    //如果方向是Dest时，src_card_id 和 dest_card_id交换
    if direction == EdgeDirection::Dest {
        let temp = src_card_id;
        src_card_id = dest_card_id;
        dest_card_id = temp;
    }

    // 构造唯一ID - 使用src_card_id作为基础id
    // 注意：这里可能需要更复杂的id生成逻辑，但现在简化处理
    let id = src_card_id;

    // 转换边属性为Proto字段值
    // 实际应用中需要根据具体的字段类型进行转换
    let field_values = Vec::new(); // 简化处理，实际应将edge.props转换为ProtoFieldValue
    // 注释：在实际实现中，这里应该根据edge.props的内容转换为Proto字段值
    // 如果edge.props存在（Some），则遍历其中的字段值并转换
    // 如果edge.props为None，则使用空的字段值列表

    // 创建Link
    let link = Link {
        id,
        lt_id: edge.t.to_string(),
        src_id: src_card_id,
        dest_id: dest_card_id,
        field_values,
    };

    Some(link)
}

/// 将边集合转换为Link集合
fn edges_to_links<'a, T: Transaction<'a>>(
    edges_result: DbResult<Vec<Edge>>,
    direction: EdgeDirection,
    txn: &'a T,
) -> Vec<Link> {
    let mut links = Vec::new();

    if let Ok(edges) = edges_result {
        for edge in edges {
            if let Some(link) = edge_to_link(edge, direction, txn) {
                links.push(link);
            }
        }
    } else {
        error!("Error occurred while querying edges");
    }

    links
}
