<script setup lang="ts">
/**
 * 文本字段显示组件
 */
import { computed } from 'vue'

const props = defineProps<{
  value?: string | null
  placeholder?: string
}>()

const displayValue = computed(() => {
  if (props.value === null || props.value === undefined) {
    return props.placeholder || '-'
  }
  return String(props.value) || props.placeholder || '-'
})
</script>

<template>
  <span class="field-text-display">{{ displayValue }}</span>
</template>

<style scoped>
.field-text-display {
  color: inherit;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
