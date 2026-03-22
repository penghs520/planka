<script setup lang="ts">
import { computed, type Component, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { useOrgStore } from '@/stores/org'
import { IconSettings } from '@arco-design/web-vue/es/icon'
import { IconMenuFold, IconMenuUnfold } from '@arco-design/web-vue/es/icon'
import IconAuditLog from '@/components/icons/IconAuditLog.vue'
import IconStructure from '@/components/icons/IconStructure.vue'
import IconLinkType from '@/components/icons/IconLinkType.vue'
import IconFormulaDefinition from '@/components/icons/IconFormulaDefinition.vue'
import IconCardType from '@/components/icons/IconCardType.vue'
import { usePermission } from '@/hooks/usePermission'
import OrgSelector from '@/components/auth/OrgSelector.vue'
import UserDropdown from '@/components/auth/UserDropdown.vue'

const { t } = useI18n()

const props = defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  'update:collapsed': [value: boolean]
}>()

const route = useRoute()
const router = useRouter()
const { isAdmin } = usePermission()
const userStore = useUserStore()
const orgStore = useOrgStore()

// 当前选中菜单
const activeKey = computed(() => {
  const activeMenu = route.meta.activeMenu as string | undefined
  return activeMenu || ''
})

// ========================================
// 侧边栏宽度拖拽调整
// ========================================
const DEFAULT_WIDTH = 240
const MIN_WIDTH = 200
const MAX_WIDTH = 400
const SIDEBAR_WIDTH_KEY = 'sidebar-width'

const sidebarWidth = ref(DEFAULT_WIDTH)
const isResizing = ref(false)

onMounted(() => {
  const savedWidth = localStorage.getItem(SIDEBAR_WIDTH_KEY)
  if (savedWidth) {
    sidebarWidth.value = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, parseInt(savedWidth, 10)))
  }
})

function startResize(e: MouseEvent) {
  if (props.collapsed) return
  isResizing.value = true
  const startX = e.clientX
  const startWidth = sidebarWidth.value

  function handleMouseMove(e: MouseEvent) {
    const delta = e.clientX - startX
    const newWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, startWidth + delta))
    sidebarWidth.value = newWidth
  }

  function handleMouseUp() {
    isResizing.value = false
    localStorage.setItem(SIDEBAR_WIDTH_KEY, sidebarWidth.value.toString())
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

// ========================================
// 菜单分组定义
// ========================================
interface MenuItem {
  key: string
  titleKey: string
  icon: Component
  path: string
  visible?: () => boolean
}

interface MenuGroup {
  key: string
  labelKey: string
  items: MenuItem[]
  visible?: () => boolean
}

const menuGroups: MenuGroup[] = [
  {
    key: 'schema',
    labelKey: 'admin.menuGroup.schemaDefinition',
    items: [
      { key: 'card-type', titleKey: 'admin.menu.cardType', icon: IconCardType, path: '/admin/card-type' },
      { key: 'link-type', titleKey: 'admin.menu.linkType', icon: IconLinkType, path: '/admin/link-type' },
      { key: 'formula-definition', titleKey: 'admin.menu.formulaDefinition', icon: IconFormulaDefinition, path: '/admin/formula-definition' },
      { key: 'structure', titleKey: 'admin.menu.structure', icon: IconStructure, path: '/admin/structure' },
    ],
  },
  {
    key: 'operations',
    labelKey: 'admin.menuGroup.operations',
    items: [
      { key: 'audit-log', titleKey: 'admin.menu.auditLog', icon: IconAuditLog, path: '/admin/audit-log' },
    ],
  },
  {
    key: 'admin',
    labelKey: 'admin.menuGroup.administration',
    visible: () => isAdmin.value,
    items: [
      { key: 'org-settings', titleKey: 'admin.menu.orgSettings', icon: IconSettings, path: '/admin/org-settings' },
    ],
  },
]

const visibleGroups = computed(() => {
  return menuGroups
    .filter(g => !g.visible || g.visible())
    .map(g => ({
      ...g,
      items: g.items.filter(item => !item.visible || item.visible()),
    }))
    .filter(g => g.items.length > 0)
})

function navigateTo(item: MenuItem) {
  router.push(item.path)
}

// ========================================
// 数据获取（从 Header.vue 移入）
// ========================================
onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.user) {
    try { await userStore.fetchMe() } catch { /* interceptor handles */ }
  }
  if (userStore.isLoggedIn && orgStore.myOrgs.length === 0) {
    try { await orgStore.fetchMyOrganizations() } catch { /* interceptor handles */ }
  }
})
</script>

