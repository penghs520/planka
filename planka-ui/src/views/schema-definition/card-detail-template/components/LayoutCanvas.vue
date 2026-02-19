<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { IconPlus, IconShareInternal, IconDelete, IconDown } from '@arco-design/web-vue/es/icon'
import FieldTypeIcon from '@/components/field/FieldTypeIcon.vue'
import TemplateSection from './TemplateSection.vue'
import { useAutoScroll } from '../composables/useAutoScroll'
import type {
  CardDetailTemplateDefinition,
  TabConfig,
  FieldItemConfig,
  SelectedItem,
} from '@/types/card-detail-template'
import {
  createEmptyCustomTab,
  createEmptySection,
  createEmptyFieldItem,
  SystemTabTypeConfig,
  FieldRowSpacingConfig,
} from '@/types/card-detail-template'

const props = defineProps<{
  modelValue: CardDetailTemplateDefinition
  selectedItem: SelectedItem | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: CardDetailTemplateDefinition): void
  (e: 'select', item: SelectedItem | null): void
  (e: 'change'): void
}>()

// 注入字段列表 (不再使用)
// import { inject } from 'vue'
// const fields = inject<{ value: FieldConfig[] }>('fields', { value: [] })

// 当前激活的 Tab
const activeTabId = ref<string>('')

// 宽度拖拽状态
const resizingField = ref<{ sectionId: string; fieldConfigId: string; startX: number; startWidth: number } | null>(null)
const sectionContentRefs = ref<Map<string, HTMLElement>>(new Map())
const layoutCanvasRef = ref<HTMLElement | null>(null)

// 自动滚动 (使用 Composable)
const { stopAutoScroll } = useAutoScroll(layoutCanvasRef)

// 允许的宽度值
const allowedWidths = [25, 33, 50, 66, 75, 100]

// 字段拖拽排序状态
const draggingFieldItem = ref<{ sectionId: string; fieldConfigId: string } | null>(null)
const dropTargetField = ref<{ sectionId: string; fieldConfigId: string; position: 'before' | 'after' } | null>(null)
// 拖放到字段下方（新行）
const dropBelowFieldId = ref<string | null>(null)

// 区域拖拽排序状态
const draggingSectionId = ref<string | null>(null)
const dropTargetSection = ref<{ sectionId: string; position: 'before' | 'after' } | null>(null)

// Tab 拖拽排序状态
const draggingTabId = ref<string | null>(null)
const dropTargetTab = ref<{ tabId: string; position: 'before' | 'after' } | null>(null)

// 初始化激活 Tab
const firstTab = props.modelValue.tabs[0]
if (props.modelValue.tabs.length > 0 && !activeTabId.value && firstTab) {
  activeTabId.value = firstTab.tabId
}

// 当前 Tab
const currentTab = computed(() => {
  return props.modelValue.tabs.find((tab) => tab.tabId === activeTabId.value)
})

// 当前 Tab 的区域列表
const currentSections = computed(() => {
  return currentTab.value?.sections || []
})

// Tab 列表
const tabs = computed(() => props.modelValue.tabs)

// 当前 Tab 的字段行间距
const currentFieldRowGap = computed(() => {
  const spacing = currentTab.value?.fieldRowSpacing || 'NORMAL'
  return FieldRowSpacingConfig[spacing].rowGap
})

// 选中 Tab
function selectTab(tab: TabConfig) {
  activeTabId.value = tab.tabId
  emit('select', { type: 'tab', id: tab.tabId })
}

// 是否选中
function isTabSelected(tab: TabConfig): boolean {
  return props.selectedItem?.type === 'tab' && props.selectedItem.id === tab.tabId
}

// 添加 Tab
function handleAddTab() {
  const newTab = createEmptyCustomTab()
  props.modelValue.tabs.push(newTab)
  activeTabId.value = newTab.tabId
  emit('change')
}

// 删除 Tab
function handleDeleteTab(tab: TabConfig) {
  const index = props.modelValue.tabs.findIndex((t) => t.tabId === tab.tabId)
  if (index === -1) return

  props.modelValue.tabs.splice(index, 1)

  // 如果删除的是当前激活的 Tab，切换到第一个 Tab
  if (activeTabId.value === tab.tabId) {
    const firstTab = tabs.value[0]
    activeTabId.value = firstTab?.tabId || ''
  }

  // 清除选中状态
  emit('select', null)
  emit('change')
}

// 添加区域
function handleAddSection() {
  if (!currentTab.value) return
  if (!currentTab.value.sections) {
    currentTab.value.sections = []
  }
  const newSection = createEmptySection()
  currentTab.value.sections.push(newSection)
  emit('change')
}

