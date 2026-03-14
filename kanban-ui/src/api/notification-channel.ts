import request from './request'
import type { SchemaDefinition } from '@/types/schema'
import { SchemaSubType } from '@/types/schema'

const BASE_URL = '/api/v1/schemas/notification-channels'

/**
 * 通知渠道配置定义
 */
export interface NotificationChannelConfigDefinition extends SchemaDefinition {
  schemaSubType: SchemaSubType.NOTIFICATION_CHANNEL_CONFIG
  /** 渠道类型标识：builtin, email, feishu, dingtalk, wecom */
  channelId: string
  /** 渠道配置参数 */
  config: Record<string, unknown>
  /** 是否为默认渠道 */
  isDefault: boolean
  /** 优先级 */
  priority: number
}

/**
 * 创建通知渠道请求
 */
export interface CreateNotificationChannelRequest {
  name: string
  channelId: string
  config: Record<string, unknown>
  isDefault?: boolean
  priority?: number
}

/**
 * 更新通知渠道请求
 */
export interface UpdateNotificationChannelRequest {
  name: string
  config: Record<string, unknown>
  isDefault?: boolean
  priority?: number
  expectedVersion?: number
}

/**
 * 通知渠道 API
 */
export const notificationChannelApi = {
  /**
   * 获取渠道列表
   */
  list(): Promise<NotificationChannelConfigDefinition[]> {
    return request.get(BASE_URL)
  },

  /**
   * 根据 ID 获取渠道
   */
  getById(id: string): Promise<NotificationChannelConfigDefinition> {
    return request.get(`${BASE_URL}/${id}`)
  },

  /**
   * 创建渠道
   */
  create(data: CreateNotificationChannelRequest): Promise<NotificationChannelConfigDefinition> {
    return request.post(BASE_URL, data)
  },

  /**
   * 更新渠道
   */
  update(id: string, data: UpdateNotificationChannelRequest): Promise<NotificationChannelConfigDefinition> {
    return request.put(`${BASE_URL}/${id}`, data)
  },

  /**
   * 删除渠道
   */
  delete(id: string): Promise<void> {
    return request.delete(`${BASE_URL}/${id}`)
  },

  /**
   * 启用渠道
   */
  activate(id: string): Promise<void> {
    return request.put(`${BASE_URL}/${id}/activate`)
  },

  /**
   * 停用渠道
   */
  disable(id: string): Promise<void> {
    return request.put(`${BASE_URL}/${id}/disable`)
  },
}
