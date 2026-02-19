/**
 * 卡片相关类型定义
 */

// ==================== 基础类型 ====================

/**
 * 卡片生命周期状态
 */
export type CardStyle = 'ACTIVE' | 'ARCHIVED' | 'DISCARDED'

/**
 * 属性值基础接口
 */
interface FieldValueBase {
  /** 属性 ID */
  fieldId: string
  /** 是否可读 */
  readable: boolean
}

/**
 * 文本属性值
 */
export interface TextFieldValue extends FieldValueBase {
  type: 'TEXT'
  value: string | null
  maxStringLength?: number | null
}

/**
 * 数字属性值
 */
export interface NumberFieldValue extends FieldValueBase {
  type: 'NUMBER'
  value: number | null
}

/**
 * 日期属性值（时间戳）
 */
export interface DateFieldValue extends FieldValueBase {
  type: 'DATE'
  value: number | null
}

/**
 * 枚举属性值
 * 存储枚举选项的 ID 列表，显示时依赖 renderConfig 中的 options 获取标签
 */
export interface EnumFieldValue extends FieldValueBase {
  type: 'ENUM'
  value: string[] | null
}

/**
 * 关联属性值
 * 关联卡片通过 CardDTO.linkedCards 返回，而非作为属性值
 */
export interface LinkFieldValue extends FieldValueBase {
  type: 'LINK'
  value: unknown
}

/**
 * 关联卡片摘要（用于关联编辑器中显示可选卡片列表）
 */
export interface LinkedCard {
  /** 卡片 ID */
  cardId: string
  /** 卡片标题 */
  title: CardTitle
}

// 重新导出 link-field-utils 工具函数
export { buildLinkFieldId, parseLinkFieldId, getLinkTypeId, getLinkPosition, isValidLinkFieldId } from '@/utils/link-field-utils'
export type { LinkPosition } from '@/utils/link-field-utils'

/**
 * 结构属性值
 */
export interface StructureFieldValue extends FieldValueBase {
  type: 'STRUCTURE'
  value: unknown
}

/**
 * 网页链接属性值
 */
export interface WebLinkFieldValue extends FieldValueBase {
  type: 'WEB_URL'
  value: string | null
}

/**
 * 附件属性值
 */
export interface AttachmentFieldValue extends FieldValueBase {
  type: 'ATTACHMENT'
  value: unknown
}

/**
 * 属性值联合类型（多态）
 */
export type FieldValue =
  | TextFieldValue
  | NumberFieldValue
  | DateFieldValue
  | EnumFieldValue
  | LinkFieldValue
  | StructureFieldValue
  | WebLinkFieldValue
  | AttachmentFieldValue

/**
 * 拼接标题的单个部分
 */
export interface JointTitlePart {
  /** 名称（显示值） */
  name: string
}

/**
 * 拼接标题的多个部分组
 */
export interface JointTitleParts {
  parts: JointTitlePart[]
}

/**
 * 卡片标题（多态类型）
 * - PURE: 纯文本标题
 * - JOINT: 拼接标题
 */
export type CardTitle =
  | { type: 'PURE'; value: string; displayValue: string }
  | {
    type: 'JOINT'
    /** 原始标题值（用户编辑的部分） */
    value: string
    /** 前缀或后缀 */
    area: 'PREFIX' | 'SUFFIX'
    /** 拼接部分（前后缀内容） */
    multiParts: JointTitleParts[]
    /** 完整显示值（原始标题 + 拼接部分） */
    displayValue: string
  }

/**
 * 获取卡片标题的原始值（用于编辑）
 * 对于纯标题和拼接标题都返回 value
 */
export function getCardTitleValue(title: CardTitle | undefined): string {
  if (!title) return ''
  return title.value || ''
}

/**
 * 获取拼接部分的显示文本
 * 多个 part 之间用空格分隔
 */
export function getJointPartsText(title: CardTitle | undefined): string {
  if (!title || title.type !== 'JOINT') return ''
  if (!title.multiParts || title.multiParts.length === 0) return ''

  // 拼接所有部分的名称，用空格分隔
  return title.multiParts
    .flatMap(group => group.parts.map(part => part.name))
    .join(' ')
}

/**
 * 判断是否是拼接标题
 */
export function isJointTitle(title: CardTitle | undefined): boolean {
  return title?.type === 'JOINT'
}

/**
 * 卡片描述
 */
