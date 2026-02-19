<script setup lang="ts">
/**
 * 架构字段显示组件
 */
import { computed } from 'vue'

const props = defineProps<{
  value?: unknown
  placeholder?: string
}>()

// 格式化架构属性值为显示文本
function formatStructureValue(item: { id?: string; name?: string; next?: unknown } | null): string {
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

const displayValue = computed(() => {
  return formatStructureValue(props.value as { id?: string; name?: string; next?: unknown }) || props.placeholder || '-'
})
</script>

<template>
  <span class="field-structure-display">{{ displayValue }}</span>
</template>

<style scoped>
.field-structure-display {
  color: inherit;
}
</style>
