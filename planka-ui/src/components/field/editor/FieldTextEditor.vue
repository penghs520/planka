<script setup lang="ts">
/**
 * 文本字段编辑组件
 */
import { ref, onMounted } from 'vue'

const props = defineProps<{
  /** 占位符 */
  placeholder?: string
  /** 是否禁用 */
  disabled?: boolean
  /** 是否自动聚焦 */
  autoFocus?: boolean
}>()

const modelValue = defineModel<string>({ required: false })

const inputRef = ref()

const emit = defineEmits<{
  keydown: [event: KeyboardEvent]
  blur: [event: FocusEvent]
}>()

function handleKeydown(event: KeyboardEvent) {
  emit('keydown', event)
}

function handleBlur(event: FocusEvent) {
  emit('blur', event)
}

onMounted(() => {
  if (props.autoFocus && inputRef.value) {
    inputRef.value.focus()
  }
})
</script>

<template>
  <a-input
    ref="inputRef"
    v-model="modelValue"
    class="field-text-editor"
    size="mini"
    :placeholder="placeholder"
    :disabled="disabled"
    @keydown="handleKeydown"
    @blur="handleBlur"
  />
</template>

<style scoped>
.field-text-editor {
  width: 100%;
}
/* 调整输入框内边距以适应紧凑显示 */
:deep(.arco-input) {
  font-size: 13px;
  padding: 2px 6px;
}
</style>
