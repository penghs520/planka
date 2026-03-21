<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/hooks/useAuth'
import { usePermission } from '@/hooks/usePermission'
import { useSidebarTheme, THEME_OPTIONS, type UiThemeId } from '@/composables/useSidebarTheme'
import {
  IconBgColors,
  IconExport,
  IconCheck,
  IconSettings,
  IconUser,
  IconRight,
  IconEdit,
} from '@arco-design/web-vue/es/icon'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const { logout } = useAuth()
const { isAdmin, isSuperAdmin } = usePermission()
const canEnterAdminPanel = computed(() => isAdmin.value || isSuperAdmin.value)
const { theme, setTheme } = useSidebarTheme()

const user = computed(() => userStore.user)
const displayName = computed(() => user.value?.nickname || user.value?.email || '')
const avatarText = computed(() => displayName.value.charAt(0).toUpperCase())

function goToProfile() {
  router.push('/profile')
}

function goSidebarSettings() {
  router.push('/settings/sidebar')
}

function goToAdminPanel() {
  router.push('/admin/card-type')
}

async function handleLogout() {
  await logout()
}

function pickTheme(id: UiThemeId) {
  setTheme(id)
}
</script>

<template>
  <div class="sidebar-footer">
    <div class="sidebar-footer-row">
      <a-dropdown
        trigger="click"
        position="tr"
        class="sidebar-user-dropdown"
      >
        <div class="sidebar-user">
          <a-avatar :size="22" :image-url="user?.avatar || undefined" class="sidebar-user-avatar">
            {{ avatarText }}
          </a-avatar>
          <span class="sidebar-user-name">{{ displayName }}</span>
        </div>
        <template #content>
        <a-doption v-if="canEnterAdminPanel" @click="goToAdminPanel">
          <template #icon><IconSettings /></template>
          {{ t('common.layout.adminPanel') }}
        </a-doption>
        <a-divider v-if="canEnterAdminPanel" :margin="4" />
        <a-doption @click="goToProfile">
          <template #icon><IconUser /></template>
          {{ t('common.layout.profile') }}
        </a-doption>
        <a-dropdown trigger="hover" position="rt">
          <a-doption class="theme-submenu-trigger">
            <template #icon><IconBgColors /></template>
            <span class="theme-submenu-label">{{ t('sidebar.themeSettings') }}</span>
            <IconRight class="theme-submenu-arrow" />
          </a-doption>
          <template #content>
            <a-doption
              v-for="opt in THEME_OPTIONS"
              :key="opt.id"
              @click="pickTheme(opt.id)"
            >
              <template #icon>
                <IconCheck v-if="theme === opt.id" :size="14" />
              </template>
              {{ t(opt.labelKey) }}
            </a-doption>
          </template>
        </a-dropdown>
        <a-divider :margin="4" />
        <a-doption @click="handleLogout">
          <template #icon><IconExport /></template>
          {{ t('common.layout.logout') }}
        </a-doption>
        </template>
      </a-dropdown>
      <button
        type="button"
        class="sidebar-edit-btn"
        :title="t('sidebar.editSidebar')"
        :aria-label="t('sidebar.editSidebar')"
        @click="goSidebarSettings"
      >
        <IconEdit class="sidebar-edit-icon" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.sidebar-footer {
  padding: 8px;
  flex-shrink: 0;
}

.sidebar-footer-row {
  display: flex;
  align-items: center;
  gap: 2px;
  min-width: 0;
}

.sidebar-user-dropdown {
  flex: 1;
  min-width: 0;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 6px;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.15s;
  height: 28px;
  min-width: 0;
}

.sidebar-user:hover {
  background: var(--sidebar-bg-hover);
}

.sidebar-user-avatar {
  flex-shrink: 0;
  font-size: 11px !important;
}

.sidebar-user-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--sidebar-nav-font-family);
  font-size: var(--sidebar-nav-font-size);
  font-weight: 400;
  color: var(--sidebar-text-secondary);
}

/* 嵌套下拉：与 Arco doption 行高对齐，右侧箭头 */
.theme-submenu-trigger {
  display: flex !important;
  align-items: center;
  width: 100%;
  gap: 0;
}

.theme-submenu-label {
  flex: 1;
  min-width: 0;
}

.theme-submenu-arrow {
  flex-shrink: 0;
  margin-left: 8px;
  font-size: 12px;
  color: var(--color-text-3);
}

.sidebar-edit-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: var(--sidebar-text-secondary);
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s;
}

.sidebar-edit-btn:hover {
  background: var(--sidebar-bg-hover);
  color: var(--sidebar-accent);
}

.sidebar-edit-icon {
  width: 15px;
  height: 15px;
}
</style>
