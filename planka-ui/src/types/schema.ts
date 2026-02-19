import type { EntityState } from './common'

/**
 * Schema 大类枚举（用于 API 查询）
 *
 * 对应后端 SchemaType 枚举，用于分页查询接口的 type 参数
 */
export enum SchemaType {
  /** 卡片类型 */
  CARD_TYPE = 'CARD_TYPE',
  /** 属性定义 */
  FIELD_DEFINITION = 'FIELD_DEFINITION',
  /** 属性配置 */
  FIELD_CONFIG = 'FIELD_CONFIG',
  /** 视图 */
  VIEW = 'VIEW',
  /** 价值流定义 */
  VALUE_STREAM = 'VALUE_STREAM',
  /** 关联类型 */
  LINK_TYPE = 'LINK_TYPE',
  /** 架构线定义 */
  STRUCTURE_DEFINITION = 'STRUCTURE_DEFINITION',
  /** 业务规则 */
  BIZ_RULE = 'BIZ_RULE',
  /** 流转策略 */
  FLOW_POLICY = 'FLOW_POLICY',
  /** 卡片详情页模板 */
  CARD_DETAIL_TEMPLATE = 'CARD_DETAIL_TEMPLATE',
  /** 卡片新建页模板 */
  CARD_CREATE_PAGE_TEMPLATE = 'CARD_CREATE_PAGE_TEMPLATE',
  /** 卡面定义 */
  CARD_FACE = 'CARD_FACE',
  /** 卡片权限定义 */
  CARD_PERMISSION = 'CARD_PERMISSION',
  /** 菜单定义 */
  MENU = 'MENU',
  /** 通知模板 */
  NOTIFICATION_TEMPLATE = 'NOTIFICATION_TEMPLATE',
  /** 通知渠道配置 */
  NOTIFICATION_CHANNEL_CONFIG = 'NOTIFICATION_CHANNEL_CONFIG',
  /** 计算公式定义 */
  FORMULA_DEFINITION = 'FORMULA_DEFINITION',
  /** 卡片动作 */
  CARD_ACTION = 'CARD_ACTION',
}

/**
 * Schema 子类型枚举（用于 Jackson 多态序列化）
 *
 * 对应后端 @JsonSubTypes 注解的 name 属性，用于创建/更新时的 type 字段
 */
export enum SchemaSubType {
  // ==================== 卡片类型 ====================
  /** 属性集 */
  TRAIT_CARD_TYPE = 'TRAIT_CARD_TYPE',
  /** 实体类型 */
  ENTITY_CARD_TYPE = 'ENTITY_CARD_TYPE',

  // ==================== 属性定义 ====================
  /** 单行文本属性定义 */
  SINGLE_LINE_TEXT_FIELD_DEFINITION = 'SINGLE_LINE_TEXT_FIELD_DEFINITION',
  /** 多行文本属性定义 */
  MULTI_LINE_TEXT_FIELD_DEFINITION = 'MULTI_LINE_TEXT_FIELD_DEFINITION',
  /** Markdown属性定义 */
  MARKDOWN_FIELD_DEFINITION = 'MARKDOWN_FIELD_DEFINITION',
  /** 数字属性定义 */
  NUMBER_FIELD_DEFINITION = 'NUMBER_FIELD_DEFINITION',
  /** 日期属性定义 */
  DATE_FIELD_DEFINITION = 'DATE_FIELD_DEFINITION',
  /** 枚举属性定义 */
  ENUM_FIELD_DEFINITION = 'ENUM_FIELD_DEFINITION',
  /** 附件属性定义 */
  ATTACHMENT_FIELD_DEFINITION = 'ATTACHMENT_FIELD_DEFINITION',
  /** 网页链接属性定义 */
  WEB_URL_FIELD_DEFINITION = 'WEB_URL_FIELD_DEFINITION',
  /** 架构层级属性定义 */
  STRUCTURE_FIELD_DEFINITION = 'STRUCTURE_FIELD_DEFINITION',

