<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import { IconApps, IconEdit, IconDelete, IconMinusCircle, IconCheckCircle, IconLink, IconHistory } from '@arco-design/web-vue/es/icon'
import { viewApi } from '@/api'

const { t } = useI18n()
import CreateButton from '@/components/common/CreateButton.vue'
import HighlightText from '@/components/common/HighlightText.vue'
import { AdminTable, AdminTableActions } from '@/components/table'
import ViewEditForm from './ViewEditForm.vue'
import SchemaReferenceDrawer from '@/components/schema/SchemaReferenceDrawer.vue'
import SchemaChangelogDrawer from '@/components/schema/SchemaChangelogDrawer.vue'
import { useTableSearch } from '@/hooks/useTableSearch'
import { useTableSort } from '@/hooks/useTableSort'
import { useResponsiveColumnWidth } from '@/hooks/useResponsiveColumnWidth'
import type { ViewListItemVO, ListViewDefinition } from '@/types/view'
import type { ActionItem } from '@/types/table'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const route = useRoute()
const { scrollXExtra } = useResponsiveColumnWidth()

// 列表相关状态
const loading = ref(false)
const views = ref<ViewListItemVO[]>([])
const selectedViewType = ref<'LIST'>('LIST')

// 使用表格搜索和排序 hooks
const { searchKeyword, filteredData } = useTableSearch({
  data: views,
  searchFields: ['name', 'cardTypeName', 'description'],
})
const { sortedData, handleSortChange } = useTableSort(filteredData)

// 抽屉状态
const drawerVisible = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')
const currentView = ref<ListViewDefinition | null>(null)
const editLoading = ref(false)
const saving = ref(false)

// 引用关系抽屉状态
const referenceDrawerVisible = ref(false)
const currentReferenceSchemaId = ref<string | undefined>(undefined)

// 审计日志抽屉状态
const changelogDrawerVisible = ref(false)
const currentChangelogSchemaId = ref<string | undefined>(undefined)
const currentChangelogSchemaName = ref<string | undefined>(undefined)

// 视图类型选项（目前只有列表视图）
const viewTypeOptions = computed(() => [
  {
    value: 'LIST' as const,
    label: t('admin.view.listView'),
    icon: IconApps,
  },
])

// 获取类型数量
function getTypeCount(_type: 'LIST'): number {
  return views.value.length
}

// 获取搜索匹配数量
function getTypeMatchCount(_type: 'LIST'): number {
  if (!searchKeyword.value) return 0
  return sortedData.value.length
}

// 列表数据加载
async function fetchData() {
  loading.value = true
  try {
    views.value = await viewApi.list()
  } catch (error: any) {
    console.error('Failed to fetch views:', error)
    // 错误提示已在 request.ts 统一处理
  } finally {
    loading.value = false
  }
}


// 打开新建抽屉
function handleCreate() {
  drawerMode.value = 'create'
  currentView.value = null
  drawerVisible.value = true
}

// 打开编辑抽屉
async function handleEdit(record: ViewListItemVO) {
  drawerMode.value = 'edit'
  editLoading.value = true
  try {
    currentView.value = await viewApi.getById(record.id)
    drawerVisible.value = true
  } catch (error) {
    console.error('Failed to fetch view:', error)
    // 错误提示已在 request.ts 统一处理
  } finally {
    editLoading.value = false
  }
}

// 保存视图（创建或更新）
async function handleSave(view: ListViewDefinition) {
  saving.value = true
  try {
    if (drawerMode.value === 'create') {
      await viewApi.create(view)
      Message.success(t('admin.view.createSuccess'))
    } else {
      await viewApi.update(view.id!, view, view.contentVersion)
      Message.success(t('admin.view.saveSuccess'))
    }
    drawerVisible.value = false
    await fetchData()
  } catch (error: any) {
    console.error('Failed to save view:', error)
    // 错误提示已在 request.ts 统一处理，此处不再重复显示
  } finally {
    saving.value = false
  }
}

// 打开引用关系抽屉
function handleReference(record: ViewListItemVO) {
  if (!record.id) return
  currentReferenceSchemaId.value = record.id
  referenceDrawerVisible.value = true
}

// 打开审计日志抽屉
function handleChangelog(record: ViewListItemVO) {
  if (!record.id) return
  currentChangelogSchemaId.value = record.id
  currentChangelogSchemaName.value = record.name
  changelogDrawerVisible.value = true
}

import { handleReferenceConflictError } from '@/utils/error-handler'

// 删除视图
function handleDelete(record: ViewListItemVO) {
  Modal.confirm({
    title: t('admin.view.confirmDelete'),
    content: t('admin.view.deleteConfirmContent', { name: record.name }),
    okText: t('admin.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await viewApi.delete(record.id)
        Message.success(t('admin.message.deleteSuccess'))
        await fetchData()
      } catch (error: any) {
        if (!handleReferenceConflictError(error)) {
          console.error('Failed to delete view:', error)
        }
      }
    },
  })
}

// 启用/停用视图
async function handleToggleEnabled(record: ViewListItemVO) {
  try {
    if (record.enabled) {
      await viewApi.disable(record.id)
      Message.success(t('admin.message.disableSuccess'))
    } else {
      await viewApi.activate(record.id)
      Message.success(t('admin.message.enableSuccess'))
    }
    await fetchData()
  } catch (error: any) {
    console.error('Failed to toggle enabled:', error)
  }
}

