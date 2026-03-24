<script setup lang="ts">
import { watch, computed } from 'vue'
import type { CascadeRelationDefinition } from '@/types/cascade-relation'
import { useCascadeRelationNavStore } from '@/stores/cascadeRelationNav'
import SidebarCascadeRelationNodeItem from './SidebarCascadeRelationNodeItem.vue'

const props = defineProps<{
  cascadeRelation: CascadeRelationDefinition
  /** 由上层分组控制；展开时再拉树 */
  expanded: boolean
}>()

const cascadeRelationNav = useCascadeRelationNavStore()
const cascadeRelationIdStr = computed(() => String(props.cascadeRelation.id))

watch(
  () => props.expanded,
  (open) => {
    if (open) {
      void cascadeRelationNav.ensureTree(cascadeRelationIdStr.value)
    }
  },
  { immediate: true },
)

const tree = computed(() => cascadeRelationNav.trees[cascadeRelationIdStr.value] || [])
const treeLoading = computed(() => cascadeRelationNav.loadingTree[cascadeRelationIdStr.value] === true)
</script>

<template>
  <div class="cascade-relation-tree">
    <div
      v-if="treeLoading && tree.length === 0"
      class="tree-placeholder"
    >
      …
    </div>
    <SidebarCascadeRelationNodeItem
      v-for="n in tree"
      :key="n.id"
      :cascade-relation-id="cascadeRelationIdStr"
      :node="n"
      :depth="0"
    />
  </div>
</template>

<style scoped>
.cascade-relation-tree {
  width: 100%;
}

.tree-placeholder {
  padding: 4px 8px 4px 8px;
  font-size: 12px;
  color: var(--sidebar-text-secondary);
}
</style>
