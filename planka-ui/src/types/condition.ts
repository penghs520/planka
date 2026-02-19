/**
 * 条件定义类型
 *
 * 与后端 dev.planka.domain.schema.definition.condition 包对应
 * 支持任意深度的嵌套条件结构
 */

// ==================== 核心结构 ====================

/**
 * 条件定义
 */
export interface Condition {
  /** 根条件节点 */
  root?: ConditionNode
}

/**
 * 条件节点（联合类型）
 *
 * 可以是条件组（GROUP）或具体的条件项
 */
export type ConditionNode = ConditionGroup | ConditionItem

/**
 * 条件组（递归结构）
 *
 * 通过 AND/OR 逻辑运算符组合多个子节点（支持递归嵌套）
 */
export interface ConditionGroup {
  /** 节点类型标识 */
  nodeType: 'GROUP'

  /** 逻辑操作符 */
  operator: 'AND' | 'OR'

  /** 子节点列表（可以包含 ConditionGroup 或 ConditionItem，支持递归） */
  children: ConditionNode[]
}

/**
 * 条件项联合类型
 */
export type ConditionItem =
  | TextConditionItem
  | NumberConditionItem
  | DateConditionItem
  | EnumConditionItem
  | StatusConditionItem
  | TitleConditionItem
  | CodeConditionItem
  | WebUrlConditionItem
  | SystemUserConditionItem
  | CardCycleConditionItem
  | LinkConditionItem
// SystemDateConditionItem 已合并到 DateConditionItem（通过 DateSubject 接口支持系统日期字段）
// StateConditionItem 已移除，使用 CardCycleConditionItem 替代

/**
 * 节点类型常量
 */
export const NodeType = {
  GROUP: 'GROUP',
  TEXT: 'TEXT',
  NUMBER: 'NUMBER',
  DATE: 'DATE',
  ENUM: 'ENUM',
  STATUS: 'STATUS',
  TITLE: 'TITLE',
  CODE: 'CODE',
  LINK: 'LINK',
  WEB_URL: 'WEB_URL',
  // 系统用户字段
  CREATED_BY: 'CREATED_BY',
  UPDATED_BY: 'UPDATED_BY',
  // 卡片生命周期（活跃/丢弃/归档）
  CARD_CYCLE: 'CARD_CYCLE',
  // 注：STATE 已移除，使用 CARD_CYCLE 替代
  // 注：系统日期字段（CREATED_AT, UPDATED_AT, DISCARDED_AT, ARCHIVED_AT）
  // 通过 DateConditionItem 的 DateSubject.systemField 来支持，不再作为独立的 NodeType
} as const

export type NodeTypeValue = typeof NodeType[keyof typeof NodeType]

// ==================== 路径和Subject ====================

/**
 * 路径：表示多级关联
 *
 * 例如：当前卡.父需求.创建人
 * linkFieldId 格式为 "{linkTypeId}:{SOURCE|TARGET}"
 */
export interface Path {
  /** 关联节点列表（linkFieldId 字符串数组） */
  linkNodes: string[]
}

/**
 * 通用Subject（用于大部分条件类型）
 */
export interface Subject {
  /** 路径（可选，用于引用关联卡片的属性） */
  path?: Path

  /** 字段ID */
  fieldId: string
}

/**
 * 状态Subject（特殊：使用streamId而非fieldId）
 */
export interface StatusSubject {
  /** 路径（可选） */
  path?: Path

  /** 价值流ID */
  streamId: string
}

/**
 * 系统字段Subject（无fieldId，如TITLE、KEYWORD、CODE、CARD_CYCLE等）
 */
export interface SystemSubject {
  /** 路径（可选） */
  path?: Path
}

// ==================== 文本条件（TEXT, TITLE, CODE共用） ====================

export interface TextConditionItem {
  nodeType: 'TEXT' | 'TITLE' | 'CODE'
  subject: Subject | SystemSubject
  operator: TextOperator
}

export type TextOperator =
  | { type: 'EQ'; value: string }           // 等于
  | { type: 'NE'; value: string }           // 不等于
  | { type: 'CONTAINS'; value: string }     // 包含
  | { type: 'NOT_CONTAINS'; value: string } // 不包含
  | { type: 'STARTS_WITH'; value: string }  // 以...开始
  | { type: 'ENDS_WITH'; value: string }    // 以...结束
  | { type: 'IS_EMPTY' }                    // 为空
  | { type: 'IS_NOT_EMPTY' }                // 不为空

// ==================== 数字条件（NUMBER） ====================

export interface NumberConditionItem {
  nodeType: 'NUMBER'
  subject: Subject
  operator: NumberOperator
}

/**
 * 数字值（第一版仅支持静态值）
 */
export interface NumberValue {
  type: 'STATIC'
  value: number
}

