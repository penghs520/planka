<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  text?: string
  keyword?: string
}>()

const highlightedHtml = computed(() => {
  const text = props.text || ''
  const keyword = props.keyword || ''

  if (!text) return ''
  if (!keyword) return text

  // 转义正则特殊字符
  const escapedKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const reg = new RegExp(`(${escapedKeyword})`, 'gi')
  return text.replace(reg, '<span class="highlight-text">$1</span>')
})
</script>

<template>
  <span class="highlight-wrapper" v-html="highlightedHtml"></span>
</template>

<style scoped>
:deep(.highlight-text) {
  color: rgb(var(--primary-6));
  font-weight: 500;
}
</style>
