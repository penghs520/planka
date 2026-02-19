<script setup lang="ts">
import { IconInfoCircle } from '@arco-design/web-vue/es/icon'
import { useSlots } from 'vue'

defineProps<{
  title?: string
  content?: string
  position?: 'top' | 'tl' | 'tr' | 'bottom' | 'bl' | 'br' | 'left' | 'lt' | 'lb' | 'right' | 'rt' | 'rb'
  width?: string
  simple?: boolean
}>()

const slots = useSlots()
</script>

<template>
  <!-- 简单模式：使用 tooltip -->
  <a-tooltip v-if="simple" :position="position || 'right'" :content="content">
    <IconInfoCircle class="help-icon" />
  </a-tooltip>
  <!-- 完整模式：使用 popover -->
  <a-popover v-else :position="position || 'right'" :content-style="{ padding: '16px', width: width }">
    <IconInfoCircle class="help-icon" />
    <template #content>
      <div v-if="title" class="popover-title">{{ title }}</div>
      <slot v-if="slots.default" />
      <div v-else-if="content" class="popover-content" v-html="content" />
    </template>
  </a-popover>
</template>

<style scoped>
.popover-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--color-border-2);
}

.help-icon {
  margin-left: 4px;
  font-size: 14px;
  color: rgb(var(--primary-6));
  cursor: help;
  vertical-align: middle;
  transition: color 0.2s;
}

.help-icon:hover {
  color: rgb(var(--primary-5));
}

.popover-content {
  max-width: 320px;
  padding: 16px 20px;
  background: rgb(var(--warning-1));
  border-left: 3px solid rgb(var(--warning-6));
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-2);
}

.popover-content :deep(p) {
  margin: 4px 0;
}

.popover-content :deep(p:first-child) {
  margin-top: 0;
}

.popover-content :deep(p:last-child) {
  margin-bottom: 0;
}

.popover-content :deep(strong) {
  color: rgb(var(--warning-6));
}
</style>
