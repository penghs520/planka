import type { SchemaDefinition } from './schema'
import { SchemaSubType } from './schema'

/**
 * 属性类型枚举
 */
export enum FieldType {
  /** 单行文本 */
  SINGLE_LINE_TEXT = 'SINGLE_LINE_TEXT',
  /** 多行文本 */
  MULTI_LINE_TEXT = 'MULTI_LINE_TEXT',
  /** Markdown */
  MARKDOWN = 'MARKDOWN',
  /** 数字 */
  NUMBER = 'NUMBER',
  /** 日期（包含日期时间） */
  DATE = 'DATE',
  /** 枚举（单选和多选） */
  ENUM = 'ENUM',
  /** 附件 */
  ATTACHMENT = 'ATTACHMENT',
  /** 网页链接 */
  WEB_URL = 'WEB_URL',
  /** 架构层级 */
  STRUCTURE = 'STRUCTURE',
}

/**
 * 属性类型显示配置
 * labelKey 用于国际化，运行时通过 t(labelKey) 获取实际标签
 */
export const FieldTypeConfig: Record<FieldType, { labelKey: string; icon: string }> = {
  [FieldType.SINGLE_LINE_TEXT]: { labelKey: 'admin.fieldType.SINGLE_LINE_TEXT', icon: 'icon-font-size' },
  [FieldType.MULTI_LINE_TEXT]: { labelKey: 'admin.fieldType.MULTI_LINE_TEXT', icon: 'icon-font-size' },
  [FieldType.MARKDOWN]: { labelKey: 'admin.fieldType.MARKDOWN', icon: 'icon-font-size' },
  [FieldType.NUMBER]: { labelKey: 'admin.fieldType.NUMBER', icon: 'icon-sort-ascending' },
  [FieldType.DATE]: { labelKey: 'admin.fieldType.DATE', icon: 'icon-calendar' },
  [FieldType.ENUM]: { labelKey: 'admin.fieldType.ENUM', icon: 'icon-list' },
  [FieldType.ATTACHMENT]: { labelKey: 'admin.fieldType.ATTACHMENT', icon: 'icon-attachment' },
  [FieldType.WEB_URL]: { labelKey: 'admin.fieldType.WEB_URL', icon: 'icon-link' },
  [FieldType.STRUCTURE]: { labelKey: 'admin.fieldType.STRUCTURE', icon: 'icon-layers' },
}


/**
 * 日期格式
 * 与后端 DateFieldConfig.DateFormat 保持一致
 */
export enum DateFormat {
  /** 年-月-日 (2024-01-15) */
  DATE = 'DATE',
  /** 年-月-日 时:分 (2024-01-15 14:30) */
  DATETIME = 'DATETIME',
  /** 年-月-日 时:分:秒 (2024-01-15 14:30:00) */
  DATETIME_SECOND = 'DATETIME_SECOND',
  /** 年-月 (2024-01) */
  YEAR_MONTH = 'YEAR_MONTH',
}

/**
 * 卡片类型信息
 */
export interface CardTypeInfo {
  id: string
  name: string
}

/**
 * 属性定义基类
 */
export interface AbstractFieldDefinition extends SchemaDefinition {
  /** 属性编码 */
  code?: string
  /** 关联的卡片类型 ID 列表（创建/编辑时使用） */
  cardTypeIds?: string[]
  /** 关联的卡片类型列表（列表查询返回） */
  cardTypes?: CardTypeInfo[]
  /** 是否系统内置属性 */
  systemField: boolean
}

/**
 * 单行文本属性定义
 */
export interface SingleLineTextFieldDefinition extends AbstractFieldDefinition {
  schemaSubType: SchemaSubType.SINGLE_LINE_TEXT_FIELD_DEFINITION
  /** 最大长度 */
  maxLength?: number
  /** 默认值 */
  defaultValue?: string
  /** 占位提示 */
  placeholder?: string
}

/**
 * 多行文本属性定义
 */
export interface MultiLineTextFieldDefinition extends AbstractFieldDefinition {
  schemaSubType: SchemaSubType.MULTI_LINE_TEXT_FIELD_DEFINITION
  /** 最大长度 */
  maxLength?: number
  /** 默认值 */
  defaultValue?: string
  /** 占位提示 */
  placeholder?: string
}

/**
 * Markdown属性定义
 */
export interface MarkdownFieldDefinition extends AbstractFieldDefinition {
  schemaSubType: SchemaSubType.MARKDOWN_FIELD_DEFINITION
  /** 最大长度 */
  maxLength?: number
  /** 默认值 */
  defaultValue?: string
  /** 占位提示 */
  placeholder?: string
}

/**
 * 数字属性定义
 */
export interface NumberFieldDefinition extends AbstractFieldDefinition {
  schemaSubType: SchemaSubType.NUMBER_FIELD_DEFINITION
  /** 最小值 */
  minValue?: number
  /** 最大值 */
  maxValue?: number
  /** 小数精度 */
  precision?: number
  /** 单位 */
  unit?: string
  /** 是否显示千分位 */
  showThousandSeparator?: boolean
  /** 默认值 */
  defaultValue?: number
}

