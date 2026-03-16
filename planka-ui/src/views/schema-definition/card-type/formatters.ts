import { SchemaSubType } from '@/types/schema'
import { CardTypeSubTypeConfig, type FieldConfig, type FieldConfigListWithSource } from '@/types/card-type'

// 类型标签和颜色
export function getTypeLabel(type: SchemaSubType): string {
  return CardTypeSubTypeConfig[type as keyof typeof CardTypeSubTypeConfig]?.label || type
}

export function getTypeColor(type: SchemaSubType): string {
  return type === SchemaSubType.TRAIT_CARD_TYPE ? 'purple' : 'blue'
}

// 属性来源类型
export type SourceType = 'own-definition' | 'own-config' | 'inherited-definition' | 'inherited-config' | 'link-type-definition'

/**
 * 获取属性配置在 fieldSources 中的 key
 * 后端返回的 fieldId 对于 LinkFieldConfig 已经是 "linkTypeId:position" 格式
 * 所以直接使用 fieldId 即可
 */
export function getFieldSourceKey(config: FieldConfig): string {
  return config.fieldId
}

export function getSourceType(config: FieldConfig, fieldList: FieldConfigListWithSource | null): SourceType {
  if (!fieldList) return 'own-definition'
  const key = getFieldSourceKey(config)
  const source = fieldList.fieldSources[key]
  if (!source) return 'own-definition'

  // 来自关联类型定义
  if (source.fromLinkTypeDefinition) {
    return 'link-type-definition'
  }

  // 有配置存在时
  if (source.configSourceCardTypeId) {
    return source.configInherited ? 'inherited-config' : 'own-config'
  }

  // 无配置，根据定义判断
  return source.definitionInherited ? 'inherited-definition' : 'own-definition'
}

// 高亮卡片类型名称，带有可点击跳转功能
function highlightName(name: string, cardTypeId?: string): string {
  if (cardTypeId) {
    return `<a class="source-link" data-card-type-id="${cardTypeId}">${name}</a>`
  }
  return `<span class="source-highlight">${name}</span>`
}

// i18n-aware version of getSourceLabel - 简化版本，只区分继承和自有
export function getSourceLabelI18n(
  config: FieldConfig,
  fieldList: FieldConfigListWithSource | null,
  t: (key: string, params?: Record<string, unknown>) => string,
): string {
  if (!fieldList) return t('admin.cardType.fieldConfig.ownProperty')
  const key = getFieldSourceKey(config)
  const source = fieldList.fieldSources[key]
  if (!source) return t('admin.cardType.fieldConfig.ownProperty')

  // 判断是否是继承的（配置继承或定义继承）
  const isInherited = source.configInherited || source.definitionInherited

  if (isInherited) {
    // 优先使用配置来源名称，其次是定义来源名称
    const sourceName = source.configSourceCardTypeName || source.definitionSourceCardTypeName
    const sourceId = source.configSourceCardTypeId || source.definitionSourceCardTypeId
    if (sourceName) {
      return t('admin.cardType.fieldConfig.inheritedProperty', {
        name: highlightName(sourceName, sourceId),
      })
    }
  }

  return t('admin.cardType.fieldConfig.ownProperty')
}

export function getSourceLabel(config: FieldConfig, fieldList: FieldConfigListWithSource | null): string {
  if (!fieldList) return '-'
  const key = getFieldSourceKey(config)
  const source = fieldList.fieldSources[key]
  if (!source) return '自有属性定义'

  // 来自关联类型定义
  if (source.fromLinkTypeDefinition) {
    if (source.definitionInherited) {
      return `继承自 ${source.definitionSourceCardTypeName} 的关联关系`
    }
    return '自有关联类型定义'
  }

  // 有配置存在时，显示配置来源
  if (source.configSourceCardTypeId) {
    if (source.configInherited) {
      return `继承自 ${source.configSourceCardTypeName} 的属性配置`
    }
    return '自有属性配置'
  }

  // 无配置，显示定义来源
  if (source.definitionInherited) {
    return `继承自 ${source.definitionSourceCardTypeName} 的属性定义`
  }
  return '自有属性定义'
}

export function getSourceColor(config: FieldConfig, fieldList: FieldConfigListWithSource | null): string {
  if (!fieldList) return 'green'
  const key = getFieldSourceKey(config)
  const source = fieldList.fieldSources[key]
  if (!source) return 'green'

  // 判断是否自有：
  // 1. 有配置时，看配置是否自有（configInherited = false）
  // 2. 无配置时，看定义是否自有（definitionInherited = false）
  // 3. 来自关联类型定义时，看是否自有（definitionInherited = false）
  if (source.configSourceCardTypeId) {
    // 有配置，看配置是否自有
    return source.configInherited ? 'gray' : 'green'
  }
  // 无配置（包括来自关联类型定义），看定义是否自有
  return source.definitionInherited ? 'gray' : 'green'
}

