<script setup lang="ts">
/**
 * 日期字段编辑组件
 * 
 * 接收时间戳(number)作为 modelValue，内部转换为 Date/String 处理
 */
import { computed } from 'vue'
import dayjs from 'dayjs'

const props = defineProps<{
  placeholder?: string
  disabled?: boolean
  autoFocus?: boolean // DatePicker might standardly auto-popup if needed
}>()

// 使用 void 表示 props 在模板中使用
void props

// modelValue is timestamp number
const modelValue = defineModel<number | undefined | null>({ required: false })

const emit = defineEmits<{
  save: []
  'popup-visible-change': [visible: boolean]
}>()

// Convert timestamp to string/Date for ARCO DatePicker
const dateValue = computed({
  get() {
    return modelValue.value ? dayjs(modelValue.value).format('YYYY-MM-DD') : undefined
  },
  set(val: string | undefined) {
    modelValue.value = val ? dayjs(val).valueOf() : undefined
  }
})

function handleChange() {
  emit('save')
}

function handleClear() {
  emit('save')
}

function handlePopupVisibleChange(visible: boolean) {
  emit('popup-visible-change', visible)
  if (!visible) {
    emit('save')
  }
}
</script>

<template>
  <a-date-picker
    v-model="dateValue"
    class="field-date-editor"
    size="mini"
    :placeholder="placeholder"
    :disabled="disabled"
    allow-clear
    :default-popup-visible="true"
    @change="handleChange"
    @clear="handleClear"
    @popup-visible-change="handlePopupVisibleChange"
  />
</template>

<style scoped>
.field-date-editor {
  width: 100%;
}
:deep(.arco-picker-input input) {
  font-size: 13px;
}
</style>