export type NumberOperator =
  | { type: 'EQ'; value: NumberValue }              // 等于
  | { type: 'NE'; value: NumberValue }              // 不等于
  | { type: 'GT'; value: NumberValue }              // 大于
  | { type: 'GE'; value: NumberValue }              // 大于等于
  | { type: 'LT'; value: NumberValue }              // 小于
  | { type: 'LE'; value: NumberValue }              // 小于等于
  | { type: 'BETWEEN'; start: NumberValue; end: NumberValue }  // 在范围内
  | { type: 'IS_EMPTY' }                            // 为空
  | { type: 'IS_NOT_EMPTY' }                        // 不为空

// ==================== 日期条件（DATE） ====================

/**
 * 系统日期字段枚举
 */
export enum SystemDateField {
  CREATED_AT = 'CREATED_AT',
  UPDATED_AT = 'UPDATED_AT',
  DISCARDED_AT = 'DISCARDED_AT',
  ARCHIVED_AT = 'ARCHIVED_AT',
}

/**
 * 日期主体类型（支持自定义字段和系统字段）
 */
export type DateSubject =
  | { type: 'FIELD'; path?: Path; fieldId: string }                     // 自定义日期字段
  | { type: 'SYSTEM'; path?: Path; systemField: SystemDateField }       // 系统日期字段

export interface DateConditionItem {
  nodeType: 'DATE'
  subject: DateSubject
  operator: DateOperator
}

/**
 * 日期值
 */
export type DateValue =
  | { type: 'SPECIFIC'; value: string }       // 具体日期（ISO 8601格式）
  | { type: 'KEY_DATE'; keyDate: KeyDate }    // 关键日期

/**
 * 关键日期枚举
 */
export enum KeyDate {
  TODAY = 'TODAY',
  YESTERDAY = 'YESTERDAY',
  TOMORROW = 'TOMORROW',
  THIS_WEEK = 'THIS_WEEK',
  LAST_WEEK = 'LAST_WEEK',
  NEXT_WEEK = 'NEXT_WEEK',
  THIS_MONTH = 'THIS_MONTH',
  LAST_MONTH = 'LAST_MONTH',
  NEXT_MONTH = 'NEXT_MONTH',
  THIS_QUARTER = 'THIS_QUARTER',
  THIS_YEAR = 'THIS_YEAR',
  LAST_7_DAYS = 'LAST_7_DAYS',
  LAST_30_DAYS = 'LAST_30_DAYS',
  NEXT_7_DAYS = 'NEXT_7_DAYS',
  NEXT_30_DAYS = 'NEXT_30_DAYS',
}

export type DateOperator =
  | { type: 'EQ'; value: DateValue }                    // 等于
  | { type: 'NE'; value: DateValue }                    // 不等于
  | { type: 'BEFORE'; value: DateValue }                // 早于
  | { type: 'AFTER'; value: DateValue }                 // 晚于
  | { type: 'BETWEEN'; start: DateValue; end: DateValue }  // 在范围内
  | { type: 'IS_EMPTY' }                                // 为空
  | { type: 'IS_NOT_EMPTY' }                            // 不为空

// ==================== 枚举条件（ENUM） ====================

export interface EnumConditionItem {
  nodeType: 'ENUM'
  subject: Subject
  operator: EnumOperator
}

export type EnumOperator =
  | { type: 'EQ'; optionId: string }              // 等于（单选）
  | { type: 'NE'; optionId: string }              // 不等于
  | { type: 'IN'; optionIds: string[] }           // 在列表中（多选）
  | { type: 'NOT_IN'; optionIds: string[] }       // 不在列表中
  | { type: 'IS_EMPTY' }                          // 为空
  | { type: 'IS_NOT_EMPTY' }                      // 不为空

// ==================== 状态条件（STATUS - 价值流状态） ====================

export interface StatusConditionItem {
  nodeType: 'STATUS'
  subject: StatusSubject
  operator: StatusOperator
}

export type StatusOperator =
  | { type: 'EQ'; statusId: string }              // 等于
  | { type: 'NE'; statusId: string }              // 不等于
  | { type: 'IN'; statusIds: string[] }           // 在列表中
  | { type: 'NOT_IN'; statusIds: string[] }       // 不在列表中
  | { type: 'REACHED'; statusId: string }         // 已到达（包含当前状态）
  | { type: 'NOT_REACHED'; statusId: string }     // 未到达
  | { type: 'PASSED'; statusId: string }          // 已超过

// ==================== 卡片生命周期状态枚举 ====================

/**
 * 卡片生命周期状态枚举
 */
export enum LifecycleState {
  ACTIVE = 'ACTIVE',         // 活跃
  DISCARDED = 'DISCARDED',   // 丢弃
  ARCHIVED = 'ARCHIVED',     // 归档
}

// ==================== 标题条件（TITLE - 系统字段） ====================

export interface TitleConditionItem {
  nodeType: 'TITLE'
  subject: SystemSubject
  operator: TextOperator  // 复用文本操作符
}

