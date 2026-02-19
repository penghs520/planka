use crate::database::model::{
    CardState, EdgeDescriptor, EdgeDirection, FieldId, FieldValue, Identifier, NeighborQuery,
    Vertex, VertexId,
};
use crate::database::transaction::Transaction;
use crate::proto::pgraph::{
    common::SortWay,
    query::{
        sort_field::FieldType, Page, Sort, SortAndPage, SortDateField, SortEnumField, SortField,
        SortInnerField, SortLinkField, SortNumberField, SortTextField,
    },
};
use crate::proto::SortBlockState;
use crate::server::helper::get_related_vertices;
use crate::utils::pinyin_utils;
use std::cmp::Ordering;
use std::collections::BinaryHeap;
use std::str::FromStr;
use std::sync::Arc;
use std::time::Instant;
use tracing::{debug, trace, warn};

/// 最大处理的分页数量
const MAX_EFFICIENT_PAGE: usize = 10;

/// 用于排序的顶点和排序键的组合结构
struct VertexWithKeys {
    /// 原始顶点
    vertex: Arc<Vertex>,
    /// 预计算的排序键
    sort_keys: Vec<SortKey>,
}

/// 排序和分页处理器
pub struct SortPageProcessor;

impl SortPageProcessor {
    /// 应用排序和分页
    pub fn sort_and_page<'a, T: Transaction<'a>>(
        vertices: &[Arc<Vertex>],
        sort_and_page: &Option<SortAndPage>,
        txn: &T,
    ) -> Vec<Arc<Vertex>> {
        let start_time = Instant::now();
        debug!(
            "Starting sort and pagination processing, input vertex count: {}",
            vertices.len()
        );

        // 如果没有排序和分页配置，直接返回原始节点
        if sort_and_page.is_none() {
            debug!(
                "No sort and pagination configuration, duration: {:?}",
                start_time.elapsed()
            );
            return vertices.to_vec();
        }

        let sort_and_page = sort_and_page.as_ref().unwrap();

        // 如果有分页，但没有排序，进行简单分页
        if sort_and_page.sorts.is_empty() {
            let mut result = vertices.to_vec();
            if let Some(page) = &sort_and_page.page {
                // 使用 page_size >= 0 判断是否启用分页（page_num 从 0 开始）
                if page.page_size >= 0 {
                    let page_start = Instant::now();
                    Self::apply_pagination(&mut result, page);
                    debug!(
                        "Applied pagination only, duration: {:?}",
                        page_start.elapsed()
                    );
                }
            }
            debug!(
                "No sort configuration, pagination only processing completed, total duration: {:?}",
                start_time.elapsed()
            );
            return result;
        }

        // 有排序条件的情况下
        debug!(
            "Applying sort, sort condition count: {}",
            sort_and_page.sorts.len()
        );

        // 确定使用完全排序还是TopK算法
        if let Some(page) = &sort_and_page.page {
            // 使用 page_size > 0 判断是否启用分页（page_num 从 0 开始）
            if page.page_size > 0 {
                // 是否使用topK算法取决于数据量和请求的页码
                let page_num = page.page_num as usize;
                let page_size = page.page_size as usize;

                // 计算需要的元素数量（page_num 从 0 开始，所以需要 (page_num + 1) * page_size）
                let total_needed = (page_num + 1) * page_size;

                // 如果页码较小（前几页），且数据量足够大，使用topK算法
                if page_num < MAX_EFFICIENT_PAGE && total_needed < vertices.len() {
                    let result = Self::top_k_sort(vertices, &sort_and_page.sorts, page, txn);
                    debug!(
                        "TopK排序完成，输出结果数量: {}, 总耗时: {:?}",
                        result.len(),
                        start_time.elapsed()
                    );
                    return result;
                }

                // 如果页码过大，使用完全排序效果更好
                let sort_start = Instant::now();
                let mut result = Self::full_sort(vertices, &sort_and_page.sorts, txn);
                debug!("完全排序完成，耗时: {:?}", sort_start.elapsed());

                Self::apply_pagination(&mut result, page);

                debug!(
                    "排序分页处理完成，输出结果数量: {}, 总耗时: {:?}",
                    result.len(),
                    start_time.elapsed()
                );
                return result;
            }
        }

        // 没有分页或分页不合法，使用完全排序
        debug!("无有效分页配置，使用完全排序");
        let result = Self::full_sort(vertices, &sort_and_page.sorts, txn);
        debug!(
            "排序处理完成，输出结果数量: {}, 总耗时: {:?}",
            result.len(),
            start_time.elapsed()
        );
        result
    }

