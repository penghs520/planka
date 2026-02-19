/**
 * 视图类型注册表
 * 管理不同视图类型的组件映射，支持动态加载
 */
import type { Component } from 'vue'

/** 支持的视图类型 */
export type ViewType = 'LIST' | 'planka' | 'GANTT'

/** View config */
export interface ViewConfig {
  /** View type */
  type: ViewType
  /** i18n key for view name */
  nameKey: string
  /** View component (async loaded) */
  component: () => Promise<{ default: Component }>
}

/** View registry */
const viewRegistry = new Map<ViewType, ViewConfig>()

// Register list view
viewRegistry.set('LIST', {
  type: 'LIST',
  nameKey: 'common.viewTypeName.list',
  component: () => import('../components/list/ListViewPanel.vue'),
})

// 未来扩展：注册看板视图
// viewRegistry.set('planka', {
//   type: 'planka',
//   name: '看板视图',
//   component: () => import('../components/planka/plankaViewPanel.vue'),
// })

// 未来扩展：注册甘特图视图
// viewRegistry.set('GANTT', {
//   type: 'GANTT',
//   name: '甘特图视图',
//   component: () => import('../components/gantt/GanttViewPanel.vue'),
// })

/**
 * 获取视图配置
 * @param type 视图类型
 */
export function getViewConfig(type: ViewType): ViewConfig | undefined {
  return viewRegistry.get(type)
}

/**
 * 获取视图组件
 * @param type 视图类型
 */
export function getViewComponent(type: ViewType): ViewConfig['component'] | null {
  return viewRegistry.get(type)?.component || null
}

/**
 * 获取所有注册的视图类型
 */
export function getAllViewTypes(): ViewConfig[] {
  return Array.from(viewRegistry.values())
}

/**
 * 检查视图类型是否已注册
 * @param type 视图类型
 */
export function isViewTypeRegistered(type: string): type is ViewType {
  return viewRegistry.has(type as ViewType)
}
