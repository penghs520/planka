<script setup lang="ts">
import { ref, computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  IconDown,
  IconCheckSquare,
  IconFolder,
  IconUserGroup,
  IconIdcard,
  IconLayout,
} from '@arco-design/web-vue/es/icon'

const { t } = useI18n()
const route = useRoute()
const expanded = ref(true)

/** 与快捷入口一致：Arco 线性图标，语义贴近 Linear / 项目管理 */
const links = computed(() => [
  {
    to: '/workspace/issues',
    labelKey: 'sidebar.issues' as const,
    match: 'exact' as const,
    path: '/workspace/issues',
    icon: IconCheckSquare,
  },
  {
    to: '/workspace/projects',
    labelKey: 'sidebar.projects' as const,
    match: 'exact' as const,
    path: '/workspace/projects',
    icon: IconFolder,
  },
  {
    to: '/workspace/teams',
    labelKey: 'sidebar.teams' as const,
    match: 'exact' as const,
    path: '/workspace/teams',
    icon: IconUserGroup,
  },
  {
    to: '/admin/members',
    labelKey: 'sidebar.members' as const,
    match: 'prefix' as const,
    path: '/admin/members',
    icon: IconIdcard,
  },
  {
    to: '/workspace',
    labelKey: 'sidebar.views' as const,
    match: 'workspaceRoot' as const,
    path: '/workspace',
    icon: IconLayout,
  },
])

function isActive(
  path: string,
  match: 'exact' | 'prefix' | 'workspaceRoot',
) {
  if (match === 'workspaceRoot') {
    return route.path === '/workspace'
  }
  if (match === 'prefix') {
    return route.path === path || route.path.startsWith(`${path}/`)
  }
  return route.path === path || route.path.startsWith(`${path}/`)
}
</script>

<template>
  <div class="sidebar-workspace">
    <button
      type="button"
      class="section-header"
      @click="expanded = !expanded"
    >
      <IconDown
        class="chevron"
        :class="{ 'chevron--collapsed': !expanded }"
      />
      <span class="section-title">{{ t('sidebar.workspaceSection') }}</span>
    </button>
    <div
      v-show="expanded"
      class="section-body"
    >
      <RouterLink
        v-for="item in links"
        :key="item.to"
        :to="item.to"
        class="nav-row"
        :class="{ active: isActive(item.path, item.match) }"
      >
        <component :is="item.icon" class="nav-icon" />
        <span class="nav-label">{{ t(item.labelKey) }}</span>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.sidebar-workspace {
  padding: 4px 8px 8px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
  height: 28px;
  padding: 0 8px;
  margin: 0;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: var(--sidebar-text-secondary);
  font-family: var(--sidebar-nav-font-family);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  cursor: pointer;
  text-align: left;
}

.section-header:hover {
  background: var(--sidebar-bg-hover);
  color: var(--sidebar-text-primary);
}

.chevron {
  width: 12px;
  height: 12px;
  transition: transform 0.15s ease;
  flex-shrink: 0;
}

.chevron--collapsed {
  transform: rotate(-90deg);
}

/* 与分区标题（chevron + 文案）错层：整体右移，避免与一级 chevron 纵向对齐 */
.section-body {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding-top: 2px;
  padding-left: 14px;
}

.nav-row {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 28px;
  padding: 0 8px;
  border-radius: 5px;
  font-family: var(--sidebar-nav-font-family);
  font-size: var(--sidebar-nav-font-size);
  font-weight: var(--sidebar-nav-font-weight);
  color: var(--sidebar-text-secondary);
  text-decoration: none;
  transition: background 0.1s ease, color 0.1s ease;
}

.nav-icon {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
  opacity: 0.7;
}

.nav-row:hover .nav-icon,
.nav-row.active .nav-icon {
  color: var(--sidebar-text-active);
  opacity: 1;
}

.nav-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-row:hover {
  background: var(--sidebar-bg-hover);
  color: var(--sidebar-text-primary);
}

.nav-row.active {
  background: var(--sidebar-bg-active);
  color: var(--sidebar-text-active);
}
</style>