/**
 * 日期属性定义
 */
export interface DateFieldDefinition extends AbstractFieldDefinition {
  schemaSubType: SchemaSubType.DATE_FIELD_DEFINITION
  /** 日期格式 */
  dateFormat: DateFormat
  /** 是否使用当前时间作为默认值 */
  useNowAsDefault?: boolean
}

/**
 * 枚举选项
 * 与后端 EnumOptionDefinition 保持一致
 */
export interface EnumOption {
  /** 选项 ID */
  id: string
  /** 选项值 */
  value: string
  /** 显示标签 */
  label: string
  /** 颜色 */
  color?: string
  /** 排序号（与后端 order 字段对应） */
  order?: number
  /** 是否启用 */
  enabled?: boolean
}

/**
 * 枚举属性定义
 */
export interface EnumFieldDefinition extends AbstractFieldDefinition {
  schemaSubType: SchemaSubType.ENUM_FIELD_DEFINITION
  /** 枚举选项列表 */
  options: EnumOption[]
  /** 是否多选 */
  multiSelect: boolean
  /** 默认选项 ID */
  defaultOptionIds?: string[]
}

/**
 * 附件属性定义
 */
export interface AttachmentFieldDefinition extends AbstractFieldDefinition {
  schemaSubType: SchemaSubType.ATTACHMENT_FIELD_DEFINITION
  /** 允许的文件类型 */
  allowedFileTypes?: string[]
  /** 最大文件大小(MB) */
  maxFileSize?: number
  /** 最大文件数量 */
  maxFileCount?: number
}

/**
 * 网页链接属性定义
 */
export interface WebUrlFieldDefinition extends AbstractFieldDefinition {
  schemaSubType: SchemaSubType.WEB_URL_FIELD_DEFINITION
}

/**
 * 架构层级属性定义
 */
export interface StructureFieldDefinition extends AbstractFieldDefinition {
  schemaSubType: SchemaSubType.STRUCTURE_FIELD_DEFINITION
  /** 关联的架构线 ID */
  structureId?: string
  /** 层级配置列表 */
  levelBindings?: StructureLevelBinding[]
}



/**
 * 架构层级配置
 */
export interface StructureLevelBinding {
  /** 层级索引 */
  levelIndex: number
  /** 关联属性ID，格式: {linkTypeId}:{SOURCE|TARGET} */
  linkFieldId?: string
  /** 是否必须 */
  required: boolean
}

/**
 * 属性定义联合类型
 */
export type FieldDefinition =
  | SingleLineTextFieldDefinition
  | MultiLineTextFieldDefinition
  | MarkdownFieldDefinition
  | NumberFieldDefinition
  | DateFieldDefinition
  | EnumFieldDefinition
  | AttachmentFieldDefinition
  | WebUrlFieldDefinition
  | StructureFieldDefinition

/**
 * 属性类型到 SchemaSubType 的映射
 * 对应后端 Jackson 多态类型标识
 */
export const FieldTypeToSchemaSubType: Record<FieldType, SchemaSubType> = {
  [FieldType.SINGLE_LINE_TEXT]: SchemaSubType.SINGLE_LINE_TEXT_FIELD_DEFINITION,
  [FieldType.MULTI_LINE_TEXT]: SchemaSubType.MULTI_LINE_TEXT_FIELD_DEFINITION,
  [FieldType.MARKDOWN]: SchemaSubType.MARKDOWN_FIELD_DEFINITION,
  [FieldType.NUMBER]: SchemaSubType.NUMBER_FIELD_DEFINITION,
  [FieldType.DATE]: SchemaSubType.DATE_FIELD_DEFINITION,
  [FieldType.ENUM]: SchemaSubType.ENUM_FIELD_DEFINITION,
  [FieldType.ATTACHMENT]: SchemaSubType.ATTACHMENT_FIELD_DEFINITION,
  [FieldType.WEB_URL]: SchemaSubType.WEB_URL_FIELD_DEFINITION,
  [FieldType.STRUCTURE]: SchemaSubType.STRUCTURE_FIELD_DEFINITION,
}

/**
 * SchemaSubType 到 FieldType 的反向映射
 * 用于从后端返回的 type 字段解析出属性类型
 * 包含 FIELD_DEFINITION 和 FIELD_CONFIG 两种类型
 */
