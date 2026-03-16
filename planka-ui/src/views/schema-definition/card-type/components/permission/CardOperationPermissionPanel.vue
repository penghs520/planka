<script setup lang="ts">
import type { CardOperationPermission } from '@/types/permission'
import { CardOperation } from '@/types/permission'
import type { FieldOption } from '@/types/field-option'
import BasePermissionPanel from './BasePermissionPanel.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps<{
  modelValue?: CardOperationPermission[]
  cardTypeId: string
  availableFields: FieldOption[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: CardOperationPermission[]]
}>()

function getRuleTitle(rule: CardOperationPermission): string {
  // 兼容存量数据：如果没有 operations 数组，使用空数组
  const operations = rule.operations || []
  const operationLabels = operations.map(op => t(`admin.cardType.permission.operations.${op}`))
  return t('admin.cardType.permission.ruleTitle', {
    operation: operationLabels.join('、') || '-'
  })
}

function createDefaultRule(): CardOperationPermission {
  return {
    operations: [CardOperation.EDIT],
    cardConditions: [],
    operatorConditions: [],
    alertMessage: '',
  }
}
</script>

<template>
  <BasePermissionPanel
    v-model="props.modelValue"
    :card-type-id="cardTypeId"
    :available-fields="availableFields"
    permission-type="card"
    :get-rule-title="getRuleTitle"
    :create-default-rule="createDefaultRule"
    @update:model-value="emit('update:modelValue', $event)"
  />
</template>