// ==================== 关键词条件（KEYWORD - 全文搜索，暂不实现） ====================

// export interface KeywordConditionItem {
//   nodeType: 'KEYWORD'
//   subject: SystemSubject
//   operator: TextOperator  // 复用文本操作符
// }

// ==================== 编码条件（CODE - 卡片编号） ====================

export interface CodeConditionItem {
  nodeType: 'CODE'
  subject: SystemSubject
  operator: TextOperator  // 复用文本操作符
}

// ==================== 网页链接条件（WEB_URL） ====================

export interface WebUrlConditionItem {
  nodeType: 'WEB_URL'
  subject: Subject
  operator: WebUrlOperator
}

export type WebUrlOperator =
  | { type: 'IS_EMPTY' }      // 为空
  | { type: 'IS_NOT_EMPTY' }  // 不为空

// ==================== 系统用户字段条件（CREATED_BY, UPDATED_BY） ====================

export interface SystemUserConditionItem {
  nodeType: 'CREATED_BY' | 'UPDATED_BY'
  subject: SystemSubject
  operator: UserOperator
}

export type UserOperator =
  | { type: 'EQ'; userId: string }             // 等于（某个用户）
  | { type: 'NE'; userId: string }             // 不等于
  | { type: 'IN'; userIds: string[] }          // 在列表中
  | { type: 'NOT_IN'; userIds: string[] }      // 不在列表中
  | { type: 'IS_CURRENT_USER' }                // 是当前用户
  | { type: 'IS_NOT_CURRENT_USER' }            // 不是当前用户
  | { type: 'IS_EMPTY' }                       // 为空
  | { type: 'IS_NOT_EMPTY' }                   // 不为空

// ==================== 卡片生命周期条件（CARD_CYCLE - 活跃/丢弃/归档） ====================

export interface CardCycleConditionItem {
  nodeType: 'CARD_CYCLE'
  subject: SystemSubject
  operator: LifecycleOperator
}

export type LifecycleOperator =
  | { type: 'IN'; values: LifecycleState[] }         // 在列表中
  | { type: 'NOT_IN'; values: LifecycleState[] }     // 不在列表中

// ==================== 关联条件（LINK） ====================

/**
 * 关联条件项
 *
 * 用于对关联字段本身进行过滤（有关联/无关联/在列表中）
 */
export interface LinkConditionItem {
  nodeType: 'LINK'
  subject: LinkSubject
  operator: LinkOperator
}

/**
 * 关联主体
 *
 * linkFieldId 描述当前要过滤的关联字段，格式为 "{linkTypeId}:{SOURCE|TARGET}"
 * path 描述到达该关联字段的路径（多级关联时使用）
 */
export interface LinkSubject {
  /** 前置路径（可选，用于多级关联） */
  path?: Path
  /** 当前关联字段ID，格式为 "{linkTypeId}:{SOURCE|TARGET}" */
  linkFieldId: string
}

/**
 * 关联值联合类型（用于 IN/NOT_IN 操作符）
 */
export type LinkValue = StaticLinkValue | ReferenceLinkValue

/**
 * 静态关联值（直接指定卡片ID列表）
 */
export interface StaticLinkValue {
  type: 'STATIC'
  cardIds: string[]
}

/**
 * 引用关联值（引用其他来源的卡片）
 */
export interface ReferenceLinkValue {
  type: 'REFERENCE'
  source: ReferenceSource
}

/**
 * 引用来源（第一版仅支持当前用户/成员）
 */
export interface ReferenceSource {
  /** 引用类型：MEMBER表示当前用户/成员卡片 */
  type: 'MEMBER'
  /** 可选的级联路径，用于引用成员卡片的关联属性 */
  path?: Path
}

/**
 * 关联操作符
 */
export type LinkOperator =
  | { type: 'IN'; value: LinkValue }      // 关联的卡片在列表中
  | { type: 'NOT_IN'; value: LinkValue }  // 关联的卡片不在列表中
  | { type: 'HAS_ANY' }                   // 有任何关联
  | { type: 'IS_EMPTY' }                  // 没有关联

// ==================== 工具类型 ====================

/**
 * 获取条件项的节点类型
 */
export function getNodeType(item: ConditionItem): NodeTypeValue {
  return item.nodeType
}

/**
 * 判断节点是否为条件组
 */
export function isConditionGroup(node: ConditionNode): node is ConditionGroup {
  return node.nodeType === NodeType.GROUP
}

/**
 * 判断节点是否为条件项
 */
export function isConditionItem(node: ConditionNode): node is ConditionItem {
  return node.nodeType !== NodeType.GROUP
}

/**
 * 系统日期字段ID前缀（用于区分系统日期字段和其他字段）
 */
export const SYSTEM_DATE_FIELD_PREFIX = 'SYSTEM_DATE:'
