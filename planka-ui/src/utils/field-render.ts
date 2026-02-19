/**
 * 统一的字段渲染工具函数
 *
 * 供列表视图和详情页共用，基于 FieldRenderConfig 进行字段值显示
 */
import type { CardDTO, FieldValue, FieldRenderMeta } from '@/types/card'
import type {
  ColumnMeta,
  FieldRenderConfig,
  EnumRenderConfig,
  NumberRenderConfig,
  DateRenderConfig,
  StatusOption,
  EnumOptionDTO,
} from '@/types/view-data'
import { isBuiltinField, CARD_STYLE_LABELS, CARD_STYLE_COLORS, getStepKindColor } from '@/types/builtin-field'
import { isValidLinkFieldId } from '@/utils/link-field-utils'
import { formatDate, formatDateTime } from '@/utils/format'

/**
 * 渲染配置源：可以是列表视图的 ColumnMeta 或详情页的 FieldRenderMeta
 */
export type RenderConfigSource = ColumnMeta[] | FieldRenderMeta[]

/**
 * 判断是否为 ColumnMeta（有 title 字段）
 */
function isColumnMeta(item: ColumnMeta | FieldRenderMeta): item is ColumnMeta {
  return 'title' in item
}

/**
 * 从渲染配置源中查找匹配的项（统一使用 fieldId 匹配）
 */
function findRenderMeta(fieldId: string, source?: RenderConfigSource): ColumnMeta | FieldRenderMeta | undefined {
  if (!source || source.length === 0) return undefined
  return source.find(item => item.fieldId === fieldId)
}

/**
 * 从渲染配置源中获取指定字段的渲染配置
 */
function getRenderConfig(fieldId: string, source?: RenderConfigSource): FieldRenderConfig | undefined {
  const meta = findRenderMeta(fieldId, source)
  return meta?.renderConfig
}

/**
 * 常见字段代码的中文映射（用于后备显示）
 */
const FIELD_CODE_NAMES: Record<string, string> = {
  // 请假申请字段
  'reason': '原因',
  'start-date': '开始日期',
  'end-date': '结束日期',
  'duration': '时长',
  'leave-type': '请假类型',
  'approval-status': '审批状态',
  'approval-time': '审批时间',
  'approval-comment': '审批意见',
  'applicant': '申请人',
  'approver': '审批人',

  // 加班申请字段
  'overtime-type': '加班类型',
  'overtime-date': '加班日期',
  'start-time': '开始时间',
  'end-time': '结束时间',

  // 补卡申请字段
  'makeup-date': '补卡日期',
  'makeup-type': '补卡类型',
  'makeup-time': '补卡时间',

  // 考勤记录字段
  'date': '日期',
  'sign-in-time': '签到时间',
  'sign-out-time': '签退时间',
  'work-duration': '工作时长',
  'status': '状态',
  'remark': '备注',
  'member': '成员',
}

/**
 * 获取字段名称
 */
export function getFieldName(fieldId: string, source?: RenderConfigSource): string {
  const meta = findRenderMeta(fieldId, source)
  if (!meta) {
    // 如果没有找到元数据，尝试从 fieldId 中提取字段代码作为后备方案
    // fieldId 格式通常为: {orgId}:{cardTypeCode}:{fieldCode}
    // 对于 LINK 类型: {orgId}:{cardTypeCode}:link:{fieldCode}:SOURCE
    const parts = fieldId.split(':')
    if (parts.length >= 3) {
      // 获取字段代码部分
      let fieldCode: string | undefined
      // 如果最后一部分是 SOURCE 或 TARGET，说明是 LINK 类型，取倒数第二部分
      if (parts[parts.length - 1] === 'SOURCE' || parts[parts.length - 1] === 'TARGET') {
        fieldCode = parts[parts.length - 2]
      } else {
        // 否则取最后一部分
        fieldCode = parts[parts.length - 1]
      }
      // 优先使用中文映射
      if (fieldCode) {
        const mappedName = FIELD_CODE_NAMES[fieldCode]
        if (mappedName) {
          return mappedName
        }
      }
      // 否则将连字符转换为空格，首字母大写
      return (fieldCode || fieldId)
        .split('-')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ')
    }
    return fieldId
  }
  // ColumnMeta 使用 title，FieldRenderMeta 使用 name
  return isColumnMeta(meta) ? meta.title : meta.name || fieldId
}

