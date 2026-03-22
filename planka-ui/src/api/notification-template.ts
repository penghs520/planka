import request from './request'
import type { SchemaDefinition } from '@/types/schema'
import { SchemaSubType } from '@/types/schema'

const BASE_URL = '/api/v1/schemas/notification-templates'

/**
 * 模板类型
 */
export type TemplateType = 'BUILTIN' | 'CUSTOM'

/**
 * 定义参数类型
 */
export type DefinitionParameterType = 'CARD_TYPE' | 'DATE' | 'TEXT' | 'MULTILINE_TEXT' | 'LINK' | 'NUMBER'

/**
 * 实体类型定义参数
 */
export interface CardTypeDefinitionParameter {
  type: 'CARD_TYPE'
  cardTypeId: string
  cardTypeName?: string
}

/**
 * 简单定义参数
 */
export interface SimpleDefinitionParameter {
  type: 'DATE' | 'TEXT' | 'MULTILINE_TEXT' | 'LINK' | 'NUMBER'
  name: string
}

/**
 * 定义参数（联合类型）
 */
export type DefinitionParameter = CardTypeDefinitionParameter | SimpleDefinitionParameter

/**
 * 接收者选择器类型
 */
export type SelectorType = 'CURRENT_OPERATOR' | 'FIXED_MEMBERS' | 'FROM_FIELD' | 'CARD_WATCHERS' | 'LINKED_CARD_FIELD'

/**
 * 通知对象类型
 */
export type RecipientType = 'MEMBER' | 'GROUP'

/**
 * 触发事件类型
 */
export type TriggerEvent = 'ON_CREATE' | 'ON_DISCARD' | 'ON_ARCHIVE' | 'ON_RESTORE' | 'ON_STATUS_MOVE' | 'ON_STATUS_ROLLBACK' | 'ON_FIELD_CHANGE' | 'ON_SCHEDULE'

/**
 * 选择器项（多选择器模式）
 */
export interface SelectorItem {
  /** 选择类型 */
  selectorType: SelectorType
  /** 固定成员ID列表 */
  memberIds?: string[]
  /** 字段ID */
  fieldId?: string
  /** 关联路径 */
  linkPath?: { linkNodes: string[] }
  /** 来源标识，用于区分操作人属性和卡片字段 */
  source?: 'OPERATOR' | 'CARD'
}

/**
 * 接收者选择器
 */
export interface RecipientSelector {
  /** 多选择器模式（推荐） */
  selectors?: SelectorItem[]
  /** 选择类型（单选择器模式，向后兼容） */
  selectorType?: SelectorType
  /** 固定成员ID列表 */
  memberIds?: string[]
  /** 字段ID */
  fieldId?: string
  /** 字段ID列表（多选） */
  fieldIds?: string[]
  /** 是否包含系统人员 */
  includeSystemUsers?: boolean
  /** 系统人员ID列表 */
  systemUserIds?: string[]
}

/**
 * 通知内容类型
 */
export type NotificationContentType = 'SHORT' | 'LONG'

/**
 * 短通知内容
 */
export interface ShortNotificationContent {
  type: 'SHORT'
  /** 文本表达式模板 */
  textTemplate: string
}

/**
 * 长通知内容
 */
export interface LongNotificationContent {
  type: 'LONG'
  /** 抄送人选择器 */
  ccSelector?: RecipientSelector
  /** 富文本表达式模板 */
  richTextTemplate: string
}

/**
 * 通知内容（联合类型）
 */
export type NotificationContent = ShortNotificationContent | LongNotificationContent

/**
 * 通知模板定义
 */
export interface NotificationTemplateDefinition extends SchemaDefinition {
  schemaSubType: SchemaSubType.NOTIFICATION_TEMPLATE
  /** 模板类型（内置/自定义） */
  templateType?: TemplateType
  /** 定义参数 */
  definitionParameter: DefinitionParameter
  /** 所属实体类型ID（兼容字段） */
  cardTypeId?: string
  /** 所属实体类型名称（兼容字段） */
  cardTypeName?: string
  /** 触发事件类型 */
  triggerEvent: TriggerEvent
  /** 触发事件显示名称 */
  triggerEventName?: string
  /** 通知对象类型 */
  recipientType: RecipientType
  /** 接收者选择器 */
  recipientSelector: RecipientSelector
  /** 适用的通知渠道列表 */
  channels: string[]
  /** 通知标题模板 */
  titleTemplate: string
  /** 通知内容 */
  content: NotificationContent
}

/**
 * 通知内容请求
 */
export interface NotificationContentRequest {
  /** 内容类型：SHORT-短内容, LONG-长内容 */
  type: NotificationContentType
  /** 文本模板（短内容时使用） */
  textTemplate?: string
  /** 富文本模板（长内容时使用） */
  richTextTemplate?: string
  /** 抄送人选择器（长内容时使用） */
  ccSelector?: RecipientSelector
}

/**
 * 创建通知模板请求
 */
