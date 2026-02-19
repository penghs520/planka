<template>
  <div class="formula-definition-page">
    <!-- 左侧导航 -->
    <aside class="formula-type-sidebar">
      <CreateButton class="sidebar-create-btn" @click="handleCreate">
        {{ t('admin.formulaDefinition.createButton') }}
      </CreateButton>
      <div class="type-list">
        <div
          v-for="typeItem in formulaTypeOptions"
          :key="typeItem.value"
          :class="[
            'type-item',
            { active: selectedFormulaType === typeItem.value },
            { 'has-match': searchKeyword && getTypeMatchCount(typeItem.value) > 0 },
          ]"
          @click="handleTypeSelect(typeItem.value)"
        >
          <FormulaTypeIcon :formula-type="typeItem.value" class="type-icon" />
          <span class="type-name">{{ typeItem.label }}</span>
          <span
            v-if="searchKeyword"
            :class="getTypeMatchCount(typeItem.value) > 0 ? 'type-match-count' : 'type-count'"
          >
            {{ getTypeMatchCount(typeItem.value) }}
          </span>
          <span v-else class="type-count">{{ getTypeCount(typeItem.value) }}</span>
        </div>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <main class="formula-list-content">
      <!-- 顶部工具栏 -->
      <div class="content-header">
        <a-input-search
          v-model="searchKeyword"
          :placeholder="t('admin.search.placeholder')"
          style="width: 260px"
          size="small"
          allow-clear
        />
      </div>

      <!-- 公式表格 -->
      <AdminTable
        :data="filteredFormulas"
        :loading="loading"
        :scroll="{ x: 1200 + scrollXExtra, y: '100%' }"
        @sorter-change="handleSortChange"
      >
        <a-table-column
          :title="t('admin.formulaDefinition.name')"
          data-index="name"
          :width="180"
          fixed="left"
          :sortable="{ sortDirections: ['ascend', 'descend'] }"
        >
          <template #cell="{ record }">
            <a-link @click="handleEdit(record.id)">
              <HighlightText :text="record.name" :keyword="searchKeyword" />
            </a-link>
          </template>
        </a-table-column>
        <a-table-column
          :title="t('admin.formulaDefinition.code')"
          data-index="code"
          :width="200"
          :sortable="{ sortDirections: ['ascend', 'descend'] }"
        >
          <template #cell="{ record }">
            <HighlightText :text="record.code || '-'" :keyword="searchKeyword" />
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.formulaDefinition.formulaType')" :width="150">
          <template #cell="{ record }">
            <span class="cell-text">{{ getFormulaTypeLabel(record.schemaSubType) }}</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.formulaDefinition.cardTypes')" :width="200">
          <template #cell="{ record }">
            <template v-if="record.cardTypes && record.cardTypes.length > 0">
              <template v-for="(ct, index) in record.cardTypes" :key="ct.id">
                <SchemaLink
                  :schema-id="ct.id"
                  :schema-type="SchemaType.CARD_TYPE"
                  :name="ct.name"
                />
                <span v-if="index < record.cardTypes.length - 1">、</span>
              </template>
            </template>
            <span v-else>-</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.formulaDefinition.enabled')" :width="100">
          <template #cell="{ record }">
            <a-tag :color="record.enabled ? 'green' : 'red'">
              {{ record.enabled ? t('admin.status.enabled') : t('admin.status.disabled') }}
            </a-tag>
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
        <a-table-column
          :title="t('admin.table.createdAt')"
          data-index="createdAt"
          :width="160"
          :sortable="{ sortDirections: ['ascend', 'descend'] }"
        >
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
          <a-empty :description="t('common.state.empty')" />
        </template>
      </AdminTable>
    </main>

    <!-- 创建/编辑抽屉 -->
    <FormulaDefinitionDrawer
      v-model:visible="drawerVisible"
      :mode="drawerMode"
      :formula-id="editFormulaId"
      @success="handleDrawerSuccess"
    />

    <!-- 引用关系抽屉 -->
    <SchemaReferenceDrawer
      v-model:visible="referenceDrawerVisible"
      :schema-id="currentReferenceSchemaId"
      :schema-type="SchemaType.FORMULA_DEFINITION"
    />

    <!-- 审计日志抽屉 -->
    <SchemaChangelogDrawer
      v-model:visible="changelogDrawerVisible"
      :schema-id="currentChangelogSchemaId"
      :schema-name="currentChangelogSchemaName"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import FormulaTypeIcon from '@/components/formula/FormulaTypeIcon.vue'
