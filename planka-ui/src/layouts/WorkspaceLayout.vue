<script setup lang="ts">
import { ref, computed, provide, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { useOrgStore } from '@/stores/org'
import { useCardTabsStore } from '@/stores/cardTabs'
import { useAuth } from '@/hooks/useAuth'
import {
  IconSettings,
  IconPoweroff,
  IconUser,
  IconSwap,
  IconMenu,
} from '@arco-design/web-vue/es/icon'
import WorkspaceMenu from './WorkspaceMenu.vue'
import { CardDetailView } from '@/views/workspace/components/card-detail'
import LocaleSwitcher from '@/components/common/LocaleSwitcher.vue'
import logoImg from '@/assets/logo.png'
import type { ColumnMeta } from '@/types/view-data'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const orgStore = useOrgStore()
const cardTabsStore = useCardTabsStore()
const { logout } = useAuth()

// 当前选中的视图 ID
const selectedViewId = ref<string>('')

// 左侧菜单折叠状态
const menuCollapsed = ref(false)

// 当前视图名称（由子组件设置）
const currentViewName = ref('')

// 当前视图的列元数据（由子组件设置）
const currentViewColumns = ref<ColumnMeta[]>([])

function toggleMenu() {
  menuCollapsed.value = !menuCollapsed.value
}

function setViewName(name: string) {
  currentViewName.value = name
}

function setViewColumns(columns: ColumnMeta[]) {
  currentViewColumns.value = columns
}

// 提供给子组件
provide('menuCollapsed', menuCollapsed)
provide('toggleMenu', toggleMenu)
provide('setViewName', setViewName)
provide('setViewColumns', setViewColumns)
provide('cardTabsStore', cardTabsStore)

// 用户显示名称
const displayName = computed(() => userStore.user?.nickname || userStore.user?.email || '')

// 组织名称
const orgName = computed(() => orgStore.currentOrg?.name || '')

// 获取名称首字符作为头像
const avatarText = computed(() => {
  const name = displayName.value
  return name ? name.charAt(0).toUpperCase() : 'U'
})

// 当前打开的卡片详情抽屉
const drawerCardId = computed(() => cardTabsStore.activeTabId)
const drawerVisible = computed(() => drawerCardId.value !== null)

// 处理视图选择
function handleViewSelect(viewId: string) {
  selectedViewId.value = viewId
  router.push({ path: '/workspace', query: { viewId } })
}

// 关闭抽屉
function handleDrawerClose() {
  cardTabsStore.closeAllTabs()
}

// 用户菜单点击
function handleUserMenuClick(key: string) {
  switch (key) {
    case 'profile':
      router.push('/profile')
      break
    case 'switch-org':
      router.push('/select-org')
      break
    case 'admin':
      // 在新标签页中打开管理后台
      window.open(router.resolve('/admin').href, '_blank')
      break
    case 'logout':
      logout()
      break
  }
}

// 当切换视图时，关闭抽屉
// 当切换视图时，关闭抽屉
watch(selectedViewId, () => {
  cardTabsStore.closeAllTabs()
})
</script>

<template>
  <div class="workspace-layout">
    <!-- 左侧菜单区域 -->
    <aside class="workspace-sidebar" :class="{ collapsed: menuCollapsed }">
      <!-- Logo 和组织信息 -->
      <div class="org-header">
        <img :src="logoImg" alt="Logo" class="org-logo" />
        <span class="org-name">{{ orgName }}</span>
      </div>

      <!-- 菜单树 -->
      <WorkspaceMenu
        v-model="selectedViewId"
        @select="handleViewSelect"
      />
    </aside>

    <!-- 右侧内容区域 -->
    <div class="workspace-main">
      <!-- 顶部工具栏 -->
      <header class="workspace-header">
        <div class="header-left">
          <a-button
            type="text"
            class="menu-toggle-btn"
            @click="toggleMenu"
          >
            <template #icon><IconMenu /></template>
          </a-button>

          <!-- 视图名称标签 -->
          <a-tag v-if="currentViewName" class="view-name-tag">
            {{ currentViewName }}
          </a-tag>
        </div>
        <div class="header-right">
          <LocaleSwitcher />
          <a-dropdown trigger="click" @select="handleUserMenuClick">
            <div class="user-avatar">
              {{ avatarText }}
            </div>
            <template #content>
              <a-doption value="profile">
                <IconUser /> {{ t('common.layout.profile') }}
              </a-doption>
              <a-doption value="switch-org">
                <IconSwap /> {{ t('common.layout.switchOrg') }}
              </a-doption>
              <a-doption value="admin">
                <IconSettings /> {{ t('common.layout.adminPanel') }}
              </a-doption>
              <a-divider style="margin: 4px 0" />
              <a-doption value="logout">
                <IconPoweroff /> {{ t('common.layout.logout') }}
              </a-doption>
            </template>
          </a-dropdown>
        </div>
      </header>

      <!-- 主内容区域 -->
      <main class="workspace-content">
        <div class="view-content">
          <router-view />
        </div>
      </main>
    </div>

    <!-- 卡片详情抽屉 -->
    <a-drawer
      :visible="drawerVisible"
      :width="900"
      :footer="false"
      :header="false"
      :mask-closable="true"
      :esc-to-close="true"
      placement="right"
      class="card-detail-drawer"
      @cancel="handleDrawerClose"
    >
      <CardDetailView
        v-if="drawerCardId"
        :card-id="drawerCardId"
      />
    </a-drawer>
  </div>
</template>

<style scoped lang="scss">
.workspace-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--color-bg-1);
}

.workspace-sidebar {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-2);
  border-right: 1px solid var(--color-border);
  transition: width 0.2s ease;

  &.collapsed {
    width: 0;
    overflow: hidden;
    border-right: none;
  }
}

.org-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.org-logo {
  height: 40px;
  width: auto;
  object-fit: contain;
}

.org-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.workspace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 40px;
  padding: 0 16px 0 0;
  background: var(--color-bg-1);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-toggle-btn {
  color: rgb(var(--primary-5));
  padding: 0 4px;

  &:hover {
    color: rgb(var(--primary-6));
    background: var(--color-fill-2);
  }
}

.view-name-tag {
  font-size: 13px;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 4px;
  background-color: rgb(var(--primary-5)) !important;
  color: #fff !important;
  flex-shrink: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  margin-left: 16px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgb(var(--primary-6));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.85;
  }
}

.workspace-content {
  flex: 1;
  overflow: hidden;
}

.view-content {
  height: 100%;
  overflow: hidden;
}
</style>

<style lang="scss">
// 全局样式 - drawer 被 portal 到 body，scoped 无法穿透
.card-detail-drawer {
  .arco-drawer-body {
    padding: 0 !important;
    overflow: hidden;
  }
}
</style>
