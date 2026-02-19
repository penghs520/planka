<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrgStore } from '@/stores/org'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import BasicSettingsTab from './BasicSettingsTab.vue'
import AdvancedFeaturesTab from './AdvancedFeaturesTab.vue'

const { t } = useI18n()
const router = useRouter()
const orgStore = useOrgStore()

const activeTab = ref('basic')
const loading = ref(false)

const currentOrg = computed(() => orgStore.currentOrg)
const isAdmin = computed(() => orgStore.isAdmin)

onMounted(async () => {
  if (!orgStore.currentOrgId) {
    router.push('/select-org')
    return
  }
  // 获取当前用户在组织中的角色
  loading.value = true
  try {
    await orgStore.fetchCurrentOrgRole()

    if (!isAdmin.value) {
      Message.warning('只有管理员可见')
      router.push('/')
      return
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="org-settings-container">
    <a-spin :loading="loading">
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="basic" :title="t('admin.orgSettings.tabs.basic')">
          <BasicSettingsTab v-if="currentOrg" :org="currentOrg" :can-edit="isAdmin" />
        </a-tab-pane>
        <a-tab-pane key="advanced" :title="t('admin.orgSettings.tabs.advanced')">
          <AdvancedFeaturesTab v-if="currentOrg" :org="currentOrg" :can-edit="isAdmin" />
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </div>
</template>

<style scoped>
.org-settings-container {
  padding: 8px 16px;
}

.org-settings-container :deep(.arco-tabs-nav) {
  background-color: var(--color-fill-1);
  padding: 4px 12px 0;
  margin-bottom: 8px;
}

.org-settings-container :deep(.arco-tabs-nav-ink) {
  height: 3px;
  border-radius: 0;
}
</style>
