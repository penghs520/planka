<template>
  <div class="template-config-tab">
    <!-- 头部工具栏 -->
    <div class="toolbar">
      <a-button type="primary" @click="handleCreate">
        <template #icon><icon-plus /></template>
        {{ t('admin.notificationSettings.template.addTemplate') }}
      </a-button>
    </div>

    <!-- 模板列表 -->
    <a-table
      :data="templateList"
      :loading="loading"
      :pagination="false"
      class="template-table"
    >
      <template #columns>
        <a-table-column :title="t('admin.table.name')" data-index="name" />
        <a-table-column :title="t('admin.notificationSettings.template.cardType')" data-index="definitionParameter">
          <template #cell="{ record }">
            <span>{{ getDefinitionParameterDisplay(record) }}</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.notificationSettings.template.triggerEvent')" data-index="triggerEventName">
          <template #cell="{ record }">
            <a-tag>{{ record.triggerEventName || record.triggerEvent }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.notificationSettings.template.recipientType.label')" data-index="recipientType">
          <template #cell="{ record }">
            <span>{{ getRecipientTypeLabel(record.recipientType) }}</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.notificationSettings.template.channels')" data-index="channels">
          <template #cell="{ record }">
            <a-space>
              <a-tag v-for="channel in record.channels" :key="channel" size="small">
                {{ getChannelLabel(channel) }}
              </a-tag>
            </a-space>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.status')" data-index="enabled">
          <template #cell="{ record }">
            <a-switch
              :model-value="record.enabled"
              size="small"
              @change="(val) => handleToggleStatus(record, val as boolean)"
            />
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.table.operations')" width="200">
          <template #cell="{ record }">
            <a-space>
              <a-button type="text" size="small" @click="handleEdit(record)">
                {{ t('admin.action.edit') }}
              </a-button>
              <a-popconfirm
                :content="t('admin.message.confirmDelete', { type: t('admin.notificationSettings.template.title'), name: record.name })"
                type="warning"
                @ok="handleDelete(record)"
              >
                <a-button type="text" size="small" status="danger">
                  {{ t('admin.action.delete') }}
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </a-table-column>
      </template>
      <template #empty>
        <a-empty :description="t('admin.notificationSettings.template.emptyDescription')" />
      </template>
    </a-table>

    <!-- 编辑抽屉 -->
    <TemplateEditDrawer
      v-model:visible="drawerVisible"
      :template="editingTemplate"
      @success="handleSaveSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import {
  notificationTemplateApi,
  type NotificationTemplateDefinition,
  type RecipientType,
} from '@/api/notification-template'
import { cardTypeApi } from '@/api/card-type'
import TemplateEditDrawer from './TemplateEditDrawer.vue'

const { t } = useI18n()

// 加载状态
const loading = ref(false)

// 模板列表
const templateList = ref<NotificationTemplateDefinition[]>([])

// 实体类型名称映射
const cardTypeMap = ref<Map<string, string>>(new Map())

// 抽屉状态
const drawerVisible = ref(false)
const editingTemplate = ref<NotificationTemplateDefinition | null>(null)

// 加载实体类型列表
const loadCardTypes = async () => {
  try {
    const data = await cardTypeApi.list()
    const map = new Map<string, string>()
    data.forEach((item) => {
      if (item.id) {
        map.set(item.id, item.name)
      }
    })
    cardTypeMap.value = map
  } catch {
    // 忽略错误
  }
}

// 加载模板列表
const loadTemplateList = async () => {
  loading.value = true
  try {
    const data = await notificationTemplateApi.list()
    templateList.value = data || []
  } catch {
    Message.error(t('admin.notificationSettings.template.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 获取通知对象类型标签
const getRecipientTypeLabel = (type: RecipientType) => {
  const labels: Record<RecipientType, string> = {
    MEMBER: t('admin.notificationSettings.template.recipientType.member'),
    GROUP: t('admin.notificationSettings.template.recipientType.group'),
  }
  return labels[type] || type
}

// 获取定义参数显示文本
const getDefinitionParameterDisplay = (record: NotificationTemplateDefinition) => {
  if (record.definitionParameter) {
    if (record.definitionParameter.type === 'CARD_TYPE') {
      const cardTypeId = record.definitionParameter.cardTypeId
      return record.definitionParameter.cardTypeName || cardTypeMap.value.get(cardTypeId) || cardTypeId
    } else {
      const typeLabels: Record<string, string> = {
        DATE: t('admin.notificationSettings.template.parameterType.date'),
        TEXT: t('admin.notificationSettings.template.parameterType.text'),
        MULTILINE_TEXT: t('admin.notificationSettings.template.parameterType.multilineText'),
        LINK: t('admin.notificationSettings.template.parameterType.link'),
        NUMBER: t('admin.notificationSettings.template.parameterType.number'),
      }
      const typeLabel = typeLabels[record.definitionParameter.type] || record.definitionParameter.type
      return `${typeLabel}: ${record.definitionParameter.name}`
    }
  }
  // 兼容旧数据
  return record.cardTypeName || cardTypeMap.value.get(record.cardTypeId || '') || record.cardTypeId || '-'
}

// 获取渠道标签
const getChannelLabel = (channel: string) => {
  const labels: Record<string, string> = {
    builtin: t('admin.notificationSettings.channel.type.builtin'),
    email: t('admin.notificationSettings.channel.type.email'),
    feishu: t('admin.notificationSettings.channel.type.feishu'),
    dingtalk: t('admin.notificationSettings.channel.type.dingtalk'),
    wecom: t('admin.notificationSettings.channel.type.wecom'),
  }
  return labels[channel] || channel
}

// 创建模板
const handleCreate = () => {
  editingTemplate.value = null
  drawerVisible.value = true
}

// 编辑模板
const handleEdit = (record: NotificationTemplateDefinition) => {
  editingTemplate.value = record
  drawerVisible.value = true
}

// 删除模板
const handleDelete = async (record: NotificationTemplateDefinition) => {
  try {
    await notificationTemplateApi.delete(record.id!)
    Message.success(t('admin.message.deleteSuccess'))
    loadTemplateList()
  } catch {
    Message.error(t('admin.notificationSettings.template.deleteFailed'))
  }
}

// 切换状态
const handleToggleStatus = async (record: NotificationTemplateDefinition, enabled: boolean) => {
  try {
    if (enabled) {
      await notificationTemplateApi.activate(record.id!)
      Message.success(t('admin.message.enableSuccess'))
    } else {
      await notificationTemplateApi.disable(record.id!)
      Message.success(t('admin.message.disableSuccess'))
    }
    loadTemplateList()
  } catch {
    Message.error(t('admin.notificationSettings.template.operationFailed'))
  }
}

// 保存成功
const handleSaveSuccess = () => {
  loadTemplateList()
}

onMounted(() => {
  loadCardTypes()
  loadTemplateList()
})
</script>

<style scoped lang="scss">
.template-config-tab {
  .toolbar {
    margin-bottom: 16px;
  }

  .template-table {
    :deep(.arco-table-cell) {
      padding: 12px 16px;
    }
  }
}
</style>
