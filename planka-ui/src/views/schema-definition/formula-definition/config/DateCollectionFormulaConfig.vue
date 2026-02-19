<template>
  <div class="date-collection-formula-config">
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
      :label="t('admin.formulaDefinition.dateCollection.linkFieldId')"
      :rules="[{ required: true, message: t('admin.formulaDefinition.dateCollection.linkFieldIdRequired') }]"
    >
      <LinkFieldSelect
        v-model="formula.linkFieldId"
        :card-type-ids="cardTypeIds"
        :placeholder="t('admin.formulaDefinition.dateCollection.selectLinkField')"
      />
    </a-form-item>

    <a-form-item :label="t('admin.formulaDefinition.dateCollection.targetCardTypeIds')">
      <CardTypeSelect
        v-model="formula.targetCardTypeIds"
        :multiple="true"
        :placeholder="t('admin.formulaDefinition.dateCollection.selectTargetCardTypes')"
      />
      <div class="form-item-help">
        {{ t('admin.formulaDefinition.dateCollection.targetCardTypeIdsHelp') }}
      </div>
    </a-form-item>

    <a-form-item
      :label="t('admin.formulaDefinition.dateCollection.sourceFieldId')"
      :rules="[{ required: true, message: t('admin.formulaDefinition.dateCollection.sourceFieldIdRequired') }]"
    >
      <FieldSelect
        v-model="formula.sourceFieldId"
        :link-field-id="formula.linkFieldId"
        :field-types="['DATE']"
        :placeholder="t('admin.formulaDefinition.dateCollection.selectSourceField')"
      />
    </a-form-item>

    <a-form-item
      :label="t('admin.formulaDefinition.dateCollection.aggregationTypeLabel')"
      :rules="[{ required: true, message: t('admin.formulaDefinition.dateCollection.aggregationTypeRequired') }]"
    >
      <a-select v-model="formula.aggregationType">
        <a-option value="EARLIEST">{{ t('admin.formulaDefinition.dateCollection.aggregationType.earliest') }}</a-option>
        <a-option value="LATEST">{{ t('admin.formulaDefinition.dateCollection.aggregationType.latest') }}</a-option>
      </a-select>
    </a-form-item>

    <a-form-item :label="t('admin.formulaDefinition.dateCollection.filterCondition')">
      <a-textarea
        v-model="formula.filterCondition"
        :placeholder="t('admin.formulaDefinition.dateCollection.filterConditionPlaceholder')"
        :rows="4"
      />
      <div class="form-item-help">
        {{ t('admin.formulaDefinition.dateCollection.filterConditionHelp') }}
      </div>
    </a-form-item>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { DateCollectionFormulaDefinition } from '@/types/formula'
import CardTypeSelect from '@/components/card-type/CardTypeSelect.vue'
import LinkFieldSelect from '@/components/link-field/LinkFieldSelect.vue'
import FieldSelect from '@/components/field/FieldSelect.vue'

const props = defineProps<{
  modelValue: DateCollectionFormulaDefinition
  cardTypeIds?: string[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: DateCollectionFormulaDefinition): void
}>()

const { t } = useI18n()

const formula = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
</script>

<style scoped lang="scss">
.date-collection-formula-config {
  .form-item-help {
    margin-top: 4px;
    font-size: 12px;
    color: var(--color-text-3);
  }
}
</style>
