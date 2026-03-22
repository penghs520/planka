<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Modal, Message } from '@arco-design/web-vue'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import { schemaApi } from '@/api'
import { AdminTable } from '@/components/table'
import SchemaReferenceDrawer from '@/components/schema/SchemaReferenceDrawer.vue'
import HighlightText from '@/components/common/HighlightText.vue'
import { useTableSearch } from '@/hooks/useTableSearch'
import type { FieldConfigListWithSource, FieldConfig } from '@/types/card-type'
import {
  getSourceType,
  getSourceLabelI18n,
  getValueSourceLabelI18n,
  getFieldTypeLabelI18n,
} from '../formatters'
import { handleReferenceConflictError } from '@/utils/error-handler'

const { t } = useI18n()
const router = useRouter()

// 引用关系抽屉
const referenceDrawerVisible = ref(false)
const referenceSchemaId = ref<string>('')

// Wrapper functions to pass t() to i18n-aware formatters
function getSourceLabel(config: FieldConfig, fieldList: FieldConfigListWithSource | null): string {
  return getSourceLabelI18n(config, fieldList, t)
}

function getValueSourceLabel(valueSource?: string): string {
  return getValueSourceLabelI18n(valueSource, t)
}

function getFieldTypeLabel(schemaSubType: string): string {
  return getFieldTypeLabelI18n(schemaSubType, t)
}

/** 飞书风格字段类型标签配色 */
const fieldTypePillClassMap: Record<string, string> = {
  TEXT_FIELD: 'ft-field-pill--blue',
  MULTI_LINE_TEXT_FIELD: 'ft-field-pill--cyan',
  MARKDOWN_FIELD: 'ft-field-pill--purple',
  NUMBER_FIELD: 'ft-field-pill--orange',
  DATE_FIELD: 'ft-field-pill--pink',
  ENUM_FIELD: 'ft-field-pill--green',
  ATTACHMENT_FIELD: 'ft-field-pill--amber',
  WEB_URL_FIELD: 'ft-field-pill--blue',
  STRUCTURE_FIELD: 'ft-field-pill--violet',
  LINK_FIELD: 'ft-field-pill--teal',
}

function fieldTypePillClass(schemaSubType: string): string {
  return fieldTypePillClassMap[schemaSubType] || 'ft-field-pill--gray'
}

const props = defineProps<{
  fieldList: FieldConfigListWithSource | null
  cardTypeId: string
}>()

// 将 fields 转换为响应式数据用于搜索
const fields = computed(() => props.fieldList?.fields ?? [])

// 使用表格搜索 hook
const { searchKeyword, filteredData } = useTableSearch({
  data: fields,
  searchFields: ['name', 'code'],
})

const emit = defineEmits<{
  'open-detail': [config: FieldConfig]
  'create-new': []
  'refresh': []
}>()

// 处理来源列中实体类型链接的点击
function handleSourceClick(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (target.classList.contains('source-link') && target.dataset.cardTypeId) {
    event.preventDefault()
    // 在新标签页打开实体类型列表，并带上 edit 参数自动打开编辑抽屉
    const url = router.resolve({
      name: 'CardTypeCard',
      query: { edit: target.dataset.cardTypeId },
    }).href
    window.open(url, '_blank')
  }
}

function handleOpenFieldConfigDetail(config: FieldConfig) {
  emit('open-detail', config)
}

function handleCreateNewField() {
  emit('create-new')
}

async function handleDeleteFieldConfig(config: FieldConfig) {
  if (!config.id) return

  Modal.confirm({
    title: t('admin.cardType.fieldConfig.confirmDeleteTitle'),
    content: t('admin.cardType.fieldConfig.confirmDeleteContent', { name: config.name }),
    okText: t('admin.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        // 使用通用的 Schema 删除 API，会自动处理引用校验
        await schemaApi.delete(config.id!)
        Message.success(t('admin.cardType.fieldConfig.deleteSuccess'))
        emit('refresh')
      } catch (error) {
        if (!handleReferenceConflictError(error)) {
          console.error('Failed to delete:', error)
        }
      }
    },
  })
}
</script>

