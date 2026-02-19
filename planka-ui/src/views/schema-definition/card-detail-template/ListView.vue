<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconRefresh,
  IconFile,
  IconEdit,
  IconDelete,
  IconMinusCircle,
  IconCheckCircle,
  IconCopy,
} from '@arco-design/web-vue/es/icon'
import { cardDetailTemplateApi } from '@/api/card-detail-template'
import CreateButton from '@/components/common/CreateButton.vue'
import HighlightText from '@/components/common/HighlightText.vue'
import { AdminTable, AdminTableActions } from '@/components/table'
import { useTableSearch } from '@/hooks/useTableSearch'
import { useTableSort } from '@/hooks/useTableSort'
import type { TemplateListItemVO } from '@/types/card-detail-template'
import type { ActionItem } from '@/types/table'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

// 列表相关状态
const loading = ref(false)
const templates = ref<TemplateListItemVO[]>([])

// 使用表格搜索和排序 hooks
const { searchKeyword, filteredData } = useTableSearch({
  data: templates,
  searchFields: ['name', 'cardTypeName', 'description'],
})
const { sortedData, handleSortChange } = useTableSort(filteredData)

// 列表数据加载
async function fetchData() {
  loading.value = true
  try {
    templates.value = await cardDetailTemplateApi.list()
  } catch (error: any) {
    console.error('Failed to fetch templates:', error)
    Message.error(error.message || '加载模板列表失败')
  } finally {
    loading.value = false
  }
}


// 打开新建页面
function handleCreate() {
  router.push({ name: 'CardDetailTemplateCreate' })
}

// 打开编辑页面
function handleEdit(record: TemplateListItemVO) {
  router.push({ name: 'CardDetailTemplateEdit', params: { id: record.id } })
}

// 复制模板
async function handleCopy(record: TemplateListItemVO) {
  try {
    await cardDetailTemplateApi.copy(record.id, `${record.name} - 副本`)
    Message.success('复制成功')
    await fetchData()
  } catch (error: any) {
    console.error('Failed to copy template:', error)
    Message.error(error.message || '复制失败')
  }
}

import { handleReferenceConflictError } from '@/utils/error-handler'

