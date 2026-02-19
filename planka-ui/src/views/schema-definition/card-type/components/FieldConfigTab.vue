<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Modal, Message } from '@arco-design/web-vue'
import { schemaApi } from '@/api'
import FieldTypeIcon from '@/components/field/FieldTypeIcon.vue'
import { AdminTable } from '@/components/table'
import SchemaReferenceDrawer from '@/components/schema/SchemaReferenceDrawer.vue'
import CreateButton from '@/components/common/CreateButton.vue'
import HighlightText from '@/components/common/HighlightText.vue'
import { useTableSearch } from '@/hooks/useTableSearch'
import type { FieldConfigListWithSource, FieldConfig } from '@/types/card-type'
import {
  getSourceType,
  getSourceLabelI18n,
  getValueSourceLabelI18n,
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

// 处理来源列中卡片类型链接的点击
function handleSourceClick(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (target.classList.contains('source-link') && target.dataset.cardTypeId) {
    event.preventDefault()
    // 在新标签页打开卡片类型列表，并带上 edit 参数自动打开编辑抽屉
    const url = router.resolve({
      name: 'CardTypeList',
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
    <!-- 工具栏 -->
    <div class="toolbar">
      <a-input-search
        v-model="searchKeyword"
        :placeholder="t('admin.search.placeholder')"
        style="width: 200px"
        size="small"
        allow-clear
      />
      <CreateButton @click="handleCreateNewField">
        {{ t('admin.cardType.fieldConfig.createNew') }}
      </CreateButton>
    </div>
    <AdminTable
      :data="filteredData"
      row-key="fieldId"
      class="field-config-table"
      :scroll="{ y: '100%' }"
    >
      <a-table-column :title="t('admin.cardType.fieldConfig.fieldName')" data-index="name" :min-width="160" fixed="left">
        <template #cell="{ record }">
          <div class="field-name-cell">
            <FieldTypeIcon :field-type="record.schemaSubType" size="small" />
            <a-link @click="handleOpenFieldConfigDetail(record)">
              <HighlightText :text="record.name" :keyword="searchKeyword" />
            </a-link>
          </div>
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
            @click="handleDeleteFieldConfig(record)"
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
    <CreateButton style="margin-top: 16px;" @click="handleCreateNewField">
      {{ t('admin.cardType.fieldConfig.createNew') }}
    </CreateButton>
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
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
}

.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.field-name-cell {
  display: flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
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
</style>
