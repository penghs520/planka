<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

interface Props {
  maxLength?: number
  defaultValue?: string
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
})

const emit = defineEmits<{
  'update:maxLength': [value: number | undefined]
  'update:defaultValue': [value: string]
}>()

const { t } = useI18n()

const maxLengthValue = computed({
  get: () => props.maxLength,
  set: (value: number | undefined) => emit('update:maxLength', value),
})

const defaultValueValue = computed({
  get: () => props.defaultValue || '',
  set: (value: string) => emit('update:defaultValue', value),
})
</script>

<template>
  <div class="text-field-config">
    <div class="section-header">
      <span class="section-title">{{ t('admin.cardType.fieldConfig.textSettings') || '文本设置' }}</span>
    </div>
    <div class="config-field" style="max-width: 320px;">
      <label class="field-label">{{ t('admin.cardType.fieldConfig.maxLength') }}</label>
      <a-input-number
        v-model="maxLengthValue"
        :min="1"
        :disabled="disabled"
        style="width: 100%"
      />
      <span class="field-hint">{{ t('admin.cardType.fieldConfig.maxLengthHint') || '留空表示不限制' }}</span>
    </div>
    <div class="config-field" style="margin-top: 16px; max-width: 320px;">
      <label class="field-label">{{ t('admin.cardType.fieldConfig.defaultValue') }}</label>
      <a-input
        v-model="defaultValueValue"
        :disabled="disabled"
        style="width: 100%"
      />
      <span class="field-hint">{{ t('admin.cardType.fieldConfig.defaultValueHint') || '创建时自动填入' }}</span>
    </div>
  </div>
</template>

<style scoped>
.text-field-config {
  margin-top: 24px;
}

.section-header {
  margin-bottom: 16px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-1);
  letter-spacing: 0.02em;
}

.config-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
}

.field-hint {
  font-size: 12px;
  color: var(--color-text-3);
}
</style>
