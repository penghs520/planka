import type { LinkPosition } from './link-type'
import type { EnumOptionDTO } from './view-data'
import type { Condition } from './condition'

// Re-export LinkPosition for convenience
export type { LinkPosition }

/** 属性值来源类型 */
export type ValueSourceType = 'MANUAL' | 'FORMULA' | 'REFERENCE' | 'SYSTEM'

/**
 * 属性值校验规则
 */
export interface ValidationRule {
  /** 校验条件（条件满足则通过，不满足则失败） */
  condition?: Condition
  /** 错误消息模板（支持表达式如 ${当前卡.字段名}） */
  errorMessage?: string
  /** 是否启用 */
  enabled?: boolean
  /** 规则描述 */
  description?: string
}

/**
 * 属性配置基础接口
 */
export interface FieldConfigBase {
  /** 配置 ID */
  id?: string
  /** Schema 子类型 */
  schemaSubType: string
  /** 属性 ID */
  fieldId: string
  /** 属性名称 */
  name: string
  /** 属性编码 */
  code?: string
  /** 属性值来源 */
  valueSource?: ValueSourceType
  /** 计算公式 ID（值来源为 FORMULA 时使用） */
  formulaId?: string
  /** 引用属性 ID（值来源为 REFERENCE 时使用） */
  referenceFieldId?: string
  /** 是否必填 */
  required?: boolean
  /** 是否只读 */
  readOnly?: boolean
  /** 排序号 */
  sortOrder?: number
  /** 是否为系统内置属性 */
  systemField?: boolean
  /** 属性值校验规则列表 */
  validationRules?: ValidationRule[]
}

/** 单行文本属性配置 */
export interface SingleLineTextFieldConfig extends FieldConfigBase {
  schemaSubType: 'TEXT_FIELD'
  maxLength?: number
  defaultValue?: string
  placeholder?: string
}

/** 多行文本属性配置 */
export interface MultiLineTextFieldConfig extends FieldConfigBase {
  schemaSubType: 'MULTI_LINE_TEXT_FIELD'
  maxLength?: number
  defaultValue?: string
  placeholder?: string
}

/** Markdown 属性配置 */
export interface MarkdownFieldConfig extends FieldConfigBase {
  schemaSubType: 'MARKDOWN_FIELD'
  maxLength?: number
  defaultValue?: string
  placeholder?: string
}

/** 数字显示格式 */
export type NumberDisplayFormat = 'NORMAL' | 'PERCENT' | 'THOUSAND_SEPARATOR'

/** 百分数显示效果 */
export type PercentStyle = 'NUMBER' | 'PROGRESS_BAR'

/** 数字属性配置 */
export interface NumberFieldConfig extends FieldConfigBase {
  schemaSubType: 'NUMBER_FIELD'
  minValue?: number
  maxValue?: number
  precision?: number
  unit?: string
  defaultValue?: number
  /** 显示格式 */
  displayFormat?: NumberDisplayFormat
  /** 百分数显示效果（仅当 displayFormat 为 PERCENT 时有效） */
  percentStyle?: PercentStyle
  /** @deprecated 使用 displayFormat === 'THOUSAND_SEPARATOR' 代替 */
  showThousandSeparator?: boolean
}

/** 日期属性配置 */
export interface DateFieldConfig extends FieldConfigBase {
  schemaSubType: 'DATE_FIELD'
  dateFormat?: 'DATE' | 'DATETIME' | 'DATETIME_SECOND' | 'YEAR_MONTH'
  /** 是否使用当前时间作为默认值 */
  useNowAsDefault?: boolean
}

/** 枚举属性配置 */
export interface EnumFieldConfig extends FieldConfigBase {
  schemaSubType: 'ENUM_FIELD'
  /** 枚举选项列表（卡片类型级别可覆盖选项，复用 EnumOptionDTO 统一类型） */
  options?: EnumOptionDTO[]
  multiSelect?: boolean
  defaultOptionIds?: string[]
}

