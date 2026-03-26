<script setup lang="ts">
/**
 * 画布底部悬浮条：缩放控件 + 缩放比例 + 添加节点（需在 VueFlow 内渲染）
 */
import { useI18n } from 'vue-i18n'
import { Panel } from '@vue-flow/core'
import { Controls } from '@vue-flow/controls'
import { WorkflowNodeType } from '@/types/workflow'
import { useWorkflowPopupContainer } from './workflowPopupContext'

import '@vue-flow/controls/dist/style.css'

defineProps<{
  zoomPercent: number
}>()

const emit = defineEmits<{
  addNode: [type: WorkflowNodeType]
}>()

const { t } = useI18n()
const workflowPopupContainer = useWorkflowPopupContainer()

function addApproval() {
  emit('addNode', WorkflowNodeType.APPROVAL)
}

function addAutoAction() {
  emit('addNode', WorkflowNodeType.AUTO_ACTION)
}
</script>

<template>
  <Panel position="bottom-center" class="wf-flow-bottom-bar">
    <div class="wf-flow-bottom-bar__inner">
      <Controls
        class="wf-flow-bottom-bar__controls"
        :show-zoom="true"
        :show-fit-view="true"
        :show-interactive="false"
      />
      <span class="wf-flow-bottom-bar__zoom">
        {{ t('admin.workflow.canvas.zoomPercent', { percent: zoomPercent }) }}
      </span>
      <a-dropdown trigger="click" position="top" :popup-container="workflowPopupContainer">
        <a-button type="primary" size="small" class="wf-flow-bottom-bar__add">
          <template #icon>
            <icon-plus />
          </template>
          {{ t('admin.workflow.addNode') }}
        </a-button>
        <template #content>
          <a-doption @click="addApproval">
            <span class="wf-node-option">
              <span class="wf-node-dot" style="background: #3370ff" />
              {{ t('admin.workflow.nodeType.APPROVAL') }}
            </span>
          </a-doption>
          <a-doption @click="addAutoAction">
            <span class="wf-node-option">
              <span class="wf-node-dot" style="background: #ff9500" />
              {{ t('admin.workflow.nodeType.AUTO_ACTION') }}
            </span>
          </a-doption>
        </template>
      </a-dropdown>
    </div>
  </Panel>
</template>

<style scoped lang="scss">
.wf-flow-bottom-bar {
  margin-bottom: 16px;
}

.wf-flow-bottom-bar__inner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 14px;
  background: #fff;
  border: 1px solid var(--color-border-1);
  border-radius: 10px;
  box-shadow:
    0 4px 14px rgba(31, 35, 41, 0.08),
    0 0 1px rgba(31, 35, 41, 0.06);
}

.wf-flow-bottom-bar__controls {
  display: flex;
  align-items: center;
}

.wf-flow-bottom-bar__controls :deep(.vue-flow__controls-button) {
  background: #f5f6f8;
  border-color: var(--color-border-1);
  color: var(--color-text-2);

  &:hover {
    background: #e8eaed;
    color: var(--color-text-1);
  }
}

.wf-flow-bottom-bar__zoom {
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-3);
  min-width: 3.5em;
  text-align: center;
}

.wf-flow-bottom-bar__add {
  border-radius: 6px;
}

.wf-node-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wf-node-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