import { formulaDefinitionApi } from '@/api'
import HighlightText from '@/components/common/HighlightText.vue'
import CreateButton from '@/components/common/CreateButton.vue'
import { AdminTable, AdminTableActions } from '@/components/table'
import type { FormulaDefinition } from '@/types/formula'
import type { ActionItem } from '@/types/table'
import { SchemaSubType, SchemaType } from '@/types/schema'
import SchemaReferenceDrawer from '@/components/schema/SchemaReferenceDrawer.vue'
import SchemaChangelogDrawer from '@/components/schema/SchemaChangelogDrawer.vue'
import SchemaLink from '@/components/schema/SchemaLink.vue'
import FormulaDefinitionDrawer from './FormulaDefinitionDrawer.vue'
import { formatDateTime } from '@/utils/format'
import { useResponsiveColumnWidth } from '@/hooks/useResponsiveColumnWidth'
import { handleReferenceConflictError } from '@/utils/error-handler'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const { columnWidth: _columnWidth, scrollXExtra } = useResponsiveColumnWidth()

// 列表相关状态
const loading = ref(false)
const formulas = ref<FormulaDefinition[]>([])
const searchKeyword = ref('')
const selectedFormulaType = ref<SchemaSubType | undefined>(undefined)

// 抽屉状态
const drawerVisible = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')
const editFormulaId = ref<string | undefined>(undefined)

// 引用关系抽屉状态
const referenceDrawerVisible = ref(false)
const currentReferenceSchemaId = ref<string | undefined>(undefined)

// 审计日志抽屉状态
const changelogDrawerVisible = ref(false)
const currentChangelogSchemaId = ref<string | undefined>(undefined)
const currentChangelogSchemaName = ref<string | undefined>(undefined)

// 排序状态
const sortField = ref<string>('')
const sortOrder = ref<'ascend' | 'descend' | ''>('')

// 公式类型选项
const formulaTypeOptions = computed(() => [
  { value: SchemaSubType.TIME_POINT_FORMULA_DEFINITION, label: t('admin.formulaDefinition.type.timePoint') },
  { value: SchemaSubType.TIME_RANGE_FORMULA_DEFINITION, label: t('admin.formulaDefinition.type.timeRange') },
  { value: SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION, label: t('admin.formulaDefinition.type.dateCollection') },
  { value: SchemaSubType.CARD_COLLECTION_FORMULA_DEFINITION, label: t('admin.formulaDefinition.type.cardCollection') },
  { value: SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION, label: t('admin.formulaDefinition.type.numberCalculation') },
])

// 按当前选中类型筛选的公式列表
const filteredFormulas = computed(() => {
  let result = formulas.value
  if (selectedFormulaType.value) {
    result = result.filter((formula) => formula.schemaSubType === selectedFormulaType.value)
  }
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(
      (formula) =>
        formula.name.toLowerCase().includes(keyword) ||
        formula.code?.toLowerCase().includes(keyword)
    )
  }
  // 排序（仅在有排序字段时）
  if (sortField.value && sortOrder.value) {
    result = [...result].sort((a, b) => {
      const field = sortField.value as keyof typeof a
      const aVal = a[field] ?? ''
      const bVal = b[field] ?? ''
      const compare = String(aVal).localeCompare(String(bVal))
      return sortOrder.value === 'ascend' ? compare : -compare
    })
  }
  return result
})

// 处理排序变化
function handleSortChange(field: string, order: 'ascend' | 'descend' | '') {
  sortField.value = order ? field : ''
  sortOrder.value = order
}

// 获取各类型数量
function getTypeCount(type: SchemaSubType): number {
  return formulas.value.filter((f) => f.schemaSubType === type).length
}

// 获取各类型搜索匹配数量
function getTypeMatchCount(type: SchemaSubType): number {
  if (!searchKeyword.value) return 0
  const keyword = searchKeyword.value.toLowerCase()
  return formulas.value.filter(
    (f) =>
      f.schemaSubType === type &&
      (f.name.toLowerCase().includes(keyword) || f.code?.toLowerCase().includes(keyword))
  ).length
}

// 类型切换处理
function handleTypeSelect(type: SchemaSubType) {
  selectedFormulaType.value = type
}

// 获取公式类型标签
function getFormulaTypeLabel(schemaSubType: string): string {
  const option = formulaTypeOptions.value.find((opt) => opt.value === schemaSubType)
  return option?.label || schemaSubType
}

