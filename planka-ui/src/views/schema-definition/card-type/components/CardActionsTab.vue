<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconEdit,
  IconDelete,
  IconMore,
  IconCheckCircle,
  IconMinusCircle,
} from '@arco-design/web-vue/es/icon'
import { cardActionApi } from '@/api/card-action'
import CreateButton from '@/components/common/CreateButton.vue'
import CardActionEditDrawer from './card-action/CardActionEditDrawer.vue'
import type { CardActionConfigDefinition } from '@/types/card-action'
import type { ActionCategory } from '@/types/card-action'
import { formatDateTime } from '@/utils/format'
import { handleReferenceConflictError } from '@/utils/error-handler'

const { t } = useI18n()

const props = defineProps<{
  cardTypeId: string
  cardTypeName?: string
}>()

const loading = ref(false)
const actions = ref<CardActionConfigDefinition[]>([])
const drawerVisible = ref(false)
const editingAction = ref<CardActionConfigDefinition | null>(null)

// 加载动作列表
async function fetchActions() {
  if (!props.cardTypeId) return

  loading.value = true
  try {
    actions.value = await cardActionApi.getByCardTypeId(props.cardTypeId)
  } catch (error: any) {
    console.error('Failed to fetch actions:', error)
    Message.error(error.message || t('admin.message.fetchFailed', { type: t('admin.cardAction.title') }))
  } finally {
    loading.value = false
  }
}

// 新建动作
function handleCreate() {
  editingAction.value = null
  drawerVisible.value = true
}

// 编辑动作
function handleEdit(action: CardActionConfigDefinition) {
  editingAction.value = action
  drawerVisible.value = true
}

// 删除动作
function handleDelete(action: CardActionConfigDefinition) {
  if (action.builtIn) {
    Message.warning(t('admin.cardAction.message.builtInCannotDelete'))
    return
  }
  Modal.confirm({
    title: t('admin.message.confirmDelete'),
    content: t('admin.cardAction.message.confirmDelete', { name: action.name }),
    okText: t('admin.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await cardActionApi.delete(action.id!)
        Message.success(t('admin.cardAction.message.deleteSuccess'))
        await fetchActions()
      } catch (error: any) {
        if (!handleReferenceConflictError(error)) {
          console.error('Failed to delete action:', error)
          Message.error(error.message || t('admin.message.deleteSuccess'))
        }
      }
    },
  })
}

// 启用/停用动作
async function handleToggleEnabled(action: CardActionConfigDefinition) {
  try {
    if (action.enabled) {
      await cardActionApi.disable(action.id!)
      Message.success(t('admin.cardAction.message.disableSuccess'))
    } else {
      await cardActionApi.activate(action.id!)
      Message.success(t('admin.cardAction.message.enableSuccess'))
    }
    await fetchActions()
  } catch (error: any) {
    console.error('Failed to toggle enabled:', error)
    Message.error(error.message || t('admin.message.saveSuccess'))
  }
}

// 保存成功后刷新列表
function handleSaveSuccess() {
  drawerVisible.value = false
  fetchActions()
}

// 获取动作类别标签
function getCategoryLabel(category?: ActionCategory): string {
  if (!category) return t('admin.cardAction.category.CUSTOM')
  return t(`admin.cardAction.category.${category}`)
}

// 获取执行类型标签
function getExecutionTypeLabel(action: CardActionConfigDefinition): string {
  if (action.builtIn && action.builtInActionType) {
    return t(`admin.cardAction.builtInType.${action.builtInActionType}`)
  }
  if (action.executionType?.type) {
    return t(`admin.cardAction.execution.${action.executionType.type}`)
  }
  return '-'
}

// 监听 cardTypeId 变化
watch(
  () => props.cardTypeId,
  (newId) => {
    if (newId) {
      fetchActions()
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="card-actions-tab">
    <!-- 头部操作栏 -->
    <div v-if="actions.length > 0" class="tab-header">
      <CreateButton @click="handleCreate">{{ t('admin.cardAction.createButton') }}</CreateButton>
    </div>

    <!-- 动作列表 -->
    <a-spin :loading="loading" class="action-list">
      <a-table
        v-if="actions.length > 0"
        :data="actions"
        :pagination="false"
        :bordered="false"
        row-key="id"
        size="small"
        :scroll="{ y: '100%' }"
      >
        <template #columns>
          <a-table-column :title="t('admin.cardAction.actionName')" data-index="name" :width="200">
            <template #cell="{ record }">
              <a-link @click="handleEdit(record)">{{ record.name }}</a-link>
              <a-tag v-if="record.builtIn" size="small" color="gray" style="margin-left: 4px">
                {{ t('admin.cardAction.builtIn') }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.cardAction.actionCategory')" :width="100">
            <template #cell="{ record }">
              {{ getCategoryLabel(record.actionCategory) }}
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.cardAction.executionType')" :width="120">
            <template #cell="{ record }">
              {{ getExecutionTypeLabel(record) }}
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.table.status')" :width="80" align="center">
            <template #cell="{ record }">
              <a-tag v-if="record.enabled" color="green" size="small">{{ t('admin.status.enabled') }}</a-tag>
              <a-tag v-else color="red" size="small">{{ t('admin.status.disabled') }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.table.updatedAt')" data-index="updatedAt" :width="140">
            <template #cell="{ record }">
              {{ formatDateTime(record.updatedAt) }}
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
                  <a-doption v-if="!record.builtIn" @click="handleToggleEnabled(record)">
                    <IconMinusCircle v-if="record.enabled" />
                    <IconCheckCircle v-else />
                    {{ record.enabled ? t('admin.action.disable') : t('admin.action.enable') }}
                  </a-doption>
                  <a-divider v-if="!record.builtIn" style="margin: 4px 0" />
                  <a-doption
                    v-if="!record.builtIn"
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

      <div v-else class="empty-state">
        <a-empty :description="t('admin.cardAction.emptyDescription')" />
        <CreateButton @click="handleCreate">{{ t('admin.cardAction.createButton') }}</CreateButton>
      </div>
    </a-spin>

    <!-- 编辑抽屉 -->
    <CardActionEditDrawer
      v-model:visible="drawerVisible"
      :card-type-id="cardTypeId"
      :action="editingAction"
      @success="handleSaveSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
.card-actions-tab {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.tab-header {
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
}

.action-list {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.action-list :deep(.arco-spin-children) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.action-list :deep(.arco-table) {
  height: 100%;
}

.action-list :deep(.arco-table-container) {
  height: 100%;
}

.action-list :deep(.arco-table-body) {
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