/**
 * 获取内置字段的显示值
 */
function getBuiltinFieldDisplayValue(
  card: CardDTO,
  fieldId: string,
  statusOptions?: StatusOption[],
): string {
  switch (fieldId) {
    case '$createdAt':
      return card.createdAt ? formatDateTime(card.createdAt) : '-'
    case '$updatedAt':
      return card.updatedAt ? formatDateTime(card.updatedAt) : '-'
    case '$archivedAt':
      return card.archivedAt ? formatDateTime(card.archivedAt) : '-'
    case '$discardedAt':
      return card.abandonedAt ? formatDateTime(card.abandonedAt) : '-'
    case '$cardStyle':
      return card.cardStyle ? (CARD_STYLE_LABELS[card.cardStyle] || card.cardStyle) : '-'
    case '$statusId':
      if (!card.statusId) return '-'
      if (statusOptions) {
        const statusOption = statusOptions.find((opt) => opt.id === card.statusId)
        if (statusOption) return statusOption.name
      }
      return card.statusId
    case '$code':
      return card.customCode || String(card.codeInOrg || '-')
    default:
      return '-'
  }
}

/**
 * 根据 FieldRenderConfig 格式化字段值
 */
function formatFieldValueByConfig(
  fieldValue: FieldValue,
  renderConfig?: FieldRenderConfig,
): string {
  const { type, value } = fieldValue
  if (value === null || value === undefined) return '-'

  switch (type) {
    case 'TEXT':
      return String(value) || '-'

    case 'NUMBER':
      if (typeof value !== 'number') return '-'
      if (renderConfig?.type === 'NUMBER') {
        const numConfig = renderConfig as NumberRenderConfig
        let result = numConfig.showThousandSeparator
          ? value.toLocaleString('zh-CN', { maximumFractionDigits: numConfig.precision ?? 2 })
          : value.toString()
        if (numConfig.unit) {
          result += ` ${numConfig.unit}`
        }
        return result
      }
      return value.toString()

    case 'DATE':
      if (!value) return '-'
      if (renderConfig?.type === 'DATE') {
        const dateConfig = renderConfig as DateRenderConfig
        if (dateConfig.dateFormat === 'DATETIME' || dateConfig.dateFormat === 'DATETIME_SECOND') {
          return formatDateTime(value as number)
        }
      }
      return formatDate(value as number)

    case 'ENUM':
      if (!Array.isArray(value) || value.length === 0) return '-'
      if (renderConfig?.type === 'ENUM') {
        const enumConfig = renderConfig as EnumRenderConfig
        if (enumConfig.options) {
          const labels = value
            .map((optionId: string) => enumConfig.options?.find(opt => opt.id === optionId)?.label)
            .filter(Boolean)
          return labels.length > 0 ? labels.join('、') : '-'
        }
      }
      // 没有渲染配置时返回 ID
      return value.join('、') || '-'

    case 'STRUCTURE':
      // 架构属性：遍历链表结构，显示每层名称
      return formatStructureValue(value as StructureItem | null) || '-'

    default:
      return String(value) || '-'
  }
}

/**
 * 架构属性值链表节点
 */
interface StructureItem {
  id?: string
  name?: string
  next?: StructureItem | null
}

/**
 * 格式化架构属性值为显示文本
 */
function formatStructureValue(item: StructureItem | null): string {
  if (!item) return ''
  const names: string[] = []
  let current: StructureItem | null = item
  while (current) {
    if (current.name) {
      names.push(current.name)
    }
    current = current.next || null
  }
  return names.join(' / ')
}