export interface CardDescription {
  value: string
}

// ==================== 卡片 DTO ====================

/**
 * 卡片 DTO（完整版，用于详情）
 * 注意：所有 ID 字段后端使用 @JsonValue 直接返回字符串
 */
export interface CardDTO {
  /** 卡片 ID */
  id: string
  /** 组织 ID */
  orgId: string
  /** 卡片类型 ID */
  typeId: string
  /** 卡片内置编号 */
  codeInOrg: number
  /** 卡片自定义编号 */
  customCode?: string
  /** 卡片标题（多态对象） */
  title: CardTitle
  /** 卡片描述 */
  description?: CardDescription
  /** 生命周期状态 */
  cardStyle: CardStyle
  /** 价值流定义 ID */
  streamId?: string
  /** 价值流状态 ID */
  statusId?: string
  /** 属性值映射 */
  fieldValues?: Record<string, FieldValue>
  /** 关联卡片 */
  linkedCards?: Record<string, CardDTO[]>
  /** 创建时间 */
  createdAt?: string
  /** 创建人 ID */
  createdBy?: string
  /** 更新时间 */
  updatedAt?: string
  /** 更新人 ID */
  updatedBy?: string
  /** 丢弃时间 */
  abandonedAt?: string
  /** 归档时间 */
  archivedAt?: string
}

// ==================== 卡片详情响应 ====================

import type { CardDetailTemplateDefinition } from './card-detail-template'
import type { FieldRenderConfig } from './view-data'
import type { CardFieldControls } from './field-control'

/**
 * 字段渲染元数据（与列表视图统一的渲染配置）
 */
export interface FieldRenderMeta {
  /** 属性定义 ID（用于匹配 card.fieldValues 的 key） */
  fieldId: string
  /** 字段名称 */
  name: string
  /** 渲染配置（与列表视图统一） */
  renderConfig?: FieldRenderConfig
}

/**
 * 卡片类型信息（用于头部显示）
 */
export interface CardTypeInfo {
  /** 卡片类型 ID */
  id: string
  /** 卡片类型名称 */
  name?: string
  /** 卡片类型图标（可能为 null） */
  icon?: string
  /** 卡片类型颜色（可能为 null） */
  color?: string
}

/**
 * 价值流状态信息（用于头部显示）
 */
export interface ValueStreamStatusInfo {
  /** 状态 ID */
  statusId: string
  /** 状态名称 */
  statusName: string
  /** 状态所属阶段类别：TODO, IN_PROGRESS, DONE, CANCELLED */
  stepKind?: 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'
  /** 状态颜色 */
  color?: string
}

/**
 * 卡片详情响应（含模板、字段渲染配置和字段控制配置）
 */
export interface CardDetailResponse {
  /** 卡片数据 */
  card: CardDTO
  /** 详情模板配置 */
  template: CardDetailTemplateDefinition
  /** 字段渲染元数据列表（与列表视图统一的渲染配置） */
  fieldRenderMetas?: FieldRenderMeta[]
  /** 字段控制配置（必填/只读） */
  fieldControls?: CardFieldControls
  /** 卡片类型信息（用于头部显示） */
  cardTypeInfo?: CardTypeInfo
  /** 价值流状态信息（用于头部显示，可能为 null） */
  valueStreamStatusInfo?: ValueStreamStatusInfo
}

// ==================== 请求类型 ====================

/**
 * 卡片标题请求（多态类型）
 * - PURE: 纯文本标题
 * - JOINT: 拼接标题
 */
export type CardTitleRequest =
  | { type: 'PURE'; value: string }
  | { type: 'JOINT'; name: string; area: 'PREFIX' | 'SUFFIX'; multiParts: unknown[] }

/**
 * 创建纯文本标题
 */
export function pureTitle(value: string): CardTitleRequest {
  return { type: 'PURE', value }
}

/**
 * 创建卡片请求
 * 注意：orgId/typeId 序列化为直接字符串（后端使用 @JsonValue）
 * operatorId 通过请求头 X-Member-Card-Id 传递
 */
export interface CreateCardRequest {
  /** 组织 ID（直接字符串） */
  orgId: string
  /** 卡片类型 ID（直接字符串） */
  typeId: string
  /** 卡片标题 */
  title: CardTitleRequest
  /** 卡片描述 */
  description?: string
  /** 属性值 */
  fieldValues?: Record<string, FieldValue>
  /** 关联属性（可选，覆盖式更新） */
  linkUpdates?: LinkFieldUpdate[]
}

