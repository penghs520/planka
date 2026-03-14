import type { Component } from 'vue'

/**
 * 表格操作列的操作项定义
 */
export interface ActionItem {
  /** 操作标识，用于事件回调区分 */
  key: string
  /** 显示标签 */
  label: string
  /** 图标组件 */
  icon?: Component
  /** 是否为危险操作（红色显示） */
  danger?: boolean
  /** 是否禁用 */
  disabled?: boolean
  /** 是否在此项前显示分割线 */
  divider?: boolean
  /** 是否可见，默认 true */
  visible?: boolean
  /** 点击回调（可选） */
  onClick?: () => void
}

/**
 * AdminTable 组件 Props
 */
export interface AdminTableProps {
  /** 表格数据 */
  data: unknown[]
  /** 加载状态 */
  loading?: boolean
  /** 行唯一标识 */
  rowKey?: string | ((record: unknown) => string)
  /** 表格大小 */
  size?: 'mini' | 'small' | 'medium' | 'large'
  /** 是否显示边框 */
  bordered?: boolean
  /** 滚动配置 */
  scroll?: { x?: number | string; y?: number | string }
  /** 空状态文字 */
  emptyText?: string
}
