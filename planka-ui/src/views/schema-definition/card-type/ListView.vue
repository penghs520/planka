<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconEdit,
  IconDelete,
  IconCheckCircle,
  IconMinusCircle,
  IconLink,
  IconHistory,
} from '@arco-design/web-vue/es/icon'
import { cardTypeApi } from '@/api'
import HighlightText from '@/components/common/HighlightText.vue'
import CreateButton from '@/components/common/CreateButton.vue'
import SchemaReferenceDrawer from '@/components/schema/SchemaReferenceDrawer.vue'
import SchemaChangelogDrawer from '@/components/schema/SchemaChangelogDrawer.vue'
import { AdminTable, AdminTableActions } from '@/components/table'
import CardTypeFormDrawer from './CardTypeFormDrawer.vue'
import { useTableSearch } from '@/hooks/useTableSearch'
import { useTableSort } from '@/hooks/useTableSort'
import { useResponsiveColumnWidth } from '@/hooks/useResponsiveColumnWidth'
import { formatDateTime } from '@/utils/format'
import { handleReferenceConflictError } from '@/utils/error-handler'
import type { CardTypeDefinition } from '@/types/card-type'
import type { ActionItem } from '@/types/table'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()

// 列表状态
const loading = ref(false)
const cardTypes = ref<CardTypeDefinition[]>([])

// 使用表格搜索和排序 hooks
const { searchKeyword, filteredData } = useTableSearch({
  data: cardTypes,
  searchFields: ['name', 'code', 'description'],
})
const { sortedData, handleSortChange } = useTableSort(filteredData)
const { columnWidth, scrollXExtra } = useResponsiveColumnWidth()

// 表单抽屉状态
const drawerVisible = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')
const editingCardType = ref<CardTypeDefinition | null>(null)

// 引用关系抽屉状态
const referenceDrawerVisible = ref(false)
const currentReferenceSchemaId = ref<string | undefined>(undefined)

// 审计日志抽屉状态
const changelogDrawerVisible = ref(false)
const currentChangelogSchemaId = ref<string | undefined>(undefined)
const currentChangelogSchemaName = ref<string | undefined>(undefined)

// 数据加载
async function fetchData() {
  if (loading.value) return

  loading.value = true
  try {
    cardTypes.value = await cardTypeApi.listAll()
  } catch (error) {
    console.error('Failed to fetch card types:', error)
  } finally {
    loading.value = false
  }
}

// 打开新建抽屉
function handleCreate() {
  drawerMode.value = 'create'
  editingCardType.value = null
  drawerVisible.value = true
}

// 打开编辑抽屉
function handleEdit(cardType: CardTypeDefinition) {
  drawerMode.value = 'edit'
  editingCardType.value = cardType
  drawerVisible.value = true
}

// 删除卡片类型
async function handleDelete(cardType: CardTypeDefinition) {
  Modal.confirm({
    title: t('admin.message.confirmDelete'),
    content: t('admin.message.deleteConfirmContent', { type: t('admin.cardType.title'), name: cardType.name }),
    okText: t('admin.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await cardTypeApi.delete(cardType.id!)
        Message.success(t('admin.message.deleteSuccess'))
        await fetchData()
      } catch (error) {
        if (!handleReferenceConflictError(error)) {
          console.error('Failed to delete:', error)
        }
      }
    },
  })
}

// 启用/停用
async function handleToggleEnabled(cardType: CardTypeDefinition) {
  try {
    if (cardType.enabled) {
      await cardTypeApi.disable(cardType.id!)
      Message.success(t('admin.message.disableSuccess'))
    } else {
      await cardTypeApi.activate(cardType.id!)
      Message.success(t('admin.message.enableSuccess'))
    }
    await fetchData()
  } catch (error) {
    console.error('Failed to toggle enabled:', error)
  }
}

// 打开引用关系抽屉
function handleReference(cardType: CardTypeDefinition) {
  if (!cardType.id) return
  currentReferenceSchemaId.value = cardType.id
  referenceDrawerVisible.value = true
}

// 打开审计日志抽屉
function handleChangelog(cardType: CardTypeDefinition) {
  if (!cardType.id) return
  currentChangelogSchemaId.value = cardType.id
  currentChangelogSchemaName.value = cardType.name
  changelogDrawerVisible.value = true
}

// 表单保存成功
async function handleFormSuccess() {
  await fetchData()
}

// 获取操作项列表
function getActions(record: CardTypeDefinition): ActionItem[] {
  return [
    { key: 'edit', label: t('admin.action.edit'), icon: IconEdit, visible: !record.systemCardType },
    { key: 'reference', label: t('admin.action.reference'), icon: IconLink },
    { key: 'changelog', label: t('admin.action.changelog'), icon: IconHistory },
    {
      key: 'toggle',
      label: record.enabled ? t('admin.action.disable') : t('admin.action.enable'),
      icon: record.enabled ? IconMinusCircle : IconCheckCircle,
    },
    {
      key: 'delete',
      label: t('admin.action.delete'),
      icon: IconDelete,
      danger: true,
      divider: true,
      visible: !record.systemCardType,
    },
  ]
}

