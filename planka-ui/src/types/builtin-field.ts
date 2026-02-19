/**
 * 内置字段定义
 */
export interface BuiltinFieldDef {
  /** 字段ID，以 $ 开头 */
  id: string
  /** 显示名称 */
  name: string
  /** 字段类型 */
  fieldType: string
  /** 是否可排序 */
  sortable: boolean
  /** 是否可编辑 */
  editable: boolean
}

/**
 * 内置字段列表
 */
export const BUILTIN_FIELDS: BuiltinFieldDef[] = [
  { id: '$createdAt', name: '创建时间', fieldType: 'DATE', sortable: true, editable: false },
  { id: '$updatedAt', name: '更新时间', fieldType: 'DATE', sortable: true, editable: false },
  { id: '$statusId', name: '价值流状态', fieldType: 'ENUM', sortable: false, editable: true },
  { id: '$cardStyle', name: '卡片周期', fieldType: 'ENUM', sortable: true, editable: false },
  { id: '$archivedAt', name: '归档时间', fieldType: 'DATE', sortable: true, editable: false },
  { id: '$discardedAt', name: '丢弃时间', fieldType: 'DATE', sortable: true, editable: false },
  { id: '$code', name: '卡片编号', fieldType: 'TEXT', sortable: true, editable: false },
  { id: '$description', name: '详情描述', fieldType: 'MARKDOWN', sortable: false, editable: true },
]

/**
 * 判断是否为内置字段
 * @param fieldId 字段ID
 * @returns 如果以 $ 开头则为内置字段
 */
export function isBuiltinField(fieldId: string): boolean {
  return fieldId != null && fieldId.startsWith('$')
}

/**
 * 获取内置字段定义
 * @param fieldId 字段ID
 * @returns 内置字段定义，未找到时返回 undefined
 */
export function getBuiltinField(fieldId: string): BuiltinFieldDef | undefined {
  return BUILTIN_FIELDS.find((f) => f.id === fieldId)
}

/**
 * 卡片周期状态映射
 */
export const CARD_STYLE_LABELS: Record<string, string> = {
  ACTIVE: `活跃`,
  ARCHIVED: '已归档',
  DISCARDED: '已丢弃',
}

/**
 * 卡片周期状态颜色
 */
export const CARD_STYLE_COLORS: Record<string, string> = {
  ACTIVE: '#52c41a',
  ARCHIVED: '#8c8c8c',
  DISCARDED: '#d4380d',
}

/**
 * 价值流阶段类型颜色（浅色系，适配黑色文字）
 */
export const STEP_KIND_COLORS: Record<string, string> = {
  TODO: '#ffe27a',        // 待办 - 黄色
  IN_PROGRESS: '#c8d9ff', // 进行中 - 蓝色
  DONE: '#b4db94',        // 完成 - 绿色
  CANCELLED: '#d9d9d9',   // 取消 - 灰色
}

/**
 * 获取阶段类型对应的颜色
 * @param stepKind 阶段类型
 * @returns 颜色值，默认返回浅蓝色
 */
export function getStepKindColor(stepKind?: string): string {
  const defaultColor = STEP_KIND_COLORS.IN_PROGRESS ?? '#c8d9ff'
  if (!stepKind) return defaultColor
  return STEP_KIND_COLORS[stepKind] ?? defaultColor
}
