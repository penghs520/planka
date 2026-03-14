/**
 * 关联位置枚举
 */
export enum LinkPosition {
  SOURCE = 'SOURCE',
  TARGET = 'TARGET',
}

/**
 * 卡片类型信息
 */
export interface CardTypeInfo {
  id: string
  name: string
}

/**
 * 关联类型 VO
 */
export interface LinkTypeVO {
  id: string
  orgId: string
  name: string
  description?: string
  sourceName: string
  targetName: string
  sourceVisible: boolean
  targetVisible: boolean
  sourceCardTypes?: CardTypeInfo[]
  targetCardTypes?: CardTypeInfo[]
  sourceMultiSelect: boolean
  targetMultiSelect: boolean
  systemLinkType: boolean
  enabled: boolean
  contentVersion: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 关联类型选项 VO（用于下拉框）
 */
export interface LinkTypeOptionVO {
  id: string
  name: string
  sourceName: string
  targetName: string
  sourceVisible: boolean
  targetVisible: boolean
  sourceMultiSelect: boolean
  targetMultiSelect: boolean
}

/**
 * 创建关联类型请求
 */
export interface CreateLinkTypeRequest {
  name: string
  description?: string
  sourceName: string
  targetName: string
  sourceVisible?: boolean
  targetVisible?: boolean
  sourceCardTypeIds?: string[]
  targetCardTypeIds?: string[]
  sourceMultiSelect?: boolean
  targetMultiSelect?: boolean
}

/**
 * 更新关联类型请求
 */
export interface UpdateLinkTypeRequest {
  name?: string
  description?: string
  sourceName?: string
  targetName?: string
  sourceVisible?: boolean
  targetVisible?: boolean
  sourceCardTypeIds?: string[]
  targetCardTypeIds?: string[]
  sourceMultiSelect?: boolean
  targetMultiSelect?: boolean
  enabled?: boolean
  expectedVersion?: number
}

// Re-export LinkFieldConfig from field-config for backward compatibility
export type { LinkFieldConfig } from './field-config'
