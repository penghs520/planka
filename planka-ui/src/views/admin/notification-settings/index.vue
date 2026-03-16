<template>
  <div class="notification-settings-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">{{ t('admin.notificationSettings.title') }}</h1>
        <p class="page-description">{{ t('admin.notificationSettings.description') }}</p>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="page-content">
      <a-card :bordered="false" class="content-card">
        <a-tabs v-model:active-key="activeTab" type="line">
          <a-tab-pane key="channel-config" :title="t('admin.notificationSettings.tabs.channelConfig')">
            <ChannelConfigTab />
          </a-tab-pane>
          <a-tab-pane key="template-config" :title="t('admin.notificationSettings.tabs.templateConfig')">
            <TemplateConfigTab />
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import ChannelConfigTab from './components/ChannelConfigTab.vue'
import TemplateConfigTab from './components/TemplateConfigTab.vue'

const { t } = useI18n()
const activeTab = ref('channel-config')
</script>

<style scoped>
.notification-settings-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-1);
}

.page-header {
  background: #fff;
  border-bottom: 1px solid var(--color-border-1);
  padding: 16px 24px;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-1);
  margin: 0 0 4px 0;
}

.page-description {
  font-size: 13px;
  color: var(--color-text-3);
  margin: 0;
}

.page-content {
  flex: 1;
  overflow: auto;
  padding: 16px 24px;
}

.content-card {
  max-width: 1200px;
  margin: 0 auto;
}

.content-card :deep(.arco-card-body) {
  padding: 0;
}

.content-card :deep(.arco-tabs) {
  height: 100%;
}

.content-card :deep(.arco-tabs-nav) {
  padding: 0 20px;
  margin-bottom: 0;
}

.content-card :deep(.arco-tabs-content) {
  padding: 20px;
}

.empty-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}
</style>
