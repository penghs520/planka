<template>
  <a-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :loading="loading"
    allow-clear
    @update:model-value="handleChange"
  >
    <a-option
      v-for="option in linkFieldOptions"
      :key="option.value"
      :value="option.value"
    >
      {{ option.label }}
    </a-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { linkTypeApi } from '@/api'
import { buildLinkFieldId } from '@/utils/link-field-utils'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    cardTypeIds?: string[]
    placeholder?: string
  }>(),
  {
    placeholder: '请选择关联属性',
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | undefined): void
}>()

const loading = ref(false)
const linkFieldOptions = ref<Array<{ value: string; label: string }>>([])

function handleChange(value: string | undefined) {
  emit('update:modelValue', value)
}

async function loadLinkFields() {
  if (!props.cardTypeIds || props.cardTypeIds.length === 0) {
    linkFieldOptions.value = []
    return
  }

  // 使用第一个卡片类型获取关联类型
  const cardTypeId = props.cardTypeIds[0]
  if (!cardTypeId) {
    linkFieldOptions.value = []
    return
  }

  loading.value = true
  try {
    const linkTypes = await linkTypeApi.getAvailableForCardType(cardTypeId)
    
    // 构建 LinkFieldId 选项
    const options: Array<{ value: string; label: string }> = []
    
    for (const linkType of linkTypes) {
      // SOURCE 位置
      const sourceLinkFieldId = buildLinkFieldId(linkType.id, 'SOURCE')
      options.push({
        value: sourceLinkFieldId,
        label: `${linkType.name} (SOURCE)`,
      })
      
      // TARGET 位置
      const targetLinkFieldId = buildLinkFieldId(linkType.id, 'TARGET')
      options.push({
        value: targetLinkFieldId,
        label: `${linkType.name} (TARGET)`,
      })
    }
    
    linkFieldOptions.value = options
  } catch (error) {
    console.error('Failed to load link fields:', error)
    linkFieldOptions.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => props.cardTypeIds,
  () => {
    loadLinkFields()
  },
  { immediate: true }
)

onMounted(() => {
  loadLinkFields()
})
</script>
