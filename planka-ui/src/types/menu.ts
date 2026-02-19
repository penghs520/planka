import type { SchemaDefinition } from './schema'
import { SchemaSubType } from './schema'
import { EntityState } from './common'
import type { Condition } from './condition'

// ==================== 菜单分组定义 ====================

/**
 * 菜单分组定义
 */
export interface MenuGroupDefinition extends SchemaDefinition {
  schemaSubType: SchemaSubType.MENU_GROUP
  /** 父分组ID（根分组为null） */
  parentId?: string
  /** 分组图标 */
  icon?: string
  /** 分组内的视图项 */
  viewItems: ViewMenuItem[]
  /** 是否展开 */
  expanded?: boolean
  /** 可见性配置（旧版，保留兼容） */
  visibility?: VisibilityConfig
  /** 可见性条件（基于成员属性的条件表达式） */
  visibilityCondition?: Condition
}

/**
 * 视图菜单项
 */
export interface ViewMenuItem {
  /** 视图ID */
  viewId: string
  /** 排序号 */
  sortOrder: number
  /** 自定义显示名称 */
  displayName?: string
}

/**
 * 可见性配置
 */
export interface VisibilityConfig {
  type: VisibilityType
  allowedUserIds?: string[]
  allowedRoleIds?: string[]
}

export type VisibilityType = 'ALL' | 'SPECIFIED_USERS' | 'SPECIFIED_ROLES'

// ==================== VO 类型 ====================

/**
 * 菜单树
 */
export interface MenuTreeVO {
  /** 根节点列表 */
  roots: MenuTreeNodeVO[]
  /** 未分组的视图 */
  ungroupedViews: MenuTreeNodeVO[]
}

/**
 * 菜单树节点
 */
export interface MenuTreeNodeVO {
  /** 节点ID */
  id: string
  /** 节点类型：GROUP 或 VIEW */
  type: 'GROUP' | 'VIEW'
  /** 显示名称 */
  name: string
  /** 图标（分组专用） */
  icon?: string
  /** 视图类型（视图专用，如 LIST, planka） */
  viewType?: string
  /** 关联的卡片类型ID（视图专用） */
  cardTypeId?: string
  /** 关联的卡片类型名称（视图专用） */
  cardTypeName?: string
  /** 排序号 */
  sortOrder?: number
  /** 是否展开（分组专用） */
  expanded?: boolean
  /** 是否启用 */
  enabled?: boolean
  /** 子节点 */
  children: MenuTreeNodeVO[]
}

/**
 * 菜单分组摘要
 */
export interface MenuGroupVO {
  id: string
  orgId: string
  name: string
  description?: string
  parentId?: string
  icon?: string
  sortOrder?: number
  viewCount: number
  enabled: boolean
  contentVersion: number
  createdAt?: string
  updatedAt?: string
}

// ==================== 请求类型 ====================

/**
 * 添加视图到分组请求
 */
export interface AddViewToGroupRequest {
  viewId: string
  sortOrder?: number
  displayName?: string
}

/**
 * 重新排序视图请求
 */
export interface ReorderViewsRequest {
  viewIds: string[]
}

// ==================== 工具函数 ====================

/**
 * 创建空的菜单分组定义
 */
export function createEmptyMenuGroup(orgId: string): MenuGroupDefinition {
  return {
    schemaSubType: SchemaSubType.MENU_GROUP,
    orgId,
    name: '',
    enabled: true,
    state: EntityState.ACTIVE,
    contentVersion: 0,
    viewItems: [],
    expanded: true,
  }
}

/**
 * 判断节点是否为分组
 */
export function isGroupNode(node: MenuTreeNodeVO): boolean {
  return node.type === 'GROUP'
}

/**
 * 判断节点是否为视图
 */
export function isViewNode(node: MenuTreeNodeVO): boolean {
  return node.type === 'VIEW'
}

/**
 * 可见性类型配置
 */
export const VisibilityTypeConfig: Record<VisibilityType, { label: string; description: string }> = {
  ALL: { label: '所有人可见', description: '组织内所有用户都可以看到此分组' },
  SPECIFIED_USERS: { label: '指定用户', description: '仅指定的用户可以看到此分组' },
  SPECIFIED_ROLES: { label: '指定角色', description: '仅指定角色的用户可以看到此分组' },
}

/**
 * 常用图标列表
 */
export const MenuIconOptions = [
  { value: 'folder', label: '文件夹' },
  { value: 'apps', label: '应用' },
  { value: 'star', label: '收藏' },
  { value: 'home', label: '首页' },
  { value: 'settings', label: '设置' },
  { value: 'dashboard', label: '仪表盘' },
  { value: 'list', label: '列表' },
  { value: 'grid', label: '网格' },
  { value: 'calendar', label: '日历' },
  { value: 'chart', label: '图表' },
  { value: 'user', label: '用户' },
  { value: 'team', label: '团队' },
]
