<template>
  <div class="workload-management-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">{{ t('admin.workloadManagement.title') }}</h1>
        <p class="page-description">{{ t('admin.workloadManagement.windowConfig.description') }}</p>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="page-content">
      <a-card :bordered="false" class="content-card">
        <a-tabs v-model:active-key="activeTab" type="line">
          <a-tab-pane key="window-config" :title="t('admin.workloadManagement.tabs.windowConfig')">
            <WindowConfigTab />
          </a-tab-pane>
          <a-tab-pane key="worklog-config" :title="t('admin.workloadManagement.tabs.worklogConfig')">
            <div class="empty-tab">
              <a-empty :description="t('common.status.inDevelopment')" />
            </div>
          </a-tab-pane>
          <a-tab-pane key="settlement-config" :title="t('admin.workloadManagement.tabs.settlementConfig')">
            <div class="empty-tab">
              <a-empty :description="t('common.status.inDevelopment')" />
            </div>
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import WindowConfigTab from './components/WindowConfigTab.vue'

const { t } = useI18n()
const activeTab = ref('window-config')
</script>

<style scoped>
.workload-management-page {
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
