<script setup lang="ts">
import { ref, watch } from 'vue'
import { IconDown } from '@arco-design/web-vue/es/icon'
import { useCascadeRelationNavStore } from '@/stores/cascadeRelationNav'
import SidebarCascadeRelationLine from './SidebarCascadeRelationLine.vue'

const cascadeRelationNav = useCascadeRelationNavStore()

/** 每条已钉选级联关系独立折叠，默认展开（与「工作空间」一致） */
const expandedById = ref<Record<string, boolean>>({})

watch(
  () => cascadeRelationNav.displayedCascadeRelations,
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

function isExpanded(cascadeRelationId: string) {
  return expandedById.value[cascadeRelationId] !== false
}

function toggle(cascadeRelationId: string) {
  expandedById.value = {
    ...expandedById.value,
    [cascadeRelationId]: !isExpanded(cascadeRelationId),
  }
}
</script>

<template>
  <div class="sidebar-cascade-relation-wrap">
    <template
      v-for="s in cascadeRelationNav.displayedCascadeRelations"
      :key="String(s.id)"
    >
      <div class="sidebar-cascade-relation-group">
        <button
          type="button"
          class="section-header"
          :title="s.description || undefined"
          @click="toggle(String(s.id))"
        >
          <span class="section-title">{{ s.name }}</span>
          <IconDown
            class="chevron"
            :class="{ 'chevron--collapsed': !isExpanded(String(s.id)) }"
          />
        </button>
        <div
          v-show="isExpanded(String(s.id))"
          class="section-body"
        >
          <SidebarCascadeRelationLine
            :cascade-relation="s"
            :expanded="isExpanded(String(s.id))"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.sidebar-cascade-relation-wrap {
  padding-bottom: 4px;
}

.sidebar-cascade-relation-group {
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

.section-body {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding-top: 2px;
  padding-left: 8px;
}
</style>