// 属性类型标签 i18n key 映射
const fieldTypeLabelKeyMap: Record<string, string> = {
  TEXT_FIELD: 'admin.fieldType.SINGLE_LINE_TEXT',
  MULTI_LINE_TEXT_FIELD: 'admin.fieldType.MULTI_LINE_TEXT',
  MARKDOWN_FIELD: 'admin.fieldType.MARKDOWN',
  NUMBER_FIELD: 'admin.fieldType.NUMBER',
  DATE_FIELD: 'admin.fieldType.DATE',
  ENUM_FIELD: 'admin.fieldType.ENUM',
  ATTACHMENT_FIELD: 'admin.fieldType.ATTACHMENT',
  WEB_URL_FIELD: 'admin.fieldType.WEB_URL',
  STRUCTURE_FIELD: 'admin.fieldType.STRUCTURE',
  LINK_FIELD: 'admin.fieldType.LINK',
}

export function getFieldTypeLabelKey(schemaSubType: string): string {
  return fieldTypeLabelKeyMap[schemaSubType] || 'common.unknown'
}

export function getFieldTypeLabelI18n(
  schemaSubType: string,
  t: (key: string) => string,
): string {
  const key = fieldTypeLabelKeyMap[schemaSubType]
  if (key) {
    return t(key)
  }
  return schemaSubType
}

// 支持值来源配置的属性类型
const valueSourceSupportedTypes = [
  'TEXT_FIELD',
  'MULTI_LINE_TEXT_FIELD',
  'MARKDOWN_FIELD',
  'NUMBER_FIELD',
  'DATE_FIELD',
  'ENUM_FIELD',
  'WEB_URL_FIELD',
]

// 支持计算公式的属性类型（仅数字和日期）
const formulaSupportedTypes = ['NUMBER_FIELD', 'DATE_FIELD']

export function supportsValueSource(schemaSubType: string): boolean {
  return valueSourceSupportedTypes.includes(schemaSubType)
}

export function supportsFormula(schemaSubType: string): boolean {
  return formulaSupportedTypes.includes(schemaSubType)
}

// 属性值来源标签
const valueSourceLabelMap: Record<string, string> = {
  MANUAL: '手动输入',
  FORMULA: '计算公式',
  REFERENCE: '引用',
  SYSTEM: '系统更新',
}

export function getValueSourceLabel(valueSource?: string): string {
  if (!valueSource) return '手动输入'
  return valueSourceLabelMap[valueSource] || valueSource
}

// i18n-aware version
const valueSourceI18nKeyMap: Record<string, string> = {
  MANUAL: 'admin.cardType.fieldConfig.valueSourceManual',
  FORMULA: 'admin.cardType.fieldConfig.valueSourceFormula',
  REFERENCE: 'admin.cardType.fieldConfig.valueSourceReference',
  SYSTEM: 'admin.cardType.fieldConfig.valueSourceSystem',
}

export function getValueSourceLabelI18n(
  valueSource: string | undefined,
  t: (key: string) => string,
): string {
  if (!valueSource) return t('admin.cardType.fieldConfig.valueSourceManual')
  const key = valueSourceI18nKeyMap[valueSource]
  return key ? t(key) : valueSource
}

// 获取默认值显示文本
export function getDefaultValueDisplay(config: any): string {
  const subType = config.schemaSubType

  // 文本类型
  if (['TEXT_FIELD', 'MULTI_LINE_TEXT_FIELD', 'MARKDOWN_FIELD'].includes(subType)) {
    return config.defaultValue || '-'
  }

  // 数字类型
  if (subType === 'NUMBER_FIELD') {
    return config.defaultValue !== undefined && config.defaultValue !== null
      ? String(config.defaultValue)
      : '-'
  }

  // 日期类型
  if (subType === 'DATE_FIELD') {
    return config.useNowAsDefault ? '当前时间' : '-'
  }

  // 枚举类型
  if (subType === 'ENUM_FIELD') {
    if (!config.defaultOptionIds?.length || !config.items?.length) return '-'
    const names = config.defaultOptionIds
      .map((id: string) => config.items?.find((item: any) => item.id === id)?.name)
      .filter(Boolean)
    return names.length > 0 ? names.join(', ') : '-'
  }

  // 网页链接
  if (subType === 'WEB_URL_FIELD') {
    return config.defaultUrl || '-'
  }

  return '-'
}

// i18n-aware version
export function getDefaultValueDisplayI18n(config: any, t: (key: string) => string): string {
  const subType = config.schemaSubType

  // 文本类型
  if (['TEXT_FIELD', 'MULTI_LINE_TEXT_FIELD', 'MARKDOWN_FIELD'].includes(subType)) {
    return config.defaultValue || '-'
  }

  // 数字类型
  if (subType === 'NUMBER_FIELD') {
    return config.defaultValue !== undefined && config.defaultValue !== null
      ? String(config.defaultValue)
      : '-'
  }

  // 日期类型
  if (subType === 'DATE_FIELD') {
    return config.useNowAsDefault ? t('admin.cardType.fieldConfig.defaultValueNow') : '-'
  }

  // 枚举类型
  if (subType === 'ENUM_FIELD') {
    if (!config.defaultOptionIds?.length || !config.items?.length) return '-'
    const names = config.defaultOptionIds
      .map((id: string) => config.items?.find((item: any) => item.id === id)?.name)
      .filter(Boolean)
    return names.length > 0 ? names.join(', ') : '-'
  }

  // 网页链接
  if (subType === 'WEB_URL_FIELD') {
    return config.defaultUrl || '-'
  }

  return '-'
}
