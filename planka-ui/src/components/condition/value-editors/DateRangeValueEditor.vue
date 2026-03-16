<template>
  <div class="date-range-value-editor">
    <!-- 日期范围选择器 -->
    <a-range-picker
      :model-value="dateRangeValue"
      size="small"
      style="width: 100%"
      @change="handleRangeChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DateOperator } from '@/types/condition'

/**
 * Props定义
 */
const props = defineProps<{
  /** 日期操作符 */
  modelValue: DateOperator
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: DateOperator]
}>()

/**
 * 日期范围值
 * a-range-picker 需要 [start, end] 格式的数组
 */
const dateRangeValue = computed(() => {
  const op = props.modelValue as any
  const start = op.start?.value || ''
  const end = op.end?.value || ''

  if (start && end) {
    return [start, end]
  }
  return []
})

/**
 * 处理日期范围变化
 */
function handleRangeChange(dates: (string | undefined)[] | undefined) {
  const newOp = { ...props.modelValue } as any

  if (dates && dates.length === 2 && dates[0] && dates[1]) {
    newOp.start = { type: 'SPECIFIC', value: dates[0] }
    newOp.end = { type: 'SPECIFIC', value: dates[1] }
  } else {
    // 如果没有选择完整范围，清除值
    newOp.start = { type: 'SPECIFIC', value: '' }
    newOp.end = { type: 'SPECIFIC', value: '' }
  }

  emit('update:modelValue', newOp)
}
</script>

<style scoped>
.date-range-value-editor {
  width: 100%;
}
</style>
