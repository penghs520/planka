<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconEdit,
  IconDelete,
  IconCopy,
  IconMore,
  IconCheckCircle,
  IconMinusCircle,
  IconStar,
  IconStarFill,
} from '@arco-design/web-vue/es/icon'
import { cardCreatePageTemplateApi } from '@/api/card-create-page-template'
import CreateButton from '@/components/common/CreateButton.vue'
import type { CreatePageTemplateListItemVO } from '@/types/card-create-page-template'
import { handleReferenceConflictError } from '@/utils/error-handler'
import { formatDateTime } from '@/utils/format'
import CreatePageTemplateDrawer from './CreatePageTemplateDrawer.vue'

const { t } = useI18n()

const props = defineProps<{
  cardTypeId: string
  cardTypeName?: string
}>()

// const { columnWidth } = useResponsiveColumnWidth()

const loading = ref(false)
const templates = ref<CreatePageTemplateListItemVO[]>([])

async function fetchTemplates(): Promise<void> {
  if (!props.cardTypeId) return

  loading.value = true
  try {
    templates.value = await cardCreatePageTemplateApi.list(props.cardTypeId)
  } catch (error: any) {
    console.error('Failed to fetch templates:', error)
    Message.error(error.message || t('admin.cardType.createPageTemplate.loadFailed'))
  } finally {
    loading.value = false
  }
}

const drawerVisible = ref(false)
const currentTemplateId = ref<string | undefined>(undefined)

function handleCreate(): void {
  currentTemplateId.value = undefined
  drawerVisible.value = true
}

function handleEdit(template: CreatePageTemplateListItemVO): void {
  currentTemplateId.value = template.id
  drawerVisible.value = true
}

function handleDrawerSuccess(): void {
  fetchTemplates()
}

async function handleCopy(template: CreatePageTemplateListItemVO): Promise<void> {
  try {
    await cardCreatePageTemplateApi.copy(template.id, t('admin.cardType.createPageTemplate.copyName', { name: template.name }))
    Message.success(t('admin.cardType.createPageTemplate.copySuccess'))
    await fetchTemplates()
  } catch (error: any) {
    console.error('Failed to copy template:', error)
    Message.error(error.message || t('admin.cardType.createPageTemplate.copyFailed'))
  }
}

function handleDelete(template: CreatePageTemplateListItemVO): void {
  if (template.systemTemplate) {
    Message.warning(t('admin.cardType.createPageTemplate.deleteSystemNotAllowed'))
    return
  }
  Modal.confirm({
    title: t('admin.cardType.createPageTemplate.confirmDelete'),
    content: t('admin.cardType.createPageTemplate.deleteConfirmContent', { name: template.name }),
    okText: t('admin.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await cardCreatePageTemplateApi.delete(template.id)
        Message.success(t('admin.message.deleteSuccess'))
        await fetchTemplates()
      } catch (error: any) {
        if (!handleReferenceConflictError(error)) {
          console.error('Failed to delete template:', error)
          Message.error(error.message || t('admin.cardType.createPageTemplate.deleteFailed'))
        }
      }
    },
  })
}

async function handleToggleEnabled(template: CreatePageTemplateListItemVO): Promise<void> {
  try {
    if (template.enabled) {
      await cardCreatePageTemplateApi.disable(template.id)
      Message.success(t('admin.cardType.createPageTemplate.disableSuccess'))
    } else {
      await cardCreatePageTemplateApi.activate(template.id)
      Message.success(t('admin.cardType.createPageTemplate.enableSuccess'))
    }
    await fetchTemplates()
  } catch (error: any) {
    console.error('Failed to toggle enabled:', error)
    Message.error(error.message || t('admin.cardType.createPageTemplate.operationFailed'))
  }
}

async function handleSetDefault(template: CreatePageTemplateListItemVO): Promise<void> {
  if (template.isDefault) return
  try {
    await cardCreatePageTemplateApi.setDefault(template.id)
    Message.success(t('admin.cardType.createPageTemplate.setDefaultSuccess'))
    await fetchTemplates()
  } catch (error: any) {
    console.error('Failed to set default:', error)
    Message.error(error.message || t('admin.cardType.createPageTemplate.setDefaultFailed'))
  }
}