    /// 完整排序算法 - 对所有元素进行排序
    fn full_sort<'a, T: Transaction<'a>>(
        vertices: &[Arc<Vertex>],
        sorts: &[Sort],
        txn: &T,
    ) -> Vec<Arc<Vertex>> {
        let start_time = Instant::now();
        debug!("开始完全排序，顶点数量: {}", vertices.len());

        // 性能优化：预处理顶点和排序键
        let prep_start = Instant::now();
        let vertices_with_keys = Self::prepare_vertices_with_keys(vertices, sorts, txn);
        debug!("准备排序数据完成，耗时: {:?}", prep_start.elapsed());

        // 对整个集合进行排序
        let sort_start = Instant::now();
        let mut sorted_vertices_with_keys = vertices_with_keys;
        sorted_vertices_with_keys.sort_by(|a, b| Self::compare_vertices_with_keys(a, b, sorts));
        debug!("排序操作完成，耗时: {:?}", sort_start.elapsed());

        // 提取排序后的顶点
        let result: Vec<Arc<Vertex>> = sorted_vertices_with_keys
            .into_iter()
            .map(|v_with_keys| v_with_keys.vertex)
            .collect();

        debug!("完全排序处理完成，总耗时: {:?}", start_time.elapsed());
        result
    }

    /// TopK排序算法 - 只返回指定页的元素
    fn top_k_sort<'a, T: Transaction<'a>>(
        vertices: &[Arc<Vertex>],
        sorts: &[Sort],
        page: &Page,
        txn: &T,
    ) -> Vec<Arc<Vertex>> {
        // 使用 page_size > 0 判断是否启用分页（page_num 从 0 开始）
        if vertices.is_empty() || page.page_size <= 0 {
            return Vec::new();
        }

        // 计算需要的元素数量（page_num 从 0 开始）
        let k = page.page_size as usize * (page.page_num as usize + 1);

        // 预处理顶点和排序键
        let sort_start = Instant::now();
        let vertices_with_keys = Self::prepare_vertices_with_keys(vertices, sorts, txn);
        debug!("准备排序数据完成，耗时: {:?}", sort_start.elapsed());

        let result = Self::find_top_k(&vertices_with_keys, k, sorts);

        // 应用分页（page_num 从 0 开始）
        let page_start = Instant::now();
        let start = page.page_num as usize * page.page_size as usize;
        let end = std::cmp::min(start + page.page_size as usize, result.len());

        let result = if start < result.len() {
            result[start..end].to_vec()
        } else {
            Vec::new()
        };
        debug!(
            "从TopK结果中提取分页结果完成，结果大小: {}, 耗时: {:?}",
            result.len(),
            page_start.elapsed()
        );

        debug!("TopK排序处理完成，总耗时: {:?}", sort_start.elapsed());
        result
    }

