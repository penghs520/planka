<script setup lang="ts">
import { computed, provide, ref, watch, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useOrgStore } from '@/stores/org'
import { useCardTabsStore } from '@/stores/cardTabs'
import { useSidebarTheme } from '@/composables/useSidebarTheme'
import AppSidebar from './components/AppSidebar.vue'
import { CardDetailView } from '@/views/workspace/components/card-detail'
import OrgSwitchDetector from '@/components/common/OrgSwitchDetector.vue'
import type { ColumnMeta } from '@/types/view-data'

// 初始化主题
useSidebarTheme()

const userStore = useUserStore()
const orgStore = useOrgStore()
const cardTabsStore = useCardTabsStore()

// 当前视图名称和列（由子组件设置）
const currentViewName = ref('')
const currentViewColumns = ref<ColumnMeta[]>([])

function setViewName(name: string) {
  currentViewName.value = name
}

function setViewColumns(columns: ColumnMeta[]) {
  currentViewColumns.value = columns
}

// Provide 给子组件
provide('setViewName', setViewName)
provide('setViewColumns', setViewColumns)
provide('cardTabsStore', cardTabsStore)

// 卡片详情抽屉
const drawerCardId = computed(() => cardTabsStore.activeTabId)
const drawerVisible = computed(() => drawerCardId.value !== null)

function handleDrawerClose() {
  cardTabsStore.closeAllTabs()
}

// 命令面板状态（Phase 2 接入）
const commandPaletteVisible = ref(false)

function openCommandPalette() {
  commandPaletteVisible.value = true
}

// 获取用户信息和组织列表
onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.user) {
    try { await userStore.fetchMe() } catch { /* interceptor handles */ }
  }
  if (userStore.isLoggedIn && orgStore.myOrgs.length === 0) {
    try { await orgStore.fetchMyOrganizations() } catch { /* interceptor handles */ }
  }
  // 获取当前用户在当前组织的角色
  if (userStore.isLoggedIn && orgStore.currentOrgId && !orgStore.myRole) {
    try { await orgStore.fetchCurrentOrgRole() } catch { /* interceptor handles */ }
  }
})
</script>

<template>
  <div class="app-layout">
    <!-- 侧边栏 -->
    <AppSidebar @open-command-palette="openCommandPalette" />

    <!-- 主内容区域 -->
    <main class="app-main">
      <router-view />
    </main>

    <!-- 组织切换检测 -->
    <OrgSwitchDetector v-if="orgStore.currentOrgId" />

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

    <!-- TODO: Phase 2 - CommandPalette 组件 -->
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--color-bg-1);
}

.app-main {
  flex: 1;
  overflow: auto;
  background: var(--color-bg-1);
  border-left: 1px solid var(--color-border-3);
}
</style>

<style lang="scss">
/* 全局样式 - drawer 被 portal 到 body */
.card-detail-drawer {
  .arco-drawer-body {
    padding: 0 !important;
    overflow: hidden;
  }
}
</style>
