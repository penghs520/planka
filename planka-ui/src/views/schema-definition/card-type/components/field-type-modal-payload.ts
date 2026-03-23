import type { SchemaSubType } from '@/types/schema'

/** 选择字段类型弹窗确认时携带的数据 */
export interface FieldTypeModalConfirmPayload {
  schemaSubType: SchemaSubType
  /** 仅关联类型：对侧是否多选（由入口决定） */
  linkTargetMulti?: boolean
  /** 仅枚举类型：是否多选（由入口决定） */
  enumMultiSelect?: boolean
}
