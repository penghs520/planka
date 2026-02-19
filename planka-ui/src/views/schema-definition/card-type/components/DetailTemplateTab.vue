<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
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
import { cardDetailTemplateApi } from '@/api/card-detail-template'
import CreateButton from '@/components/common/CreateButton.vue'
import type { TemplateListItemVO } from '@/types/card-detail-template'
import { formatDateTime } from '@/utils/format'

const { t } = useI18n()

const props = defineProps<{
  cardTypeId: string
  cardTypeName?: string
}>()

const router = useRouter()
// const { columnWidth } = useResponsiveColumnWidth()

const loading = ref(false)
const templates = ref<TemplateListItemVO[]>([])

// 加载模板列表
async function fetchTemplates() {
  if (!props.cardTypeId) return

  loading.value = true
  try {
    templates.value = await cardDetailTemplateApi.list(props.cardTypeId)
  } catch (error: any) {
    console.error('Failed to fetch templates:', error)
    Message.error(error.message || t('admin.cardType.detailTemplate.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 新建模板
function handleCreate() {
  router.push({
    name: 'CardDetailTemplateCreate',
    query: { cardTypeId: props.cardTypeId, cardTypeName: props.cardTypeName },
  })
}

// 编辑模板
function handleEdit(template: TemplateListItemVO) {
  router.push({
    name: 'CardDetailTemplateEdit',
    params: { id: template.id },
  })
}

// 复制模板
async function handleCopy(template: TemplateListItemVO) {
  try {
    await cardDetailTemplateApi.copy(template.id, t('admin.cardType.detailTemplate.copyName', { name: template.name }))
    Message.success(t('admin.cardType.detailTemplate.copySuccess'))
    await fetchTemplates()
  } catch (error: any) {
    console.error('Failed to copy template:', error)
    Message.error(error.message || t('admin.cardType.detailTemplate.copyFailed'))
  }
}

import { handleReferenceConflictError } from '@/utils/error-handler'

// 删除模板
function handleDelete(template: TemplateListItemVO) {
  if (template.systemTemplate) {
    Message.warning(t('admin.cardType.detailTemplate.deleteSystemNotAllowed'))
    return
  }
  Modal.confirm({
    title: t('admin.cardType.detailTemplate.confirmDelete'),
    content: t('admin.cardType.detailTemplate.deleteConfirmContent', { name: template.name }),
    okText: t('admin.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await cardDetailTemplateApi.delete(template.id)
        Message.success(t('admin.message.deleteSuccess'))
        await fetchTemplates()
      } catch (error: any) {
        if (!handleReferenceConflictError(error)) {
          console.error('Failed to delete template:', error)
          Message.error(error.message || t('admin.cardType.detailTemplate.deleteFailed'))
        }
      }
    },
  })
}

// 启用/停用模板
async function handleToggleEnabled(template: TemplateListItemVO) {
  try {
    if (template.enabled) {
      await cardDetailTemplateApi.disable(template.id)
      Message.success(t('admin.cardType.detailTemplate.disableSuccess'))
    } else {
      await cardDetailTemplateApi.activate(template.id)
      Message.success(t('admin.cardType.detailTemplate.enableSuccess'))
    }
    await fetchTemplates()
  } catch (error: any) {
    console.error('Failed to toggle enabled:', error)
    Message.error(error.message || t('admin.cardType.detailTemplate.operationFailed'))
  }
}

// 设置为默认模板
async function handleSetDefault(template: TemplateListItemVO) {
  if (template.default) return
  try {
    await cardDetailTemplateApi.setDefault(template.id)
    Message.success(t('admin.cardType.detailTemplate.setDefaultSuccess'))
    await fetchTemplates()
  } catch (error: any) {
    console.error('Failed to set default:', error)
    Message.error(error.message || t('admin.cardType.detailTemplate.setDefaultFailed'))
  }
}


// 监听 cardTypeId 变化
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
  <div class="detail-template-tab">
    <!-- 头部操作栏（仅有数据时显示） -->
    <div v-if="templates.length > 0" class="tab-header">
      <CreateButton @click="handleCreate">{{ t('admin.cardType.detailTemplate.createButton') }}</CreateButton>
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
          <a-table-column :title="t('admin.cardType.detailTemplate.templateName')" data-index="name" :width="200">
            <template #cell="{ record }">
              <a-link @click="handleEdit(record)">{{ record.name }}</a-link>
              <a-tag v-if="record.systemTemplate" size="small" color="gray" style="margin-left: 4px">
                {{ t('admin.cardType.detailTemplate.system') }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.cardType.detailTemplate.default')" :width="70" align="center">
            <template #cell="{ record }">
              <a-tooltip :content="record.default ? t('admin.cardType.detailTemplate.defaultTemplate') : t('admin.cardType.detailTemplate.clickToSetDefault')">
                <span
                  class="default-icon"
                  :class="{ 'is-default': record.default }"
                  @click="handleSetDefault(record)"
                >
                  <IconStarFill v-if="record.default" />
                  <IconStar v-else />
                </span>
              </a-tooltip>
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.cardType.detailTemplate.priority')" data-index="priority" :width="80" align="center" />
          <a-table-column :title="t('admin.cardType.detailTemplate.tabCount')" data-index="tabCount" :width="80" align="center" />
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
                  <a-doption :disabled="record.default" @click="handleSetDefault(record)">
                    <IconStarFill v-if="record.default" />
                    <IconStar v-else />
                    {{ record.default ? t('admin.cardType.detailTemplate.defaultTemplate') : t('admin.cardType.detailTemplate.setAsDefault') }}
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
        <a-empty :description="t('admin.cardType.detailTemplate.emptyDescription')" />
        <CreateButton @click="handleCreate">{{ t('admin.cardType.detailTemplate.createButton') }}</CreateButton>
      </div>
    </a-spin>
  </div>
</template>

<style scoped lang="scss">
.detail-template-tab {
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
