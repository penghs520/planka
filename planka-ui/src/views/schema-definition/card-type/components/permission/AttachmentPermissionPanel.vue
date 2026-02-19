<script setup lang="ts">
import type { AttachmentPermission } from '@/types/permission'
import { AttachmentOperation } from '@/types/permission'
import type { FieldOption } from '@/types/field-option'
import BasePermissionPanel from './BasePermissionPanel.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps<{
  modelValue?: AttachmentPermission[]
  cardTypeId: string
  attachmentFields: FieldOption[]
  availableFields: FieldOption[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: AttachmentPermission[]]
}>()

function getFieldNames(fieldIds: string[]): string {
  if (!fieldIds || fieldIds.length === 0) {
    return t('admin.cardType.permission.allAttachmentFields')
  }
  const names = fieldIds
    .map((id) => {
      const field = props.attachmentFields.find((f) => f.id === id)
      return field?.name || id
    })
    .slice(0, 3)
  if (fieldIds.length > 3) {
    return `${names.join('、')} ${t('admin.cardType.permission.andMore', { count: fieldIds.length - 3 })}`
  }
  return names.join('、')
}

function getRuleTitle(rule: AttachmentPermission): string {
  return t('admin.cardType.permission.attachmentRuleTitle', {
    fields: getFieldNames(rule.fieldIds),
    operation: t(`admin.cardType.permission.attachmentOperations.${rule.attachmentOperation}`)
  })
}

function createDefaultRule(): AttachmentPermission {
  return {
    attachmentOperation: AttachmentOperation.DOWNLOAD,
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
    :selectable-fields="attachmentFields"
    permission-type="attachment"
    :get-rule-title="getRuleTitle"
    :create-default-rule="createDefaultRule"
    @update:model-value="emit('update:modelValue', $event)"
  />
</template>
