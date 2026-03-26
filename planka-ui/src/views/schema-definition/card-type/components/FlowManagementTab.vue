<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconEdit,
  IconDelete,
  IconMore,
  IconCheckCircle,
  IconMinusCircle,
  IconSearch,
} from '@arco-design/web-vue/es/icon'
import { workflowApi } from '@/api/workflow'
import CreateButton from '@/components/common/CreateButton.vue'
import WorkflowEditDrawer from './flow-management/WorkflowEditDrawer.vue'
import type { WorkflowDefinition } from '@/types/workflow'
import { formatDateTime } from '@/utils/format'

const { t } = useI18n()

const props = defineProps<{
  cardTypeId: string
  cardTypeName?: string
}>()

const loading = ref(false)
const workflows = ref<WorkflowDefinition[]>([])
const drawerVisible = ref(false)
const editingWorkflow = ref<WorkflowDefinition | null>(null)

// 筛选
const searchKeyword = ref('')

const filteredWorkflows = computed(() => {
  if (!searchKeyword.value) return workflows.value
  return workflows.value.filter((wf) =>
    wf.name.toLowerCase().includes(searchKeyword.value.toLowerCase()),
  )
})

// 加载列表
async function fetchWorkflows() {
  if (!props.cardTypeId) return

  loading.value = true
  try {
    workflows.value = await workflowApi.getByCardTypeId(props.cardTypeId)
  } catch (error: any) {
    console.error('Failed to fetch workflows:', error)
    Message.error(error.message || t('admin.message.fetchFailed', { type: t('admin.workflow.title') }))
  } finally {
    loading.value = false
  }
}

// 新建
function handleCreate() {
  editingWorkflow.value = null
  drawerVisible.value = true
}

// 编辑
function handleEdit(workflow: WorkflowDefinition) {
  editingWorkflow.value = workflow
  drawerVisible.value = true
}

// 删除
function handleDelete(workflow: WorkflowDefinition) {
  Modal.confirm({
    title: t('admin.message.confirmDelete'),
    content: t('admin.workflow.message.confirmDelete', { name: workflow.name }),
    okText: t('admin.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await workflowApi.delete(workflow.id!)
        Message.success(t('admin.message.deleteSuccess'))
        await fetchWorkflows()
      } catch (error: any) {
        console.error('Failed to delete workflow:', error)
        Message.error(error.message || t('admin.workflow.message.deleteFailed'))
      }
    },
  })
}

// 启用/停用
async function handleToggleEnabled(workflow: WorkflowDefinition) {
  try {
    if (workflow.enabled) {
      await workflowApi.disable(workflow.id!)
      Message.success(t('admin.message.disableSuccess'))
    } else {
      await workflowApi.enable(workflow.id!)
      Message.success(t('admin.message.enableSuccess'))
    }
    await fetchWorkflows()
  } catch (error: any) {
    console.error('Failed to toggle enabled:', error)
    Message.error(error.message || t('admin.workflow.message.toggleFailed'))
  }
}

function handleSaveSuccess() {
  drawerVisible.value = false
  fetchWorkflows()
}

// 获取节点数描述
function getNodesCountLabel(workflow: WorkflowDefinition): string {
  const count = workflow.nodes?.length || 0
  return t('admin.workflow.nodesCount', { count })
}

watch(
  () => props.cardTypeId,
  (newId) => {
    if (newId) fetchWorkflows()
  },
  { immediate: true },
)
</script>

<template>
  <div class="flow-management-tab">
    <!-- 头部操作栏 -->
    <div v-if="workflows.length > 0" class="tab-header">
      <div class="tab-header-left">
        <a-input
          v-model="searchKeyword"
          :placeholder="t('admin.workflow.searchPlaceholder')"
          allow-clear
          size="small"
          class="filter-search"
        >
          <template #suffix><IconSearch /></template>
        </a-input>
      </div>
      <CreateButton @click="handleCreate">{{ t('admin.workflow.createButton') }}</CreateButton>
    </div>

    <!-- 工作流列表 -->
    <a-spin :loading="loading" class="workflow-list">
      <div v-if="workflows.length > 0" class="table-wrapper">
        <a-table
          :data="filteredWorkflows"
          :pagination="false"
          :bordered="false"
          row-key="id"
          size="small"
          :scroll="{ y: '100%' }"
        >
          <template #columns>
            <a-table-column :title="t('admin.workflow.workflowName')" data-index="name" :width="200">
              <template #cell="{ record }">
                <a-link @click="handleEdit(record)">{{ record.name }}</a-link>
              </template>
            </a-table-column>
            <a-table-column :title="t('admin.workflow.nodesLabel')" :width="100">
              <template #cell="{ record }">
                {{ getNodesCountLabel(record) }}
              </template>
            </a-table-column>
            <a-table-column :title="t('admin.table.status')" :width="80" align="center">
              <template #cell="{ record }">
                <a-tag v-if="record.enabled" color="green" size="small">{{ t('admin.status.enabled') }}</a-tag>
                <a-tag v-else color="gray" size="small">{{ t('admin.status.disabled') }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="t('admin.table.updatedAt')" data-index="updatedAt" :width="140">
              <template #cell="{ record }">
                <span class="time-column">{{ formatDateTime(record.updatedAt) }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="t('admin.table.operations')" :width="80" fixed="right">
              <template #cell="{ record }">
                <a-dropdown trigger="click">
                  <a-button type="text" size="small">
                    <template #icon><IconMore /></template>
                  </a-button>
                  <template #content>
                    <a-doption @click="handleEdit(record)">
                      <IconEdit /> {{ t('admin.action.edit') }}
                    </a-doption>
                    <a-doption @click="handleToggleEnabled(record)">
                      <IconMinusCircle v-if="record.enabled" />
                      <IconCheckCircle v-else />
                      {{ record.enabled ? t('admin.action.disable') : t('admin.action.enable') }}
                    </a-doption>
                    <a-divider style="margin: 4px 0" />
                    <a-doption
                      class="danger-option"
                      @click="handleDelete(record)"
                    >
                      <IconDelete /> {{ t('admin.action.delete') }}
                    </a-doption>
                  </template>
                </a-dropdown>
              </template>
            </a-table-column>
          </template>
        </a-table>
      </div>

      <div v-else class="empty-state">
        <a-empty :description="t('admin.workflow.emptyDescription')" />
        <CreateButton @click="handleCreate">{{ t('admin.workflow.createButton') }}</CreateButton>
      </div>
    </a-spin>

    <!-- 编辑抽屉 -->
    <WorkflowEditDrawer
      v-model:visible="drawerVisible"
      :card-type-id="cardTypeId"
      :workflow="editingWorkflow"
      @success="handleSaveSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
.flow-management-tab {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.tab-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-search {
  width: 200px;
}

.workflow-list {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.workflow-list :deep(.arco-spin-children) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.workflow-list :deep(.arco-table) {
  height: 100%;
}

.workflow-list :deep(.arco-table-container) {
  height: 100%;
}

.workflow-list :deep(.arco-table-body) {
  height: 100%;
}

.table-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 40px 0;
}

:deep(.danger-option) {
  color: rgb(var(--danger-6));
}
</style>
