<template>
  <div ref="wrapperRef" class="parameter-input-wrapper">
    <a-input
      v-if="!multiline"
      ref="inputRef"
      :model-value="modelValue"
      :placeholder="placeholder"
      @input="handleInput"
      @keydown="handleKeydown"
      @blur="handleBlur"
    />
    <a-textarea
      v-else
      ref="textareaRef"
      :model-value="modelValue"
      :placeholder="placeholder"
      :auto-size="autoSize"
      @input="handleInput"
      @keydown="handleKeydown"
      @blur="handleBlur"
    />

    <!-- 参数建议下拉框 -->
    <div
      v-if="showSuggestions && filteredParameters.length > 0"
      class="parameter-suggestions"
      :style="dropdownStyle"
    >
      <div
        v-for="(param, index) in filteredParameters"
        :key="param.value"
        class="parameter-item"
        :class="{ active: index === selectedIndex }"
        @mousedown.prevent="selectParameter(param)"
        @mouseenter="selectedIndex = index"
      >
        <span class="parameter-label">{{ param.label }}</span>
        <span class="parameter-type">{{ param.type }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'

export interface ParameterOption {
  label: string
  value: string
  type?: string
}

interface Props {
  modelValue: string
  parameters: ParameterOption[]
  placeholder?: string
  multiline?: boolean
  autoSize?: { minRows?: number; maxRows?: number }
  triggerChar?: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '',
  multiline: false,
  triggerChar: '#',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const wrapperRef = ref<HTMLElement>()
const inputRef = ref<any>()
const textareaRef = ref<any>()
const showSuggestions = ref(false)
const query = ref('')
const selectedIndex = ref(0)
const triggerPos = ref(-1)
const dropdownStyle = ref<Record<string, string>>({})

// 过滤后的参数列表
const filteredParameters = computed(() => {
  if (!query.value) return props.parameters
  const lowerQuery = query.value.toLowerCase()
  return props.parameters.filter(p =>
    p.label.toLowerCase().includes(lowerQuery) ||
    p.value.toLowerCase().includes(lowerQuery)
  )
})

// 获取当前输入框元素
const getInputElement = (): HTMLInputElement | HTMLTextAreaElement | null => {
  if (props.multiline) {
    return textareaRef.value?.$el?.querySelector('textarea') || null
  }
  return inputRef.value?.$el?.querySelector('input') || null
}

// 处理输入
const handleInput = (value: string) => {
  emit('update:modelValue', value)

  const inputEl = getInputElement()
  if (!inputEl) return

  const cursorPos = inputEl.selectionStart || 0
  const textBeforeCursor = value.substring(0, cursorPos)

  // 查找最后一个触发字符
  const lastTriggerIndex = textBeforeCursor.lastIndexOf(props.triggerChar)

  if (lastTriggerIndex !== -1) {
    const textAfterTrigger = textBeforeCursor.substring(lastTriggerIndex + 1)
    // 如果触发字符后没有空格，显示建议
    if (!/\s/.test(textAfterTrigger)) {
      triggerPos.value = lastTriggerIndex
      query.value = textAfterTrigger
      showSuggestions.value = true
      selectedIndex.value = 0
      updateDropdownPosition()
      return
    }
  }

  showSuggestions.value = false
}

// 更新下拉框位置
const updateDropdownPosition = () => {
  nextTick(() => {
    const inputEl = getInputElement()
    if (!inputEl || !wrapperRef.value) return

    // 简单定位：在输入框下方
    dropdownStyle.value = {
      top: `${inputEl.offsetHeight + 4}px`,
      left: '0px',
    }
  })
}

// 处理键盘事件
const handleKeydown = (e: KeyboardEvent) => {
  if (!showSuggestions.value) return

  switch (e.key) {
    case 'ArrowDown':
      e.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, filteredParameters.value.length - 1)
      break
    case 'ArrowUp':
      e.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
      break
    case 'Enter':
      if (filteredParameters.value.length > 0) {
        e.preventDefault()
        const param = filteredParameters.value[selectedIndex.value]
        if (param) {
          selectParameter(param)
        }
      }
      break
    case 'Escape':
      e.preventDefault()
      showSuggestions.value = false
      break
  }
}

// 选择参数
const selectParameter = (param: ParameterOption) => {
  const inputEl = getInputElement()
  if (!inputEl) return

  const currentValue = props.modelValue
  const cursorPos = inputEl.selectionStart || 0

  // 替换 # 和查询文本为参数
  const beforeTrigger = currentValue.substring(0, triggerPos.value)
  const afterCursor = currentValue.substring(cursorPos)
  const newValue = `${beforeTrigger}\${${param.value}}${afterCursor}`

  emit('update:modelValue', newValue)
  showSuggestions.value = false

  // 恢复焦点并设置光标位置
  nextTick(() => {
    inputEl.focus()
    const newCursorPos = beforeTrigger.length + param.value.length + 3 // ${} 的长度
    inputEl.setSelectionRange(newCursorPos, newCursorPos)
  })
}

// 失焦时隐藏建议
const handleBlur = () => {
  // 延迟隐藏，以便点击事件能够触发
  setTimeout(() => {
    showSuggestions.value = false
  }, 200)
}

// 监听参数变化，重置选中索引
watch(() => filteredParameters.value, () => {
  selectedIndex.value = 0
})
</script>

<style scoped>
.parameter-input-wrapper {
  position: relative;
  width: 100%;
}

.parameter-suggestions {
  position: absolute;
  z-index: 1000;
  background: var(--color-bg-popup);
  border: 1px solid var(--color-border-2);
  border-radius: var(--radius-md);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-height: 240px;
  overflow-y: auto;
  min-width: 200px;
}

.parameter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.parameter-item:hover,
.parameter-item.active {
  background-color: var(--color-fill-2);
}

.parameter-label {
  font-size: 14px;
  color: var(--color-text-1);
}

.parameter-type {
  font-size: 12px;
  color: var(--color-text-3);
  margin-left: 8px;
}
</style>
