use crate::database::errors::DbError;
use crate::database::model::{ EdgeDescriptor, EdgeDirection, EdgeType, NeighborQuery, Vertex
};
use crate::database::{database::Database, errors::DbResult, transaction::Transaction};
use crate::proto::pgraph::{
    common,
    query,
    query::{
        CardCountByGroupRequest, CardCountByGroupResponse, CardQueryRequest, CardQueryResponse,
        QueryCardTitlesRequest, QueryCardTitlesResponse,
    },
};
use crate::proto::{
    CardCountRequest, CardCountResponse, Condition, QueryContext, QueryScope,
};
use crate::server::card_converter::{convert_to_cards, vertex_title_to_proto_title};
use crate::server::filter::{apply_filter_conditions, FilterContext};
use crate::server::helper::resolve_condition;
use crate::server::SortPageProcessor;
use std::collections::{HashMap};
use std::str::FromStr;
use std::sync::Arc;
use tracing::{debug};

/// 执行查询并应用过滤条件的公共函数
fn execute_query_with_filter<'a, T: Transaction<'a>>(
    query_scope: &QueryScope,
    query_context: &Option<QueryContext>,
    condition: &Option<Condition>,
    txn: &T,
) -> DbResult<(Vec<Arc<Vertex>>, bool)> {
    let filter_context = build_filter_context(query_context, txn);
    
    let (vertex_query, resolved_condition, return_empty_directly) = 
        resolve_condition(query_scope, &filter_context, condition, txn);
    
    if return_empty_directly {
        return Ok((vec![], true));
    }
    
    debug!("vertex query: {:?}", vertex_query);
    
    let vertices = txn.query_vertices(vertex_query)?;

    // 应用高级过滤条件
    let filtered_vertices = 
        apply_filter_conditions(&vertices, &filter_context, &resolved_condition, txn);
    
    Ok((filtered_vertices, false))
}

