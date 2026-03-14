<template>
  <a-select
    :model-value="lifecycleValue"
    size="small"
    multiple
    allow-clear
    class="tag-select"
    @change="handleLifecycleValueChange"
  >
    <a-option v-for="state in lifecycleStates" :key="state" :value="state">
      {{ t(`common.lifecycleState.${state}`) }}
    </a-option>
  </a-select>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { LifecycleOperator } from '@/types/condition'
import { LifecycleState } from '@/types/condition'

const { t } = useI18n()

// 所有生命周期状态
const lifecycleStates = Object.values(LifecycleState)

/**
 * Props定义
 */
const props = defineProps<{
  /** 生命周期操作符 */
  modelValue: LifecycleOperator
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: LifecycleOperator]
}>()

/**
 * 生命周期状态值
 */
const lifecycleValue = computed(() => {
  return props.modelValue.values || []
})

/**
 * 处理生命周期状态值变化
 */
function handleLifecycleValueChange(value: LifecycleState[]) {
  const newOp: LifecycleOperator = {
    ...props.modelValue,
    values: value,
  }
  emit('update:modelValue', newOp)
}
</script>

<style scoped>
.tag-select {
  width: 100%;
}
</style>

<style lang="scss">
@import './tag-select.scss';
</style>
