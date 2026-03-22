<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { useStructureNavStore } from '@/stores/structureNav'
import { useStructureNodeViewsMenu } from '@/composables/useStructureNodeViewsMenu'
import { viewApi } from '@/api/view'
import MenuNode from '@/layouts/components/MenuNode.vue'
import ViewEditForm from '@/views/schema-definition/view/ViewEditForm.vue'
import type { MenuTreeNodeVO } from '@/types/menu'
import type { ListViewDefinition } from '@/types/view'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const structureNav = useStructureNavStore()
const { state: viewsMenu, workspaceLinkForView, reloadMenu } = useStructureNodeViewsMenu()

const createViewVisible = ref(false)
const createViewSaving = ref(false)

const expandedGroups = ref(new Set<string>())

function collectExpandedInto(nodes: MenuTreeNodeVO[], into: Set<string>) {
  for (const n of nodes) {
    if (n.type === 'GROUP') {
      if (n.expanded !== false) {
        into.add(n.id)
      }
      if (n.children?.length) {
        collectExpandedInto(n.children, into)
      }
    }
  }
}

watch(
  () => viewsMenu.menuTree,
  (tree) => {
    const next = new Set<string>()
    if (tree?.roots?.length) {
      collectExpandedInto(tree.roots, next)
    }
    if (tree?.ungroupedViews?.length) {
      collectExpandedInto(tree.ungroupedViews, next)
    }
    expandedGroups.value = next
  },
  { immediate: true },
)

function toggleGroup(groupId: string) {
  const next = new Set(expandedGroups.value)
  if (next.has(groupId)) {
    next.delete(groupId)
  } else {
    next.add(groupId)
  }
  expandedGroups.value = next
}

function isGroupExpanded(groupId: string) {
  return expandedGroups.value.has(groupId)
}

function isStructureViewSelected(viewId: string) {
  const q = route.query.viewId
  const cur = typeof q === 'string' ? q : Array.isArray(q) ? String(q[0] ?? '') : ''
  return cur === viewId
}

const hasViewsMenuTree = computed(() => {
  const m = viewsMenu.menuTree
  if (!m) {
    return false
  }
  return m.roots.length > 0 || (m.ungroupedViews?.length ?? 0) > 0
})

const structureId = computed(() => route.params.structureId as string)
const nodeId = computed(() => route.params.nodeId as string)

const node = computed(() => {
  if (!structureId.value || !nodeId.value) return undefined
  return structureNav.findNode(structureId.value, nodeId.value)
})

const nodeName = computed(() => node.value?.name ?? '')
const levelName = computed(() => node.value?.levelName ?? '')

const basePath = computed(
  () => `/structure/${structureId.value}/node/${nodeId.value}`,
)

const activeSection = computed(() => {
  const path = route.path
  if (path.endsWith('/projects')) return 'projects'
  if (path.endsWith('/views')) return 'views'
  return 'issues'
})

const nodeNavItems = computed(() => [
  { key: 'issues' as const, labelKey: 'sidebar.teamIssues' },
  { key: 'projects' as const, labelKey: 'sidebar.teamProjects' },
])

watch(
  structureId,
  (sid) => {
    if (sid) {
      void structureNav.ensureTree(sid)
    }
  },
  { immediate: true },
)

async function handleCreateViewSave(def: ListViewDefinition) {
  createViewSaving.value = true
  try {
    const created = await viewApi.create(def)
    Message.success(t('common.state.success'))
    createViewVisible.value = false
    await reloadMenu()
    if (created.id) {
      await router.push(workspaceLinkForView(created.id))
    }
  } catch {
    Message.error(t('sidebar.createViewFailed'))
  } finally {
    createViewSaving.value = false
  }
}
</script>

