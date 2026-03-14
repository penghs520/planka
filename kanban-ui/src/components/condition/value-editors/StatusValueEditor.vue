<template>
  <a-select
    :model-value="statusValue"
    size="small"
    :placeholder="t('common.statusValue.selectOption')"
    :multiple="isMultiple"
    :allow-clear="isMultiple"
    :class="{ 'tag-select': isMultiple }"
    @change="handleStatusValueChange"
    @focus="handleFocus"
  >
    <a-option
      v-for="option in displayOptions"
      :key="option.id"
      :value="option.id"
    >
      {{ option.name }}
    </a-option>
  </a-select>
</template>

<script setup lang="ts">
import { computed, inject, ref, unref, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import type { StatusOperator } from '@/types/condition'
import type { StatusOption } from '@/api/value-stream'
import { valueStreamBranchApi } from '@/api/value-stream'
import { CONDITION_DISPLAY_INFO_KEY } from '../useConditionDisplayInfo'

const { t } = useI18n()

/**
 * Props定义
 */
const props = defineProps<{
  /** 状态操作符 */
  modelValue: StatusOperator
  /** 价值流ID */
  streamId: string
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: StatusOperator]
  'update:streamId': [streamId: string]
}>()

/**
 * 从父组件注入卡片类型ID（可能是 ComputedRef 或字符串）
 */
const injectedCardTypeId = inject<ComputedRef<string> | string>('cardTypeId', '')
const cardTypeId = computed(() => unref(injectedCardTypeId))

/**
 * 注入条件显示信息上下文
 */
const displayInfo = inject(CONDITION_DISPLAY_INFO_KEY, null as any)

/**
 * 状态选项列表
 */
const options = ref<StatusOption[]>([])

/**
 * 是否已加载选项
 */
const optionsLoaded = ref(false)

/**
 * 是否为多选操作符
 */
const isMultiple = computed(() => {
  const type = props.modelValue.type
  return type === 'IN' || type === 'NOT_IN'
})

/**
 * 状态值
 */
const statusValue = computed(() => {
  const op = props.modelValue as any
  if (isMultiple.value) {
    return op.statusIds || []
  }
  return op.statusId || ''
})

/**
 * 显示的选项列表（合并API选项和已选状态的Display-info名称）
 */
const displayOptions = computed(() => {
  const selectedIds = Array.isArray(statusValue.value) ? statusValue.value : [statusValue.value].filter(Boolean)

  // 构建选项列表
  const result = [...options.value]

  // 添加已选但不在API选项中的状态（从Display-info获取名称）
  for (const statusId of selectedIds) {
    if (!result.some(opt => opt.id === statusId)) {
      const name = displayInfo?.getStatusName?.(statusId) || statusId
      result.push({
        id: statusId,
        name,
        stepKind: '',
        streamId: ''
      })
    }
  }

  return result
})

/**
 * 处理状态值变化
 */
function handleStatusValueChange(value: string | string[]) {
  const newOp = { ...props.modelValue } as any
  if (Array.isArray(value)) {
    newOp.statusIds = value
    delete newOp.statusId
  } else {
    newOp.statusId = value
    delete newOp.statusIds
  }
  emit('update:modelValue', newOp)

  // 从选中的状态选项中获取 streamId 并通知父组件更新
  const selectedId = Array.isArray(value) ? value[0] : value
  if (selectedId && options.value.length > 0) {
    const selectedOption = options.value.find(opt => opt.id === selectedId)
    if (selectedOption?.streamId) {
      emit('update:streamId', selectedOption.streamId)
    }
  }
}

/**
 * 处理下拉框获得焦点（懒加载选项）
 */
async function handleFocus() {
  if (!optionsLoaded.value) {
    await loadStatusOptions()
  }
}

/**
 * 加载状态选项
 */
async function loadStatusOptions() {
  if (!cardTypeId.value) {
    options.value = []
    return
  }
  try {
    options.value = await valueStreamBranchApi.getStatusOptions(cardTypeId.value)
    optionsLoaded.value = true
  } catch (error) {
    console.error('Failed to load status options:', error)
    options.value = []
  }
}
</script>

<style lang="scss">
@import './tag-select.scss';
</style>

