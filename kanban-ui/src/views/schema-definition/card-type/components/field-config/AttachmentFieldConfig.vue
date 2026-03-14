<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

interface Props {
  maxFileSize?: number
  maxFileCount?: number
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  maxFileCount: 10,
  disabled: false,
})

const emit = defineEmits<{
  'update:maxFileSize': [value: number | undefined]
  'update:maxFileCount': [value: number]
}>()

const { t } = useI18n()

const maxFileSizeValue = computed({
  get: () => props.maxFileSize,
  set: (value: number | undefined) => emit('update:maxFileSize', value),
})

const maxFileCountValue = computed({
  get: () => props.maxFileCount,
  set: (value: number) => emit('update:maxFileCount', value),
})
</script>

<template>
  <div class="attachment-field-config">
    <div class="section-header">
      <span class="section-title">{{ t('admin.cardType.fieldConfig.attachmentConfig') }}</span>
    </div>
    <div class="config-grid">
      <div class="config-field">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.maxFileSize') }}</label>
        <a-input-number
          v-model="maxFileSizeValue"
          :min="1"
          :disabled="disabled"
          style="width: 100%"
        />
        <span class="field-hint">{{ t('admin.cardType.fieldConfig.maxLengthHint') || '留空表示不限制' }}</span>
      </div>
      <div class="config-field">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.maxFileCount') }}</label>
        <a-input-number
          v-model="maxFileCountValue"
          :min="1"
          :disabled="disabled"
          style="width: 100%"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.attachment-field-config {
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

.config-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
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
