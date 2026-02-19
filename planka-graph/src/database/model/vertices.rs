use super::Identifier;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::hash::{Hash, Hasher};
use std::fmt;

/// 节点（Vertex）结构体 - 图数据库中的基本节点
///
/// 节点代表图中的一个实体，包含多种属性如ID、名称、状态等
/// 可通过边与其他节点相连，形成关系网络
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Vertex {
    /// 卡片ID，作为卡片的唯一标识符（u64类型）
    pub card_id: CardId,
    /// 组织ID
    pub org_id: Identifier,
    /// 卡片类型ID，另外一个可能的名字叫做 CardTypeId，vu指的是ValueUnit，这是以往旧系统的对于卡片的命名
    pub card_type_id: CardTypeId,
    /// 容器ID，指明卡片所属的容器
    pub container_id: ContainerId,
    /// 流程信息，包含价值流ID、步骤ID和状态ID
    pub stream_info: StreamInfo,
    /// 卡片状态（进行中、已归档、已放弃）
    pub state: CardState,
    /// 卡片标题
    pub title: VertexTitle,
    /// 在组织中的编码(字符串类型,用来做过滤关键字性能更好，不用将code_in_org_int转成字符串)
    pub code_in_org: String,
    /// 在组织中的编码（int类型，用来做排序更好）
    pub code_in_org_int: u32,
    /// 自定义编码（可选）
    pub custom_code: Option<String>,
    /// 位置值
    pub position: u64,
    /// 卡片创建时间戳
    pub created_at: u64,
    /// 卡片更新时间戳
    pub updated_at: u64,
    /// 卡片归档日期（可选）
    pub archived_at: Option<u64>,
    /// 卡片丢弃日期（可选）
    pub discarded_at: Option<u64>,
    /// 卡片丢弃原因（可选）
    pub discard_reason: Option<String>,
    /// 卡片还原原因（可选）
    pub restore_reason: Option<String>,
    /// 字段值映射（可选），使用字段ID作为键，字段值作为值
    pub field_values: Option<HashMap<FieldId, FieldValue>>,
    /// 卡片描述（可选，单独存储，序列化和反序列化时跳过
    #[serde(skip_serializing, skip_deserializing)]
    pub desc: Description,
}


#[derive(Default,Debug, Serialize, Deserialize, Clone)]
pub struct Description{
    pub content: Option<String>,
    pub changed: bool
}


/// 卡片ID类型 - 使用u64表示，作为卡片的唯一标识符
pub type CardId = u64;
/// 节点ID类型 - 现在是 CardId 的别名，保持向后兼容
pub type VertexId = CardId;
/// 卡片类型ID
pub type CardTypeId = Identifier; //对于重复性比较多的字符串类型，使用Identifier类型，可以减少内存使用
/// 组织ID类型
pub type OrgId = Identifier;
/// 容器ID类型
pub type ContainerId = Identifier;
/// 流程ID类型
pub type StreamId = Identifier;
/// 步骤ID类型
pub type StepId = Identifier;
/// 状态ID类型
pub type StatusId = Identifier;
/// 字段ID类型
pub type FieldId = Identifier;

///卡片标题
#[derive(Debug, Serialize, Deserialize, Clone)]
pub enum VertexTitle {
    /// 纯标题 - 只包含一个字符串值
    PureTitle(String),
    /// 拼接标题 - 由多个部分组成的复杂标题
    JointTitle(JointTitleInfo),
}

/// 拼接标题信息
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct JointTitleInfo {
    /// 标题名称
    pub name: String,
    /// 标题区域类型
    pub area: TitleJointArea,
    /// 标题组成部分
    pub multi_parts: Vec<JointTitleParts>,
}


impl VertexTitle {
    pub fn name(&self) -> &String {
        match self {
            VertexTitle::PureTitle(p) => {
                p
            }
            VertexTitle::JointTitle(j) => {
                &j.name
            }
        }
    }
}

impl fmt::Display for JointTitleInfo {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        // 从 multi_parts 中提取最后一个部分的名称，然后用 "-" 连接
        let joint = self.multi_parts
            .iter()
            .filter_map(|parts| {
                // 获取每个 JointTitleParts 中 parts 的最后一个元素
                parts.parts.last()
            })
            .map(|part| part.name.as_str())
            .filter(|name| !name.is_empty())
            .collect::<Vec<_>>()
            .join("-");

