import type { ListViewDefinition, SortField } from './view'
import type { Condition } from './condition'
import type { CardDTO } from './card'
import { renderFieldValue as renderFieldValueUtil } from '@/utils/field-render'

// 重新导出 CardDTO，方便其他地方引用
export type { CardDTO }

// ==================== 请求类型 ====================

/**
 * 视图数据查询请求
 */
export interface ViewDataRequest {
  /** 用户筛选条件（与视图条件 AND 合并） */
  userCondition?: Condition

  /** 用户排序配置（覆盖视图默认排序） */
  userSorts?: SortField[]

  /** 指定分组值（查询该分组的具体数据） */
  groupValue?: string

  /** 分页：页码（从 0 开始） */
  page?: number

  /** 分页：每页大小 */
  size?: number
}

/**
 * 视图预览请求
 */
export interface ViewPreviewRequest {
  /** 视图定义（未持久化） */
  viewDefinition: ListViewDefinition

  /** 数据查询请求 */
  dataRequest?: ViewDataRequest
}

// ==================== 响应类型 ====================

/**
 * 视图数据响应基类
 */
export interface ViewDataResponse {
  /** 视图 ID */
  viewId: string

  /** 视图名称 */
  viewName: string

  /** 视图类型 */
  viewType: string

  /** 列元数据 */
  columns: ColumnMeta[]

  /** 分页信息（非分组模式） */
  pageInfo?: PageInfo

  /** 分组数据（分组模式） */
  groups?: GroupedCardData[]

  /** 是否分组模式 */
  grouped: boolean
}

/**
 * 列表视图数据响应
 */
export interface ListViewDataResponse extends ViewDataResponse {
  viewType: 'LIST'

  /** 关联的卡片类型 ID */
  cardTypeId?: string

  /** 卡片数据列表（非分组模式） */
  cards?: CardDTO[]

  /** 价值流状态选项列表（用于 $statusId 内置字段渲染） */
  statusOptions?: StatusOption[]
}

/**
 * 价值流状态选项
 */
export interface StatusOption {
  /** 状态 ID */
  id: string
  /** 状态名称 */
  name: string
  /** 阶段类型：TODO, IN_PROGRESS, DONE, CANCELLED */
  stepKind?: string
}

/**
 * 关联位置
 */
export type LinkPosition = 'SOURCE' | 'TARGET'

/**
 * 列元数据
 * <p>
 * 对于关联字段（LINK类型），fieldId 格式为 "{linkTypeId}:{SOURCE|TARGET}"
 */
export interface ColumnMeta {
  /**
   * 字段 ID
   * 对于关联字段（LINK类型），格式为 "{linkTypeId}:{SOURCE|TARGET}"
   */
  fieldId: string

  /** 列标题 */
  title: string

  /** 字段类型 */
  fieldType?: string

  /** 列宽 */
  width?: number

  /** 是否冻结 */
  frozen: boolean

  /** 是否可见 */
  visible: boolean

  /** 是否可排序 */
  sortable: boolean

  /** 是否可编辑 */
  editable?: boolean

  /** 是否必填 */
  required?: boolean

  /** 是否为内置字段 */
  builtin?: boolean

  /** 渲染配置（根据 fieldType 有不同的子类型） */
  renderConfig?: FieldRenderConfig
}

// ==================== 渲染配置类型 ====================

/**
 * 字段渲染配置基础类型
 */
export type FieldRenderConfig =
  | EnumRenderConfig
  | DateRenderConfig
  | NumberRenderConfig
  | TextRenderConfig
  | AttachmentRenderConfig
  | LinkRenderConfig
  | StructureRenderConfig
  | WebUrlRenderConfig
  | MarkdownRenderConfig

/**
 * 枚举类型渲染配置
 */
export interface EnumRenderConfig {
  type: 'ENUM'
  /** 是否多选 */
  multiSelect: boolean
  /** 枚举选项列表 */
  options?: EnumOptionDTO[]
}

/**
 * 枚举选项（统一的枚举选项类型，用于渲染配置和字段配置）
 */
export interface EnumOptionDTO {
  /** 选项 ID */
  id: string
  /** 选项值（存储在数据库中，与 id 相同） */
  value?: string
  /** 显示文本 */
  label: string
  /** 选项颜色 */
  color?: string
  /** 排序号（可选） */
  order?: number
  /** 是否启用 */
  enabled?: boolean
}

