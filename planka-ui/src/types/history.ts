/**
 * 操作历史类型定义
 */

import type { CardTitle } from './card'

/**
 * 操作类型
 */
export type OperationType =
  | 'CARD_CREATED'
  | 'CARD_ARCHIVED'
  | 'CARD_ABANDONED'
  | 'CARD_RESTORED'
  | 'FIELD_CUSTOM_UPDATED'
  | 'FIELD_TITLE_UPDATED'
  | 'FIELD_DESC_UPDATED'
  | 'FIELD_STATUS_UPDATED'
  | 'FIELD_MEMBER_UPDATED'
  | 'STREAM_MOVED'
  | 'LINK_ADDED'
  | 'LINK_REMOVED'
  | 'COMMENT_ADDED'
  | 'COMMENT_UPDATED'
  | 'COMMENT_DELETED'
  | 'ATTACHMENT_UPLOADED'
  | 'ATTACHMENT_DELETED'

/**
 * 操作来源类型
 */
export type OperationSourceType =
  | 'USER'
  | 'BIZ_RULE'
  | 'THIRD_PARTY'
  | 'FIELD_LINKAGE'
  | 'SCHEDULED_TASK'
  | 'IMPORT'
  | 'API_CALL'

/**
 * 操作来源
 */
export interface OperationSource {
  type: OperationSourceType
  // 可选的额外信息
  ruleName?: string
  systemName?: string
  taskName?: string
  fileName?: string
  apiName?: string
}

/**
 * 历史参数类型
 */
export type HistoryArgumentType =
  | 'TEXT'
  | 'TEXT_DIFF'
  | 'OPERATE_FIELD'
  | 'STATUS'
  | 'FIELD_VALUE_TEXT'
  | 'FIELD_VALUE_NUMBER'
  | 'FIELD_VALUE_DATE'
  | 'FIELD_VALUE_DATETIME'
  | 'FIELD_VALUE_ENUM'
  | 'FIELD_VALUE_STRUCTURE'
  | 'FIELD_VALUE_LINK'

/**
 * 差异行类型
 */
export type DiffLineType = 'CONTEXT' | 'ADD' | 'DELETE'

/**
 * 差异行
 */
export interface DiffLine {
  type: DiffLineType
  content: string
}

/**
 * 差异块
 */
export interface DiffHunk {
  oldStart: number
  oldCount: number
  newStart: number
  newCount: number
  lines: DiffLine[]
}

/**
 * 关联卡片引用（用于 FIELD_VALUE_LINK 类型）
 */
export interface LinkedCardRef {
  cardId: string
  cardTitle?: CardTitle  // 由后端查询填充，支持拼接标题
  cardTypeId: string
}

/**
 * 国际化消息参数（后端 HistoryArgumentVO）
 */
export interface HistoryArgument {
  type: HistoryArgumentType
  // 通用字段
  value?: string
  displayValue?: string
  deleted?: boolean
  // OPERATE_FIELD / FIELD_VALUE_* 类型
  fieldId?: string
  fieldName?: string
  // STATUS 类型
  statusId?: string
  statusName?: string
  // FIELD_VALUE_LINK 类型
  cards?: LinkedCardRef[]
  // TEXT_DIFF 类型
  hunks?: DiffHunk[]
}

/**
 * 国际化消息
 */
export interface HistoryMessage {
  messageKey: string
  args: HistoryArgument[]
}

/**
 * 操作历史记录
 */
export interface CardHistoryRecord {
  id: string
  cardId: number
  operationType: OperationType
  operatorId: string
  /** 操作人显示名称（后端从成员卡片名称填充） */
  operatorName: string
  operatorIp: string
  operationSource: OperationSource
  message: HistoryMessage
  traceId: string
  createdAt: string
}

/**
 * 历史记录搜索请求
 */
export interface HistorySearchRequest {
  operationTypes?: OperationType[]
  operatorIds?: string[]
  sourceTypes?: OperationSourceType[]
  startTime?: string
  endTime?: string
  /** true=正序（最早的在前），false/undefined=倒序（最新的在前，默认） */
  sortAsc?: boolean
  page?: number
  size?: number
}

/**
 * 历史记录筛选选项
 */
export interface HistoryFilters {
  operatorIds: string[]
  operationTypes: OperationType[]
  sourceTypes: string[]
}

// 注意：操作类型和操作来源的显示标签已移至 i18n 语言包
// 使用 t('history.operation.XXX') 和 t('history.source.XXX') 获取本地化标签
