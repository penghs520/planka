<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import SidebarStructureNodeItem from './SidebarStructureNodeItem.vue'
import { RouterLink, useRoute } from 'vue-router'
import { IconDown } from '@arco-design/web-vue/es/icon'
import type { StructureNodeDTO } from '@/api/structure-options'
import { useStructureNavStore } from '@/stores/structureNav'

const props = withDefaults(
  defineProps<{
    structureId: string
    node: StructureNodeDTO
    depth?: number
  }>(),
  { depth: 0 },
)

const route = useRoute()
const structureNav = useStructureNavStore()
const expanded = ref(false)

const routeNodeId = computed(() => (route.params.nodeId as string) || '')
const routeStructureId = computed(() => (route.params.structureId as string) || '')

const hasChildren = computed(() => !!(props.node.children && props.node.children.length > 0))

const isNodeActive = computed(() => {
  return routeStructureId.value === props.structureId && routeNodeId.value === props.node.id
})

const isInSubtree = computed(() => {
  if (!routeNodeId.value || routeStructureId.value !== props.structureId) return false
  return structureNav.subtreeContains(props.structureId, props.node.id, routeNodeId.value)
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

function toggleExpand(e: Event) {
  e.stopPropagation()
  e.preventDefault()
  expanded.value = !expanded.value
}

const nodePad = computed(() => `${8 + Math.min(props.depth, 6) * 8}px`)
</script>

<template>
  <div class="structure-node">
    <div
      class="node-row"
      :class="{ 'node-row--active': isNodeActive, 'node-row--in-subtree': isInSubtree && !isNodeActive }"
      :style="{ paddingLeft: nodePad }"
    >
      <button
        v-if="hasChildren"
        type="button"
        class="expand-btn"
        @click="toggleExpand"
      >
        <IconDown
          class="chevron"
          :class="{ 'chevron--collapsed': !expanded }"
        />
      </button>
      <span v-else class="expand-placeholder" />
      <RouterLink
        :to="`/structure/${structureId}/node/${node.id}`"
        class="node-link"
      >
        <span class="node-name">{{ node.name }}</span>
        <span
          v-if="node.levelName"
          class="level-tag"
          :title="node.levelName"
        >{{ node.levelName }}</span>
      </RouterLink>
    </div>
    <div
      v-if="hasChildren"
      v-show="expanded"
      class="node-children"
    >
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

.node-row {
  display: flex;
  align-items: center;
  gap: 2px;
  min-height: 28px;
  padding-right: 8px;
  border-radius: 5px;
  transition: background 0.1s ease;
}

.node-row:hover {
  background: var(--sidebar-bg-hover);
}

.node-row--active {
  background: var(--sidebar-bg-active);
}

.node-row--active .node-link {
  color: var(--sidebar-text-active);
  font-weight: var(--sidebar-nav-font-weight-active);
}

.expand-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: 3px;
  background: transparent;
  color: var(--sidebar-text-secondary);
  cursor: pointer;
}

.expand-btn:hover {
  background: var(--sidebar-bg-hover);
  color: var(--sidebar-text-primary);
}

.expand-placeholder {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
}

.chevron {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
  transition: transform 0.15s ease;
}

.chevron--collapsed {
  transform: rotate(-90deg);
}

.node-link {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
  text-decoration: none;
  color: var(--sidebar-text-primary);
  font-family: var(--sidebar-nav-font-family);
  font-size: var(--sidebar-nav-font-size);
  font-weight: 600;
}

.node-link:hover {
  color: var(--sidebar-text-primary);
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
</style>
