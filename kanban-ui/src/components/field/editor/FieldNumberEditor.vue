<script setup lang="ts">
/**
 * 数字字段编辑组件
 */
import { ref, onMounted } from 'vue'
import type { NumberRenderConfig } from '@/types/view-data'

const props = defineProps<{
  renderConfig?: NumberRenderConfig | null
  placeholder?: string
  disabled?: boolean
  autoFocus?: boolean
}>()

const modelValue = defineModel<number | undefined>({ required: false })

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
  <a-input-number
    ref="inputRef"
    v-model="modelValue"
    class="field-number-editor"
    size="mini"
    :placeholder="placeholder"
    :disabled="disabled"
    :precision="renderConfig?.precision ?? 0"
    hide-button
    @keydown="handleKeydown"
    @blur="handleBlur"
  />
</template>

<style scoped>
.field-number-editor {
  width: 100%;
}
:deep(.arco-input-number-input) {
  font-size: 13px;
  padding: 2px 6px;
}
</style>