export interface CreateNotificationTemplateRequest {
  name: string
  templateType?: TemplateType
  definitionParameter: DefinitionParameter
  triggerEvent: TriggerEvent
  recipientType: RecipientType
  recipientSelector: RecipientSelector
  strongNotification?: boolean
  channels: string[]
  titleTemplate: string
  content: NotificationContentRequest
}

/**
 * 更新通知模板请求
 */
export interface UpdateNotificationTemplateRequest {
  name: string
  templateType?: TemplateType
  definitionParameter: DefinitionParameter
  triggerEvent: TriggerEvent
  recipientType: RecipientType
  recipientSelector: RecipientSelector
  strongNotification?: boolean
  channels: string[]
  titleTemplate: string
  content: NotificationContentRequest
  enabled: boolean
  expectedVersion?: number
}

/**
 * 定义参数类型选项
 */
export const DEFINITION_PARAMETER_TYPE_OPTIONS: { value: DefinitionParameterType; label: string }[] = [
  { value: 'CARD_TYPE', label: 'admin.notificationSettings.template.parameterType.cardType' },
  { value: 'DATE', label: 'admin.notificationSettings.template.parameterType.date' },
  { value: 'TEXT', label: 'admin.notificationSettings.template.parameterType.text' },
  { value: 'MULTILINE_TEXT', label: 'admin.notificationSettings.template.parameterType.multilineText' },
  { value: 'LINK', label: 'admin.notificationSettings.template.parameterType.link' },
  { value: 'NUMBER', label: 'admin.notificationSettings.template.parameterType.number' },
]

/**
 * 触发事件选项
 */
export const TRIGGER_EVENT_OPTIONS: { value: TriggerEvent; label: string }[] = [
  { value: 'ON_CREATE', label: 'admin.bizRule.triggerEvent.ON_CREATE' },
  { value: 'ON_DISCARD', label: 'admin.bizRule.triggerEvent.ON_DISCARD' },
  { value: 'ON_ARCHIVE', label: 'admin.bizRule.triggerEvent.ON_ARCHIVE' },
  { value: 'ON_RESTORE', label: 'admin.bizRule.triggerEvent.ON_RESTORE' },
  { value: 'ON_STATUS_MOVE', label: 'admin.bizRule.triggerEvent.ON_STATUS_MOVE' },
  { value: 'ON_STATUS_ROLLBACK', label: 'admin.bizRule.triggerEvent.ON_STATUS_ROLLBACK' },
  { value: 'ON_FIELD_CHANGE', label: 'admin.bizRule.triggerEvent.ON_FIELD_CHANGE' },
  { value: 'ON_SCHEDULE', label: 'admin.bizRule.triggerEvent.ON_SCHEDULE' },
]

/**
 * 通知对象类型选项
 */
export const RECIPIENT_TYPE_OPTIONS: { value: RecipientType; label: string }[] = [
  { value: 'MEMBER', label: 'admin.notificationSettings.template.recipientType.member' },
  { value: 'GROUP', label: 'admin.notificationSettings.template.recipientType.group' },
]

/**
 * 选择器类型选项
 */
export const SELECTOR_TYPE_OPTIONS: { value: SelectorType; label: string }[] = [
  { value: 'CURRENT_OPERATOR', label: 'admin.notificationSettings.template.selectorType.currentOperator' },
  { value: 'FIXED_MEMBERS', label: 'admin.notificationSettings.template.selectorType.fixedMembers' },
  { value: 'FROM_FIELD', label: 'admin.notificationSettings.template.selectorType.fromField' },
  { value: 'CARD_WATCHERS', label: 'admin.notificationSettings.template.selectorType.cardWatchers' },
]

/**
 * 通知模板 API
 */
export const notificationTemplateApi = {
  /**
   * 获取模板列表
   */
  list(): Promise<NotificationTemplateDefinition[]> {
    return request.get(BASE_URL)
  },

  /**
   * 根据实体类型获取模板列表
   */
  listByCardType(cardTypeId: string): Promise<NotificationTemplateDefinition[]> {
    return request.get(`${BASE_URL}/by-card-type/${cardTypeId}`)
  },

  /**
   * 根据 ID 获取模板
   */
  getById(id: string): Promise<NotificationTemplateDefinition> {
    return request.get(`${BASE_URL}/${id}`)
  },

  /**
   * 创建模板
   */
  create(data: CreateNotificationTemplateRequest): Promise<NotificationTemplateDefinition> {
    return request.post(BASE_URL, data)
  },

  /**
   * 更新模板
   */
  update(id: string, data: UpdateNotificationTemplateRequest): Promise<NotificationTemplateDefinition> {
    return request.put(`${BASE_URL}/${id}`, data)
  },

  /**
   * 删除模板
   */
  delete(id: string): Promise<void> {
    return request.delete(`${BASE_URL}/${id}`)
  },

  /**
   * 启用模板
   */
  activate(id: string): Promise<void> {
    return request.put(`${BASE_URL}/${id}/activate`)
  },

  /**
   * 停用模板
   */
  disable(id: string): Promise<void> {
    return request.put(`${BASE_URL}/${id}/disable`)
  },
}
