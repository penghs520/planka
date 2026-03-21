<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import SidebarStructureNodeItem from './SidebarStructureNodeItem.vue'
import { RouterLink, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { IconDown } from '@arco-design/web-vue/es/icon'
import type { StructureNodeDTO } from '@/api/structure-options'
import { getCardTitle } from '@/types/card'
import { useStructureNavStore } from '@/stores/structureNav'

const props = withDefaults(
  defineProps<{
    structureId: string
    node: StructureNodeDTO
    depth?: number
  }>(),
  { depth: 0 },
)

const { t } = useI18n()
const route = useRoute()
const structureNav = useStructureNavStore()
const expanded = ref(false)

const routeNodeId = computed(() => (route.params.nodeId as string) || '')
const routeStructureId = computed(() => (route.params.structureId as string) || '')

watch(expanded, (open) => {
  if (open) {
    void structureNav.ensureNodeProjects(props.structureId, props.node.id)
  }
})

watch(
  [routeNodeId, routeStructureId],
  () => {
    if (routeStructureId.value !== props.structureId || !routeNodeId.value) {
      return
    }
    if (structureNav.subtreeContains(props.structureId, props.node.id, routeNodeId.value)) {
      expanded.value = true
    }
  },
  { immediate: true },
)

function toggle() {
  expanded.value = !expanded.value
}

function projectActive(projectId: string) {
  return route.path.startsWith(`/project/${projectId}`)
}

function nodeSubActive(segment: 'issues' | 'projects' | 'views') {
  return (
    routeStructureId.value === props.structureId
    && routeNodeId.value === props.node.id
    && route.path.endsWith(`/${segment}`)
  )
}

const childPad = computed(() => `${8 + Math.min(props.depth, 6) * 8}px`)
</script>

<template>
  <div class="structure-node">
    <button
      type="button"
      class="node-header"
      :style="{ paddingLeft: childPad }"
      @click="toggle"
    >
      <span class="node-name">{{ node.name }}</span>
      <span
        v-if="node.levelName"
        class="level-tag"
        :title="node.levelName"
      >{{ node.levelName }}</span>
      <IconDown
        class="chevron"
        :class="{ 'chevron--collapsed': !expanded }"
      />
    </button>
    <div
      v-show="expanded"
      class="node-children"
    >
      <RouterLink
        :to="`/structure/${structureId}/node/${node.id}/issues`"
        class="nav-row"
        :class="{ active: nodeSubActive('issues') }"
      >
        {{ t('sidebar.teamIssues') }}
      </RouterLink>
      <RouterLink
        :to="`/structure/${structureId}/node/${node.id}/views`"
        class="nav-row"
        :class="{ active: nodeSubActive('views') }"
      >
        {{ t('sidebar.teamViews') }}
      </RouterLink>
      <RouterLink
        :to="`/structure/${structureId}/node/${node.id}/projects`"
        class="nav-row"
        :class="{ active: nodeSubActive('projects') }"
      >
        {{ t('sidebar.teamProjects') }}
      </RouterLink>
      <RouterLink
        v-for="p in (structureNav.nodeProjects[structureNav.nodeCacheKey(structureId, node.id)] || [])"
        :key="p.id"
        :to="`/project/${p.id}/issues`"
        class="nav-row nav-row--project"
        :class="{ active: projectActive(p.id) }"
      >
        {{ getCardTitle(p) }}
      </RouterLink>
      <SidebarStructureNodeItem
        v-for="c in node.children || []"
        :key="c.id"
        :structure-id="structureId"
        :node="c"
        :depth="depth + 1"
      />
    </div>
  </div>
</template>

<style scoped>
.structure-node {
  margin-bottom: 1px;
}

.node-header {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
  min-height: 28px;
  padding: 0 8px 0 0;
  margin: 0;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: var(--sidebar-text-primary);
  font-family: var(--sidebar-nav-font-family);
  font-size: var(--sidebar-nav-font-size);
  font-weight: 600;
  cursor: pointer;
  text-align: left;
}

.node-header:hover {
  background: var(--sidebar-bg-hover);
}

.chevron {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
  transition: transform 0.15s ease;
  color: var(--sidebar-text-secondary);
}

.chevron--collapsed {
  transform: rotate(-90deg);
}

.node-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.level-tag {
  flex-shrink: 0;
  max-width: 56px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
  font-weight: 500;
  color: var(--sidebar-text-secondary);
  opacity: 0.85;
}

.node-children {
  padding-left: 2px;
}

.nav-row {
  display: block;
  height: 26px;
  line-height: 26px;
  padding: 0 8px 0 12px;
  border-radius: 5px;
  font-family: var(--sidebar-nav-font-family);
  font-size: var(--sidebar-nav-font-size);
  font-weight: var(--sidebar-nav-font-weight);
  color: var(--sidebar-text-secondary);
  text-decoration: none;
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

.nav-row--project {
  padding-left: 20px;
}
</style>
