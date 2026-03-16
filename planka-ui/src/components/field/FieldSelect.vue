<template>
  <a-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :loading="loading"
    allow-clear
    @update:model-value="handleChange"
  >
    <a-option
      v-for="field in fieldOptions"
      :key="field.id"
      :value="field.id"
    >
      {{ field.name }}
    </a-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { fieldOptionsApi } from '@/api'
import type { FieldOption } from '@/types/field-option'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    cardTypeIds?: string[]
    linkFieldId?: string
    fieldTypes?: string[]
    placeholder?: string
  }>(),
  {
    placeholder: '请选择字段',
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | undefined): void
}>()

const loading = ref(false)
const fieldOptions = ref<FieldOption[]>([])

function handleChange(value: string | undefined) {
  emit('update:modelValue', value)
}

async function loadFields() {
  loading.value = true
  try {
    let fields: FieldOption[] = []

    if (props.linkFieldId) {
      // 根据关联属性ID获取字段选项
      fields = await fieldOptionsApi.getFieldsByLinkFieldId(props.linkFieldId)
    } else if (props.cardTypeIds && props.cardTypeIds.length > 0) {
      // 获取共同字段（单个或多个卡片类型都使用此方法）
      const response = await fieldOptionsApi.getCommonFields(props.cardTypeIds, props.fieldTypes)
      fields = response.fields
    } else {
      // 没有卡片类型ID，返回空列表
      fields = []
    }

    fieldOptions.value = fields
  } catch (error) {
    console.error('Failed to load fields:', error)
    fieldOptions.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.cardTypeIds, props.linkFieldId, props.fieldTypes],
  () => {
    if (props.linkFieldId || (props.cardTypeIds && props.cardTypeIds.length > 0)) {
      loadFields()
    } else {
      fieldOptions.value = []
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadFields()
})
</script>
