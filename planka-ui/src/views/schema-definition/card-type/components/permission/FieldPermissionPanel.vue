<script setup lang="ts">
import type { FieldPermission } from '@/types/permission'
import { FieldOperation } from '@/types/permission'
import type { FieldOption } from '@/types/field-option'
import BasePermissionPanel from './BasePermissionPanel.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps<{
  modelValue?: FieldPermission[]
  cardTypeId: string
  fieldList: FieldOption[]
  availableFields: FieldOption[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: FieldPermission[]]
}>()

function getFieldNames(fieldIds: string[]): string {
  if (!fieldIds || fieldIds.length === 0) {
    return t('admin.cardType.permission.allFields')
  }
  const names = fieldIds
    .map((id) => {
      const field = props.fieldList.find((f) => f.id === id)
      return field?.name || id
    })
    .slice(0, 3)
  if (fieldIds.length > 3) {
    return `${names.join('、')} ${t('admin.cardType.permission.andMore', { count: fieldIds.length - 3 })}`
  }
  return names.join('、')
}

function getRuleTitle(rule: FieldPermission): string {
  return t('admin.cardType.permission.fieldRuleTitle', {
    fields: getFieldNames(rule.fieldIds),
    operation: t(`admin.cardType.permission.fieldOperations.${rule.operation}`)
  })
}

function createDefaultRule(): FieldPermission {
  return {
    operation: FieldOperation.EDIT,
    fieldIds: [],
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
    :selectable-fields="fieldList"
    permission-type="field"
    :get-rule-title="getRuleTitle"
    :create-default-rule="createDefaultRule"
    @update:model-value="emit('update:modelValue', $event)"
  />
</template>