// 删除区域
function handleDeleteSection(sectionId: string) {
  if (!currentTab.value?.sections) return
  const index = currentTab.value.sections.findIndex((s) => s.sectionId === sectionId)
  if (index > -1) {
    currentTab.value.sections.splice(index, 1)
    emit('change')
  }
}

// 更新区域名称
function handleUpdateSectionName(sectionId: string, name: string) {
  if (!currentTab.value?.sections) return
  const section = currentTab.value.sections.find((s) => s.sectionId === sectionId)
  if (section) {
    section.name = name
    emit('change')
  }
}

// 删除字段
function handleDeleteField(sectionId: string, fieldConfigId: string, event?: Event) {
  event?.stopPropagation()
  const section = currentSections.value.find((s) => s.sectionId === sectionId)
  if (!section) return
  const index = section.fieldItems.findIndex((f) => f.fieldConfigId === fieldConfigId)
  if (index > -1) {
    section.fieldItems.splice(index, 1)
    emit('change')
  }
}

// 拖拽状态
const dragOverSectionId = ref<string | null>(null)

// 处理拖拽进入区域（仅用于从字段库拖入新字段）
function handleDragEnterSection(sectionId: string, event: DragEvent) {
  event.preventDefault()
  // 如果是字段排序拖拽，不显示区域高亮
  if (draggingFieldItem.value) return
  // 如果是区域排序拖拽，也不显示区域高亮（使用专门的 dropTargetSection 指示器）
  if (draggingSectionId.value) return
  dragOverSectionId.value = sectionId
}

// 处理拖拽离开区域
function handleDragLeaveSection() {
  dragOverSectionId.value = null
}

// 处理放置到区域
// 执行区域排序逻辑
function executeSectionReorder(targetSectionId: string, position: 'before' | 'after') {
  const sections = currentTab.value?.sections
  if (!sections) return

  const sourceIndex = sections.findIndex(s => s.sectionId === draggingSectionId.value)
  const targetIndex = sections.findIndex(s => s.sectionId === targetSectionId)

  if (sourceIndex > -1 && targetIndex > -1) {
    const [movedSection] = sections.splice(sourceIndex, 1)
    let insertIndex = targetIndex
    if (sourceIndex < insertIndex) {
      insertIndex--
    }
    
    if (position === 'after') {
      insertIndex++
    }
    
    if (movedSection) {
      sections.splice(insertIndex, 0, movedSection)
      emit('change')
    }
  }
}

// 处理放置到区域
function handleDropToSection(sectionId: string, event: DragEvent) {
  event.preventDefault()
  const data = event.dataTransfer?.getData('application/json')
  if (!data) return

  try {
    const payload = JSON.parse(data)
    
    // 处理区域排序
    if (payload.type === 'section-reorder') {
       event.stopPropagation()
       if (draggingSectionId.value && dropTargetSection.value) {
          // 使用当前保存的 dropTargetSection 状态，因为拖拽到 gap 可能导致 sectionId 参数不准确（如果是在 gap 处触发）
          // 但其实 handleDropToSection 是绑定在 section 上的。
          // 不过统一使用 dropTargetSection 更安全。
          const { sectionId: targetId, position } = dropTargetSection.value
          executeSectionReorder(targetId, position)
       }
       return
    }

    const { type, field } = payload
    if (type === 'field' && field) {
      const section = currentSections.value.find((s) => s.sectionId === sectionId)
      if (!section) return

      // 检查是否已存在（使用 fieldId）
      if (section.fieldItems.some((f) => f.fieldConfigId === field.fieldId)) {
        return
      }

      const newItem = createEmptyFieldItem(field.fieldId)
      section.fieldItems.push(newItem)
      emit('change')
    }
  } catch (e) {
    console.error('Drop error:', e)
  }

  dragOverSectionId.value = null
  dropTargetSection.value = null
}

// 区域拖拽开始
function handleSectionDragStart(sectionId: string, event: DragEvent) {
  if (draggingFieldItem.value || resizingField.value || draggingTabId.value) {
    event.preventDefault()
    return
  }

  event.stopPropagation()
  draggingSectionId.value = sectionId
  
  // 清除选中状态
  emit('select', null)

  if (event.dataTransfer) {
     event.dataTransfer.setData('application/json', JSON.stringify({
      type: 'section-reorder',
      sectionId
    }))
    event.dataTransfer.effectAllowed = 'move'
  }
}