<template>
  <aside
    class="admin-sidebar"
    :style="{ width: collapsed ? '64px' : sidebarWidth + 'px' }"
  >
    <!-- 顶部：组织选择器 -->
    <div class="sidebar-top">
      <OrgSelector :collapsed="collapsed" />
    </div>

    <!-- 中间：可滚动的菜单分组 -->
    <nav class="sidebar-nav">
      <template v-for="group in visibleGroups" :key="group.key">
        <div v-if="!collapsed" class="sidebar-group-label">
          {{ t(group.labelKey) }}
        </div>
        <a-tooltip
          v-for="item in group.items"
          :key="item.key"
          :content="collapsed ? t(item.titleKey) : ''"
          position="right"
          :disabled="!collapsed"
          mini
        >
          <div
            class="sidebar-item"
            :class="{
              'sidebar-item--active': activeKey === item.key,
              'sidebar-item--collapsed': collapsed,
            }"
            @click="navigateTo(item)"
          >
            <component :is="item.icon" class="sidebar-item-icon" />
            <span v-if="!collapsed" class="sidebar-item-text">
              {{ t(item.titleKey) }}
            </span>
          </div>
        </a-tooltip>
      </template>
    </nav>

    <!-- 底部：用户控件 -->
    <div class="sidebar-bottom">
      <div class="sidebar-controls">
        <button
          class="sidebar-collapse-btn"
          @click="emit('update:collapsed', !collapsed)"
        >
          <IconMenuFold v-if="!collapsed" />
          <IconMenuUnfold v-else />
        </button>
      </div>
      <div v-if="!collapsed" class="sidebar-user">
        <UserDropdown />
      </div>
    </div>

    <!-- 拖拽调整宽度手柄 -->
    <div
      v-if="!collapsed"
      class="resize-handle"
      :class="{ 'is-resizing': isResizing }"
      @mousedown="startResize"
    />
  </aside>
</template>

<style scoped>
.admin-sidebar {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--sidebar-bg, #FAFBFC);
  border-right: 1px solid var(--sidebar-border, #EDEEF0);
  position: relative;
  transition: width 0.2s ease;
  flex-shrink: 0;
  overflow: hidden;
  user-select: none;
}

/* 顶部区域 */
.sidebar-top {
  padding: 8px 12px;
  border-bottom: 1px solid var(--sidebar-border, #EDEEF0);
  flex-shrink: 0;
}

/* 菜单导航区域 */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 0;
}

.sidebar-nav::-webkit-scrollbar {
  width: 4px;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 2px;
}

.sidebar-nav:hover::-webkit-scrollbar-thumb {
  background: var(--color-fill-3, #DEE0E3);
}

/* 分组标签 */
.sidebar-group-label {
  font-size: var(--menu-group-label-size, 11px);
  color: var(--menu-group-label-color, #9CA3AF);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 16px 16px 4px;
  line-height: 1;
}

/* 菜单项 */
.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: var(--menu-item-height, 32px);
  padding: 0 12px;
  margin: 1px 8px;
  border-radius: var(--menu-item-radius, 6px);
  color: var(--menu-item-text-color, #4B5563);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.sidebar-item:hover {
  background: var(--menu-item-hover-bg, #F0F1F3);
}

.sidebar-item--active {
  background: var(--menu-item-active-bg, #EEF0FF);
  color: var(--menu-item-active-color, #3B5BDB);
  font-weight: 500;
}

.sidebar-item--active .sidebar-item-icon {
  color: var(--menu-item-active-color, #3B5BDB);
}

.sidebar-item--collapsed {
  justify-content: center;
  padding: 0;
  margin: 1px 8px;
}

.sidebar-item-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: var(--color-text-3, #8F959E);
  transition: color 0.15s ease;
}

.sidebar-item:hover .sidebar-item-icon {
  color: var(--color-text-2, #646A73);
}

.sidebar-item--active .sidebar-item-icon {
  color: var(--menu-item-active-color, #3B5BDB);
}

.sidebar-item-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 底部区域 */
.sidebar-bottom {
  border-top: 1px solid var(--sidebar-border, #EDEEF0);
  padding: 8px 12px;
  flex-shrink: 0;
}

.sidebar-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 4px;
}

.sidebar-collapse-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  color: var(--color-text-3, #8F959E);
  cursor: pointer;
  transition: all 0.15s ease;
  font-size: 16px;
}

.sidebar-collapse-btn:hover {
  background: var(--menu-item-hover-bg, #F0F1F3);
  color: var(--color-text-1, #1F2329);
}

.sidebar-user {
  margin-top: 4px;
}

/* 拖拽手柄 */
.resize-handle {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  cursor: col-resize;
  background-color: transparent;
  transition: background-color 0.2s;
  z-index: 100;
}

.resize-handle:hover,
.resize-handle.is-resizing {
  background-color: var(--color-primary, #0083E0);
}
</style>
