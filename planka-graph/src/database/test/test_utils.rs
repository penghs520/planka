use crate::database::model::{
    CardId, CardState, CardTypeId, ContainerId, DateValue, Description, EnumItemId, EnumValue,
    FieldId, FieldValue, NumberValue, OrgId, StatusId, StepId, StreamId, StreamInfo, TextValue,
    Vertex, VertexId, VertexTitle,
};
use crate::database::rdb::rdb::RocksDatabase;
use crate::database::rdb::rdb_config::RocksDbConfig;
use std::collections::HashMap;
use std::fs;
use std::sync::atomic::{AtomicU32, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};

// 用于生成唯一的测试数据库路径
static TEST_COUNTER: AtomicU32 = AtomicU32::new(0);

/// 生成唯一的测试数据库路径
pub fn get_test_db_path() -> String {
    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs();
    let counter = TEST_COUNTER.fetch_add(1, Ordering::SeqCst);
    let path = format!("/tmp/rdb_test_{}_{}", timestamp, counter);

    // 确保目录不存在
    fs::remove_dir_all(&path).ok();
    path
}

/// 测试夹具，负责创建和清理数据库
pub struct TestDb {
    pub path: String,
    pub db: RocksDatabase,
}

impl TestDb {
    pub fn new() -> Self {
        let path = get_test_db_path();
        let db = RocksDatabase::new_db_with_config(RocksDbConfig::new(path.clone()));
        TestDb { path, db }
    }
}

impl Drop for TestDb {
    fn drop(&mut self) {
        // 测试结束后清理数据库文件
        fs::remove_dir_all(&self.path).ok();
    }
}

/// 创建一个用于测试的节点
pub fn new_test_vertex(card_id: CardId) -> Vertex {
    Vertex {
        card_id,
        org_id: OrgId::new("default_org_id"),
        card_type_id: CardTypeId::new("default_card_type_id"),
        container_id: ContainerId::new("default_container_id"),
        stream_info: StreamInfo {
            stream_id: StreamId::new("default_stream_id"),
            status_id: StatusId::new("default_status_id"),
        },
        state: CardState::Active,
        title: VertexTitle::PureTitle("default_name".to_string()),
        code_in_org: "1".to_string(),
        code_in_org_int: 1,
        custom_code: Some("default_code".to_string()),
        position: 1,
        created_at: 1_694_000_000_000, // Example timestamp for September 2023
        updated_at: 1_694_000_000_000, // Example timestamp for September 2023
        archived_at: Some(1_694_000_000_000), // Example timestamp for September 2023
        discarded_at: Some(1_694_000_000_000), // Example timestamp for September 2023
        discard_reason: Some("default_reason".to_string()),
        restore_reason: Some("default_reason".to_string()),
        field_values: {
            let mut map = HashMap::new();
            map.insert(
                FieldId::new("text_field_id"),
                FieldValue::Text(TextValue {
                    text: "default_text".to_string(),
                }),
            );
            map.insert(
                FieldId::new("number_field_id"),
                FieldValue::Number(NumberValue { number: 42.0 }),
            );
            map.insert(
                FieldId::new("date_field_id"),
                FieldValue::Date(DateValue {
                    timestamp: 1_694_000_000_000, // Example timestamp for September 2023
                }),
            );
            map.insert(
                FieldId::new("enum_field_id"),
                FieldValue::Enum(EnumValue {
                    items: vec![
                        EnumItemId::new("优先级高"),
                        EnumItemId::new("优先级中"),
                    ],
                }),
            );
            Some(map)
        },
        desc: Description {
            content: Some("测试描述".to_string()),
            changed: true,
        },
    }
}

/// 创建测试用节点，指定更多参数
pub fn create_test_vertex(
    card_id: u64,
    card_type_id: &str,
    title: &str,
    state: CardState,
) -> Vertex {
    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs();
    let code_in_org_int = card_id as u32;
    Vertex {
        card_id,
        org_id: OrgId::new("org1"),
        card_type_id: CardTypeId::new(card_type_id),
        container_id: ContainerId::new("container1"),
        stream_info: StreamInfo {
            stream_id: StreamId::new("stream1"),
            status_id: StatusId::new("status1"),
        },
        state,
        title: VertexTitle::PureTitle(title.to_string()),
        code_in_org: code_in_org_int.to_string(),
        code_in_org_int,
        custom_code: None,
        desc: Description {
            content: Some("测试描述".to_string()),
            changed: true,
        },
        position: 0,
        created_at: now,
        updated_at: now,
        archived_at: None,
        discarded_at: None,
        discard_reason: None,
        restore_reason: None,
        field_values: {
            let mut map = HashMap::new();
            let field_id = FieldId::new("field1");
            map.insert(
                field_id,
                FieldValue::Text(TextValue {
                    text: "测试字段值".to_string(),
                }),
            );
            Some(map)
        },

    }
}

/// 字段值设置项，用于构建测试节点的字段值
pub enum FieldValueSetting {
    /// 文本类型字段值
    Text { field_id: String, text: String },
    /// 数值类型字段值
    Number { field_id: String, number: f64 },
    /// 日期类型字段值
    Date { field_id: String, timestamp: u64 },
    /// 枚举类型字段值
    Enum {
        field_id: String,
        items: Vec<String>,
    },
}

/// 创建包含指定字段值的测试节点
///
/// 这个函数可以更灵活地创建带有自定义字段值的测试节点。
/// 你可以通过提供一个FieldValueSetting枚举的列表来添加或更新多个字段值。
///
/// # 示例
///
/// ```rust
pub fn create_vertex_with_fields(
    card_id: u64,
    card_type_id: &str,
    title: &str,
    state: CardState,
    field_settings: Vec<FieldValueSetting>,
) -> Vertex {
    let mut vertex = create_test_vertex(card_id, card_type_id, title, state);

    // 如果没有设置字段值，则不做任何修改
    if field_settings.is_empty() {
        return vertex;
    }

    // 确保field_values不为None
    let field_values = vertex.field_values.get_or_insert_with(HashMap::new);

    // 根据设置添加或更新字段值
    for setting in field_settings {
        match setting {
            FieldValueSetting::Text { field_id, text } => {
                let field_id_obj = FieldId::new(&field_id);
                // 直接插入或替换值
                field_values.insert(field_id_obj, FieldValue::Text(TextValue { text }));
            }
            FieldValueSetting::Number { field_id, number } => {
                let field_id_obj = FieldId::new(&field_id);
                // 直接插入或替换值
                field_values.insert(field_id_obj, FieldValue::Number(NumberValue { number }));
            }
            FieldValueSetting::Date {
                field_id,
                timestamp,
            } => {
                let field_id_obj = FieldId::new(&field_id);
                // 直接插入或替换值
                field_values.insert(field_id_obj, FieldValue::Date(DateValue { timestamp }));
            }
            FieldValueSetting::Enum { field_id, items } => {
                let field_id_obj = FieldId::new(&field_id);
                let enum_items = items
                    .iter()
                    .map(|item| EnumItemId::new(item))
                    .collect();
                // 直接插入或替换值
                field_values.insert(
                    field_id_obj,
                    FieldValue::Enum(EnumValue { items: enum_items }),
                );
            }
        }
    }

    vertex
}
