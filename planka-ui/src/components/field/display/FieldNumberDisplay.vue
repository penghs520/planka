<script setup lang="ts">
/**
 * 数字字段显示组件
 * 支持普通数字、百分数、千位分隔符、进度条等多种显示样式
 */
import { computed } from 'vue'
import type { NumberRenderConfig } from '@/types/view-data'

const props = defineProps<{
  value?: number | null
  renderConfig?: NumberRenderConfig | null
  placeholder?: string
}>()

// 计算显示格式（兼容旧数据）
const displayFormat = computed(() => {
  if (!props.renderConfig) return 'NORMAL'
  if (props.renderConfig.displayFormat) {
    return props.renderConfig.displayFormat
  }
  // 兼容旧数据
  return props.renderConfig.showThousandSeparator ? 'THOUSAND_SEPARATOR' : 'NORMAL'
})

// 是否显示进度条
const isProgressBar = computed(() => {
  return displayFormat.value === 'PERCENT' && props.renderConfig?.percentStyle === 'PROGRESS_BAR'
})

// 计算进度条百分比（0-100）
const progressPercent = computed(() => {
  if (props.value === null || props.value === undefined) return 0
  const val = props.value
  // 如果值在0-1之间，转换为0-100
  if (val >= 0 && val <= 1) {
    return val * 100
  }
  // 限制在0-100范围内
  return Math.min(Math.max(val, 0), 100)
})

// 格式化数字显示
const displayValue = computed(() => {
  if (props.value === null || props.value === undefined || typeof props.value !== 'number') {
    return props.placeholder || '-'
  }

  const precision = props.renderConfig?.precision ?? 2
  let result: string

  switch (displayFormat.value) {
    case 'PERCENT': {
      // 百分数格式：如果值在0-1之间，乘以100
      const percentValue = props.value <= 1 && props.value >= 0 ? props.value * 100 : props.value
      result = percentValue.toFixed(precision) + '%'
      break
    }

    case 'THOUSAND_SEPARATOR':
      // 千位分隔符格式
      result = props.value.toLocaleString('zh-CN', {
        minimumFractionDigits: precision,
        maximumFractionDigits: precision,
      })
      if (props.renderConfig?.unit) {
        result += ` ${props.renderConfig.unit}`
      }
      break

    default:
      // 普通格式
      result = props.value.toFixed(precision)
      if (props.renderConfig?.unit) {
        result += ` ${props.renderConfig.unit}`
      }
  }

  return result
})
</script>

<template>
  <div class="field-number-display">
    <!-- 进度条模式 -->
    <template v-if="isProgressBar">
      <a-progress :percent="progressPercent" size="small" :show-text="true">
        <template #text>
          {{ displayValue }}
        </template>
      </a-progress>
    </template>
    <!-- 普通文本模式 -->
    <template v-else>
      <span>{{ displayValue }}</span>
    </template>
  </div>
</template>

<style scoped>
.field-number-display {
  color: inherit;
}
.field-number-display :deep(.arco-progress) {
  min-width: 100px;
}
</style>
