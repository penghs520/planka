<script setup lang="ts">
/**
 * 审批节点属性面板
 */
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ApprovalNodeDefinition } from '@/types/workflow'
import { ApprovalMode } from '@/types/workflow'

const { t } = useI18n()

const props = defineProps<{
  node: ApprovalNodeDefinition
}>()

const emit = defineEmits<{
  'update:node': [node: ApprovalNodeDefinition]
}>()

const localNode = computed({
  get: () => props.node,
  set: (val) => emit('update:node', val),
})

function updateName(val: string) {
  emit('update:node', { ...props.node, name: val })
}

function updateApprovalMode(val: string) {
  emit('update:node', { ...props.node, approvalMode: val as ApprovalMode })
}

function updateSelectorType(val: string) {
  if (val === 'FIXED_MEMBERS') {
    emit('update:node', {
      ...props.node,
      approverSelector: { selectorType: 'FIXED_MEMBERS', memberIds: [] },
    })
  } else if (val === 'ROLE_BASED') {
    emit('update:node', {
      ...props.node,
      approverSelector: { selectorType: 'ROLE_BASED', roleIds: [] },
    })
  }
}

function updateMemberIds(val: string[]) {
  if (props.node.approverSelector.selectorType === 'FIXED_MEMBERS') {
    emit('update:node', {
      ...props.node,
      approverSelector: { selectorType: 'FIXED_MEMBERS', memberIds: val },
    })
  }
}

function updateRoleIds(val: string[]) {
  if (props.node.approverSelector.selectorType === 'ROLE_BASED') {
    emit('update:node', {
      ...props.node,
      approverSelector: { selectorType: 'ROLE_BASED', roleIds: val },
    })
  }
}

const approvalModeOptions = computed(() => [
  { label: t('admin.workflow.approvalMode.ANY_ONE'), value: ApprovalMode.ANY_ONE },
  { label: t('admin.workflow.approvalMode.ALL_REQUIRED'), value: ApprovalMode.ALL_REQUIRED },
])

const selectorTypeOptions = computed(() => [
  { label: t('admin.workflow.selectorType.FIXED_MEMBERS'), value: 'FIXED_MEMBERS' },
  { label: t('admin.workflow.selectorType.ROLE_BASED'), value: 'ROLE_BASED' },
])
</script>

<template>
  <div class="approval-panel">
    <a-form :model="localNode" layout="vertical" size="small">
      <a-form-item :label="t('admin.workflow.panel.nodeName')">
        <a-input
          :model-value="node.name"
          :placeholder="t('admin.workflow.panel.nodeNamePlaceholder')"
          @update:model-value="updateName"
        />
      </a-form-item>

      <a-form-item :label="t('admin.workflow.panel.approvalMode')">
        <a-select
          :model-value="node.approvalMode"
          :options="approvalModeOptions"
          @update:model-value="updateApprovalMode"
        />
      </a-form-item>

      <a-form-item :label="t('admin.workflow.panel.approverType')">
        <a-select
          :model-value="node.approverSelector.selectorType"
          :options="selectorTypeOptions"
          @update:model-value="updateSelectorType"
        />
      </a-form-item>

      <a-form-item
        v-if="node.approverSelector.selectorType === 'FIXED_MEMBERS'"
        :label="t('admin.workflow.panel.approvers')"
      >
        <a-select
          :model-value="node.approverSelector.memberIds"
          multiple
          allow-search
          :placeholder="t('admin.workflow.panel.approversPlaceholder')"
          @update:model-value="updateMemberIds"
        />
      </a-form-item>

      <a-form-item
        v-if="node.approverSelector.selectorType === 'ROLE_BASED'"
        :label="t('admin.workflow.panel.roles')"
      >
        <a-select
          :model-value="node.approverSelector.roleIds"
          multiple
          allow-search
          :placeholder="t('admin.workflow.panel.rolesPlaceholder')"
          @update:model-value="updateRoleIds"
        />
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped lang="scss">
.approval-panel {
  padding: 0 4px;
}
</style>
