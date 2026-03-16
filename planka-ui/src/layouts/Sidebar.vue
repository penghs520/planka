<script setup lang="ts">
import { computed, type Component, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { IconSettings } from '@arco-design/web-vue/es/icon'
import IconAuditLog from '@/components/icons/IconAuditLog.vue'
import IconMember from '@/components/icons/IconMember.vue'
import IconStructure from '@/components/icons/IconStructure.vue'
import IconLinkType from '@/components/icons/IconLinkType.vue'
import IconFormulaDefinition from '@/components/icons/IconFormulaDefinition.vue'
import IconCardType from '@/components/icons/IconCardType.vue'
import IconView from '@/components/icons/IconView.vue'
import IconMenuConfig from '@/components/icons/IconMenuConfig.vue'
import IconNotification from '@/components/icons/IconNotification.vue'
import { usePermission } from '@/hooks/usePermission'
import OrgSelector from '@/components/auth/OrgSelector.vue'

const { t } = useI18n()

const props = defineProps<{
  collapsed: boolean
}>()

defineEmits<{
  'update:collapsed': [value: boolean]
}>()

const route = useRoute()
const router = useRouter()
const { isAdmin } = usePermission()

const selectedKeys = computed(() => {
  const activeMenu = route.meta.activeMenu as string | undefined
  return activeMenu ? [activeMenu] : []
})

// 侧边栏宽度配置
const DEFAULT_WIDTH = 200
const MIN_WIDTH = 160
const MAX_WIDTH = 400
const SIDEBAR_WIDTH_KEY = 'sidebar-width'

const sidebarWidth = ref(DEFAULT_WIDTH)
const isResizing = ref(false)

// 从本地存储读取保存的宽度
onMounted(() => {
  const savedWidth = localStorage.getItem(SIDEBAR_WIDTH_KEY)
  if (savedWidth) {
    sidebarWidth.value = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, parseInt(savedWidth, 10)))
  }
})

// 开始拖拽
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

onUnmounted(() => {
  document.removeEventListener('mousemove', () => {})
  document.removeEventListener('mouseup', () => {})
})

interface MenuItem {
  key: string
  titleKey: string
  icon: Component
  path: string
  visible?: () => boolean
}

const menuItems: MenuItem[] = [
  {
    key: 'card-type',
    titleKey: 'admin.menu.cardType',
    icon: IconCardType,
    path: '/admin/card-type',
  },
  {
    key: 'link-type',
    titleKey: 'admin.menu.linkType',
    icon: IconLinkType,
    path: '/admin/link-type',
  },
  {
    key: 'formula-definition',
    titleKey: 'admin.menu.formulaDefinition',
    icon: IconFormulaDefinition,
    path: '/admin/formula-definition',
  },
  {
    key: 'structure',
    titleKey: 'admin.menu.structure',
    icon: IconStructure,
    path: '/admin/structure',
  },
  {
    key: 'view',
    titleKey: 'admin.menu.view',
    icon: IconView,
    path: '/admin/view',
  },
  {
    key: 'menu',
    titleKey: 'admin.menu.menuConfig',
    icon: IconMenuConfig,
    path: '/admin/menu',
  },
  {
    key: 'audit-log',
    titleKey: 'admin.menu.auditLog',
    icon: IconAuditLog,
    path: '/admin/audit-log',
  },
  {
    key: 'notification-settings',
    titleKey: 'admin.menu.notificationSettings',
    icon: IconNotification,
    path: '/admin/notification-settings',
  },
  {
    key: 'members',
    titleKey: 'admin.menu.members',
    icon: IconMember,
    path: '/admin/members',
    visible: () => isAdmin.value,
  },
  {
    key: 'org-settings',
    titleKey: 'admin.menu.orgSettings',
    icon: IconSettings,
    path: '/admin/org-settings',
    visible: () => isAdmin.value,
  },
]

const visibleMenuItems = computed(() => {
  return menuItems.filter((item) => !item.visible || item.visible())
})

function handleMenuClick(key: string) {
  const item = menuItems.find((m) => m.key === key)
  if (item) {
    router.push(item.path)
  }
}
</script>

<template>
  <a-layout-sider
    :collapsed="collapsed"
    :width="sidebarWidth"
    :collapsed-width="48"
    collapsible
    hide-trigger
    breakpoint="lg"
    class="resizable-sidebar"
    @collapse="$emit('update:collapsed', $event)"
  >
    <div class="logo" :class="{ 'is-collapsed': collapsed }">
      <OrgSelector :collapsed="collapsed" />
    </div>

    <a-menu
      :selected-keys="selectedKeys"
      :collapsed="collapsed"
      @menu-item-click="handleMenuClick"
    >
      <a-menu-item v-for="item in visibleMenuItems" :key="item.key">
        <template #icon>
          <component :is="item.icon" />
        </template>
        {{ t(item.titleKey) }}
      </a-menu-item>
    </a-menu>

    <!-- 拖拽调整宽度的手柄 -->
    <div
      v-if="!collapsed"
      class="resize-handle"
      :class="{ 'is-resizing': isResizing }"
      @mousedown="startResize"
    />
  </a-layout-sider>
</template>

<style scoped>
.logo {
  display: flex;
  align-items: center;
  height: 48px;
  padding: 0 12px;
  border-bottom: 1px solid var(--color-border);
  overflow: hidden;
  transition: all 0.2s;
}

.logo.is-collapsed {
  padding: 0;
  justify-content: center;
}

/* 菜单样式调整 */
:deep(.arco-menu-inner) {
  font-size: 14px;
  padding: 0 8px;
}

:deep(.arco-menu-item) {
  font-size: 14px;
  height: 40px;
  padding: 0 8px 0 4px;
  margin: 4px 0;
  border-radius: 8px;
}

:deep(.arco-menu-item .arco-icon) {
  font-size: 17px;
  margin-right: 4px;
  margin-left: 14px;
}

/* 选中状态样式 */
:deep(.arco-menu-item.arco-menu-selected) {
  background-color: #eff6ff !important;
  color: #3370FF !important;
}

:deep(.arco-menu-item.arco-menu-selected .arco-icon) {
  color: #3370FF !important;
}

/* 确保选中时的文字颜色为蓝色 */
:deep(.arco-menu-item.arco-menu-selected .arco-menu-title) {
  color: #3370FF !important;
}

/* 悬停状态 */
:deep(.arco-menu-item:not(.arco-menu-selected):hover) {
  background-color: #F5F6F7;
}

/* 可调整宽度的侧边栏 */
.resizable-sidebar {
  position: relative;
}

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
  background-color: #3370FF;
}
</style>