    /// 查找前K个元素
    fn find_top_k(
        vertices_with_keys: &[VertexWithKeys],
        k: usize,
        sorts: &[Sort],
    ) -> Vec<Arc<Vertex>> {
        // 创建最小堆，使用反向比较函数来实现最大堆（保留最小的K个元素）
        let mut heap = BinaryHeap::with_capacity(k + 1);

        // 为了支持反转排序顺序，我们需要反转比较方向
        let mut reversed_sorts = Vec::with_capacity(sorts.len());
        for sort in sorts {
            let mut new_sort = sort.clone();
            // 临时反转排序方向，使topK算法工作
            if new_sort.sort_way == SortWay::Asc as i32 {
                new_sort.sort_way = SortWay::Desc as i32;
            } else {
                new_sort.sort_way = SortWay::Asc as i32;
            }
            reversed_sorts.push(new_sort);
        }

        // 先分析顶点，将空值和非空值分开处理
        let mut non_empty_indices = Vec::new();
        let mut empty_indices = Vec::new();

        for (idx, vertex_with_keys) in vertices_with_keys.iter().enumerate() {
            // 检查是否含有NaN值
            let has_nan = vertex_with_keys.sort_keys.iter().any(|k| match k {
                SortKey::Number(n) => n.is_nan(),
                SortKey::OptionalNumber(Some(n)) => n.is_nan(),
                _ => false,
            });

            // 检查是否全部为空值
            let all_empty = vertex_with_keys.sort_keys.iter().all(|k| match k {
                SortKey::Empty => true,
                SortKey::OptionalNumber(None) => true,
                SortKey::OptionalInteger(None) => true,
                SortKey::OptionalString(None) => true,
                _ => false,
            });

            // 将空值和NaN值顶点索引放到单独的列表中
            if has_nan || all_empty {
                empty_indices.push(idx);
            } else {
                non_empty_indices.push(idx);
            }
        }

        // 处理非空值顶点，使用堆排序找出topK
        let heap_start = Instant::now();
        for &idx in &non_empty_indices {
            let item = HeapItem {
                vertex_index: idx,
                vertex: Arc::clone(&vertices_with_keys[idx].vertex),
                sort_keys: vertices_with_keys[idx].sort_keys.clone(),
            };

            if heap.len() < k {
                // 堆未满，直接添加
                heap.push(VertexComparator {
                    item,
                    sorts: &reversed_sorts,
                });
            } else {
                // 堆已满，比较新元素与堆顶
                if let Some(mut top) = heap.peek_mut() {
                    // 注意：这里使用反向比较方向
                    let order = Self::compare_heap_items(&top.item, &item, &reversed_sorts);
                    if order == Ordering::Less {
                        // 新元素比堆顶小，替换堆顶
                        *top = VertexComparator {
                            item,
                            sorts: &reversed_sorts,
                        };
                    }
                }
            }
        }
        trace!(
            "构建TopK堆完成，堆大小: {}, 耗时: {:?}",
            heap.len(),
            heap_start.elapsed()
        );

        // 从堆中提取结果并进行最终排序
        let mut result_items: Vec<HeapItem> =
            heap.into_iter().map(|comparator| comparator.item).collect();

        // 重新排序以保证正确的顺序（使用原始的排序方向）
        result_items.sort_by(|a, b| Self::compare_heap_items(a, b, sorts));

        // 收集堆中的结果
        let mut result: Vec<Arc<Vertex>> =
            result_items.into_iter().map(|item| item.vertex).collect();

        // 只有当结果不足K个时，才考虑添加空值顶点
        if result.len() < k && !empty_indices.is_empty() {
            // 对空值顶点索引按ID排序以保证稳定性
            let mut empty_vertices_with_ids: Vec<(usize, VertexId)> = empty_indices
                .iter()
                .map(|&idx| (idx, vertices_with_keys[idx].vertex.card_id))
                .collect();

            empty_vertices_with_ids.sort_by_key(|&(_, id)| id);

            // 添加空值顶点直到达到K个或空值顶点用完
            let remaining = k - result.len();
            let to_add = std::cmp::min(remaining, empty_vertices_with_ids.len());

            for i in 0..to_add {
                let idx = empty_vertices_with_ids[i].0;
                result.push(Arc::clone(&vertices_with_keys[idx].vertex));
            }
        }

        result
    }

    /// 准备带排序键的顶点数据
    fn prepare_vertices_with_keys<'a, T: Transaction<'a>>(
        vertices: &[Arc<Vertex>],
        sorts: &[Sort],
        txn: &T,
    ) -> Vec<VertexWithKeys> {
        let start_time = Instant::now();
        let mut result = Vec::with_capacity(vertices.len());

        // 为每个顶点预计算所有排序键
        for vertex in vertices.iter() {
            let mut keys = Vec::with_capacity(sorts.len());
            for sort in sorts {
                if let Some(sort_field) = &sort.sort_field {
                    let key = Self::extract_sort_key(vertex, sort_field, txn);
                    keys.push(key);
                }
            }

            result.push(VertexWithKeys {
                vertex: Arc::clone(vertex),
                sort_keys: keys,
            });
        }

        trace!(
            "准备带排序键的顶点数据完成，数量: {}, 耗时: {:?}",
            result.len(),
            start_time.elapsed()
        );
        result
    }