/**
 * 关联属性更新
 */
export interface LinkFieldUpdate {
  /** 关联属性ID，格式为 "{linkTypeId}:{SOURCE|TARGET}" */
  linkFieldId: string
  /** 目标卡片ID列表，空列表表示清空 */
  targetCardIds: string[]
}

/**
 * 更新卡片请求
 * operatorId 通过请求头 X-Member-Card-Id 传递
 * 
 * 注意：title 为原始标题字符串，不影响拼接标题部分
 * 拼接标题的更新由后端通过 Kafka 事件异步处理
 */
export interface UpdateCardRequest {
  /** 卡片 ID（直接字符串） */
  cardId: string
  /** 卡片标题（可选，只更新原始标题值） */
  title?: string
  /** 卡片描述 */
  description?: string
  /** 属性值（增量更新） */
  fieldValues?: Record<string, FieldValue>
}

/**
 * 批量操作请求
 */
export interface BatchOperationRequest {
  /** 卡片 ID 列表 */
  cardIds: string[]
  /** 操作原因 */
  reason?: string
}

/**
 * 移动卡片请求
 */
export interface MoveCardRequest {
  /** 卡片 ID */
  cardId: string
  /** 价值流 ID */
  streamId: string
  /** 目标状态 ID */
  toStatusId: string
}

/**
 * 返回字段控制
 */
export interface Yield {
  /** 当前卡片需要返回的字段 */
  field?: YieldField
  /** 需要返回的关联卡片 */
  links?: YieldLink[]
}

/**
 * 字段返回定义
 */
export interface YieldField {
  /** 是否返回所有字段 */
  allFields?: boolean
  /** 指定返回的字段 ID 列表 */
  fieldIds?: string[]
}

/**
 * 关联卡片返回定义
 */
export interface YieldLink {
  /** 关联字段 ID */
  linkFieldId: string
  /** 目标卡片的返回定义 */
  targetYield?: Yield
}

// ==================== 工具函数 ====================

/**
 * 获取卡片编号显示
 */
export function getCardCode(card: CardDTO): string {
  return card.customCode || String(card.codeInOrg)
}

/**
 * 获取卡片标题（显示值）
 */
export function getCardTitle(card: CardDTO): string {
  return card.title?.displayValue || ''
}

/**
 * 获取卡片 ID 字符串
 */
export function getCardId(card: CardDTO): string {
  return card.id;
}

/**
 * 创建空的创建卡片请求
 */
export function createEmptyCreateRequest(orgId: string, typeId: string): CreateCardRequest {
  return {
    orgId,
    typeId,
    title: pureTitle(''),
    description: '',
    fieldValues: {},
  }
}

/**
 * 创建空的更新卡片请求
 */
export function createEmptyUpdateRequest(cardId: string, title?: string): UpdateCardRequest {
  return {
    cardId,
    title,
    description: '',
    fieldValues: {},
  }
}

// ==================== 查询相关类型 ====================

/**
 * 查询上下文
 */
export interface QueryContext {
  orgId: string
  operatorId: string
}

/**
 * 查询范围
 */
export interface QueryScope {
  cardTypeIds: string[]
  cardStyles: CardStyle[]
}

/**
 * 查询条件（简化版，支持 LINK 类型）
 */
export interface QueryCondition {
  type: 'LINK' | 'AND' | 'OR' | 'NOT'
  linkFieldId?: string
  targetCardIds?: string[]
  conditions?: QueryCondition[]
}

/**
 * 排序配置
 */
export interface SortConfig {
  field: string
  order: 'ASC' | 'DESC'
}

/**
 * 分页配置
 */
export interface PageConfig {
  pageNum: number
  pageSize: number
}

/**
 * 排序和分页
 */
export interface SortAndPage {
  sort?: SortConfig[]
  page?: PageConfig
}

/**
 * 卡片查询请求
 */
export interface CardQueryRequest {
  queryContext: QueryContext
  queryScope: QueryScope
  condition?: QueryCondition
  yield?: Yield
}

/**
 * 卡片分页查询请求
 */
export interface CardPageQueryRequest extends CardQueryRequest {
  sortAndPage?: SortAndPage
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  pageNum: number
  pageSize: number
}
