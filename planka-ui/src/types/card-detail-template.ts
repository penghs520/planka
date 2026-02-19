import type { SchemaDefinition } from './schema'
import type { Condition } from './condition'
import { SchemaSubType } from './schema'
import { EntityState } from './common'

// ==================== 卡片详情页模板定义 ====================

/**
 * 卡片详情页模板定义
 */
export interface CardDetailTemplateDefinition extends SchemaDefinition {
  /** Schema 子类型标识 */
  schemaSubType: SchemaSubType.CARD_DETAIL_TEMPLATE

  /** 所属卡片类型 ID */
  cardTypeId: string

  /** 是否系统内置模板 */
  systemTemplate: boolean

  /** 生效条件（当卡片满足此条件时使用该模板） */
  effectiveCondition?: Condition

  /** 优先级（数字越小优先级越高，用于多模板匹配） */
  priority: number

  /** 头部配置 */
  header?: DetailHeaderConfig

  /** 标签页配置列表 */
  tabs: TabConfig[]
}

// ==================== 头部配置 ====================

/**
 * 详情页头部配置
 */
export interface DetailHeaderConfig {
  /** 是否显示卡片类型图标 */
  showTypeIcon: boolean

  /** 是否显示卡片编号 */
  showCardNumber: boolean

  /** 是否显示状态标签 */
  showStatus: boolean

  /** 标题字段配置ID */
  titleFieldConfigId?: string

  /** 头部快捷操作字段ID列表 */
  quickActionFieldIds: string[]
}

// ==================== 标签页配置 ====================

/**
 * Tab 类型
 */
export type TabType = 'SYSTEM' | 'CUSTOM'

/**
 * 系统 Tab 类型
 */
export type SystemTabType = 'BASIC_INFO' | 'COMMENT' | 'ACTIVITY_LOG'

/**
 * 字段行间距
 */
export type FieldRowSpacing = 'COMPACT' | 'NORMAL' | 'LOOSE'

/**
 * 字段行间距配置
 */
export const FieldRowSpacingConfig: Record<FieldRowSpacing, { label: string; rowGap: string }> = {
  COMPACT: { label: '紧凑', rowGap: '6px' },
  NORMAL: { label: '正常', rowGap: '12px' },
  LOOSE: { label: '宽松', rowGap: '18px' },
}

/**
 * 标签页配置
 */
export interface TabConfig {
  /** Tab 唯一标识 */
  tabId: string

  /** Tab 类型 */
  tabType: TabType

  /** Tab 名称 */
  name: string

  /** 系统 Tab 类型（仅 SYSTEM 类型有效） */
  systemTabType?: SystemTabType

  /** 字段行间距（紧凑、正常、宽松） */
  fieldRowSpacing?: FieldRowSpacing

  /** 区域配置列表（仅 CUSTOM 类型有效） */
  sections?: SectionConfig[]
}

// ==================== 区域配置 ====================

/**
 * 区域配置
 */
export interface SectionConfig {
  /** 区域唯一标识 */
  sectionId: string

  /** 区域名称 */
  name: string

  /** 是否折叠 */
  collapsed: boolean

  /** 是否可折叠 */
  collapsible: boolean

  /** 字段项配置列表 */
  fieldItems: FieldItemConfig[]
}

// ==================== 字段项配置 ====================

/**
 * 字段项配置
 */
export interface FieldItemConfig {
  /** 字段配置 ID */
  fieldConfigId: string

  /** 宽度百分比（25-100，默认50表示一半宽度） */
  widthPercent: number

  /** 自定义标签（为空则使用字段名） */
  customLabel?: string

  /** 占位提示文本 */
  placeholder?: string

  /** 新建页是否可见 */
  visibleOnCreate?: boolean

  /** 新建页是否必填 */
  requiredOnCreate?: boolean

  /** 可见条件 */
  visibleCondition?: Condition

  /** 是否从新行开始（强制换行） */
  startNewRow?: boolean

  /** 固定高度（px），用于描述等特殊字段 */
  height?: number
}

// ==================== 辅助 DTO ====================

/**
 * 模板列表项 VO（用于列表展示）
 */
export interface TemplateListItemVO {
  /** 模板 ID */
  id: string

  /** 组织 ID */
  orgId: string

  /** 模板名称 */
  name: string

  /** 模板描述 */
  description?: string

  /** 所属卡片类型 ID */
  cardTypeId: string

