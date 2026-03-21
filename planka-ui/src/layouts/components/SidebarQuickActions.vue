<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  IconSearch,
  IconNotification,
  IconUser,
  IconArrowLeft,
} from '@arco-design/web-vue/es/icon'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const isMac = navigator.platform.toUpperCase().includes('MAC')

const isAdminRoute = computed(() => route.path.startsWith('/admin'))

defineEmits<{
  'open-command-palette': []
}>()

const items = computed(() => [
  {
    key: 'inbox',
    label: t('sidebar.inbox'),
    icon: IconNotification,
    path: '/inbox',
    // TODO: Phase 4 接入真实未读数
    badge: 0,
  },
  {
    key: 'my-issues',
    label: t('sidebar.myIssues'),
    icon: IconUser,
    path: '/my-issues',
  },
])

function isActive(path: string) {
  return route.path === path
}

function navigateTo(path: string) {
  router.push(path)
}

function goWorkspace() {
  router.push('/workspace')
}
</script>

<template>
  <div class="quick-actions">
    <!-- 管理后台：先回工作区，不展示工作区快捷入口 -->
    <template v-if="isAdminRoute">
      <div
        class="quick-action-item quick-action-item--back"
        @click="goWorkspace"
      >
        <IconArrowLeft class="quick-action-icon" />
        <span class="quick-action-label">{{ t('sidebar.backToWorkspace') }}</span>
      </div>
    </template>
    <template v-else>
      <div
        v-for="item in items"
        :key="item.key"
        class="quick-action-item"
        :class="{ active: isActive(item.path) }"
        @click="navigateTo(item.path)"
      >
        <component :is="item.icon" class="quick-action-icon" />
        <span class="quick-action-label">{{ item.label }}</span>
        <span
          v-if="item.badge && item.badge > 0"
          class="quick-action-badge"
        >
          {{ item.badge > 99 ? '99+' : item.badge }}
        </span>
      </div>
    </template>

    <!-- 搜索触发器 -->
    <div
      class="quick-action-item"
      @click="$emit('open-command-palette')"
    >
      <IconSearch class="quick-action-icon" />
      <span class="quick-action-label">{{ t('sidebar.search') }}</span>
      <kbd class="quick-action-shortcut">{{ isMac ? '⌘' : 'Ctrl' }}K</kbd>
    </div>
  </div>
</template>

<style scoped>
.quick-actions {
  padding: 4px 8px 8px;
  flex-shrink: 0;
}

.quick-action-item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 28px;
  padding: 0 8px;
  border-radius: 5px;
  color: var(--sidebar-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.1s ease;
  position: relative;
}

.quick-action-item:hover {
  background: var(--sidebar-bg-hover);
  color: var(--sidebar-text-primary);
}

.quick-action-item.active {
  background: var(--sidebar-bg-active);
  color: var(--sidebar-text-active);
}

.quick-action-icon {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
  opacity: 0.7;
}

.quick-action-item:hover .quick-action-icon,
.quick-action-item.active .quick-action-icon {
  color: var(--sidebar-text-active);
  opacity: 1;
}

.quick-action-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-action-badge {
  background: var(--sidebar-accent);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  padding: 0 5px;
  height: 16px;
  line-height: 16px;
  border-radius: 8px;
  flex-shrink: 0;
}

.quick-action-shortcut {
  font-size: 11px;
  color: var(--sidebar-text-muted);
  background: none;
  border: 1px solid var(--sidebar-border);
  border-radius: 3px;
  padding: 0 4px;
  font-family: inherit;
  line-height: 1.4;
  flex-shrink: 0;
}
</style>