<template>
  <div v-if="fieldList && fieldList.fields.length > 0" class="fields-tab-content">
    <!-- 工具栏：右侧搜索 + 主按钮（飞书字段管理布局） -->
    <div class="field-mgmt-toolbar">
      <a-input-search
        v-model="searchKeyword"
        class="field-mgmt-search"
        :placeholder="t('admin.cardType.fieldConfig.listSearchPlaceholder')"
        allow-clear
      />
      <a-button type="primary" class="field-mgmt-create-btn btn-primary" @click="handleCreateNewField">
        <template #icon>
          <IconPlus />
        </template>
        {{ t('admin.cardType.fieldConfig.createNew') }}
      </a-button>
    </div>
    <AdminTable
      :data="filteredData"
      row-key="fieldId"
      class="field-config-table field-mgmt-table"
      :scroll="{ x: 1100, y: '100%' }"
      row-class="clickable-row field-mgmt-row"
      @row-click="(record: unknown) => handleOpenFieldConfigDetail(record as FieldConfig)"
    >
      <a-table-column :title="t('admin.cardType.fieldConfig.fieldName')" data-index="name" :min-width="168" fixed="left">
        <template #cell="{ record }">
          <div class="field-name-cell">
            <a-link class="name-link" @click="handleOpenFieldConfigDetail(record)">
              <HighlightText :text="record.name" :keyword="searchKeyword" />
            </a-link>
          </div>
        </template>
      </a-table-column>
      <a-table-column :title="t('admin.cardType.fieldConfig.fieldType')" :min-width="128">
        <template #cell="{ record }">
          <span
            class="ft-field-pill"
            :class="fieldTypePillClass(record.schemaSubType)"
          >{{ getFieldTypeLabel(record.schemaSubType) }}</span>
        </template>
      </a-table-column>
      <a-table-column :title="t('admin.table.code')" data-index="fieldId" :min-width="120">
        <template #cell="{ record }">
          <span class="cell-text wrap-text">{{ record.code }}</span>
        </template>
      </a-table-column>
      <a-table-column :title="t('admin.cardType.fieldConfig.source')" :min-width="180">
        <template #cell="{ record }">
          <div class="source-cell" @click="handleSourceClick">
            <span class="source-text wrap-text" v-html="getSourceLabel(record, fieldList)" />
          </div>
        </template>
      </a-table-column>
      <a-table-column :title="t('admin.cardType.fieldConfig.valueSource')" :min-width="100">
        <template #cell="{ record }">
          <span class="cell-text wrap-text">{{ getValueSourceLabel(record.valueSource) }}</span>
        </template>
      </a-table-column>
      <a-table-column :title="t('admin.cardType.fieldConfig.systemField')" :min-width="90">
        <template #cell="{ record }">
          <span class="cell-text wrap-text">{{ record.systemField ? t('common.yes') : t('common.no') }}</span>
        </template>
      </a-table-column>
      <a-table-column :title="t('admin.table.operations')" :width="100" fixed="right">
        <template #cell="{ record }">
          <a-button
            v-if="getSourceType(record, fieldList) === 'own-config'"
            type="text"
            size="mini"
            status="danger"
            @click.stop="handleDeleteFieldConfig(record)"
          >
            {{ t('admin.action.delete') }}
          </a-button>
          <span v-else class="text-placeholder">-</span>
        </template>
      </a-table-column>
    </AdminTable>
  </div>
  <div v-else class="empty-container">
    <a-empty :description="t('admin.cardType.fieldConfig.emptyDescription')" />
    <a-button type="primary" class="btn-primary mt-4" @click="handleCreateNewField">
      <template #icon>
        <IconPlus />
      </template>
      {{ t('admin.cardType.fieldConfig.createNew') }}
    </a-button>
  </div>

  <!-- 引用关系抽屉 -->
  <SchemaReferenceDrawer
    v-model:visible="referenceDrawerVisible"
    :schema-id="referenceSchemaId"
  />
