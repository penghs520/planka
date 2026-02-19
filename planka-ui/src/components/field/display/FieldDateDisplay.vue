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

const displayValue = computed(() => {
  if (!props.value) {
    return props.placeholder || '-'
  }

  if (props.renderConfig) {
    if (props.renderConfig.dateFormat === 'DATETIME' || props.renderConfig.dateFormat === 'DATETIME_SECOND') {
      return formatDateTime(props.value)
    }
  }
  return formatDate(props.value)
})
</script>

<template>
  <span class="field-date-display">{{ displayValue }}</span>
</template>

<style scoped>
.field-date-display {
  color: inherit;
}
</style>