        // 根据区域类型决定拼接顺序
        match self.area {
            TitleJointArea::Prefix => {
                if joint.is_empty() {
                    write!(f, "{}", self.name)
                } else {
                    write!(f, "{} {}", joint, self.name)
                }
            }
            TitleJointArea::Suffix => {
                if joint.is_empty() {
                    write!(f, "{}", self.name)
                } else {
                    write!(f, "{} {}", self.name, joint)
                }
            }
        }
    }
}

impl fmt::Display for VertexTitle {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            VertexTitle::PureTitle(title) => write!(f, "{}", title),
            VertexTitle::JointTitle(joint_info) => write!(f, "{}", joint_info),
        }
    }
}

/// 拼接标题的区域类型
#[derive(Debug, Serialize, Deserialize, Clone, Copy, PartialEq, Eq)]
pub enum TitleJointArea {
    /// 前缀区域 - 显示在卡片标题前面
    Prefix,
    /// 后置区域 - 显示在卡片标题后面
    Suffix,
}

/// 拼接标题的多个组成部分
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct JointTitleParts {
    /// 组成部分列表
    pub parts: Vec<JointTitlePart>,
}

/// 拼接标题的单个组成部分
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct JointTitlePart {
    /// 名称
    pub name: String,
}

/// 流程信息结构体 - 存储与流程相关的所有ID
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct StreamInfo {
    /// 流程ID
    pub stream_id: StreamId,
    /// 状态ID
    pub status_id: StatusId,
}

/// 实现节点的相等比较 - 只比较card_id
impl PartialEq for Vertex {
    fn eq(&self, other: &Vertex) -> bool {
        self.card_id == other.card_id
    }
}

/// 实现节点的哈希计算 - 只使用card_id
impl Hash for Vertex {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.card_id.hash(state);
    }
}

/// 声明Vertex实现完全相等
impl Eq for Vertex {}

/// 卡片状态枚举 - 定义卡片可能的状态
#[derive(Serialize, Deserialize, Debug, PartialEq, Eq, Hash, Clone, Copy)]
pub enum CardState {
    /// 活跃 - 卡片处于活跃状态
    Active,
    /// 已归档 - 卡片已完成且归档
    Archived,
    /// 已丢弃 - 卡片已被丢弃
    Discarded,
}

/// 字段值枚举 - 不同类型的字段值
#[derive(Serialize, Deserialize, Debug, Clone)]
pub enum FieldValue {
    /// 文本值
    Text(TextValue),
    /// 数值
    Number(NumberValue),
    /// 日期值
    Date(DateValue),
    /// 枚举值
    Enum(EnumValue),
    /// 网址链接值
    WebLink(WebLinkValue),
    /// 附件值
    Attachment(AttachmentValue),
}

/// 枚举项ID类型
pub type EnumItemId = Identifier;

/// 枚举值结构体 - 存储枚举类型的字段
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct EnumValue {
    /// 选中的枚举项列表
    pub items: Vec<EnumItemId>,
}

/// 文本值结构体 - 存储文本类型的字段
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct TextValue {
    /// 文本内容
    pub text: String,
}

/// 数值结构体 - 存储数值类型的字段
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct NumberValue {
    /// 数值
    pub number: f64,
}

/// 日期值结构体 - 存储日期类型的字段
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct DateValue {
    /// 时间戳
    pub timestamp: u64,
}

/// 网址链接值结构体 - 存储网址链接类型的字段
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct WebLinkValue {
    /// 链接地址
    pub href: String,
    /// 链接名称
    pub name: String,
}

/// 附件项结构体 - 存储单个附件信息
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AttachmentItem {
    /// 附件ID
    pub id: String,
    /// 附件名称
    pub name: String,
    /// 上传者
    pub uploader: String,
    /// 创建时间
    pub created_at: u64,
    /// 文件大小
    pub size: u64,
}

/// 附件值结构体 - 存储附件类型的字段
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AttachmentValue {
    /// 附件列表
    pub items: Vec<AttachmentItem>,
}
