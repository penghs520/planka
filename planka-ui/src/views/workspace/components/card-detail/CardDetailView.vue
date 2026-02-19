<script setup lang="ts">
/**
 * 卡片详情视图组件
 * 包含头部（编号、标题、状态、分享）和内容区域（标签页、字段）
 * 用于抽屉详情页和独立详情页
 */
import { ref, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconLock, IconShareInternal, IconCheckCircleFill, IconCloseCircleFill, IconDown, IconRight, IconExport, IconClockCircle } from '@arco-design/web-vue/es/icon'
import dayjs from 'dayjs'
import { cardApi, attendanceApi } from '@/api'
import { valueStreamBranchApi, type StatusOption } from '@/api/value-stream'
import type { CardDTO, CardDetailResponse, FieldRenderMeta, CardTypeInfo, ValueStreamStatusInfo } from '@/types/card'
import { getCardCode, getCardTitle, getCardTitleValue, getJointPartsText, isJointTitle } from '@/types/card'
import FieldDisplay from '@/components/field/display/FieldDisplay.vue'
import FieldEnumDisplay from '@/components/field/display/FieldEnumDisplay.vue'
import FieldMarkdownDisplay from '@/components/field/display/FieldMarkdownDisplay.vue'
import FieldEditor from '@/components/field/editor/FieldEditor.vue'
import FieldMarkdownEditor from '@/components/field/editor/FieldMarkdownEditor.vue'
import type { EnumOptionDTO } from '@/types/view-data'
import type { CardDetailTemplateDefinition, TabConfig, SectionConfig, FieldItemConfig } from '@/types/card-detail-template'
import { FieldRowSpacingConfig } from '@/types/card-detail-template'
import type { CardFieldControls, FieldControlConfig } from '@/types/field-control'
import { useCardTabsStore } from '@/stores/cardTabs'
import { isBuiltinField, getBuiltinField, getStepKindColor, CARD_STYLE_LABELS, CARD_STYLE_COLORS } from '@/types/builtin-field'
import { getFieldName } from '@/utils/field-render'
import ActivityLogPanel from '@/components/history/ActivityLogPanel.vue'
import CommentPanel from '@/components/comment/CommentPanel.vue'
import { isValidLinkFieldId } from '@/utils/link-field-utils'
import { linkCardApi } from '@/api/link-card'
import type { FieldValue, LinkedCard, UpdateCardRequest } from '@/types/card'
import type { FieldRenderConfig } from '@/types/view-data'
import CardActionButtons from './CardActionButtons.vue'
import WorklogModal from '@/components/worklog/WorklogModal.vue'

import html2pdf from 'html2pdf.js'

const { t } = useI18n()

const props = defineProps<{
  /** 卡片 ID */
  cardId: string
}>()

const emit = defineEmits<{
  /** 卡片信息加载完成 */
  loaded: [info: { code: string; title: string; cardStyle?: string }]
}>()

const cardTabsStore = useCardTabsStore()

// 状态
const loading = ref(false)

// ==================== 报工相关 ====================
const worklogModalVisible = ref(false)

// 是否显示报工按钮（TODO: 后续根据后端配置判断）
const showWorklogButton = computed(() => {
  // 暂时返回 false，等后端配置接口完成后改为 true
  // return card.value?.typeId?.includes('requirement') ?? false
  return false
})

// 打开报工弹窗
function openWorklogModal() {
  worklogModalVisible.value = true
}

// 报工成功回调
function handleWorklogSuccess() {
  // 刷新卡片数据或显示提示
  fetchCardDetail()
}
const card = ref<CardDTO | null>(null)
const template = ref<CardDetailTemplateDefinition | null>(null)
const fieldRenderMetas = ref<FieldRenderMeta[]>([])
const fieldControls = ref<CardFieldControls>({})
const activeTabId = ref<string>('')
const cardTypeInfo = ref<CardTypeInfo | null>(null)
const valueStreamStatusInfo = ref<ValueStreamStatusInfo | null>(null)

// ==================== 头部相关 ====================
const cardCode = computed(() => (card.value ? getCardCode(card.value) : ''))
const cardTitle = computed(() => (card.value ? getCardTitle(card.value) : ''))
const displayCode = computed(() => cardCode.value ? `#${cardCode.value}` : '')
const displayTitle = computed(() => cardTitle.value || '加载中...')
const shareUrl = computed(() => `${window.location.origin}/card/${props.cardId}`)

// 拼接标题相关
const isJoint = computed(() => isJointTitle(card.value?.title))
const jointArea = computed(() => card.value?.title?.type === 'JOINT' ? card.value.title.area : null)
const jointPartsText = computed(() => getJointPartsText(card.value?.title))
const rawTitleValue = computed(() => getCardTitleValue(card.value?.title))


// 价值流状态枚举选项（用于 EnumDisplay 组件）
const statusEnumOptions = computed<EnumOptionDTO[]>(() => {
  if (!valueStreamStatusInfo.value) return []
  return [{
    id: valueStreamStatusInfo.value.statusId,
    label: valueStreamStatusInfo.value.statusName,
    color: getStepKindColor(valueStreamStatusInfo.value.stepKind),
    enabled: true,
  }]
})

// 卡片丢弃/归档状态标签（仅非活跃状态显示）
const cardStyleLabel = computed(() => {
  const style = card.value?.cardStyle
  if (!style || style === 'ACTIVE') return ''
  return CARD_STYLE_LABELS[style] || ''
})

// 卡片丢弃/归档状态颜色
const cardStyleColor = computed(() => {
  const style = card.value?.cardStyle
  if (!style || style === 'ACTIVE') return ''
  return CARD_STYLE_COLORS[style] || '#8c8c8c'
})

// 复制状态（防止重复点击）
const isCopying = ref(false)

// 价值流状态编辑
const isEditingStatus = ref(false)
const statusOptionsLoading = ref(false)
const allStatusOptions = ref<EnumOptionDTO[]>([])
const statusStepKindMap = ref<Map<string, 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'>>(new Map()) // statusId -> stepKind
const selectedStatusId = ref<string>('')

// 标题编辑状态
const isEditingTitle = ref(false)
const editingTitle = ref('')
const titleInputRef = ref<HTMLTextAreaElement | null>(null)

// 描述编辑器引用
const descriptionEditorRef = ref<InstanceType<typeof FieldMarkdownEditor> | null>(null)

/** 获取描述编辑器实例（v-for 中 ref 可能为数组） */
function getDescriptionEditor(): InstanceType<typeof FieldMarkdownEditor> | null {
  const ref = descriptionEditorRef.value
  return Array.isArray(ref) ? ref[0] ?? null : ref
}

// 内部消息提示
const internalMessage = ref<{ type: 'success' | 'error'; text: string } | null>(null)
let messageTimer: ReturnType<typeof setTimeout> | null = null

function showInternalMessage(type: 'success' | 'error', text: string) {
  if (messageTimer) {
    clearTimeout(messageTimer)
  }
  internalMessage.value = { type, text }
  messageTimer = setTimeout(() => {
    internalMessage.value = null
  }, 2000)
}

// 标签页操作
function handleTabClick(cardId: string) {
  cardTabsStore.activateTab(cardId)
}

function handleTabClose(cardId: string) {
  cardTabsStore.closeTab(cardId)
}

// 自动调整 textarea 高度
function autoResizeTextarea() {
  const textarea = titleInputRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = `${textarea.scrollHeight}px`
  }
}

