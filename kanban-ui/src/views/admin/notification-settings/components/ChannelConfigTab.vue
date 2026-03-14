<template>
  <div class="channel-config-tab">
    <div class="channel-layout">
      <!-- 左侧渠道列表 -->
      <div class="channel-list-panel">
        <div class="panel-header">
          <a-button type="text" size="small" @click="handleAddChannel">
            <template #icon><icon-plus /></template>
          </a-button>
          <span class="header-title">{{ t('admin.notificationSettings.channel.channelType') }}</span>
          <span class="header-title name-col">{{ t('admin.notificationSettings.channel.channelName') }}</span>
        </div>
        <a-spin :loading="loading" class="channel-list-content">
          <div v-if="channels.length === 0 && !loading" class="empty-state">
            <a-empty :description="t('admin.notificationSettings.channel.emptyDescription')" />
          </div>
          <div
            v-for="channel in channels"
            :key="channel.id"
            class="channel-item"
            :class="{ active: selectedChannel?.id === channel.id }"
            @click="handleSelectChannel(channel)"
          >
            <span class="channel-type">{{ getChannelTypeLabel(channel.channelId) }}</span>
            <span class="channel-name">{{ channel.name }}</span>
          </div>
        </a-spin>
      </div>

      <!-- 右侧编辑区域 -->
      <div class="channel-edit-panel">
        <ChannelEditDrawer
          v-if="selectedChannel || isCreating"
          :channel="selectedChannel"
          :is-creating="isCreating"
          @save="handleSaveChannel"
          @cancel="handleCancelEdit"
          @delete="handleDeleteChannel"
        />
        <div v-else class="empty-edit-panel">
          <a-empty :description="t('admin.notificationSettings.channel.emptyDescription')" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import {
  notificationChannelApi,
  type NotificationChannelConfigDefinition,
  type CreateNotificationChannelRequest,
  type UpdateNotificationChannelRequest,
} from '@/api/notification-channel'
import ChannelEditDrawer from './ChannelEditDrawer.vue'

const { t } = useI18n()

const loading = ref(false)
const channels = ref<NotificationChannelConfigDefinition[]>([])
const selectedChannel = ref<NotificationChannelConfigDefinition | null>(null)
const isCreating = ref(false)

// MORE_CONTENT_PLACEHOLDER

const channelTypeLabels: Record<string, string> = {
  builtin: 'admin.notificationSettings.channel.type.builtin',
  email: 'admin.notificationSettings.channel.type.email',
  feishu: 'admin.notificationSettings.channel.type.feishu',
  dingtalk: 'admin.notificationSettings.channel.type.dingtalk',
  wecom: 'admin.notificationSettings.channel.type.wecom',
  zhiwei: 'admin.notificationSettings.channel.type.zhiwei',
  xiaowei: 'admin.notificationSettings.channel.type.xiaowei',
}

function getChannelTypeLabel(channelId: string): string {
  const key = channelTypeLabels[channelId]
  return key ? t(key) : channelId
}

async function loadChannels() {
  loading.value = true
  try {
    channels.value = await notificationChannelApi.list()
  } catch {
    Message.error(t('admin.notificationSettings.channel.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handleAddChannel() {
  selectedChannel.value = null
  isCreating.value = true
}

function handleSelectChannel(channel: NotificationChannelConfigDefinition) {
  isCreating.value = false
  selectedChannel.value = channel
}

async function handleSaveChannel(channel: NotificationChannelConfigDefinition) {
  try {
    if (isCreating.value) {
      const createRequest: CreateNotificationChannelRequest = {
        name: channel.name,
        channelId: channel.channelId,
        config: channel.config,
        isDefault: channel.isDefault,
        priority: channel.priority,
      }
      const created = await notificationChannelApi.create(createRequest)
      channels.value.push(created)
      selectedChannel.value = created
      isCreating.value = false
      Message.success(t('admin.notificationSettings.channel.createSuccess'))
    } else {
      const updateRequest: UpdateNotificationChannelRequest = {
        name: channel.name,
        config: channel.config,
        isDefault: channel.isDefault,
        priority: channel.priority,
        expectedVersion: channel.contentVersion,
      }
      const updated = await notificationChannelApi.update(channel.id!, updateRequest)
      const index = channels.value.findIndex((c) => c.id === channel.id)
      if (index !== -1) {
        channels.value[index] = updated
      }
      selectedChannel.value = updated
      Message.success(t('admin.notificationSettings.channel.saveSuccess'))
    }
  } catch {
    Message.error(t('admin.message.saveFailed', { type: '' }))
  }
}

function handleCancelEdit() {
  isCreating.value = false
  if (!selectedChannel.value && channels.value.length > 0) {
    selectedChannel.value = channels.value[0] ?? null
  }
}

async function handleDeleteChannel(channel: NotificationChannelConfigDefinition) {
  Modal.confirm({
    title: t('admin.message.confirmDelete'),
    content: t('admin.notificationSettings.channel.deleteConfirm', { name: channel.name }),
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        await notificationChannelApi.delete(channel.id!)
        channels.value = channels.value.filter((c) => c.id !== channel.id)
        selectedChannel.value = channels.value.length > 0 ? (channels.value[0] ?? null) : null
        Message.success(t('admin.notificationSettings.channel.deleteSuccess'))
      } catch {
        Message.error(t('admin.message.deleteSuccess'))
      }
    },
  })
}

onMounted(() => {
  loadChannels()
})
</script>

<style scoped>
.channel-config-tab {
  height: 100%;
}

.channel-layout {
  display: flex;
  height: 500px;
  border: 1px solid var(--color-border-1);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.channel-list-panel {
  width: 360px;
  border-right: 1px solid var(--color-border-1);
  display: flex;
  flex-direction: column;
  background: var(--color-bg-2);
}

.panel-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border-1);
  background: var(--color-fill-1);
  font-size: 13px;
  color: var(--color-text-2);
}

.header-title {
  flex: 1;
}

.header-title.name-col {
  flex: 1;
}

.channel-list-content {
  flex: 1;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.channel-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--color-border-1);
  transition: background-color 0.2s;
}

.channel-item:hover {
  background: var(--color-fill-2);
}

.channel-item.active {
  background: var(--color-primary-light-1);
}

.channel-type {
  flex: 1;
  font-size: 14px;
  color: var(--color-text-1);
}

.channel-name {
  flex: 1;
  font-size: 14px;
  color: var(--color-text-2);
}

.channel-edit-panel {
  flex: 1;
  overflow-y: auto;
  background: #fff;
}

.empty-edit-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>