    /// 比较两个带排序键的顶点
    fn compare_vertices_with_keys(
        v1: &VertexWithKeys,
        v2: &VertexWithKeys,
        sorts: &[Sort],
    ) -> Ordering {
        // 主要比较逻辑：依次比较每个排序键
        for (idx, sort) in sorts.iter().enumerate() {
            if idx >= v1.sort_keys.len() || idx >= v2.sort_keys.len() {
                break; // 防止越界
            }

            let key1 = &v1.sort_keys[idx];
            let key2 = &v2.sort_keys[idx];

            // 空值快速处理（空值始终排在后面）
            let key1_empty = Self::is_empty_key(key1);
            let key2_empty = Self::is_empty_key(key2);

            if key1_empty && !key2_empty {
                return Ordering::Greater;
            } else if !key1_empty && key2_empty {
                return Ordering::Less;
            } else if key1_empty && key2_empty {
                continue; // 两者都为空，继续比较下一个键
            }

            // 非空值正常比较
            let order = key1.compare(key2);
            if order != Ordering::Equal {
                // 应用排序方向
                return if sort.sort_way == SortWay::Desc as i32 {
                    order.reverse()
                } else {
                    order
                };
            }
        }

        // 所有排序字段相等时
        Ordering::Equal
    }

    /// 检查键是否为空
    #[inline]
    fn is_empty_key(key: &SortKey) -> bool {
        match key {
            SortKey::Empty => true,
            SortKey::OptionalNumber(None) => true,
            SortKey::OptionalInteger(None) => true,
            SortKey::OptionalString(None) => true,
            _ => false,
        }
    }

    /// 比较两个堆项
    fn compare_heap_items(item1: &HeapItem, item2: &HeapItem, sorts: &[Sort]) -> Ordering {
        // 主比较逻辑
        for (idx, sort) in sorts.iter().enumerate() {
            if idx >= item1.sort_keys.len() || idx >= item2.sort_keys.len() {
                break;
            }

            let key1 = &item1.sort_keys[idx];
            let key2 = &item2.sort_keys[idx];

            // 空值快速处理
            let key1_empty = Self::is_empty_key(key1);
            let key2_empty = Self::is_empty_key(key2);

            if key1_empty && !key2_empty {
                return Ordering::Greater;
            } else if !key1_empty && key2_empty {
                return Ordering::Less;
            } else if key1_empty && key2_empty {
                continue;
            }

            // 非空值比较
            let order = key1.compare(key2);
            if order != Ordering::Equal {
                return if sort.sort_way == SortWay::Desc as i32 {
                    order.reverse()
                } else {
                    order
                };
            }
        }

        Ordering::Equal
    }

    /// 应用分页（page_num 从 0 开始）
    fn apply_pagination(vertices: &mut Vec<Arc<Vertex>>, page: &Page) {
        let start_time = Instant::now();
        // 使用 page_size > 0 判断是否启用分页
        if page.page_size <= 0 {
            trace!("页大小无效，跳过分页，耗时: {:?}", start_time.elapsed());
            return;
        }

        // page_num 从 0 开始
        let start = page.page_num as usize * page.page_size as usize;
        let end = std::cmp::min(start + page.page_size as usize, vertices.len());
        trace!(
            "计算分页范围: start={}, end={}, 总数={}",
            start,
            end,
            vertices.len()
        );

        if start < vertices.len() {
            // 优化：使用drain替代slice操作，避免克隆
            let new_vertices = vertices.drain(start..end).collect();
            *vertices = new_vertices;
            trace!(
                "应用分页完成，结果大小: {}, 耗时: {:?}",
                vertices.len(),
                start_time.elapsed()
            );
        } else {
            // 如果开始索引超出范围，则返回空结果
            vertices.clear();
            trace!(
                "分页范围超出数据范围，返回空结果，耗时: {:?}",
                start_time.elapsed()
            );
        }
    }

