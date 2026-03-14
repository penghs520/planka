/**
 * 条件编辑器公共常量
 */
import { IconCalendar, IconMenu, IconFontColors, IconTags } from '@arco-design/web-vue/es/icon'
import { NodeType, SystemDateField, SYSTEM_DATE_FIELD_PREFIX } from '@/types/condition'
import type { Component } from 'vue'

/**
 * 系统字段定义
 */
export interface SystemFieldDefinition {
  id: string
  /** i18n key for field name */
  nameKey: string
  icon: Component
}

/**
 * 系统字段列表
 */
export const SYSTEM_FIELDS: SystemFieldDefinition[] = [
  { id: NodeType.CARD_CYCLE, nameKey: 'common.systemField.cardCycle', icon: IconMenu },
  { id: NodeType.STATUS, nameKey: 'common.systemField.valueStreamStatus', icon: IconTags },
  { id: NodeType.TITLE, nameKey: 'common.systemField.title', icon: IconFontColors },
  { id: NodeType.CODE, nameKey: 'common.systemField.number', icon: IconFontColors },
  { id: `${SYSTEM_DATE_FIELD_PREFIX}${SystemDateField.CREATED_AT}`, nameKey: 'common.systemField.createdAt', icon: IconCalendar },
  { id: `${SYSTEM_DATE_FIELD_PREFIX}${SystemDateField.UPDATED_AT}`, nameKey: 'common.systemField.updatedAt', icon: IconCalendar },
  { id: `${SYSTEM_DATE_FIELD_PREFIX}${SystemDateField.DISCARDED_AT}`, nameKey: 'common.systemField.discardedAt', icon: IconCalendar },
  { id: `${SYSTEM_DATE_FIELD_PREFIX}${SystemDateField.ARCHIVED_AT}`, nameKey: 'common.systemField.archivedAt', icon: IconCalendar },
]

/**
 * 系统字段的 nodeType 列表（这些字段的 subject 中没有 fieldId）
 */
export const SYSTEM_FIELD_NODE_TYPES = ['CARD_CYCLE', 'STATUS', 'TITLE', 'CODE']

/**
 * 字段类型到节点类型的映射（schemaSubType -> nodeType）
 */
export const FIELD_TYPE_TO_NODE_TYPE: Record<string, string> = {
  TEXT_FIELD: 'TEXT',
  MULTI_LINE_TEXT_FIELD: 'TEXT',
  MARKDOWN_FIELD: 'TEXT',
  NUMBER_FIELD: 'NUMBER',
  DATE_FIELD: 'DATE',
  ENUM_FIELD: 'ENUM',
  WEB_URL_FIELD: 'WEB_URL',
  ATTACHMENT_FIELD: 'TEXT',
  LINK_FIELD: 'LINK',
}

/**
 * FieldOption.fieldType 到节点类型的映射
 * 用于处理从 field-options API 返回的字段类型
 */
export const FIELD_SUMMARY_TYPE_TO_NODE_TYPE: Record<string, string> = {
  SINGLE_LINE_TEXT: 'TEXT',
  MULTI_LINE_TEXT: 'TEXT',
  MARKDOWN: 'TEXT',
  NUMBER: 'NUMBER',
  DATE: 'DATE',
  ENUM: 'ENUM',
  WEB_URL: 'WEB_URL',
  ATTACHMENT: 'TEXT',
  LINK: 'LINK',
}