// 开始编辑标题
function startEditTitle() {
  isEditingTitle.value = true
  // 编辑时只编辑原始标题部分（对于拼接标题是 name，对于纯标题是 value）
  editingTitle.value = rawTitleValue.value
  nextTick(() => {
    const textarea = titleInputRef.value
    if (textarea) {
      textarea.focus()
      // 将光标移到末尾
      textarea.setSelectionRange(textarea.value.length, textarea.value.length)
      autoResizeTextarea()
    }
  })
}

// 取消编辑标题
function cancelEditTitle() {
  isEditingTitle.value = false
  editingTitle.value = ''
}

// 保存标题
async function saveTitle() {
  const newTitle = editingTitle.value.trim()
  if (!newTitle) {
    Message.warning('标题不能为空')
    return
  }
  // 与原始标题值比较（不是 displayValue）
  if (newTitle === rawTitleValue.value) {
    cancelEditTitle()
    return
  }
  try {
    await cardApi.update({
      cardId: props.cardId,
      title: newTitle,
    })
    // 更新本地数据
    if (card.value) {
      if (card.value.title.type === 'JOINT') {
        // 拼接标题：更新 value 和 displayValue
        const oldTitle = card.value.title
        const newDisplayValue = oldTitle.area === 'PREFIX' 
          ? jointPartsText.value + newTitle 
          : newTitle + jointPartsText.value
        card.value.title = { 
          ...oldTitle, 
          value: newTitle, 
          displayValue: newDisplayValue 
        }
      } else {
        // 纯标题
        card.value.title = { type: 'PURE', value: newTitle, displayValue: newTitle }
      }
    }
    // 更新标签页信息
    cardTabsStore.updateTab(props.cardId, { title: newTitle })
    showInternalMessage('success', '标题已更新')
    cancelEditTitle()
  } catch (error: any) {
    showInternalMessage('error', error.message || '保存失败')
  }
}

// 处理标题输入框按键
function handleTitleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    event.preventDefault()
    saveTitle()
  } else if (event.key === 'Escape') {
    cancelEditTitle()
  }
}

// 复制编号和标题
async function handleCopyCodeAndTitle() {
  if (isCopying.value) return
  isCopying.value = true
  const text = `${displayCode.value} ${displayTitle.value}`
  try {
    await navigator.clipboard.writeText(text)
    Message.success('已复制')
  } catch {
    Message.error('复制失败')
  } finally {
    setTimeout(() => {
      isCopying.value = false
    }, 1000)
  }
}

// 复制分享链接
async function handleShare() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    Message.success('链接已复制')
  } catch {
    Message.error('复制失败')
  }
}

// ==================== 价值流状态编辑 ====================
// 加载所有状态选项
async function loadStatusOptions() {
  if (!card.value?.typeId || allStatusOptions.value.length > 0) return

  statusOptionsLoading.value = true
  try {
    const statusList: StatusOption[] = await valueStreamBranchApi.getStatusOptions(card.value.typeId)
    // 保存 stepKind 映射
    type StepKindType = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'
    const stepKindMapData = new Map<string, StepKindType>()
    statusList.forEach(status => {
      if (status.stepKind) {
        stepKindMapData.set(status.id, status.stepKind as StepKindType)
      }
    })
    statusStepKindMap.value = stepKindMapData

    allStatusOptions.value = statusList.map(status => ({
      id: status.id,
      label: status.name,
      color: getStepKindColor(status.stepKind),
      enabled: true,
    }))
  } catch (error) {
    console.error('Failed to load status options:', error)
  } finally {
    statusOptionsLoading.value = false
  }
}

// 开始编辑状态
async function startEditStatus() {
  if (!card.value?.statusId) return
  selectedStatusId.value = card.value.statusId
  await loadStatusOptions()
  isEditingStatus.value = true
}

// 取消编辑状态
function cancelEditStatus() {
  isEditingStatus.value = false
}

// 处理状态变更
async function handleStatusChange(newStatusId: string | number | boolean | Record<string, unknown> | (string | number | boolean | Record<string, unknown>)[]) {
  const statusId = String(newStatusId)
  if (!card.value || statusId === card.value.statusId) {
    cancelEditStatus()
    return
  }

  if (!card.value.streamId) {
    Message.error('卡片未配置价值流')
    cancelEditStatus()
    return
  }

  try {
    await cardApi.move({
      cardId: props.cardId,
      streamId: card.value.streamId,
      toStatusId: statusId,
    })

    // 更新本地数据
    card.value.statusId = statusId
    const option = allStatusOptions.value.find(opt => opt.id === statusId)
    if (option) {
      valueStreamStatusInfo.value = {
        statusId,
        statusName: option.label,
        stepKind: statusStepKindMap.value.get(statusId),
      }
    }

    showInternalMessage('success', '状态更新成功')
  } catch (error: any) {
    showInternalMessage('error', error.message || '状态更新失败')
  } finally {
    cancelEditStatus()
  }
}

// 状态下拉框关闭时
function handleStatusPopupVisibleChange(visible: boolean) {
  if (!visible) {
    cancelEditStatus()
  }
}

// ==================== 内容相关 ====================
// 当前激活的标签页
const activeTab = computed(() => {
  const tab = template.value?.tabs.find((t) => t.tabId === activeTabId.value)
  console.log('[CardDetailView] activeTab computed:', {
    activeTabId: activeTabId.value,
    foundTab: tab?.tabId,
    tabName: tab?.name,
    allTabIds: template.value?.tabs?.map(t => t.tabId)
  })
  return tab
})

// 当前标签页的字段行间距
const fieldRowGap = computed(() => {
  const spacing = activeTab.value?.fieldRowSpacing || 'NORMAL'
  return FieldRowSpacingConfig[spacing]?.rowGap || '6px'
})

// 获取卡片详情
async function fetchCardDetail() {
  if (!props.cardId) return

  loading.value = true
  cardTabsStore.updateTab(props.cardId, { loading: true })

  try {
    const response: CardDetailResponse = await cardApi.getDetail(props.cardId)

    card.value = response.card
    template.value = response.template
    fieldRenderMetas.value = response.fieldRenderMetas || []
    fieldControls.value = response.fieldControls || {}
    cardTypeInfo.value = response.cardTypeInfo || null
    valueStreamStatusInfo.value = response.valueStreamStatusInfo || null

    // 调试日志
    console.log('[CardDetailView] 卡片详情加载成功:', {
      cardId: card.value?.id,
      typeId: card.value?.typeId,
      hasTemplate: !!template.value,
      templateId: template.value?.id,
      tabsCount: template.value?.tabs?.length,
      tabs: template.value?.tabs?.map(t => ({ id: t.tabId, name: t.name, type: t.tabType })),
      fieldRenderMetasCount: fieldRenderMetas.value?.length,
    })

    // 初始化激活标签页
    const tabs = template.value?.tabs
    console.log('[CardDetailView] 初始化标签页, tabs:', tabs?.length, 'activeTabId:', activeTabId.value)
    if (tabs && tabs.length > 0) {
      // 如果没有激活的标签页，或者当前激活的标签页不在新的 tabs 列表中，则选择第一个
      const currentTabExists = tabs.some(t => t.tabId === activeTabId.value)
      if (!activeTabId.value || !currentTabExists) {
        activeTabId.value = tabs[0]!.tabId
        console.log('[CardDetailView] 设置默认标签页:', activeTabId.value)
      }
    }

    // 更新标签页信息
    cardTabsStore.updateTab(props.cardId, {
      title: cardTitle.value,
      code: cardCode.value,
      cardStyle: card.value?.cardStyle,
      loading: false,
    })

    // 触发加载完成事件
    emit('loaded', {
      code: cardCode.value,
      title: cardTitle.value,
      cardStyle: card.value?.cardStyle,
    })
  } catch (error: any) {
    console.error('Failed to fetch card detail:', error)
    card.value = null
    cardTabsStore.updateTab(props.cardId, { loading: false })
  } finally {
    loading.value = false
  }
}

