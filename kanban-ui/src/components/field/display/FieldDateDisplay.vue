<script setup lang="ts">
/**
 * 日期字段显示组件
 */
import { computed } from 'vue'
import type { DateRenderConfig } from '@/types/view-data'
import { formatDate, formatDateTime } from '@/utils/format'

const props = defineProps<{
  value?: number | null
  renderConfig?: DateRenderConfig | null
  placeholder?: string
}>()

const isEmpty = computed(() => !props.value)

const displayValue = computed(() => {
  if (isEmpty.value) {
    return props.placeholder || '-'
  }

  const value = props.value!
  if (props.renderConfig) {
    if (props.renderConfig.dateFormat === 'DATETIME' || props.renderConfig.dateFormat === 'DATETIME_SECOND') {
      return formatDateTime(value)
    }
  }
  return formatDate(value)
})
</script>

<template>
  <span :class="['field-date-display', { 'is-empty': isEmpty }]">{{ displayValue }}</span>
</template>

<style scoped>
.field-date-display {
  color: inherit;

  &.is-empty {
    color: var(--color-text-3);
  }
}
</style>
