import type { Router } from 'vue-router'
import i18n from '@/i18n'
import { SchemaType } from '@/types/schema'

/**
 * Schema类型到路由的映射配置
 */
interface SchemaRouteConfig {
  /** 路由路径 */
  path: string
  /** 是否支持跳转 */
  navigable: boolean
  /** 是否使用params而不是query传递ID */
  useParams?: boolean
  /** 默认tab（如果有） */
  defaultTab?: string
}

/**
 * Schema类型路由映射表
 */
const SCHEMA_ROUTE_MAP: Record<string, SchemaRouteConfig> = {
  [SchemaType.CARD_TYPE]: {
    path: '/admin/card-type',
    navigable: true,
    defaultTab: undefined, // 默认显示基础信息tab
  },
  [SchemaType.FIELD_DEFINITION]: {
    path: '/admin/field-definition',
    navigable: true,
  },
  [SchemaType.FORMULA_DEFINITION]: {
    path: '/admin/formula-definition',
    navigable: true,
  },
  [SchemaType.LINK_TYPE]: {
    path: '/admin/link-type',
    navigable: true,
  },
  [SchemaType.VIEW]: {
    path: '/admin/view',
    navigable: true,
  },
  [SchemaType.CARD_DETAIL_TEMPLATE]: {
    path: '/admin/card-detail-template',
    navigable: true,
    useParams: true, // 使用 /admin/card-detail-template/:id/edit 形式
  },
  // 不可跳转的类型
  [SchemaType.FIELD_CONFIG]: {
    path: '',
    navigable: false,
  },
  [SchemaType.VALUE_STREAM]: {
    path: '',
    navigable: false,
  },
  [SchemaType.BIZ_RULE]: {
    path: '',
    navigable: false,
  },
  [SchemaType.FLOW_POLICY]: {
    path: '',
    navigable: false,
  },
  [SchemaType.CARD_FACE]: {
    path: '',
    navigable: false,
  },
  [SchemaType.CARD_PERMISSION]: {
    path: '',
    navigable: false,
  },
  [SchemaType.MENU]: {
    path: '',
    navigable: false,
  },
  [SchemaType.NOTIFICATION_TEMPLATE]: {
    path: '',
    navigable: false,
  },
}

/**
 * 检查Schema类型是否支持跳转
 * @param schemaType Schema类型
 * @returns 是否支持跳转
 */
export function isNavigableSchemaType(schemaType: string): boolean {
  const config = SCHEMA_ROUTE_MAP[schemaType]
  return config?.navigable ?? false
}

/**
 * 获取Schema类型对应的默认tab（如果有）
 * @param schemaType Schema类型
 * @returns 默认tab名称或undefined
 */
export function getDefaultTab(schemaType: string): string | undefined {
  const config = SCHEMA_ROUTE_MAP[schemaType]
  return config?.defaultTab
}

/**
 * 根据Schema类型和ID在新标签页中打开对应的定义页面
 * @param schemaType Schema类型（如 'CARD_TYPE', 'LINK_TYPE'）
 * @param schemaId Schema ID
 * @param router Vue Router实例
 * @param tab 可选的tab参数，用于定位到特定标签页（如 'fields', 'detailTemplate'）
 */
export function navigateToSchemaInNewTab(
  schemaType: string,
  schemaId: string,
  router: Router,
  tab?: string
): void {
  const config = SCHEMA_ROUTE_MAP[schemaType]

  if (!config || !config.navigable) {
    const { t } = i18n.global
    console.warn(t('common.schemaTypeName.notSupported', { type: schemaType }))
    return
  }

  let routeLocation

  // CARD_DETAIL_TEMPLATE 使用 params 方式
  if (config.useParams) {
    routeLocation = router.resolve({
      path: `${config.path}/${schemaId}/edit`,
    })
  } else {
    // 其他类型使用 query 方式
    const query: Record<string, string> = { edit: schemaId }
    if (tab) {
      query.tab = tab
    }
    routeLocation = router.resolve({
      path: config.path,
      query,
    })
  }

  // 在新标签页打开
  const newWindow = window.open(routeLocation.href, '_blank')

  // Check if blocked by browser
  if (!newWindow || newWindow.closed || typeof newWindow.closed === 'undefined') {
    const { t } = i18n.global
    console.warn(t('common.schemaTypeName.popupBlocked'))
    // TODO: Show a notification message to user
  }
}

/**
 * Get schema type display name (for tooltips, etc.)
 * @param schemaType Schema type
 * @returns Display name
 */
export function getSchemaTypeDisplayName(schemaType: string): string {
  const { t } = i18n.global
  const keyMap: Record<string, string> = {
    [SchemaType.CARD_TYPE]: 'common.schemaTypeName.CARD_TYPE',
    [SchemaType.FIELD_DEFINITION]: 'common.schemaTypeName.FIELD_DEFINITION',
    [SchemaType.FORMULA_DEFINITION]: 'common.schemaTypeName.FORMULA_DEFINITION',
    [SchemaType.LINK_TYPE]: 'common.schemaTypeName.LINK_TYPE',
    [SchemaType.VIEW]: 'common.schemaTypeName.VIEW',
    [SchemaType.CARD_DETAIL_TEMPLATE]: 'common.schemaTypeName.CARD_DETAIL_TEMPLATE',
    [SchemaType.FIELD_CONFIG]: 'common.schemaTypeName.FIELD_CONFIG',
    [SchemaType.VALUE_STREAM]: 'common.schemaTypeName.VALUE_STREAM',
    [SchemaType.BIZ_RULE]: 'common.schemaTypeName.BIZ_RULE',
    [SchemaType.FLOW_POLICY]: 'common.schemaTypeName.FLOW_POLICY',
    [SchemaType.CARD_FACE]: 'common.schemaTypeName.CARD_FACE',
    [SchemaType.CARD_PERMISSION]: 'common.schemaTypeName.CARD_PERMISSION',
    [SchemaType.MENU]: 'common.schemaTypeName.MENU',
    [SchemaType.NOTIFICATION_TEMPLATE]: 'common.schemaTypeName.NOTIFICATION_TEMPLATE',
  }
  const key = keyMap[schemaType]
  return key ? t(key) : schemaType
}
