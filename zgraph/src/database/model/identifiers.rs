use std::ops::Deref;
use std::str::FromStr;

use internment::Intern;
use serde::{Deserialize, Deserializer, Serialize, Serializer};

/// 标识符结构体 - 内部化的字符串标识符
///
/// 使用内部化（interning）技术确保相同的字符串只存储一次，
/// 这样可以节省内存并加速字符串比较操作（变为指针比较）
#[derive(Eq, PartialEq, Copy, Clone, Debug, Hash, Ord, PartialOrd)]
pub struct Identifier(pub Intern<String>);

impl Identifier {
    /// 创建新的标识符
    ///
    /// # 参数
    /// * `s` - 可转换为String的输入
    ///
    /// # 返回
    /// 内部化后的标识符
    pub fn new<S: Into<String>>(s: S) -> Self {
        Self(Intern::new(s.into()))
    }

    /// 获取标识符的字符串引用
    ///
    /// # 返回
    /// 对内部字符串的引用
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

/// 实现默认值 - 返回空字符串标识符
impl Default for Identifier {
    fn default() -> Self {
        Self(Intern::new("".to_string()))
    }
}

/// 实现解引用 - 允许像使用String一样使用Identifier
impl Deref for Identifier {
    type Target = String;
    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

/// 实现从字符串解析 - 允许使用`str::parse()`方法创建标识符
impl FromStr for Identifier {
    type Err = ();

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        Ok(Self::new(s.to_string()))
    }
}

/// 实现序列化 - 将标识符序列化为普通字符串
impl Serialize for Identifier {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        (*self.0).serialize(serializer)
    }
}

/// 实现反序列化 - 从字符串反序列化为标识符
impl<'de> Deserialize<'de> for Identifier {
    fn deserialize<D>(deserializer: D) -> Result<Identifier, D::Error>
    where
        D: Deserializer<'de>,
    {
        let v: String = Deserialize::deserialize(deserializer)?;
        Ok(Identifier::new(v))
    }
}

#[cfg(test)]
mod tests {
    use super::Identifier;


    #[test]
    fn should_create() {
        assert_eq!(Identifier::new("foo").as_str(), "foo");
    }
}