  /** 所属卡片类型名称 */
  cardTypeName?: string

  /** 是否系统内置模板 */
  systemTemplate: boolean

  /** 优先级 */
  priority: number

  /** Tab 数量 */
  tabCount: number

  /** 是否启用 */
  enabled: boolean

  /** 是否默认模板 */
  default: boolean

  /** 内容版本号 */
  contentVersion: number

  /** 创建时间 */
  createdAt: string

  /** 更新时间 */
  updatedAt: string
}

// ==================== 编辑器状态类型 ====================

/**
 * 选中项类型
 */
export type SelectedItemType = 'tab' | 'section' | 'field' | null

/**
 * 选中项信息
 */
export interface SelectedItem {
  /** 选中项类型 */
  type: SelectedItemType

  /** 选中项 ID */
  id: string

  /** Tab ID（如果选中的是区域或字段） */
  tabId?: string

  /** 区域 ID（如果选中的是字段） */
  sectionId?: string
}

// ==================== 工具函数 ====================

let idCounter = 0

/**
 * 生成唯一 ID（仅用于前端临时标识）
 */
export function generateTempId(prefix: string = 'temp'): string {
  return `${prefix}_${Date.now()}_${++idCounter}`
}

/**
 * 创建空的详情页模板定义
 */
export function createEmptyTemplate(orgId: string, cardTypeId: string): CardDetailTemplateDefinition {
  return {
    schemaSubType: SchemaSubType.CARD_DETAIL_TEMPLATE,
    orgId,
    name: '',
    description: '',
    enabled: true,
    state: EntityState.ACTIVE,
    contentVersion: 0,
    cardTypeId,
    systemTemplate: false,
    priority: 100,
    header: createDefaultHeaderConfig(),
    tabs: createDefaultTabs(),
  }
}

/**
 * 创建默认头部配置
 */
export function createDefaultHeaderConfig(): DetailHeaderConfig {
  return {
    showTypeIcon: true,
    showCardNumber: true,
    showStatus: true,
    quickActionFieldIds: [],
  }
}

/**
 * 创建默认 Tab 列表（系统预置）
 */
export function createDefaultTabs(): TabConfig[] {
  return [
    {
      tabId: 'basic_info',
      tabType: 'SYSTEM',
      name: '基础信息',
      systemTabType: 'BASIC_INFO',
      fieldRowSpacing: 'NORMAL',
      sections: [createEmptySection('默认区域')],
    },
    {
      tabId: 'comment',
      tabType: 'SYSTEM',
      name: '评论',
      systemTabType: 'COMMENT',
      fieldRowSpacing: 'NORMAL',
    },
    {
      tabId: 'activity_log',
      tabType: 'SYSTEM',
      name: '操作记录',
      systemTabType: 'ACTIVITY_LOG',
      fieldRowSpacing: 'NORMAL',
    },
  ]
}

/**
 * 创建空的自定义 Tab
 */
export function createEmptyCustomTab(name: string = '新标签页'): TabConfig {
  return {
    tabId: generateTempId('tab'),
    tabType: 'CUSTOM',
    name,
    fieldRowSpacing: 'NORMAL',
    sections: [],
  }
}

/**
 * 创建空的区域配置
 */
export function createEmptySection(name: string = '新区域'): SectionConfig {
  return {
    sectionId: generateTempId('section'),
    name,
    collapsed: false,
    collapsible: true,
    fieldItems: [],
  }
}

/**
 * 创建空的字段项配置
 */
export function createEmptyFieldItem(fieldConfigId: string): FieldItemConfig {
  const baseConfig: FieldItemConfig = {
    fieldConfigId,
    widthPercent: 50,
    visibleOnCreate: true,
    requiredOnCreate: false,
  }

  // 描述字段特殊处理：100% 宽度、独占一行、最小高度 120px
  if (fieldConfigId === '$description') {
    return {
      ...baseConfig,
      widthPercent: 100,
      startNewRow: true,
      height: 120,
    }
  }

  return baseConfig
}

/**
 * 系统 Tab 类型配置
 */
export const SystemTabTypeConfig: Record<SystemTabType, { label: string; icon: string }> = {
  BASIC_INFO: { label: '基础信息', icon: 'icon-file' },
  COMMENT: { label: '评论', icon: 'icon-message' },
  ACTIVITY_LOG: { label: '操作记录', icon: 'icon-history' },
}