watch(
  () => props.cardTypeId,
  (newId) => {
    if (newId) {
      fetchTemplates()
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="create-page-template-tab">
    <!-- 头部操作栏（仅有数据时显示） -->
    <div v-if="templates.length > 0" class="tab-header">
      <CreateButton @click="handleCreate">{{ t('admin.cardType.createPageTemplate.createButton') }}</CreateButton>
    </div>

    <!-- 模板列表 -->
    <a-spin :loading="loading" class="template-list">
      <a-table
        v-if="templates.length > 0"
        :data="templates"
        :pagination="false"
        :bordered="false"
        row-key="id"
        size="small"
      >
        <template #columns>
          <a-table-column :title="t('admin.cardType.createPageTemplate.templateName')" data-index="name" :width="200">
            <template #cell="{ record }">
              <a-link @click="handleEdit(record)">{{ record.name }}</a-link>
              <a-tag v-if="record.systemTemplate" size="small" color="gray" style="margin-left: 4px">
                {{ t('admin.cardType.createPageTemplate.system') }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.cardType.createPageTemplate.default')" :width="70" align="center">
            <template #cell="{ record }">
              <a-tooltip :content="record.isDefault ? t('admin.cardType.createPageTemplate.defaultTemplate') : t('admin.cardType.createPageTemplate.clickToSetDefault')">
                <span
                  class="default-icon"
                  :class="{ 'is-default': record.isDefault }"
                  @click="handleSetDefault(record)"
                >
                  <IconStarFill v-if="record.isDefault" />
                  <IconStar v-else />
                </span>
              </a-tooltip>
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.cardType.createPageTemplate.fieldCount')" data-index="fieldCount" :width="80" align="center" />
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
          <a-table-column :title="t('admin.table.operations')" :width="100" fixed="right">
            <template #cell="{ record }">
              <a-dropdown trigger="click">
                <a-button type="text" size="small">
                  <template #icon><IconMore /></template>
                </a-button>
                <template #content>
                  <a-doption @click="handleEdit(record)">
                    <IconEdit /> {{ t('admin.action.edit') }}
                  </a-doption>
                  <a-doption @click="handleCopy(record)">
                    <IconCopy /> {{ t('admin.action.copy') }}
                  </a-doption>
                  <a-doption :disabled="record.isDefault" @click="handleSetDefault(record)">
                    <IconStarFill v-if="record.isDefault" />
                    <IconStar v-else />
                    {{ record.isDefault ? t('admin.cardType.createPageTemplate.defaultTemplate') : t('admin.cardType.createPageTemplate.setAsDefault') }}
                  </a-doption>
                  <a-doption @click="handleToggleEnabled(record)">
                    <IconMinusCircle v-if="record.enabled" />
                    <IconCheckCircle v-else />
                    {{ record.enabled ? t('admin.action.disable') : t('admin.action.enable') }}
                  </a-doption>
                  <a-divider style="margin: 4px 0" />
                  <a-doption
                    class="danger-option"
                    :disabled="record.systemTemplate"
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
        <a-empty :description="t('admin.cardType.createPageTemplate.emptyDescription')" />
        <CreateButton @click="handleCreate">{{ t('admin.cardType.createPageTemplate.createButton') }}</CreateButton>
      </div>
    </a-spin>

    <CreatePageTemplateDrawer
      v-model:visible="drawerVisible"
      :template-id="currentTemplateId"
      :card-type-id="cardTypeId"
      :card-type-name="cardTypeName"
      @success="handleDrawerSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
.create-page-template-tab {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tab-header {
  display: flex;
  justify-content: flex-end;
}

.template-list {
  min-height: 200px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 40px 0;
}

.default-icon {
  cursor: pointer;
  color: var(--color-text-4);
  font-size: 16px;
  transition: color 0.2s;

  &:hover {
    color: rgb(var(--warning-6));
  }

  &.is-default {
    color: rgb(var(--warning-6));
    cursor: default;
  }
}

:deep(.danger-option) {
  color: rgb(var(--danger-6));
}
</style>
