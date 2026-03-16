<template>
  <a-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :loading="loading"
    :disabled="!cardTypeId"
    allow-clear
    @update:model-value="handleChange"
  >
    <a-option
      v-for="status in statusOptions"
      :key="status.id"
      :value="status.id"
    >
      {{ status.name }}
    </a-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { valueStreamBranchApi } from '@/api'
import { schemaApi } from '@/api/schema'
import type { StatusOption } from '@/api/value-stream'
import type { ValueStreamDefinition } from '@/types/value-stream'
import { SchemaSubType } from '@/types/schema'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    streamId?: string
    cardTypeId?: string
    placeholder?: string
  }>(),
  {
    placeholder: '请选择状态',
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | undefined): void
}>()

const loading = ref(false)
const statusOptions = ref<StatusOption[]>([])
const resolvedCardTypeId = ref<string | undefined>(undefined)

function handleChange(value: string | undefined) {
  emit('update:modelValue', value)
}

// 计算实际使用的 cardTypeId
const effectiveCardTypeId = computed(() => {
  return props.cardTypeId || resolvedCardTypeId.value
})

async function loadStatuses() {
  const cardTypeId = effectiveCardTypeId.value
  if (!cardTypeId) {
    statusOptions.value = []
    return
  }

  loading.value = true
  try {
    const options = await valueStreamBranchApi.getStatusOptions(cardTypeId)
    statusOptions.value = options
  } catch (error) {
    console.error('Failed to load statuses:', error)
    statusOptions.value = []
  } finally {
    loading.value = false
  }
}

// 如果提供了 streamId 但没有 cardTypeId，则通过 streamId 获取价值流定义
async function resolveCardTypeIdFromStream() {
  if (props.cardTypeId || !props.streamId) {
    resolvedCardTypeId.value = undefined
    return
  }

  loading.value = true
  try {
    const stream = await schemaApi.getById<ValueStreamDefinition>(props.streamId)
    if (stream && stream.schemaSubType === SchemaSubType.VALUE_STREAM) {
      resolvedCardTypeId.value = stream.cardTypeId
    }
  } catch (error) {
    console.error('Failed to resolve card type from stream:', error)
    resolvedCardTypeId.value = undefined
  } finally {
    loading.value = false
  }
}

watch(
  () => props.streamId,
  () => {
    resolveCardTypeIdFromStream()
  },
  { immediate: true }
)

watch(
  () => effectiveCardTypeId.value,
  () => {
    loadStatuses()
  },
  { immediate: true }
)
</script>
