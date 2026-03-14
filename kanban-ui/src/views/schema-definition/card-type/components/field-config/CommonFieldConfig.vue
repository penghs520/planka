<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ToggleButtonGroup from '@/components/common/ToggleButtonGroup.vue'

interface Props {
  required: boolean
  readOnly: boolean
  disabled?: boolean
  /** 当值来源不是手动输入时，自动禁用这些配置 */
  valueSourceIsManual?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  valueSourceIsManual: true,
})

const emit = defineEmits<{
  'update:required': [value: boolean]
  'update:readOnly': [value: boolean]
}>()

const { t } = useI18n()

const requiredValue = computed({
  get: () => props.required,
  set: (value: boolean) => emit('update:required', value),
})

const editableValue = computed({
  get: () => !props.readOnly,
  set: (value: boolean) => emit('update:readOnly', !value),
})

const isDisabled = computed(() => props.disabled || !props.valueSourceIsManual)

const requiredOptions = computed(() => [
  { label: t('admin.cardType.fieldConfig.notRequired'), value: false },
  { label: t('admin.cardType.fieldConfig.required'), value: true },
])

const editableOptions = computed(() => [
  { label: t('admin.cardType.fieldConfig.editable'), value: true },
  { label: t('admin.cardType.fieldConfig.notEditable'), value: false },
])
</script>

<template>
  <div class="common-field-config">
    <div class="config-row">
      <div class="config-item">
        <div class="config-label">{{ t('admin.cardType.fieldConfig.isRequired') }}</div>
        <ToggleButtonGroup
          v-model="requiredValue"
          :options="requiredOptions"
          :disabled="isDisabled"
        />
      </div>
      <div class="config-item">
        <div class="config-label">{{ t('admin.cardType.fieldConfig.isEditable') }}</div>
        <ToggleButtonGroup
          v-model="editableValue"
          :options="editableOptions"
          :disabled="isDisabled"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.common-field-config {
  margin-top: 20px;
}

.config-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.config-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.config-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
  letter-spacing: 0.01em;
}
</style>