    /// 提取排序键
    fn extract_sort_key<'a, T: Transaction<'a>>(
        vertex: &Arc<Vertex>,
        sort_field: &SortField,
        txn: &T,
    ) -> SortKey {
        if sort_field.field_type.is_none() {
            return SortKey::Empty;
        }

        // 检查是否有排序路径，有路径表示需要对关联卡片的属性排序
        if let Some(path) = &sort_field.path {
            // 获取关联卡片
            if !path.nodes.is_empty() {
                let related_vertices = get_related_vertices(vertex, path, txn);

                // 如果找不到关联节点，返回空值
                if related_vertices.is_empty() {
                    return SortKey::Empty;
                }

                // 取第一个关联节点进行排序（后续可优化为考虑多个关联节点）
                let related_vertex = &related_vertices[0];

                // 对关联节点应用排序字段
                return match &sort_field.field_type {
                    Some(FieldType::InnerField(inner_field)) => {
                        Self::extract_inner_field_key(related_vertex, inner_field)
                    }
                    Some(FieldType::TextField(text_field)) => {
                        Self::extract_text_field_key(related_vertex, text_field)
                    }
                    Some(FieldType::NumberField(number_field)) => {
                        Self::extract_number_field_key(related_vertex, number_field)
                    }
                    Some(FieldType::DateField(date_field)) => {
                        Self::extract_date_field_key(related_vertex, date_field)
                    }
                    Some(FieldType::EnumField(enum_field)) => {
                        Self::extract_enum_field_key(related_vertex, enum_field)
                    }
                    Some(FieldType::LinkField(link_field)) => {
                        Self::extract_link_field_key(related_vertex, link_field, txn)
                    }
                    Some(FieldType::BlockState(sort_block_state)) => {
                        Self::extract_block_state_key(related_vertex, sort_block_state, txn)
                    }
                    None => SortKey::Empty,
                };
            }
        }

        // 无路径，直接处理当前节点
        match &sort_field.field_type {
            Some(FieldType::InnerField(inner_field)) => {
                Self::extract_inner_field_key(vertex, inner_field)
            }
            Some(FieldType::TextField(text_field)) => {
                Self::extract_text_field_key(vertex, text_field)
            }
            Some(FieldType::NumberField(number_field)) => {
                Self::extract_number_field_key(vertex, number_field)
            }
            Some(FieldType::DateField(date_field)) => {
                Self::extract_date_field_key(vertex, date_field)
            }
            Some(FieldType::EnumField(enum_field)) => {
                Self::extract_enum_field_key(vertex, enum_field)
            }
            Some(FieldType::LinkField(link_field)) => {
                Self::extract_link_field_key(vertex, link_field, txn)
            }
            Some(FieldType::BlockState(sort_block_state)) => {
                Self::extract_block_state_key(vertex, sort_block_state, txn)
            }
            None => SortKey::Empty,
        }
    }

    /// 提取标题拼音首字母作为排序键
    fn extract_title_pinyin_key(vertex: &Arc<Vertex>) -> SortKey {
        let title_str = vertex.title.to_string();
        // 获取拼音首字母
        let pinyin_initials = pinyin_utils::to_pinyin_initials(&title_str);
        SortKey::String(pinyin_initials)
    }

