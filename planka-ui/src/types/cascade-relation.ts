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
 * 级联层级
 */
export interface CascadeRelationLevel {
  /** 层级索引（0为根层级） */
  index: number
  /** 层级名称（如"部落"、"小队"） */
  name: string
  /** 关联的实体类型 ID（每层仅一个） */
  cardTypeId: string
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
 * 级联关系定义
 */
export interface CascadeRelationDefinition extends SchemaDefinition {
  schemaSubType: SchemaSubType.CASCADE_RELATION_DEFINITION
  /** 级联关系层级列表（有序，从根到叶） */
  levels: CascadeRelationLevel[]
  /** 是否系统内置 */
  systemCascadeRelation: boolean
}

/**
 * 创建/更新级联关系请求
 */
export interface CascadeRelationDefinitionRequest {
  name: string
  description?: string
  levels: CascadeRelationLevel[]
}