/**
 * 日期类型渲染配置
 */
export interface DateRenderConfig {
  type: 'DATE'
  /** 日期格式：DATE, DATETIME, DATETIME_SECOND, YEAR_MONTH */
  dateFormat: string
}

/**
 * 数字类型渲染配置
 */
export interface NumberRenderConfig {
  type: 'NUMBER'
  /** 小数位数 */
  precision?: number
  /** 单位 */
  unit?: string
  /** 最小值 */
  minValue?: number
  /** 最大值 */
  maxValue?: number
  /** 显示格式 */
  displayFormat?: 'NORMAL' | 'PERCENT' | 'THOUSAND_SEPARATOR'
  /** 百分数显示效果 */
  percentStyle?: 'NUMBER' | 'PROGRESS_BAR'
  /** @deprecated 使用 displayFormat === 'THOUSAND_SEPARATOR' 代替 */
  showThousandSeparator: boolean
}

/**
 * 文本类型渲染配置（单行/多行文本共用）
 */
export interface TextRenderConfig {
  type: 'TEXT'
  /** 最大长度 */
  maxLength?: number
  /** 占位符 */
  placeholder?: string
  /** 是否多行 */
  multiLine: boolean
}

/**
 * 附件类型渲染配置
 */
export interface AttachmentRenderConfig {
  type: 'ATTACHMENT'
  /** 允许的文件类型（扩展名列表） */
  allowedFileTypes?: string[]
  /** 最大文件大小（字节） */
  maxFileSize?: number
  /** 最大文件数量 */
  maxFileCount?: number
}

/**
 * 关联类型渲染配置
 */
export interface LinkRenderConfig {
  type: 'LINK'
  /** 是否多选 */
  multiple: boolean
  /** 目标卡片类型 ID */
  targetCardTypeId?: string
  /** 目标卡片类型名称 */
  targetCardTypeName?: string
}

/**
 * 架构层级类型渲染配置
 */
export interface StructureRenderConfig {
  type: 'STRUCTURE'
  /** 架构线 ID */
  structureId?: string
  /** 是否只能选择叶子节点 */
  leafOnly: boolean
}

/**
 * 网页链接类型渲染配置
 */
export interface WebUrlRenderConfig {
  type: 'WEB_URL'
  /** 是否验证 URL 格式 */
  validateUrl: boolean
  /** 是否显示链接预览 */
  showPreview: boolean
}

/**
 * Markdown 类型渲染配置
 */
export interface MarkdownRenderConfig {
  type: 'MARKDOWN'
  /** 最大长度 */
  maxLength?: number
  /** 占位符 */
  placeholder?: string
}

/**
 * 分页信息
 */
export interface PageInfo {
  /** 当前页码（从 0 开始） */
  page: number

  /** 每页大小 */
  size: number

  /** 总记录数 */
  total: number

  /** 总页数 */
  totalPages: number
}

/**
 * 分组卡片数据
 */
export interface GroupedCardData {
  /** 分组字段 ID */
  groupFieldId: string

  /** 分组值 */
  groupValue: string

  /** 分组显示名称 */
  groupLabel: string

  /** 该组的卡片数量 */
  count: number

  /** 该组的卡片数据（懒加载） */
  cards?: CardDTO[]

  /** 分页信息 */
  pageInfo?: PageInfo

  /** 是否已加载数据 */
  loaded: boolean
}

// ==================== 工具函数 ====================

/**
 * 创建空的视图数据请求
 */
export function createEmptyViewDataRequest(): ViewDataRequest {
  return {
    page: 0,
    size: 20,
  }
}

/**
 * 获取卡片属性值的显示文本（使用统一的字段渲染工具函数）
 * @param card 卡片数据
 * @param fieldId 字段 ID
 * @param columns 列元数据（可选，用于获取枚举选项标签）
 * @param statusOptions 价值流状态选项（可选，用于 $statusId 内置字段）
 */
export function getFieldDisplayValue(
  card: CardDTO,
  fieldId: string,
  columns?: ColumnMeta[],
  statusOptions?: StatusOption[],
): string {
  return renderFieldValueUtil(card, fieldId, columns, statusOptions)
}

