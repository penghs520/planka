<script setup lang="ts">
/**
 * 单元格编辑器组件
 * 根据字段类型渲染不同的编辑控件
 */
import { computed } from 'vue'
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
    // Date: CellEditor uses string (YYYY-MM-DD), FieldEditor expects number (timestamp)
    // We need to convert.
    // Wait, CellEditor useCellEditing might be binding string 'YYYY-MM-DD' to dateValue?
    // Let's check FieldDateEditor again. It takes `modelValue: number`.
    // And internally converts to/from string for a-date-picker.
    // If CellEditor.dateValue is string 'YYYY-MM-DD' (from Arco DatePicker default), we need to parse it to timestamp for FieldEditor.
    // But wait, if CellEditor.dateValue IS bound to a-date-picker in the OLD code, then it WAS string.
    // Now we use FieldEditor -> FieldDateEditor. FieldDateEditor expects number.
    // So here we must convert dateValue (string) -> timestamp (number).
    // And setter: timestamp (number) -> dateValue (string).
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
      // val is number (timestamp) from FieldDateEditor
      // Convert back to string for dateValue
      if (val) {
        const d = new Date(val)
        // Format YYYY-MM-DD
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
  <div class="cell-editing">
    <!-- 所有类型：直接渲染 FieldEditor -->
    <FieldEditor
      v-if="editingCell"
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
    <span v-else class="cell-value">{{ displayValue }}</span>
  </div>
</template>

<style scoped lang="scss">
.cell-editing {
  width: 100%;
}

.cell-value {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.link-trigger {
    cursor: pointer;
    color: var(--color-primary);

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