<template>
  <div class="structure-node-layout">
    <header class="page-header">
      <h1 class="page-title">{{ nodeName }}</h1>
      <span
        v-if="levelName"
        class="level-tag"
      >{{ levelName }}</span>
    </header>
    <div class="node-body">
      <aside
        class="node-submenu"
        :aria-label="t('sidebar.structureNodeSectionNav')"
      >
        <nav class="node-submenu-nav">
          <RouterLink
            v-for="item in nodeNavItems"
            :key="item.key"
            :to="`${basePath}/${item.key}`"
            class="node-submenu-link"
            :class="{ 'node-submenu-link--active': activeSection === item.key }"
          >
            {{ t(item.labelKey) }}
          </RouterLink>
          <div class="node-submenu-group">
            <RouterLink
              :to="`${basePath}/views`"
              class="node-submenu-link"
              :class="{ 'node-submenu-link--active': activeSection === 'views' }"
            >
              {{ t('sidebar.teamViews') }}
            </RouterLink>
            <div
              v-if="activeSection === 'views'"
              class="node-submenu-children"
            >
              <button
                type="button"
                class="structure-node-create-view-btn"
                :disabled="createViewSaving || !nodeId"
                @click="createViewVisible = true"
              >
                + {{ t('sidebar.createView') }}
              </button>
              <div
                v-if="viewsMenu.loadingMenu"
                class="node-submenu-loading"
              >
                …
              </div>
              <template v-else-if="hasViewsMenuTree">
                <div class="structure-node-menu-embed">
                  <MenuNode
                    v-for="node in viewsMenu.menuTree!.roots"
                    :key="node.id"
                    :node="node"
                    :level="0"
                    :is-expanded="isGroupExpanded"
                    :is-selected="isStructureViewSelected"
                    :view-to="workspaceLinkForView"
                    @toggle-group="toggleGroup"
                    @select-view="() => {}"
                  />
                  <MenuNode
                    v-for="node in viewsMenu.menuTree!.ungroupedViews || []"
                    :key="`ug-${node.id}`"
                    :node="node"
                    :level="0"
                    :is-expanded="isGroupExpanded"
                    :is-selected="isStructureViewSelected"
                    :view-to="workspaceLinkForView"
                    @toggle-group="toggleGroup"
                    @select-view="() => {}"
                  />
                </div>
              </template>
              <div
                v-else
                class="node-submenu-empty"
              >
                {{ t('sidebar.structureViewsEmpty') }}
              </div>
            </div>
          </div>
        </nav>
      </aside>
      <div class="node-main">
        <router-view />
      </div>
    </div>

    <ViewEditForm
      v-model:visible="createViewVisible"
      mode="create"
      :structure-node-context-id="nodeId || undefined"
      :save-loading="createViewSaving"
      @save="handleCreateViewSave"
    />
  </div>
</template>

<style scoped>
.structure-node-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-main-panel);
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 20px 24px 12px;
  flex-shrink: 0;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-1);
}

.level-tag {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-3);
}

.node-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: row;
  border-top: 1px solid var(--color-border-2, var(--color-border-1));
}

.node-submenu {
  flex-shrink: 0;
  width: 248px;
  border-right: 1px solid var(--color-border-2, var(--color-border-1));
  background: var(--color-bg-2);
  overflow-y: auto;
}

.node-submenu-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 8px;
}

.node-submenu-link {
  display: block;
  padding: 8px 12px;
  border-radius: var(--radius-md, 6px);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-2);
  text-decoration: none;
  transition:
    background 0.12s ease,
    color 0.12s ease;
}

.node-submenu-link:hover {
  color: var(--color-text-1);
  background: var(--color-fill-2);
}

.node-submenu-link--active {
  color: rgb(var(--primary-6));
  background: var(--color-primary-light-1);
  font-weight: 600;
}

.node-submenu-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.node-submenu-children {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 2px 0 6px 8px;
  margin-left: 4px;
  border-left: 1px solid var(--color-border-2);
}

.node-submenu-loading {
  padding: 6px 10px;
  font-size: 12px;
  color: var(--color-text-3);
}

.node-submenu-empty {
  padding: 6px 10px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-text-3);
}

.structure-node-create-view-btn {
  display: flex;
  align-items: center;
  width: 100%;
  height: 28px;
  margin-bottom: 4px;
  padding: 0 10px;
  border: none;
  border-radius: var(--radius-md, 6px);
  background: transparent;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-2);
  cursor: pointer;
  text-align: left;
}

.structure-node-create-view-btn:hover:not(:disabled) {
  background: var(--color-fill-2);
  color: rgb(var(--primary-6));
}

.structure-node-create-view-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.structure-node-menu-embed {
  padding: 2px 0 0;
}

.structure-node-menu-embed :deep(.group-header) {
  color: var(--color-text-2);
  font-size: 12px;
  padding: 5px 6px;
  border-radius: var(--radius-md, 6px);
}

.structure-node-menu-embed :deep(.group-header:hover) {
  color: var(--color-text-1);
  background: var(--color-fill-2);
}

.structure-node-menu-embed :deep(.menu-item) {
  margin: 1px 2px 1px 0;
  padding: 5px 8px;
  border-radius: var(--radius-md, 6px);
  font-size: 13px;
  text-decoration: none;
  color: inherit;
  box-sizing: border-box;
}

.structure-node-menu-embed :deep(.menu-item:hover) {
  background: var(--color-fill-2);
}

.structure-node-menu-embed :deep(.menu-item.active) {
  background: var(--color-primary-light-1);
  color: rgb(var(--primary-6));
  font-weight: 600;
}

.structure-node-menu-embed :deep(.menu-item.active .item-icon) {
  color: rgb(var(--primary-6));
}

.structure-node-menu-embed :deep(.item-icon) {
  color: var(--color-text-3);
}

.node-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
}

.node-main > :deep(*) {
  flex: 1;
  min-height: 0;
}
</style>
