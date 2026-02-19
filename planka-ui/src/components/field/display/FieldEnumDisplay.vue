<script setup lang="ts">
/**
 * 枚举字段显示组件
 *
 * 用于显示枚举字段的值，支持单选和多选，显示带颜色的标签
 */
import type { EnumOptionDTO } from '@/types/view-data'

defineProps<{
  /** 已选中的枚举选项列表 */
  options: EnumOptionDTO[]
  /** 空值占位符 */
  placeholder?: string
}>()

function getTagStyle(color?: string) {
  if (!color) {
    return {}
  }
  return {
    backgroundColor: color,
  }
}
</script>

<template>
  <span v-if="options.length > 0" class="field-enum-display">
    <span
      v-for="option in options"
      :key="option.id"
      class="enum-tag"
      :style="getTagStyle(option.color)"
    >
      {{ option.label }}
    </span>
  </span>
  <span v-else class="field-enum-empty">{{ placeholder || '-' }}</span>
</template>

<style scoped lang="scss">
.field-enum-display {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.enum-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  background-color: var(--color-fill-2);
  color: #333 !important;
  white-space: nowrap;
}

.field-enum-empty {
  color: var(--color-text-4);
}
</style>
