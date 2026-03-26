<script setup lang="ts">
/**
 * 工作流全屏编辑器（顶栏 + 画布 + 底部悬浮控件；从节点右侧拖线选择插入类型）
 */
import { ref, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import WorkflowCanvas from './WorkflowCanvas.vue'
import { workflowApi } from '@/api/workflow'
import { useOrgStore } from '@/stores/org'
import type { WorkflowDefinition } from '@/types/workflow'
import { createEmptyWorkflow } from '@/types/workflow'

const { t } = useI18n()
const orgStore = useOrgStore()

const props = defineProps<{
  visible: boolean
  cardTypeId: string
  workflow: WorkflowDefinition | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const saving = ref(false)
const localWorkflow = ref<WorkflowDefinition | null>(null)

const isEdit = computed(() => !!props.workflow?.id)

// 初始化
watch(
  () => props.visible,
  (val) => {
    if (val) {
      if (props.workflow) {
        localWorkflow.value = JSON.parse(JSON.stringify(props.workflow))
      } else {
        localWorkflow.value = createEmptyWorkflow(props.cardTypeId, orgStore.currentOrgId!)
      }
    }
  },
)

function handleClose() {
  emit('update:visible', false)
}

function handleUpdateWorkflow(wf: WorkflowDefinition) {
  localWorkflow.value = wf
}

async function handleSave() {
  if (!localWorkflow.value) return

  if (!localWorkflow.value.name?.trim()) {
    Message.warning(t('admin.workflow.message.nameRequired'))
    return
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await workflowApi.update(
        localWorkflow.value.id!,
        localWorkflow.value,
        localWorkflow.value.contentVersion,
      )
    } else {
      await workflowApi.create(localWorkflow.value)
    }
    Message.success(t('admin.message.saveSuccess'))
    emit('success')
    handleClose()
  } catch (error: unknown) {
    // 失败提示已由 @/api/request 拦截器统一弹出，此处不再 Message.error 避免重复
    console.error('Failed to save workflow:', error)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <teleport to="body">
    <transition name="wf-editor-fade">
      <div v-if="visible && localWorkflow" class="wf-editor-overlay">
        <!-- 顶部工具栏 -->
        <div class="wf-toolbar">
          <div class="wf-toolbar__left">
            <a-button type="text" size="small" class="wf-toolbar__back" @click="handleClose">
              <template #icon><icon-left /></template>
              {{ t('admin.action.back') }}
            </a-button>
            <a-divider direction="vertical" :margin="8" />
            <a-input
              v-model="localWorkflow.name"
              :placeholder="t('admin.workflow.form.namePlaceholder')"
              class="wf-toolbar__name"
              size="small"
            />
          </div>

          <div class="wf-toolbar__right">
            <a-button size="small" @click="handleClose">
              {{ t('admin.action.cancel') }}
            </a-button>
            <a-button type="primary" size="small" :loading="saving" @click="handleSave">
              {{ t('admin.action.save') }}
            </a-button>
          </div>
        </div>

        <div class="wf-editor-body">
          <WorkflowCanvas
            :workflow="localWorkflow"
            :card-type-id="cardTypeId"
            @update:workflow="handleUpdateWorkflow"
          />
        </div>
      </div>
    </transition>
  </teleport>
</template>

<style lang="scss">
.wf-editor-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  background: #f0f1f3;
}

// 顶部工具栏
.wf-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid var(--color-border-1);
  flex-shrink: 0;
  z-index: 10;
  box-shadow: 0 1px 0 rgba(31, 35, 41, 0.04);
}

.wf-toolbar__left,
.wf-toolbar__right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wf-toolbar__left {
  flex: 1;
  min-width: 0;
}

.wf-toolbar__right {
  flex-shrink: 0;
}

.wf-toolbar__back {
  color: var(--color-text-2);
  flex-shrink: 0;
}

.wf-toolbar__name {
  width: min(320px, 40vw);
}

.wf-editor-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: #f5f6f8;
}

// 进出动画
.wf-editor-fade-enter-active,
.wf-editor-fade-leave-active {
  transition: opacity 0.2s ease;
}

.wf-editor-fade-enter-from,
.wf-editor-fade-leave-to {
  opacity: 0;
}
</style>
