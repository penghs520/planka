import type { SchemaDefinition } from './schema'
import { SchemaSubType } from './schema'
import { EntityState } from './common'
import type { EnumOptionDTO } from './view-data'

/**
 * 卡片新建页模板定义
 */
export interface CardCreatePageTemplateDefinition extends SchemaDefinition {
    /** Schema 子类型标识 */
    schemaSubType: SchemaSubType.CARD_CREATE_PAGE_TEMPLATE

    /** 所属卡片类型 ID */
    cardTypeId: string

    /** 是否系统内置模板 */
    systemTemplate: boolean

    /** 是否默认模板 */
    isDefault: boolean

    /** 字段项配置列表（平铺布局，无区域分组） */
    fieldItems: CreatePageFieldItemConfig[]
}


/**
 * 新建页字段项配置
 */
export interface CreatePageFieldItemConfig {
    /** 字段定义 ID */
    fieldId: string

    /** 宽度百分比（25%, 33%, 50%, 66%, 75%, 100%） */
    widthPercent: number

    /** 是否从新行开始（强制换行） */
    startNewRow: boolean
}

/**
 * 模板列表项 VO（用于列表展示）
 */
export interface CreatePageTemplateListItemVO {
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

    /** 是否系统内置模板 */
    systemTemplate: boolean

    /** 字段数量 */
    fieldCount: number

    /** 是否启用 */
    enabled: boolean

    /** 是否默认模板 */
    isDefault: boolean

    /** 内容版本号 */
    contentVersion: number

    /** 创建时间 */
    createdAt: string

    /** 更新时间 */
    updatedAt: string
}

/**
 * 选中项类型
 */
export type CreatePageSelectedItemType = 'field' | null

/**
 * 选中项信息
 */
export interface CreatePageSelectedItem {
    /** 选中项类型 */
    type: CreatePageSelectedItemType

    /** 选中字段的 ID */
    fieldId: string
}

let idCounter = 0

/**
 * 生成唯一 ID（仅用于前端临时标识）
 */
export function generateCreatePageTempId(prefix: string = 'temp'): string {
    return `${prefix}_${Date.now()}_${++idCounter}`
}

/**
 * 创建空的新建页模板定义
 * 默认包含标题字段
 */
export function createEmptyCreatePageTemplate(orgId: string, cardTypeId: string): CardCreatePageTemplateDefinition {
    return {
        schemaSubType: SchemaSubType.CARD_CREATE_PAGE_TEMPLATE,
        orgId,
        name: '',
        description: '',
        enabled: true,
        state: EntityState.ACTIVE,
        contentVersion: 0,
        cardTypeId,
        systemTemplate: false,
        isDefault: false,
        // 默认包含标题字段，宽度 100%
        fieldItems: [
            {
                fieldId: '$title',
                widthPercent: 100,
                startNewRow: false,
            },
        ],
    }
}

/**
 * 创建空的字段项配置
 */
export function createEmptyCreatePageFieldItem(fieldId: string): CreatePageFieldItemConfig {
    return {
        fieldId,
        widthPercent: 50,
        startNewRow: false,
    }
}

/**
 * 宽度百分比选项
 */
export const WidthPercentOptions = [
    { value: 50, label: '50%' },
    { value: 100, label: '100%' },
]

// ==================== 运行时表单 VO ====================

/**
 * 新建页表单 VO（运行时渲染用）
 */
export interface CreatePageFormVO {
    /** 模板 ID（如果使用的是默认生成的模板，则为 null） */
    templateId: string | null
    /** 模板名称 */
    templateName: string | null
    /** 卡片类型 ID */
    cardTypeId: string
    /** 卡片类型名称 */
    cardTypeName: string
    /** 字段配置列表 */
    fields: CreatePageFieldVO[]
}

/**
 * 字段 VO 基础类型
 */
