<script setup lang="ts">
import { computed, type Component } from 'vue'
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
import IconOutsourcingConfig from '@/components/icons/IconOutsourcingConfig.vue'
import IconNotification from '@/components/icons/IconNotification.vue'
import { usePermission } from '@/hooks/usePermission'
import OrgSelector from '@/components/auth/OrgSelector.vue'

const { t } = useI18n()

defineProps<{
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
    key: 'outsourcing-config',
    titleKey: 'admin.menu.outsourcingConfig',
    icon: IconOutsourcingConfig,
    path: '/admin/outsourcing-config',
  },
  {
    key: 'workload-management',
    titleKey: 'admin.menu.workloadManagement',
    icon: IconSettings,
    path: '/admin/workload-management',
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
    :width="180"
    :collapsed-width="48"
    collapsible
    hide-trigger
    breakpoint="lg"
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
</style>
