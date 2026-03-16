<template>
  <div class="date-value-editor">
    <!-- 栅格布局 -->
    <a-row :gutter="8" class="date-value-row">
      <!-- 主选择器（值类型选择）- 始终存在 -->
      <a-col :span="mainSelectSpan">
        <a-select
          :model-value="displayValue"
          size="small"
          style="width: 100%"
          @change="handleSelectChange"
        >
          <!-- 指定日期 -->
          <a-option value="__SPECIFIC__">{{ t('common.dateValue.specificDate') }}</a-option>

          <!-- 关键日期选项 -->
          <a-option v-for="key in keyDateKeys" :key="key" :value="key">
            {{ t(`common.dateValue.${key}`) }}
          </a-option>

          <!-- 动态时间范围 -->
          <a-option value="__RECENT__">{{ t('common.dateValue.recent') }}</a-option>
          <a-option value="__FUTURE__">{{ t('common.dateValue.future') }}</a-option>
        </a-select>
      </a-col>

      <!-- 数量输入（最近/未来时显示） -->
      <a-col v-if="showTimeRangeInput" :span="amountSpan">
        <a-input-number
          v-model="timeRangeAmount"
          size="small"
          :min="1"
          :max="999"
          style="width: 100%"
          @change="handleTimeRangeChange"
        />
      </a-col>

      <!-- 单位选择（最近/未来时显示） -->
      <a-col v-if="showTimeRangeInput" :span="unitSpan">
        <a-select
          v-model="timeRangeUnit"
          size="small"
          style="width: 100%"
          @change="handleTimeRangeChange"
        >
          <a-option v-for="unit in timeUnits" :key="unit" :value="unit">
            {{ t(`common.timeUnit.${unit}`) }}
          </a-option>
        </a-select>
      </a-col>

      <!-- 具体日期选择器（指定日期时显示在同一行） -->
      <a-col v-if="showDatePicker" :span="datePickerSpan">
        <a-date-picker
          :model-value="specificDateValue"
          size="small"
          style="width: 100%"
          @change="handleSpecificDateChange"
        />
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { DateOperator } from '@/types/condition'
import { KeyDate, TimeUnit } from '@/types/condition'

const { t } = useI18n()

// 所有关键日期的 key
const keyDateKeys = Object.values(KeyDate)

// 所有时间单位
const timeUnits = Object.values(TimeUnit)

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

// 控制日期选择器显示
const showDatePicker = ref(false)

// 控制时间范围输入显示
const showTimeRangeInput = ref(false)

// 时间范围数量
const timeRangeAmount = ref(1)

// 时间范围单位
const timeRangeUnit = ref<TimeUnit>(TimeUnit.DAY)

// 当前值类型
const currentValueType = ref<string>('')

/**
 * 栅格分配：值区域总宽度为 24（使用 a-row/a-col 的标准）
 * 注意：这个组件被放在 ConditionItemEditor 的 10/24 宽度的值区域内
 * 所以这里的 24 是指值区域内部的相对宽度
 *
 * 栏数分配：
 * - 单栏（关键日期）：主选择器占满 24
 * - 三栏（最近/未来）：主选择器 10 + 数量 7 + 单位 7
 * - 双栏（指定日期）：主选择器 8 + 日期选择器 16
 */
const mainSelectSpan = computed(() => {
  if (showTimeRangeInput.value) {
    return 10 // 最近/未来时，主选择器占 10
  }
  if (showDatePicker.value) {
    return 8 // 指定日期时，主选择器占 8
  }
  return 24 // 关键日期时占满
})

const amountSpan = 7
const unitSpan = 7
const datePickerSpan = 16

/**
 * 显示值（下拉框中显示的内容）
 */
const displayValue = computed(() => {
  const op = props.modelValue as any
  const valueType = op.value?.type

  if (valueType === 'SPECIFIC') {
    return '__SPECIFIC__'
  }
  if (valueType === 'KEY_DATE') {
    return op.value?.keyDate || KeyDate.TODAY
  }
  if (valueType === 'RECENT') {
    return '__RECENT__'
  }
  if (valueType === 'FUTURE') {
    return '__FUTURE__'
  }

  // 默认值
  return KeyDate.TODAY
})

/**
 * 具体日期值
 */
const specificDateValue = computed(() => {
  const op = props.modelValue as any
  return op.value?.value || ''
})

/**
 * 监听值变化，同步时间范围输入状态
 */
watch(() => props.modelValue, (newVal) => {
  const op = newVal as any
  const valueType = op.value?.type

  // 同步时间范围输入显示状态和值
  if (valueType === 'RECENT' || valueType === 'FUTURE') {
    showTimeRangeInput.value = true
    timeRangeAmount.value = op.value?.amount || 1
    timeRangeUnit.value = op.value?.unit || TimeUnit.DAY
    showDatePicker.value = false
  } else if (valueType === 'SPECIFIC') {
    showDatePicker.value = true
    showTimeRangeInput.value = false
  } else {
    showDatePicker.value = false
    showTimeRangeInput.value = false
  }

  currentValueType.value = valueType
}, { immediate: true })

/**
 * 处理选择变更
 */
function handleSelectChange(value: string) {
  const newOp = { ...props.modelValue } as any

  if (value === '__SPECIFIC__') {
    newOp.value = { type: 'SPECIFIC', value: '' }
    showDatePicker.value = true
    showTimeRangeInput.value = false
  } else if (value === '__RECENT__') {
    newOp.value = { type: 'RECENT', amount: 1, unit: TimeUnit.DAY }
    showDatePicker.value = false
    showTimeRangeInput.value = true
    timeRangeAmount.value = 1
    timeRangeUnit.value = TimeUnit.DAY
  } else if (value === '__FUTURE__') {
    newOp.value = { type: 'FUTURE', amount: 1, unit: TimeUnit.DAY }
    showDatePicker.value = false
    showTimeRangeInput.value = true
    timeRangeAmount.value = 1
    timeRangeUnit.value = TimeUnit.DAY
  } else {
    // 关键日期
    newOp.value = { type: 'KEY_DATE', keyDate: value as KeyDate }
    showDatePicker.value = false
    showTimeRangeInput.value = false
  }

  currentValueType.value = newOp.value.type
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
 * 处理时间范围变化
 */
function handleTimeRangeChange() {
  const newOp = { ...props.modelValue } as any
  const valueType = newOp.value?.type

  if (valueType === 'RECENT' || valueType === 'FUTURE') {
    newOp.value = {
      type: valueType,
      amount: timeRangeAmount.value,
      unit: timeRangeUnit.value
    }
    emit('update:modelValue', newOp)
  }
}
</script>

<style scoped>
.date-value-editor {
  width: 100%;
}

.date-value-row {
  display: flex;
  align-items: center;
}
</style>