// 区域拖拽经过
function handleSectionDragOver(sectionId: string, event: DragEvent) {
  event.preventDefault()
  
  if (draggingSectionId.value) {
     event.stopPropagation()
     
     if (draggingSectionId.value === sectionId) {
       dropTargetSection.value = null
       return
     }
     
     const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
     const midY = rect.top + rect.height / 2
     const position = event.clientY < midY ? 'before' : 'after'
     
     dropTargetSection.value = { sectionId, position }
     if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'move'
     }
  } else {
    // 默认行为（如拖入字段）
    if (event.dataTransfer) {
       event.dataTransfer.dropEffect = 'copy'
    }
  }
}


// 点击画布空白处取消选中
function handleCanvasClick() {
  emit('select', null)
}

// 设置 section 内容区域的引用
function setSectionContentRef(sectionId: string, el: unknown) {
  // TemplateSection exposes sectionContentRef
  const component = el as { sectionContentRef: HTMLElement | null } | null
  if (component && component.sectionContentRef) {
    sectionContentRefs.value.set(sectionId, component.sectionContentRef)
  } else {
    // If not component instance or unmounting
    if (sectionContentRefs.value.has(sectionId)) {
        // Only delete if it was previously set, might need better logic if dynamic
        // For now, if el is null, delete it
         if(!el) sectionContentRefs.value.delete(sectionId)
    }
  }
}

// 开始调整宽度
function startResize(sectionId: string, fieldConfigId: string, event: MouseEvent) {
  event.stopPropagation()
  event.preventDefault()

  const section = currentSections.value.find((s) => s.sectionId === sectionId)
  if (!section) return

  const fieldItem = section.fieldItems.find((f) => f.fieldConfigId === fieldConfigId)
  if (!fieldItem) return

  resizingField.value = {
    sectionId,
    fieldConfigId,
    startX: event.clientX,
    startWidth: fieldItem.widthPercent,
  }

  document.addEventListener('mousemove', handleResizeMove)
  document.addEventListener('mouseup', handleResizeEnd)
}

// 处理调整宽度移动
function handleResizeMove(event: MouseEvent) {
  if (!resizingField.value) return

  const { sectionId, fieldConfigId, startX, startWidth } = resizingField.value
  const section = currentSections.value.find((s) => s.sectionId === sectionId)
  if (!section) return

  const fieldItem = section.fieldItems.find((f) => f.fieldConfigId === fieldConfigId)
  if (!fieldItem) return

  // 获取 section 内容区域宽度
  const sectionEl = sectionContentRefs.value.get(sectionId)
  if (!sectionEl) return

  const containerWidth = sectionEl.offsetWidth
  const deltaX = event.clientX - startX
  const deltaPercent = (deltaX / containerWidth) * 100
  const rawWidth = startWidth + deltaPercent

  // 吸附到最近的允许宽度值 (25%, 50%, 75%, 100%)
  let newWidth = allowedWidths.reduce((prev, curr) =>
    Math.abs(curr - rawWidth) < Math.abs(prev - rawWidth) ? curr : prev
  )

  fieldItem.widthPercent = newWidth
}

// 结束调整宽度
function handleResizeEnd() {
  if (resizingField.value) {
    resizingField.value = null
    emit('change')
  }
  document.removeEventListener('mousemove', handleResizeMove)
  document.removeEventListener('mouseup', handleResizeEnd)
}

// --- 自动滚动逻辑 (moved to composable) ---

// 全局拖拽结束处理 - 确保清除从字段库拖拽时的状态
function handleGlobalDragEnd() {
  dragOverSectionId.value = null
  draggingFieldItem.value = null
  dropTargetField.value = null
  dropBelowFieldId.value = null
  draggingSectionId.value = null
  dropTargetSection.value = null
  draggingTabId.value = null
  dropTargetTab.value = null
  stopAutoScroll()
}

// 处理容器级别的 drop (用于处理 gap 区域的放置)
function handleContainerDrop(event: DragEvent) {
  event.preventDefault()
  
  // 仅处理区域排序
  if (draggingSectionId.value && dropTargetSection.value) {
    const { sectionId, position } = dropTargetSection.value
    // 只有当 dropTargetSection 有效时才执行（意味着之前鼠标经过了有效的 section）
    executeSectionReorder(sectionId, position)
    dropTargetField.value = null
    dropBelowFieldId.value = null
    draggingSectionId.value = null
    dropTargetSection.value = null
    // 停止自动滚动
    stopAutoScroll()
  }
}

onMounted(() => {
  // 监听全局 dragend 事件，确保拖拽状态被清除
  document.addEventListener('dragend', handleGlobalDragEnd)
})