/// 查询卡片
pub fn query_cards<D: Database>(query: CardQueryRequest, db: &D) -> DbResult<CardQueryResponse> {
    let txn = db.transaction();

    let (filtered_vertices, return_empty_directly) = execute_query_with_filter(
        &query.query_scope.unwrap(),
        &query.query_context,
        &query.condition,
        &txn,
    )?;

    if return_empty_directly {
        return Ok(CardQueryResponse {
            cards: vec![],
            count: 0,
            total: 0,
        });
    }

    let total = filtered_vertices.len() as u32;

    // 应用排序和分页
    let paginated_vertices =
        apply_sort_and_pagination(&filtered_vertices, &query.sort_and_page, &txn);

    // 转换节点为卡片
    let cards = convert_to_cards(&paginated_vertices, &txn, query.r#yield.as_ref());

    // 构建并返回结果
    Ok(CardQueryResponse {
        cards,
        count: paginated_vertices.len() as u32,
        total,
    })
}

/// 应用排序和分页
fn apply_sort_and_pagination<'a, T: Transaction<'a>>(
    vertices: &[Arc<Vertex>],
    sort_and_page: &Option<query::SortAndPage>,
    txn: &T,
) -> Vec<Arc<Vertex>> {
    // 使用 SortPageProcessor 实现排序和分页
    SortPageProcessor::sort_and_page(vertices, sort_and_page, txn)
}

/// 查询卡片数量
pub fn count_cards<D: Database>(query: CardCountRequest, db: &D) -> DbResult<CardCountResponse> {
    if query.query_scope.is_none() {
        return Err(DbError::ValidationError(
            crate::database::errors::ValidationError::EmptyInput(
                "query_scope cannot be null".to_string(),
            ),
        ));
    }
    
    let txn = db.transaction();

    let (filtered_vertices, return_empty_directly) = execute_query_with_filter(
        &query.query_scope.unwrap(),
        &query.query_context,
        &query.condition,
        &txn,
    )?;

    if return_empty_directly {
        return Ok(CardCountResponse { count: 0 });
    }

    let count = filtered_vertices.len() as u32;

    // 构建并返回结果
    Ok(CardCountResponse { count })
}

/// 按分组查询卡片数量
pub fn count_cards_by_group<D: Database>(
    query: CardCountByGroupRequest,
    db: &D,
) -> DbResult<CardCountByGroupResponse> {
    let txn = db.transaction();

    if query.group_ids.is_empty() {
        return Ok(CardCountByGroupResponse {
            counts: Default::default(),
        });
    }

    // card_id 就是 vertex_id，将字符串解析为 u64 并存储到 HashSet
    let group_ids_set: std::collections::HashSet<u64> = query
        .group_ids
        .iter()
        .filter_map(|s| s.parse::<u64>().ok())
        .collect();

    if query.query_scope.is_none() {
        return Err(DbError::ValidationError(
            crate::database::errors::ValidationError::EmptyInput(
                "query_scope cannot be null".to_string(),
            ),
        ));
    }

    let (filtered_vertices, return_empty_directly) = execute_query_with_filter(
        &query.query_scope.unwrap(),
        &query.query_context,
        &query.condition,
        &txn,
    )?;

    if return_empty_directly || filtered_vertices.is_empty() {
        return Ok(CardCountByGroupResponse {
            counts: HashMap::new(),
        });
    }

    // 从分组方式获取关联类型ID和方向
    if query.group_by.is_none() {
        return Err(DbError::ValidationError(
            crate::database::errors::ValidationError::EmptyInput(
                "group_by cannot be null".to_string(),
            ),
        ));
    }

    let group_by = query.group_by.unwrap();
    let lt_id = &group_by.lt_id;
    let direction = match &group_by.position() {
        common::LinkPosition::Src => EdgeDirection::Src,
        common::LinkPosition::Dest => EdgeDirection::Dest,
    };

    // 创建计数结果的HashMap
    let mut counts = HashMap::new();

    // 对每个顶点，查询其指定类型和方向的关联卡片
    for vertex in filtered_vertices {
        let vertex_id = vertex.card_id;

        // 构建邻居查询条件
        let neighbor_query = NeighborQuery {
            src_vertex_ids: vec![vertex_id],
            edge_descriptor: EdgeDescriptor {
                t: EdgeType::from_str(&lt_id).unwrap(),
                direction,
            },
            dest_vertex_states: None, // 默认只返回活跃和归档的节点
        };

        // 查询邻居顶点
        let neighbor_vertex_ids = txn.query_neighbor_vertex_ids(&neighbor_query)?;

        // 对于每个邻居顶点，如果其ID在group_ids中，则增加计数
        for neighbor_vertex_id in neighbor_vertex_ids {
           if group_ids_set.contains(&neighbor_vertex_id) {
               let count = counts.entry(neighbor_vertex_id.to_string()).or_insert(0);
               *count += 1;
           }
        }
    }

    // 返回按分组计数的结果
    Ok(CardCountByGroupResponse { counts })
}

/// 查询卡片IDs
pub fn query_card_ids<D: Database>(
    query: query::CardIdQueryRequest,
    db: &D,
) -> DbResult<query::QueryIdsResponse> {
    if query.query_scope.is_none() {
        return Err(DbError::ValidationError(
            crate::database::errors::ValidationError::EmptyInput(
                "query_scope cannot be null".to_string(),
            ),
        ));
    }
    
    let txn = db.transaction();

    let (filtered_vertices, return_empty_directly) = execute_query_with_filter(
        &query.query_scope.unwrap(),
        &query.query_context,
        &query.condition,
        &txn,
    )?;

    if return_empty_directly {
        return Ok(query::QueryIdsResponse { ids: vec![] });
    }

    // 提取卡片ID（card_id 现在是 u64）
    let ids: Vec<u64> = filtered_vertices
        .iter()
        .map(|vertex| vertex.card_id)
        .collect();

    // 构建并返回结果
    Ok(query::QueryIdsResponse { ids })
}

fn build_filter_context<'a, T: Transaction<'a>>(
    query_context: &Option<QueryContext>,
    txn: &T,
) -> Option<FilterContext> {
    if query_context.is_none() {
        return None;
    }
    let mut filter_context = FilterContext {
        member_id: None,
        parameters: Default::default(),
    };
    let query_context = query_context.as_ref().unwrap();

    if let Some(member_id_str) = &query_context.member_id {
        // 将 member_id 字符串解析为 u64
        if let Ok(member_id) = member_id_str.parse::<u64>() {
            filter_context.member_id = Some(member_id);
        }
    }

    let mut parameters = HashMap::new();
    for (parameter_card_type_id, card_id_str) in &query_context.parameters {
        // 将 card_id 字符串解析为 u64
        if let Ok(card_id) = card_id_str.parse::<u64>() {
            parameters.insert(parameter_card_type_id.to_string(), card_id);
        }
    }
    filter_context.parameters = parameters;
    Some(filter_context)
}

/// 查询卡片标题
///
/// 轻量级查询接口，只返回卡片ID和标题映射，用于审计日志、下拉框等只需要显示名称的场景
pub fn query_card_titles<D: Database>(
    request: QueryCardTitlesRequest,
    db: &D,
) -> DbResult<QueryCardTitlesResponse> {
    let txn = db.transaction();

    // 将card_ids字符串解析为u64
    let card_ids: Vec<u64> = request
        .card_ids
        .iter()
        .filter_map(|s| s.parse::<u64>().ok())
        .collect();

    if card_ids.is_empty() {
        return Ok(QueryCardTitlesResponse {
            titles: HashMap::new(),
        });
    }

    // 批量查询顶点
    let vertices = txn.get_specific_vertices(&card_ids)?;

    // 构建标题映射
    let mut titles = HashMap::new();
    for vertex in vertices {
        let card_id_str = vertex.card_id.to_string();
        let title = vertex_title_to_proto_title(&vertex.title);
        titles.insert(card_id_str, title);
    }

    Ok(QueryCardTitlesResponse { titles })
}
