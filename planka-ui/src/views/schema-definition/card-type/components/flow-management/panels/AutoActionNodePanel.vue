<script setup lang="ts">
/**
 * 自动执行节点属性面板
 */
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { AutoActionNodeDefinition } from '@/types/workflow'
import { FailureStrategy } from '@/types/workflow'

const { t } = useI18n()

const props = defineProps<{
  node: AutoActionNodeDefinition
  cardTypeId: string
}>()

const emit = defineEmits<{
  'update:node': [node: AutoActionNodeDefinition]
}>()

function updateName(val: string) {
  emit('update:node', { ...props.node, name: val })
}

function updateFailureStrategy(val: string) {
  emit('update:node', { ...props.node, failureStrategy: val as FailureStrategy })
}

const failureStrategyOptions = computed(() => [
  {
    label: t('admin.workflow.failureStrategy.BLOCK_WORKFLOW'),
    value: FailureStrategy.BLOCK_WORKFLOW,
  },
  {
    label: t('admin.workflow.failureStrategy.CONTINUE'),
    value: FailureStrategy.CONTINUE,
  },
])
</script>

<template>
  <div class="auto-action-panel">
    <a-form :model="node" layout="vertical" size="small">
      <a-form-item :label="t('admin.workflow.panel.nodeName')">
        <a-input
          :model-value="node.name"
          :placeholder="t('admin.workflow.panel.nodeNamePlaceholder')"
          @update:model-value="updateName"
        />
      </a-form-item>

      <a-form-item :label="t('admin.workflow.panel.failureStrategy')">
        <a-select
          :model-value="node.failureStrategy"
          :options="failureStrategyOptions"
          @update:model-value="updateFailureStrategy"
        />
      </a-form-item>

      <a-form-item :label="t('admin.workflow.panel.actions')">
        <div class="actions-placeholder">
          <a-empty :description="t('admin.workflow.panel.actionsEmpty')" />
        </div>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped lang="scss">
.auto-action-panel {
  padding: 0 4px;
}

.actions-placeholder {
  padding: 16px 0;
  border: 1px dashed var(--color-border-2);
  border-radius: var(--radius-md);
}
</style>
