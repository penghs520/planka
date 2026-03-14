<template>
  <span class="card-title-display">
    <template v-if="!title">
      <span class="card-title-fallback">{{ fallback }}</span>
    </template>
    <template v-else-if="title.type === 'PURE'">
      <span class="card-title-pure">{{ title.value || title.displayValue }}</span>
    </template>
    <template v-else-if="title.type === 'JOINT'">
      <!-- 前缀拼接 -->
      <template v-if="title.area === 'PREFIX'">
        <span class="card-title-joint-parts">{{ jointPartsText }}</span>
        <span class="card-title-value">{{ title.value }}</span>
      </template>
      <!-- 后缀拼接 -->
      <template v-else>
        <span class="card-title-value">{{ title.value }}</span>
        <span class="card-title-joint-parts">{{ jointPartsText }}</span>
      </template>
    </template>
    <template v-else>
      <!-- 兜底：直接显示 displayValue (使用 as any 断言因为类型系统已覆盖所有类型) -->
      <span class="card-title-fallback">{{ (title as any).displayValue || fallback }}</span>
    </template>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CardTitle } from '@/types/card'

interface Props {
  title?: CardTitle | null
  fallback?: string
}

const props = withDefaults(defineProps<Props>(), {
  fallback: '-',
})

/**
 * 计算拼接部分的文本
 * 多个 part 之间用空格分隔
 */
const jointPartsText = computed(() => {
  if (!props.title || props.title.type !== 'JOINT') {
    return ''
  }
  const multiParts = props.title.multiParts as Array<{ parts?: Array<{ name?: string }> }> | undefined
  if (!multiParts || multiParts.length === 0) {
    return ''
  }
  // 收集所有 part 的名称
  const partNames: string[] = []
  for (const group of multiParts) {
    if (group.parts) {
      for (const part of group.parts) {
        if (part.name) {
          partNames.push(part.name)
        }
      }
    }
  }
  // 用空格连接多个 part
  return partNames.join(' ')
})
</script>

<style scoped>
.card-title-display {
  display: inline;
}

.card-title-pure,
.card-title-value {
  color: inherit;
}

.card-title-joint-parts {
  color: var(--color-text-3);
  /* 添加与原始值之间的间距 */
  margin: 0 2px;
}

.card-title-fallback {
  color: var(--color-text-4);
}
</style>
