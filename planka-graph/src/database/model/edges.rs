use super::EnumItemId;
use super::{FieldId, Identifier, VertexId};

use crate::database::errors;
use serde::{Deserialize, Serialize};
use std::str::FromStr;

/// 边类型的标识符类型别名
pub type EdgeType = Identifier;

/// 边（Edge）结构体 - 表示图中两个节点之间的关系
///
/// 边连接两个节点，具有方向性（从源到目标），
/// 每条边有一个类型和可选的属性集合
#[derive(Debug, Serialize, Deserialize)]
pub struct Edge {
    /// 源节点ID - 边的起始节点
    pub src_id: VertexId,
    /// 边类型 - 定义了这种关系的语义
    pub t: EdgeType,
    /// 目标节点ID - 边的终止节点
    pub dest_id: VertexId,
    /// 边的属性列表 - 存储与边相关的附加信息
    pub props: Option<Vec<EdgeProp>>,
}


/// 边属性类型，目前只支持数值、日期、枚举
/// 边属性类型和Vertex属性类型有点不一致，它的类型没那么多，同时还有字段ID，所以这里单独定义
#[derive(Serialize, Deserialize, Debug, Clone)]
pub enum EdgeProp {
    /// 数值
    Number(NumberProp),
    /// 日期值
    Date(DateProp),
    /// 枚举值
    Enum(EnumProp),
}

/// 枚举值结构体 - 存储枚举类型的字段
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct EnumProp {
    /// 字段ID : 这个实际上是衍生属性的定义id：deriveFieldDefineId
    pub field_id: FieldId,
    /// 选中的枚举项列表
    pub items: Vec<EnumItemId>,
}

/// 数值结构体 - 存储数值类型的字段
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct NumberProp {
    /// 字段ID : 这个实际上是衍生属性的定义id：deriveFieldDefineId
    pub field_id: FieldId,
    /// 数值
    pub number: f64,
}

/// 日期值结构体 - 存储日期类型的字段
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct DateProp {
    /// 字段ID : 这个实际上是衍生属性的定义id：deriveFieldDefineId
    pub field_id: FieldId,
    /// 时间戳
    pub timestamp: u64,
}

/// 边描述符结构体 - 用于描述和标识边的类型和方向
///
/// 不包含具体的节点ID，主要用于查询和分类边
#[derive(Clone, Copy, Debug, Eq, PartialEq, Hash, Serialize, Deserialize)]
pub struct EdgeDescriptor {
    /// 边类型
    pub t: EdgeType,
    /// 边的方向（源向或目标向）
    pub direction: EdgeDirection,
}

/// 边描述符的实现
impl EdgeDescriptor {
    /// 创建新的边描述符
    ///
    /// # 参数
    /// * `t` - 边类型
    /// * `direction` - 边的方向
    pub fn new(t: EdgeType, direction: EdgeDirection) -> Self {
        Self { t, direction }
    }

}

/// 边的实现
impl Edge {
    /// 创建新的边
    ///
    /// # 参数
    /// * `inbound_id` - 源节点ID
    /// * `t` - 边类型
    /// * `outbound_id` - 目标节点ID
    /// * `props` - 边的属性列表
    pub fn new(inbound_id: VertexId, t: EdgeType, outbound_id: VertexId, props: Option<Vec<EdgeProp>>) -> Edge {
        Edge {
            src_id: inbound_id,
            t,
            dest_id: outbound_id,
            props,
        }
    }

    /// 创建方向翻转的边
    ///
    /// 返回一个新的边，其源节点和目标节点交换，属性为空
    pub fn reversed(&self) -> Edge {
        Edge::new(self.src_id, self.t, self.dest_id, None)
    }
}

/// 边方向枚举 - 定义边的方向性
#[derive(Eq, PartialEq, Clone, Debug, Hash, Copy, Serialize, Deserialize)]
pub enum EdgeDirection {
    /// 源向 - 从当前节点出发的边
    Src,
    /// 目标向 - 进入当前节点的边
    Dest,
}

/// 从字符串解析边方向
impl FromStr for EdgeDirection {
    type Err = errors::ValidationError;

    /// 从字符串转换为边方向
    ///
    /// 支持的值:
    /// * "src" - 源向
    /// * "dest" - 目标向
    fn from_str(s: &str) -> Result<EdgeDirection, Self::Err> {
        match s {
            "dest" => Ok(EdgeDirection::Dest),
            "src" => Ok(EdgeDirection::Src),
            _ => Err(errors::ValidationError::InvalidValue(format!("{} is an invalid edgeDirection", s))),
        }
    }
}

/// 将边方向转换为字符串
impl From<EdgeDirection> for String {
    fn from(d: EdgeDirection) -> Self {
        match d {
            EdgeDirection::Dest => "dest".to_string(),
            EdgeDirection::Src => "src".to_string(),
        }
    }
}