    /// 提取内置字段排序键
    fn extract_inner_field_key(vertex: &Arc<Vertex>, field: &SortInnerField) -> SortKey {
        match field.field_id.as_str() {
            //cli 中执行select语句时会传title
            "name" | "title" => Self::extract_title_pinyin_key(vertex),
            "code" => {
                if let Some(code) = &vertex.custom_code {
                    // 按字符串排序
                    return SortKey::String(code.clone());
                }
                // 如果没有自定义编码，则比较组织内编码
                SortKey::Integer(vertex.code_in_org_int as i64)
            }
            //cli 中执行select语句时会传下划线格式，java驱动会传驼峰格式，所以干脆都兼容
            "card_typeId" | "card_type_id" => SortKey::String(vertex.card_type_id.to_string()),
            "state" => {
                let state_value = match vertex.state {
                    CardState::Active => 0,
                    CardState::Discarded => 1,
                    CardState::Archived => 2,
                };
                SortKey::Integer(state_value)
            }
            "created" => SortKey::Integer(vertex.created_at as i64),
            "updated" => SortKey::Integer(vertex.updated_at as i64),
            "position" => SortKey::Integer(vertex.position as i64),
            "containerId" | "container_id" => SortKey::String(vertex.container_id.to_string()),
            "archivedDate" | "archived_date" => {
                SortKey::OptionalInteger(vertex.archived_at.map(|v| v as i64))
            }
            "abandonDate" | "discarded_at" => {
                SortKey::OptionalInteger(vertex.discarded_at.map(|v| v as i64))
            }
            _ => {
                warn!("Unsupported inner field: {}", field.field_id);
                SortKey::Empty
            }
        }
    }

    /// 提取文本字段排序键
    fn extract_text_field_key(vertex: &Arc<Vertex>, field: &SortTextField) -> SortKey {
        if let Some(field_values) = &vertex.field_values {
            if let Some(field_id) = FieldId::from_str(&field.field_id).ok() {
                if let Some(field_value) = field_values.get(&field_id) {
                    if let FieldValue::Text(text_value) = field_value {
                        if text_value.text.is_empty() {
                            return SortKey::OptionalString(None);
                        }
                        return SortKey::OptionalString(Some(text_value.text.clone()));
                    }
                }
            }
        }
        // 如果没有找到字段值，返回OptionalString(None)表示空值
        SortKey::OptionalString(None)
    }

    /// 提取数字字段排序键
    fn extract_number_field_key(vertex: &Arc<Vertex>, field: &SortNumberField) -> SortKey {
        if let Some(field_values) = &vertex.field_values {
            if let Some(field_id) = FieldId::from_str(&field.field_id).ok() {
                if let Some(field_value) = field_values.get(&field_id) {
                    if let FieldValue::Number(number_value) = field_value {
                        // 统一使用浮点数类型，不做整数判断
                        return SortKey::Number(number_value.number);
                    }
                }
            }
        }
        // 如果没有找到字段值，返回OptionalNumber(None)
        SortKey::OptionalNumber(None)
    }

    /// 提取日期字段排序键
    fn extract_date_field_key(vertex: &Arc<Vertex>, field: &SortDateField) -> SortKey {
        if let Some(field_values) = &vertex.field_values {
            if let Some(field_id) = FieldId::from_str(&field.field_id).ok() {
                if let Some(field_value) = field_values.get(&field_id) {
                    if let FieldValue::Date(date_value) = field_value {
                        // 将 u64 类型转换为 i64，处理可能的溢出
                        let timestamp = if date_value.timestamp > i64::MAX as u64 {
                            i64::MAX
                        } else {
                            date_value.timestamp as i64
                        };
                        return SortKey::OptionalInteger(Some(timestamp));
                    }
                }
            }
        }
        // 如果没有找到字段值，返回OptionalInteger(None)表示空值
        SortKey::OptionalInteger(None)
    }

    /// 提取枚举字段排序键
    fn extract_enum_field_key(vertex: &Arc<Vertex>, field: &SortEnumField) -> SortKey {
        let mut enum_value = None;

        if let Some(field_values) = &vertex.field_values {
            if let Some(field_id) = FieldId::from_str(&field.field_id).ok() {
                if let Some(field_value) = field_values.get(&field_id) {
                    if let FieldValue::Enum(enum_value_data) = field_value {
                        if !enum_value_data.items.is_empty() {
                            enum_value = Some(enum_value_data.items[0].to_string());
                        }
                    }
                }
            }
        }

        // 获取枚举项的顺序
        if let Some(value) = enum_value {
            let order = field.enum_item_order_map.get(&value).cloned().unwrap_or(0);
            SortKey::OptionalInteger(Some(order as i64))
        } else {
            // 如果没有找到字段值，返回OptionalInteger(None)表示空值
            SortKey::OptionalInteger(None)
        }
    }

