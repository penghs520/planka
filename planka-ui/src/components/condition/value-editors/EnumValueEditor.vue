<template>
  <a-select
    :model-value="enumValue"
    size="small"
    :placeholder="t('common.enumValue.selectOption')"
    :multiple="isMultiple"
    :allow-clear="isMultiple"
    :class="{ 'tag-select': isMultiple }"
    @change="handleEnumValueChange"
  >
    <a-option
      v-for="option in options"
      :key="option.id"
      :value="option.id"
    >
      {{ option.label }}
    </a-option>
  </a-select>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EnumOperator } from '@/types/condition'
import type { EnumOptionDTO } from '@/types/view-data'

const { t } = useI18n()

/**
 * Props定义
 */
const props = defineProps<{
  /** 枚举操作符 */
  modelValue: EnumOperator
  /** 枚举选项列表 */
  options: EnumOptionDTO[]
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: EnumOperator]
}>()

/**
 * 是否为多选操作符
 */
const isMultiple = computed(() => {
  const type = props.modelValue.type
  return type === 'IN' || type === 'NOT_IN'
})

/**
 * 枚举值
 */
const enumValue = computed(() => {
  const op = props.modelValue as any
  if (isMultiple.value) {
    return op.optionIds || []
  }
  return op.optionId || ''
})

/**
 * 处理枚举值变化
 */
function handleEnumValueChange(value: string | string[]) {
  const newOp = { ...props.modelValue } as any
  if (Array.isArray(value)) {
    newOp.optionIds = value
    delete newOp.optionId
  } else {
    newOp.optionId = value
    delete newOp.optionIds
  }
  emit('update:modelValue', newOp)
}
</script>

<style lang="scss">
@import './tag-select.scss';
</style>
