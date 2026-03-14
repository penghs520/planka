<template>
  <div class="number-calculation-formula-config">
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
      :label="t('admin.formulaDefinition.numberCalculation.expression')"
      :rules="[{ required: true, message: t('admin.formulaDefinition.numberCalculation.expressionRequired') }]"
    >
      <a-textarea
        v-model="formula.expression"
        :placeholder="t('admin.formulaDefinition.numberCalculation.expressionPlaceholder')"
        :rows="6"
      />
      <div class="form-item-help">
        {{ t('admin.formulaDefinition.numberCalculation.expressionHelp') }}
      </div>
    </a-form-item>

    <a-form-item :label="t('admin.formulaDefinition.numberCalculation.expressionStructure')">
      <a-textarea
        v-model="formula.expressionStructure"
        :placeholder="t('admin.formulaDefinition.numberCalculation.expressionStructurePlaceholder')"
        :rows="8"
      />
      <div class="form-item-help">
        {{ t('admin.formulaDefinition.numberCalculation.expressionStructureHelp') }}
      </div>
    </a-form-item>

    <a-form-item :label="t('admin.formulaDefinition.numberCalculation.precision')">
      <a-input-number
        v-model="formula.precision"
        :min="0"
        :max="10"
        :placeholder="t('admin.formulaDefinition.numberCalculation.precisionPlaceholder')"
      />
      <div class="form-item-help">
        {{ t('admin.formulaDefinition.numberCalculation.precisionHelp') }}
      </div>
    </a-form-item>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { NumberCalculationFormulaDefinition } from '@/types/formula'
import CardTypeSelect from '@/components/card-type/CardTypeSelect.vue'

const props = defineProps<{
  modelValue: NumberCalculationFormulaDefinition
  cardTypeIds?: string[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: NumberCalculationFormulaDefinition): void
}>()

const { t } = useI18n()

const formula = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
</script>

<style scoped lang="scss">
.number-calculation-formula-config {
  .form-item-help {
    margin-top: 4px;
    font-size: 12px;
    color: var(--color-text-3);
  }
}
</style>
