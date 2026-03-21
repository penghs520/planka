<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/hooks/useAuth'
import { useSidebarTheme, THEME_OPTIONS, type UiThemeId } from '@/composables/useSidebarTheme'
import {
  IconBgColors,
  IconExport,
  IconCheck,
} from '@arco-design/web-vue/es/icon'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const { logout } = useAuth()
const { theme, setTheme } = useSidebarTheme()

const user = computed(() => userStore.user)
const displayName = computed(() => user.value?.nickname || user.value?.email || '')
const avatarText = computed(() => displayName.value.charAt(0).toUpperCase())

function goToProfile() {
  router.push('/profile')
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
    <a-dropdown trigger="click" position="tr">
      <button
        type="button"
        class="theme-menu-btn"
        :title="t('sidebar.uiThemeMenu')"
      >
        <IconBgColors :size="16" />
      </button>
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

    <!-- 用户信息 -->
    <a-dropdown trigger="click" position="tr">
      <div class="sidebar-user">
        <a-avatar :size="22" :image-url="user?.avatar || undefined" class="sidebar-user-avatar">
          {{ avatarText }}
        </a-avatar>
        <span class="sidebar-user-name">{{ displayName }}</span>
      </div>
      <template #content>
        <a-doption @click="goToProfile">
          <template #icon><icon-user /></template>
          {{ t('common.layout.profile') }}
        </a-doption>
        <a-divider :margin="4" />
        <a-doption @click="handleLogout">
          <template #icon><IconExport /></template>
          {{ t('common.layout.logout') }}
        </a-doption>
      </template>
    </a-dropdown>
  </div>
</template>

<style scoped>
.sidebar-footer {
  padding: 8px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}

.theme-menu-btn {
  padding: 0;
  margin: 0;
  box-sizing: border-box;
  width: 28px;
  height: 28px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  color: var(--sidebar-text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
  font-size: 16px;
  line-height: 1;
}

.theme-menu-btn:hover {
  background: var(--sidebar-bg-hover);
  color: var(--sidebar-text-primary);
}

.sidebar-user {
  flex: 1;
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
  font-size: 13px;
  color: var(--sidebar-text-secondary);
}
</style>
