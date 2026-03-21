<script setup lang="ts">
import { ref, computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  IconDown,
  IconCheckSquare,
  IconFolder,
  IconUserGroup,
  IconIdcard,
  IconLayout,
} from '@arco-design/web-vue/es/icon'
import WorkspaceMenu from '@/layouts/WorkspaceMenu.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const expanded = ref(true)
/** 「视图」下后台菜单树展开/收起（与架构线分区 chevron 行为一致） */
const viewsMenuExpanded = ref(true)

/** Issues / Projects / Teams / Members — 视图单独挂载后台菜单树 */
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
    to: '/workspace/members',
    labelKey: 'sidebar.members' as const,
    match: 'exact' as const,
    path: '/workspace/members',
    icon: IconIdcard,
  },
])

const workspaceViewsActive = computed(() => route.path === '/workspace')

const selectedWorkspaceViewId = computed(() => {
  const v = route.query.viewId
  return typeof v === 'string' ? v : ''
})

function handleWorkspaceViewSelect(viewId: string) {
  router.push({ path: '/workspace', query: { viewId } })
}

/** 已在 /workspace 时 RouterLink 不会再次导航，需主动展开菜单树 */
function expandViewsMenuOnLinkClick() {
  viewsMenuExpanded.value = true
}

function isActive(path: string, match: 'exact' | 'prefix') {
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
      <span class="section-title">{{ t('sidebar.workspaceSection') }}</span>
      <IconDown
        class="chevron"
        :class="{ 'chevron--collapsed': !expanded }"
      />
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

      <div class="nav-views-branch">
        <div
          class="nav-views-row"
          :class="{ 'nav-views-row--active': workspaceViewsActive }"
        >
          <RouterLink
            to="/workspace"
            class="nav-row nav-views-link"
            :aria-current="workspaceViewsActive ? 'page' : undefined"
            @click="expandViewsMenuOnLinkClick"
          >
            <IconLayout class="nav-icon" />
            <span class="nav-label">{{ t('sidebar.views') }}</span>
          </RouterLink>
          <button
            type="button"
            class="nav-views-toggle"
            :aria-expanded="viewsMenuExpanded"
            :aria-label="viewsMenuExpanded ? t('sidebar.collapseViewsMenu') : t('sidebar.expandViewsMenu')"
            @click.stop="viewsMenuExpanded = !viewsMenuExpanded"
          >
            <IconDown
              class="nav-views-chevron"
              :class="{ 'nav-views-chevron--collapsed': !viewsMenuExpanded }"
            />
          </button>
        </div>
        <div
          v-show="viewsMenuExpanded"
          class="nav-views-embed"
        >
          <WorkspaceMenu
            variant="sidebar"
            :model-value="selectedWorkspaceViewId"
            @select="handleWorkspaceViewSelect"
          />
        </div>
      </div>
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

.section-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
}

/* 与分区标题文案左缘对齐（收起图标在标题右侧） */
.section-body {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding-top: 2px;
  padding-left: 8px;
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
  font-weight: var(--sidebar-nav-font-weight-active);
}

.nav-views-branch {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.nav-views-row {
  display: flex;
  align-items: stretch;
  min-height: 28px;
  border-radius: 5px;
  overflow: hidden;
  transition: background 0.1s ease, color 0.1s ease;
}

.nav-views-row:hover {
  background: var(--sidebar-bg-hover);
}

.nav-views-row:hover .nav-views-link,
.nav-views-row:hover .nav-views-toggle {
  color: var(--sidebar-text-primary);
}

.nav-views-row:hover .nav-views-link .nav-icon {
  color: var(--sidebar-text-primary);
  opacity: 1;
}

.nav-views-row--active {
  background: var(--sidebar-bg-active);
}

.nav-views-row--active .nav-views-link,
.nav-views-row--active .nav-views-toggle {
  color: var(--sidebar-text-active);
  font-weight: var(--sidebar-nav-font-weight-active);
}

.nav-views-row--active .nav-views-link .nav-icon {
  color: var(--sidebar-text-active);
  opacity: 1;
}

.nav-views-row--active:hover {
  background: var(--sidebar-bg-active);
}

.nav-views-toggle {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  min-width: 28px;
  width: 28px;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  color: var(--sidebar-text-secondary);
  cursor: pointer;
  transition: color 0.1s ease;
}

.nav-views-toggle:focus-visible {
  outline: 2px solid var(--sidebar-accent);
  outline-offset: -2px;
}

.nav-views-chevron {
  width: 12px;
  height: 12px;
  transition: transform 0.15s ease;
  flex-shrink: 0;
}

.nav-views-chevron--collapsed {
  transform: rotate(-90deg);
}

.nav-views-link {
  flex: 1;
  min-width: 0;
  margin: 0;
  border-radius: 0;
  background: transparent !important;
  color: var(--sidebar-text-secondary);
  font-weight: var(--sidebar-nav-font-weight);
}

.nav-views-row .nav-views-link.nav-row:hover {
  background: transparent !important;
}

.nav-views-embed {
  padding: 0 0 2px 8px;
  margin-left: 0;
}

.nav-views-embed :deep(.group-header) {
  color: var(--sidebar-text-secondary);
  font-family: var(--sidebar-nav-font-family);
  font-size: 12px;
  padding: 4px 6px;
  border-radius: 5px;
}

.nav-views-embed :deep(.group-header:hover) {
  color: var(--sidebar-text-primary);
  background: var(--sidebar-bg-hover);
}

.nav-views-embed :deep(.menu-item) {
  margin: 1px 0;
  padding: 4px 8px;
  border-radius: 5px;
  font-family: var(--sidebar-nav-font-family);
  font-size: var(--sidebar-nav-font-size);
  font-weight: var(--sidebar-nav-font-weight);
}

.nav-views-embed :deep(.menu-item:hover) {
  background: var(--sidebar-bg-hover);
}

.nav-views-embed :deep(.menu-item.active) {
  background: var(--sidebar-bg-active);
  color: var(--sidebar-text-active);
  font-weight: var(--sidebar-nav-font-weight-active);
}

.nav-views-embed :deep(.menu-item.active .item-icon) {
  color: var(--sidebar-text-active);
  opacity: 1;
}

.nav-views-embed :deep(.item-icon) {
  color: var(--sidebar-text-secondary);
  opacity: 0.7;
}
</style>
