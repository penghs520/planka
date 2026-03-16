/**
 * 字段控制配置类型定义
 *
 * 用于卡片详情页字段的编辑控制（只读/可编辑）和必填级别
 */

/**
 * 只读原因类型
 */
export type ReadOnlyReasonType =
  | 'BUILTIN_FIELD'          // 内置字段
  | 'FIELD_CONFIG'           // 字段配置为只读
  | 'PERMISSION_DENIED'      // 无权限（未来扩展）
  | 'WORKFLOW_RESTRICTION'   // 流程限制（未来扩展）

/**
 * 必填级别
 */
export type RequiredLevel =
  | 'HINT'                   // 仅提示必填（显示黄色星号，不阻止保存）
  | 'STRICT'                 // 强制必填（显示红色星号，必须填写才能保存）

/**
 * 字段控制配置
 */
export interface FieldControlConfig {
  /** 是否可编辑 */
  editable: boolean
  /** 只读原因类型 */
  readOnlyReasonType?: ReadOnlyReasonType
  /** 只读原因文本（自定义说明） */
  readOnlyReasonText?: string
  /** 必填级别（undefined 表示非必填） */
  requiredLevel?: RequiredLevel
  /** 必填原因说明（为什么需要必填） */
  requiredReasonText?: string
}

/**
 * 卡片字段控制映射
 * key: fieldId
 * value: 字段控制配置
 */
export type CardFieldControls = Record<string, FieldControlConfig>

/**
 * 只读原因类型的显示文本
 */
export const ReadOnlyReasonLabels: Record<ReadOnlyReasonType, string> = {
  BUILTIN_FIELD: '内置字段不可编辑',
  FIELD_CONFIG: '此字段已被配置为只读',
  PERMISSION_DENIED: '您没有编辑此字段的权限',
  WORKFLOW_RESTRICTION: '当前流程状态不允许编辑此字段',
}

/**
 * 必填级别的显示配置
 */
export const RequiredLevelConfig: Record<RequiredLevel, { color: string; label: string }> = {
  HINT: { color: 'rgb(var(--warning-6))', label: '建议填写' },
  STRICT: { color: 'rgb(var(--danger-6))', label: '必须填写' },
}
