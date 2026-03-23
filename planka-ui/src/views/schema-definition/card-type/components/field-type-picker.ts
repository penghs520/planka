import { SchemaSubType } from '@/types/schema'

export type FieldTypeCategoryId = 'text' | 'selection' | 'date' | 'number' | 'other'

/** 选择器中的单项（关联、枚举拆成单选/多选两个入口） */
export interface FieldTypePickerItem {
  /** 稳定唯一键，用于选中态与列表 :key */
  key: string
  schemaSubType: SchemaSubType
  /**
   * 仅 `LINK_FIELD`：对侧是否多选；当前实体类型恒为源侧
   * `false` = 单选关联入口，`true` = 多选关联入口
   */
  linkTargetMulti?: boolean
  /**
   * 仅 `ENUM_FIELD`：是否多选；由入口决定
   * `false` = 单选枚举，`true` = 多选枚举
   */
  enumMultiSelect?: boolean
}

export interface FieldTypeCategoryDef {
  id: FieldTypeCategoryId
  items: FieldTypePickerItem[]
}

function item(
  schemaSubType: SchemaSubType,
  extra?: Partial<Pick<FieldTypePickerItem, 'key' | 'linkTargetMulti' | 'enumMultiSelect'>>
): FieldTypePickerItem {
  return {
    key: extra?.key ?? schemaSubType,
    schemaSubType,
    linkTargetMulti: extra?.linkTargetMulti,
    enumMultiSelect: extra?.enumMultiSelect,
  }
}

/** 新建字段第一步：飞书式侧栏分类与类型归属 */
export const FIELD_TYPE_CATEGORIES: FieldTypeCategoryDef[] = [
  {
    id: 'text',
    items: [
      item(SchemaSubType.TEXT_FIELD),
      item(SchemaSubType.MULTI_LINE_TEXT_FIELD),
      item(SchemaSubType.MARKDOWN_FIELD),
    ],
  },
  {
    id: 'selection',
    items: [
      item(SchemaSubType.ENUM_FIELD, { key: 'ENUM_FIELD_SINGLE', enumMultiSelect: false }),
      item(SchemaSubType.ENUM_FIELD, { key: 'ENUM_FIELD_MULTI', enumMultiSelect: true }),
      item(SchemaSubType.LINK_FIELD, { key: 'LINK_FIELD_SINGLE', linkTargetMulti: false }),
      item(SchemaSubType.LINK_FIELD, { key: 'LINK_FIELD_MULTI', linkTargetMulti: true }),
      item(SchemaSubType.STRUCTURE_FIELD),
    ],
  },
  { id: 'date', items: [item(SchemaSubType.DATE_FIELD)] },
  { id: 'number', items: [item(SchemaSubType.NUMBER_FIELD)] },
  {
    id: 'other',
    items: [item(SchemaSubType.ATTACHMENT_FIELD), item(SchemaSubType.WEB_URL_FIELD)],
  },
]
