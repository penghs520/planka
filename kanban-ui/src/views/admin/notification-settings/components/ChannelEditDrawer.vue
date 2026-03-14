<template>
  <div class="channel-edit-drawer">
    <div class="drawer-header">
      <h3 class="drawer-title">
        {{ isCreating ? t('admin.notificationSettings.channel.createTitle') : t('admin.notificationSettings.channel.editTitle') }}
      </h3>
      <a-button type="text" size="small" @click="handleClose">
        <template #icon><icon-close /></template>
      </a-button>
    </div>

    <div class="drawer-content">
      <a-form ref="formRef" :model="formData" layout="vertical" :rules="rules">
        <!-- 渠道类型选择（仅新建时显示） -->
        <a-form-item v-if="isCreating" field="channelId" :label="t('admin.notificationSettings.channel.form.channelType')">
          <a-select
            v-model="formData.channelId"
            :placeholder="t('admin.notificationSettings.channel.form.channelTypePlaceholder')"
            @change="handleChannelTypeChange"
          >
            <a-option v-for="type in channelTypes" :key="type.value" :value="type.value">
              {{ type.label }}
            </a-option>
          </a-select>
        </a-form-item>

        <!-- 渠道名称 -->
        <a-form-item field="name" :label="t('admin.notificationSettings.channel.form.channelName')">
          <a-input
            v-model="formData.name"
            :placeholder="t('admin.notificationSettings.channel.form.channelNamePlaceholder')"
          />
        </a-form-item>

        <!-- 动态配置参数 -->
        <template v-if="configFields.length > 0">
          <div class="config-section">
            <div v-for="field in configFields" :key="field.key" class="config-row">
              <div class="config-label">
                <span class="label-title">{{ t('admin.notificationSettings.channel.form.paramName') }}</span>
                <a-input :model-value="field.label" disabled class="label-input" />
              </div>
              <div class="config-value">
                <span class="value-title">{{ t('admin.notificationSettings.channel.form.paramValue') }}</span>
                <component
                  :is="getFieldComponent(field.type)"
                  v-model="formData.config[field.key]"
                  :placeholder="field.placeholder"
                  :type="field.type === 'password' ? 'password' : 'text'"
                  allow-clear
                />
              </div>
            </div>
          </div>
        </template>
      </a-form>
    </div>

    <div class="drawer-footer">
      <a-space>
        <a-button @click="handleClose">{{ t('admin.action.cancel') }}</a-button>
        <a-button type="primary" :loading="saving" @click="handleSave">{{ t('admin.action.confirm') }}</a-button>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconClose } from '@arco-design/web-vue/es/icon'
import type { FormInstance } from '@arco-design/web-vue'
import type { NotificationChannelConfigDefinition } from '@/api/notification-channel'
import { SchemaSubType } from '@/types/schema'
import { EntityState } from '@/types/common'
import { useOrgStore } from '@/stores/org'

// MORE_CONTENT_PLACEHOLDER

const props = defineProps<{
  channel: NotificationChannelConfigDefinition | null
  isCreating: boolean
}>()

const emit = defineEmits<{
  save: [channel: NotificationChannelConfigDefinition]
  cancel: []
  delete: [channel: NotificationChannelConfigDefinition]
}>()

const { t } = useI18n()
const orgStore = useOrgStore()
const formRef = ref<FormInstance>()
const saving = ref(false)

interface ConfigField {
  key: string
  label: string
  type: 'text' | 'password' | 'switch'
  placeholder?: string
}

const formData = ref<{
  channelId: string
  name: string
  config: Record<string, unknown>
  isDefault: boolean
  enabled: boolean
}>({
  channelId: '',
  name: '',
  config: {},
  isDefault: false,
  enabled: true,
})

const rules = {
  channelId: [{ required: true, message: t('admin.notificationSettings.channel.form.channelTypeRequired') }],
  name: [{ required: true, message: t('admin.notificationSettings.channel.form.channelNameRequired') }],
}

const channelTypes = computed(() => [
  { value: 'email', label: t('admin.notificationSettings.channel.type.email') },
  { value: 'feishu', label: t('admin.notificationSettings.channel.type.feishu') },
  { value: 'dingtalk', label: t('admin.notificationSettings.channel.type.dingtalk') },
  { value: 'wecom', label: t('admin.notificationSettings.channel.type.wecom') },
  { value: 'xiaowei', label: t('admin.notificationSettings.channel.type.xiaowei') },
  { value: 'zhiwei', label: t('admin.notificationSettings.channel.type.zhiwei') },
])

