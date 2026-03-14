<template>
  <div class="card-collection-formula-config">
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
      :label="t('admin.formulaDefinition.cardCollection.linkFieldId')"
      :rules="[{ required: true, message: t('admin.formulaDefinition.cardCollection.linkFieldIdRequired') }]"
    >
      <LinkFieldSelect
        v-model="formula.linkFieldId"
        :card-type-ids="cardTypeIds"
        :placeholder="t('admin.formulaDefinition.cardCollection.selectLinkField')"
      />
    </a-form-item>

    <a-form-item :label="t('admin.formulaDefinition.cardCollection.targetCardTypeIds')">
      <CardTypeSelect
        v-model="formula.targetCardTypeIds"
        :multiple="true"
        :placeholder="t('admin.formulaDefinition.cardCollection.selectTargetCardTypes')"
      />
      <div class="form-item-help">
        {{ t('admin.formulaDefinition.cardCollection.targetCardTypeIdsHelp') }}
      </div>
    </a-form-item>

    <a-form-item
      :label="t('admin.formulaDefinition.cardCollection.aggregationTypeLabel')"
      :rules="[{ required: true, message: t('admin.formulaDefinition.cardCollection.aggregationTypeRequired') }]"
    >
      <a-select v-model="formula.aggregationType">
        <a-option value="COUNT">{{ t('admin.formulaDefinition.cardCollection.aggregationType.count') }}</a-option>
        <a-option value="DISTINCT_COUNT">{{ t('admin.formulaDefinition.cardCollection.aggregationType.distinctCount') }}</a-option>
        <a-option value="SUM">{{ t('admin.formulaDefinition.cardCollection.aggregationType.sum') }}</a-option>
        <a-option value="AVG">{{ t('admin.formulaDefinition.cardCollection.aggregationType.avg') }}</a-option>
        <a-option value="MIN">{{ t('admin.formulaDefinition.cardCollection.aggregationType.min') }}</a-option>
        <a-option value="MAX">{{ t('admin.formulaDefinition.cardCollection.aggregationType.max') }}</a-option>
        <a-option value="P85">{{ t('admin.formulaDefinition.cardCollection.aggregationType.p85') }}</a-option>
      </a-select>
    </a-form-item>

    <a-form-item
      v-if="needsSourceFieldId"
      :label="t('admin.formulaDefinition.cardCollection.sourceFieldId')"
      :rules="[{ required: true, message: t('admin.formulaDefinition.cardCollection.sourceFieldIdRequired') }]"
    >
      <FieldSelect
        v-model="formula.sourceFieldId"
        :link-field-id="formula.linkFieldId"
        :field-types="['NUMBER']"
        :placeholder="t('admin.formulaDefinition.cardCollection.selectSourceField')"
      />
    </a-form-item>

    <a-form-item :label="t('admin.formulaDefinition.cardCollection.filterCondition')">
      <a-textarea
        v-model="formula.filterCondition"
        :placeholder="t('admin.formulaDefinition.cardCollection.filterConditionPlaceholder')"
        :rows="4"
      />
      <div class="form-item-help">
        {{ t('admin.formulaDefinition.cardCollection.filterConditionHelp') }}
      </div>
    </a-form-item>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CardCollectionFormulaDefinition } from '@/types/formula'
import { CardAggregationType } from '@/types/formula'
import CardTypeSelect from '@/components/card-type/CardTypeSelect.vue'
import LinkFieldSelect from '@/components/link-field/LinkFieldSelect.vue'
import FieldSelect from '@/components/field/FieldSelect.vue'

const props = defineProps<{
  modelValue: CardCollectionFormulaDefinition
  cardTypeIds?: string[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: CardCollectionFormulaDefinition): void
}>()

const { t } = useI18n()

const formula = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const needsSourceFieldId = computed(() => {
  return ![
    CardAggregationType.COUNT,
    CardAggregationType.DISTINCT_COUNT,
  ].includes(formula.value.aggregationType as CardAggregationType)
})
</script>

<style scoped lang="scss">
.card-collection-formula-config {
  .form-item-help {
    margin-top: 4px;
    font-size: 12px;
    color: var(--color-text-3);
  }
}
</style>