    /// 提取链接字段排序键
    fn extract_link_field_key<'a, T: Transaction<'a>>(
        vertex: &Arc<Vertex>,
        field: &SortLinkField,
        txn: &T,
    ) -> SortKey {
        // 构建邻居查询参数
        let edge_descriptor = EdgeDescriptor {
            t: Identifier::new(&field.lt_id),
            direction: if field.position == 0 {
                EdgeDirection::Src
            } else {
                EdgeDirection::Dest
            },
        };

        let neighbor_query = NeighborQuery {
            src_vertex_ids: vec![vertex.card_id],
            edge_descriptor,
            dest_vertex_states: Some(vec![CardState::Active, CardState::Archived]), // 不过滤顶点状态
        };

        // 查询邻居边
        match txn.query_neighbor_vertices(&neighbor_query) {
            Ok(related_vertices) => {
                if related_vertices.is_empty() {
                    return SortKey::Empty;
                }
                // 获取顶点标题的拼音首字母
                let related_vertex = &related_vertices[0];
                Self::extract_title_pinyin_key(related_vertex)
            }
            Err(e) => {
                warn!("查询邻居边失败: {:?}", e);
                SortKey::Empty
            }
        }
    }

    fn extract_block_state_key<'a, T: Transaction<'a>>(
        vertex: &Arc<Vertex>,
        sort_block_state: &SortBlockState,
        txn: &T,
    ) -> SortKey {
        let edge_descriptor = EdgeDescriptor {
            t: Identifier::new(&sort_block_state.block_lt_id),
            direction: if sort_block_state.position == 0 {
                EdgeDirection::Src
            } else {
                EdgeDirection::Dest
            },
        };

        let neighbor_query = NeighborQuery {
            src_vertex_ids: vec![vertex.card_id],
            edge_descriptor,
            dest_vertex_states: Some(vec![CardState::Active]), // 只取活跃的受阻卡
        };

        match txn.query_neighbor_vertex_ids(&neighbor_query) {
            Ok(edges) => {
                if edges.is_empty() {
                    return SortKey::Integer(0);
                }
                SortKey::Integer(1)
            }
            Err(e) => {
                warn!("查询邻居边失败: {:?}", e);
                SortKey::Empty
            }
        }
    }
}

/// 排序键枚举，用于缓存和比较排序值
#[derive(Debug, Clone)]
enum SortKey {
    Empty,
    String(String),
    Integer(i64),
    Number(f64),
    OptionalNumber(Option<f64>),
    OptionalInteger(Option<i64>),
    OptionalString(Option<String>),
}

