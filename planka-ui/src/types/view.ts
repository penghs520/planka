import type { SchemaDefinition } from './schema'
import { SchemaSubType } from './schema'
import { EntityState } from './common'
import type { Condition } from './condition'
// ==================== 列表视图定义 ====================

/**
 * 列表视图定义
 */
export interface ListViewDefinition extends SchemaDefinition {
  /** Schema 子类型标识 */
  schemaSubType: SchemaSubType.LIST_VIEW

  /** 关联的卡片类型 ID */
  cardTypeId: string

  /** 列配置列表 */
  columnConfigs?: ColumnConfig[]

  /** 分组字段 ID（可选） */
  groupBy?: string

  /** 排序配置列表 */
  sorts?: SortField[]

  /** 分页配置 */
  pageConfig?: PageConfig

  /** 过滤条件（支持复杂的嵌套逻辑） */
  condition?: Condition

  // AbstractViewDefinition 字段
  /** 是否为默认视图 */
  defaultView?: boolean

  /** 是否共享（其他用户可见） */
  shared?: boolean

  /** 可见性范围（用户 ID 列表，空表示所有人可见） */
  visibleTo?: string[]
}

/**
 * 列配置
 * <p>
 * 对于关联字段（LINK类型），fieldId 格式为 "{linkTypeId}:{SOURCE|TARGET}"
 */
export interface ColumnConfig {
  /**
   * 字段 ID
   * 对于关联字段（LINK类型），格式为 "{linkTypeId}:{SOURCE|TARGET}"
   */
  fieldId: string

  /** 列宽（像素） */
  width?: number

  /** 是否可见 */
  visible: boolean

  /** 是否可拖拽宽度 */
  resizable: boolean

  /** 是否冻结 */
  frozen: boolean
}

/**
 * 排序字段
 */
export interface SortField {
  /** 字段 ID */
  field: string

  /** 排序方向 */
  direction: 'ASC' | 'DESC'
}

/**
 * 分页配置
 */
export interface PageConfig {
  /** 默认每页大小 */
  defaultPageSize: number

  /** 可选的每页大小列表 */
  pageSizeOptions: number[]

  /** 是否启用虚拟滚动 */
  enableVirtualScroll: boolean
}

// ==================== 辅助 DTO ====================

/**
 * 视图列表项 VO（用于列表展示）
 */
export interface ViewListItemVO {
  /** 视图 ID */
  id: string

  /** 组织 ID */
  orgId: string

  /** 视图名称 */
  name: string

  /** 视图描述 */
  description?: string

  /** 视图类型（LIST、planka 等） */
  viewType: string

  /** Schema 子类型标识 */
  schemaSubType: string

  /** 关联的卡片类型 ID */
  cardTypeId: string

  /** 关联的卡片类型名称 */
  cardTypeName?: string

  /** 列数量 */
  columnCount: number

  /** 是否为默认视图 */
  defaultView: boolean

  /** 是否共享 */
  shared: boolean

  /** 是否启用 */
  enabled: boolean

  /** 内容版本号（乐观锁） */
  contentVersion: number

  /** 创建时间 */
  createdAt: string

  /** 更新时间 */
  updatedAt: string
}

// ==================== 工具函数 ====================

/**
 * 创建空的列表视图定义
 */
export function createEmptyListView(orgId: string): ListViewDefinition {
  return {
    schemaSubType: SchemaSubType.LIST_VIEW,
    orgId,
    name: '',
    description: '',
    enabled: true,
    state: EntityState.ACTIVE,
    contentVersion: 0,
    cardTypeId: '',
    columnConfigs: [],
    sorts: [],
    pageConfig: {
      defaultPageSize: 20,
      pageSizeOptions: [10, 20, 50, 100],
      enableVirtualScroll: false,
    },
    defaultView: false,
    shared: true,
  }
}

/**
 * 创建空的列配置
 */
export function createEmptyColumnConfig(fieldId: string): ColumnConfig {
  return {
    fieldId,
    width: 150,
    visible: true,
    resizable: true,
    frozen: false,
  }
}