// 组件卸载时清理事件监听
onUnmounted(() => {
  document.removeEventListener('mousemove', handleResizeMove)
  document.removeEventListener('mouseup', handleResizeEnd)
  document.removeEventListener('dragend', handleGlobalDragEnd)
})

// 开始拖拽字段排序
function handleFieldDragStart(sectionId: string, fieldConfigId: string, event: DragEvent) {
  if (draggingTabId.value) {
    event.preventDefault()
    return
  }
  event.stopPropagation()
  // 取消当前选中状态
  emit('select', null)
  draggingFieldItem.value = { sectionId, fieldConfigId }
  if (event.dataTransfer) {
    event.dataTransfer.setData('application/json', JSON.stringify({
      type: 'field-reorder',
      sectionId,
      fieldConfigId,
    }))
    event.dataTransfer.effectAllowed = 'move'
  }
}

// 字段拖拽经过 - 支持字段排序和从字段库拖入新字段
function handleFieldDragOver(sectionId: string, fieldConfigId: string, event: DragEvent) {
  event.preventDefault()
  event.stopPropagation()

  // 如果正在拖拽区域，忽略字段的 drop 事件
  if (draggingSectionId.value) return

  // 如果是字段排序拖拽，忽略拖拽到自己
  if (draggingFieldItem.value && draggingFieldItem.value.fieldConfigId === fieldConfigId) {
    dropTargetField.value = null
    return
  }

  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const midX = rect.left + rect.width / 2
  const position = event.clientX < midX ? 'before' : 'after'

  dropTargetField.value = { sectionId, fieldConfigId, position }
  // 清除区域级别的高亮，因为我们有了更精确的目标
  dragOverSectionId.value = null
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = draggingFieldItem.value ? 'move' : 'copy'
  }
}

// 字段拖拽离开
function handleFieldDragLeave(event: DragEvent) {
  event.stopPropagation()
  // 只有离开整个字段区域时才清除
  const relatedTarget = event.relatedTarget as HTMLElement
  if (!relatedTarget || !(event.currentTarget as HTMLElement).contains(relatedTarget)) {
    dropTargetField.value = null
  }
}

// 字段放置 - 支持字段排序和从字段库拖入新字段
function handleFieldDrop(targetSectionId: string, targetFieldConfigId: string, event: DragEvent) {
  event.preventDefault()
  event.stopPropagation()

  if (!dropTargetField.value) return

  const { position } = dropTargetField.value
  const targetSection = currentSections.value.find((s) => s.sectionId === targetSectionId)
  if (!targetSection) return

  // 找到目标字段
  const targetFieldIndex = targetSection.fieldItems.findIndex((f) => f.fieldConfigId === targetFieldConfigId)
  if (targetFieldIndex === -1) return
  const targetField = targetSection.fieldItems[targetFieldIndex]
  if (!targetField) return

  // 找到目标位置
  let targetIndex = targetFieldIndex
  if (position === 'after') {
    targetIndex += 1
  }

  // 如果是在 startNewRow 字段前面插入，需要转移 startNewRow 属性
  const shouldTransferStartNewRow = position === 'before' && targetField.startNewRow

  if (draggingFieldItem.value) {
    // 区域内字段排序
    const { sectionId: sourceSectionId, fieldConfigId: sourceFieldConfigId } = draggingFieldItem.value
    const sourceSection = currentSections.value.find((s) => s.sectionId === sourceSectionId)
    if (!sourceSection) return

    const sourceIndex = sourceSection.fieldItems.findIndex((f) => f.fieldConfigId === sourceFieldConfigId)
    if (sourceIndex === -1) return

    const [movedItem] = sourceSection.fieldItems.splice(sourceIndex, 1)
    if (!movedItem) return

    // 如果是同一区域且源位置在目标之前，需要调整索引
    if (sourceSectionId === targetSectionId && sourceIndex < targetIndex) {
      targetIndex -= 1
    }

    // 处理 startNewRow 属性
    if (shouldTransferStartNewRow) {
      // 在 startNewRow 字段前面插入，转移属性
      movedItem.startNewRow = true
      targetField.startNewRow = false
    } else if (position === 'after') {
      // 移动到字段后面，清除 startNewRow 属性
      movedItem.startNewRow = false
    }

    targetSection.fieldItems.splice(targetIndex, 0, movedItem)
  } else {
    // 从字段库拖入新字段
    const data = event.dataTransfer?.getData('application/json')
    if (!data) return

    try {
      const { type, field } = JSON.parse(data)
      if (type === 'field' && field) {
        // 检查是否已存在
        if (targetSection.fieldItems.some((f) => f.fieldConfigId === field.fieldId)) {
          return
        }

        const newItem = createEmptyFieldItem(field.fieldId)

        // 处理 startNewRow 属性转移
        if (shouldTransferStartNewRow) {
          newItem.startNewRow = true
          targetField.startNewRow = false
        }

        targetSection.fieldItems.splice(targetIndex, 0, newItem)
      }
    } catch (e) {
      console.error('Drop error:', e)
      return
    }
  }

  emit('change')
  draggingFieldItem.value = null
  dropTargetField.value = null
  dragOverSectionId.value = null
}