// 监听卡片 ID 变化
watch(
  () => props.cardId,
  (newCardId) => {
    if (newCardId) {
      activeTabId.value = ''
      fetchCardDetail()
    }
  },
  { immediate: true },
)

// 判断标签页类型
function isBasicInfoTab(tab: TabConfig) {
  return tab.tabType === 'SYSTEM' && tab.systemTabType === 'BASIC_INFO'
}

function isCommentTab(tab: TabConfig) {
  return tab.tabType === 'SYSTEM' && tab.systemTabType === 'COMMENT'
}

function isActivityLogTab(tab: TabConfig) {
  return tab.tabType === 'SYSTEM' && tab.systemTabType === 'ACTIVITY_LOG'
}

// 获取字段控制配置
function getFieldControl(fieldConfigId: string): FieldControlConfig | undefined {
  return fieldControls.value[fieldConfigId]
}

// 获取字段显示名称
function getFieldLabel(fieldConfigId: string): string {
  const name = getFieldName(fieldConfigId, fieldRenderMetas.value)
  if (name !== fieldConfigId) return name

  if (isBuiltinField(fieldConfigId)) {
    const builtinDef = getBuiltinField(fieldConfigId)
    return builtinDef?.name || fieldConfigId
  }

  return fieldConfigId
}

// 判断是否是描述字段
function isDescriptionField(fieldConfigId: string): boolean {
  return fieldConfigId === '$description'
}

// 获取字段渲染配置
function getFieldRenderConfig(fieldConfigId: string) {
  const meta = fieldRenderMetas.value.find(m => m.fieldId === fieldConfigId)
  return meta?.renderConfig
}

// 获取字段值
function getFieldValue(fieldConfigId: string) {
  return card.value?.fieldValues?.[fieldConfigId]
}



// 计算区域的字段行布局
function getFieldRows(section: SectionConfig): FieldItemConfig[][] {
  const rows: FieldItemConfig[][] = []
  let currentRow: FieldItemConfig[] = []
  let currentRowWidth = 0

  for (const item of section.fieldItems || []) {
    if (item.startNewRow || currentRowWidth + item.widthPercent > 100) {
      if (currentRow.length > 0) {
        rows.push(currentRow)
      }
      currentRow = [item]
      currentRowWidth = item.widthPercent
    } else {
      currentRow.push(item)
      currentRowWidth += item.widthPercent
    }
  }

  if (currentRow.length > 0) {
    rows.push(currentRow)
  }

  return rows
}

// 切换区域折叠状态
function toggleSection(section: SectionConfig) {
  if (section.collapsible === false) return
  section.collapsed = !section.collapsed
}

// ==================== 字段编辑 ====================
interface EditingFieldState {
  fieldId: string
  fieldType: string
  originalValue: unknown
  renderConfig?: FieldRenderConfig
}

const editingField = ref<EditingFieldState | null>(null)
const editingFieldValue = ref<any>(null)
const fieldSaving = ref(false)

/** 判断字段是否正在编辑 */
function isEditingField(fieldId: string): boolean {
  return editingField.value?.fieldId === fieldId
}

/** 判断字段是否可编辑 */
function isFieldEditable(fieldId: string): boolean {
  const control = getFieldControl(fieldId)
  return control?.editable !== false
}

/** 开始编辑字段 */
function startEditField(fieldId: string) {
  if (!card.value) return
  if (!isFieldEditable(fieldId)) return
  
  const meta = fieldRenderMetas.value.find(m => m.fieldId === fieldId)
  const fieldValue = card.value.fieldValues?.[fieldId]
  
  // 确定字段类型：优先使用 fieldValue.type，其次 renderConfig.type，然后通过 fieldId 格式判断关联字段
  let fieldType = fieldValue?.type || meta?.renderConfig?.type
  if (!fieldType) {
    if (isValidLinkFieldId(fieldId)) {
      fieldType = 'LINK'
    } else {
      fieldType = 'TEXT'
    }
  }
  
  // 获取原始值
  let originalValue: unknown = fieldValue?.value ?? null
  if (fieldType === 'LINK') {
    const linkedCards = card.value.linkedCards?.[fieldId]
    originalValue = linkedCards ? linkedCards.map((c: CardDTO) => c.id) : []
  }
  
  editingField.value = {
    fieldId,
    fieldType,
    originalValue,
    renderConfig: meta?.renderConfig,
  }
  
  // 初始化编辑值
  initEditingValue(fieldId, fieldType, fieldValue)
}

/** 初始化编辑值 */
function initEditingValue(fieldId: string, fieldType: string, fieldValue: FieldValue | undefined) {
  if (!card.value) return
  
  switch (fieldType) {
    case 'NUMBER':
      editingFieldValue.value = typeof fieldValue?.value === 'number' ? fieldValue.value : undefined
      break
    case 'DATE':
      if (fieldValue?.value && typeof fieldValue.value === 'number') {
        editingFieldValue.value = fieldValue.value
      } else {
        editingFieldValue.value = undefined
      }
      break
    case 'ENUM':
      if (Array.isArray(fieldValue?.value)) {
        editingFieldValue.value = fieldValue.value as string[]
      } else {
        editingFieldValue.value = []
      }
      break
    case 'LINK':
      if (isValidLinkFieldId(fieldId)) {
        const linkedCards = card.value.linkedCards?.[fieldId]
        if (linkedCards && Array.isArray(linkedCards)) {
          editingFieldValue.value = linkedCards.map((c: CardDTO) => ({
            cardId: c.id,
            title: c.title,
          }))
        } else {
          editingFieldValue.value = []
        }
      } else {
        editingFieldValue.value = []
      }
      break
    case 'STRUCTURE':
      editingFieldValue.value = fieldValue || null
      break
    default:
      editingFieldValue.value = fieldValue?.value ?? ''
  }
}

/** 取消编辑字段 */
function cancelEditField() {
  // 如果是描述字段，清理未保存的新上传图片
  if (editingField.value?.fieldId === '$description') {
    getDescriptionEditor()?.onCancelEdit?.()
  }
  editingField.value = null
  editingFieldValue.value = null
}

/** 开始编辑描述字段 */
function startEditDescription() {
  if (!card.value) return

  const descriptionValue = card.value.description?.value || ''
  editingField.value = {
    fieldId: '$description',
    fieldType: 'MARKDOWN',
    originalValue: descriptionValue,
  }
  editingFieldValue.value = descriptionValue
}

/** 保存描述字段 */
async function saveDescriptionField() {
  if (!editingField.value || !card.value || fieldSaving.value) return

  const newDescription = (editingFieldValue.value || '').trim()
  const originalValue = editingField.value.originalValue || ''

  // 检查值是否变化
  if (newDescription === originalValue) {
    cancelEditField()
    return
  }

  fieldSaving.value = true

  try {
    await cardApi.update({
      cardId: props.cardId,
      description: newDescription,
    })

    // 更新本地数据
    if (card.value) {
      card.value = {
        ...card.value,
        description: newDescription ? { value: newDescription } : undefined,
      }
    }

    // 保存成功，批量删除待删除的已保存图片（必须在关闭编辑器之前调用）
    try {
      await getDescriptionEditor()?.onSaveSuccess?.()
    } catch (e) {
      console.error('Failed to cleanup deleted images:', e)
    }

    showInternalMessage('success', '描述已更新')
    editingField.value = null
    editingFieldValue.value = null
  } catch (error: any) {
    console.error('Failed to save description:', error)
    showInternalMessage('error', error.message || '保存描述失败')
  } finally {
    fieldSaving.value = false
  }
}