</template>

<style scoped>
.fields-tab-content {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 与 AdminTable 根节点合并 class，保证在 flex 链上占满剩余高度 */
.field-config-table {
  flex: 1;
  min-height: 0;
}

.field-mgmt-toolbar {
  display: flex;
  flex-shrink: 0;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding: 0 0 12px;
}

.field-mgmt-search {
  width: 280px;
}

.field-mgmt-search :deep(.arco-input-wrapper) {
  border-radius: var(--radius-md);
  background-color: var(--color-fill-2);
  border: 1px solid transparent;
}

.field-mgmt-search :deep(.arco-input-wrapper:hover),
.field-mgmt-search :deep(.arco-input-wrapper.arco-input-focus) {
  background-color: var(--color-bg-2);
  border-color: var(--color-border-2);
}

.field-mgmt-create-btn {
  border-radius: var(--radius-md);
  font-weight: 500;
}

.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.mt-4 {
  margin-top: 16px;
}

.field-name-cell {
  min-width: 0;
  white-space: nowrap;
}

/* 字段类型彩色标签（飞书式淡底 + 圆角） */
.ft-field-pill {
  display: inline-block;
  max-width: 100%;
  padding: 2px 8px;
  font-size: 12px;
  line-height: 20px;
  font-weight: 500;
  border-radius: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.ft-field-pill--blue {
  color: rgb(var(--primary-7));
  background: rgb(var(--primary-1));
}

.ft-field-pill--cyan {
  color: rgb(var(--cyan-7));
  background: rgb(var(--cyan-1));
}

.ft-field-pill--purple {
  color: rgb(var(--purple-7));
  background: rgb(var(--purple-1));
}

.ft-field-pill--orange {
  color: rgb(var(--orange-7));
  background: rgb(var(--orange-1));
}

.ft-field-pill--pink {
  color: rgb(var(--magenta-7));
  background: rgb(var(--magenta-1));
}

.ft-field-pill--green {
  color: rgb(var(--green-7));
  background: rgb(var(--green-1));
}

.ft-field-pill--amber {
  color: rgb(var(--gold-7));
  background: rgb(var(--gold-1));
}

.ft-field-pill--violet {
  color: #5c3ecf;
  background: #f3f0ff;
}

.ft-field-pill--teal {
  color: #0d7a7a;
  background: #e6f7f7;
}

.ft-field-pill--gray {
  color: var(--color-text-3);
  background: var(--color-fill-2);
}

.source-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.source-text {
  font-size: 13px;
  color: var(--color-text-2);
  word-break: break-word;
}

.source-text :deep(.source-link) {
  color: var(--color-text-2);
  font-weight: normal;
  cursor: pointer;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.cell-text {
  color: var(--color-text-2);
}

.wrap-text {
  word-break: break-word;
  white-space: normal;
}

.text-ellipsis {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
}

.text-placeholder {
  color: var(--color-text-4);
}

.name-link {
  font-weight: 500;
}

/* 表格：仅横向分割线、表头与内容同色底、行 hover */
.field-mgmt-table :deep(.arco-table-header) {
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}

.field-mgmt-table :deep(.arco-table-th) {
  font-weight: 500;
  color: var(--color-text-2);
  background-color: var(--color-main-panel) !important;
  border-bottom: 1px solid var(--color-border-2) !important;
}

.field-mgmt-table :deep(.arco-table-td) {
  border-bottom: 1px solid var(--color-border-2);
}

.field-mgmt-table :deep(.arco-table-tr:hover .arco-table-td) {
  background-color: var(--color-fill-1) !important;
}

.field-mgmt-table :deep(.clickable-row) {
  cursor: pointer;
}

/* 减轻固定列 + 纵向滚动条区域的多余竖线感 */
.field-mgmt-table :deep(.arco-table-content-scroll) {
  border-right: none;
}
</style>