// 字段拖拽结束
function handleFieldDragEnd() {
  draggingFieldItem.value = null
  dropTargetField.value = null
  dragOverSectionId.value = null
  dropBelowFieldId.value = null
}

// 拖到字段下方区域 - dragover
function handleDropBelowOver(fieldConfigId: string, event: DragEvent) {
  event.preventDefault()
  event.stopPropagation()
  // 如果正在拖拽区域，忽略字段下方的 drop 事件
  if (draggingSectionId.value) return
  if (!draggingFieldItem.value) return
  if (draggingFieldItem.value.fieldConfigId === fieldConfigId) return
  dropBelowFieldId.value = fieldConfigId
  dropTargetField.value = null
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

// 拖到字段下方区域 - dragleave
function handleDropBelowLeave() {
  dropBelowFieldId.value = null
}

// 找到字段所在行的最后一个字段索引（排除指定字段）
function findRowEndIndex(fieldItems: FieldItemConfig[], targetIndex: number, excludeFieldId?: string): number {
  // 边界检查
  if (targetIndex < 0 || targetIndex >= fieldItems.length) return targetIndex

  // 构建不包含被排除字段的虚拟数组用于计算
  const virtualItems = excludeFieldId
    ? fieldItems.filter(f => f.fieldConfigId !== excludeFieldId)
    : fieldItems

  // 重新计算目标字段在虚拟数组中的索引
  const targetItem = fieldItems[targetIndex]
  if (!targetItem) return targetIndex
  const targetFieldConfigId = targetItem.fieldConfigId
  const virtualTargetIndex = virtualItems.findIndex(f => f.fieldConfigId === targetFieldConfigId)
  if (virtualTargetIndex === -1) return targetIndex

  // 计算目标字段所在行的结束位置
  let accumulatedWidth = 0

  // 首先找到目标字段所在行的起始位置
  for (let i = 0; i <= virtualTargetIndex; i++) {
    const item = virtualItems[i]
    if (!item) continue
    const width = item.widthPercent
    // 如果字段标记为新行开始，或宽度超过100，开始新行
    if (item.startNewRow || accumulatedWidth + width > 100) {
      // 新行开始
      accumulatedWidth = width
    } else {
      accumulatedWidth += width
    }
  }

  // 然后从目标字段开始找到该行的最后一个字段
  let rowEndIndex = virtualTargetIndex
  for (let i = virtualTargetIndex + 1; i < virtualItems.length; i++) {
    const item = virtualItems[i]
    if (!item) break
    const width = item.widthPercent
    // 如果下一个字段标记为新行开始，或宽度超过100，当前行结束
    if (item.startNewRow || accumulatedWidth + width > 100) {
      // 下一行开始，当前行结束
      break
    }
    accumulatedWidth += width
    rowEndIndex = i
  }

  // 返回在原数组中对应的索引
  const rowEndItem = virtualItems[rowEndIndex]
  if (!rowEndItem) return targetIndex
  const rowEndFieldId = rowEndItem.fieldConfigId
  return fieldItems.findIndex(f => f.fieldConfigId === rowEndFieldId)
}

// 拖到字段下方区域 - drop
function handleDropBelow(sectionId: string, targetFieldConfigId: string, event: DragEvent) {
  event.preventDefault()
  event.stopPropagation()

  if (!draggingFieldItem.value) return

  const { sectionId: sourceSectionId, fieldConfigId: sourceFieldConfigId } = draggingFieldItem.value

  // 找到源区域和目标区域
  const sourceSection = currentSections.value.find((s) => s.sectionId === sourceSectionId)
  const targetSection = currentSections.value.find((s) => s.sectionId === sectionId)
  if (!sourceSection || !targetSection) return

  // 找到源字段项
  const sourceIndex = sourceSection.fieldItems.findIndex((f) => f.fieldConfigId === sourceFieldConfigId)
  if (sourceIndex === -1) return

  // 找到目标字段位置（在移除源之前）
  const targetIndex = targetSection.fieldItems.findIndex((f) => f.fieldConfigId === targetFieldConfigId)
  if (targetIndex === -1) return

  // 计算行尾索引时，如果是同区域需要排除被拖拽的字段
  const excludeFieldId = sourceSectionId === sectionId ? sourceFieldConfigId : undefined
  const rowEndIndex = findRowEndIndex(targetSection.fieldItems, targetIndex, excludeFieldId)

  // 从源区域移除
  const [movedItem] = sourceSection.fieldItems.splice(sourceIndex, 1)
  if (!movedItem) return

  // 设置强制换行标记
  movedItem.startNewRow = true

  // 计算插入位置
  let insertIndex = rowEndIndex + 1
  // 如果是同区域且源在行尾之前或等于，因为已经移除了源，需要调整
  if (sourceSectionId === sectionId && sourceIndex <= rowEndIndex) {
    insertIndex = rowEndIndex // rowEndIndex + 1 - 1
  }

  targetSection.fieldItems.splice(insertIndex, 0, movedItem)

  emit('change')
  draggingFieldItem.value = null
  dropTargetField.value = null
  dropBelowFieldId.value = null
  dragOverSectionId.value = null
}

// Tab 拖拽开始
function handleTabDragStart(tabId: string, event: DragEvent) {
  if (draggingFieldItem.value || resizingField.value || draggingSectionId.value) {
    event.preventDefault()
    return
  }
  event.stopPropagation()
  draggingTabId.value = tabId
  emit('select', null)

  if (event.dataTransfer) {
    event.dataTransfer.setData('application/json', JSON.stringify({
      type: 'tab-reorder',
      tabId
    }))
    event.dataTransfer.effectAllowed = 'move'
  }
}

// Tab 拖拽经过
function handleTabDragOver(tabId: string, event: DragEvent) {
  event.preventDefault()
  
  if (draggingTabId.value) {
     event.stopPropagation()
     if (draggingTabId.value === tabId) {
       dropTargetTab.value = null
       return
     }
     
     const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
     const midX = rect.left + rect.width / 2
     const position = event.clientX < midX ? 'before' : 'after'
     
     dropTargetTab.value = { tabId, position }
     if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'move'
     }
  } else {
     if (event.dataTransfer) {
       event.dataTransfer.dropEffect = 'none'
     }
  }
}

