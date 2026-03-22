<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
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
import { linkTypeApi, cardTypeApi } from '@/api'
import CardTypeSelect from '@/components/card-type/CardTypeSelect.vue'
import HighlightText from '@/components/common/HighlightText.vue'
import CreateButton from '@/components/common/CreateButton.vue'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import SchemaReferenceDrawer from '@/components/schema/SchemaReferenceDrawer.vue'
import SchemaChangelogDrawer from '@/components/schema/SchemaChangelogDrawer.vue'
import { AdminTable, AdminTableActions } from '@/components/table'
import ViewSwitcher from '@/components/link-type/ViewSwitcher.vue'
import type {
  LinkTypeVO,
  CreateLinkTypeRequest,
  UpdateLinkTypeRequest,
} from '@/types/link-type'
import type { ActionItem } from '@/types/table'
import { useResponsiveColumnWidth } from '@/hooks/useResponsiveColumnWidth'
import { formatDateTime } from '@/utils/format'

const { t } = useI18n()

const router = useRouter()
const route = useRoute()
const { columnWidth, scrollXExtra } = useResponsiveColumnWidth()

// 列表相关状态
const loading = ref(false)
const linkTypes = ref<LinkTypeVO[]>([])
const searchKeyword = ref('')

// 实体类型数据（用于动态标签显示）
interface CardTypeOption {
  id: string
  name: string
  schemaSubType: string
}
const cardTypes = ref<CardTypeOption[]>([])

// 新建/编辑抽屉状态
const drawerVisible = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')

// 引用关系抽屉状态
const referenceDrawerVisible = ref(false)
const currentReferenceSchemaId = ref<string | undefined>(undefined)

// 审计日志抽屉状态
const changelogDrawerVisible = ref(false)
const currentChangelogSchemaId = ref<string | undefined>(undefined)
const currentChangelogSchemaName = ref<string | undefined>(undefined)

const formData = ref<CreateLinkTypeRequest | (UpdateLinkTypeRequest & { id: string })>({
  name: '',
  sourceName: '',
  targetName: '',
  sourceMultiSelect: false,
  targetMultiSelect: false,
})
const submitting = ref(false)
const editingId = ref<string | null>(null)
const editingVersion = ref<number>(1)

// 过滤后的列表
const filteredList = computed(() => {
  if (!searchKeyword.value) return linkTypes.value
  const keyword = searchKeyword.value.toLowerCase()
  return linkTypes.value.filter(
    (lt) =>
      lt.sourceName?.toLowerCase().includes(keyword) ||
      lt.targetName?.toLowerCase().includes(keyword) ||
      lt.sourceCardTypes?.some((ct) => ct.name.toLowerCase().includes(keyword)) ||
      lt.targetCardTypes?.some((ct) => ct.name.toLowerCase().includes(keyword)),
  )
})

const drawerTitle = computed(() => {
  return drawerMode.value === 'create' ? t('admin.linkType.createTitle') : t('admin.linkType.editTitle')
})

// 获取选中的源侧实体类型名称
const sourceCardTypeName = computed(() => {
  if (!formData.value.sourceCardTypeIds?.length) return ''
  const ids = formData.value.sourceCardTypeIds
  const names = cardTypes.value
    .filter(ct => ids.includes(ct.id))
    .map(ct => ct.name)
  return names.join('、') || ''
})

// 获取选中的对侧实体类型名称
const targetCardTypeName = computed(() => {
  if (!formData.value.targetCardTypeIds?.length) return ''
  const ids = formData.value.targetCardTypeIds
  const names = cardTypes.value
    .filter(ct => ids.includes(ct.id))
    .map(ct => ct.name)
  return names.join('、') || ''
})

// 列表数据加载
async function fetchData() {
  loading.value = true
  try {
    linkTypes.value = await linkTypeApi.list()
  } catch (error) {
    console.error('Failed to fetch link types:', error)
  } finally {
    loading.value = false
  }
}

// 获取实体类型列表（用于动态标签）
async function fetchCardTypes() {
  try {
    cardTypes.value = await cardTypeApi.listOptions()
  } catch (error) {
    console.error('Failed to fetch card types:', error)
  }
}


// 打开新建抽屉
function handleCreate() {
  drawerMode.value = 'create'
  editingId.value = null
  formData.value = {
    name: '',
    sourceName: '',
    targetName: '',
    sourceMultiSelect: false,
    targetMultiSelect: false,
  }
  drawerVisible.value = true
}