// 各渠道的配置字段定义
const channelConfigFields: Record<string, ConfigField[]> = {
  email: [
    { key: 'host', label: t('admin.notificationSettings.channel.email.host'), type: 'text', placeholder: t('admin.notificationSettings.channel.email.hostPlaceholder') },
    { key: 'port', label: t('admin.notificationSettings.channel.email.port'), type: 'text', placeholder: t('admin.notificationSettings.channel.email.portPlaceholder') },
    { key: 'username', label: t('admin.notificationSettings.channel.email.username'), type: 'text', placeholder: t('admin.notificationSettings.channel.email.usernamePlaceholder') },
    { key: 'password', label: t('admin.notificationSettings.channel.email.password'), type: 'password', placeholder: t('admin.notificationSettings.channel.email.passwordPlaceholder') },
    { key: 'enableAuth', label: t('admin.notificationSettings.channel.email.enableAuth'), type: 'switch' },
    { key: 'protocol', label: t('admin.notificationSettings.channel.email.protocol'), type: 'text', placeholder: t('admin.notificationSettings.channel.email.protocolPlaceholder') },
    { key: 'encoding', label: t('admin.notificationSettings.channel.email.encoding'), type: 'text', placeholder: t('admin.notificationSettings.channel.email.encodingPlaceholder') },
    { key: 'enableInviteTeam', label: t('admin.notificationSettings.channel.email.enableInviteTeam'), type: 'switch' },
    { key: 'enableInviteOrg', label: t('admin.notificationSettings.channel.email.enableInviteOrg'), type: 'switch' },
  ],
  feishu: [
    { key: 'appId', label: t('admin.notificationSettings.channel.feishu.appId'), type: 'text', placeholder: t('admin.notificationSettings.channel.feishu.appIdPlaceholder') },
    { key: 'appSecret', label: t('admin.notificationSettings.channel.feishu.appSecret'), type: 'password', placeholder: t('admin.notificationSettings.channel.feishu.appSecretPlaceholder') },
  ],
  dingtalk: [
    { key: 'appKey', label: t('admin.notificationSettings.channel.dingtalk.appKey'), type: 'text', placeholder: t('admin.notificationSettings.channel.dingtalk.appKeyPlaceholder') },
    { key: 'appSecret', label: t('admin.notificationSettings.channel.dingtalk.appSecret'), type: 'password', placeholder: t('admin.notificationSettings.channel.dingtalk.appSecretPlaceholder') },
    { key: 'agentId', label: t('admin.notificationSettings.channel.dingtalk.agentId'), type: 'text', placeholder: t('admin.notificationSettings.channel.dingtalk.agentIdPlaceholder') },
  ],
  wecom: [
    { key: 'corpId', label: t('admin.notificationSettings.channel.wecom.corpId'), type: 'text', placeholder: t('admin.notificationSettings.channel.wecom.corpIdPlaceholder') },
    { key: 'agentId', label: t('admin.notificationSettings.channel.wecom.agentId'), type: 'text', placeholder: t('admin.notificationSettings.channel.wecom.agentIdPlaceholder') },
    { key: 'secret', label: t('admin.notificationSettings.channel.wecom.secret'), type: 'password', placeholder: t('admin.notificationSettings.channel.wecom.secretPlaceholder') },
  ],
}

const configFields = computed(() => {
  return channelConfigFields[formData.value.channelId] || []
})

function getFieldComponent(type: string) {
  if (type === 'switch') return 'a-switch'
  return 'a-input'
}

function handleChannelTypeChange() {
  formData.value.config = {}
}

watch(
  () => props.channel,
  (channel) => {
    if (channel) {
      formData.value = {
        channelId: channel.channelId,
        name: channel.name,
        config: { ...channel.config },
        isDefault: channel.isDefault,
        enabled: channel.enabled,
      }
    } else if (props.isCreating) {
      formData.value = {
        channelId: '',
        name: '',
        config: {},
        isDefault: false,
        enabled: true,
      }
    }
  },
  { immediate: true }
)

function handleClose() {
  emit('cancel')
}

async function handleSave() {
  const valid = await formRef.value?.validate()
  if (valid) return

  saving.value = true
  try {
    const channelData: NotificationChannelConfigDefinition = {
      // Schema 基础字段
      schemaSubType: SchemaSubType.NOTIFICATION_CHANNEL_CONFIG,
      id: props.channel?.id,
      orgId: orgStore.currentOrgId || '',
      name: formData.value.name,
      enabled: formData.value.enabled,
      state: props.channel?.state || EntityState.ACTIVE,
      contentVersion: props.channel?.contentVersion || 0,
      // 渠道特有字段
      channelId: formData.value.channelId,
      config: formData.value.config,
      isDefault: formData.value.isDefault,
      priority: props.channel?.priority || 0,
    }
    emit('save', channelData)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.channel-edit-drawer {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-1);
}

.drawer-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-1);
  margin: 0;
}

.drawer-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.config-section {
  margin-top: 16px;
}

.config-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.config-label,
.config-value {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label-title,
.value-title {
  font-size: 13px;
  color: var(--color-text-2);
}

.label-input {
  background: var(--color-fill-2);
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--color-border-1);
}
</style>