  // ==================== 属性配置 ====================
  /** 单行文本属性配置 */
  TEXT_FIELD = 'TEXT_FIELD',
  /** 多行文本属性配置 */
  MULTI_LINE_TEXT_FIELD = 'MULTI_LINE_TEXT_FIELD',
  /** Markdown属性配置 */
  MARKDOWN_FIELD = 'MARKDOWN_FIELD',
  /** 数字属性配置 */
  NUMBER_FIELD = 'NUMBER_FIELD',
  /** 日期属性配置 */
  DATE_FIELD = 'DATE_FIELD',
  /** 枚举属性配置 */
  ENUM_FIELD = 'ENUM_FIELD',
  /** 附件属性配置 */
  ATTACHMENT_FIELD = 'ATTACHMENT_FIELD',
  /** 网页链接属性配置 */
  WEB_URL_FIELD = 'WEB_URL_FIELD',
  /** 架构层级属性配置 */
  STRUCTURE_FIELD = 'STRUCTURE_FIELD',

  // ==================== 关联类型 ====================
  /** 关联类型 */
  LINK_TYPE = 'LINK_TYPE',
  /** 关联属性配置 */
  LINK_FIELD = 'LINK_FIELD',

  // ==================== 架构线定义 ====================
  /** 架构线定义 */
  STRUCTURE_DEFINITION = 'STRUCTURE_DEFINITION',

  // ==================== 视图 ====================
  /** 列表视图 */
  LIST_VIEW = 'LIST_VIEW',

  // ==================== 菜单定义 ====================
  /** 菜单分组 */
  MENU_GROUP = 'MENU_GROUP',

  // ==================== 模板 ====================
  /** 卡片详情页模板 */
  CARD_DETAIL_TEMPLATE = 'CARD_DETAIL_TEMPLATE',
  /** 卡片新建页模板 */
  CARD_CREATE_PAGE_TEMPLATE = 'CARD_CREATE_PAGE_TEMPLATE',
  /** 卡面定义 */
  CARD_FACE = 'CARD_FACE',

  // ==================== 价值流定义 ====================
  /** 价值流基线 */
  VALUE_STREAM = 'VALUE_STREAM',
  /** 价值流分支 */
  VALUE_STREAM_BRANCH = 'VALUE_STREAM_BRANCH',

  // ==================== 权限定义 ====================
  /** 权限配置 */
  CARD_PERMISSION = 'CARD_PERMISSION',

  // ==================== 计算公式定义 ====================
  /** 时间点公式定义 */
  TIME_POINT_FORMULA_DEFINITION = 'TIME_POINT_FORMULA_DEFINITION',
  /** 时间段公式定义 */
  TIME_RANGE_FORMULA_DEFINITION = 'TIME_RANGE_FORMULA_DEFINITION',
  /** 日期汇集公式定义 */
  DATE_COLLECTION_FORMULA_DEFINITION = 'DATE_COLLECTION_FORMULA_DEFINITION',
  /** 卡片汇集公式定义 */
  CARD_COLLECTION_FORMULA_DEFINITION = 'CARD_COLLECTION_FORMULA_DEFINITION',
  /** 数值运算公式定义 */
  NUMBER_CALCULATION_FORMULA_DEFINITION = 'NUMBER_CALCULATION_FORMULA_DEFINITION',

  // ==================== 卡片动作 ====================
  /** 卡片动作配置 */
  CARD_ACTION_CONFIG = 'CARD_ACTION_CONFIG',

  // ==================== 通知渠道配置 ====================
  /** 通知渠道配置 */
  NOTIFICATION_CHANNEL_CONFIG = 'NOTIFICATION_CHANNEL_CONFIG',

  // ==================== 通知模板 ====================
  /** 通知模板 */
  NOTIFICATION_TEMPLATE = 'NOTIFICATION_TEMPLATE',
}

/**
 * Schema 大类显示配置
 */
