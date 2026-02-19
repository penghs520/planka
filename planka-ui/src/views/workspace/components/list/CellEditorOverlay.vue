<script setup lang="ts">
/**
 * 单元格编辑器浮层组件
 * 
 * 使用 Native Overlay Pattern：
 * - 编辑器作为表格外的独立浮层渲染
 * - 不参与表格的虚拟滚动和重渲染
 * - 通过绝对定位覆盖在目标单元格上
 */
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import type { LinkedCard, FieldValue } from '@/types/card'
import type { EditingCell } from '../../composables/list/useCellEditing'
import FieldEditor from '@/components/field/editor/FieldEditor.vue'

const props = defineProps<{
  /** 编辑中的单元格信息 */
  editingCell: EditingCell | null
  /** 是否正在保存 */
  saving: boolean
  /** 单元格显示值（用于不支持编辑的类型） */
  displayValue: string
  /** 表格容器引用（用于计算定位） */
  tableContainer: HTMLElement | null
}>()

// 编辑值 - 使用 defineModel 实现双向绑定
const textValue = defineModel<string>('textValue', { required: true })
const numberValue = defineModel<number | undefined>('numberValue', { required: true })
const dateValue = defineModel<string | undefined>('dateValue', { required: true })
const enumValue = defineModel<string[]>('enumValue', { required: true })
const linkValue = defineModel<LinkedCard[]>('linkValue', { required: true })
const structureValue = defineModel<FieldValue | null>('structureValue', { required: true })

const emit = defineEmits<{
  keydown: [event: KeyboardEvent]
  save: []
  cancel: []
}>()

// 浮层位置
const overlayStyle = ref<Record<string, string>>({
  display: 'none',
})

// 浮层引用
const overlayRef = ref<HTMLElement | null>(null)

/** 判断字段类型 */
const isTextType = computed(() => {
  const type = props.editingCell?.fieldType
  return type === 'TEXT' || type === 'TEXTAREA' || type === 'MARKDOWN' || type === 'WEB_URL' || !type
})

const isNumberType = computed(() => props.editingCell?.fieldType === 'NUMBER')
const isDateType = computed(() => props.editingCell?.fieldType === 'DATE')
const isEnumType = computed(() => props.editingCell?.fieldType === 'ENUM')
const isLinkType = computed(() => props.editingCell?.fieldType === 'LINK')
const isStructureType = computed(() => props.editingCell?.fieldType === 'STRUCTURE')

// 统一模型值代理
const activeModelValue = computed({
  get() {
    if (isTextType.value) return textValue.value
    if (isNumberType.value) return numberValue.value
    if (isDateType.value) {
      return dateValue.value ? new Date(dateValue.value).getTime() : undefined
    }
    if (isEnumType.value) return enumValue.value
    if (isLinkType.value) return linkValue.value
    if (isStructureType.value) return structureValue.value
    return undefined
  },
  set(val: any) {
    if (isTextType.value) textValue.value = val
    if (isNumberType.value) numberValue.value = val
    if (isDateType.value) {
      if (val) {
        const d = new Date(val)
        const year = d.getFullYear()
        const month = String(d.getMonth() + 1).padStart(2, '0')
        const day = String(d.getDate()).padStart(2, '0')
        dateValue.value = `${year}-${month}-${day}`
      } else {
        dateValue.value = undefined
      }
    }
    if (isEnumType.value) enumValue.value = val
    if (isLinkType.value) linkValue.value = val
    if (isStructureType.value) structureValue.value = val
  }
})

/** 计算浮层位置 */
function updatePosition() {
  if (!props.editingCell) {
    overlayStyle.value = { display: 'none' }
    return
  }

  const { cardId, fieldId } = props.editingCell

  // 查找目标单元格
  // 使用 data 属性定位单元格
  const cellSelector = `[data-cell-id="${cardId}-${fieldId}"]`
  const cellElement = document.querySelector(cellSelector) as HTMLElement

  if (!cellElement) {
    // 降级：使用 row + column 定位
    const rowElement = document.querySelector(`[data-row-id="${cardId}"]`) as HTMLElement
    if (!rowElement) {
      overlayStyle.value = { display: 'none' }
      return
    }
    
    // 暂时隐藏，无法精确定位
    overlayStyle.value = { display: 'none' }
    return
  }

  const cellRect = cellElement.getBoundingClientRect()
  
  // 使用单元格的实际尺寸，不超出列宽
  const cellWidth = cellRect.width
  const cellHeight = Math.max(cellRect.height, 32)
  
  overlayStyle.value = {
    display: 'block',
    position: 'fixed',
    top: `${cellRect.top}px`,
    left: `${cellRect.left}px`,
    width: `${cellWidth}px`,  // 使用单元格实际宽度
    minHeight: `${cellHeight}px`,
    zIndex: '1100',
  }
}

/** 手动聚焦到输入框 */
function focusInput() {
  // 延迟执行以确保 DOM 已更新
  setTimeout(() => {
    const overlay = overlayRef.value
    if (!overlay) return
    
    // 尝试多种选择器找到输入框
    const input = overlay.querySelector(
      'input:not([type="hidden"]), textarea, .arco-input, .arco-select-view-input'
    ) as HTMLElement
    
    if (input) {
      input.focus()
      // 如果是文本输入框，选中所有文本
      if (input instanceof HTMLInputElement || input instanceof HTMLTextAreaElement) {
        input.select()
      }
    }
  }, 50)
}

/** 监听编辑单元格变化 */
watch(
  () => props.editingCell,
  async (cell) => {
    if (cell) {
      await nextTick()
      updatePosition()
      // 浮层显示后聚焦到输入框
      focusInput()
    } else {
      overlayStyle.value = { display: 'none' }
    }
  },
  { immediate: true }
)

/** 处理窗口滚动和调整大小 */
function handleReposition() {
  if (props.editingCell) {
    updatePosition()
  }
}

onMounted(() => {
  window.addEventListener('scroll', handleReposition, true)
  window.addEventListener('resize', handleReposition)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleReposition, true)
  window.removeEventListener('resize', handleReposition)
})

function handleKeydown(event: KeyboardEvent) {
  emit('keydown', event)
}

function handleSave() {
  emit('save')
}

function handleCancel() {
  emit('cancel')
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="editingCell"
      ref="overlayRef"
      class="cell-editor-overlay"
      :style="overlayStyle"
    >
      <FieldEditor
        v-model="activeModelValue"
        :field-type="editingCell.fieldType"
        :field-id="editingCell.fieldId"
        :render-config="editingCell.renderConfig"
        :disabled="saving"
        :auto-focus="!isLinkType"
        @save="handleSave"
        @cancel="handleCancel"
        @keydown="handleKeydown"
      />
    </div>
  </Teleport>
</template>

<style scoped lang="scss">
.cell-editor-overlay {
  // 无背景色，直接显示输入框
  background: transparent;
  padding: 0;
  box-sizing: border-box;
  z-index: 1100 !important;
  
  // 确保输入框占满宽度
  :deep(.arco-input-wrapper),
  :deep(.arco-select),
  :deep(.arco-picker),
  :deep(.arco-input) {
    width: 100%;
  }
}
</style>
