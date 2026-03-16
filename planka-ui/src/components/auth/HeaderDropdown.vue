<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useOrgStore } from '@/stores/org'
import { useAuth } from '@/hooks/useAuth'
import { Message } from '@arco-design/web-vue'
import type { OrganizationDTO } from '@/types/member'
import CreateOrgModal from '@/views/org/CreateOrgModal.vue'

const router = useRouter()
const userStore = useUserStore()
const orgStore = useOrgStore()
const { logout } = useAuth()

const showCreateModal = ref(false)

// User Data
const user = computed(() => userStore.user)
const userDisplayName = computed(() => user.value?.nickname || user.value?.email || '用户')
const userAvatarText = computed(() => userDisplayName.value.charAt(0).toUpperCase())

// Org Data
const currentOrg = computed(() => orgStore.currentOrg)
const organizations = computed(() => orgStore.myOrgs)
const orgDisplayName = computed(() => currentOrg.value?.name || '选择组织')

// Actions
const switching = ref(false)

async function selectOrg(org: OrganizationDTO) {
  if (org.id === orgStore.currentOrgId) return
  if (switching.value) return

  switching.value = true
  try {
    await orgStore.switchOrganization(org.id)
    Message.success(`已切换到 ${org.name}`)
    router.go(0)
  } catch (error: unknown) {
    const err = error as { message?: string }
    Message.error(err.message || '切换组织失败')
  } finally {
    switching.value = false
  }
}

function handleCreateSuccess() {
  showCreateModal.value = false
  Message.success('组织创建成功')
}



function goToProfile() {
  router.push('/profile')
}

async function handleLogout() {
  await logout()
}
</script>

<template>
  <a-dropdown trigger="click" position="br">
    <div class="header-trigger">
      <!-- Organization Part -->
      <div class="trigger-org">
        <div class="org-avatar">
          <img v-if="currentOrg?.logo" :src="currentOrg.logo" alt="" />
          <span v-else class="org-avatar-text">{{ orgDisplayName.charAt(0) }}</span>
        </div>
        <span class="org-name">{{ orgDisplayName }}</span>
      </div>

      <div class="trigger-divider"></div>

      <!-- User Part -->
      <div class="trigger-user">
        <a-avatar :size="24" :image-url="user?.avatar || undefined" class="user-avatar">
          {{ userAvatarText }}
        </a-avatar>
        <span class="user-name">{{ userDisplayName }}</span>
      </div>
      
      <icon-down class="trigger-icon" />
    </div>

    <template #content>
      <!-- Organization Section -->
      <div class="dropdown-section-title">切换组织</div>
      <div class="org-list">
        <a-doption
          v-for="org in organizations"
          :key="org.id"
          :class="{ 'org-active': org.id === currentOrg?.id }"
          @click="selectOrg(org)"
        >
          <div class="org-option">
            <div class="org-option-avatar">
              <img v-if="org.logo" :src="org.logo" alt="" />
              <span v-else>{{ org.name.charAt(0) }}</span>
            </div>
            <span class="org-option-name">{{ org.name }}</span>
            <icon-check v-if="org.id === currentOrg?.id" class="org-check" />
          </div>
        </a-doption>
      </div>
      
      <a-doption v-if="userStore.isSuperAdmin" @click="showCreateModal = true">
        <template #icon><icon-plus /></template>
        创建新组织
      </a-doption>


      <a-divider :margin="4" />

      <!-- User Section -->
      <a-doption @click="goToProfile">
        <template #icon><icon-user /></template>
        个人设置
      </a-doption>
      <a-doption @click="handleLogout">
        <template #icon><icon-export /></template>
        退出登录
      </a-doption>
    </template>
  </a-dropdown>

  <CreateOrgModal
    v-model:visible="showCreateModal"
    @success="handleCreateSuccess"
  />
</template>

<style scoped>
.header-trigger {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.header-trigger:hover {
  background-color: var(--color-fill-2);
}

.trigger-org, .trigger-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.trigger-divider {
  width: 1px;
  height: 16px;
  background-color: var(--color-border-2);
  margin: 0 12px;
}

/* Org Styles */
.org-avatar {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.org-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.org-avatar-text {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.org-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
}

/* User Styles */
.user-avatar {
  background-color: rgb(var(--primary-6));
}

.user-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: var(--color-text-2);
}

.trigger-icon {
  margin-left: 8px;
  color: var(--color-text-3);
  font-size: 12px;
}

/* Dropdown Styles */
.dropdown-section-title {
  padding: 4px 12px;
  font-size: 12px;
  color: var(--color-text-3);
}

.org-list {
  max-height: 200px;
  overflow-y: auto;
}

.org-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.org-option-avatar {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.org-option-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.org-option-avatar span {
  font-size: 10px;
  font-weight: 600;
  color: #fff;
}

.org-option-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-check {
  color: rgb(var(--primary-6));
}

.org-active {
  background-color: var(--color-fill-2);
}
</style>
