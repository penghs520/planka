<script setup lang="ts">
/**
 * 链接字段编辑组件
 */
import { ref, onMounted } from 'vue'

const props = defineProps<{
  placeholder?: string
  disabled?: boolean
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
    class="field-web-url-editor"
    size="mini"
    :placeholder="placeholder"
    :disabled="disabled"
    @keydown="handleKeydown"
    @blur="handleBlur"
  >
    <template #prepend>
       http(s)://
    </template>
  </a-input>
</template>

<style scoped>
.field-web-url-editor {
  width: 100%;
}
:deep(.arco-input) {
  font-size: 13px;
  padding: 2px 6px;
}
</style>