// 删除模板
function handleDelete(record: TemplateListItemVO) {
  if (record.systemTemplate) {
    Message.warning('系统内置模板不可删除')
    return
  }
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除模板「${record.name}」吗？删除后不可恢复。`,
    okText: '删除',
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple', // 使用全局优化样式
    async onOk() {
      try {
        await cardDetailTemplateApi.delete(record.id)
        Message.success('删除成功')
        await fetchData()
      } catch (error: any) {
        if (!handleReferenceConflictError(error)) {
          console.error('Failed to delete template:', error)
          // 错误提示已在 request.ts 统一处理
        }
      }
    },
  })
}

// 启用/停用模板
async function handleToggleEnabled(record: TemplateListItemVO) {
  try {
    if (record.enabled) {
      await cardDetailTemplateApi.disable(record.id)
      Message.success('已停用')
    } else {
      await cardDetailTemplateApi.activate(record.id)
      Message.success('已启用')
    }
    await fetchData()
  } catch (error: any) {
    console.error('Failed to toggle enabled:', error)
    Message.error(error.message || '操作失败')
  }
}

// 获取操作项列表
function getActions(record: TemplateListItemVO): ActionItem[] {
  return [
    { key: 'edit', label: '编辑', icon: IconEdit },
    { key: 'copy', label: '复制', icon: IconCopy },
    {
      key: 'toggle',
      label: record.enabled ? '停用' : '启用',
      icon: record.enabled ? IconMinusCircle : IconCheckCircle,
    },
    {
      key: 'delete',
      label: '删除',
      icon: IconDelete,
      danger: true,
      divider: true,
      disabled: record.systemTemplate,
    },
  ]
}

// 处理操作项点击
function handleAction(key: string, record: TemplateListItemVO) {
  switch (key) {
    case 'edit':
      handleEdit(record)
      break
    case 'copy':
      handleCopy(record)
      break
    case 'toggle':
      handleToggleEnabled(record)
      break
    case 'delete':
      handleDelete(record)
      break
  }
}

// 页面挂载时加载数据
onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="template-list-page">
    <!-- 左侧类型导航 -->
    <aside class="template-type-sidebar">
      <CreateButton class="sidebar-create-btn" @click="handleCreate">
        新建模板
      </CreateButton>
      <div class="type-list">
        <div class="type-item active">
          <IconFile class="type-icon" />
          <span class="type-name">详情页模板</span>
          <span class="type-count">{{ templates.length }}</span>
        </div>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <main class="template-list-content">
      <!-- 顶部工具栏 -->
      <div class="content-header">
        <a-input-search
          v-model="searchKeyword"
          placeholder="搜索名称、卡片类型"
          style="width: 260px"
          size="small"
          allow-clear
        />
        <a-button size="small" :loading="loading" @click="fetchData">
          <template #icon><IconRefresh /></template>
        </a-button>
      </div>

      <!-- 模板表格 -->
      <AdminTable
        :data="sortedData"
        :loading="loading"
        :scroll="{ x: 1000, y: '100%' }"
        @sorter-change="handleSortChange"
      >
        <a-table-column
          title="模板名称"
          data-index="name"
          :width="200"
          fixed="left"
          :sortable="{ sortDirections: ['ascend', 'descend'] }"
        >
          <template #cell="{ record }">
            <a-link @click="handleEdit(record)">
              <HighlightText :text="record.name" :keyword="searchKeyword" />
            </a-link>
            <span v-if="record.systemTemplate" class="system-tag">系统</span>
          </template>
        </a-table-column>
        <a-table-column
          title="卡片类型"
          data-index="cardTypeName"
          :sortable="{ sortDirections: ['ascend', 'descend'] }"
        >
          <template #cell="{ record }">
            <HighlightText
                :text="record.cardTypeName || record.cardTypeId"
                :keyword="searchKeyword"
              />
          </template>
        </a-table-column>
        <a-table-column
          title="优先级"
          data-index="priority"
          :width="80"
          align="center"
          :sortable="{ sortDirections: ['ascend', 'descend'] }"
        />
        <a-table-column title="Tab 数量" data-index="tabCount" :width="80" align="center" />
        <a-table-column title="状态" :width="80" align="center">
          <template #cell="{ record }">
            <span class="cell-text">{{ record.enabled ? '启用' : '停用' }}</span>
          </template>
        </a-table-column>
        <a-table-column
          title="更新时间"
          data-index="updatedAt"
          :width="160"
          :sortable="{ sortDirections: ['ascend', 'descend'] }"
        >
          <template #cell="{ record }">
            <span class="nowrap">{{ formatDateTime(record.updatedAt) }}</span>
          </template>
        </a-table-column>
        <a-table-column title="操作" :width="80">
          <template #cell="{ record }">
            <AdminTableActions
              :actions="getActions(record)"
              @action="handleAction($event, record)"
            />
          </template>
        </a-table-column>

        <template #empty>
          <a-empty description="暂无模板数据" />
        </template>
      </AdminTable>
    </main>
  </div>
</template>

<style scoped lang="scss">
.template-list-page {
  display: flex;
  height: 100%;
  background: var(--color-bg-1);
  font-size: 12px;
  overflow: hidden;
}

.template-type-sidebar {
  width: 120px;
  background: var(--color-bg-2);
  border-right: 1px solid var(--color-border);
  padding: 8px 4px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.sidebar-create-btn {
  margin: 0 4px 8px;
  width: calc(100% - 8px);
}

.type-list {
  flex: 1;
  overflow-y: auto;
}

.type-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 8px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: all 0.2s;
  font-size: 12px;
}

.type-item:hover {
  background: var(--color-fill-2);
}

.type-item.active {
  background: rgb(var(--primary-1));
  color: rgb(var(--primary-6));
}

.type-icon {
  flex-shrink: 0;
  font-size: 14px;
  color: var(--color-text-3);
  margin-right: 6px;
}

.type-item.active .type-icon {
  color: rgb(var(--primary-6));
}

.type-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.type-count {
  font-size: 11px;
  color: var(--color-text-3);
  flex-shrink: 0;
}

.template-list-content {
  flex: 1;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.content-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  background: var(--color-bg-2);
}

.cell-text {
  font-size: 13px;
  color: var(--color-text-1);
  white-space: nowrap;
}

.system-tag {
  margin-left: 4px;
  font-size: 12px;
  color: var(--color-text-3);
}
</style>
