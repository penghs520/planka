<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

interface Props {
  validateUrl?: boolean
  showPreview?: boolean
  defaultUrl?: string
  defaultLinkText?: string
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  validateUrl: true,
  showPreview: false,
  disabled: false,
})

const emit = defineEmits<{
  'update:validateUrl': [value: boolean]
  'update:showPreview': [value: boolean]
  'update:defaultUrl': [value: string]
  'update:defaultLinkText': [value: string]
}>()

const { t } = useI18n()

const validateUrlValue = computed({
  get: () => props.validateUrl,
  set: (value: boolean) => emit('update:validateUrl', value),
})

const showPreviewValue = computed({
  get: () => props.showPreview,
  set: (value: boolean) => emit('update:showPreview', value),
})

const defaultUrlValue = computed({
  get: () => props.defaultUrl || '',
  set: (value: string) => emit('update:defaultUrl', value),
})

const defaultLinkTextValue = computed({
  get: () => props.defaultLinkText || '',
  set: (value: string) => emit('update:defaultLinkText', value),
})
</script>

<template>
  <div class="weburl-field-config">
    <div class="section-header">
      <span class="section-title">{{ t('admin.cardType.fieldConfig.webUrlConfig') }}</span>
    </div>

    <div class="config-row two-col">
      <div class="config-field">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.validateUrl') }}</label>
        <a-switch v-model="validateUrlValue" :disabled="disabled" />
      </div>
      <div class="config-field">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.showPreview') }}</label>
        <a-switch v-model="showPreviewValue" :disabled="disabled" />
      </div>
    </div>

    <div class="config-grid">
      <div class="config-field">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.defaultUrl') }}</label>
        <a-input
          v-model="defaultUrlValue"
          :disabled="disabled"
        />
      </div>
      <div class="config-field">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.defaultLinkText') }}</label>
        <a-input
          v-model="defaultLinkTextValue"
          :disabled="disabled"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.weburl-field-config {
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

.config-row {
  margin-bottom: 16px;
}

.config-row.two-col {
  display: flex;
  gap: 40px;
}

.config-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.config-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
}
</style>
