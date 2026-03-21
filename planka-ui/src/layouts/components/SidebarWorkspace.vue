<script setup lang="ts">
import { ref, computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { IconDown } from '@arco-design/web-vue/es/icon'

const { t } = useI18n()
const route = useRoute()
const expanded = ref(true)

const links = computed(() => [
  { to: '/workspace/issues', labelKey: 'sidebar.issues' as const, match: 'exact' as const, path: '/workspace/issues' },
  { to: '/workspace/projects', labelKey: 'sidebar.projects' as const, match: 'exact' as const, path: '/workspace/projects' },
  { to: '/workspace/teams', labelKey: 'sidebar.teams' as const, match: 'exact' as const, path: '/workspace/teams' },
  { to: '/admin/members', labelKey: 'sidebar.members' as const, match: 'prefix' as const, path: '/admin/members' },
  { to: '/workspace', labelKey: 'sidebar.views' as const, match: 'workspaceRoot' as const, path: '/workspace' },
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
        {{ t(item.labelKey) }}
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

.section-body {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding-top: 2px;
}

.nav-row {
  display: flex;
  align-items: center;
  height: 28px;
  padding: 0 8px 0 22px;
  border-radius: 5px;
  font-size: 13px;
  color: var(--sidebar-text-secondary);
  text-decoration: none;
  transition: background 0.1s ease, color 0.1s ease;
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
