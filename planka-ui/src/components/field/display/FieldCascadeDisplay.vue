<script setup lang="ts">
/**
 * 级联属性显示组件
 */
import { computed } from 'vue'

const props = defineProps<{
  value?: unknown
  placeholder?: string
}>()

// 格式化级联属性值为显示文本
function formatCascadeFieldValue(item: { id?: string; name?: string; next?: unknown } | null): string {
  if (!item) return ''
  const names: string[] = []
  let current: { id?: string; name?: string; next?: unknown } | null = item
  while (current) {
    if (current.name) {
      names.push(current.name)
    }
    current = current.next as { id?: string; name?: string; next?: unknown } | null
  }
  return names.join(' / ')
}

const formattedValue = computed(() => formatCascadeFieldValue(props.value as { id?: string; name?: string; next?: unknown }))

const isEmpty = computed(() => !formattedValue.value)

const displayValue = computed(() => {
  return formattedValue.value || props.placeholder || '-'
})
</script>

<template>
  <span :class="['field-cascade-display', { 'is-empty': isEmpty }]">{{ displayValue }}</span>
</template>

<style scoped>
.field-cascade-display {
  color: inherit;

  &.is-empty {
    color: var(--color-text-3);
  }
}
</style>
