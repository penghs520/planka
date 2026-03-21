<script setup lang="ts">
import { watch, computed } from 'vue'
import type { StructureDefinition } from '@/types/structure'
import { useStructureNavStore } from '@/stores/structureNav'
import SidebarStructureNodeItem from './SidebarStructureNodeItem.vue'

const props = defineProps<{
  structure: StructureDefinition
  /** 由上层分组控制；展开时再拉树 */
  expanded: boolean
}>()

const structureNav = useStructureNavStore()
const structureIdStr = computed(() => String(props.structure.id))

watch(
  () => props.expanded,
  (open) => {
    if (open) {
      void structureNav.ensureTree(structureIdStr.value)
    }
  },
  { immediate: true },
)

const tree = computed(() => structureNav.trees[structureIdStr.value] || [])
const treeLoading = computed(() => structureNav.loadingTree[structureIdStr.value] === true)
</script>

<template>
  <div class="structure-tree">
    <div
      v-if="treeLoading && tree.length === 0"
      class="tree-placeholder"
    >
      …
    </div>
    <SidebarStructureNodeItem
      v-for="n in tree"
      :key="n.id"
      :structure-id="structureIdStr"
      :node="n"
      :depth="0"
    />
  </div>
</template>

<style scoped>
.structure-tree {
  width: 100%;
}

.tree-placeholder {
  padding: 4px 8px 4px 8px;
  font-size: 12px;
  color: var(--sidebar-text-secondary);
}
</style>