export const SchemaTypeConfig: Record<SchemaType, { label: string; requiresBelongTo: boolean }> = {
  [SchemaType.CARD_TYPE]: { label: '卡片类型', requiresBelongTo: false },
  [SchemaType.FIELD_DEFINITION]: { label: '属性定义', requiresBelongTo: false },
  [SchemaType.FIELD_CONFIG]: { label: '属性配置', requiresBelongTo: true },
  [SchemaType.VIEW]: { label: '视图', requiresBelongTo: false },
  [SchemaType.VALUE_STREAM]: { label: '价值流定义', requiresBelongTo: true },
  [SchemaType.LINK_TYPE]: { label: '关联类型', requiresBelongTo: false },
  [SchemaType.STRUCTURE_DEFINITION]: { label: '架构线定义', requiresBelongTo: false },
  [SchemaType.BIZ_RULE]: { label: '业务规则', requiresBelongTo: true },
  [SchemaType.FLOW_POLICY]: { label: '流转策略', requiresBelongTo: true },
  [SchemaType.CARD_DETAIL_TEMPLATE]: { label: '卡片详情页模板', requiresBelongTo: true },
  [SchemaType.CARD_CREATE_PAGE_TEMPLATE]: { label: '卡片新建页模板', requiresBelongTo: true },
  [SchemaType.CARD_FACE]: { label: '卡面定义', requiresBelongTo: true },
  [SchemaType.CARD_PERMISSION]: { label: '权限定义', requiresBelongTo: true },
  [SchemaType.MENU]: { label: '菜单定义', requiresBelongTo: false },
  [SchemaType.NOTIFICATION_TEMPLATE]: { label: '通知模板', requiresBelongTo: false },
  [SchemaType.NOTIFICATION_CHANNEL_CONFIG]: { label: '通知渠道配置', requiresBelongTo: false },
  [SchemaType.CARD_ACTION]: { label: '卡片动作', requiresBelongTo: true },
  [SchemaType.FORMULA_DEFINITION]: { label: '计算公式', requiresBelongTo: true },
}

/**
 * Schema 子类型显示配置
 */
