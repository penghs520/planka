<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

type DateFormat = 'DATE' | 'DATETIME' | 'DATETIME_SECOND' | 'YEAR_MONTH'

interface Props {
  dateFormat?: DateFormat
  useNowAsDefault?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  dateFormat: 'DATE',
  useNowAsDefault: false,
  disabled: false,
})

const emit = defineEmits<{
  'update:dateFormat': [value: DateFormat]
  'update:useNowAsDefault': [value: boolean]
}>()

const { t } = useI18n()

const dateFormatValue = computed({
  get: () => props.dateFormat,
  set: (value: DateFormat) => emit('update:dateFormat', value),
})

const useNowAsDefaultValue = computed({
  get: () => props.useNowAsDefault,
  set: (value: boolean) => emit('update:useNowAsDefault', value),
})
</script>

<template>
  <div class="date-field-config">
    <div class="section-header">
      <span class="section-title">{{ t('admin.cardType.fieldConfig.dateConfig') }}</span>
    </div>
    <div class="config-field" style="max-width: 320px;">
      <label class="field-label">{{ t('admin.cardType.fieldConfig.dateFormat') }}</label>
      <a-select
        v-model="dateFormatValue"
        :disabled="disabled"
        style="width: 100%"
      >
        <a-option value="DATE">{{ t('admin.cardType.fieldConfig.dateFormatDate') }}</a-option>
        <a-option value="DATETIME">{{ t('admin.cardType.fieldConfig.dateFormatDatetime') }}</a-option>
        <a-option value="DATETIME_SECOND">{{ t('admin.cardType.fieldConfig.dateFormatDatetimeSecond') }}</a-option>
        <a-option value="YEAR_MONTH">{{ t('admin.cardType.fieldConfig.dateFormatYearMonth') }}</a-option>
      </a-select>
    </div>
    <div class="config-field" style="margin-top: 16px;">
      <label class="field-label">{{ t('admin.cardType.fieldConfig.useNowAsDefault') }}</label>
      <a-switch v-model="useNowAsDefaultValue" :disabled="disabled" />
    </div>
  </div>
</template>

<style scoped>
.date-field-config {
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
  align-items: flex-start;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
}
</style>
