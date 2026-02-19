import type { SchemaDefinition } from './schema'
import { SchemaSubType } from './schema'

/**
 * 排序方向
 */
export enum SortDirection {
  ASC = 'ASC',
  DESC = 'DESC',
}

/**
 * 架构层级
 */
export interface StructureLevel {
  /** 层级索引（0为根层级） */
  index: number
  /** 层级名称（如"部落"、"小队"） */
  name: string
  /** 关联的卡片类型ID列表（支持多选） */
  cardTypeIds: string[]
  /** 与上级的关联属性ID（根层级为null），格式: {linkTypeId}:{SOURCE|TARGET} */
  parentLinkFieldId: string | null
  /** 负责人关联属性ID（可选），格式: {linkTypeId}:{SOURCE|TARGET} */
  ownerLinkFieldId?: string
  /** 排序字段ID（可选） */
  sortFieldId?: string
  /** 排序方向（可选，默认升序） */
  sortDirection?: SortDirection
}

/**
 * 架构线定义
 */
export interface StructureDefinition extends SchemaDefinition {
  schemaSubType: SchemaSubType.STRUCTURE_DEFINITION
  /** 架构线层级列表（有序，从根到叶） */
  levels: StructureLevel[]
  /** 是否系统内置 */
  systemStructure: boolean
}

/**
 * 创建/更新架构线请求
 */
export interface StructureDefinitionRequest {
  name: string
  description?: string
  levels: StructureLevel[]
}
