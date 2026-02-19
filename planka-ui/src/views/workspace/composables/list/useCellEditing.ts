/**
 * 单元格编辑 Composable
 * 处理列表视图中单元格的行内编辑逻辑
 */
import { ref, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import dayjs from 'dayjs'
import type {
  ColumnMeta,
  FieldRenderConfig,
  EnumRenderConfig,
  StatusOption,
} from '@/types/view-data'
import type { CardDTO, UpdateCardRequest, FieldValue, LinkedCard } from '@/types/card'
import { isValidLinkFieldId } from '@/utils/link-field-utils'
import { getCardId } from '@/types/card'
import { getFieldDisplayValue } from '@/types/view-data'
import { cardApi } from '@/api/card'
import { linkCardApi } from '@/api/link-card'
import { getStepKindColor } from '@/types/builtin-field'

/** 编辑中的单元格信息 */
export interface EditingCell {
  cardId: string
  /**
   * 字段 ID
   * 对于关联字段，格式为 "{linkTypeId}:{SOURCE|TARGET}"
   */
  fieldId: string
  fieldType: string
  originalValue: unknown
  renderConfig?: FieldRenderConfig
}

export function useCellEditing(
  columns: () => ColumnMeta[],
  cards: () => CardDTO[],
  updateCards: (updater: (cards: CardDTO[]) => CardDTO[]) => void,
  statusOptions: () => StatusOption[]
) {
  const { t } = useI18n()

  // Edit state
  const editingCell = ref<EditingCell | null>(null)
  const editingTextValue = ref('')
  const editingNumberValue = ref<number | undefined>(undefined)
  const editingDateValue = ref<string>('')
  const editingEnumValue = ref<string[]>([])
  const editingLinkValue = ref<LinkedCard[]>([])
  const editingStructureValue = ref<FieldValue | null>(null)
  const cellSaving = ref(false)

  /** 判断单元格是否正在编辑 */
  function isEditing(cardId: string, fieldId: string): boolean {
    if (!editingCell.value || !cardId) return false
    return editingCell.value.cardId === cardId && editingCell.value.fieldId === fieldId
  }

  /** 获取列配置 */
  function getColumnConfig(fieldId: string): ColumnMeta | undefined {
    return columns().find((col) => col.fieldId === fieldId)
  }

  /** 开始编辑单元格 */
  function startEditing(record: CardDTO, fieldId: string) {
    const cardId = getCardId(record)
    if (!cardId) return
    const colConfig = getColumnConfig(fieldId)
    if (!colConfig) return
    // 检查列是否可编辑
    if (colConfig.editable === false) return
    startEditingCell(cardId, fieldId, record, colConfig)
  }

  /** 开始编辑单元格（内部实现） */
  function startEditingCell(cardId: string, fieldId: string, card: CardDTO, colConfig: ColumnMeta) {
    const fieldType = colConfig.renderConfig?.type || colConfig.fieldType || 'TEXT'
    const fieldValue = card.fieldValues?.[fieldId]

    // $statusId 特殊处理：构建状态选项的 renderConfig
    let renderConfig = colConfig.renderConfig
    if (fieldId === '$statusId') {
      renderConfig = {
        type: 'ENUM',
        multiSelect: false,
        options: statusOptions().map(opt => ({
          id: opt.id,
          label: opt.name,
          color: getStepKindColor(opt.stepKind),
          enabled: true,
        })),
      } as EnumRenderConfig
    }

    // 计算原始值（用于判断是否有变化）
    let originalValue: unknown = fieldValue?.value ?? null
    if (fieldId === '$statusId') {
      originalValue = card.statusId
    } else if (fieldId === 'title') {
      // title 字段特殊处理：从 card.title 获取
      originalValue = (card.title?.type === 'PURE' ? card.title.value : card.title?.displayValue) || ''
    } else if (fieldType === 'LINK') {
      // LINK 类型：从 linkedCards 获取当前关联的卡片
      // fieldId 格式已经是 "{linkTypeId}:{SOURCE|TARGET}"，直接作为 key 使用
      if (isValidLinkFieldId(fieldId)) {
        const linkedCards = card.linkedCards?.[fieldId]
        originalValue = linkedCards ? linkedCards.map((c: CardDTO) => c.id) : []
      }
    }

    // 初始化编辑状态
    editingCell.value = {
      cardId,
      fieldId,
      fieldType,
      originalValue,
      renderConfig,
    }

    // 根据字段类型初始化编辑值
    switch (fieldType) {
      case 'NUMBER':
        editingNumberValue.value = typeof fieldValue?.value === 'number' ? fieldValue.value : undefined
        break
      case 'DATE':
        if (fieldValue?.value && typeof fieldValue.value === 'number') {
          editingDateValue.value = dayjs(fieldValue.value).format('YYYY-MM-DD')
        } else {
          editingDateValue.value = ''
        }
        break
      case 'ENUM':
        // $statusId 特殊处理：使用 card.statusId
        if (fieldId === '$statusId') {
          editingEnumValue.value = card.statusId ? [card.statusId] : []
        } else if (Array.isArray(fieldValue?.value)) {
          // 枚举值已经是选项 ID 列表
          editingEnumValue.value = fieldValue.value as string[]
        } else {
          editingEnumValue.value = []
        }
        break
      case 'LINK':
        // LINK 类型：从 linkedCards 获取当前关联的卡片
        // fieldId 格式已经是 "{linkTypeId}:{SOURCE|TARGET}"，直接作为 key 使用
        if (isValidLinkFieldId(fieldId)) {
          const linkedCards = card.linkedCards?.[fieldId]
          if (linkedCards && Array.isArray(linkedCards)) {
            editingLinkValue.value = linkedCards.map((c: CardDTO) => ({
              cardId: c.id,
              title: c.title,
            }))
          } else {
            editingLinkValue.value = []
          }
        } else {
          editingLinkValue.value = []
        }
        break
      case 'STRUCTURE':
        // STRUCTURE 类型：直接使用 fieldValue
        editingStructureValue.value = fieldValue || null
        break
      default:
        const displayValue = getFieldDisplayValue(card, fieldId, columns())
        editingTextValue.value = displayValue === '-' ? '' : displayValue
    }

    // 聚焦输入框（LINK 和 STRUCTURE 类型不需要聚焦，它们会自动弹出选择器）
    if (fieldType !== 'LINK' && fieldType !== 'STRUCTURE') {
      nextTick(() => {
        const input = document.querySelector('.cell-editing-input input, .cell-editing-input .arco-input') as HTMLInputElement
        if (input) {
          input.focus()
          if (fieldType === 'TEXT' || fieldType === 'TEXTAREA' || fieldType === 'MARKDOWN') {
            input.select()
          }
        }
      })
    }
  }

  /** 取消编辑 */
  function cancelEditing() {
    editingCell.value = null
    editingTextValue.value = ''
    editingNumberValue.value = undefined
    editingDateValue.value = ''
    editingEnumValue.value = []
    editingLinkValue.value = []
    editingStructureValue.value = null
  }

  /** 构建 FieldValue */
  function buildFieldValue(fieldId: string, fieldType: string, _renderConfig?: FieldRenderConfig): FieldValue | null {
    switch (fieldType) {
      case 'NUMBER':
        return {
          type: 'NUMBER',
          fieldId,
          value: editingNumberValue.value ?? null,
          readable: true,
        } as FieldValue
      case 'DATE':
        return {
          type: 'DATE',
          fieldId,
          value: editingDateValue.value ? dayjs(editingDateValue.value).valueOf() : null,
          readable: true,
        } as FieldValue
      case 'ENUM': {
        // Handle case where single-select enum might set value as string instead of array
        const rawValue = editingEnumValue.value as unknown
        const currentIds = Array.isArray(rawValue)
          ? rawValue as string[]
          : (rawValue ? [rawValue as string] : [])

        // 直接返回选项 ID 列表
        return {
          type: 'ENUM',
          fieldId,
          value: currentIds.length > 0 ? currentIds : null,
          readable: true,
        } as FieldValue
      }
      case 'STRUCTURE':
        // STRUCTURE 类型直接返回编辑值（已经是 FieldValue 格式）
        return editingStructureValue.value
      default:
        return {
          type: 'TEXT',
          fieldId,
          value: editingTextValue.value.trim() || null,
          readable: true,
        } as FieldValue
    }
  }

  /** 检查值是否有变化 */
  function isValueChanged(fieldId: string, fieldType: string, originalValue: unknown): boolean {
    // $statusId 特殊处理：originalValue 是单个 statusId 字符串
    if (fieldId === '$statusId') {
      const rawValue = editingEnumValue.value as unknown
      const currentIds = Array.isArray(rawValue)
        ? rawValue as string[]
        : (rawValue ? [rawValue as string] : [])
      const newStatusId = currentIds[0] || null
      return newStatusId !== originalValue
    }

    switch (fieldType) {
      case 'NUMBER':
        return editingNumberValue.value !== originalValue
      case 'DATE': {
        const newTimestamp = editingDateValue.value ? dayjs(editingDateValue.value).valueOf() : null
        return newTimestamp !== originalValue
      }
      case 'ENUM': {
        // 枚举值已经是选项 ID 列表
        const originalIds = Array.isArray(originalValue)
          ? (originalValue as string[]).sort().join(',')
          : ''

        const rawValue = editingEnumValue.value as unknown
        const currentIds = Array.isArray(rawValue)
          ? rawValue as string[]
          : (rawValue ? [rawValue as string] : [])

        const newIds = [...currentIds].sort().join(',')
        return newIds !== originalIds
      }
      case 'LINK': {
        // LINK 类型：比较关联卡片 ID 列表
        const originalIds = Array.isArray(originalValue)
          ? (originalValue as string[]).sort().join(',')
          : ''
        const currentIds = editingLinkValue.value.map((c) => c.cardId).sort().join(',')
        return currentIds !== originalIds
      }
      case 'STRUCTURE': {
        // STRUCTURE 类型：比较 JSON 字符串
        const originalJson = originalValue ? JSON.stringify(originalValue) : ''
        const currentJson = editingStructureValue.value?.value
          ? JSON.stringify(editingStructureValue.value.value)
          : ''
        return currentJson !== originalJson
      }
      default:
        return editingTextValue.value.trim() !== (originalValue || '')
    }
  }

  /** 保存单元格编辑 */
  async function saveEditing() {
    if (!editingCell.value || cellSaving.value) return

    const { cardId, fieldId, fieldType, originalValue, renderConfig } = editingCell.value

    // 值未改变，直接取消编辑
    if (!isValueChanged(fieldId, fieldType, originalValue)) {
      cancelEditing()
      return
    }

    // Find the corresponding card
    const card = cards().find((c) => getCardId(c) === cardId)
    if (!card) {
      Message.error(t('common.card.notFound'))
      cancelEditing()
      return
    }

    cellSaving.value = true

    try {
      // $statusId 特殊处理：调用移动 API
      if (fieldId === '$statusId') {
        const rawValue = editingEnumValue.value as unknown
        const currentIds = Array.isArray(rawValue)
          ? rawValue as string[]
          : (rawValue ? [rawValue as string] : [])
        const newStatusId = currentIds[0]

        if (!newStatusId) {
          Message.warning(t('common.card.selectTargetStatus'))
          cancelEditing()
          return
        }

        if (!card.streamId) {
          Message.error(t('common.card.noValueStream'))
          cancelEditing()
          return
        }

        await cardApi.move({
          cardId,
          streamId: card.streamId,
          toStatusId: newStatusId,
        })

        // 更新本地卡片的 statusId
        updateCards((currentCards) => {
          const cardIndex = currentCards.findIndex((c) => getCardId(c) === cardId)
          if (cardIndex === -1) return currentCards
          const newCards = [...currentCards]
          newCards[cardIndex] = {
            ...currentCards[cardIndex],
            statusId: newStatusId,
          } as CardDTO
          return newCards
        })

        Message.success(t('common.card.statusUpdateSuccess'))
        cancelEditing()
        return
      }

      // LINK type: call link update API
      if (fieldType === 'LINK') {
        // fieldId format is already "{linkTypeId}:{SOURCE|TARGET}"
        if (!isValidLinkFieldId(fieldId)) {
          Message.error(t('common.card.invalidLinkFieldId'))
          cancelEditing()
          return
        }

        await linkCardApi.updateLink({
          cardId,
          linkFieldId: fieldId,
          targetCardIds: editingLinkValue.value.map((c) => c.cardId),
        })

        // 更新本地卡片的 linkedCards
        updateCards((currentCards) => {
          const cardIndex = currentCards.findIndex((c) => getCardId(c) === cardId)
          if (cardIndex === -1) return currentCards
          const currentCard = currentCards[cardIndex]
          if (!currentCard) return currentCards

          const newCards = [...currentCards]

          // 构建新的 linkedCards 数据，直接使用 fieldId 作为 key
          const newLinkedCards = editingLinkValue.value.map((lc) => ({
            id: lc.cardId,
            title: lc.title,
          } as CardDTO))

          newCards[cardIndex] = {
            ...currentCard,
            linkedCards: {
              ...currentCard.linkedCards,
              [fieldId]: newLinkedCards,
            },
          } as CardDTO
          return newCards
        })

        Message.success(t('common.card.linkUpdateSuccess'))
        cancelEditing()
        return
      }

      // Regular field update
      const updateRequest: UpdateCardRequest = {
        cardId,
        title: fieldId === 'title' ? editingTextValue.value.trim() : undefined,
      }

      if (fieldId !== 'title') {
        const fieldValue = buildFieldValue(fieldId, fieldType, renderConfig)
        if (fieldValue) {
          updateRequest.fieldValues = { [fieldId]: fieldValue }
        }
      }

      await cardApi.update(updateRequest)

      // Update local data
      updateLocalCardData(cardId, fieldId, fieldType, renderConfig)

      Message.success(t('common.message.saveSuccess'))
      cancelEditing()
    } catch (error: any) {
      console.error('Failed to save cell:', error)
      // 错误提示已由全局拦截器 (request.ts) 处理，此处不再重复显示
    } finally {
      cellSaving.value = false
    }
  }

  /** 更新本地卡片数据 */
  function updateLocalCardData(cardId: string, fieldId: string, fieldType: string, renderConfig?: FieldRenderConfig) {
    updateCards((currentCards) => {
      const cardIndex = currentCards.findIndex((c) => getCardId(c) === cardId)
      if (cardIndex === -1) return currentCards

      const currentCard = currentCards[cardIndex]
      if (!currentCard) return currentCards

      const newCards = [...currentCards]

      if (fieldId === 'title') {
        const newTitle = editingTextValue.value.trim()
        newCards[cardIndex] = {
          ...currentCard,
          title: { type: 'PURE', value: newTitle, displayValue: newTitle },
        } as CardDTO
      } else {
        const newFieldValue = buildFieldValue(fieldId, fieldType, renderConfig)
        if (newFieldValue) {
          newCards[cardIndex] = {
            ...currentCard,
            fieldValues: {
              ...currentCard.fieldValues,
              [fieldId]: newFieldValue,
            },
          } as CardDTO
        }
      }

      return newCards
    })
  }

  /** 处理编辑输入框按键 */
  function handleEditKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      event.preventDefault()
      saveEditing()
    } else if (event.key === 'Escape') {
      event.preventDefault()
      cancelEditing()
    }
  }

  return {
    // 状态
    editingCell,
    editingTextValue,
    editingNumberValue,
    editingDateValue,
    editingEnumValue,
    editingLinkValue,
    editingStructureValue,
    cellSaving,
    // 方法
    isEditing,
    startEditing,
    cancelEditing,
    saveEditing,
    handleEditKeydown,
  }
}