// 打开编辑抽屉
async function handleEdit(linkType: LinkTypeVO) {
  if (!linkType.id) return
  drawerMode.value = 'edit'
  editingId.value = linkType.id
  editingVersion.value = linkType.contentVersion
  try {
    const detail = await linkTypeApi.getById(linkType.id)
    formData.value = {
      id: detail.id,
      name: detail.name,
      description: detail.description,
      sourceName: detail.sourceName,
      targetName: detail.targetName,
      sourceCardTypeIds: detail.sourceCardTypes?.map((ct) => ct.id),
      targetCardTypeIds: detail.targetCardTypes?.map((ct) => ct.id),
      sourceMultiSelect: detail.sourceMultiSelect,
      targetMultiSelect: detail.targetMultiSelect,
      enabled: detail.enabled,
      expectedVersion: detail.contentVersion,
    }
    drawerVisible.value = true
  } catch (error) {
    console.error('Failed to fetch link type:', error)
    Message.error(t('admin.message.fetchFailed', { type: t('admin.linkType.title') }))
  }
}

// 提交表单
async function handleSubmit() {
  // 验证必填项
  if (!formData.value.sourceName || !formData.value.targetName) {
    Message.error(t('admin.linkType.requiredSourceTargetName'))
    return
  }
  if (!formData.value.sourceCardTypeIds || formData.value.sourceCardTypeIds.length === 0) {
    Message.error(t('admin.linkType.requiredSourceCardType'))
    return
  }
  if (!formData.value.targetCardTypeIds || formData.value.targetCardTypeIds.length === 0) {
    Message.error(t('admin.linkType.requiredTargetCardType'))
    return
  }

  submitting.value = true
  try {
    if (drawerMode.value === 'create') {
      await linkTypeApi.create(formData.value as CreateLinkTypeRequest)
      Message.success(t('admin.message.createSuccess'))
    } else {
      if (!editingId.value) return
      await linkTypeApi.update(editingId.value, {
        ...formData.value,
        expectedVersion: editingVersion.value,
      } as UpdateLinkTypeRequest)
      Message.success(t('admin.message.saveSuccess'))
    }
    drawerVisible.value = false
    await fetchData()
  } catch (error) {
    console.error('Failed to submit:', error)
  } finally {
    submitting.value = false
  }
}

// 打开引用关系抽屉
function handleReference(linkType: LinkTypeVO) {
  if (!linkType.id) return
  currentReferenceSchemaId.value = linkType.id
  referenceDrawerVisible.value = true
}

// 打开审计日志抽屉
function handleChangelog(linkType: LinkTypeVO) {
  if (!linkType.id) return
  currentChangelogSchemaId.value = linkType.id
  currentChangelogSchemaName.value = linkType.name
  changelogDrawerVisible.value = true
}

import { handleReferenceConflictError } from '@/utils/error-handler'