// 执行 Tab 排序
function executeTabReorder(targetTabId: string, position: 'before' | 'after') {
  const tabs = props.modelValue.tabs
  const sourceIndex = tabs.findIndex(t => t.tabId === draggingTabId.value)
  const targetIndex = tabs.findIndex(t => t.tabId === targetTabId)

  if (sourceIndex > -1 && targetIndex > -1) {
    const [movedTab] = tabs.splice(sourceIndex, 1)
    if (!movedTab) return

    let insertIndex = targetIndex
    if (sourceIndex < insertIndex) {
      insertIndex--
    }
    
    if (position === 'after') {
      insertIndex++
    }
    
    tabs.splice(insertIndex, 0, movedTab)
    emit('change')
  }
}

// Tab 拖拽离开
function handleTabDragLeave(event: DragEvent) {
  const relatedTarget = event.relatedTarget as HTMLElement
  if (!relatedTarget || !(event.currentTarget as HTMLElement).contains(relatedTarget)) {
    dropTargetTab.value = null
  }
}

// Tab 放置
function handleTabDrop(tabId: string, event: DragEvent) {
  event.preventDefault()
  if (draggingTabId.value && dropTargetTab.value) {
    event.stopPropagation()
    const { tabId: targetId, position } = dropTargetTab.value
    // 再次确认 id 是否匹配，防止 ghost drop
    if (targetId === tabId) {
        executeTabReorder(targetId, position)
    }
  }
  dropTargetTab.value = null
}
</script>

