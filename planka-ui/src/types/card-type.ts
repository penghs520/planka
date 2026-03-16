import type { SchemaDefinition } from './schema'
import { SchemaSubType } from './schema'
import type { LinkPosition } from './link-type'
import type { FieldConfig } from './field-config'
import type { PermissionConfig } from './permission'
import type { Path } from './condition'

// Re-export field config types for backward compatibility
export type {
  ValueSourceType,
  FieldConfigBase,
  SingleLineTextFieldConfig,
  MultiLineTextFieldConfig,
  MarkdownFieldConfig,
  NumberFieldConfig,
  DateFieldConfig,
  EnumFieldConfig,
  AttachmentFieldConfig,
  WebUrlFieldConfig,
  StructureFieldConfig,
  LinkFieldConfig,
  FieldConfig,
} from './field-config'

export { getFieldTypeFromConfig, isSystemField } from './field-config'

/**
 * 匹配的关联属性 DTO
 * 用于架构线配置场景，返回能够连接父子层级的关联属性信息
 */
export interface MatchingLinkFieldDTO {
  /** 属性配置ID */
  id: string
  /** 属性名称 */
  name: string
  /** 关联类型ID */
  linkTypeId: string
  /** 关联位置（SOURCE/TARGET） */
  linkPosition: LinkPosition
  /** 是否多选（false = 单选，true = 多选），由客户端根据场景决定是否使用 */
  multiple: boolean | null
  /** 属性编码 */
  code: string
  /** 是否为系统内置属性 */
  systemField: boolean
  /** 排序顺序 */
  sortOrder: number
}

/**
 * 卡片类型子类型显示配置
 * 使用 SchemaSubType 作为类型标识
 */
export const CardTypeSubTypeConfig: Record<
  SchemaSubType.TRAIT_CARD_TYPE | SchemaSubType.ENTITY_CARD_TYPE,
  { label: string }
> = {
  [SchemaSubType.TRAIT_CARD_TYPE]: { label: '属性集' },
  [SchemaSubType.ENTITY_CARD_TYPE]: { label: '实体类型' },
}

/**
 * 卡片编号生成规则
 */
export interface CodeGenerationRule {
  /** 前缀 */
  prefix?: string
  /** 日期格式 (e.g., "yyyyMMdd", "yyyy-MM-dd") */
  dateFormat?: string
  /** 日期和序号连接符 */
  dateSequenceConnector?: string
  /** 序列号长度 (默认 6) */
  sequenceLength?: number
}

/**
 * 卡片类型定义基类
 */
export interface AbstractCardTypeDefinition extends SchemaDefinition {
  /** 编码 */
  code?: string
  /** 是否为系统内置卡片类型 */
  systemCardType?: boolean
}

/**
 * 属性集
 */
export interface AbstractCardType extends AbstractCardTypeDefinition {
  schemaSubType: SchemaSubType.TRAIT_CARD_TYPE
}

/**
 * 父类型信息
 */
export interface ParentTypeInfo {
  id: string
  name: string
}

/**
 * 标题组成部分
 */
export interface TitlePart {
  /** 关联路径（可选，null 表示当前卡片） */
  path?: Path | null
  /** 字段ID */
  fieldId: string
}

/**
 * 标题组合规则
 */
export interface TitleCompositionRule {
  /** 是否启用 */
  enabled: boolean
  /** 拼接区域（PREFIX/SUFFIX） */
  area: 'PREFIX' | 'SUFFIX'
  /** 拼接部分列表 */
  parts: TitlePart[]
}

/**
 * 实体类型
 */
export interface EntityCardType extends AbstractCardTypeDefinition {
  schemaSubType: SchemaSubType.ENTITY_CARD_TYPE
  /** 继承的属性集 ID 列表 */
  parentTypeIds?: string[]
  /** 继承的属性集信息列表 */
  parentTypes?: ParentTypeInfo[]
  /** 价值流定义 ID */
  valueStreamId?: string
  /** 默认详情页模板 ID */
  defaultDetailTemplateId?: string
  /** 默认卡面定义 ID */
  defaultCardFaceId?: string
  /** 权限配置 */
  permissionConfig?: PermissionConfig
  /** 编号生成规则 */
  codeGenerationRule?: CodeGenerationRule
  /** 标题组合规则 */
  titleCompositionRule?: TitleCompositionRule
}

/**
 * 卡片类型联合类型
 */
export type CardTypeDefinition = AbstractCardType | EntityCardType

/**
 * 属性来源信息
 */
export interface FieldSourceInfo {
  /** 属性定义来源卡片类型 ID */
  definitionSourceCardTypeId?: string
  /** 属性定义来源卡片类型名称 */
  definitionSourceCardTypeName?: string
  /** 属性配置来源卡片类型 ID */
  configSourceCardTypeId?: string
  /** 属性配置来源卡片类型名称 */
  configSourceCardTypeName?: string
  /** 属性定义是否继承 */
  definitionInherited: boolean
  /** 属性配置是否继承 */
  configInherited: boolean
  /** 是否来自关联类型定义（而非已保存的属性配置） */
  fromLinkTypeDefinition?: boolean
}

/**
 * 卡片类型属性配置列表
 */
export interface FieldConfigListWithSource {
  /** 卡片类型 ID */
  cardTypeId: string
  /** 卡片类型名称 */
  cardTypeName: string
  /** 完整属性配置列表 */
  fields: FieldConfig[]
  /** 属性来源信息 */
  fieldSources: Record<string, FieldSourceInfo>
}

/**
 * 创建空的卡片类型定义
 * 注意：新建时不传 id，由后端自动生成
 */
export function createEmptyCardType(
  subType: SchemaSubType.TRAIT_CARD_TYPE | SchemaSubType.ENTITY_CARD_TYPE,
  orgId: string,
): CardTypeDefinition {
  const base = {
    orgId,
    name: '',
    description: '',
    enabled: true,
    contentVersion: 1,
    state: 'ACTIVE' as const,
  }

  if (subType === SchemaSubType.TRAIT_CARD_TYPE) {
    return {
      ...base,
      schemaSubType: SchemaSubType.TRAIT_CARD_TYPE,
    } as AbstractCardType
  }

  return {
    ...base,
    schemaSubType: SchemaSubType.ENTITY_CARD_TYPE,
    parentTypeIds: [],
  } as EntityCardType
}