// 获取操作项
function getActions(record: FormulaDefinition): ActionItem[] {
  const id = record.id!
  return [
    {
      key: 'edit',
      label: t('admin.action.edit'),
      onClick: () => handleEdit(id),
    },
    {
      key: 'reference',
      label: t('admin.action.reference'),
      onClick: () => handleShowReference(id),
    },
    {
      key: 'history',
      label: t('admin.action.changelog'),
      onClick: () => handleShowChangelog(id, record.name),
    },
    {
      key: 'toggle',
      label: record.enabled ? t('admin.action.disable') : t('admin.action.enable'),
      onClick: () => handleToggleEnabled(record),
    },
    {
      key: 'delete',
      label: t('admin.action.delete'),
      danger: true,
      onClick: () => handleDelete(record),
    },
  ]
}

// 处理操作
function handleAction(key: string, record: FormulaDefinition) {
  const id = record.id!
  switch (key) {
    case 'edit':
      handleEdit(id)
      break
    case 'reference':
      handleShowReference(id)
      break
    case 'history':
      handleShowChangelog(id, record.name)
      break
    case 'toggle':
      handleToggleEnabled(record)
      break
    case 'delete':
      handleDelete(record)
      break
  }
}

// 创建处理
function handleCreate() {
  drawerMode.value = 'create'
  editFormulaId.value = undefined
  drawerVisible.value = true
}

// 编辑处理
function handleEdit(formulaId: string) {
  drawerMode.value = 'edit'
  editFormulaId.value = formulaId
  drawerVisible.value = true
}

// 显示引用关系
function handleShowReference(formulaId: string) {
  currentReferenceSchemaId.value = formulaId
  referenceDrawerVisible.value = true
}

// 显示审计日志
function handleShowChangelog(formulaId: string, formulaName: string) {
  currentChangelogSchemaId.value = formulaId
  currentChangelogSchemaName.value = formulaName
  changelogDrawerVisible.value = true
}

// 切换启用状态
async function handleToggleEnabled(record: FormulaDefinition) {
  const id = record.id!
  try {
    if (record.enabled) {
      await formulaDefinitionApi.disable(id)
      Message.success(t('admin.formulaDefinition.disableSuccess'))
    } else {
      await formulaDefinitionApi.activate(id)
      Message.success(t('admin.formulaDefinition.enableSuccess'))
    }
    await fetchData()
  } catch (error) {
    console.error('Failed to toggle enabled:', error)
    handleReferenceConflictError(error)
  }
}

// 删除处理
function handleDelete(record: FormulaDefinition) {
  const id = record.id!
  Modal.confirm({
    title: t('admin.message.confirmDelete'),
    content: t('admin.formulaDefinition.deleteConfirm', { name: record.name }),
    onOk: async () => {
      try {
        await formulaDefinitionApi.delete(id)
        Message.success(t('admin.message.deleteSuccess'))
        await fetchData()
      } catch (error) {
        console.error('Failed to delete formula:', error)
        handleReferenceConflictError(error)
      }
    },
  })
}

// 抽屉成功回调
function handleDrawerSuccess() {
  drawerVisible.value = false
  fetchData()
}

// 从路由参数中读取编辑ID
async function handleEditFromRoute() {
  const editId = route.query.edit as string | undefined
  if (editId) {
    handleEdit(editId)
    // 清除路由参数
    router.replace({ query: {} })
  }
}

// 列表数据加载
async function fetchData() {
  loading.value = true
  try {
    formulas.value = await formulaDefinitionApi.list()
  } catch (error) {
    console.error('Failed to fetch formula definitions:', error)
    Message.error(t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchData()
  await handleEditFromRoute()
})
</script>

<style scoped lang="scss">
.formula-definition-page {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.formula-type-sidebar {
  width: 200px;
  background: var(--color-bg-1);
  border-right: 1px solid var(--color-border);
  padding: 12px;
  overflow-y: auto;
  flex-shrink: 0;
}

.sidebar-create-btn {
  width: 100%;
  margin-bottom: 12px;
}

.type-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
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
}

.type-icon {
  flex-shrink: 0;
  font-size: 14px;
  color: var(--color-text-3);
  margin-right: 6px;
}

.type-item.active .type-icon {
  color: rgb(var(--primary-6)) !important;
}

.type-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  color: var(--color-text-1);
}

.type-item.active .type-name {
  color: rgb(var(--primary-6)) !important;
}

.type-count {
  font-size: 11px;
  color: var(--color-text-3);
  flex-shrink: 0;
}

.type-item.active .type-count {
  color: var(--color-text-3);
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

.formula-list-content {
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

:deep(.arco-input-wrapper) {
  height: 28px;
}

:deep(.arco-input) {
  font-size: 12px;
}

.cell-text {
  font-size: 13px;
  color: var(--color-text-1);
  white-space: nowrap;
}

.nowrap {
  white-space: nowrap;
}
</style>
