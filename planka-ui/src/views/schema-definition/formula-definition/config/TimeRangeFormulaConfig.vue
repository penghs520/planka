<template>
  <div class="time-range-formula-config">
    <!-- 卡片类型单独占一行 -->
    <a-form-item :label="t('admin.formulaDefinition.cardTypes')">
      <CardTypeSelect
        v-model="formula.cardTypeIds"
        :multiple="true"
        :limit-concrete-single="true"
        :placeholder="t('admin.formulaDefinition.selectCardTypes')"
      />
      <div class="form-item-help">
        {{ t('admin.formulaDefinition.cardTypeAssociationHelp') }}
      </div>
    </a-form-item>

    <!-- 开始和结束时间数据源并排 -->
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item
          :label="t('admin.formulaDefinition.timeRange.startSourceType')"
          :rules="[{ required: true, message: t('admin.formulaDefinition.timeRange.startSourceTypeRequired') }]"
        >
          <a-select v-model="formula.startSourceType">
            <a-option
              v-for="option in sourceTypeOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </a-option>
          </a-select>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item
          :label="t('admin.formulaDefinition.timeRange.endSourceType')"
          :rules="[{ required: true, message: t('admin.formulaDefinition.timeRange.endSourceTypeRequired') }]"
        >
          <a-select v-model="formula.endSourceType">
            <a-option
              v-for="option in endSourceTypeOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </a-option>
          </a-select>
        </a-form-item>
      </a-col>
    </a-row>

    <!-- 开始时间配置 -->
    <a-form-item
      v-if="formula.startSourceType === 'CUSTOM_DATE_FIELD'"
      :label="t('admin.formulaDefinition.timeRange.startFieldId')"
    >
      <FieldSelect
        v-model="formula.startFieldId"
        :card-type-ids="cardTypeIds"
        :field-types="['DATE']"
      />
    </a-form-item>
    <a-form-item
      v-if="needsStartStreamId"
      :label="t('admin.formulaDefinition.timeRange.startStreamId')"
    >
      <ValueStreamSelect
        v-model="formula.startStreamId"
        :card-type-ids="cardTypeIds"
      />
    </a-form-item>
    <a-form-item
      v-if="needsStartStatusId"
      :label="t('admin.formulaDefinition.timeRange.startStatusId')"
    >
      <StatusSelect
        v-model="formula.startStatusId"
        :card-type-id="cardTypeIds && cardTypeIds.length > 0 ? cardTypeIds[0] : undefined"
        :stream-id="formula.startStreamId"
      />
    </a-form-item>

    <!-- 结束时间配置 -->
    <a-form-item
      v-if="formula.endSourceType === 'CUSTOM_DATE_FIELD'"
      :label="t('admin.formulaDefinition.timeRange.endFieldId')"
    >
      <FieldSelect
        v-model="formula.endFieldId"
        :card-type-ids="cardTypeIds"
        :field-types="['DATE']"
      />
    </a-form-item>
    <a-form-item
      v-if="needsEndStreamId"
      :label="t('admin.formulaDefinition.timeRange.endStreamId')"
    >
      <ValueStreamSelect
        v-model="formula.endStreamId"
        :card-type-ids="cardTypeIds"
      />
    </a-form-item>
    <a-form-item
      v-if="needsEndStatusId"
      :label="t('admin.formulaDefinition.timeRange.endStatusId')"
    >
      <StatusSelect
        v-model="formula.endStatusId"
        :card-type-id="cardTypeIds && cardTypeIds.length > 0 ? cardTypeIds[0] : undefined"
        :stream-id="formula.endStreamId"
      />
    </a-form-item>

    <!-- 统计精度 -->
    <a-form-item
      :label="t('admin.formulaDefinition.timeRange.precision')"
      :rules="[{ required: true, message: t('admin.formulaDefinition.timeRange.precisionRequired') }]"
    >
      <a-select v-model="formula.precision">
        <a-option value="DAY">{{ t('admin.formulaDefinition.timeRange.precisionOptions.day') }}</a-option>
        <a-option value="HOUR">{{ t('admin.formulaDefinition.timeRange.precisionOptions.hour') }}</a-option>
        <a-option value="MINUTE">{{ t('admin.formulaDefinition.timeRange.precisionOptions.minute') }}</a-option>
      </a-select>
    </a-form-item>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TimeRangeFormulaDefinition } from '@/types/formula'
import { TimePointSourceType } from '@/types/formula'
import CardTypeSelect from '@/components/card-type/CardTypeSelect.vue'
import FieldSelect from '@/components/field/FieldSelect.vue'
import ValueStreamSelect from '@/components/value-stream/ValueStreamSelect.vue'
import StatusSelect from '@/components/value-stream/StatusSelect.vue'

const props = defineProps<{
  modelValue: TimeRangeFormulaDefinition
  cardTypeIds?: string[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: TimeRangeFormulaDefinition): void
}>()

const { t } = useI18n()

const formula = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const sourceTypeOptions = [
  { value: TimePointSourceType.CARD_CREATED_TIME, label: t('admin.formulaDefinition.timePoint.sourceTypeOptions.cardCreatedTime') },
  { value: TimePointSourceType.CARD_UPDATED_TIME, label: t('admin.formulaDefinition.timePoint.sourceTypeOptions.cardUpdatedTime') },
  { value: TimePointSourceType.CUSTOM_DATE_FIELD, label: t('admin.formulaDefinition.timePoint.sourceTypeOptions.customDateField') },
  { value: TimePointSourceType.STATUS_ENTER_TIME, label: t('admin.formulaDefinition.timePoint.sourceTypeOptions.statusEnterTime') },
  { value: TimePointSourceType.STATUS_EXIT_TIME, label: t('admin.formulaDefinition.timePoint.sourceTypeOptions.statusExitTime') },
  { value: TimePointSourceType.CURRENT_STATUS_ENTER_TIME, label: t('admin.formulaDefinition.timePoint.sourceTypeOptions.currentStatusEnterTime') },
]

const endSourceTypeOptions = [
  ...sourceTypeOptions,
  { value: TimePointSourceType.CURRENT_TIME, label: t('admin.formulaDefinition.timePoint.sourceTypeOptions.currentTime') },
]

const needsStartStreamId = computed(() => {
  return [
    TimePointSourceType.STATUS_ENTER_TIME,
    TimePointSourceType.STATUS_EXIT_TIME,
    TimePointSourceType.CURRENT_STATUS_ENTER_TIME,
  ].includes(formula.value.startSourceType as TimePointSourceType)
})

const needsStartStatusId = computed(() => {
  return [
    TimePointSourceType.STATUS_ENTER_TIME,
    TimePointSourceType.STATUS_EXIT_TIME,
  ].includes(formula.value.startSourceType as TimePointSourceType)
})

const needsEndStreamId = computed(() => {
  return [
    TimePointSourceType.STATUS_ENTER_TIME,
    TimePointSourceType.STATUS_EXIT_TIME,
    TimePointSourceType.CURRENT_STATUS_ENTER_TIME,
  ].includes(formula.value.endSourceType as TimePointSourceType)
})

const needsEndStatusId = computed(() => {
  return [
    TimePointSourceType.STATUS_ENTER_TIME,
    TimePointSourceType.STATUS_EXIT_TIME,
  ].includes(formula.value.endSourceType as TimePointSourceType)
})
</script>

<style scoped lang="scss">
.time-range-formula-config {
  // 样式
}
</style>
