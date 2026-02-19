<script setup lang="ts">
/**
 * 枚举字段编辑组件
 */
import { computed } from 'vue'
import type { EnumRenderConfig } from '@/types/view-data'

const props = defineProps<{
  renderConfig?: EnumRenderConfig | null
  placeholder?: string
  disabled?: boolean
}>()

const modelValue = defineModel<string[]>({ required: false, default: () => [] })

const emit = defineEmits<{
  save: []
  'popup-visible-change': [visible: boolean]
}>()

/** 获取枚举选项 */
const enumOptions = computed(() => {
  return props.renderConfig?.options || []
})

/** 是否多选枚举 */
const enumMultiSelect = computed(() => {
  return props.renderConfig?.multiSelect ?? false
})

/** 内部选择器的值（单选时为字符串，多选时为数组） */
const internalValue = computed({
  get() {
    if (enumMultiSelect.value) {
      return modelValue.value
    } else {
      // 单选模式：返回数组的第一个元素（字符串）
      return modelValue.value?.[0] ?? undefined
    }
  },
  set(value: string | string[] | undefined) {
    if (enumMultiSelect.value) {
      // 多选模式：直接赋值数组
      modelValue.value = Array.isArray(value) ? value : []
    } else {
      // 单选模式：将字符串转换为数组
      modelValue.value = value && typeof value === 'string' ? [value] : []
    }
  }
})

/** 获取已选中选项的样式 */
function getSelectedOptionStyle(optionId: string): Record<string, string> {
  const option = enumOptions.value.find(opt => opt.id === optionId)
  if (option?.color) {
    return { backgroundColor: option.color }
  }
  return {}
}

function handleChange() {
  // 统一在下拉框关闭时保存，或者 change 时不保存？
  // CellEditor 里是: handleEnumChange 留空, handleEnumPopupVisibleChange 保存
  // 这里我们只 emit change 事件，由 dispatcher 或 parent 决定何时保存?
  // 但 FieldDateEditor emit 'save'. 为了统一，我们也 emit save
  // 不过对于多选，每次勾选都 save 可能太频繁，通常是收起时 save.
  // 但单选 change 即 save.
}

function handlePopupVisibleChange(visible: boolean) {
  emit('popup-visible-change', visible)
  if (!visible) {
    emit('save')
  }
}
</script>

<template>
  <a-select
    v-model="internalValue"
    class="field-enum-editor"
    size="mini"
    :placeholder="placeholder"
    :disabled="disabled"
    :multiple="enumMultiSelect"
    allow-clear
    :default-popup-visible="true"
    @change="handleChange"
    @popup-visible-change="handlePopupVisibleChange"
  >
    <a-option
      v-for="opt in enumOptions"
      :key="opt.id"
      :value="opt.id"
      :label="opt.label"
    >
      <span
        class="enum-tag"
        :style="opt.color ? { backgroundColor: opt.color } : {}"
      >
        {{ opt.label }}
      </span>
    </a-option>
    <!-- 自定义已选中标签的显示 -->
    <template #label="{ data }">
      <span
        class="enum-tag"
        :style="getSelectedOptionStyle(data.value)"
      >
        {{ data.label }}
      </span>
    </template>
  </a-select>
</template>

<style scoped lang="scss">
// 枚举标签样式
.enum-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  background-color: var(--color-fill-2);
  color: #333 !important;
}

// 枚举选择器样式调整
.field-enum-editor {
  width: 100%;
  
  :deep(.arco-select-view-tag) {
    background: transparent;
    padding: 0;
    margin: 1px 2px;

    .arco-tag {
      background: transparent;
      padding: 0;
    }
  }

  :deep(.arco-select-view-value) {
    font-size: 13px;

    .enum-tag {
      margin-right: 4px;
    }
  }
}
</style>
