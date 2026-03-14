<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { supportsFormula } from '../../formatters'
import type { ValueSourceType } from '@/types/card-type'
import ToggleButtonGroup from '@/components/common/ToggleButtonGroup.vue'

interface Props {
  modelValue: ValueSourceType
  schemaSubType: string
  disabled?: boolean
  referenceFieldId?: string
  referenceFieldOptions?: Array<{ id: string; name: string }>
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  referenceFieldOptions: () => [],
})

const emit = defineEmits<{
  'update:modelValue': [value: ValueSourceType]
  'update:referenceFieldId': [value: string | undefined]
}>()

const { t } = useI18n()

const valueSource = computed({
  get: () => props.modelValue,
  set: (value: ValueSourceType) => emit('update:modelValue', value),
})

const showFormulaOption = computed(() => supportsFormula(props.schemaSubType))

const isSystemSource = computed(() => props.modelValue === 'SYSTEM')

const sourceOptions = computed(() => {
  const options = [
    { label: t('admin.cardType.fieldConfig.valueSourceManual'), value: 'MANUAL' as ValueSourceType },
  ]
  if (showFormulaOption.value) {
    options.push({ label: t('admin.cardType.fieldConfig.valueSourceFormula'), value: 'FORMULA' as ValueSourceType })
  }
  options.push({ label: t('admin.cardType.fieldConfig.valueSourceReference'), value: 'REFERENCE' as ValueSourceType })
  return options
})
</script>

<template>
  <div class="value-source-config">
    <div class="config-label">{{ t('admin.cardType.fieldConfig.valueSource') }}</div>

    <!-- 系统更新值来源只读显示 -->
    <div v-if="isSystemSource" class="system-source-text">
      {{ t('admin.cardType.fieldConfig.valueSourceSystem') }}
    </div>

    <!-- 值来源选择 -->
    <ToggleButtonGroup
      v-else
      v-model="valueSource"
      :options="sourceOptions"
      :disabled="disabled"
    />

    <!-- 公式提示 -->
    <div v-if="valueSource === 'FORMULA'" class="formula-notice">
      {{ t('admin.cardType.fieldConfig.formulaNotAvailable') }}
    </div>

    <!-- 引用配置 -->
    <div v-if="valueSource === 'REFERENCE'" class="reference-config">
      <div class="config-label">{{ t('admin.cardType.fieldConfig.referenceField') }}</div>
      <a-select
        :model-value="referenceFieldId"
        :placeholder="t('admin.cardType.fieldConfig.selectReferenceField')"
        :disabled="disabled"
        allow-clear
        class="reference-select"
        @update:model-value="(val: string) => emit('update:referenceFieldId', val)"
      >
        <a-option v-for="field in referenceFieldOptions" :key="field.id" :value="field.id">
          {{ field.name }}
        </a-option>
      </a-select>
    </div>
  </div>
</template>

<style scoped>
.value-source-config {
  margin-top: 20px;
}

.config-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
  margin-bottom: 8px;
}

.system-source-text {
  font-size: 13px;
  color: var(--color-text-2);
}

.formula-notice {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-3);
}

.reference-config {
  margin-top: 16px;
  max-width: 320px;
}

.reference-select {
  width: 100%;
}
</style>