export interface CreatePageFieldVO {
    /** 字段 ID */
    fieldId: string
    /** 字段名称 */
    name: string
    /** 字段类型 */
    fieldType: FieldVOType
    /** 宽度百分比 (50 or 100) */
    widthPercent: number
    /** 是否必填 */
    required: boolean
    /** 是否只读 */
    readOnly: boolean
    /** 占位符 */
    placeholder?: string
}

/**
 * 字段 VO 类型枚举
 */
export type FieldVOType = 'TEXT' | 'TEXTAREA' | 'MARKDOWN' | 'NUMBER' | 'DATE' | 'ENUM' | 'ATTACHMENT' | 'WEB_URL' | 'STRUCTURE' | 'LINK'

/**
 * 单行文本字段 VO
 */
export interface TextFieldVO extends CreatePageFieldVO {
    fieldType: 'TEXT'
    /** 最大长度 */
    maxLength?: number
    /** 默认值 */
    defaultValue?: string
}

/**
 * 多行文本字段 VO
 */
export interface TextAreaFieldVO extends CreatePageFieldVO {
    fieldType: 'TEXTAREA'
    /** 最大长度 */
    maxLength?: number
    /** 默认值 */
    defaultValue?: string
}

/**
 * Markdown 字段 VO
 */
export interface MarkdownFieldVO extends CreatePageFieldVO {
    fieldType: 'MARKDOWN'
    /** 最大长度 */
    maxLength?: number
    /** 默认值 */
    defaultValue?: string
}

/**
 * 数字字段 VO
 */
export interface NumberFieldVO extends CreatePageFieldVO {
    fieldType: 'NUMBER'
    /** 最小值 */
    minValue?: number
    /** 最大值 */
    maxValue?: number
    /** 小数位数 */
    precision?: number
    /** 单位 */
    unit?: string
    /** 是否显示千分位 */
    showThousandSeparator: boolean
    /** 默认值 */
    defaultValue?: number
}

/**
 * 日期字段 VO
 */
export interface DateFieldVO extends CreatePageFieldVO {
    fieldType: 'DATE'
    /** 日期格式: DATE, DATETIME, TIME */
    dateFormat?: string
    /** 默认值类型: NONE, NOW, FIXED */
    defaultValueType?: string
    /** 固定默认值 */
    fixedDefaultValue?: string
}

/**
 * 枚举字段 VO
 */
export interface EnumFieldVO extends CreatePageFieldVO {
    fieldType: 'ENUM'
    /** 枚举选项列表（复用 view-data.ts 的 EnumOptionDTO 统一类型） */
    options: EnumOptionDTO[]
    /** 是否多选 */
    multiSelect: boolean
    /** 默认选项 ID 列表 */
    defaultOptionIds?: string[]
}

/**
 * 附件字段 VO
 */
export interface AttachmentFieldVO extends CreatePageFieldVO {
    fieldType: 'ATTACHMENT'
    /** 允许的文件类型 */
    allowedFileTypes?: string[]
    /** 最大文件大小 */
    maxFileSize?: number
    /** 最大文件数量 */
    maxFileCount?: number
}

/**
 * 网页链接字段 VO
 */
export interface WebUrlFieldVO extends CreatePageFieldVO {
    fieldType: 'WEB_URL'
    /** 是否验证 URL */
    validateUrl: boolean
    /** 是否显示预览 */
    showPreview: boolean
    /** 默认 URL */
    defaultUrl?: string
    /** 默认链接文本 */
    defaultLinkText?: string
}

/**
 * 架构层级字段 VO
 */
export interface StructureFieldVO extends CreatePageFieldVO {
    fieldType: 'STRUCTURE'
    /** 架构 ID */
    structureId?: string
    /** 是否只允许选择叶子节点 */
    leafOnly: boolean
    /** 默认节点 ID */
    defaultNodeId?: string
}

/**
 * 关联字段 VO
 */
export interface LinkFieldVO extends CreatePageFieldVO {
    fieldType: 'LINK'
    /** 是否多选 */
    multiple: boolean
    /** 渲染配置 */
    renderConfig: any
}

