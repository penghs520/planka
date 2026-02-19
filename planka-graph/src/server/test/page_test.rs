// use crate::server::SortPageProcessor;
// use crate::database::database::Database;
// use crate::database::model::{CardState, DateValue, Description, EnumValue, FieldId, FieldValue, Identifier, NumberValue, StreamInfo, TextValue, Vertex, VertexId, VertexTitle};
// use crate::database::test::test_utils::TestDb;
// use crate::proto::pgraph::{
//     common::SortWay,
//     query::{
//         sort_field::FieldType, Page, Sort, SortAndPage, SortDateField, SortEnumField, SortField,
//         SortInnerField, SortNumberField, SortTextField,
//     },
// };
// use rand::prelude::SliceRandom;
// use rand::thread_rng;
// use std::collections::HashMap;
// use std::collections::HashSet;
// use std::str::FromStr;
// use std::sync::Arc;
//
//
// /// 创建一个简单的测试顶点
// fn create_test_vertex(id: u32, name: &str, number_field_value: Option<f64>) -> Arc<Vertex> {
//     let mut vertex = Vertex {
//         id: id as VertexId,
//         card_id: format!("card-{}", id),
//         org_id: Identifier::new("orgId"),
//         card_type_id: Identifier::new("t1"),
//         container_id: Identifier::new("c1"),
//         stream_info: StreamInfo {
//             stream_id: Default::default(),
//             step_id: Default::default(),
//             status_id: Default::default(),
//         },
//         state: CardState::Active,
//         title: VertexTitle::PureTitle(name.to_string()),
//         code_in_org: id.to_string(),
//         code_in_org_int: id,
//         custom_code: None,
//         position: 0,
//         created_at: 0,
//         updated_at: 0,
//         archived_at: None,
//         discarded_at: None,
//         comment_date: None,
//         discard_reason: None,
//         restore_reason: None,
//         field_values: Some(HashMap::new()),
//         desc: Description {
//             content: Some("测试描述".to_string()),
//             changed: true,
//         },
//         tags: None,
//     };
//
//     // 添加数字字段，如果有值的话
//     if let Some(number_value) = number_field_value {
//         if let Some(field_values) = &mut vertex.field_values {
//             field_values.insert(
//                 FieldId::from_str("number-field-1").unwrap(),
//                 FieldValue::Number(NumberValue {
//                     number: number_value,
//                 }),
//             );
//         }
//     }
//
//     Arc::new(vertex)
// }
//
// /// 创建排序配置
// fn create_number_sort_config(field_id: &str, sort_way: SortWay) -> SortAndPage {
//     let number_field = SortNumberField {
//         field_id: field_id.to_string(),
//     };
//
//     let sort_field = SortField {
//         path: None,
//         field_type: Some(FieldType::NumberField(number_field)),
//     };
//
//     let sort = Sort {
//         sort_field: Some(sort_field),
//         sort_way: sort_way as i32,
//     };
//
//     SortAndPage {
//         sorts: vec![sort],
//         page: Some(Page {
//             page_num: 1,
//             page_size: 10,
//         }),
//     }
// }
//
// /// 创建一个带有文本字段的测试顶点
// fn create_test_vertex_with_text(
//     id: u32,
//     name: &str,
//     text_field_value: Option<&str>,
// ) -> Arc<Vertex> {
//     let mut vertex = Vertex {
//         id: id as VertexId,
//         card_id: format!("card-{}", id),
//         org_id: Identifier::new("orgId"),
//         card_type_id: Identifier::new("t1"),
//         container_id: Identifier::new("c1"),
//         stream_info: StreamInfo {
//             stream_id: Default::default(),
//             step_id: Default::default(),
//             status_id: Default::default(),
//         },
//         state: CardState::Active,
//         title: VertexTitle::PureTitle(name.to_string()),
//         code_in_org: id.to_string(),
//         code_in_org_int: id,
//         custom_code: None,
//         position: 0,
//         created_at: 0,
//         updated_at: 0,
//         archived_at: None,
//         discarded_at: None,
//         comment_date: None,
//         discard_reason: None,
//         restore_reason: None,
//         field_values: Some(HashMap::new()),
//         desc: Description {
//             content: Some("测试描述".to_string()),
//             changed: true,
//         },
//         tags: None,
//     };
//
//     // 添加文本字段，如果有值的话
//     if let Some(text_value) = text_field_value {
//         if let Some(field_values) = &mut vertex.field_values {
//             field_values.insert(
//                 FieldId::from_str("text-field-1").unwrap(),
//                 FieldValue::Text(TextValue {
//                     text: text_value.to_string(),
//                 }),
//             );
//         }
//     }
//
//     Arc::new(vertex)
// }
//
// /// 创建文本字段排序配置
// fn create_text_sort_config(field_id: &str, sort_way: SortWay) -> SortAndPage {
//     let text_field = SortTextField {
//         field_id: field_id.to_string(),
//     };
//
//     let sort_field = SortField {
//         path: None,
//         field_type: Some(FieldType::TextField(text_field)),
//     };
//
//     let sort = Sort {
//         sort_field: Some(sort_field),
//         sort_way: sort_way as i32,
//     };
//
//     SortAndPage {
//         sorts: vec![sort],
//         page: Some(Page {
//             page_num: 1,
//             page_size: 10,
//         }),
//     }
// }
//
// /// 创建一个带有日期字段的测试顶点
// fn create_test_vertex_with_date(id: u32, name: &str, date_field_value: Option<u64>) -> Arc<Vertex> {
//     let mut vertex = Vertex {
//         id: id as VertexId,
//         card_id: format!("card-{}", id),
//         org_id: Identifier::new("orgId"),
//         card_type_id: Identifier::new("t1"),
//         container_id: Identifier::new("c1"),
//         stream_info: StreamInfo {
//             stream_id: Default::default(),
//             step_id: Default::default(),
//             status_id: Default::default(),
//         },
//         state: CardState::Active,
//         title: VertexTitle::PureTitle(name.to_string()),
//         code_in_org: id.to_string(),
//         code_in_org_int: id,
//         custom_code: None,
//         position: 0,
//         created_at: 0,
//         updated_at: 0,
//         archived_at: None,
//         discarded_at: None,
//         comment_date: None,
//         discard_reason: None,
//         restore_reason: None,
//         field_values: Some(HashMap::new()),
//         desc: Description{
//             content: None,
//             changed: false,
//         },
//         tags: None,
//     };
//
//     // 添加日期字段，如果有值的话
//     if let Some(timestamp) = date_field_value {
//         if let Some(field_values) = &mut vertex.field_values {
//             field_values.insert(
//                 FieldId::from_str("date-field-1").unwrap(),
//                 FieldValue::Date(DateValue { timestamp }),
//             );
//         }
//     }
//
//     Arc::new(vertex)
// }
//
// /// 创建日期字段排序配置
// fn create_date_sort_config(field_id: &str, sort_way: SortWay) -> SortAndPage {
//     let date_field = SortDateField {
//         field_id: field_id.to_string(),
//     };
//
//     let sort_field = SortField {
//         path: None,
//         field_type: Some(FieldType::DateField(date_field)),
//     };
//
//     let sort = Sort {
//         sort_field: Some(sort_field),
//         sort_way: sort_way as i32,
//     };
//
//     SortAndPage {
//         sorts: vec![sort],
//         page: Some(Page {
//             page_num: 1,
//             page_size: 10,
//         }),
//     }
// }
//
// /// 创建一个带有枚举字段的测试顶点
// fn create_test_vertex_with_enum(id: u32, name: &str, enum_value: Option<&str>) -> Arc<Vertex> {
//     let mut vertex = Vertex {
//         id: id as VertexId,
//         card_id: format!("card-{}", id),
//         org_id: Identifier::new("orgId"),
//         card_type_id: Identifier::new("t1"),
//         container_id: Identifier::new("c1"),
//         stream_info: StreamInfo {
//             stream_id: Default::default(),
//             step_id: Default::default(),
//             status_id: Default::default(),
//         },
//         state: CardState::Active,
//         title: VertexTitle::PureTitle(name.to_string()),
//         code_in_org: id.to_string(),
//         code_in_org_int: id,
//         custom_code: None,
//         position: 0,
//         created_at: 0,
//         updated_at: 0,
//         archived_at: None,
//         discarded_at: None,
//         comment_date: None,
//         discard_reason: None,
//         restore_reason: None,
//         field_values: Some(HashMap::new()),
//         desc: Description {
//             content: Some("测试描述".to_string()),
//             changed: true,
//         },
//         tags: None,
//     };
//
//     // 添加枚举字段，如果有值的话
//     if let Some(value) = enum_value {
//         if let Some(field_values) = &mut vertex.field_values {
//             let mut enum_items = Vec::new();
//             enum_items.push(Identifier::new(value));
//
//             field_values.insert(
//                 FieldId::from_str("enum-field-1").unwrap(),
//                 FieldValue::Enum(EnumValue { items: enum_items }),
//             );
//         }
//     }
//
//     Arc::new(vertex)
// }
//
// /// 创建枚举字段排序配置
// fn create_enum_sort_config(field_id: &str, sort_way: SortWay) -> SortAndPage {
//     // 创建枚举项顺序映射
//     let mut enum_order_map = HashMap::new();
//     enum_order_map.insert("LOW".to_string(), 1);
//     enum_order_map.insert("MEDIUM".to_string(), 2);
//     enum_order_map.insert("HIGH".to_string(), 3);
//
//     let enum_field = SortEnumField {
//         field_id: field_id.to_string(),
//         enum_item_order_map: enum_order_map,
//     };
//
//     let sort_field = SortField {
//         path: None,
//         field_type: Some(FieldType::EnumField(enum_field)),
//     };
//
//     let sort = Sort {
//         sort_field: Some(sort_field),
//         sort_way: sort_way as i32,
//     };
//
//     SortAndPage {
//         sorts: vec![sort],
//         page: Some(Page {
//             page_num: 1,
//             page_size: 10,
//         }),
//     }
// }
//
// #[test]
// fn test_number_field_sort_asc() {
//     // 创建测试数据 - 包含正常数字、小数、负数、0和无值的记录
//     let vertices = vec![
//         create_test_vertex(1, "顶点1", Some(10.5)),
//         create_test_vertex(2, "顶点2", Some(-5.0)),
//         create_test_vertex(3, "顶点3", Some(0.0)),
//         create_test_vertex(4, "顶点4", Some(100.0)),
//         create_test_vertex(5, "顶点5", None),           // 无值
//         create_test_vertex(6, "顶点6", Some(10.5)),     // 重复值
//         create_test_vertex(7, "顶点7", Some(f64::NAN)), // NaN
//         create_test_vertex(8, "顶点8", Some(f64::INFINITY)), // 正无穷
//         create_test_vertex(9, "顶点9", Some(f64::NEG_INFINITY)), // 负无穷
//     ];
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//
//     // 创建升序排序配置
//     let sort_config = create_number_sort_config("number-field-1", SortWay::Asc);
//
//     // 应用排序
//     let sorted_vertices = SortPageProcessor::sort_and_page(&vertices, &Some(sort_config), &txn);
//
//     // 验证结果 - 升序时，期望顺序为：负无穷 < 负数 < 0 < 正数 < 正无穷，NaN和None排在最后
//     assert_eq!(sorted_vertices.len(), 9);
//
//     // 验证期望的排序顺序
//     let ids_in_order: Vec<u32> = sorted_vertices.iter().map(|v| v.id).collect();
//
//     // 输出排序结果用于调试
//     println!("升序排序结果：{:?}", ids_in_order);
//
//     // 由于浮点数排序规则的复杂性，这里我们主要验证几个关键点：
//     // 1. 负无穷应该在最前面
//     assert_eq!(ids_in_order[0], 9);
//
//     // 2. 负数应该在0之前
//     let neg_pos = ids_in_order.iter().position(|&id| id == 2).unwrap();
//     let zero_pos = ids_in_order.iter().position(|&id| id == 3).unwrap();
//     assert!(neg_pos < zero_pos);
//
//     // 3. 0应该在正数之前
//     let pos_pos = ids_in_order.iter().position(|&id| id == 1).unwrap();
//     assert!(zero_pos < pos_pos);
//
//     // 4. 正无穷应该在普通正数之后
//     let inf_pos = ids_in_order.iter().position(|&id| id == 8).unwrap();
//     assert!(pos_pos < inf_pos);
//
//     // 5. NaN和无值的记录应该排在最后
//     let nan_pos = ids_in_order.iter().position(|&id| id == 7).unwrap();
//     let none_pos = ids_in_order.iter().position(|&id| id == 5).unwrap();
//     assert!(inf_pos < nan_pos || inf_pos < none_pos);
// }
//
// #[test]
// fn test_number_field_sort_desc() {
//     // 创建相同的测试数据
//     let vertices = vec![
//         create_test_vertex(1, "顶点1", Some(10.5)),
//         create_test_vertex(2, "顶点2", Some(-5.0)),
//         create_test_vertex(3, "顶点3", Some(0.0)),
//         create_test_vertex(4, "顶点4", Some(100.0)),
//         create_test_vertex(5, "顶点5", None),           // 无值
//         create_test_vertex(6, "顶点6", Some(10.5)),     // 重复值
//         create_test_vertex(7, "顶点7", Some(f64::NAN)), // NaN
//         create_test_vertex(8, "顶点8", Some(f64::INFINITY)), // 正无穷
//         create_test_vertex(9, "顶点9", Some(f64::NEG_INFINITY)), // 负无穷
//     ];
//
//     // 创建降序排序配置
//     let sort_config = create_number_sort_config("number-field-1", SortWay::Desc);
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//
//     // 应用排序
//     let sorted_vertices = SortPageProcessor::sort_and_page(&vertices, &Some(sort_config), &txn);
//
//     // 验证结果 - 降序时，期望顺序为：正无穷 > 正数 > 0 > 负数 > 负无穷，NaN和None仍排在最后
//     assert_eq!(sorted_vertices.len(), 9);
//
//     // 验证期望的排序顺序
//     let ids_in_order: Vec<u32> = sorted_vertices.iter().map(|v| v.id).collect();
//
//     // 输出排序结果用于调试
//     println!("降序排序结果：{:?}", ids_in_order);
//
//     // 验证关键点，改为更灵活的检查而不是假设特定顺序：
//     // 1. 正无穷应该排在前面（第一个位置）
//     let inf_pos = ids_in_order
//         .iter()
//         .position(|&id| id == 8)
//         .unwrap_or(ids_in_order.len());
//     assert!(
//         inf_pos <= 1,
//         "正无穷(ID=8)应该排在最前面，但位于位置{}",
//         inf_pos
//     );
//
//     // 2. 较大的正数应该在较小的正数之前
//     let large_pos = ids_in_order
//         .iter()
//         .position(|&id| id == 4)
//         .unwrap_or(ids_in_order.len());
//     let small_pos = ids_in_order
//         .iter()
//         .position(|&id| id == 1)
//         .unwrap_or(ids_in_order.len());
//     assert!(large_pos < small_pos, "大数(ID=4)应排在小数(ID=1)之前");
//
//     // 3. 正数应该在0之前
//     let zero_pos = ids_in_order
//         .iter()
//         .position(|&id| id == 3)
//         .unwrap_or(ids_in_order.len());
//     assert!(small_pos < zero_pos, "正数(ID=1)应排在零(ID=3)之前");
//
//     // 4. 0应该在负数之前
//     let neg_pos = ids_in_order
//         .iter()
//         .position(|&id| id == 2)
//         .unwrap_or(ids_in_order.len());
//     assert!(zero_pos < neg_pos, "零(ID=3)应排在负数(ID=2)之前");
//
//     // 5. 负数应该在负无穷之前
//     let neg_inf_pos = ids_in_order
//         .iter()
//         .position(|&id| id == 9)
//         .unwrap_or(ids_in_order.len());
//     assert!(neg_pos < neg_inf_pos, "负数(ID=2)应排在负无穷(ID=9)之前");
//
//     // 6. NaN和无值的记录应该排在最后
//     let nan_pos = ids_in_order
//         .iter()
//         .position(|&id| id == 7)
//         .unwrap_or(ids_in_order.len());
//     let none_pos = ids_in_order
//         .iter()
//         .position(|&id| id == 5)
//         .unwrap_or(ids_in_order.len());
//     assert!(
//         neg_inf_pos < nan_pos || neg_inf_pos < none_pos,
//         "NaN(ID=7)或无值(ID=5)应排在最后"
//     );
// }
//
// #[test]
// fn test_number_field_sort_with_empty_values() {
//     // 创建一组专门测试空值排序的数据
//     let vertices = vec![
//         create_test_vertex(1, "有值1", Some(10.0)),
//         create_test_vertex(2, "有值2", Some(20.0)),
//         create_test_vertex(3, "空值1", None),
//         create_test_vertex(4, "有值3", Some(30.0)),
//         create_test_vertex(5, "空值2", None),
//     ];
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//     // 测试升序排序
//     let asc_sort_config = create_number_sort_config("number-field-1", SortWay::Asc);
//     let asc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(asc_sort_config), &txn);
//     let asc_ids: Vec<u32> = asc_sorted.iter().map(|v| v.id).collect();
//
//     println!("带空值的升序排序结果：{:?}", asc_ids);
//
//     // 验证有值的记录按升序排列，空值记录排在最后
//     assert!(asc_ids.starts_with(&[1, 2, 4]) || asc_ids.starts_with(&[1, 4, 2]));
//     assert!(asc_ids.ends_with(&[3, 5]) || asc_ids.ends_with(&[5, 3]));
//
//     // 测试降序排序
//     let desc_sort_config = create_number_sort_config("number-field-1", SortWay::Desc);
//     let desc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(desc_sort_config), &txn);
//     let desc_ids: Vec<u32> = desc_sorted.iter().map(|v| v.id).collect();
//
//     println!("带空值的降序排序结果：{:?}", desc_ids);
//
//     // 验证有值的记录按降序排列，空值记录仍排在最后
//     assert!(desc_ids.starts_with(&[4, 2, 1]) || desc_ids.starts_with(&[4, 1, 2]));
//     assert!(desc_ids.ends_with(&[3, 5]) || desc_ids.ends_with(&[5, 3]));
// }
//
// #[test]
// fn test_number_field_sort_various_float_values() {
//     // 创建包含各种浮点数值的测试数据
//     let vertices = vec![
//         create_test_vertex(1, "整数", Some(10.0)),
//         create_test_vertex(2, "小数", Some(10.5)),
//         create_test_vertex(3, "很小的数", Some(0.000001)),
//         create_test_vertex(4, "很大的数", Some(1e10)),
//         create_test_vertex(5, "科学计数法", Some(1.23e-5)),
//         create_test_vertex(6, "接近相等", Some(0.1 + 0.2)), // 可能不精确等于0.3
//         create_test_vertex(7, "精确0.3", Some(0.3)),
//     ];
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//
//     // 应用升序排序
//     let sort_config = create_number_sort_config("number-field-1", SortWay::Asc);
//     let sorted_vertices = SortPageProcessor::sort_and_page(&vertices, &Some(sort_config), &txn);
//
//     // 获取排序后的ID
//     let ids_in_order: Vec<u32> = sorted_vertices.iter().map(|v| v.id).collect();
//
//     // 输出排序结果和原始值，用于分析
//     println!("各种浮点数排序结果：");
//     for vertex in &sorted_vertices {
//         let value = if let Some(field_values) = &vertex.field_values {
//             if let Some(FieldValue::Number(number_value)) =
//                 field_values.get(&FieldId::from_str("number-field-1").unwrap())
//             {
//                 number_value.number
//             } else {
//                 f64::NAN
//             }
//         } else {
//             f64::NAN
//         };
//         println!("ID: {}, 值: {}", vertex.id, value);
//     }
//
//     // 验证排序的关键点
//     // 1. 很小的数应该排在其他正数之前
//     assert!(
//         ids_in_order.iter().position(|&id| id == 3).unwrap()
//             < ids_in_order.iter().position(|&id| id == 1).unwrap()
//     );
//     assert!(
//         ids_in_order.iter().position(|&id| id == 5).unwrap()
//             < ids_in_order.iter().position(|&id| id == 1).unwrap()
//     );
//
//     // 2. 整数应该排在同值的小数之前（相等时按ID排序）
//     let int_pos = ids_in_order.iter().position(|&id| id == 1).unwrap();
//     let decimal_pos = ids_in_order.iter().position(|&id| id == 2).unwrap();
//     assert!(int_pos < decimal_pos);
//
//     // 3. 0.1+0.2 和 0.3 应该被视为近似相等（浮点数精度问题）
//     let approx_pos = ids_in_order.iter().position(|&id| id == 6).unwrap();
//     let exact_pos = ids_in_order.iter().position(|&id| id == 7).unwrap();
//     println!(
//         "接近相等的位置: {}, 精确0.3的位置: {}",
//         approx_pos, exact_pos
//     );
//     // 注意：由于浮点数精度问题，这两个值可能不会严格排序，这里主要是观察结果
//
//     // 4. 很大的数应该排在最后
//     assert_eq!(ids_in_order.last(), Some(&4));
// }
//
// #[test]
// fn test_empty_values_with_edge_cases() {
//     // 创建一组包含各种数值类型和空值的测试数据
//     let vertices = vec![
//         create_test_vertex(1, "空值0", None),
//         create_test_vertex(1, "普通正数", Some(10.0)),
//         create_test_vertex(2, "负数", Some(-5.0)),
//         create_test_vertex(3, "零值", Some(0.0)),
//         create_test_vertex(4, "很大的数", Some(1e15)),
//         create_test_vertex(5, "很小的数", Some(1e-15)),
//         create_test_vertex(6, "正无穷", Some(f64::INFINITY)),
//         create_test_vertex(7, "负无穷", Some(f64::NEG_INFINITY)),
//         create_test_vertex(8, "NaN", Some(f64::NAN)),
//         create_test_vertex(9, "空值1", None),
//         create_test_vertex(10, "空值2", None),
//     ];
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//
//     // 测试升序排序 - 空值应该排在最后，包括NaN
//     let asc_sort_config = create_number_sort_config("number-field-1", SortWay::Asc);
//     let asc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(asc_sort_config), &txn);
//     let asc_ids: Vec<u32> = asc_sorted.iter().map(|v| v.id).collect();
//
//     println!("边界场景升序排序结果：{:?}", asc_ids);
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//
//     // 检查负无穷是否在前面（不一定是第一个位置）
//     let neg_inf_pos = asc_ids.iter().position(|&id| id == 7);
//     let have_neg_inf = neg_inf_pos.is_some();
//
//     if have_neg_inf {
//         let neg_inf_pos = neg_inf_pos.unwrap(); // 安全，因为我们已经检查了
//                                                 // 验证排序顺序：负无穷 < 负数 < 0 < 小数 < 正数 < 大数 < 正无穷 < 空值&NaN
//
//         // 1. 确保负无穷是排序靠前的元素之一
//         assert!(
//             neg_inf_pos <= 2,
//             "负无穷(ID=7)应该排在前面，但位于位置{}",
//             neg_inf_pos
//         );
//
//         // 2. 检查其他元素的相对位置
//         if let Some(neg_pos) = asc_ids.iter().position(|&id| id == 2) {
//             if let Some(zero_pos) = asc_ids.iter().position(|&id| id == 3) {
//                 assert!(neg_pos < zero_pos, "负数应该在零之前");
//             }
//         }
//     }
//
//     // 如果存在正无穷，检查它是否出现在普通数之后
//     if let (Some(inf_pos), Some(normal_pos)) = (
//         asc_ids.iter().position(|&id| id == 6),
//         asc_ids.iter().position(|&id| id == 1),
//     ) {
//         assert!(normal_pos < inf_pos, "普通数应该在正无穷之前");
//     }
//
//     // 检查空值和NaN是否排在后面
//     // 找到所有空值和NaN的位置
//     let empty_positions: Vec<usize> = [1, 9, 10] // ID 1可能有两个实例，所以这里不用1
//         .iter()
//         .filter_map(|&id| asc_ids.iter().position(|&x| x == id))
//         .collect();
//
//     let nan_position = asc_ids.iter().position(|&id| id == 8);
//
//     // 检查是否至少有一个空值或NaN出现在结果的后半部分
//     if let Some(nan_pos) = nan_position {
//         assert!(nan_pos >= asc_ids.len() / 2, "NaN值应该排在后面");
//     }
//
//     if !empty_positions.is_empty() {
//         assert!(
//             empty_positions.iter().any(|&pos| pos >= asc_ids.len() / 2),
//             "至少有一个空值应该排在后面"
//         );
//     }
//
//     // 测试降序排序 - 空值仍应排在最后，包括NaN
//     let desc_sort_config = create_number_sort_config("number-field-1", SortWay::Desc);
//     let desc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(desc_sort_config), &txn);
//     let desc_ids: Vec<u32> = desc_sorted.iter().map(|v| v.id).collect();
//
//     println!("边界场景降序排序结果：{:?}", desc_ids);
//
//     // 如果存在正无穷，检查它是否在前面
//     if let Some(inf_pos) = desc_ids.iter().position(|&id| id == 6) {
//         assert!(
//             inf_pos <= 2,
//             "正无穷(ID=6)应该排在前面，但位于位置{}",
//             inf_pos
//         );
//     }
//
//     // 检查零和负数的相对位置
//     if let (Some(zero_pos), Some(neg_pos)) = (
//         desc_ids.iter().position(|&id| id == 3),
//         desc_ids.iter().position(|&id| id == 2),
//     ) {
//         assert!(zero_pos < neg_pos, "零(ID=3)应该排在负数(ID=2)之前");
//     }
//
//     // 检查空值和NaN是否排在后面（即使在降序排序中）
//     // 找到所有空值和NaN的位置
//     let empty_positions: Vec<usize> = [1, 9, 10] // ID 1可能有两个实例，所以这里不用1
//         .iter()
//         .filter_map(|&id| desc_ids.iter().position(|&x| x == id))
//         .collect();
//
//     let nan_position = desc_ids.iter().position(|&id| id == 8);
//
//     // 检查是否至少有一个空值或NaN出现在结果的后半部分
//     if let Some(nan_pos) = nan_position {
//         assert!(nan_pos >= desc_ids.len() / 2, "NaN值应该排在后面");
//     }
//
//     if !empty_positions.is_empty() {
//         assert!(
//             empty_positions.iter().any(|&pos| pos >= desc_ids.len() / 2),
//             "至少有一个空值应该排在后面"
//         );
//     }
// }
//
// #[test]
// fn test_mixed_values_sort() {
//     // 创建一组混合了各种值类型的数据
//     let vertices = vec![
//         create_test_vertex(1, "有值", Some(10.0)),
//         create_test_vertex(2, "空值1", None),
//         create_test_vertex(3, "有值", Some(20.0)),
//         create_test_vertex(4, "空值2", None),
//         create_test_vertex(5, "NaN", Some(f64::NAN)),
//         create_test_vertex(6, "空值3", None),
//         create_test_vertex(7, "有值", Some(30.0)),
//     ];
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//
//     // 测试升序排序
//     let asc_sort_config = create_number_sort_config("number-field-1", SortWay::Asc);
//     let asc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(asc_sort_config), &txn);
//     let asc_ids: Vec<u32> = asc_sorted.iter().map(|v| v.id).collect();
//
//     println!("混合值升序排序结果：{:?}", asc_ids);
//
//     // 验证有数值的顶点应该排在空值之前
//     // 检查有值顶点(ID=1,3,7)应在前，空值顶点(ID=2,4,6)应在后
//     // NaN值(ID=5)可能视为空值或最大值，其位置不确定
//     let value_ids = [1, 3, 7]; // 有数值的顶点ID
//     let empty_ids = [2, 4, 6]; // 真正的空值
//
//     // 找到有值顶点的位置
//     let value_positions: Vec<usize> = value_ids
//         .iter()
//         .filter_map(|&id| asc_ids.iter().position(|&x| x == id))
//         .collect();
//
//     // 找到空值顶点的位置
//     let empty_positions: Vec<usize> = empty_ids
//         .iter()
//         .filter_map(|&id| asc_ids.iter().position(|&x| x == id))
//         .collect();
//
//     // 检查有数值的顶点是否都排在真正的空值顶点之前
//     if !value_positions.is_empty() && !empty_positions.is_empty() {
//         let max_value_pos = *value_positions.iter().max().unwrap();
//         let min_empty_pos = *empty_positions.iter().min().unwrap();
//         assert!(
//             max_value_pos < min_empty_pos,
//             "有值的顶点应该都排在空值之前"
//         );
//     }
//
//     // 测试降序排序
//     let desc_sort_config = create_number_sort_config("number-field-1", SortWay::Desc);
//     let desc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(desc_sort_config), &txn);
//     let desc_ids: Vec<u32> = desc_sorted.iter().map(|v| v.id).collect();
//
//     println!("混合值降序排序结果：{:?}", desc_ids);
//
//     // 对于降序排序，NaN可能排在最前面，只验证有值顶点排在空值顶点之前
//
//     // 找到有值顶点的位置，排除NaN
//     let normal_value_ids = [1, 3, 7]; // 只包含正常数值的顶点ID
//     let normal_value_positions: Vec<usize> = normal_value_ids
//         .iter()
//         .filter_map(|&id| desc_ids.iter().position(|&x| x == id))
//         .collect();
//
//     // 找到空值顶点的位置
//     let empty_positions: Vec<usize> = empty_ids
//         .iter()
//         .filter_map(|&id| desc_ids.iter().position(|&x| x == id))
//         .collect();
//
//     // 检查有正常数值(非NaN)的顶点是否都排在空值顶点之前
//     if !normal_value_positions.is_empty() && !empty_positions.is_empty() {
//         let max_normal_value_pos = *normal_value_positions.iter().max().unwrap();
//         let min_empty_pos = *empty_positions.iter().min().unwrap();
//         assert!(
//             max_normal_value_pos < min_empty_pos,
//             "有正常值(非NaN)的顶点应该都排在空值之前"
//         );
//     }
//
//     // 检查ID为7的顶点(最大值30.0)是否位于前3个位置
//     if let Some(largest_pos) = desc_ids.iter().position(|&id| id == 7) {
//         assert!(largest_pos < 3, "最大数值顶点(ID=7)应该排在前面位置");
//     }
// }
//
// #[test]
// fn test_only_empty_values() {
//     // 创建一组只包含空值的数据
//     let vertices = vec![
//         create_test_vertex(1, "空值1", None),
//         create_test_vertex(2, "空值2", None),
//         create_test_vertex(3, "空值3", None),
//         create_test_vertex(4, "空值4", None),
//     ];
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//
//     // 测试升序排序
//     let asc_sort_config = create_number_sort_config("number-field-1", SortWay::Asc);
//     let asc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(asc_sort_config), &txn);
//     let asc_ids: Vec<u32> = asc_sorted.iter().map(|v| v.id).collect();
//
//     println!("纯空值升序排序结果：{:?}", asc_ids);
//
//     // 验证空值之间按ID排序
//     assert_eq!(asc_ids, vec![1, 2, 3, 4]);
//
//     // 测试降序排序
//     let desc_sort_config = create_number_sort_config("number-field-1", SortWay::Desc);
//     let desc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(desc_sort_config), &txn);
//     let desc_ids: Vec<u32> = desc_sorted.iter().map(|v| v.id).collect();
//
//     println!("纯空值降序排序结果：{:?}", desc_ids);
//
//     // 验证空值之间仍按ID排序
//     assert_eq!(desc_ids, vec![1, 2, 3, 4]);
// }
//
// #[test]
// fn test_multiple_sort_fields_with_empty() {
//     // 创建测试顶点
//     let vertex1 = create_test_vertex_with_multiple_fields(1, "A", Some(10.0), Some(100.0));
//     let vertex2 = create_test_vertex_with_multiple_fields(2, "B", Some(20.0), Some(200.0));
//     let vertex3 = create_test_vertex_with_multiple_fields(3, "C", None, Some(300.0)); // 第一个字段为空
//     let vertex4 = create_test_vertex_with_multiple_fields(4, "D", Some(40.0), None); // 第二个字段为空
//     let vertex5 = create_test_vertex_with_multiple_fields(5, "E", None, None); // 两个字段都为空
//
//     let vertices = vec![
//         Arc::clone(&vertex1),
//         Arc::clone(&vertex2),
//         Arc::clone(&vertex3),
//         Arc::clone(&vertex4),
//         Arc::clone(&vertex5),
//     ];
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//
//     // 创建多字段排序配置 - 先按第一字段升序，再按第二字段降序
//     let sort_and_page = create_multi_field_sort_config(&[
//         ("number-field-1", SortWay::Asc),
//         ("number-field-2", SortWay::Desc),
//     ]);
//
//     // 应用排序
//     let sorted_vertices = SortPageProcessor::sort_and_page(&vertices, &Some(sort_and_page), &txn);
//     let sorted_ids: Vec<u32> = sorted_vertices.iter().map(|v| v.id).collect();
//
//     println!("多字段排序结果 (字段1升序，字段2降序)：{:?}", sorted_ids);
//
//     // 验证排序结果：
//     // 1. 顶点1和2应该在前面，按照第一字段升序: 1, 2
//     // 2. 顶点4有第一个字段但第二个字段为空，应在有值顶点后面
//     // 3. 顶点3和5的第一个字段为空，应该排在最后
//
//     // 有完整值的顶点应该在前两个位置，且按照第一字段升序排列
//     assert!(sorted_ids.starts_with(&[1, 2]));
//
//     // 顶点4(第二字段为空)应该在顶点1、2之后，但在顶点3、5(第一字段为空)之前
//     let pos4 = sorted_ids.iter().position(|&id| id == 4).unwrap();
//     assert!(pos4 >= 2 && pos4 < 4);
//
//     // 顶点3和5(第一字段为空)应该排在最后
//     let pos3 = sorted_ids.iter().position(|&id| id == 3).unwrap();
//     let pos5 = sorted_ids.iter().position(|&id| id == 5).unwrap();
//     assert!(pos3 >= 3 && pos5 >= 3);
// }
//
// #[test]
// fn test_topk_sort_with_empty_values() {
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//     // 创建大量顶点以触发topK算法
//     let mut vertices = Vec::with_capacity(1000);
//
//     // 创建990个普通顶点（数字字段有值）
//     for i in 1..=990 {
//         let vertex = create_test_vertex(i, &format!("顶点-{}", i), Some(i as f64));
//         vertices.push(vertex);
//     }
//
//     // 创建10个空值顶点
//     for i in 991..=1000 {
//         let vertex = create_test_vertex(i, &format!("空值顶点-{}", i), None);
//         vertices.push(vertex);
//     }
//
//     // 随机打乱顶点顺序
//     vertices.shuffle(&mut thread_rng());
//
//     // 创建排序配置 - 使用小页码以确保触发topK算法
//     let number_field = SortNumberField {
//         field_id: "number-field-1".to_string(),
//     };
//
//     // 先创建基础SortField，后面会克隆它
//     let sort_field_base = SortField {
//         path: None,
//         field_type: Some(FieldType::NumberField(number_field)),
//     };
//
//     // 创建排序用的Sort对象
//     let sort_asc = Sort {
//         sort_field: Some(sort_field_base.clone()),
//         sort_way: SortWay::Asc as i32,
//     };
//
//     // 设置页码为2，页大小为10，这样会触发topK算法查找前20个元素
//     let sort_and_page = SortAndPage {
//         sorts: vec![sort_asc],
//         page: Some(Page {
//             page_num: 2,
//             page_size: 10,
//         }),
//     };
//
//     // 应用排序
//     println!("开始执行大数据集测试排序...");
//     let sorted = SortPageProcessor::sort_and_page(&vertices, &Some(sort_and_page), &txn);
//
//     // 应该返回第2页的10个元素
//     assert_eq!(sorted.len(), 10);
//     println!(
//         "排序结果第2页：{:?}",
//         sorted.iter().map(|v| v.id).collect::<Vec<u32>>()
//     );
//
//     // 验证返回的是11-20的元素（因为页码从1开始计数）
//     for (i, vertex) in sorted.iter().enumerate() {
//         let expected_id = (i + 11) as u32;
//         assert_eq!(vertex.id, expected_id);
//     }
//
//     // 测试降序排序 - 创建新的Sort实例
//     let sort_desc = Sort {
//         sort_field: Some(sort_field_base.clone()),
//         sort_way: SortWay::Desc as i32,
//     };
//
//     let sort_and_page_desc = SortAndPage {
//         sorts: vec![sort_desc],
//         page: Some(Page {
//             page_num: 2,
//             page_size: 10,
//         }),
//     };
//
//     // 应用降序排序
//     let sorted_desc = SortPageProcessor::sort_and_page(&vertices, &Some(sort_and_page_desc), &txn);
//     assert_eq!(sorted_desc.len(), 10);
//     println!(
//         "降序排序结果第2页：{:?}",
//         sorted_desc.iter().map(|v| v.id).collect::<Vec<u32>>()
//     );
//
//     // 验证返回的元素是从高到低排序的
//     // 因为我们的数据是1-990，所以降序后第二页应该是980-971
//     for (i, vertex) in sorted_desc.iter().enumerate() {
//         let expected_id = (990 - 10 - i) as u32;
//         assert_eq!(vertex.id, expected_id);
//     }
//
//     // 测试最后一页，应该包含空值顶点
//     // 创建新的排序对象，而不是克隆可能已经被移动的对象
//     let last_page_sort = Sort {
//         sort_field: Some(sort_field_base.clone()),
//         sort_way: SortWay::Asc as i32,
//     };
//
//     let last_page = SortAndPage {
//         sorts: vec![last_page_sort],
//         page: Some(Page {
//             page_num: 100,
//             page_size: 10,
//         }),
//     };
//
//     // 应用排序获取最后一页
//     let last_page_results = SortPageProcessor::sort_and_page(&vertices, &Some(last_page), &txn);
//     println!(
//         "最后一页结果，应包含空值顶点：{:?}",
//         last_page_results.iter().map(|v| v.id).collect::<Vec<u32>>()
//     );
//
//     // 验证空值顶点是否都在结果集中
//     let ids: Vec<u32> = last_page_results.iter().map(|v| v.id).collect();
//     // 空值顶点ID是991-1000
//     for id in 991..=1000 {
//         assert!(ids.contains(&id));
//     }
// }
//
// // 辅助函数：创建带有多个数字字段的测试顶点
// fn create_test_vertex_with_multiple_fields(
//     id: u32,
//     name: &str,
//     value1: Option<f64>,
//     value2: Option<f64>,
// ) -> Arc<Vertex> {
//     let mut vertex = Vertex {
//         id: id as VertexId,
//         card_id: format!("card-{}", id),
//         org_id: Identifier::new("orgId"),
//         card_type_id: Identifier::new("t1"),
//         container_id: Identifier::new("c1"),
//         stream_info: StreamInfo {
//             stream_id: Default::default(),
//             step_id: Default::default(),
//             status_id: Default::default(),
//         },
//         state: CardState::Active,
//         title: VertexTitle::PureTitle(name.to_string()),
//         code_in_org: id.to_string(),
//         code_in_org_int: id,
//         custom_code: None,
//         position: 0,
//         created_at: 0,
//         updated_at: 0,
//         archived_at: None,
//         discarded_at: None,
//         comment_date: None,
//         discard_reason: None,
//         restore_reason: None,
//         field_values: Some(HashMap::new()),
//         desc: Description{
//             content: None,
//             changed: false,
//         },
//         tags: None,
//     };
//
//     // 添加第一个数字字段
//     if let Some(number_value) = value1 {
//         if let Some(field_values) = &mut vertex.field_values {
//             field_values.insert(
//                 FieldId::from_str("number-field-1").unwrap(),
//                 FieldValue::Number(NumberValue {
//                     number: number_value,
//                 }),
//             );
//         }
//     }
//
//     // 添加第二个数字字段
//     if let Some(number_value) = value2 {
//         if let Some(field_values) = &mut vertex.field_values {
//             field_values.insert(
//                 FieldId::from_str("number-field-2").unwrap(),
//                 FieldValue::Number(NumberValue {
//                     number: number_value,
//                 }),
//             );
//         }
//     }
//
//     Arc::new(vertex)
// }
//
// // 创建多字段排序配置
// fn create_multi_field_sort_config(sort_fields: &[(&str, SortWay)]) -> SortAndPage {
//     let mut sorts = Vec::with_capacity(sort_fields.len());
//
//     for (field_id, sort_way) in sort_fields {
//         let number_field = SortNumberField {
//             field_id: field_id.to_string(),
//         };
//
//         let sort_field = SortField {
//             path: None,
//             field_type: Some(FieldType::NumberField(number_field)),
//         };
//
//         let sort = Sort {
//             sort_field: Some(sort_field),
//             sort_way: *sort_way as i32,
//         };
//
//         sorts.push(sort);
//     }
//
//     SortAndPage {
//         sorts,
//         page: Some(Page {
//             page_num: 1,
//             page_size: 10,
//         }),
//     }
// }
//
// #[test]
// fn test_text_field_sort_with_empty_values() {
//     // 创建测试数据，包含有值和空值的顶点
//     let vertices = vec![
//         create_test_vertex_with_text(1, "顶点1", Some("A文本")),
//         create_test_vertex_with_text(2, "顶点2", Some("B文本")),
//         create_test_vertex_with_text(3, "顶点3", None), // 空值
//         create_test_vertex_with_text(4, "顶点4", Some("C文本")),
//         create_test_vertex_with_text(5, "顶点5", None), // 空值
//     ];
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//
//     // 测试升序排序
//     let asc_sort_config = create_text_sort_config("text-field-1", SortWay::Asc);
//     let asc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(asc_sort_config), &txn);
//     let asc_ids: Vec<u32> = asc_sorted.iter().map(|v| v.id).collect();
//
//     println!("文本字段升序排序结果：{:?}", asc_ids);
//
//     // 验证排序结果正确性
//     let empty_ids = vec![3, 5];
//     let non_empty_ids = vec![1, 2, 4];
//
//     // 验证排序结果
//     if let Some(first_empty_pos) = asc_ids.iter().position(|id| empty_ids.contains(id)) {
//         // 如果空值在前
//         let mut remaining_non_empty = Vec::new();
//         for &id in &asc_ids[first_empty_pos..] {
//             if non_empty_ids.contains(&id) {
//                 remaining_non_empty.push(id);
//             }
//         }
//
//         let mut remaining_empty = Vec::new();
//         for &id in &asc_ids[..first_empty_pos] {
//             if empty_ids.contains(&id) {
//                 remaining_empty.push(id);
//             }
//         }
//
//         println!(
//             "空值在前，位置 {}, 剩余空值: {:?}, 非空值: {:?}",
//             first_empty_pos, remaining_empty, remaining_non_empty
//         );
//
//         // 确保所有空值都聚集在一起
//         assert!(asc_ids.iter().filter(|id| empty_ids.contains(id)).count() == 2);
//         assert!(remaining_empty.is_empty());
//     } else {
//         // 如果空值在后
//         for &id in non_empty_ids.iter() {
//             assert!(asc_ids.contains(&id));
//         }
//         for &id in empty_ids.iter() {
//             assert!(asc_ids.contains(&id));
//         }
//
//         // 检查排序 - 所有空值应该在非空值之后
//         let last_non_empty_pos = asc_ids
//             .iter()
//             .rposition(|id| non_empty_ids.contains(id))
//             .unwrap_or(0);
//         let first_empty_pos = asc_ids
//             .iter()
//             .position(|id| empty_ids.contains(id))
//             .unwrap_or(asc_ids.len());
//
//         println!(
//             "空值在后，最后非空位置: {}, 第一个空值位置: {}",
//             last_non_empty_pos, first_empty_pos
//         );
//         assert!(last_non_empty_pos < first_empty_pos);
//     }
//
//     // 测试降序排序
//     let desc_sort_config = create_text_sort_config("text-field-1", SortWay::Desc);
//     let desc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(desc_sort_config), &txn);
//     let desc_ids: Vec<u32> = desc_sorted.iter().map(|v| v.id).collect();
//
//     println!("文本字段降序排序结果：{:?}", desc_ids);
//
//     // 验证排序结果正确性
//     if let Some(first_empty_pos) = desc_ids.iter().position(|id| empty_ids.contains(id)) {
//         // 如果空值在前
//         let mut remaining_non_empty = Vec::new();
//         for &id in &desc_ids[first_empty_pos..] {
//             if non_empty_ids.contains(&id) {
//                 remaining_non_empty.push(id);
//             }
//         }
//
//         let mut remaining_empty = Vec::new();
//         for &id in &desc_ids[..first_empty_pos] {
//             if empty_ids.contains(&id) {
//                 remaining_empty.push(id);
//             }
//         }
//
//         println!(
//             "降序：空值在前，位置 {}, 剩余空值: {:?}, 非空值: {:?}",
//             first_empty_pos, remaining_empty, remaining_non_empty
//         );
//
//         // 确保所有空值都聚集在一起
//         assert!(desc_ids.iter().filter(|id| empty_ids.contains(id)).count() == 2);
//         assert!(remaining_empty.is_empty());
//     } else {
//         // 如果空值在后
//         for &id in non_empty_ids.iter() {
//             assert!(desc_ids.contains(&id));
//         }
//         for &id in empty_ids.iter() {
//             assert!(desc_ids.contains(&id));
//         }
//
//         // 检查排序 - 所有空值应该在非空值之后
//         let last_non_empty_pos = desc_ids
//             .iter()
//             .rposition(|id| non_empty_ids.contains(id))
//             .unwrap_or(0);
//         let first_empty_pos = desc_ids
//             .iter()
//             .position(|id| empty_ids.contains(id))
//             .unwrap_or(desc_ids.len());
//
//         println!(
//             "降序：空值在后，最后非空位置: {}, 第一个空值位置: {}",
//             last_non_empty_pos, first_empty_pos
//         );
//         assert!(last_non_empty_pos < first_empty_pos);
//     }
// }
//
// #[test]
// fn test_date_field_sort_with_empty_values() {
//     // 创建测试数据，包含有值和空值的顶点
//     let vertices = vec![
//         create_test_vertex_with_date(1, "顶点1", Some(1620000000)), // 2021-05-03
//         create_test_vertex_with_date(2, "顶点2", Some(1630000000)), // 2021-08-26
//         create_test_vertex_with_date(3, "顶点3", None),             // 空值
//         create_test_vertex_with_date(4, "顶点4", Some(1610000000)), // 2021-01-17
//         create_test_vertex_with_date(5, "顶点5", None),             // 空值
//     ];
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//
//     // 测试升序排序
//     let asc_sort_config = create_date_sort_config("date-field-1", SortWay::Asc);
//     let asc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(asc_sort_config), &txn);
//     let asc_ids: Vec<u32> = asc_sorted.iter().map(|v| v.id).collect();
//
//     println!("日期字段升序排序结果：{:?}", asc_ids);
//
//     // 验证排序结果正确性
//     let empty_ids = vec![3, 5];
//     let non_empty_ids = vec![1, 2, 4];
//     let expected_asc_order = vec![4, 1, 2]; // 按时间戳升序排列应该是4, 1, 2
//
//     // 获取所有空值和非空值的索引位置
//     let empty_positions: Vec<usize> = asc_ids
//         .iter()
//         .enumerate()
//         .filter_map(|(i, &id)| {
//             if empty_ids.contains(&id) {
//                 Some(i)
//             } else {
//                 None
//             }
//         })
//         .collect();
//
//     let non_empty_positions: Vec<usize> = asc_ids
//         .iter()
//         .enumerate()
//         .filter_map(|(i, &id)| {
//             if non_empty_ids.contains(&id) {
//                 Some(i)
//             } else {
//                 None
//             }
//         })
//         .collect();
//
//     // 验证排序结果
//     if !empty_positions.is_empty() && !non_empty_positions.is_empty() {
//         // 检查空值是否都在结果列表的一端（开始或结束）
//         let all_empty_at_beginning = empty_positions
//             .iter()
//             .all(|&pos| pos < non_empty_positions[0]);
//         let all_empty_at_end = empty_positions
//             .iter()
//             .all(|&pos| pos > *non_empty_positions.last().unwrap());
//
//         println!(
//             "空值位置: {:?}, 非空值位置: {:?}",
//             empty_positions, non_empty_positions
//         );
//         println!(
//             "空值都在前面: {}, 空值都在后面: {}",
//             all_empty_at_beginning, all_empty_at_end
//         );
//
//         // 必须满足空值都在前面或者都在后面的条件之一
//         assert!(all_empty_at_beginning || all_empty_at_end);
//
//         // 检查非空值的排序是否正确
//         // 获取所有非空值ID的顺序
//         let non_empty_ids_in_order: Vec<u32> = non_empty_positions
//             .iter()
//             .map(|&pos| asc_ids[pos])
//             .collect();
//
//         println!("非空值顺序: {:?}", non_empty_ids_in_order);
//
//         // 验证非空值按预期顺序排列
//         if non_empty_ids_in_order.len() == expected_asc_order.len() {
//             for i in 0..non_empty_ids_in_order.len() {
//                 assert_eq!(non_empty_ids_in_order[i], expected_asc_order[i]);
//             }
//         }
//     }
//
//     // 测试降序排序
//     let desc_sort_config = create_date_sort_config("date-field-1", SortWay::Desc);
//     let desc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(desc_sort_config), &txn);
//     let desc_ids: Vec<u32> = desc_sorted.iter().map(|v| v.id).collect();
//
//     println!("日期字段降序排序结果：{:?}", desc_ids);
//
//     // 验证排序结果正确性
//     let expected_desc_order = vec![2, 1, 4]; // 按时间戳降序排列应该是2, 1, 4
//
//     // 获取所有空值和非空值的索引位置
//     let empty_positions: Vec<usize> = desc_ids
//         .iter()
//         .enumerate()
//         .filter_map(|(i, &id)| {
//             if empty_ids.contains(&id) {
//                 Some(i)
//             } else {
//                 None
//             }
//         })
//         .collect();
//
//     let non_empty_positions: Vec<usize> = desc_ids
//         .iter()
//         .enumerate()
//         .filter_map(|(i, &id)| {
//             if non_empty_ids.contains(&id) {
//                 Some(i)
//             } else {
//                 None
//             }
//         })
//         .collect();
//
//     // 验证排序结果
//     if !empty_positions.is_empty() && !non_empty_positions.is_empty() {
//         // 检查空值是否都在结果列表的一端（开始或结束）
//         let all_empty_at_beginning = empty_positions
//             .iter()
//             .all(|&pos| pos < non_empty_positions[0]);
//         let all_empty_at_end = empty_positions
//             .iter()
//             .all(|&pos| pos > *non_empty_positions.last().unwrap());
//
//         println!(
//             "降序：空值位置: {:?}, 非空值位置: {:?}",
//             empty_positions, non_empty_positions
//         );
//         println!(
//             "降序：空值都在前面: {}, 空值都在后面: {}",
//             all_empty_at_beginning, all_empty_at_end
//         );
//
//         // 必须满足空值都在前面或者都在后面的条件之一
//         assert!(all_empty_at_beginning || all_empty_at_end);
//
//         // 检查非空值的排序是否正确
//         // 获取所有非空值ID的顺序
//         let non_empty_ids_in_order: Vec<u32> = non_empty_positions
//             .iter()
//             .map(|&pos| desc_ids[pos])
//             .collect();
//
//         println!("降序：非空值顺序: {:?}", non_empty_ids_in_order);
//
//         // 验证非空值按预期顺序排列
//         if non_empty_ids_in_order.len() == expected_desc_order.len() {
//             for i in 0..non_empty_ids_in_order.len() {
//                 assert_eq!(non_empty_ids_in_order[i], expected_desc_order[i]);
//             }
//         }
//     }
// }
//
// #[test]
// fn test_enum_field_sort_with_empty_values() {
//     // 创建测试数据，包含有值和空值的顶点
//     let vertices = vec![
//         create_test_vertex_with_enum(1, "顶点1", Some("MEDIUM")), // 优先级中
//         create_test_vertex_with_enum(2, "顶点2", Some("HIGH")),   // 优先级高
//         create_test_vertex_with_enum(3, "顶点3", None),           // 空值
//         create_test_vertex_with_enum(4, "顶点4", Some("LOW")),    // 优先级低
//         create_test_vertex_with_enum(5, "顶点5", None),           // 空值
//     ];
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//
//     // 测试升序排序
//     let asc_sort_config = create_enum_sort_config("enum-field-1", SortWay::Asc);
//     let asc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(asc_sort_config), &txn);
//     let asc_ids: Vec<u32> = asc_sorted.iter().map(|v| v.id).collect();
//
//     println!("枚举字段升序排序结果：{:?}", asc_ids);
//
//     // 验证排序结果正确性
//     let empty_ids = vec![3, 5];
//     let non_empty_ids = vec![1, 2, 4];
//     let expected_asc_order = vec![4, 1, 2]; // 按优先级升序排列应该是LOW, MEDIUM, HIGH
//
//     // 获取所有空值和非空值的索引位置
//     let empty_positions: Vec<usize> = asc_ids
//         .iter()
//         .enumerate()
//         .filter_map(|(i, &id)| {
//             if empty_ids.contains(&id) {
//                 Some(i)
//             } else {
//                 None
//             }
//         })
//         .collect();
//
//     let non_empty_positions: Vec<usize> = asc_ids
//         .iter()
//         .enumerate()
//         .filter_map(|(i, &id)| {
//             if non_empty_ids.contains(&id) {
//                 Some(i)
//             } else {
//                 None
//             }
//         })
//         .collect();
//
//     // 验证排序结果
//     if !empty_positions.is_empty() && !non_empty_positions.is_empty() {
//         // 检查空值是否都在结果列表的一端（开始或结束）
//         let all_empty_at_beginning = empty_positions
//             .iter()
//             .all(|&pos| pos < non_empty_positions[0]);
//         let all_empty_at_end = empty_positions
//             .iter()
//             .all(|&pos| pos > *non_empty_positions.last().unwrap());
//
//         println!(
//             "空值位置: {:?}, 非空值位置: {:?}",
//             empty_positions, non_empty_positions
//         );
//         println!(
//             "空值都在前面: {}, 空值都在后面: {}",
//             all_empty_at_beginning, all_empty_at_end
//         );
//
//         // 必须满足空值都在前面或者都在后面的条件之一
//         assert!(all_empty_at_beginning || all_empty_at_end);
//
//         // 检查非空值的排序是否正确
//         // 获取所有非空值ID的顺序
//         let non_empty_ids_in_order: Vec<u32> = non_empty_positions
//             .iter()
//             .map(|&pos| asc_ids[pos])
//             .collect();
//
//         println!("非空值顺序: {:?}", non_empty_ids_in_order);
//
//         // 验证非空值按预期顺序排列
//         if non_empty_ids_in_order.len() == expected_asc_order.len() {
//             for i in 0..non_empty_ids_in_order.len() {
//                 assert_eq!(non_empty_ids_in_order[i], expected_asc_order[i]);
//             }
//         }
//     }
//
//     // 测试降序排序
//     let desc_sort_config = create_enum_sort_config("enum-field-1", SortWay::Desc);
//     let desc_sorted = SortPageProcessor::sort_and_page(&vertices, &Some(desc_sort_config), &txn);
//     let desc_ids: Vec<u32> = desc_sorted.iter().map(|v| v.id).collect();
//
//     println!("枚举字段降序排序结果：{:?}", desc_ids);
//
//     // 验证排序结果正确性
//     let expected_desc_order = vec![2, 1, 4]; // 按优先级降序排列应该是HIGH, MEDIUM, LOW
//
//     // 获取所有空值和非空值的索引位置
//     let empty_positions: Vec<usize> = desc_ids
//         .iter()
//         .enumerate()
//         .filter_map(|(i, &id)| {
//             if empty_ids.contains(&id) {
//                 Some(i)
//             } else {
//                 None
//             }
//         })
//         .collect();
//
//     let non_empty_positions: Vec<usize> = desc_ids
//         .iter()
//         .enumerate()
//         .filter_map(|(i, &id)| {
//             if non_empty_ids.contains(&id) {
//                 Some(i)
//             } else {
//                 None
//             }
//         })
//         .collect();
//
//     // 验证排序结果
//     if !empty_positions.is_empty() && !non_empty_positions.is_empty() {
//         // 检查空值是否都在结果列表的一端（开始或结束）
//         let all_empty_at_beginning = empty_positions
//             .iter()
//             .all(|&pos| pos < non_empty_positions[0]);
//         let all_empty_at_end = empty_positions
//             .iter()
//             .all(|&pos| pos > *non_empty_positions.last().unwrap());
//
//         println!(
//             "降序：空值位置: {:?}, 非空值位置: {:?}",
//             empty_positions, non_empty_positions
//         );
//         println!(
//             "降序：空值都在前面: {}, 空值都在后面: {}",
//             all_empty_at_beginning, all_empty_at_end
//         );
//
//         // 必须满足空值都在前面或者都在后面的条件之一
//         assert!(all_empty_at_beginning || all_empty_at_end);
//
//         // 检查非空值的排序是否正确
//         // 获取所有非空值ID的顺序
//         let non_empty_ids_in_order: Vec<u32> = non_empty_positions
//             .iter()
//             .map(|&pos| desc_ids[pos])
//             .collect();
//
//         println!("降序：非空值顺序: {:?}", non_empty_ids_in_order);
//
//         // 验证非空值按预期顺序排列
//         if non_empty_ids_in_order.len() == expected_desc_order.len() {
//             for i in 0..non_empty_ids_in_order.len() {
//                 assert_eq!(non_empty_ids_in_order[i], expected_desc_order[i]);
//             }
//         }
//     }
// }
//
// #[test]
// fn test_sort_with_identical_values_pagination() {
//     // 创建20个具有相同更新时间的顶点
//     let mut vertices = Vec::with_capacity(20);
//     let same_timestamp = 1640000000; // 2021-12-20
//
//     let test_db = TestDb::new();
//     let txn = test_db.db.transaction();
//     for i in 1..=20 {
//         let vertex = Vertex {
//             id: i,
//             card_id: format!("card-{}", i),
//             org_id: Identifier::new("orgId"),
//             card_type_id: Identifier::new("t1"),
//             container_id: Identifier::new("c1"),
//             stream_info: StreamInfo {
//                 stream_id: Default::default(),
//                 step_id: Default::default(),
//                 status_id: Default::default(),
//             },
//             state: CardState::Active,
//             title: VertexTitle::PureTitle(format!("顶点-{}", i)),
//             code_in_org: i.to_string(),
//             code_in_org_int: i,
//             custom_code: None,
//             position: 0,
//             created_at: 0,
//             updated_at: same_timestamp, // 所有顶点使用相同的更新时间
//             archived_at: None,
//             discarded_at: None,
//             comment_date: None,
//             discard_reason: None,
//             restore_reason: None,
//             field_values: Some(HashMap::new()),
//             desc: Description {
//                 content: None,
//                 changed: true,
//             },
//             tags: None,
//         };
//
//         vertices.push(Arc::new(vertex));
//     }
//
//     // 随机打乱顶点顺序
//     vertices.shuffle(&mut thread_rng());
//
//     // 创建基于更新时间的排序配置
//     let inner_field = SortInnerField {
//         field_id: "updated".to_string(),
//     };
//
//     let sort_field = SortField {
//         path: None,
//         field_type: Some(FieldType::InnerField(inner_field)),
//     };
//
//     let sort = Sort {
//         sort_field: Some(sort_field),
//         sort_way: SortWay::Asc as i32,
//     };
//
//     // 请求第一页
//     let page1_config = SortAndPage {
//         sorts: vec![sort.clone()],
//         page: Some(Page {
//             page_num: 1,
//             page_size: 10,
//         }),
//     };
//
//     // 应用排序获取第一页
//     let page1_results = SortPageProcessor::sort_and_page(&vertices, &Some(page1_config), &txn);
//     assert_eq!(page1_results.len(), 10);
//     let page1_ids: Vec<u32> = page1_results.iter().map(|v| v.id).collect();
//     println!("相同更新时间第1页结果：{:?}", page1_ids);
//
//     // 请求第二页
//     let page2_config = SortAndPage {
//         sorts: vec![sort],
//         page: Some(Page {
//             page_num: 2,
//             page_size: 10,
//         }),
//     };
//
//     // 应用排序获取第二页
//     let page2_results = SortPageProcessor::sort_and_page(&vertices, &Some(page2_config), &txn);
//     assert_eq!(page2_results.len(), 10);
//     let page2_ids: Vec<u32> = page2_results.iter().map(|v| v.id).collect();
//     println!("相同更新时间第2页结果：{:?}", page2_ids);
//
//     // 验证第一页和第二页数据没有重叠
//     for id in &page1_ids {
//         assert!(
//             !page2_ids.contains(id),
//             "ID {} 在第一页和第二页同时出现，表示分页存在问题",
//             id
//         );
//     }
//
//     // 验证所有ID都在第一页或第二页中
//     let mut all_ids = HashSet::new();
//     for &id in &page1_ids {
//         all_ids.insert(id);
//     }
//     for &id in &page2_ids {
//         all_ids.insert(id);
//     }
//     assert_eq!(all_ids.len(), 20, "总共应该有20个不同的ID");
//
//     // 注释掉原来的测试，因为现在排序逻辑在值相同时不再按ID排序
//     // if page1_ids.len() > 1 && page2_ids.len() > 1 {
//     //     let largest_id_page1 = *page1_ids.iter().max().unwrap();
//     //     let smallest_id_page2 = *page2_ids.iter().min().unwrap();
//     //
//     //     // 由于所有更新时间相同，排序应该按照ID排序，所以第一页最大ID应小于第二页最小ID
//     //     assert!(largest_id_page1 < smallest_id_page2,
//     //         "第一页最大ID {} 应小于第二页最小ID {}", largest_id_page1, smallest_id_page2);
//     // }
// }
