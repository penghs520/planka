<script setup lang="ts">
import { ref, watch } from 'vue'
import { IconDown } from '@arco-design/web-vue/es/icon'
import { useStructureNavStore } from '@/stores/structureNav'
import SidebarStructureLine from './SidebarStructureLine.vue'

const structureNav = useStructureNavStore()

/** 每条已钉选架构线独立折叠，默认展开（与「工作空间」一致） */
const expandedById = ref<Record<string, boolean>>({})

watch(
  () => structureNav.displayedStructures,
  (list) => {
    const next = { ...expandedById.value }
    for (const s of list) {
      const id = String(s.id)
      if (!(id in next)) {
        next[id] = true
      }
    }
    expandedById.value = next
  },
  { immediate: true },
)

function isExpanded(structureId: string) {
  return expandedById.value[structureId] !== false
}

function toggle(structureId: string) {
  expandedById.value = {
    ...expandedById.value,
    [structureId]: !isExpanded(structureId),
  }
}
</script>

<template>
  <div class="sidebar-structure-wrap">
    <template
      v-for="s in structureNav.displayedStructures"
      :key="String(s.id)"
    >
      <div class="sidebar-structure-group">
        <button
          type="button"
          class="section-header"
          :title="s.description || undefined"
          @click="toggle(String(s.id))"
        >
          <IconDown
            class="chevron"
            :class="{ 'chevron--collapsed': !isExpanded(String(s.id)) }"
          />
          <span class="section-title">{{ s.name }}</span>
        </button>
        <div
          v-show="isExpanded(String(s.id))"
          class="section-body"
        >
          <SidebarStructureLine
            :structure="s"
            :expanded="isExpanded(String(s.id))"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.sidebar-structure-wrap {
  padding-bottom: 4px;
}

.sidebar-structure-group {
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-body {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding-top: 2px;
  padding-left: 14px;
}
</style>