// ==================== 描述导出功能 ====================
/** 清理 Markdown 中的 HTML 标签，只保留纯文本内容 */
function cleanMarkdownHtml(markdown: string): string {
  return markdown
    // 移除 span 标签但保留内容
    .replace(/<span[^>]*>(.*?)<\/span>/gi, '$1')
    // 移除 mark 标签但保留内容
    .replace(/<mark[^>]*>(.*?)<\/mark>/gi, '$1')
    // 移除 u 标签但保留内容
    .replace(/<u>(.*?)<\/u>/gi, '$1')
    // 移除 br 标签替换为换行
    .replace(/<br\s*\/?>/gi, '\n')
    // 将表格中的 HTML 列表转换为文本格式
    .replace(/<ul>(.*?)<\/ul>/gis, (_, content) => {
      const items = content.match(/<li>(.*?)<\/li>/gi) || []
      return items.map((item: string) => `- ${item.replace(/<\/?li>/gi, '').trim()}`).join('\n')
    })
    .replace(/<ol>(.*?)<\/ol>/gis, (_, content) => {
      const items = content.match(/<li>(.*?)<\/li>/gi) || []
      return items.map((item: string, idx: number) => `${idx + 1}. ${item.replace(/<\/?li>/gi, '').trim()}`).join('\n')
    })
    // 移除其他可能残留的 HTML 标签
    .replace(/<[^>]+>/g, '')
}