<template>
  <div ref="layoutCanvasRef" class="layout-canvas" @click="handleCanvasClick">
    <!-- 详情页预览容器 -->
    <div class="detail-preview">
      <!-- 详情页头部 -->
      <div class="detail-header">
        <!-- 元信息行：卡片类型 + 编号 + 状态 -->
        <div class="card-meta-row">
          <span class="card-type-name">需求卡片</span>
          <span class="card-code">#CARD-001</span>
          <span class="status-tag" style="background-color: #c8d9ff; color: #333;">进行中</span>
          <div class="meta-spacer"></div>
          <a-button type="text" size="small" class="share-btn">
            <template #icon><IconShareInternal /></template>
          </a-button>
        </div>
        <!-- 标题行 -->
        <div class="title-row">
          <span class="card-title">这是一个示例卡片标题</span>
        </div>
      </div>

      <!-- Tab 栏 -->
      <div class="detail-tabs">
        <div class="tab-list">
          <div
            v-for="tab in tabs"
            :key="tab.tabId"
            class="tab-item"
            :class="{ 
              active: activeTabId === tab.tabId, 
              selected: isTabSelected(tab),
              dragging: draggingTabId === tab.tabId,
              'drop-before': dropTargetTab?.tabId === tab.tabId && dropTargetTab?.position === 'before',
              'drop-after': dropTargetTab?.tabId === tab.tabId && dropTargetTab?.position === 'after'
            }"
            draggable="true"
            @dragstart="handleTabDragStart(tab.tabId, $event)"
            @dragover="handleTabDragOver(tab.tabId, $event)"
            @dragleave="handleTabDragLeave"
            @drop="handleTabDrop(tab.tabId, $event)"
            @click.stop="selectTab(tab)"
          >
            <span class="tab-name">{{ tab.name }}</span>
            <a-dropdown v-if="tab.tabType === 'CUSTOM'" trigger="click" @click.stop>
              <IconDown class="tab-dropdown-icon" :size="12" />
              <template #content>
                <a-doption @click="handleDeleteTab(tab)">
                  <template #icon><IconDelete /></template>
                  删除
                </a-doption>
              </template>
            </a-dropdown>
          </div>
          <div class="tab-item add-tab" @click.stop="handleAddTab">
            <IconPlus style="font-size: 14px" />
          </div>
        </div>
      </div>

      <!-- Tab 内容区 -->
      <div class="detail-body">
        <!-- 系统 Tab 提示 -->
        <div
          v-if="currentTab?.tabType === 'SYSTEM' && currentTab.systemTabType !== 'BASIC_INFO'"
          class="system-tab-hint"
        >
          <div class="system-content">
            <div class="system-icon">
              <FieldTypeIcon field-type="TEXT" size="large" style="font-size: 32px" />
            </div>
            <div class="system-text">{{ SystemTabTypeConfig[currentTab.systemTabType!]?.label || '系统' }}</div>
            <div class="system-desc">此区域内容由系统自动生成</div>
          </div>
        </div>

        <!-- 自定义 Tab 或基础信息 Tab：区域列表 -->
        <div 
          v-else 
          class="sections-container"
          @dragover.prevent
          @drop="handleContainerDrop"
        >
          <TemplateSection
            v-for="section in currentSections"
            :key="section.sectionId"
            :ref="(el) => setSectionContentRef(section.sectionId, el)"
            :section="section"
            :selected-item="selectedItem"
            :dragging-section-id="draggingSectionId"
            :drop-target-section="dropTargetSection"
            :drag-over-section-id="dragOverSectionId"
            :dragging-field-item="draggingFieldItem"
            :drop-target-field="dropTargetField"
            :drop-below-field-id="dropBelowFieldId"
            :resizing-field="resizingField"
            :field-row-gap="currentFieldRowGap"
            @update:name="(name) => handleUpdateSectionName(section.sectionId, name)"
            @delete="handleDeleteSection(section.sectionId)"
            @select="(item) => emit('select', item)"
            @section-dragstart="handleSectionDragStart(section.sectionId, $event)"
            @section-dragenter="handleDragEnterSection(section.sectionId, $event)"
            @section-dragleave="handleDragLeaveSection"
            @section-dragover="handleSectionDragOver(section.sectionId, $event)"
            @section-drop="handleDropToSection(section.sectionId, $event)"
            @field-dragstart="(fieldId, evt) => handleFieldDragStart(section.sectionId, fieldId, evt)"
            @field-dragover="(fieldId, evt) => handleFieldDragOver(section.sectionId, fieldId, evt)"
            @field-dragleave="handleFieldDragLeave"
            @field-drop="(fieldId, evt) => handleFieldDrop(section.sectionId, fieldId, evt)"
            @field-drop-below-over="(fieldId, evt) => handleDropBelowOver(fieldId, evt)"
            @field-drop-below-leave="handleDropBelowLeave"
            @field-drop-below="(fieldId, evt) => handleDropBelow(section.sectionId, fieldId, evt)"
            @field-dragend="handleFieldDragEnd"
            @delete-field="(fieldId) => handleDeleteField(section.sectionId, fieldId)"
            @resize-start="(fieldId, evt) => startResize(section.sectionId, fieldId, evt)"
          />

          <!-- 添加区域 -->
          <div
            v-if="currentTab?.tabType === 'CUSTOM' || currentTab?.systemTabType === 'BASIC_INFO'"
            class="add-section"
            @click.stop="handleAddSection"
          >
            <IconPlus /> 添加区域
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.layout-canvas {
  height: 100%;
  overflow: auto;
  background: #f5f6f7;
  padding: 20px;
}

