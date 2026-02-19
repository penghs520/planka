<template>
  <a-input
    :model-value="userValue"
    :placeholder="t('common.userValue.inputUserId')"
    size="small"
    @update:model-value="handleUserValueChange"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { UserOperator } from '@/types/condition'

const { t } = useI18n()

/**
 * Props定义
 */
const props = defineProps<{
  /** 用户操作符 */
  modelValue: UserOperator
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: UserOperator]
}>()

/**
 * 用户值（用于显示）
 */
const userValue = computed(() => {
  const op = props.modelValue as any
  const opType = op.type

  if (opType === 'EQ' || opType === 'NE') {
    return op.userId || ''
  } else if (opType === 'IN' || opType === 'NOT_IN') {
    return (op.userIds || []).join(', ')
  }
  return ''
})

/**
 * 处理用户值变化
 */
function handleUserValueChange(value: string) {
  const newOp = { ...props.modelValue } as any
  const opType = newOp.type

  if (opType === 'EQ' || opType === 'NE') {
    newOp.userId = value.trim()
  } else if (opType === 'IN' || opType === 'NOT_IN') {
    newOp.userIds = value
      .split(',')
      .map((id) => id.trim())
      .filter((id) => id.length > 0)
  }
  emit('update:modelValue', newOp)
}
</script>
