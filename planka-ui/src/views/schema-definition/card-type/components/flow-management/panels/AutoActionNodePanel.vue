<script setup lang="ts">
/**
 * 自动执行节点属性面板
 */
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import CompactAddButton from '@/components/common/CompactAddButton.vue'
import type { AutoActionNodeDefinition } from '@/types/workflow'
import { FailureStrategy } from '@/types/workflow'
import type { RuleAction } from '@/types/biz-rule'
import { createDefaultRuleAction, validateRuleAction } from '@/types/biz-rule'
import RuleActionsEditor from '../../biz-rule/RuleActionsEditor.vue'
import { useWorkflowPopupContainer } from '../workflowPopupContext'

const { t } = useI18n()
const workflowPopupContainer = useWorkflowPopupContainer()

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

const modalVisible = ref(false)
const draftActions = ref<RuleAction[]>([])
const modalFocusIndex = ref<number | null>(null)
const editorKey = ref(0)

function openModal(focusIndex?: number) {
  draftActions.value = JSON.parse(JSON.stringify(props.node.actions || [])) as RuleAction[]
  if (draftActions.value.length === 0) {
    draftActions.value = [createDefaultRuleAction(0)]
    modalFocusIndex.value = 0
  } else {
    modalFocusIndex.value = focusIndex ?? null
  }
  editorKey.value += 1
  modalVisible.value = true
}

function closeModal() {
  modalVisible.value = false
  modalFocusIndex.value = null
}

/** 返回 true 关闭弹窗，false 保持打开 */
function handleModalOk(): boolean {
  const acts = draftActions.value
  for (let i = 0; i < acts.length; i++) {
    const action = acts[i]
    if (!action) continue
    const detail = validateRuleAction(action, t)
    if (detail) {
      const typeName = t(`admin.bizRule.actionType.${action.actionType}`)
      Message.error(
        t('admin.bizRule.actionConfig.actionIncomplete', {
          index: i + 1,
          type: typeName,
          detail,
        }),
      )
      return false
    }
  }
  emit('update:node', { ...props.node, actions: acts })
  return true
}

function cancelModal() {
  closeModal()
}

/** 侧栏主文案：优先自定义名称，否则动作类型 */
function primaryLine(action: RuleAction): string {
  const n = action.name?.trim()
  if (n) return n
  return t(`admin.bizRule.actionType.${action.actionType}`)
}
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
          :popup-container="workflowPopupContainer"
          @update:model-value="updateFailureStrategy"
        />
      </a-form-item>

      <a-form-item :label="t('admin.workflow.panel.actions')">
        <!-- 单根包裹：避免 a-form-item 内多子节点被横向 flex 排成一行 -->
        <div class="wf-auto-actions-block">
          <ul v-if="node.actions?.length" class="action-line-list">
            <li
              v-for="(action, index) in node.actions"
              :key="index"
              class="action-line"
              @click="openModal(index)"
            >
              <span class="action-line__index">{{ index + 1 }}</span>
              <span class="action-line__text">{{ primaryLine(action) }}</span>
            </li>
          </ul>
          <div class="action-add-btn-wrap">
            <CompactAddButton @click="openModal()">
              {{ t('admin.bizRule.actionConfig.addAction') }}
            </CompactAddButton>
          </div>
        </div>
      </a-form-item>
    </a-form>

    <a-modal
      v-model:visible="modalVisible"
      popup-container="#workflow-action-modal-root"
      :title="t('admin.workflow.panel.actionsModalTitle')"
      :width="840"
      :body-style="{ maxHeight: '70vh', overflowY: 'auto' }"
      :mask-closable="false"
      unmount-on-close
      :on-before-ok="handleModalOk"
      @cancel="cancelModal"
    >
      <RuleActionsEditor
        v-if="modalVisible"
        :key="editorKey"
        v-model="draftActions"
        :card-type-id="cardTypeId"
        :initial-expanded-index="modalFocusIndex"
        single-action-mode
      />
    </a-modal>
  </div>
</template>

<style scoped lang="scss">
.auto-action-panel {
  padding: 0 4px;
}

.wf-auto-actions-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.action-line-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.action-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 6px;
  font-size: 13px;
  line-height: 1.4;
  color: var(--color-text-1);
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-2);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s;

  &:hover {
    background: var(--color-fill-2);
    border-color: var(--color-primary-light-3);
  }
}

.action-line__index {
  flex-shrink: 0;
  min-width: 18px;
  font-weight: 500;
  color: var(--color-text-3);
}

.action-line__text {
  flex: 1;
  min-width: 0;
  word-break: break-word;
}

.action-add-btn-wrap {
  flex-shrink: 0;
}
</style>
