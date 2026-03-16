<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useOrgStore } from '@/stores/org'
import { useUserStore } from '@/stores/user'
import { Message } from '@arco-design/web-vue'
import type { OrganizationDTO } from '@/types/member'
import CreateOrgModal from '@/views/org/CreateOrgModal.vue'

const { t } = useI18n()

const router = useRouter()
const route = useRoute()
const orgStore = useOrgStore()
const userStore = useUserStore()

const showCreateModal = ref(false)
const loading = ref(false)

const organizations = computed(() => orgStore.myOrgs)
const hasOrganizations = computed(() => organizations.value.length > 0)
const switching = ref(false)

// 页面加载时获取组织列表
onMounted(async () => {
  loading.value = true
  try {
    await orgStore.fetchMyOrganizations()
  } catch (error) {
    console.error('Failed to fetch organizations:', error)
  } finally {
    loading.value = false
  }
})

async function selectOrg(org: OrganizationDTO) {
  if (switching.value) return

  switching.value = true
  try {
    await orgStore.switchOrganization(org.id)
    Message.success(t('auth.selectOrg.enterSuccess', { name: org.name }))
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (error: unknown) {
    const err = error as { message?: string }
    Message.error(err.message || t('auth.selectOrg.enterFailed'))
  } finally {
    switching.value = false
  }
}

function handleCreateSuccess() {
  showCreateModal.value = false
  Message.success(t('auth.selectOrg.createSuccess'))
  // 创建组织时已经自动切换到该组织
  const redirect = (route.query.redirect as string) || '/'
  router.push(redirect)
}

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="select-org-container">
    <!-- 顶部 Logo -->
    <div class="top-bar">
      <img src="/favicon.svg" alt="Logo" class="top-logo" />
      <span class="top-title">Agilean planka</span>
    </div>

    <div class="select-org-card">
      <div class="select-org-header">
        <h1 class="select-org-title">{{ t('auth.selectOrg.title') }}</h1>
        <p class="select-org-subtitle">
          {{ hasOrganizations ? t('auth.selectOrg.selectPrompt') : t('auth.selectOrg.noOrg') }}
        </p>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-container">
        <a-spin :size="32" />
      </div>

      <!-- 组织列表 -->
      <div v-else-if="hasOrganizations" class="org-list">
        <div
          v-for="org in organizations"
          :key="org.id"
          class="org-item"
          @click="selectOrg(org)"
        >
          <div class="org-avatar">
            <img v-if="org.logo" :src="org.logo" alt="" />
            <span v-else class="org-avatar-text">{{ org.name.charAt(0) }}</span>
          </div>
          <div class="org-info">
            <div class="org-name">{{ org.name }}</div>
            <div v-if="org.description" class="org-desc">{{ org.description }}</div>
          </div>
          <icon-right class="org-arrow" />
        </div>
      </div>

      <!-- 没有组织时的提示 -->
      <div v-else class="no-org-tip">
        <a-empty :description="t('auth.selectOrg.noOrg')">
          <template #image>
            <icon-home :size="48" />
          </template>
        </a-empty>
      </div>

      <!-- 创建组织按钮 -->
      <div class="create-org-section">
        <a-button type="outline" long size="large" @click="showCreateModal = true">
          <template #icon>
            <icon-plus />
          </template>
          {{ t('auth.selectOrg.createOrg') }}
        </a-button>
      </div>

      <div class="select-org-footer">
        <a-link @click="handleLogout">{{ t('auth.selectOrg.logout') }}</a-link>
      </div>
    </div>

    <!-- 创建组织弹窗 -->
    <CreateOrgModal
      v-model:visible="showCreateModal"
      @success="handleCreateSuccess"
    />
  </div>
</template>

<style scoped>
.select-org-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(180deg, #f2f7ff 0%, #e8f0fe 100%);
  position: relative;
}

.select-org-card {
  width: 480px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

/* 顶部 Logo 栏 */
.top-bar {
  position: absolute;
  top: 24px;
  left: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 10;
}

.top-logo {
  width: 40px;
  height: 40px;
}

.top-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-1);
}

.select-org-header {
  text-align: center;
  margin-bottom: 32px;
}

.select-org-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-1);
}

.select-org-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-3);
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 48px 0;
}

.org-list {
  margin-bottom: 24px;
  max-height: 360px;
  overflow-y: auto;
}

.org-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.org-item:hover {
  border-color: rgb(var(--primary-6));
  background-color: var(--color-fill-1);
}

.org-item:last-child {
  margin-bottom: 0;
}

.org-avatar {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: #165DFF;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.org-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.org-avatar-text {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.org-info {
  flex: 1;
  margin-left: 16px;
  overflow: hidden;
}

.org-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text-1);
  margin-bottom: 4px;
}

.org-desc {
  font-size: 13px;
  color: var(--color-text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-arrow {
  color: var(--color-text-3);
  flex-shrink: 0;
}

.no-org-tip {
  padding: 32px 0;
}

.create-org-section {
  margin-top: 24px;
}

.select-org-footer {
  margin-top: 24px;
  text-align: center;
}
</style>