export const SchemaSubTypeToFieldType: Partial<Record<SchemaSubType, FieldType>> = {
  // FIELD_DEFINITION 映射
  [SchemaSubType.SINGLE_LINE_TEXT_FIELD_DEFINITION]: FieldType.SINGLE_LINE_TEXT,
  [SchemaSubType.MULTI_LINE_TEXT_FIELD_DEFINITION]: FieldType.MULTI_LINE_TEXT,
  [SchemaSubType.MARKDOWN_FIELD_DEFINITION]: FieldType.MARKDOWN,
  [SchemaSubType.NUMBER_FIELD_DEFINITION]: FieldType.NUMBER,
  [SchemaSubType.DATE_FIELD_DEFINITION]: FieldType.DATE,
  [SchemaSubType.ENUM_FIELD_DEFINITION]: FieldType.ENUM,
  [SchemaSubType.ATTACHMENT_FIELD_DEFINITION]: FieldType.ATTACHMENT,
  [SchemaSubType.WEB_URL_FIELD_DEFINITION]: FieldType.WEB_URL,
  [SchemaSubType.STRUCTURE_FIELD_DEFINITION]: FieldType.STRUCTURE,
  // FIELD_CONFIG 映射（使用新的 _FIELD 后缀）
  [SchemaSubType.TEXT_FIELD]: FieldType.SINGLE_LINE_TEXT,
  [SchemaSubType.MULTI_LINE_TEXT_FIELD]: FieldType.MULTI_LINE_TEXT,
  [SchemaSubType.MARKDOWN_FIELD]: FieldType.MARKDOWN,
  [SchemaSubType.NUMBER_FIELD]: FieldType.NUMBER,
  [SchemaSubType.DATE_FIELD]: FieldType.DATE,
  [SchemaSubType.ENUM_FIELD]: FieldType.ENUM,
  [SchemaSubType.ATTACHMENT_FIELD]: FieldType.ATTACHMENT,
  [SchemaSubType.WEB_URL_FIELD]: FieldType.WEB_URL,
  [SchemaSubType.STRUCTURE_FIELD]: FieldType.STRUCTURE,
}

/**
 * 根据 SchemaSubType 获取 FieldType
 */
export function getFieldTypeFromSchemaSubType(subType: SchemaSubType): FieldType | undefined {
  return SchemaSubTypeToFieldType[subType]
}

/**
 * 根据 SchemaSubType 获取字段类型的国际化 key
 * 支持 FIELD_DEFINITION 和 FIELD_CONFIG 类型
 * 返回翻译 key，需要在运行时通过 t() 函数获取实际标签
 */
export function getFieldTypeLabelKey(subType: SchemaSubType | string): string {
  // 特殊处理 DESCRIPTION 类型（用于详情描述内置字段）
  if (subType === 'DESCRIPTION') {
    return 'admin.fieldType.DESCRIPTION'
  }
  const fieldType = SchemaSubTypeToFieldType[subType as SchemaSubType]
  if (fieldType) {
    return FieldTypeConfig[fieldType].labelKey
  }
  return 'common.unknown'
}

/**
 * 创建空的属性定义
 * 注意：新建时不传 id，由后端自动生成
 */
export function createEmptyFieldDefinition(fieldType: FieldType, orgId: string): FieldDefinition {
  const base = {
    orgId,
    name: '',
    description: '',
    enabled: true,
    systemField: false,
    searchable: true,
    contentVersion: 1,
  }

  switch (fieldType) {
    case FieldType.SINGLE_LINE_TEXT:
      return {
        ...base,
        schemaSubType: FieldTypeToSchemaSubType[FieldType.SINGLE_LINE_TEXT],
        state: 'ACTIVE' as const,
      } as SingleLineTextFieldDefinition
    case FieldType.MULTI_LINE_TEXT:
      return {
        ...base,
        schemaSubType: FieldTypeToSchemaSubType[FieldType.MULTI_LINE_TEXT],
        state: 'ACTIVE' as const,
      } as MultiLineTextFieldDefinition
    case FieldType.MARKDOWN:
      return {
        ...base,
        schemaSubType: FieldTypeToSchemaSubType[FieldType.MARKDOWN],
        state: 'ACTIVE' as const,
      } as MarkdownFieldDefinition
    case FieldType.NUMBER:
      return {
        ...base,
        schemaSubType: FieldTypeToSchemaSubType[FieldType.NUMBER],
        precision: 0,
        showThousandSeparator: false,
        state: 'ACTIVE' as const,
      } as NumberFieldDefinition
    case FieldType.DATE:
      return {
        ...base,
        schemaSubType: FieldTypeToSchemaSubType[FieldType.DATE],
        dateFormat: DateFormat.DATE,
        useNowAsDefault: false,
        state: 'ACTIVE' as const,
      } as DateFieldDefinition
    case FieldType.ENUM:
      return {
        ...base,
        schemaSubType: FieldTypeToSchemaSubType[FieldType.ENUM],
        options: [],
        multiSelect: false,
        state: 'ACTIVE' as const,
      } as EnumFieldDefinition
    case FieldType.ATTACHMENT:
      return {
        ...base,
        schemaSubType: FieldTypeToSchemaSubType[FieldType.ATTACHMENT],
        maxFileCount: 10,
        state: 'ACTIVE' as const,
      } as AttachmentFieldDefinition
    case FieldType.WEB_URL:
      return {
        ...base,
        schemaSubType: FieldTypeToSchemaSubType[FieldType.WEB_URL],
        state: 'ACTIVE' as const,
      } as WebUrlFieldDefinition
    case FieldType.STRUCTURE:
      return {
        ...base,
        schemaSubType: FieldTypeToSchemaSubType[FieldType.STRUCTURE],
        structureId: '',
        levelBindings: [],
        state: 'ACTIVE' as const,
      } as StructureFieldDefinition
  }
}