/** 附件属性配置 */
export interface AttachmentFieldConfig extends FieldConfigBase {
  schemaSubType: 'ATTACHMENT_FIELD'
  allowedFileTypes?: string[]
  maxFileSize?: number
  maxFileCount?: number
}

/** 网页链接属性配置 */
export interface WebUrlFieldConfig extends FieldConfigBase {
  schemaSubType: 'WEB_URL_FIELD'
  validateUrl?: boolean
  showPreview?: boolean
  defaultUrl?: string
  defaultLinkText?: string
}

/** 层级绑定配置 */
export interface LevelBinding {
  /** 层级索引 */
  levelIndex: number
  /** 层级名称 */
  levelName?: string
  /** 层级对应的卡片类型名称列表 */
  levelCardTypeNames?: string[]
  /** 关联属性ID */
  linkFieldId?: string
  /** 关联属性名称 */
  linkFieldName?: string
  /** 是否必填 */
  required: boolean
}

/** 架构层级属性配置 */
export interface StructureFieldConfig extends FieldConfigBase {
  schemaSubType: 'STRUCTURE_FIELD'
  /** 架构线ID */
  structureId?: string
  /** 架构线名称 */
  structureName?: string
  /** 层级绑定配置 */
  levelBindings?: LevelBinding[]
  /** @deprecated 仅用于兼容 */
  leafOnly?: boolean
  /** @deprecated 仅用于兼容 */
  maxLevel?: number
}

/**
 * 关联属性配置
 * <p>
 * fieldId 格式为 "{linkTypeId}:{SOURCE|TARGET}"
 */
export interface LinkFieldConfig {
  schemaSubType: 'LINK_FIELD'
  id?: string
  orgId: string
  name: string
  cardTypeId: string
  /**
   * 关联属性ID
   * 格式为 "{linkTypeId}:{SOURCE|TARGET}"
   */
  fieldId: string
  /** 属性编码 */
  code?: string
  displayName?: string
  maxCount?: number
  required?: boolean
  readOnly?: boolean
  /** 是否为系统内置属性 */
  systemField?: boolean
}

/** 属性配置联合类型 */
export type FieldConfig =
  | SingleLineTextFieldConfig
  | MultiLineTextFieldConfig
  | MarkdownFieldConfig
  | NumberFieldConfig
  | DateFieldConfig
  | EnumFieldConfig
  | AttachmentFieldConfig
  | WebUrlFieldConfig
  | StructureFieldConfig
  | LinkFieldConfig

/**
 * 从 schemaSubType 获取字段类型
 */
export function getFieldTypeFromConfig(schemaSubType: string): string {
  const mapping: Record<string, string> = {
    TEXT_FIELD: 'TEXT',
    MULTI_LINE_TEXT_FIELD: 'TEXT',
    MARKDOWN_FIELD: 'MARKDOWN',
    NUMBER_FIELD: 'NUMBER',
    DATE_FIELD: 'DATE',
    ENUM_FIELD: 'ENUM',
    ATTACHMENT_FIELD: 'ATTACHMENT',
    WEB_URL_FIELD: 'WEB_URL',
    STRUCTURE_FIELD: 'STRUCTURE',
    LINK_FIELD: 'LINK',
    DESCRIPTION: 'DESCRIPTION',
  }
  return mapping[schemaSubType] || 'TEXT'
}

/**
 * 判断字段是否为系统字段
 */
export function isSystemField(fieldConfig: FieldConfig): boolean {
  // 优先使用 systemField 属性
  if ('systemField' in fieldConfig && fieldConfig.systemField !== undefined) {
    return fieldConfig.systemField
  }
  // 兼容旧逻辑：根据 fieldId 判断
  const systemFieldIds = ['title', 'status', 'priority', 'assignee', 'created_at', 'updated_at']
  return systemFieldIds.some((id) => fieldConfig.fieldId.toLowerCase().includes(id))
}
