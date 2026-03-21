<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { usePermission } from '@/hooks/usePermission'
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

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { isAdmin } = usePermission()

const activeKey = computed(() => {
  const activeMenu = route.meta.activeMenu as string | undefined
  return activeMenu || ''
})

interface MenuItem {
  key: string
  titleKey: string
  icon: Component
  path: string
  visible?: () => boolean
}

interface MenuSection {
  key: string
  labelKey: string
  items: MenuItem[]
  visible?: () => boolean
}

const sections: MenuSection[] = [
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
    key: 'config',
    labelKey: 'admin.menuGroup.configuration',
    items: [
      { key: 'view', titleKey: 'admin.menu.view', icon: IconView, path: '/admin/view' },
      { key: 'menu', titleKey: 'admin.menu.menuConfig', icon: IconMenuConfig, path: '/admin/menu' },
    ],
  },
  {
    key: 'operations',
    labelKey: 'admin.menuGroup.operations',
    items: [
      { key: 'audit-log', titleKey: 'admin.menu.auditLog', icon: IconAuditLog, path: '/admin/audit-log' },
      { key: 'notification-settings', titleKey: 'admin.menu.notificationSettings', icon: IconNotification, path: '/admin/notification-settings' },
    ],
  },
  {
    key: 'admin',
    labelKey: 'admin.menuGroup.administration',
    visible: () => isAdmin.value,
    items: [
      { key: 'members', titleKey: 'admin.menu.members', icon: IconMember, path: '/admin/members' },
      { key: 'org-settings', titleKey: 'admin.menu.orgSettings', icon: IconSettings, path: '/admin/org-settings' },
    ],
  },
]

const visibleSections = computed(() => {
  return sections
    .filter(s => !s.visible || s.visible())
    .map(s => ({
      ...s,
      items: s.items.filter(item => !item.visible || item.visible()),
    }))
    .filter(s => s.items.length > 0)
})

function navigateTo(item: MenuItem) {
  router.push(item.path)
}
</script>

<template>
  <div v-if="isAdmin" class="sidebar-admin">
    <template v-for="section in visibleSections" :key="section.key">
      <div class="section-label">{{ t(section.labelKey) }}</div>
      <div
        v-for="item in section.items"
        :key="item.key"
        class="menu-item"
        :class="{ 'menu-item--active': activeKey === item.key }"
        @click="navigateTo(item)"
      >
        <component :is="item.icon" class="menu-item-icon" />
        <span class="menu-item-text">{{ t(item.titleKey) }}</span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.sidebar-admin {
  padding: 4px 0;
}

.section-label {
  font-size: 11px;
  color: var(--sidebar-text-muted);
  font-weight: 500;
  padding: 14px 16px 4px;
  line-height: 1;
}

.section-label:first-child {
  padding-top: 6px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 28px;
  padding: 0 8px 0 16px;
  margin: 0 8px;
  border-radius: 5px;
  color: var(--sidebar-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.1s ease;
}

.menu-item:hover {
  background: var(--sidebar-bg-hover);
  color: var(--sidebar-text-primary);
}

.menu-item--active {
  background: var(--sidebar-bg-active);
  color: var(--sidebar-text-primary);
}

.menu-item-icon {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
  color: var(--sidebar-text-muted);
  opacity: 0.7;
  transition: all 0.1s;
}

.menu-item:hover .menu-item-icon {
  color: var(--sidebar-text-secondary);
  opacity: 1;
}

.menu-item--active .menu-item-icon {
  color: var(--sidebar-text-primary);
  opacity: 1;
}

.menu-item-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
