<script setup lang="ts">
/**
 * Markdown 字段显示组件
 * 使用 Tiptap 只读模式渲染 Markdown 内容，与编辑器外观一致
 */
import TiptapDisplay from '../display/TiptapDisplay.vue'

defineProps<{
  /** Markdown 内容 */
  value?: string | null
  /** 空值占位符 */
  placeholder?: string
  /** 最小高度 (px) */
  height?: number
}>()
</script>

<template>
  <div class="field-markdown-display">
    <TiptapDisplay
      :value="value"
      :placeholder="placeholder"
      :height="height"
    />
  </div>
</template>

<style scoped lang="scss">
.field-markdown-display {
  width: 100%;
}
</style>
