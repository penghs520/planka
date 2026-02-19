<template>
  <a-row :gutter="8">
    <a-col :span="8">
      <a-select
        :model-value="dateValueType"
        size="small"
        @change="handleDateTypeChange"
      >
        <a-option value="SPECIFIC">{{ t('common.dateValue.specificDate') }}</a-option>
        <a-option value="KEY_DATE">{{ t('common.dateValue.keyDate') }}</a-option>
      </a-select>
    </a-col>
    <a-col :span="16">
      <!-- 具体日期 -->
      <a-date-picker
        v-if="dateValueType === 'SPECIFIC'"
        :model-value="specificDateValue"
        size="small"
        style="width: 100%"
        show-time
        @change="handleSpecificDateChange"
      />
      <!-- 关键日期 -->
      <a-select
        v-else
        :model-value="keyDateValue"
        size="small"
        @change="handleKeyDateChange"
      >
        <a-option v-for="key in keyDateKeys" :key="key" :value="key">
          {{ t(`common.dateValue.${key}`) }}
        </a-option>
      </a-select>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { DateOperator } from '@/types/condition'
import { KeyDate } from '@/types/condition'

const { t } = useI18n()

// 所有关键日期的 key
const keyDateKeys = Object.values(KeyDate)

/**
 * Props定义
 */
const props = defineProps<{
  /** 日期操作符 */
  modelValue: DateOperator
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: DateOperator]
}>()

/**
 * 日期值类型
 */
const dateValueType = computed(() => {
  const op = props.modelValue as any
  return op.value?.type || 'KEY_DATE'
})

/**
 * 具体日期值
 */
const specificDateValue = computed(() => {
  const op = props.modelValue as any
  return op.value?.value || ''
})

/**
 * 关键日期值
 */
const keyDateValue = computed(() => {
  const op = props.modelValue as any
  return op.value?.keyDate || KeyDate.TODAY
})

/**
 * 处理日期类型切换
 */
function handleDateTypeChange(type: string) {
  const newOp = { ...props.modelValue } as any
  if (type === 'SPECIFIC') {
    newOp.value = { type: 'SPECIFIC', value: '' }
  } else {
    newOp.value = { type: 'KEY_DATE', keyDate: KeyDate.TODAY }
  }
  emit('update:modelValue', newOp)
}

/**
 * 处理具体日期变化
 */
function handleSpecificDateChange(dateString: string | undefined) {
  const newOp = { ...props.modelValue } as any
  newOp.value = { type: 'SPECIFIC', value: dateString || '' }
  emit('update:modelValue', newOp)
}

/**
 * 处理关键日期变化
 */
function handleKeyDateChange(keyDate: KeyDate) {
  const newOp = { ...props.modelValue } as any
  newOp.value = { type: 'KEY_DATE', keyDate }
  emit('update:modelValue', newOp)
}
</script>