impl SortKey {
    /// 比较两个排序键
    fn compare(&self, other: &Self) -> Ordering {
        // 处理空值排序 - 无论排序方向如何，空值都应该排在最后
        match (self, other) {
            // 都是空值的情况，按相等处理
            (SortKey::Empty, SortKey::Empty) => Ordering::Equal,
            (SortKey::OptionalNumber(None), SortKey::OptionalNumber(None)) => Ordering::Equal,
            (SortKey::OptionalInteger(None), SortKey::OptionalInteger(None)) => Ordering::Equal,
            (SortKey::OptionalString(None), SortKey::OptionalString(None)) => Ordering::Equal,

            // 一个是空值，一个不是空值，空值始终排在后面
            (SortKey::Empty, _) => Ordering::Greater,
            (_, SortKey::Empty) => Ordering::Less,
            (SortKey::OptionalNumber(None), _) => Ordering::Greater,
            (_, SortKey::OptionalNumber(None)) => Ordering::Less,
            (SortKey::OptionalInteger(None), _) => Ordering::Greater,
            (_, SortKey::OptionalInteger(None)) => Ordering::Less,
            (SortKey::OptionalString(None), _) => Ordering::Greater,
            (_, SortKey::OptionalString(None)) => Ordering::Less,

            // 非空值同类型比较
            (SortKey::String(s1), SortKey::String(s2)) => s1.cmp(s2),
            (SortKey::Integer(n1), SortKey::Integer(n2)) => n1.cmp(n2),
            (SortKey::OptionalInteger(Some(n1)), SortKey::OptionalInteger(Some(n2))) => n1.cmp(n2),
            (SortKey::OptionalString(Some(s1)), SortKey::OptionalString(Some(s2))) => s1.cmp(s2),

            // 数字类型特殊处理
            (SortKey::Number(n1), SortKey::Number(n2)) => {
                // 处理特殊浮点数值：NaN和无穷大
                if n1.is_nan() && n2.is_nan() {
                    Ordering::Equal
                } else if n1.is_nan() {
                    // NaN 应该排在最后，认为它比所有值都"大"
                    Ordering::Greater
                } else if n2.is_nan() {
                    Ordering::Less
                } else if n1.is_infinite() && n2.is_infinite() {
                    if n1.is_sign_positive() == n2.is_sign_positive() {
                        Ordering::Equal
                    } else if n1.is_sign_positive() {
                        Ordering::Greater // 正无穷 > 负无穷
                    } else {
                        Ordering::Less // 负无穷 < 正无穷
                    }
                } else if n1.is_infinite() {
                    if n1.is_sign_positive() {
                        Ordering::Greater // 正无穷 > 任何有限值
                    } else {
                        Ordering::Less // 负无穷 < 任何有限值
                    }
                } else if n2.is_infinite() {
                    if n2.is_sign_positive() {
                        Ordering::Less // 任何有限值 < 正无穷
                    } else {
                        Ordering::Greater // 任何有限值 > 负无穷
                    }
                } else {
                    // 正常浮点数比较
                    n1.partial_cmp(n2).unwrap_or(Ordering::Equal)
                }
            }
            (SortKey::OptionalNumber(Some(n1)), SortKey::OptionalNumber(Some(n2))) => {
                // 处理特殊浮点数值：NaN和无穷大
                if n1.is_nan() && n2.is_nan() {
                    Ordering::Equal
                } else if n1.is_nan() {
                    // NaN 应该排在最后，认为它比所有值都"大"
                    Ordering::Greater
                } else if n2.is_nan() {
                    Ordering::Less
                } else if n1.is_infinite() && n2.is_infinite() {
                    if n1.is_sign_positive() == n2.is_sign_positive() {
                        Ordering::Equal
                    } else if n1.is_sign_positive() {
                        Ordering::Greater // 正无穷 > 负无穷
                    } else {
                        Ordering::Less // 负无穷 < 正无穷
                    }
                } else if n1.is_infinite() {
                    if n1.is_sign_positive() {
                        Ordering::Greater // 正无穷 > 任何有限值
                    } else {
                        Ordering::Less // 负无穷 < 任何有限值
                    }
                } else if n2.is_infinite() {
                    if n2.is_sign_positive() {
                        Ordering::Less // 任何有限值 < 正无穷
                    } else {
                        Ordering::Greater // 任何有限值 > 负无穷
                    }
                } else {
                    // 正常浮点数比较
                    n1.partial_cmp(n2).unwrap_or(Ordering::Equal)
                }
            }

            // 不同类型的键不做比较，预期不会来到这里，直接返回相等
            _ => {
                warn!("无法进行不同类型的键比较: {:?}, {:?}", self, other);
                Ordering::Equal
            }
        }
    }
}

/// 堆排序用的顶点项
struct HeapItem {
    vertex_index: usize,
    vertex: Arc<Vertex>,
    sort_keys: Vec<SortKey>,
}

/// 用于BinaryHeap比较的包装器
struct VertexComparator<'a> {
    item: HeapItem,
    sorts: &'a [Sort],
}

impl<'a> Eq for VertexComparator<'a> {}

impl<'a> PartialEq for VertexComparator<'a> {
    fn eq(&self, other: &Self) -> bool {
        // 实际上我们只需要实现这个以满足Ord trait的要求
        // 由于顶点ID唯一，所以这里简单地比较索引
        self.item.vertex_index == other.item.vertex_index
    }
}

impl<'a> Ord for VertexComparator<'a> {
    fn cmp(&self, other: &Self) -> Ordering {
        // 注意：这里翻转了比较顺序，使BinaryHeap成为最小堆
        SortPageProcessor::compare_heap_items(&other.item, &self.item, self.sorts)
    }
}

impl<'a> PartialOrd for VertexComparator<'a> {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}