export const SchemaSubTypeConfig: Record<SchemaSubType, { label: string; category: SchemaType }> = {
  // 卡片类型
  [SchemaSubType.TRAIT_CARD_TYPE]: { label: '属性集', category: SchemaType.CARD_TYPE },
  [SchemaSubType.ENTITY_CARD_TYPE]: { label: '实体类型', category: SchemaType.CARD_TYPE },
  // 属性定义
  [SchemaSubType.SINGLE_LINE_TEXT_FIELD_DEFINITION]: { label: '单行文本属性', category: SchemaType.FIELD_DEFINITION },
  [SchemaSubType.MULTI_LINE_TEXT_FIELD_DEFINITION]: { label: '多行文本属性', category: SchemaType.FIELD_DEFINITION },
  [SchemaSubType.MARKDOWN_FIELD_DEFINITION]: { label: 'Markdown属性', category: SchemaType.FIELD_DEFINITION },
  [SchemaSubType.NUMBER_FIELD_DEFINITION]: { label: '数字属性', category: SchemaType.FIELD_DEFINITION },
  [SchemaSubType.DATE_FIELD_DEFINITION]: { label: '日期属性', category: SchemaType.FIELD_DEFINITION },
  [SchemaSubType.ENUM_FIELD_DEFINITION]: { label: '枚举属性', category: SchemaType.FIELD_DEFINITION },
  [SchemaSubType.ATTACHMENT_FIELD_DEFINITION]: { label: '附件属性', category: SchemaType.FIELD_DEFINITION },
  [SchemaSubType.WEB_URL_FIELD_DEFINITION]: { label: '网页链接属性', category: SchemaType.FIELD_DEFINITION },
  [SchemaSubType.STRUCTURE_FIELD_DEFINITION]: { label: '架构层级属性', category: SchemaType.FIELD_DEFINITION },
  // 属性配置
  [SchemaSubType.TEXT_FIELD]: { label: '单行文本属性配置', category: SchemaType.FIELD_CONFIG },
  [SchemaSubType.MULTI_LINE_TEXT_FIELD]: { label: '多行文本属性配置', category: SchemaType.FIELD_CONFIG },
  [SchemaSubType.MARKDOWN_FIELD]: { label: 'Markdown属性配置', category: SchemaType.FIELD_CONFIG },
  [SchemaSubType.NUMBER_FIELD]: { label: '数字属性配置', category: SchemaType.FIELD_CONFIG },
  [SchemaSubType.DATE_FIELD]: { label: '日期属性配置', category: SchemaType.FIELD_CONFIG },
  [SchemaSubType.ENUM_FIELD]: { label: '枚举属性配置', category: SchemaType.FIELD_CONFIG },
  [SchemaSubType.ATTACHMENT_FIELD]: { label: '附件属性配置', category: SchemaType.FIELD_CONFIG },
  [SchemaSubType.WEB_URL_FIELD]: { label: '网页链接属性配置', category: SchemaType.FIELD_CONFIG },
  [SchemaSubType.STRUCTURE_FIELD]: { label: '架构层级属性配置', category: SchemaType.FIELD_CONFIG },
  // 关联类型
  [SchemaSubType.LINK_TYPE]: { label: '关联类型', category: SchemaType.LINK_TYPE },
  [SchemaSubType.LINK_FIELD]: { label: '关联属性配置', category: SchemaType.FIELD_CONFIG },
  // 架构线定义
  [SchemaSubType.STRUCTURE_DEFINITION]: { label: '架构线定义', category: SchemaType.STRUCTURE_DEFINITION },
  // 价值流定义
  [SchemaSubType.VALUE_STREAM]: { label: '价值流基线', category: SchemaType.VALUE_STREAM },
  [SchemaSubType.VALUE_STREAM_BRANCH]: { label: '价值流分支', category: SchemaType.VALUE_STREAM },
  // 视图
  [SchemaSubType.LIST_VIEW]: { label: '列表视图', category: SchemaType.VIEW },
  // 菜单
  [SchemaSubType.MENU_GROUP]: { label: '菜单分组', category: SchemaType.MENU },
  // 模板
  [SchemaSubType.CARD_DETAIL_TEMPLATE]: { label: '卡片详情页模板', category: SchemaType.CARD_DETAIL_TEMPLATE },
  [SchemaSubType.CARD_CREATE_PAGE_TEMPLATE]: { label: '卡片新建页模板', category: SchemaType.CARD_CREATE_PAGE_TEMPLATE },
  [SchemaSubType.CARD_FACE]: { label: '卡面定义', category: SchemaType.CARD_FACE },
  // 权限定义
  [SchemaSubType.CARD_PERMISSION]: { label: '权限配置', category: SchemaType.CARD_PERMISSION },
  // 卡片动作
  [SchemaSubType.CARD_ACTION_CONFIG]: { label: '卡片动作配置', category: SchemaType.CARD_ACTION },
  // 计算公式
  [SchemaSubType.TIME_POINT_FORMULA_DEFINITION]: { label: '时间点公式', category: SchemaType.FORMULA_DEFINITION },
  [SchemaSubType.TIME_RANGE_FORMULA_DEFINITION]: { label: '时间段公式', category: SchemaType.FORMULA_DEFINITION },
  [SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION]: { label: '日期汇集公式', category: SchemaType.FORMULA_DEFINITION },
  [SchemaSubType.CARD_COLLECTION_FORMULA_DEFINITION]: { label: '卡片汇集公式', category: SchemaType.FORMULA_DEFINITION },
  [SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION]: { label: '数值运算公式', category: SchemaType.FORMULA_DEFINITION },
  // 通知渠道配置
  [SchemaSubType.NOTIFICATION_CHANNEL_CONFIG]: { label: '通知渠道配置', category: SchemaType.NOTIFICATION_CHANNEL_CONFIG },
  // 通知模板
  [SchemaSubType.NOTIFICATION_TEMPLATE]: { label: '通知模板', category: SchemaType.NOTIFICATION_TEMPLATE },
}

