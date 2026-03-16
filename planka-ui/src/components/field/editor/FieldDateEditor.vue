<script setup lang="ts">
/**
 * 日期字段编辑组件
 *
 * 接收时间戳(number)作为 modelValue，内部转换为 Date/String 处理
 * 支持 DATE 和 DATETIME 格式
 */
import { computed } from 'vue'
import dayjs from 'dayjs'
import type { DateRenderConfig } from '@/types/view-data'

const props = defineProps<{
  placeholder?: string
  disabled?: boolean
  autoFocus?: boolean
  renderConfig?: DateRenderConfig | null
}>()

// modelValue is timestamp number
const modelValue = defineModel<number | undefined | null>({ required: false })

const emit = defineEmits<{
  save: []
  'popup-visible-change': [visible: boolean]
}>()

// 是否为日期时间格式（显示时间选择器）
const isDateTime = computed(() => {
  return props.renderConfig?.dateFormat === 'DATETIME' ||
         props.renderConfig?.dateFormat === 'DATETIME_SECOND'
})

// 日期选择器格式
const pickerFormat = computed(() => {
  if (props.renderConfig?.dateFormat === 'DATETIME_SECOND') {
    return 'YYYY-MM-DD HH:mm:ss'
  }
  if (props.renderConfig?.dateFormat === 'DATETIME') {
    return 'YYYY-MM-DD HH:mm'
  }
  return 'YYYY-MM-DD'
})

// Convert timestamp to string/Date for ARCO DatePicker
const dateValue = computed({
  get() {
    if (!modelValue.value) return undefined
    const format = isDateTime.value ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD'
    return dayjs(modelValue.value).format(format)
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
    :show-time="isDateTime"
    :format="pickerFormat"
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
