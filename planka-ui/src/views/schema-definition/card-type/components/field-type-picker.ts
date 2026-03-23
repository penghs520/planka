import { SchemaSubType } from '@/types/schema'

export type FieldTypeCategoryId = 'text' | 'selection' | 'date' | 'number' | 'other'

export interface FieldTypeCategoryDef {
  id: FieldTypeCategoryId
  subTypes: SchemaSubType[]
}

/** 新建字段第一步：飞书式侧栏分类与类型归属 */
export const FIELD_TYPE_CATEGORIES: FieldTypeCategoryDef[] = [
  {
    id: 'text',
    subTypes: [
      SchemaSubType.TEXT_FIELD,
      SchemaSubType.MULTI_LINE_TEXT_FIELD,
      SchemaSubType.MARKDOWN_FIELD,
    ],
  },
  {
    id: 'selection',
    subTypes: [SchemaSubType.ENUM_FIELD, SchemaSubType.STRUCTURE_FIELD],
  },
  { id: 'date', subTypes: [SchemaSubType.DATE_FIELD] },
  { id: 'number', subTypes: [SchemaSubType.NUMBER_FIELD] },
  {
    id: 'other',
    subTypes: [SchemaSubType.ATTACHMENT_FIELD, SchemaSubType.WEB_URL_FIELD],
  },
]