/**
 * Schema 定义基类
 */
export interface SchemaDefinition {
  /** Schema 子类型（Jackson 多态类型标识字段） */
  schemaSubType: SchemaSubType
  /** ID（新建时可不传，由后端自动生成） */
  id?: string
  /** 组织 ID */
  orgId: string
  /** 名称 */
  name: string
  /** 描述 */
  description?: string
  /** 排序号 */
  sortOrder?: number
  /** 是否启用 */
  enabled: boolean
  /** 实体状态 */
  state: EntityState
  /** 内容版本号 */
  contentVersion: number
  /** 结构版本号 */
  structureVersion?: string
  /** 创建时间 */
  createdAt?: string
  /** 创建人 ID */
  createdBy?: string
  /** 更新时间 */
  updatedAt?: string
  /** 更新人 ID */
  updatedBy?: string
}

/**
 * 创建 Schema 请求
 */
export interface CreateSchemaRequest {
  definition: SchemaDefinition
}

/**
 * 更新 Schema 请求
 */
export interface UpdateSchemaRequest {
  definition?: SchemaDefinition
  expectedVersion?: number
}

/**
 * 字段变更类型
 */
export type FieldChangeType = 'ADDED' | 'MODIFIED' | 'REMOVED'

/**
 * 语义变更类型
 */
export type SemanticChangeType = 'ADDED' | 'MODIFIED' | 'REMOVED' | 'REORDERED'

/**
 * 字段级变更
 */
export interface FieldChange {
  /** 字段路径 */
  fieldPath: string
  /** 字段显示名 */
  fieldLabel: string
  /** 变更类型 */
  changeType: FieldChangeType
  /** 值类型 */
  valueType: string
  /** 旧值 */
  oldValue?: unknown
  /** 新值 */
  newValue?: unknown
}

/**
 * 语义级变更（复杂嵌套结构）
 */
export interface SemanticChange {
  /** 变更类别（如 ENUM_ITEM, COLUMN_CONFIG） */
  category: string
  /** 操作类型 */
  operation: SemanticChangeType
  /** 目标项标识 */
  targetId?: string
  /** 目标项名称 */
  targetName?: string
  /** 详细字段变更（当operation为MODIFIED时） */
  details?: FieldChange[]
}

/**
 * 变更详情
 */
export interface ChangeDetail {
  action: 'CREATE' | 'UPDATE' | 'DELETE'
  schemaType: string
  schemaSubType?: string
  /** 字段级变更（后端字段名为 changes） */
  changes: FieldChange[]
  /** 语义级变更 */
  semanticChanges: SemanticChange[]
}

/**
 * Schema 变更日志
 */
export interface SchemaChangelogDTO {
  id: number
  schemaId: string
  /** Schema 名称（用于显示附属 Schema 的变更时区分来源） */
  schemaName?: string
  schemaType: string
  action: 'CREATE' | 'UPDATE' | 'DELETE'
  contentVersion: number
  beforeSnapshot?: string
  afterSnapshot?: string
  changeSummary?: string
  changeDetail?: ChangeDetail
  changedAt: string
  changedBy?: string
  changedByName?: string
  traceId?: string
}

/**
 * Schema 引用摘要
 */
export interface SchemaReferenceSummaryDTO {
  schemaId: string
  schemaName: string
  schemaType: string
  outgoing: ReferenceNodeDTO[]
  incoming: ReferenceNodeDTO[]
}

/**
 * 引用节点
 */
export interface ReferenceNodeDTO {
  schemaId: string
  schemaName: string
  schemaType: string
  referenceType: 'COMPOSITION' | 'AGGREGATION'
}
