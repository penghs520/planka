/**
 * 工具模块 - 提供各种通用工具函数和数据处理工具
 *
 * 该模块包含:
 * - 二进制数据序列化和反序列化功能
 * - UUID生成工具
 * - 字符串处理工具
 * - 各种类型转换辅助函数
 * - 拼音工具函数
 */

use crate::database::model;
use std::collections::HashMap;
use std::io::{Cursor, Error as IoError, Read, Write};
use std::str::FromStr;
use std::sync::Mutex;
use std::{str, u8};


use crate::database::model::{EdgeDirection, VertexId};
use compact_str::CompactString;
use once_cell::sync::Lazy;
use pinyin::ToPinyin;
use uuid::v1::{Context, Timestamp};
use uuid::Uuid;

/// 节点ID常量 - 用于UUID生成
const NODE_ID: [u8; 6] = [0, 0, 0, 0, 0, 0];

/// UUID上下文单例 - 使用Lazy静态初始化
static CONTEXT: Lazy<Context> = Lazy::new(|| Context::new(0));

/// 拼音缓存 - 缓存已转换的拼音结果以提高性能
static PINYIN_CACHE: Lazy<Mutex<HashMap<String, String>>> =
    Lazy::new(|| Mutex::new(HashMap::new()));

/// 组件枚举 - 表示可序列化/反序列化的不同类型
///
/// 用于在二进制数据和程序数据结构之间进行转换
pub enum Component<'a> {
    /// 节点ID组件
    VertexId(&'a VertexId),
    /// 边方向组件
    EdgeDirection(&'a EdgeDirection),
    /// 紧凑字符串组件
    CompactString(CompactString),
    /// 固定长度字符串组件
    FixedLengthString(&'a str),
    /// 标识符组件
    Identifier(&'a model::Identifier),
}

impl<'a> Component<'a> {
    /// 计算组件的字节长度
    ///
    /// 返回序列化后组件将占用的字节数
    pub fn byte_len(&self) -> usize {
        match self {
            Component::VertexId(_) => 4,
            Component::FixedLengthString(s) => s.len(),
            Component::Identifier(t) => t.0.len() + 1,
            Component::CompactString(s) => { s.len() + 1 }
            Component::EdgeDirection(dir) => {
                match dir {
                    EdgeDirection::Dest => { 4 }
                    EdgeDirection::Src => { 3 }
                }
            }
        }
    }

    /// 将组件写入字节游标
    ///
    /// # 参数
    /// * `cursor` - 要写入的字节缓冲区游标
    /// 
    /// # 返回
    /// IO操作结果
    pub fn write(&self, cursor: &mut Cursor<Vec<u8>>) -> Result<(), IoError> {
        match self {
            Component::VertexId(vid) => cursor.write_all(vid.to_be_bytes().as_ref()),
            Component::FixedLengthString(s) => {
                cursor.write_all(&[s.len() as u8])?;
                cursor.write_all(s.as_bytes())
            }
            Component::Identifier(i) => {
                //先写一个长度，再写实际的字节
                cursor.write_all(&[i.0.len() as u8])?;
                cursor.write_all(i.0.as_bytes())
            }
            Component::CompactString(s) => {
                cursor.write_all(&[s.len() as u8])?;
                cursor.write_all(s.as_bytes())
            }
            Component::EdgeDirection(dir) => {
                match dir {
                    EdgeDirection::Dest => {
                        cursor.write_all(&[4u8])?;
                        cursor.write_all(b"dest")
                    }
                    EdgeDirection::Src => {
                        cursor.write_all(&[3u8])?;
                        cursor.write_all(b"src")
                    }
                }
            }
        }
    }
}

/// 从组件列表构建字节数组
///
/// # 参数
/// * `components` - 要序列化的组件列表
/// 
/// # 返回
/// 包含序列化数据的字节向量
pub fn build(components: &[Component]) -> Vec<u8> {
    let len = components.iter().fold(0, |len, component| len + component.byte_len());
    let mut cursor: Cursor<Vec<u8>> = Cursor::new(Vec::with_capacity(len));

    for component in components {
        if let Err(err) = component.write(&mut cursor) {
            panic!("Could not write bytes: {err}");
        }
    }
    cursor.into_inner()
}

/// 从字节流读取节点ID
///
/// # 参数
/// * `cursor` - 字节流游标
///
/// # 返回
/// 解析出的节点ID（现在是 u64）
pub fn read_vertex_id<T: AsRef<[u8]>>(cursor: &mut Cursor<T>) -> VertexId {
    let mut buf: [u8; 8] = [0; 8];
    cursor.read_exact(&mut buf).unwrap();
    u64::from_be_bytes(buf)
}

/// 从字节流读取标识符
///
/// # 参数
/// * `cursor` - 字节流游标
/// 
/// # 返回
/// 解析出的标识符
pub fn read_identifier<T: AsRef<[u8]>>(cursor: &mut Cursor<T>) -> model::Identifier {
    let t_len = {
        let mut buf: [u8; 1] = [0; 1];
        cursor.read_exact(&mut buf).unwrap();
        buf[0] as usize
    };

    let mut buf = vec![0u8; t_len];
    cursor.read_exact(&mut buf).unwrap();

    unsafe {
        let s = str::from_utf8_unchecked(&buf).to_string();
        model::Identifier::new(s)
    }
}

/// 从字节流读取边方向
///
/// # 参数
/// * `cursor` - 字节流游标
/// 
/// # 返回
/// 解析出的边方向枚举值
pub fn read_edge_direction<T: AsRef<[u8]>>(cursor: &mut Cursor<T>) -> EdgeDirection {
    let t_len = {
        let mut buf: [u8; 1] = [0; 1];
        cursor.read_exact(&mut buf).unwrap();
        buf[0] as usize
    };

    let mut buf = vec![0u8; t_len];
    cursor.read_exact(&mut buf).unwrap();

    unsafe {
        let s = str::from_utf8_unchecked(&buf).to_string();
        EdgeDirection::from_str(s.as_str()).unwrap()
    }
}

/// 从字节流读取紧凑字符串
///
/// # 参数
/// * `cursor` - 字节流游标
/// 
/// # 返回
/// 解析出的紧凑字符串
pub fn read_compact_string<T: AsRef<[u8]>>(cursor: &mut Cursor<T>) -> CompactString {
    let t_len = {
        let mut buf: [u8; 1] = [0; 1];
        cursor.read_exact(&mut buf).unwrap();
        buf[0] as usize
    };

    let mut buf = vec![0u8; t_len];
    cursor.read_exact(&mut buf).unwrap();

    unsafe {
        let s = str::from_utf8_unchecked(&buf).to_string();
        CompactString::new(s)
    }
}

/// 从字节流读取固定长度字符串
///
/// # 参数
/// * `cursor` - 字节流游标
/// 
/// # 返回
/// 解析出的字符串
pub fn read_fixed_length_string<T: AsRef<[u8]>>(cursor: &mut Cursor<T>) -> String {
    let t_len = {
        let mut buf: [u8; 1] = [0; 1];
        cursor.read_exact(&mut buf).unwrap();
        buf[0] as usize
    };
    let mut buf = vec![0u8; t_len];
    cursor.read_exact(&mut buf).unwrap();
    unsafe {
        str::from_utf8_unchecked(&buf).to_string()
    }
}

/// 生成版本1 UUID
///
/// 使用时间戳和固定节点ID生成唯一标识符
/// 
/// # 返回
/// 新生成的UUID
pub fn generate_uuid_v1() -> Uuid {
    Uuid::new_v1(Timestamp::now(&*CONTEXT), &NODE_ID)
}

/// 拼音工具命名空间 - 包含拼音相关功能
pub mod pinyin_utils {
    use super::*;

    /// 将汉字转换为拼音（不带声调）
    ///
    /// # 参数
    /// * `text` - 要转换的汉字文本
    ///
    /// # 返回
    /// 返回转换后的拼音字符串，多个拼音之间用空格分隔
    pub fn to_pinyin(text: &str) -> String {
        // 检查缓存
        if let Ok(cache) = PINYIN_CACHE.lock() {
            if let Some(cached) = cache.get(text) {
                return cached.clone();
            }
        }

        // 转换为拼音
        let mut result = String::new();
        for c in text.chars() {
            if let Some(pinyin_iter) = c.to_pinyin() {
                let pinyin_str = pinyin_iter.plain().to_string();
                if !result.is_empty() {
                    result.push(' ');
                }
                result.push_str(&pinyin_str);
            } else {
                // 如果是非汉字，直接添加（转为小写）
                if !result.is_empty() && !c.is_whitespace() {
                    result.push(' ');
                }
                if !c.is_whitespace() {
                    result.push(c.to_lowercase().next().unwrap_or(c));
                }
            }
        }

        // 更新缓存
        if let Ok(mut cache) = PINYIN_CACHE.lock() {
            cache.insert(text.to_string(), result.clone());
        }

        result
    }

    /// 将汉字转换为拼音首字母
    ///
    /// # 参数
    /// * `text` - 要转换的汉字文本
    ///
    /// # 返回
    /// 返回转换后的拼音首字母字符串，不包含空格
    pub fn to_pinyin_initials(text: &str) -> String {
        let pinyin = to_pinyin(text);
        pinyin
            .split_whitespace()
            .filter_map(|word| word.chars().next().map(|c| c.to_lowercase().next().unwrap_or(c)))
            .collect()
    }

    /// 检查文本是否只包含拼音字符（a-z和空格）
    ///
    /// # 参数
    /// * `text` - 要检查的文本
    ///
    /// # 返回
    /// 如果文本只包含拼音字符返回true，否则返回false
    fn is_pinyin_text(text: &str) -> bool {
        text.chars().all(|c| c.is_ascii_lowercase() || c == ' ')
    }

    /// 检查文本是否匹配拼音
    ///
    /// # 参数
    /// * `text` - 要检查的文本
    /// * `keyword` - 要匹配的关键词（可以是汉字、拼音或拼音首字母）
    ///
    /// # 返回
    /// 如果匹配则返回true，否则返回false
    pub fn is_pinyin_match(text: &str, keyword: &str) -> bool {
        // 1. 直接文本匹配
        if text.contains(keyword) {
            return true;
        }
        
        // 将关键词转为小写
        let keyword_lowercase = keyword.to_lowercase();
        
        // 如果关键词不是拼音字符，不进行拼音匹配，直接返回false
        if !is_pinyin_text(&keyword_lowercase) {
            return false;
        }
        
        // 2. 拼音匹配 - 移除空格后比较
        let text_pinyin = to_pinyin(text);
        let text_pinyin_nospace = text_pinyin.replace(" ", "");
        
        if text_pinyin_nospace.contains(&keyword_lowercase.replace(" ", "")) {
            return true;
        }

        // 3. 拼音首字母匹配
        let text_initials = to_pinyin_initials(text);
        if text_initials.contains(&keyword_lowercase.replace(" ", "")) {
            return true;
        }

        // 4. 匹配关键词的拼音
        let keyword_pinyin = to_pinyin(keyword);
        let keyword_pinyin_nospace = keyword_pinyin.replace(" ", "");
        
        if !keyword_pinyin_nospace.is_empty() && text_pinyin_nospace.contains(&keyword_pinyin_nospace) {
            return true;
        }

        false
    }
}

#[cfg(test)]
mod pinyin_tests {
    use super::pinyin_utils::*;

    #[test]
    fn test_to_pinyin() {
        assert_eq!(to_pinyin("你好"), "ni hao");
        assert_eq!(to_pinyin("中国"), "zhong guo");
        assert_eq!(to_pinyin("Hello"), "h e l l o");
    }

    #[test]
    fn test_to_pinyin_initials() {
        assert_eq!(to_pinyin_initials("你好"), "nh");
        assert_eq!(to_pinyin_initials("中国人"), "zgr");
        assert_eq!(to_pinyin_initials("Hello"), "hello");
    }

    #[test]
    fn test_is_pinyin_match() {
        // 拼音匹配
        assert!(is_pinyin_match("你好", "nihao"));
        assert!(is_pinyin_match("你好", "ni hao"));
        
        // 拼音首字母匹配
        assert!(is_pinyin_match("你好世界", "nhsj"));
        
        // 搜索中文关键词的拼音匹配
        assert!(is_pinyin_match("hello你好", "nihao"));

        assert!(is_pinyin_match("hello你好", "he llo"));

        // 不匹配的情况
        assert!(!is_pinyin_match("你好", "hello"));
        assert!(!is_pinyin_match("hello", "世界"));
        
        // 非拼音字符时直接返回false
        assert!(!is_pinyin_match("你好", "ni123"));
        assert!(!is_pinyin_match("你好", "ni@hao"));
        assert!(!is_pinyin_match("你好", "中文"));
    }
}

#[cfg(test)]
mod tests {
    use crate::database::model::Identifier;
    use crate::utils::{build, read_compact_string, read_fixed_length_string, read_identifier, Component};
    use compact_str::CompactString;
    use std::io::Cursor;

    #[test]
    fn should_cursor_read() {
        let id = Identifier::new("998");
        let cmp1 = Component::FixedLengthString("hello world");
        let cmp2 = Component::FixedLengthString("你好，世界");
        let cmp3 = Component::Identifier(&id);
        let cmp4 = Component::CompactString(CompactString::new("12345"));
        let cmp5 = Component::CompactString(CompactString::new("45432"));
        let vec = vec![cmp1, cmp2, cmp3, cmp4, cmp5];
        let v = build(&vec);
        let mut cursor = Cursor::new(v);

        assert_eq!("hello world", read_fixed_length_string(&mut cursor));
        assert_eq!("你好，世界", read_fixed_length_string(&mut cursor));
        assert_eq!(Identifier::new("998"), read_identifier(&mut cursor));
        assert_eq!(CompactString::new("12345"), read_compact_string(&mut cursor));
        assert_eq!(CompactString::new("45432"), read_compact_string(&mut cursor));
    }
}
