<script setup lang="ts">
/**
 * 文本字段显示组件
 */
import { computed } from 'vue'

const props = defineProps<{
  value?: string | null
  placeholder?: string
}>()

const isEmpty = computed(() => {
  return props.value === null || props.value === undefined || props.value === ''
})

const displayValue = computed(() => {
  if (isEmpty.value) {
    return props.placeholder || '-'
  }
  return String(props.value)
})
</script>

<template>
  <span :class="['field-text-display', { 'is-empty': isEmpty }]">{{ displayValue }}</span>
</template>

<style scoped>
.field-text-display {
  color: inherit;
  white-space: pre-wrap;
  word-break: break-all;

  &.is-empty {
    color: var(--color-text-3);
  }
}
</style>