// 删除关联类型
async function handleDelete(linkType: LinkTypeVO) {
  if (!linkType.id) return
  const linkTypeId = linkType.id
  Modal.confirm({
    title: t('admin.message.confirmDelete'),
    content: t('admin.message.deleteConfirmContent', { type: t('admin.linkType.title'), name: linkType.name }),
    okText: t('admin.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await linkTypeApi.delete(linkTypeId)
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
async function handleToggleEnabled(linkType: LinkTypeVO) {
  if (!linkType.id) return
  try {
    await linkTypeApi.update(linkType.id, {
      enabled: !linkType.enabled,
      expectedVersion: linkType.contentVersion,
    })
    Message.success(linkType.enabled ? t('admin.message.disableSuccess') : t('admin.message.enableSuccess'))
    await fetchData()
  } catch (error) {
    console.error('Failed to toggle enabled:', error)
  }
}

// 处理路由参数中的编辑请求
async function handleEditFromRoute() {
  const editId = route.query.edit as string | undefined

  if (editId) {
    // 清除路由参数，避免刷新后重复打开
    router.replace({ path: '/admin/link-type', query: {} })

    // 查找对应的关联类型
    const linkType = linkTypes.value.find((lt) => lt.id === editId)
    if (linkType) {
      await handleEdit(linkType)
    } else {
      // 如果列表中没有，直接用 ID 获取详情并打开编辑
      try {
        const detail = await linkTypeApi.getById(editId)
        await handleEdit(detail as LinkTypeVO)
      } catch (error) {
        console.error('Failed to fetch link type:', error)
        Message.error(t('admin.message.fetchFailed', { type: t('admin.linkType.title') }))
      }
    }
    // TODO: 如果后续有tab功能，可以在这里处理 targetTab
  }
}

// 获取操作项列表
function getActions(record: LinkTypeVO): ActionItem[] {
  return [
    { key: 'edit', label: t('admin.action.edit'), icon: IconEdit, visible: !record.systemLinkType },
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
      visible: !record.systemLinkType,
    },
  ]
}

// 处理操作项点击
function handleAction(key: string, record: LinkTypeVO) {
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

onMounted(async () => {
  await Promise.all([fetchData(), fetchCardTypes()])
  await handleEditFromRoute()
})
</script>

<template>
  <div class="link-type-page">
    <!-- 顶部工具栏 -->
    <div class="link-type-content">
      <!-- 顶部工具栏 -->
      <div class="content-header">
          <ViewSwitcher />
          <div class="header-divider"></div>
          <a-input-search
            v-model="searchKeyword"
            :placeholder="t('admin.search.namePlaceholder')"
            style="width: 260px"
            size="small"
            allow-clear
          />
          <div class="header-spacer"></div>
          <CreateButton @click="handleCreate">
            {{ t('admin.linkType.createButton') }}
          </CreateButton>
      </div>

      <!-- 关联类型表格 -->
      <AdminTable
        :data="filteredList"
        :loading="loading"
        :scroll="{ x: 1200 + scrollXExtra, y: '100%' }"
        row-class="clickable-row"
        @row-click="(record: unknown) => handleEdit(record as LinkTypeVO)"
      >
        <a-table-column :title="t('admin.linkType.sourceName')" data-index="sourceName" :width="180">
          <template #cell="{ record }">
            <span class="name-text">
              <HighlightText :text="record.sourceName" :keyword="searchKeyword" />
            </span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.linkType.targetName')" data-index="targetName" :width="180">
          <template #cell="{ record }">
            <span class="name-text">
              <HighlightText :text="record.targetName" :keyword="searchKeyword" />
            </span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.linkType.sourceCardType')" :width="180">
          <template #cell="{ record }">
            <template v-if="record.sourceCardTypes && record.sourceCardTypes.length">
              <span v-for="(ct, index) in record.sourceCardTypes" :key="ct.id">
                <HighlightText :text="ct.name" :keyword="searchKeyword" />
                <span v-if="index < record.sourceCardTypes.length - 1">、</span>
              </span>
            </template>
            <span v-else>{{ t('admin.status.noLimit') }}</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.linkType.targetCardType')" :width="180">
          <template #cell="{ record }">
            <template v-if="record.targetCardTypes && record.targetCardTypes.length">
              <span v-for="(ct, index) in record.targetCardTypes" :key="ct.id">
                <HighlightText :text="ct.name" :keyword="searchKeyword" />
                <span v-if="index < record.targetCardTypes.length - 1">、</span>
              </span>
            </template>
            <span v-else>{{ t('admin.status.noLimit') }}</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.status')" :width="columnWidth.status">
          <template #cell="{ record }">
            <span class="cell-text">
              {{ record.enabled ? t('admin.status.enabled') : t('admin.status.disabled') }}
            </span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.systemBuiltin')" :width="columnWidth.systemBuiltin + 20">
          <template #cell="{ record }">
            <span v-if="record.systemLinkType" class="cell-text">{{ t('admin.status.yes') }}</span>
            <span v-else>-</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.updatedAt')" :width="180">
          <template #cell="{ record }">
            <span class="nowrap time-column">{{ formatDateTime(record.updatedAt) }}</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.createdAt')" :width="180">
          <template #cell="{ record }">
            <span class="nowrap time-column">{{ formatDateTime(record.createdAt) }}</span>
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
          <a-empty :description="t('admin.linkType.emptyDescription')" />
        </template>
      </AdminTable>
    </div>

    <!-- 新建/编辑抽屉 -->
    <a-drawer
      v-model:visible="drawerVisible"
      :width="600"
      :mask-closable="true"
      :esc-to-close="true"
      unmount-on-close
    >
      <template #title>
        {{ drawerTitle }}
      </template>
      <a-form class="link-type-edit-form" :model="formData" layout="vertical">

        <!-- 1. 先选择实体类型 -->
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('admin.linkType.sourceCardTypeLabel')" required>
              <CardTypeSelect
                v-model="formData.sourceCardTypeIds"
                :placeholder="t('admin.linkType.selectCardType')"
                :limit-concrete-single="true"
                :options="cardTypes"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('admin.linkType.targetCardTypeLabel')" required>
              <CardTypeSelect
                v-model="formData.targetCardTypeIds"
                :placeholder="t('admin.linkType.selectCardType')"
                :limit-concrete-single="true"
                :options="cardTypes"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 2. 再填写关系名称 -->
        <a-row :gutter="16" class="name-row">
          <a-col :span="12">
            <a-form-item required>
              <template #label>
                <span v-if="sourceCardTypeName && targetCardTypeName" class="dynamic-label">
                  从 <span class="type-highlight">{{ sourceCardTypeName }}</span> 看 <span class="type-highlight">{{ targetCardTypeName }}</span> 时显示为
                </span>
                <span v-else>{{ t('admin.linkType.sourceName') }} <span class="label-hint">{{ t('admin.linkType.sourceNameHint') }}</span></span>
              </template>
              <a-input
                v-model="formData.sourceName"
                :placeholder="t('admin.linkType.sourceNamePlaceholder', { target: targetCardTypeName || 'Epic' })"
                :max-length="20"
                :disabled="!sourceCardTypeName || !targetCardTypeName"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item required>
              <template #label>
                <span v-if="sourceCardTypeName && targetCardTypeName" class="dynamic-label">
                  从 <span class="type-highlight">{{ targetCardTypeName }}</span> 看 <span class="type-highlight">{{ sourceCardTypeName }}</span> 时显示为
                </span>
                <span v-else>{{ t('admin.linkType.targetName') }} <span class="label-hint">{{ t('admin.linkType.sourceNameHint') }}</span></span>
              </template>
              <a-input
                v-model="formData.targetName"
                :placeholder="t('admin.linkType.targetNamePlaceholder', { source: sourceCardTypeName || 'User Story' })"
                :max-length="20"
                :disabled="!sourceCardTypeName || !targetCardTypeName"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16" class="multi-select-row">
          <a-col :span="12">
            <div class="setting-item">
              <div class="setting-label">
                <span v-if="sourceCardTypeName && formData.sourceName" class="setting-title">
                  在 <span class="type-highlight">{{ sourceCardTypeName }}</span> 中的 <span class="link-highlight">{{ formData.sourceName }}</span> 可多选
                </span>
                <span v-else class="setting-title">{{ t('admin.linkType.sourceMultiSelect') }}</span>
              </div>
              <a-switch v-model="formData.sourceMultiSelect" />
            </div>
          </a-col>
          <a-col :span="12">
            <div class="setting-item">
              <div class="setting-label">
                <span v-if="targetCardTypeName && formData.targetName" class="setting-title">
                  在 <span class="type-highlight">{{ targetCardTypeName }}</span> 中的 <span class="link-highlight">{{ formData.targetName }}</span> 可多选
                </span>
                <span v-else class="setting-title">{{ t('admin.linkType.targetMultiSelect') }}</span>
              </div>
              <a-switch v-model="formData.targetMultiSelect" />
            </div>
          </a-col>
        </a-row>

      <a-form-item :label="t('admin.linkType.descriptionLabel')" class="description-item">
          <a-textarea
            v-model="formData.description"
            :placeholder="t('admin.linkType.descriptionPlaceholder')"
            :max-length="200"
            :auto-size="{ minRows: 2, maxRows: 4 }"
          />
        </a-form-item>
      </a-form>

      <template #footer>
        <div class="drawer-footer">
          <a-space>
            <CancelButton @click="drawerVisible = false" />
            <SaveButton
              :loading="submitting"
              :text="drawerMode === 'create' ? t('admin.action.create') : t('admin.action.save')"
              @click="handleSubmit"
            />
          </a-space>
        </div>
      </template>
    </a-drawer>

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

<style scoped>
.link-type-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.link-type-content {
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

.header-divider {
  width: 1px;
  height: 20px;
  background: var(--color-border-2);
  margin: 0 12px;
}

.form-hint {
  display: block;
  font-size: 11px;
  color: var(--color-text-3);
  margin-top: 4px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #E5E6EB;
  border-radius: 8px;
  transition: all 0.2s;
}

.setting-item:hover {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(51, 112, 255, 0.1);
}

.setting-label {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.setting-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
  line-height: 1.4;
}

.form-extra-text {
  font-size: 11px;
  color: var(--color-text-3);
  line-height: 1.4;
}

/* 关联类型编辑表单：统一行距与表单项间距 */
.link-type-edit-form > .arco-row + .arco-row {
  margin-top: 12px;
}

.description-item {
  margin-top: 16px;
}

.link-type-edit-form :deep(.arco-form-item) {
  margin-bottom: 12px;
}

.link-type-edit-form :deep(.arco-form-item:last-child) {
  margin-bottom: 0;
}

.link-type-edit-form :deep(.arco-form-item-label) {
  font-weight: 600;
  line-height: 1.45;
  margin-bottom: 4px;
}

.example-section {
  margin-bottom: 20px;
}

.example-section:last-child {
  margin-bottom: 0;
}

.example-title {
  font-size: 13px;
  font-weight: 600;
  color: rgb(var(--primary-6));
  margin-bottom: 10px;
}

.example-config {
  background: var(--color-fill-1);
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 10px;
}

.example-item {
  margin: 6px 0;
  font-size: 12px;
}

.example-label {
  color: var(--color-text-3);
  font-size: 11px;
  display: inline-block;
  min-width: 90px;
}

.example-value {
  color: var(--color-text-1);
  font-weight: 600;
  background: rgb(var(--primary-1));
  color: rgb(var(--primary-6));
  padding: 2px 8px;
  border-radius: 3px;
}

.example-text {
  color: var(--color-text-2);
  font-weight: 500;
}

.example-divider {
  height: 1px;
  background: var(--color-border-2);
  margin: 10px 0;
}

.example-scenario {
  margin-bottom: 10px;
}

.scenario-title {
  font-weight: 600;
  color: var(--color-text-2);
  margin-bottom: 4px;
  font-size: 12px;
}

.scenario-desc {
  color: var(--color-text-3);
  font-size: 11px;
  line-height: 1.5;
  padding: 6px 10px;
  background: var(--color-fill-1);
  border-radius: 3px;
  border-left: 2px solid rgb(var(--primary-6));
}

.example-result {
  margin-top: 8px;
}

.result-title {
  font-weight: 600;
  color: var(--color-text-2);
  margin-bottom: 8px;
  font-size: 12px;
}

.result-item {
  margin: 8px 0;
  padding: 8px 10px;
  background: var(--color-fill-1);
  border-radius: 4px;
  border-left: 3px solid rgb(var(--success-6));
}

.result-card {
  font-size: 11px;
  font-weight: 600;
  color: rgb(var(--success-6));
  margin-bottom: 4px;
}

.result-text {
  font-size: 12px;
  color: var(--color-text-1);
  margin-bottom: 2px;
}

.result-explain {
  font-size: 11px;
  color: var(--color-text-3);
  font-style: italic;
}

/* 动态标签样式 */
.dynamic-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  line-height: 1.8;
}

.dynamic-label :deep(.arco-tag) {
  margin: 0 2px;
}

.label-hint {
  font-size: 11px;
  color: var(--color-text-3);
  font-weight: normal;
}

/* 高亮文字样式 */
.hl-blue {
  color: rgb(var(--primary-6));
  font-weight: 500;
}

.hl-green {
  color: rgb(var(--success-6));
  font-weight: 500;
}

.hl-orange {
  color: rgb(var(--warning-6));
  font-weight: 500;
}

/* 可点击行样式 */
:deep(.clickable-row) {
  cursor: pointer;
}

:deep(.clickable-row:hover) {
  background-color: var(--color-fill-2);
}

.cell-text {
  font-size: 13px;
  color: var(--color-text-1);
  white-space: nowrap;
}

/* 实体类型名称高亮 */
.type-highlight {
  color: var(--color-primary);
  font-weight: 500;
}

/* 关联名称高亮 */
.link-highlight {
  color: var(--color-primary);
  font-weight: 500;
}

/* 抽屉底部按钮区域 */
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #E5E6EB;
  margin-top: 12px;
}

.name-text {
  font-weight: 500;
}
</style>