.detail-preview {
  max-width: 960px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

// 头部
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

.meta-spacer {
  flex: 1;
}

.share-btn {
  color: var(--color-text-3);

  &:hover {
    color: rgb(var(--primary-6));
  }
}

.card-type-name {
  font-size: 12px;
  color: var(--color-text-3);
}

.card-code {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-3);
  flex-shrink: 0;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  background-color: var(--color-fill-2);
  color: var(--color-text-1) !important;
  white-space: nowrap;
}

.card-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-1);
  max-width: 700px;
  word-break: break-all;
}

// Tab 栏
.detail-tabs {
  padding: 0 0 0 20px;
  border-bottom: 1px solid var(--color-border-2);
  flex-shrink: 0;
  overflow-x: auto;
  overflow-y: hidden;

  &::-webkit-scrollbar {
    height: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--color-border-3);
    border-radius: 2px;
  }
}

.tab-list {
  display: flex;
  padding: 0;
  gap: 8px;
  white-space: nowrap;
}

.tab-item {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 4px;
  padding: 8px 0;
  margin-right: 18px;
  font-size: 13px;
  color: var(--color-text-2);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.2s;

  &:first-child {
    margin-left: 0;
    padding-left: 0;
  }

  &:hover {
    color: rgb(var(--primary-6));
  }

  &.active {
    font-weight: 600;
    color: var(--color-text-1);
    border-bottom-color: rgb(var(--primary-6));
  }

  &.selected {
    background: rgb(var(--primary-1));
    border-radius: 4px 4px 0 0;
  }

  &.add-tab {
    display: flex;
    align-items: center;
    color: var(--color-text-3);
    padding: 10px 10px;

    &:hover {
      color: rgb(var(--primary-6));
    }
  }

  .tab-dropdown-icon {
    color: var(--color-text-3);
    transition: color 0.2s;

    &:hover {
      color: rgb(var(--primary-6));
    }
  }

  &.dragging {
    opacity: 0.5;
  }

  &.drop-before {
    position: relative;
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 2px;
      background: rgb(var(--primary-6));
      z-index: 10;
      border-radius: 2px;
    }
  }

  &.drop-after {
    position: relative;
    &::after {
      content: '';
      position: absolute;
      right: 0;
      top: 0;
      bottom: 0;
      width: 2px;
      background: rgb(var(--primary-6));
      z-index: 10;
      border-radius: 2px;
    }
  }
}

// 内容区
.detail-body {
  padding: 16px 20px 16px 20px;
  min-height: 500px;
  background: var(--color-bg-1);
}

.system-tab-hint {
  .system-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 20px;
    color: var(--color-text-3);
  }

  .system-icon {
    margin-bottom: 16px;
    opacity: 0.5;
  }

  .system-text {
    font-size: 16px;
    margin-bottom: 8px;
  }

  .system-desc {
    font-size: 13px;
  }
}

// 区域容器
.sections-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid var(--color-border);
  overflow: hidden;
  transition: all 0.2s;

  &:hover {
    .section-actions {
      opacity: 1;
    }
  }

  &.selected {
    border-color: rgb(var(--primary-6));
    box-shadow: 0 0 0 1px rgb(var(--primary-6));
  }

  &.drag-over {
    border-color: rgb(var(--primary-6));
    border-style: dashed;
    background: rgb(var(--primary-1));
  }

  &.dragging {
    opacity: 0.5;
    border-style: dashed;
  }

  &.drop-before {
    position: relative;
    &::before {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      top: 0;
      height: 4px;
      background: rgb(var(--primary-6));
      z-index: 10;
      border-radius: 2px;
    }
  }

  &.drop-after {
    position: relative;
    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: 0;
      height: 4px;
      background: rgb(var(--primary-6));
      z-index: 10;
      border-radius: 2px;
    }
  }
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fafbfc;
  border-bottom: 1px solid var(--color-border);
}

// 添加区域按钮
.add-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 20px;
  color: var(--color-text-3);
  font-size: 14px;
  border: 2px dashed var(--color-border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;

  &:hover {
    color: rgb(var(--primary-6));
    border-color: rgb(var(--primary-6));
    background: rgb(var(--primary-1));
  }
}
</style>
