<script setup lang="ts">
/**
 * Markdown 字段编辑组件
 * 使用 Tiptap 编辑器，提供类似飞书文档的编辑体验
 */
import { ref } from 'vue'
import TiptapEditor from './TiptapEditor.vue'

defineProps<{
  /** 占位符 */
  placeholder?: string
  /** 是否禁用 */
  disabled?: boolean
  /** 编辑器高度 (px) */
  height?: number
}>()

const modelValue = defineModel<string>({ required: false, default: '' })

const emit = defineEmits<{
  save: []
  cancel: []
}>()

const editorRef = ref<InstanceType<typeof TiptapEditor> | null>(null)

// 暴露编辑器的方法供父组件调用
defineExpose({
  onSaveSuccess: () => editorRef.value?.onSaveSuccess(),
  onCancelEdit: () => editorRef.value?.onCancelEdit(),
})
</script>

<template>
  <div class="field-markdown-editor">
    <TiptapEditor
      ref="editorRef"
      v-model="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :height="height"
      @save="emit('save')"
      @cancel="emit('cancel')"
    />
  </div>
</template>

<style scoped lang="scss">
.field-markdown-editor {
  width: 100%;
}
</style>
