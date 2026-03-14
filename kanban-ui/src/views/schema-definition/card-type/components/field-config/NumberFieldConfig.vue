<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ToggleButtonGroup from '@/components/common/ToggleButtonGroup.vue'

type DisplayFormat = 'NORMAL' | 'PERCENT' | 'THOUSAND_SEPARATOR'
type PercentStyle = 'NUMBER' | 'PROGRESS_BAR'

interface Props {
  defaultValue?: number
  minValue?: number
  maxValue?: number
  precision?: number
  unit?: string
  displayFormat?: DisplayFormat
  percentStyle?: PercentStyle
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  precision: 0,
  displayFormat: 'NORMAL',
  percentStyle: 'NUMBER',
  disabled: false,
})

const emit = defineEmits<{
  'update:defaultValue': [value: number | undefined]
  'update:minValue': [value: number | undefined]
  'update:maxValue': [value: number | undefined]
  'update:precision': [value: number]
  'update:unit': [value: string | undefined]
  'update:displayFormat': [value: DisplayFormat]
  'update:percentStyle': [value: PercentStyle]
}>()

const { t } = useI18n()

const defaultValueValue = computed({
  get: () => props.defaultValue,
  set: (value: number | undefined) => emit('update:defaultValue', value),
})

const minValueValue = computed({
  get: () => props.minValue,
  set: (value: number | undefined) => emit('update:minValue', value),
})

const maxValueValue = computed({
  get: () => props.maxValue,
  set: (value: number | undefined) => emit('update:maxValue', value),
})

const precisionValue = computed({
  get: () => props.precision,
  set: (value: number) => emit('update:precision', value),
})

const unitValue = computed({
  get: () => props.unit || '',
  set: (value: string) => emit('update:unit', value || undefined),
})

const displayFormatValue = computed({
  get: () => props.displayFormat,
  set: (value: DisplayFormat) => {
    emit('update:displayFormat', value)
    // 百分数模式时清除单位
    if (value === 'PERCENT') {
      emit('update:unit', undefined)
    }
  },
})

const percentStyleValue = computed({
  get: () => props.percentStyle,
  set: (value: PercentStyle) => emit('update:percentStyle', value),
})

const showUnit = computed(() => props.displayFormat !== 'PERCENT')
const showPercentStyle = computed(() => props.displayFormat === 'PERCENT')

const displayFormatOptions = computed(() => [
  { label: t('admin.cardType.fieldConfig.displayFormatNormal'), value: 'NORMAL' as DisplayFormat },
  { label: t('admin.cardType.fieldConfig.displayFormatPercent'), value: 'PERCENT' as DisplayFormat },
  { label: t('admin.cardType.fieldConfig.displayFormatThousandSeparator'), value: 'THOUSAND_SEPARATOR' as DisplayFormat },
])

const percentStyleOptions = computed(() => [
  { label: t('admin.cardType.fieldConfig.percentStyleNumber'), value: 'NUMBER' as PercentStyle },
  { label: t('admin.cardType.fieldConfig.percentStyleProgressBar'), value: 'PROGRESS_BAR' as PercentStyle },
])
</script>

<template>
  <div class="number-field-config">
    <div class="section-header">
      <span class="section-title">{{ t('admin.cardType.fieldConfig.numberConfig') }}</span>
    </div>

    <!-- 默认值 -->
    <div class="config-row">
      <div class="config-field" style="max-width: 200px;">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.defaultValue') }}</label>
        <a-input-number
          v-model="defaultValueValue"
          :disabled="disabled"
          style="width: 100%"
        />
      </div>
    </div>

    <!-- 输入范围 -->
    <div class="config-row">
      <div class="config-field">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.inputRange') }}</label>
        <div class="range-inputs">
          <a-input-number
            v-model="minValueValue"
            :placeholder="t('admin.cardType.fieldConfig.minValue')"
            :disabled="disabled"
            style="width: 120px"
          />
          <span class="range-separator">—</span>
          <a-input-number
            v-model="maxValueValue"
            :placeholder="t('admin.cardType.fieldConfig.maxValue')"
            :disabled="disabled"
            style="width: 120px"
          />
        </div>
      </div>
    </div>

    <!-- 显示样式 -->
    <div class="config-row">
      <div class="config-field">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.displayFormat') }}</label>
        <ToggleButtonGroup
          v-model="displayFormatValue"
          :options="displayFormatOptions"
          :disabled="disabled"
        />
      </div>
    </div>

    <!-- 小数位数和单位 -->
    <div class="config-row two-col">
      <div class="config-field" style="max-width: 120px;">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.precision') }}</label>
        <a-input-number
          v-model="precisionValue"
          :min="0"
          :max="10"
          :disabled="disabled"
          style="width: 100%"
        />
      </div>
      <div v-if="showUnit" class="config-field" style="max-width: 120px;">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.unit') }}</label>
        <a-input
          v-model="unitValue"
          :placeholder="t('admin.cardType.fieldConfig.unitPlaceholder')"
          :disabled="disabled"
        />
      </div>
    </div>

    <!-- 百分数显示效果 -->
    <div v-if="showPercentStyle" class="config-row">
      <div class="config-field">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.percentStyle') }}</label>
        <ToggleButtonGroup
          v-model="percentStyleValue"
          :options="percentStyleOptions"
          :disabled="disabled"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.number-field-config {
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
  gap: 20px;
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

.range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.range-separator {
  color: var(--color-text-3);
}
</style>