// 处理操作项点击
function handleAction(key: string, record: CardTypeDefinition) {
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

// 处理路由参数中的编辑请求
async function handleEditFromRoute() {
  const editId = route.query.edit as string | undefined
  if (editId) {
    router.replace({ path: '/admin/card-type', query: {} })
    const cardType = cardTypes.value.find((ct) => ct.id === editId)
    if (cardType) {
      handleEdit(cardType)
    } else {
      // 如果列表中没有，直接用 ID 构造一个临时对象
      handleEdit({ id: editId, name: '' } as CardTypeDefinition)
    }
  }
}

onMounted(async () => {
  await fetchData()
  await handleEditFromRoute()
})
</script>

<template>
  <div class="card-type-page">
    <div class="card-type-content">
      <div class="content-header">
        <a-input-search
          v-model="searchKeyword"
          :placeholder="t('admin.search.placeholder')"
          style="width: 260px"
          size="small"
          allow-clear
        />
        <div class="header-spacer"></div>
        <CreateButton @click="handleCreate">
          {{ t('admin.cardType.createButton') }}
        </CreateButton>
      </div>

      <AdminTable
        :data="sortedData"
        :loading="loading"
        :scroll="{ x: 1400 + scrollXExtra, y: '100%' }"
        @sorter-change="handleSortChange"
      >
        <a-table-column :title="t('admin.table.name')" data-index="name" :width="180" fixed="left" :sortable="{ sortDirections: ['ascend', 'descend'] }">
          <template #cell="{ record }">
            <div class="name-cell">
              <a-tooltip :content="record.name">
                <a-link class="name-link" @click="handleEdit(record)">
                  <HighlightText :text="record.name" :keyword="searchKeyword" />
                </a-link>
              </a-tooltip>
            </div>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.code')" data-index="code" :width="200" :sortable="{ sortDirections: ['ascend', 'descend'] }">
          <template #cell="{ record }">
            <HighlightText :text="record.code || '-'" :keyword="searchKeyword" />
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.type')" data-index="schemaSubType" :width="120">
          <template #cell="{ record }">
            <span class="type-text">
              {{ t(`admin.cardType.schemaSubType.${record.schemaSubType}`) }}
            </span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.parentTypes')" data-index="parentTypes" :width="150">
          <template #cell="{ record }">
            <template v-if="record.parentTypes && record.parentTypes.length > 0">
              <a-tag v-for="parent in record.parentTypes" :key="parent.id" size="small" color="arcoblue">
                {{ parent.name }}
              </a-tag>
            </template>
            <span v-else class="text-placeholder">-</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.status')" :width="80">
          <template #cell="{ record }">
            <span class="status-text">
              {{ record.enabled ? t('admin.status.enabled') : t('admin.status.disabled') }}
            </span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.systemBuiltin')" :width="columnWidth.systemBuiltin">
          <template #cell="{ record }">
            <span v-if="record.systemCardType" class="builtin-text">{{ t('admin.status.yes') }}</span>
            <span v-else>-</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.updatedAt')" data-index="updatedAt" :width="180" :sortable="{ sortDirections: ['ascend', 'descend'] }">
          <template #cell="{ record }">
            <span class="nowrap">{{ formatDateTime(record.updatedAt) }}</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.createdAt')" data-index="createdAt" :width="180" :sortable="{ sortDirections: ['ascend', 'descend'] }">
          <template #cell="{ record }">
            <span class="nowrap">{{ formatDateTime(record.createdAt) }}</span>
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
          <a-empty :description="t('admin.cardType.emptyDescription')" />
        </template>
      </AdminTable>
    </div>

    <!-- 新建/编辑抽屉 -->
    <CardTypeFormDrawer
      v-model:visible="drawerVisible"
      :mode="drawerMode"
      :editing-card-type="editingCardType"
      @success="handleFormSuccess"
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
      :include-children="true"
    />
  </div>
</template>

<style scoped>
.card-type-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card-type-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-2);
  border-radius: 4px;
  overflow: hidden;
}

.content-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
}

.header-spacer {
  flex: 1;
}

/* 表格名称列 */
.name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 占位文字 */
.text-placeholder {
  color: var(--color-text-4);
}

.name-link {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

/* 表格文字样式 */
.type-text,
.status-text,
.builtin-text,
.level-text {
  font-size: 13px;
  color: var(--color-text-1);
  white-space: nowrap;
}
</style>
