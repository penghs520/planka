<script setup lang="ts">
import { computed } from 'vue'
import { IconCalendar, IconClockCircle, IconApps, IconTool } from '@arco-design/web-vue/es/icon'
import type { Component } from 'vue'
import { SchemaSubType } from '@/types/schema'

export interface FormulaTypeIconProps {
  /** 公式类型（SchemaSubType） */
  formulaType: string
  /** 图标大小 */
  size?: 'small' | 'medium' | 'large'
  /** 图标颜色 */
  color?: string
}

const props = withDefaults(defineProps<FormulaTypeIconProps>(), {
  size: 'medium',
  color: 'var(--color-text-3)',
})

// 公式类型到图标组件的映射
const formulaTypeIconMap: Record<string, Component> = {
  [SchemaSubType.TIME_POINT_FORMULA_DEFINITION]: IconClockCircle,
  [SchemaSubType.TIME_RANGE_FORMULA_DEFINITION]: IconClockCircle,
  [SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION]: IconCalendar,
  [SchemaSubType.CARD_COLLECTION_FORMULA_DEFINITION]: IconApps,
  [SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION]: IconTool,
}

// 获取公式类型对应的图标组件
const iconComponent = computed(() => {
  return formulaTypeIconMap[props.formulaType] || IconTool
})

// 大小映射
const sizeMap = {
  small: '14px',
  medium: '16px',
  large: '20px',
}
</script>

<template>
  <component
    :is="iconComponent"
    class="formula-type-icon"
    :style="{
      fontSize: sizeMap[size],
      color: color,
    }"
  />
</template>

<style scoped lang="scss">
.formula-type-icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
</style>