// 处理路由参数中的编辑请求
async function handleEditFromRoute() {
  const editId = route.query.edit as string | undefined
  // tab 参数保留但暂不使用
  void route.query.tab
  if (editId) {
    // 清除路由参数，避免刷新后重复打开
    router.replace({ path: '/admin/view', query: {} })

    // 查找对应的视图
    const view = views.value.find((v) => v.id === editId)
    if (view) {
      await handleEdit(view)
    } else {
      // 如果列表中没有，直接用 ID 获取详情并打开编辑
      try {
        currentView.value = await viewApi.getById(editId)
        drawerMode.value = 'edit'
        drawerVisible.value = true
      } catch (error) {
        console.error('Failed to fetch view:', error)
        // 错误提示已在 request.ts 统一处理
      }
    }
    // TODO: 如果后续有tab功能，可以在这里处理 targetTab
  }
}

// 获取操作项列表
function getActions(record: ViewListItemVO): ActionItem[] {
  return [
    { key: 'edit', label: '编辑', icon: IconEdit },
    { key: 'reference', label: '引用关系', icon: IconLink },
    { key: 'changelog', label: '审计日志', icon: IconHistory },
    {
      key: 'toggle',
      label: record.enabled ? '停用' : '启用',
      icon: record.enabled ? IconMinusCircle : IconCheckCircle,
    },
    { key: 'delete', label: '删除', icon: IconDelete, danger: true, divider: true },
  ]
}

// 处理操作项点击
function handleAction(key: string, record: ViewListItemVO) {
  switch (key) {
    case 'edit':
      handleEdit(record)
      break
    case 'reference':
      handleReference(record)
      break
    case 'changelog':
      handleChangelog(record)
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
onMounted(async () => {
  await fetchData()
  await handleEditFromRoute()
})
</script>

<template>
  <div class="view-list-page">
    <!-- 左侧类型导航 -->
    <aside class="view-type-sidebar">
      <CreateButton class="sidebar-create-btn" @click="handleCreate">
        新建视图
      </CreateButton>
      <div class="type-list">
        <div
          v-for="typeItem in viewTypeOptions"
          :key="typeItem.value"
          :class="[
            'type-item',
            { active: selectedViewType === typeItem.value },
            { 'has-match': searchKeyword && getTypeMatchCount(typeItem.value) > 0 },
          ]"
        >
          <component :is="typeItem.icon" class="type-icon" />
          <span class="type-name">{{ typeItem.label }}</span>
          <span v-if="searchKeyword" :class="getTypeMatchCount(typeItem.value) > 0 ? 'type-match-count' : 'type-count'">
            {{ getTypeMatchCount(typeItem.value) }}
          </span>
          <span v-else class="type-count">{{ getTypeCount(typeItem.value) }}</span>
        </div>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <main class="view-list-content">
      <!-- 顶部工具栏 -->
      <div class="content-header">
        <a-input-search
          v-model="searchKeyword"
          placeholder="搜索名称、卡片类型"
          style="width: 260px"
          size="small"
          allow-clear
        />
      </div>

      <!-- 视图表格 -->
      <AdminTable
        :data="sortedData"
        :loading="loading"
        :scroll="{ x: 1000 + scrollXExtra, y: '100%' }"
        @sorter-change="handleSortChange"
      >
        <a-table-column
          :title="t('admin.view.viewName')"
          data-index="name"
          :width="200"
          fixed="left"
          :sortable="{ sortDirections: ['ascend', 'descend'] }"
        >
          <template #cell="{ record }">
            <a-link @click="handleEdit(record)">
              <HighlightText :text="record.name" :keyword="searchKeyword" />
            </a-link>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.view.viewType')" data-index="viewType">
          <template #cell>
            <span class="cell-text">{{ t('admin.view.listView') }}</span>
          </template>
        </a-table-column>
        <a-table-column
          :title="t('admin.view.cardType')"
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
        <a-table-column :title="t('admin.table.status')" align="center">
          <template #cell="{ record }">
            <span class="cell-text">
              {{ record.enabled ? t('admin.status.enabled') : t('admin.status.disabled') }}
            </span>
          </template>
        </a-table-column>
        <a-table-column
          :title="t('admin.table.updatedAt')"
          data-index="updatedAt"
          :width="160"
          :sortable="{ sortDirections: ['ascend', 'descend'] }"
        >
          <template #cell="{ record }">
            <span class="nowrap">{{ formatDateTime(record.updatedAt) }}</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.operations')" :width="100" fixed="right">
          <template #cell="{ record }">
            <AdminTableActions
              :actions="getActions(record)"
              @action="handleAction($event, record)"
            />
          </template>
        </a-table-column>

        <template #empty>
          <a-empty :description="t('admin.view.emptyDescription')" />
        </template>
      </AdminTable>
    </main>

    <!-- 新建/编辑抽屉 -->
    <ViewEditForm
      v-model:visible="drawerVisible"
      :mode="drawerMode"
      :view="currentView"
      @save="handleSave"
    />

    <!-- 引用关系抽屉 -->
    <SchemaReferenceDrawer
      v-model:visible="referenceDrawerVisible"
      :schema-id="currentReferenceSchemaId"
    />

    <!-- 审计日志抽屉 -->
    <SchemaChangelogDrawer
      v-model:visible="changelogDrawerVisible"
      :schema-id="currentChangelogSchemaId"
      :schema-name="currentChangelogSchemaName"
    />
  </div>
</template>

<style scoped lang="scss">
.view-list-page {
  display: flex;
  height: 100%;
  background: var(--color-bg-1);
  font-size: 12px;
  overflow: hidden;
}

.view-type-sidebar {
  width: 100px;
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

.type-match-count {
  font-size: 11px;
  color: #d48806;
  font-weight: 500;
  flex-shrink: 0;
}

.type-item.has-match:not(.active) {
  background: #fffbe6;
}

.type-item.has-match:not(.active) .type-name {
  color: #d48806;
}

.view-list-content {
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
</style>
