import request from './request'
import type { SchemaDefinition } from '@/types/schema'
import { SchemaSubType } from '@/types/schema'

const BASE_URL = '/api/v1/schemas/notification-templates'

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
 * 接收者选择器
 */
export interface RecipientSelector {
  /** 选择类型 */
  selectorType: SelectorType
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
 * 通知模板定义
 */
export interface NotificationTemplateDefinition extends SchemaDefinition {
  schemaSubType: SchemaSubType.NOTIFICATION_TEMPLATE
  /** 所属卡片类型ID */
  cardTypeId: string
  /** 所属卡片类型名称 */
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
  /** 短内容模板（用于 IM/系统通知） */
  shortContent?: string
  /** 长内容模板（用于邮件） */
  longContent?: string
  /** 优先级 */
  priority: number
}

/**
 * 创建通知模板请求
 */
export interface CreateNotificationTemplateRequest {
  name: string
  cardTypeId: string
  triggerEvent: TriggerEvent
  recipientType: RecipientType
  recipientSelector: RecipientSelector
  channels: string[]
  titleTemplate: string
  shortContent?: string
  longContent?: string
  priority?: number
}

/**
 * 更新通知模板请求
 */
export interface UpdateNotificationTemplateRequest {
  name: string
  cardTypeId: string
  triggerEvent: TriggerEvent
  recipientType: RecipientType
  recipientSelector: RecipientSelector
  channels: string[]
  titleTemplate: string
  shortContent?: string
  longContent?: string
  priority: number
  enabled: boolean
  expectedVersion?: number
}

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
   * 根据卡片类型获取模板列表
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
