/**
 * 权限配置类型定义
 *
 * 与后端 dev.planka.domain.schema.definition.permission 包对应
 * 采用白名单模式：满足任一条件即拥有权限
 */

import type { Condition } from './condition'
import type { SchemaDefinition } from './schema'
import { SchemaSubType } from './schema'

// ==================== 权限配置定义（Schema） ====================

/**
 * 权限配置定义（独立 Schema）
 *
 * 对应后端 PermissionConfigDefinition
 * belongTo: cardTypeId（所属卡片类型）
 */
export interface PermissionConfigDefinition extends SchemaDefinition {
  schemaSubType: SchemaSubType.CARD_PERMISSION
  /** 所属卡片类型ID */
  cardTypeId: string
  /** 卡片操作权限列表 */
  cardOperations?: CardOperationPermission[]
  /** 属性级别权限列表 */
  fieldPermissions?: FieldPermission[]
  /** 附件权限列表 */
  attachmentPermissions?: AttachmentPermission[]
}

// ==================== 权限配置（运行时 POJO） ====================

/**
 * 权限配置
 */
export interface PermissionConfig {
  /** 卡片操作权限列表 */
  cardOperations?: CardOperationPermission[]
  /** 属性级别权限列表 */
  fieldPermissions?: FieldPermission[]
  /** 附件权限列表 */
  attachmentPermissions?: AttachmentPermission[]
}

// ==================== 卡片操作权限 ====================

/**
 * 卡片操作权限
 *
 * 采用白名单模式：满足任一卡片条件 AND 满足任一操作人条件 → 有权限
 */
export interface CardOperationPermission {
  /** 操作类型 */
  operation: CardOperation
  /** 卡片条件列表（多个条件为"或"关系，满足任一即可） */
  cardConditions?: Condition[]
  /** 操作人条件列表（多个条件为"或"关系，满足任一即可） */
  operatorConditions?: Condition[]
  /** 权限不通过时的提示信息 */
  alertMessage?: string
}

/**
 * 卡片操作类型
 */
export enum CardOperation {
  /** 创建卡片 */
  CREATE = 'CREATE',
  /** 查看卡片 */
  READ = 'READ',
  /** 编辑卡片 */
  EDIT = 'EDIT',
  /** 移动卡片（改变状态） */
  MOVE = 'MOVE',
  /** 回退卡片（改变状态） */
  ROLLBACK = 'ROLLBACK',
  /** 归档卡片 */
  ARCHIVE = 'ARCHIVE',
  /** 丢弃卡片 */
  DISCARD = 'DISCARD',
}

// ==================== 属性权限 ====================

/**
 * 属性级别权限
 *
 * 采用白名单模式：满足任一卡片条件 AND 满足任一操作人条件 → 有权限
 */
export interface FieldPermission {
  /** 属性操作类型 */
  operation: FieldOperation
  /** 属性配置ID列表 */
  fieldIds: string[]
  /** 卡片条件列表（多个条件为"或"关系，满足任一即可） */
  cardConditions?: Condition[]
  /** 操作人条件列表（多个条件为"或"关系，满足任一即可） */
  operatorConditions?: Condition[]
  /** 权限不通过时的提示信息 */
  alertMessage?: string
}

/**
 * 属性操作类型
 */
export enum FieldOperation {
  /** 查看属性 */
  READ = 'READ',
  /** 脱敏查看 */
  DESENSITIZED_READ = 'DESENSITIZED_READ',
  /** 编辑属性 */
  EDIT = 'EDIT',
}

// ==================== 附件权限 ====================

/**
 * 附件属性权限
 *
 * 采用白名单模式：满足任一卡片条件 AND 满足任一操作人条件 → 有权限
 */
export interface AttachmentPermission {
  /** 附件操作类型 */
  attachmentOperation: AttachmentOperation
  /** 属性配置ID列表（针对附件类型的属性） */
  fieldIds: string[]
  /** 卡片条件列表（多个条件为"或"关系，满足任一即可） */
  cardConditions?: Condition[]
  /** 操作人条件列表（多个条件为"或"关系，满足任一即可） */
  operatorConditions?: Condition[]
  /** 权限不通过时的提示信息 */
  alertMessage?: string
}

/**
 * 附件操作类型
 */
export enum AttachmentOperation {
  /** 上传 */
  UPLOAD = 'UPLOAD',
  /** 下载 */
  DOWNLOAD = 'DOWNLOAD',
  /** 编辑 */
  EDIT = 'EDIT',
  /** 预览 */
  PREVIEW = 'PREVIEW',
  /** 删除 */
  DELETE = 'DELETE',
}

// ==================== 显示标签 ====================

/**
 * 卡片操作类型显示标签
 */
export const CardOperationLabels: Record<CardOperation, string> = {
  [CardOperation.CREATE]: '创建卡片',
  [CardOperation.READ]: '查看卡片',
  [CardOperation.EDIT]: '编辑卡片',
  [CardOperation.MOVE]: '移动状态',
  [CardOperation.ROLLBACK]: '回退状态',
  [CardOperation.ARCHIVE]: '归档卡片',
  [CardOperation.DISCARD]: '丢弃卡片',
}

/**
 * 属性操作类型显示标签
 */
export const FieldOperationLabels: Record<FieldOperation, string> = {
  [FieldOperation.READ]: '查看',
  [FieldOperation.DESENSITIZED_READ]: '脱敏查看',
  [FieldOperation.EDIT]: '编辑',
}

/**
 * 附件操作类型显示标签
 */
export const AttachmentOperationLabels: Record<AttachmentOperation, string> = {
  [AttachmentOperation.UPLOAD]: '上传',
  [AttachmentOperation.DOWNLOAD]: '下载',
  [AttachmentOperation.EDIT]: '编辑',
  [AttachmentOperation.PREVIEW]: '预览',
  [AttachmentOperation.DELETE]: '删除',
}
