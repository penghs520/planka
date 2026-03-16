<template>
  <div class="time-point-formula-config">
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

    <a-form-item
      :label="t('admin.formulaDefinition.timePoint.sourceType')"
      :rules="[{ required: true, message: t('admin.formulaDefinition.timePoint.sourceTypeRequired') }]"
    >
      <a-select v-model="formula.sourceType" :placeholder="t('admin.formulaDefinition.timePoint.selectSourceType')">
        <a-option
          v-for="option in sourceTypeOptions"
          :key="option.value"
          :value="option.value"
        >
          {{ option.label }}
        </a-option>
      </a-select>
    </a-form-item>

    <a-form-item
      v-if="formula.sourceType === 'CUSTOM_DATE_FIELD'"
      :label="t('admin.formulaDefinition.timePoint.sourceFieldId')"
      :rules="[{ required: true, message: t('admin.formulaDefinition.timePoint.sourceFieldIdRequired') }]"
    >
      <FieldSelect
        v-model="formula.sourceFieldId"
        :card-type-ids="cardTypeIds"
        :field-types="['DATE']"
        :placeholder="t('admin.formulaDefinition.timePoint.selectSourceField')"
      />
    </a-form-item>

    <a-form-item
      v-if="needsStreamId"
      :label="t('admin.formulaDefinition.timePoint.streamId')"
      :rules="[{ required: true, message: t('admin.formulaDefinition.timePoint.streamIdRequired') }]"
    >
      <ValueStreamSelect
        v-model="formula.streamId"
        :card-type-ids="cardTypeIds"
        :placeholder="t('admin.formulaDefinition.timePoint.selectStream')"
      />
    </a-form-item>

    <a-form-item
      v-if="needsStatusId"
      :label="t('admin.formulaDefinition.timePoint.statusId')"
      :rules="[{ required: true, message: t('admin.formulaDefinition.timePoint.statusIdRequired') }]"
    >
      <StatusSelect
        v-model="formula.statusId"
        :card-type-id="cardTypeIds && cardTypeIds.length > 0 ? cardTypeIds[0] : undefined"
        :stream-id="formula.streamId"
        :placeholder="t('admin.formulaDefinition.timePoint.selectStatus')"
      />
    </a-form-item>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TimePointFormulaDefinition } from '@/types/formula'
import { TimePointSourceType } from '@/types/formula'
import CardTypeSelect from '@/components/card-type/CardTypeSelect.vue'
import FieldSelect from '@/components/field/FieldSelect.vue'
import ValueStreamSelect from '@/components/value-stream/ValueStreamSelect.vue'
import StatusSelect from '@/components/value-stream/StatusSelect.vue'

const props = defineProps<{
  modelValue: TimePointFormulaDefinition
  cardTypeIds?: string[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: TimePointFormulaDefinition): void
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

const needsStreamId = computed(() => {
  return [
    TimePointSourceType.STATUS_ENTER_TIME,
    TimePointSourceType.STATUS_EXIT_TIME,
    TimePointSourceType.CURRENT_STATUS_ENTER_TIME,
  ].includes(formula.value.sourceType as TimePointSourceType)
})

const needsStatusId = computed(() => {
  return [
    TimePointSourceType.STATUS_ENTER_TIME,
    TimePointSourceType.STATUS_EXIT_TIME,
  ].includes(formula.value.sourceType as TimePointSourceType)
})
</script>

<style scoped lang="scss">
.time-point-formula-config {
  // 样式
}
</style>
