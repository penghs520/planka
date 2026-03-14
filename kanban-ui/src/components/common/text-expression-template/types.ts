import type { FieldOption } from '@/types/field-option'

export type VariableSource = 'card' | 'member' | 'system'

export interface ExpressionVariable {
  source: VariableSource
  path: string[]        // 中间 link field ID 路径
  fieldId: string       // 终端字段 ID
  displayLabel: string  // 显示名称，如 "当前卡.所属需求.优先级"
}

export interface SystemVariable {
  id: string            // e.g. 'currentYear'
  nameKey: string       // i18n key
}

export interface FieldProvider {
  getCardFields: () => Promise<FieldOption[]>
  getMemberFields: () => Promise<FieldOption[]>
  getFieldsByLinkFieldId: (linkFieldId: string) => Promise<FieldOption[]>
}

export const SYSTEM_VARIABLES: SystemVariable[] = [
  { id: 'currentYear', nameKey: 'common.textExpressionTemplate.system.currentYear' },
  { id: 'currentMonth', nameKey: 'common.textExpressionTemplate.system.currentMonth' },
  { id: 'currentDate', nameKey: 'common.textExpressionTemplate.system.currentDate' },
  { id: 'currentTime', nameKey: 'common.textExpressionTemplate.system.currentTime' },
]

// 内置卡片属性（不通过 API 获取，直接展示）
export const CARD_BUILTIN_PROPERTIES: SystemVariable[] = [
  { id: 'id', nameKey: 'common.textExpressionTemplate.card.id' },
  { id: 'title', nameKey: 'common.textExpressionTemplate.card.title' },
  { id: 'code', nameKey: 'common.textExpressionTemplate.card.code' },
  { id: 'statusId', nameKey: 'common.textExpressionTemplate.card.statusId' },
  { id: 'createdAt', nameKey: 'common.textExpressionTemplate.card.createdAt' },
  { id: 'updatedAt', nameKey: 'common.textExpressionTemplate.card.updatedAt' },
]