/** 导出描述为 Markdown */
function exportDescriptionAsMarkdown() {
  const markdown = card.value?.description?.value
  if (!markdown) {
    Message.warning(t('common.message.descriptionEmpty'))
    return
  }

  // 清理 HTML 标签，只保留纯 Markdown
  const cleanedMarkdown = cleanMarkdownHtml(markdown)

  const blob = new Blob([cleanedMarkdown], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${cardCode.value || 'description'}.md`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/** 导出描述为 PDF（使用 html2pdf.js） */
async function exportDescriptionAsPdf() {
  const markdown = card.value?.description?.value
  if (!markdown) {
    Message.warning(t('common.message.descriptionEmpty'))
    return
  }

  // 从 DOM 获取渲染后的内容
  const descriptionEl = document.querySelector('.description-display-wrapper .ProseMirror')
    || document.querySelector('.description-value .ProseMirror')

  if (!descriptionEl) {
    Message.warning(t('common.message.cannotGetDescription'))
    return
  }

  // 创建临时容器用于导出
  const container = document.createElement('div')
  container.style.cssText = `
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
    padding: 20px;
    max-width: 800px;
    line-height: 1.6;
    color: #333;
  `

  // 添加标题
  const title = `${cardCode.value ? `#${cardCode.value} ` : ''}${cardTitle.value}`
  const titleEl = document.createElement('h1')
  titleEl.style.cssText = 'font-size: 20px; margin-bottom: 16px; border-bottom: 1px solid #eee; padding-bottom: 8px;'
  titleEl.textContent = title
  container.appendChild(titleEl)

  // 克隆描述内容
  const contentEl = descriptionEl.cloneNode(true) as HTMLElement
  contentEl.style.cssText = 'font-size: 14px;'
  container.appendChild(contentEl)

  // 添加样式
  const style = document.createElement('style')
  style.textContent = `
    h1, h2, h3 { margin-top: 16px; margin-bottom: 8px; }
    p { margin: 8px 0; }
    ul, ol { padding-left: 20px; margin: 8px 0; }
    li { margin: 4px 0; }
    table { border-collapse: collapse; width: 100%; margin: 12px 0; }
    th, td { border: 1px solid #ddd; padding: 6px 10px; text-align: left; }
    th { background: #f5f5f5; font-weight: 600; }
    code { background: #f5f5f5; padding: 1px 4px; border-radius: 3px; font-size: 0.9em; }
    pre { background: #f5f5f5; padding: 12px; border-radius: 4px; overflow-x: auto; }
    blockquote { border-left: 3px solid #ddd; margin: 12px 0; padding-left: 12px; color: #666; }
    mark { background-color: #fff3cd; padding: 0 2px; }
  `
  container.appendChild(style)

  // 临时添加到 DOM
  document.body.appendChild(container)

  const filename = `${cardCode.value || 'description'}.pdf`

  try {
    await html2pdf()
      .set({
        margin: 10,
        filename,
        image: { type: 'jpeg', quality: 0.98 },
        html2canvas: { scale: 2, useCORS: true },
        jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
      })
      .from(container)
      .save()

    Message.success(t('common.message.exportPdfSuccess'))
  } catch (error) {
    console.error('PDF export failed:', error)
    Message.error(t('common.message.exportPdfFailed'))
  } finally {
    // 移除临时容器
    document.body.removeChild(container)
  }
}

/** 保存字段编辑 */
async function saveEditField() {
  if (!editingField.value || !card.value || fieldSaving.value) return

  const { fieldId, fieldType, originalValue } = editingField.value

  // 检查值是否变化
  if (!isFieldValueChanged(fieldId, fieldType, originalValue)) {
    cancelEditField()
    return
  }

  fieldSaving.value = true

  try {
    // LINK 类型特殊处理
    if (fieldType === 'LINK' && isValidLinkFieldId(fieldId)) {
      await linkCardApi.updateLink({
        cardId: props.cardId,
        linkFieldId: fieldId,
        targetCardIds: editingFieldValue.value.map((c: LinkedCard) => c.cardId),
      })

      // 更新本地数据
      if (card.value) {
        const newLinkedCards = editingFieldValue.value.map((lc: LinkedCard) => ({
          id: lc.cardId,
          title: lc.title,
        } as CardDTO))

        card.value = {
          ...card.value,
          linkedCards: {
            ...card.value.linkedCards,
            [fieldId]: newLinkedCards,
          },
        }
      }

      showInternalMessage('success', '保存成功')
      cancelEditField()
      return
    }

    // 普通字段更新
    const newFieldValue = buildFieldValue(fieldId, fieldType)
    const updateRequest: UpdateCardRequest = {
      cardId: props.cardId,
      // 更新字段时不需要更新标题
    }

    if (newFieldValue) {
      updateRequest.fieldValues = { [fieldId]: newFieldValue }
    }

    // 如果是考勤申请类型，检查是否需要验证
    const cardTypeId = card.value.typeId
    const isLeaveApplication = cardTypeId?.includes('leave-application')
    const isOvertimeApplication = cardTypeId?.includes('overtime-application')
    const isMakeupApplication = cardTypeId?.includes('makeup-application')
    const isAttendanceApplication = isLeaveApplication || isOvertimeApplication || isMakeupApplication

    if (isAttendanceApplication) {
      // 收集所有字段值（包括当前编辑的字段）
      const allFieldValues: Record<string, any> = {}

      // 添加现有字段值
      if (card.value.fieldValues) {
        for (const [fId, fValue] of Object.entries(card.value.fieldValues)) {
          allFieldValues[fId] = fValue
        }
      }

      // 覆盖当前编辑的字段值
      if (newFieldValue) {
        allFieldValues[fieldId] = newFieldValue
      }

      // 检查必填字段是否都有值
      let shouldValidate = false
      let missingFields: string[] = []
      let autoCalculatedDuration: any = null // 存储自动计算的请假时长

      if (isLeaveApplication) {
        // 请假申请：需要请假类型、开始日期、结束日期、时长
        const leaveTypeField = allFieldValues[`${cardTypeId}:leave-type`]
        const startDateField = allFieldValues[`${cardTypeId}:start-date`]
        const endDateField = allFieldValues[`${cardTypeId}:end-date`]
        let durationField = allFieldValues[`${cardTypeId}:duration`]

        if (!leaveTypeField?.value || (Array.isArray(leaveTypeField.value) && leaveTypeField.value.length === 0)) {
          missingFields.push('请假类型')
        }
        if (!startDateField?.value) {
          missingFields.push('开始日期')
        }
        if (!endDateField?.value) {
          missingFields.push('结束日期')
        }

        // 如果开始日期和结束日期都有值，自动计算或重新计算请假时长
        // 条件：1) 请假时长为空或为 0，或 2) 当前编辑的是开始日期或结束日期字段
        const isEditingDateField = fieldId === `${cardTypeId}:start-date` || fieldId === `${cardTypeId}:end-date`
        const shouldCalculateDuration = startDateField?.value && endDateField?.value &&
          ((!durationField?.value || durationField.value <= 0) || isEditingDateField)

        if (shouldCalculateDuration) {
          const startDate = dayjs(startDateField.value)
          const endDate = dayjs(endDateField.value)

          if (startDate.isValid() && endDate.isValid() && !endDate.isBefore(startDate)) {
            // 计算天数差（包含起始日）
            const daysDiff = endDate.diff(startDate, 'day') + 1

            // 创建请假时长字段值
            durationField = {
              type: 'NUMBER',
              fieldId: `${cardTypeId}:duration`,
              value: daysDiff,
              readable: true
            }

            // 更新到 allFieldValues 中
            allFieldValues[`${cardTypeId}:duration`] = durationField
            // 保存自动计算的请假时长，稍后需要一起保存到后端
            autoCalculatedDuration = durationField

            console.log('[CardDetailView] 自动计算请假时长', {
              startDate: startDate.format('YYYY-MM-DD'),
              endDate: endDate.format('YYYY-MM-DD'),
              duration: daysDiff,
              reason: isEditingDateField ? '编辑日期字段' : '请假时长为空'
            })
          }
        }

        if (!durationField?.value || durationField.value <= 0) {
          missingFields.push('请假时长')
        }

        shouldValidate = missingFields.length === 0
      } else if (isOvertimeApplication) {
        // 加班申请：需要加班日期、时长
        const overtimeDateField = allFieldValues[`${cardTypeId}:overtime-date`]
        const durationField = allFieldValues[`${cardTypeId}:duration`]

        if (!overtimeDateField?.value) {
          missingFields.push('加班日期')
        }
        if (!durationField?.value || durationField.value <= 0) {
          missingFields.push('加班时长')
        }

        shouldValidate = missingFields.length === 0
      } else if (isMakeupApplication) {
        // 补卡申请：需要补卡日期、补卡时间
        const makeupDateField = allFieldValues[`${cardTypeId}:makeup-date`]
        const makeupTimeField = allFieldValues[`${cardTypeId}:makeup-time`]

        if (!makeupDateField?.value) {
          missingFields.push('补卡日期')
        }
        if (!makeupTimeField?.value) {
          missingFields.push('补卡时间')
        }

        shouldValidate = missingFields.length === 0
      }

      // 只有当所有必填字段都有值时才验证
      if (shouldValidate) {
        console.log('[CardDetailView] 所有必填字段已填写，开始验证')

        // 提取申请人ID
        const applicantLinkFieldId = `${cardTypeId}:link:applicant:SOURCE`
        const applicantCards = card.value.linkedCards?.[applicantLinkFieldId]
        const applicantId = applicantCards?.[0]?.id

        console.log('[CardDetailView] 申请人信息', { applicantLinkFieldId, applicantCards, applicantId })

        if (!applicantId) {
          showInternalMessage('error', '无法获取申请人信息')
          return
        }

        console.log('[CardDetailView] 调用验证接口', {
          orgId: card.value.orgId,
          applicantId,
          cardTypeId,
          fieldValuesCount: Object.keys(allFieldValues).length
        })

        // 调用验证接口
        const validationResult = await attendanceApi.validateApplication({
          orgId: card.value.orgId,
          applicantId,
          cardTypeId,
          fieldValues: allFieldValues,
        })

        console.log('[CardDetailView] 验证结果', validationResult)

        // 如果验证失败，显示错误信息并返回
        if (!validationResult.valid) {
          showInternalMessage('error', validationResult.message || '验证失败')
          return
        }

        console.log('[CardDetailView] 验证通过，继续保存')

        // 如果自动计算了请假时长，需要一起保存
        if (autoCalculatedDuration) {
          if (!updateRequest.fieldValues) {
            updateRequest.fieldValues = {}
          }
          updateRequest.fieldValues[autoCalculatedDuration.fieldId] = autoCalculatedDuration
          console.log('[CardDetailView] 将自动计算的请假时长加入保存请求', autoCalculatedDuration.value)
        }
      } else {
        console.log('[CardDetailView] 跳过验证，缺少必填字段:', missingFields)
      }
    }

    await cardApi.update(updateRequest)

    // 更新本地数据
    if (card.value) {
      const updatedFields = { ...card.value.fieldValues }

      // 更新当前编辑的字段
      if (newFieldValue) {
        updatedFields[fieldId] = newFieldValue
      }

      // 如果有自动计算的字段（如请假时长），也要更新
      if (updateRequest.fieldValues) {
        for (const [fId, fValue] of Object.entries(updateRequest.fieldValues)) {
          if (fId !== fieldId) { // 避免重复更新
            updatedFields[fId] = fValue
          }
        }
      }

      card.value = {
        ...card.value,
        fieldValues: updatedFields,
      }
    }

    showInternalMessage('success', '保存成功')
    cancelEditField()
  } catch (error: any) {
    console.error('Failed to save field:', error)
    showInternalMessage('error', error.message || '保存失败')
  } finally {
    fieldSaving.value = false
  }
}

/** 构建 FieldValue */
function buildFieldValue(fieldId: string, fieldType: string): FieldValue | null {
  switch (fieldType) {
    case 'NUMBER':
      return {
        type: 'NUMBER',
        fieldId,
        value: editingFieldValue.value ?? null,
        readable: true,
      } as FieldValue
    case 'DATE':
      return {
        type: 'DATE',
        fieldId,
        value: editingFieldValue.value ?? null,
        readable: true,
      } as FieldValue
    case 'ENUM': {
      const rawValue = editingFieldValue.value
      const ids = Array.isArray(rawValue) ? rawValue : (rawValue ? [rawValue] : [])
      return {
        type: 'ENUM',
        fieldId,
        value: ids.length > 0 ? ids : null,
        readable: true,
      } as FieldValue
    }
    case 'STRUCTURE':
      return editingFieldValue.value
    default:
      return {
        type: 'TEXT',
        fieldId,
        value: typeof editingFieldValue.value === 'string' 
          ? editingFieldValue.value.trim() || null 
          : editingFieldValue.value,
        readable: true,
      } as FieldValue
  }
}

/** 检查值是否变化 */
function isFieldValueChanged(_fieldId: string, fieldType: string, originalValue: unknown): boolean {
  switch (fieldType) {
    case 'NUMBER':
      return editingFieldValue.value !== originalValue
    case 'DATE':
      return editingFieldValue.value !== originalValue
    case 'ENUM': {
      const originalIds = Array.isArray(originalValue) ? [...originalValue].sort().join(',') : ''
      const currentIds = Array.isArray(editingFieldValue.value) 
        ? [...editingFieldValue.value].sort().join(',') 
        : ''
      return currentIds !== originalIds
    }
    case 'LINK': {
      const originalIds = Array.isArray(originalValue) ? [...originalValue].sort().join(',') : ''
      const currentIds = editingFieldValue.value.map((c: LinkedCard) => c.cardId).sort().join(',')
      return currentIds !== originalIds
    }
    case 'STRUCTURE': {
      const originalJson = originalValue ? JSON.stringify(originalValue) : ''
      const currentJson = editingFieldValue.value?.value 
        ? JSON.stringify(editingFieldValue.value.value) 
        : ''
      return currentJson !== originalJson
    }
    default:
      const currentText = typeof editingFieldValue.value === 'string' 
        ? editingFieldValue.value.trim() 
        : ''
      return currentText !== (originalValue || '')
  }
}
</script>

<template>
  <div class="card-detail-view">
    <!-- 卡片标签栏 -->
    <div v-if="cardTabsStore.tabs.length > 1" class="card-tabs-bar">
      <div class="tabs-scroll-container">
        <div
          v-for="tab in cardTabsStore.tabs"
          :key="tab.cardId"
          class="card-tab"
          :class="{ 'active': tab.cardId === props.cardId }"
          @click="handleTabClick(tab.cardId)"
        >
          <span class="tab-title">{{ tab.title || '加载中...' }}</span>
          <span class="tab-close" @click.stop="handleTabClose(tab.cardId)">×</span>
        </div>
      </div>
    </div>

    <!-- 内部消息提示 -->
    <transition name="message-fade">
      <div v-if="internalMessage" :class="['internal-message', `internal-message-${internalMessage.type}`]">
        <icon-check-circle-fill v-if="internalMessage.type === 'success'" />
        <icon-close-circle-fill v-else />
        <span>{{ internalMessage.text }}</span>
      </div>
    </transition>

    <a-spin :loading="loading" class="detail-spinner">
      <!-- 调试信息 -->
      <div v-if="card && template" style="display: none;">
        DEBUG: template.id={{ template.id }}, tabs.length={{ template.tabs?.length }}
      </div>
      <template v-if="card && template">
        <!-- 头部区域 -->
        <header class="detail-header">
          <!-- 元信息行：图标 + 卡片类型 + 编号 + 价值流状态 + 分享按钮 -->
          <div class="card-meta-row">
            <span v-if="cardTypeInfo" class="card-type-name">{{ cardTypeInfo.name }}</span>
            <a-tooltip v-if="displayCode" content="点击复制编号和标题" position="bottom">
              <span class="card-code clickable" @click="handleCopyCodeAndTitle">{{ displayCode }}</span>
            </a-tooltip>
            <!-- 价值流状态：编辑态 -->
            <div v-if="isEditingStatus" class="status-select-wrapper">
              <a-select
                v-model="selectedStatusId"
                size="mini"
                class="status-select"
                :loading="statusOptionsLoading"
                :default-popup-visible="true"
                :default-active-first-option="false"
                :trigger-props="{ autoFitPopupMinWidth: true }"
                @change="handleStatusChange"
                @popup-visible-change="handleStatusPopupVisibleChange"
              >
                <a-option
                  v-for="opt in allStatusOptions"
                  :key="opt.id"
                  :value="opt.id"
                  :label="opt.label"
                >
                  <span class="status-tag" :style="{ backgroundColor: opt.color }">
                    {{ opt.label }}
                  </span>
                </a-option>
                <template #label="{ data }">
                  <span
                    class="status-tag"
                    :style="{ backgroundColor: allStatusOptions.find(o => o.id === data.value)?.color }"
                  >
                    {{ data.label }}
                  </span>
                </template>
              </a-select>
            </div>
            <!-- 价值流状态：显示态 -->
            <span v-else-if="statusEnumOptions.length > 0" class="status-display" @click="startEditStatus">
              <FieldEnumDisplay 
                :options="statusEnumOptions" 
              />
            </span>
            <!-- 丢弃/归档状态标签 -->
            <span 
              v-if="cardStyleLabel" 
              class="card-style-tag"
              :style="{ backgroundColor: cardStyleColor, color: '#fff' }"
            >
              {{ cardStyleLabel }}
            </span>
            <div class="meta-spacer"></div>
            <!-- 卡片动作按钮 -->
            <CardActionButtons v-if="card" :card="card" @refresh="fetchCardDetail" />
            <!-- 报工按钮 -->
            <a-tooltip v-if="showWorklogButton" content="填报工时" position="bottom">
              <a-button type="text" size="small" class="worklog-btn" @click="openWorklogModal">
                <template #icon><IconClockCircle /></template>
                报工
              </a-button>
            </a-tooltip>
            <a-tooltip content="复制链接" position="bottom">
              <a-button type="text" size="small" class="share-btn" @click="handleShare">
                <template #icon><IconShareInternal /></template>
              </a-button>
            </a-tooltip>
          </div>
          <!-- 标题行 -->
          <div class="title-row">
            <!-- 编辑态：只编辑原始标题部分 -->
            <textarea
              v-if="isEditingTitle"
              ref="titleInputRef"
              v-model="editingTitle"
              class="card-title-input"
              rows="1"
              @blur="saveTitle"
              @keydown="handleTitleKeydown"
              @input="autoResizeTextarea"
            />
            <!-- 显示态：拼接标题区分前后缀和原始标题样式 -->
            <span v-else class="card-title editable" @click="startEditTitle">
              <template v-if="isJoint && jointArea === 'PREFIX'">
                <span class="joint-parts">{{ jointPartsText }}</span>{{ rawTitleValue }}
              </template>
              <template v-else-if="isJoint && jointArea === 'SUFFIX'">
                {{ rawTitleValue }}<span class="joint-parts">{{ jointPartsText }}</span>
              </template>
              <template v-else>
                {{ displayTitle }}
              </template>
            </span>
          </div>
        </header>

        <!-- 标签页导航 -->
        <div class="detail-tabs">
          <a-tabs v-model:active-key="activeTabId" type="line" size="small">
            <a-tab-pane v-for="tab in template.tabs" :key="tab.tabId" :title="tab.name" />
          </a-tabs>
        </div>

        <!-- 标签页内容 -->
        <div class="detail-content">
          <template v-if="activeTab">
            <!-- 基础信息 / 自定义标签页 -->
            <template v-if="isBasicInfoTab(activeTab) || activeTab.tabType === 'CUSTOM'">
              <div class="sections-container">
                <div
                  v-for="section in activeTab.sections || []"
                  :key="section.sectionId"
                  class="section-card"
                >
                  <div class="section-header" :class="{ 'is-collapsible': section.collapsible !== false }">
                    <span class="section-title">{{ section.name }}</span>
                    <span 
                      v-if="section.collapsible !== false" 
                      class="section-icon"
                      @click.stop="toggleSection(section)"
                    >
                      <IconRight v-if="section.collapsed" />
                      <IconDown v-else />
                    </span>
                  </div>
                  <div v-show="!section.collapsed" class="section-content" :style="{ gap: fieldRowGap }">
                    <div v-for="(row, rowIndex) in getFieldRows(section)" :key="rowIndex" class="field-row">
                      <div
                        v-for="item in row"
                        :key="item.fieldConfigId"
                        class="field-item"
                        :style="{ width: `${item.widthPercent}%` }"
                      >
                        <!-- 描述字段特殊渲染 -->
                        <template v-if="isDescriptionField(item.fieldConfigId)">
                          <div class="description-header">
                            <span class="field-label description-label">{{ item.customLabel || t('admin.fieldType.DESCRIPTION') }}</span>
                            <a-dropdown v-if="card?.description?.value" trigger="click" position="bl">
                              <a-button type="text" size="mini" class="export-btn">
                                <template #icon><IconExport /></template>
                                {{ t('common.action.export') }}
                              </a-button>
                              <template #content>
                                <a-doption @click="exportDescriptionAsPdf">{{ t('common.action.exportPdf') }}</a-doption>
                                <a-doption @click="exportDescriptionAsMarkdown">{{ t('common.action.exportMarkdown') }}</a-doption>
                              </template>
                            </a-dropdown>
                          </div>
                          <div class="field-value description-value">
                            <!-- 编辑状态 -->
                            <template v-if="isEditingField(item.fieldConfigId)">
                              <FieldMarkdownEditor
                                ref="descriptionEditorRef"
                                v-model="editingFieldValue"
                                :height="item.height || 300"
                                :disabled="fieldSaving"
                                placeholder="请输入描述内容..."
                                @save="saveDescriptionField"
                                @cancel="cancelEditField"
                              />
                            </template>
                            <!-- 显示状态 -->
                            <template v-else>
                              <div
                                class="field-display-wrapper description-display-wrapper"
                                @click="startEditDescription"
                              >
                                <FieldMarkdownDisplay
                                  v-if="card?.description?.value"
                                  :value="card.description.value"
                                  :height="item.height || 120"
                                />
                                <span v-else class="no-value">点击添加描述</span>
                              </div>
                            </template>
                          </div>
                        </template>
                        <!-- 关联字段渲染 -->
                        <template v-else-if="isValidLinkFieldId(item.fieldConfigId)">
                          <div class="field-label">
                            {{ item.customLabel || getFieldLabel(item.fieldConfigId) }}
                          </div>
                          <div 
                            class="field-value link-field-value"
                            :class="{ 'field-editable': isFieldEditable(item.fieldConfigId) }"
                          >
                            <!-- 编辑状态 -->
                            <template v-if="isEditingField(item.fieldConfigId)">
                                  <FieldEditor
                                    v-model="editingFieldValue"
                                    field-type="LINK"
                                    :field-id="item.fieldConfigId"
                                    :render-config="getFieldRenderConfig(item.fieldConfigId)"
                                    :disabled="fieldSaving"
                                    :auto-focus="true"
                                    @save="saveEditField"
                                    @cancel="cancelEditField"
                                  />
                            </template>
                            <!-- 显示状态 -->
                            <template v-else>
                              <div 
                                class="field-display-wrapper"
                                @click="isFieldEditable(item.fieldConfigId) && startEditField(item.fieldConfigId)"
                              >
                                <FieldDisplay
                                  :field-id="item.fieldConfigId"
                                  :field-value="getFieldValue(item.fieldConfigId)"
                                  :render-config="getFieldRenderConfig(item.fieldConfigId)"
                                  :card="card"
                                />
                              </div>
                            </template>
                          </div>
                        </template>
                        <!-- 普通字段渲染 -->
                        <template v-else>
                          <div class="field-label">
                            <!-- 必填标识 -->
                            <a-tooltip
                              v-if="getFieldControl(item.fieldConfigId)?.requiredLevel"
                              :content="getFieldControl(item.fieldConfigId)?.requiredReasonText || '必填字段'"
                              position="top"
                            >
                              <span
                                class="required-mark"
                                :class="{
                                  'required-hint': getFieldControl(item.fieldConfigId)?.requiredLevel === 'HINT',
                                  'required-strict': getFieldControl(item.fieldConfigId)?.requiredLevel === 'STRICT'
                                }"
                              >*</span>
                            </a-tooltip>
                            {{ item.customLabel || getFieldLabel(item.fieldConfigId) }}
                            <!-- 只读标识 -->
                            <a-tooltip
                              v-if="!getFieldControl(item.fieldConfigId)?.editable && getFieldControl(item.fieldConfigId)?.readOnlyReasonText"
                              :content="getFieldControl(item.fieldConfigId)?.readOnlyReasonText"
                              position="top"
                            >
                              <IconLock class="readonly-icon" />
                            </a-tooltip>
                          </div>
                          <div 
                            class="field-value"
                            :class="{ 'field-editable': isFieldEditable(item.fieldConfigId) }"
                          >
                            <!-- 编辑状态 -->
                            <template v-if="isEditingField(item.fieldConfigId)">
                              <FieldEditor
                                v-model="editingFieldValue"
                                :field-type="editingField?.fieldType || 'TEXT'"
                                :field-id="item.fieldConfigId"
                                :render-config="getFieldRenderConfig(item.fieldConfigId)"
                                :disabled="fieldSaving"
                                :auto-focus="true"
                                @save="saveEditField"
                                @cancel="cancelEditField"
                              />
                            </template>
                            <!-- 显示状态 -->
                            <template v-else>
                              <div 
                                class="field-display-wrapper"
                                @click="isFieldEditable(item.fieldConfigId) && startEditField(item.fieldConfigId)"
                              >
                                <FieldDisplay
                                  :field-id="item.fieldConfigId"
                                  :field-value="getFieldValue(item.fieldConfigId)"
                                  :render-config="getFieldRenderConfig(item.fieldConfigId)"
                                  :card="card"
                                />
                              </div>
                            </template>
                          </div>
                        </template>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <!-- 评论标签页 -->
            <template v-else-if="isCommentTab(activeTab)">
              <CommentPanel
                v-if="card?.id"
                :card-id="cardId"
                :card-type-id="card.typeId"
              />
            </template>

            <!-- 操作记录标签页 -->
            <template v-else-if="isActivityLogTab(activeTab)">
              <ActivityLogPanel
                v-if="card?.typeId"
                :card-id="cardId"
                :card-type-id="card.typeId"
              />
            </template>
          </template>
        </div>
      </template>
      <a-empty v-else-if="!loading" description="卡片不存在或已被删除" />
    </a-spin>

    <!-- 报工弹窗 -->
    <WorklogModal
      v-if="card"
      v-model:visible="worklogModalVisible"
      :card-id="card.id"
      :card-title="cardTitle"
      @success="handleWorklogSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
.card-detail-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-1);
  overflow: hidden;
  position: relative;
}

// 卡片标签栏
.card-tabs-bar {
  flex-shrink: 0;
  background: var(--color-bg-2);
  border-bottom: 1px solid var(--color-border);
  padding: 0 8px;
}

.tabs-scroll-container {
  display: flex;
  gap: 2px;
  overflow-x: auto;
  padding: 6px 0;

  &::-webkit-scrollbar {
    height: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--color-fill-3);
    border-radius: 2px;
  }
}

.card-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--color-text-2);
  background: transparent;
  cursor: pointer;
  white-space: nowrap;
  max-width: 150px;
  transition: all 0.2s;

  &:hover {
    background: var(--color-fill-2);
    
    .tab-close {
      opacity: 1;
    }
  }

  &.active {
    background: var(--color-bg-1);
    color: var(--color-text-1);
    font-weight: 500;
    box-shadow: 0 1px 2px rgba(0,0,0,0.05);
    
    .tab-close {
      opacity: 0.7;
    }
  }
}

.tab-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tab-close {
  opacity: 0;
  font-size: 14px;
  line-height: 1;
  color: var(--color-text-3);
  transition: all 0.2s;
  flex-shrink: 0;

  &:hover {
    color: var(--color-text-1);
  }
}

// 内部消息提示
.internal-message {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 14px;
  color: var(--color-text-1);
  background: var(--color-bg-1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

  :deep(svg) {
    font-size: 18px;
  }

  &-success :deep(svg) {
    color: rgb(var(--green-6));
  }

  &-error :deep(svg) {
    color: rgb(var(--red-6));
  }
}

// 消息淡入淡出动画
.message-fade-enter-active,
.message-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.message-fade-enter-from,
.message-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}

.detail-spinner {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.meta-spacer {
  flex: 1;
}

.share-btn {
  color: var(--color-text-3);

  &:hover {
    color: rgb(var(--primary-6));
  }
}

.worklog-btn {
  color: var(--color-text-3);

  &:hover {
    color: rgb(var(--primary-6));
  }
}

// ==================== 头部样式 ====================
.detail-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-2);
  flex-shrink: 0;
}

.card-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.title-row {
  display: flex;
  align-items: flex-start;
  min-width: 0;
}

.meta-type-icon {
  flex-shrink: 0;
}

.card-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-1);
  max-width: 700px;
  word-break: break-all;

  &.editable {
    cursor: text;
    padding: 2px 4px;
    margin: -2px -4px;
    border-radius: 4px;
    transition: background-color 0.2s;

    &:hover {
      background-color: var(--color-fill-2);
    }
  }

  // 拼接部分样式：浅色显示，添加间距
  .joint-parts {
    color: var(--color-text-3);
    font-weight: 400;
    margin: 0 4px;
  }
}

.card-title-input {
  font-size: 18px;
  font-weight: 500;
  line-height: 1.3;
  color: var(--color-text-1);
  width: 700px;
  padding: 4px 4px;
  margin: -4px -4px;
  border: 1px solid rgb(var(--primary-6));
  border-radius: 4px;
  outline: none;
  background: var(--color-bg-1);
  resize: none;
  overflow: hidden;
  font-family: inherit;
  display: block;
  box-sizing: border-box;
  field-sizing: content;

  &:focus {
    box-shadow: 0 0 0 2px rgb(var(--primary-6) / 20%);
  }
}

.card-code {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-3);
  flex-shrink: 0;

  &.clickable {
    cursor: pointer;

    &:hover {
      color: rgb(var(--primary-6));
    }
  }
}

.card-type-name {
  font-size: 12px;
  color: var(--color-text-3);
}

// 价值流状态显示（可点击编辑）
.status-display {
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;

  &:hover {
    background-color: var(--color-fill-2);
  }
}

// 卡片丢弃/归档状态标签
.card-style-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  border-radius: 4px;
  margin-left: 8px;
  flex-shrink: 0;
}

// 价值流状态选择器包装
.status-select-wrapper {
  display: inline-flex;
  flex: none;
}

// 价值流状态下拉选择器
.status-select {
  width: auto !important;

  :deep(.arco-select-view) {
    width: auto !important;
    min-width: unset !important;
  }

  :deep(.arco-select-view-single) {
    width: auto !important;
    padding-right: 28px;
  }

  :deep(.arco-select-view-value) {
    padding: 0;
    flex: none;
  }
}

// 状态标签样式
.status-tag {
  display: block;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  background-color: var(--color-fill-2);
  color: var(--color-text-1) !important;
  white-space: nowrap;
  text-align: center;
}

// ==================== 标签页样式 ====================
.detail-tabs {
  padding: 0 0 0 20px;
  border-bottom: 1px solid var(--color-border-2);
  flex-shrink: 0;
  overflow-x: auto;

  :deep(.arco-tabs-nav) {
    margin-bottom: -1px;

    &::before {
      display: none;
    }
  }

  :deep(.arco-tabs-tab) {
    padding: 8px 0;
    margin-right: 18px;
    white-space: nowrap;

    &:first-child {
      margin-left: 0;
      padding-left: 0;
    }

    &.arco-tabs-tab-active {
      font-weight: 600;
      color: var(--color-text-1);
    }
  }

  :deep(.arco-tabs-nav-ink) {
    height: 3px;
    background-color: rgb(var(--primary-6));
  }

  :deep(.arco-tabs-nav-tab-list) {
    padding-left: 0;
  }

  :deep(.arco-tabs-content) {
    display: none;
  }
}

// ==================== 内容样式 ====================
.detail-content {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 16px 20px 16px 20px;
}

.sections-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-card {
  background: transparent;
  border-radius: 0;
  padding: 0;
}

.section-header {
  margin-bottom: 12px;
  margin-left: -8px;
  margin-right: -8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  background: var(--color-fill-1);
  padding: 6px 8px;
  border-radius: 6px;

  &.is-collapsible {
    // cursor: pointer; // Not clickable on whole header anymore
    user-select: none;

    &:hover {
      .section-title {
        // color: rgb(var(--primary-6)); // No hover effect on title since it's not clickable
      }
      .section-icon {
        color: rgb(var(--primary-6));
      }
    }
  }
}

.section-icon {
  display: flex;
  align-items: center;
  color: var(--color-text-3);
  font-size: 14px;
  transition: all 0.2s;
  cursor: pointer; // Icon is now the clickable element

  &:hover {
    color: rgb(var(--primary-6));
    background-color: var(--color-fill-2); // Add a subtle background hover effect
    border-radius: 4px;
  }
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
  transition: color 0.2s;
}

.section-content {
  display: flex;
  flex-direction: column;
}

.field-row {
  display: flex;
  gap: 16px;
}

.field-item {
  flex-shrink: 0;
  min-width: 0;
}

.field-label {
  position: relative;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-1);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
}

.description-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
  margin-bottom: 12px;
}

.field-value {
  font-size: 14px;
  color: var(--color-text-1);
  word-break: break-word;
  width: 98%;
}

.description-value {
  width: 100%;

  .description-text {
    white-space: pre-wrap;
    line-height: 1.6;
  }
}

.description-display-wrapper {
  width: 100%;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;

  &:hover {
    background-color: var(--color-fill-1);
  }
}

.no-value {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60px;
  color: var(--color-text-4);
  font-style: italic;
  border: 1px dashed var(--color-border-2);
  border-radius: 6px;
  background: var(--color-fill-1);
  transition: all 0.2s;

  &:hover {
    border-color: rgb(var(--primary-6));
    color: var(--color-text-3);
  }
}

// 关联字段值
.link-field-value {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  width: 98%;

  .field-display-wrapper {
    width: 100%;
    min-height: 28px;
  }
}

.linked-card-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  background-color: var(--color-fill-2);
  border-radius: 4px;
  font-size: 13px;
  color: var(--color-text-1);
  cursor: default;
  transition: background-color 0.2s;

  &:hover {
    background-color: var(--color-fill-3);
  }
}

.required-mark {
  position: absolute;
  left: -10px;
  cursor: help;
}

.required-hint {
  color: rgb(var(--warning-6));
}

.required-strict {
  color: rgb(var(--danger-6));
}

.readonly-icon {
  margin-left: 4px;
  font-size: 12px;
  color: var(--color-text-4);
  cursor: help;
}

// 可编辑字段样式
.field-editable {
  .field-display-wrapper {
    cursor: pointer;
    padding: 2px 6px;
    margin-left: -6px;
    border-radius: 4px;
    min-height: 24px;
    transition: background-color 0.2s;

    &:hover {
      background-color: var(--color-fill-2);
    }
  }
}

.edit-trigger {
  color: var(--color-text-3);
  cursor: pointer;

  &:hover {
    color: rgb(var(--primary-6));
  }
}

// 描述字段头部（标签 + 导出按钮）
.description-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  margin-bottom: 12px;

  .description-label {
    margin-bottom: 0;
  }
}

.export-btn {
  display: inline-flex;
  align-items: center;
  color: var(--color-text-3);
  font-size: 12px;
  padding: 0 8px;
  height: 24px;

  &:hover {
    color: rgb(var(--primary-6));
    background: var(--color-fill-2);
  }

  :deep(.arco-btn-icon) {
    display: inline-flex;
    align-items: center;
    font-size: 14px;
    margin-right: 2px;
  }
}
</style>