/**
 * 获取字段显示值（统一入口）
 *
 * @param card 卡片数据
 * @param fieldId 字段 ID（属性定义 ID）
 * @param renderSource 渲染配置源（ColumnMeta[] 或 FieldRenderMeta[]）
 * @param statusOptions 价值流状态选项（用于 $statusId 内置字段）
 */
export function renderFieldValue(
  card: CardDTO,
  fieldId: string,
  renderSource?: RenderConfigSource,
  statusOptions?: StatusOption[],
): string {
  // 1. 处理内置字段（以 $ 开头）
  if (isBuiltinField(fieldId)) {
    return getBuiltinFieldDisplayValue(card, fieldId, statusOptions)
  }

  // 2. 处理特殊字段
  if (fieldId === 'title') {
    return card.title?.displayValue || '-'
  }
  if (fieldId === 'code' || fieldId === 'serialNumber') {
    return card.customCode || String(card.codeInOrg || '-')
  }

  // 3. 处理关联字段（LINK 类型）
  // LINK 类型字段的数据存储在 card.linkedCards 中，而非 fieldValues
  if (isValidLinkFieldId(fieldId)) {
    const linkedCards = card.linkedCards?.[fieldId]
    if (!linkedCards || linkedCards.length === 0) return '-'
    // 显示关联卡片的标题，多个用顿号分隔
    return linkedCards
      .map(c => c.title?.displayValue || (c.title?.type === 'PURE' ? c.title.value : null) || '-')
      .join('、')
  }

  // 4. 获取字段值
  const fieldValue = card.fieldValues?.[fieldId]
  if (!fieldValue) return '-'

  // 5. 获取渲染配置
  const renderConfig = getRenderConfig(fieldId, renderSource)

  // 6. 根据渲染配置格式化字段值
  return formatFieldValueByConfig(fieldValue, renderConfig)
}

/**
 * 判断是否为内置枚举字段（$cardStyle 或 $statusId）
 */
export function isBuiltinEnumField(fieldId: string): boolean {
  return fieldId === '$cardStyle' || fieldId === '$statusId'
}

/**
 * 获取内置枚举字段的选项列表（带颜色）
 *
 * @param card 卡片数据
 * @param fieldId 字段 ID（$cardStyle 或 $statusId）
 * @param statusOptions 价值流状态选项列表（用于 $statusId 字段）
 */
export function getBuiltinEnumOptions(
  card: CardDTO,
  fieldId: string,
  statusOptions?: StatusOption[],
): EnumOptionDTO[] {
  if (fieldId === '$cardStyle') {
    const style = card.cardStyle
    if (!style) return []
    return [{
      id: style,
      label: CARD_STYLE_LABELS[style] || style,
      color: CARD_STYLE_COLORS[style] || '',
      enabled: true,
    }]
  }
  if (fieldId === '$statusId') {
    const statusId = card.statusId
    if (!statusId) return []
    if (statusOptions) {
      const option = statusOptions.find((opt) => opt.id === statusId)
      if (option) {
        return [{
          id: option.id,
          label: option.name,
          color: getStepKindColor(option.stepKind),
          enabled: true,
        }]
      }
    }
    return [{ id: statusId, label: statusId, color: getStepKindColor(), enabled: true }]
  }
  return []
}

/**
 * 获取枚举字段的已选选项列表（按定义顺序）
 *
 * @param fieldValue 字段值
 * @param renderConfig 渲染配置
 */
export function getEnumSelectedOptions(
  fieldValue: FieldValue | null | undefined,
  renderConfig?: FieldRenderConfig | null,
): EnumOptionDTO[] {
  if (!fieldValue || fieldValue.type !== 'ENUM' || !Array.isArray(fieldValue.value)) {
    return []
  }
  if (renderConfig?.type !== 'ENUM') {
    return []
  }
  const enumConfig = renderConfig as EnumRenderConfig
  const options = enumConfig.options || []
  const selectedIds = new Set(fieldValue.value as string[])

  // 按照选项定义的顺序返回已选中的选项
  return options.filter(opt => selectedIds.has(opt.id))
}
