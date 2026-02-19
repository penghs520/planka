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
      v-if="streamOption"
      :key="streamOption.id"
      :value="streamOption.id"
    >
      {{ streamOption.name }}
    </a-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { valueStreamApi } from '@/api'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    cardTypeIds?: string[]
    placeholder?: string
  }>(),
  {
    placeholder: '请选择价值流',
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | undefined): void
}>()

const loading = ref(false)
const streamOption = ref<{ id: string; name: string } | null>(null)

const cardTypeId = computed(() => {
  return props.cardTypeIds && props.cardTypeIds.length > 0 ? props.cardTypeIds[0] : undefined
})

function handleChange(value: string | undefined) {
  emit('update:modelValue', value)
}

async function loadStream() {
  if (!cardTypeId.value) {
    streamOption.value = null
    return
  }

  loading.value = true
  try {
    const baseline = await valueStreamApi.getByCardType(cardTypeId.value)
    if (baseline) {
      streamOption.value = {
        id: baseline.id || '',
        name: baseline.name || '',
      }
      // 如果当前没有选中值，自动选中
      if (!props.modelValue && baseline.id) {
        emit('update:modelValue', baseline.id)
      }
    } else {
      streamOption.value = null
    }
  } catch (error) {
    console.error('Failed to load value stream:', error)
    streamOption.value = null
  } finally {
    loading.value = false
  }
}

watch(
  () => cardTypeId.value,
  () => {
    loadStream()
  },
  { immediate: true }
)

onMounted(() => {
  loadStream()
})
</script>
