<script setup lang="ts">
import { computed } from 'vue'
import { IconDelete } from '@arco-design/web-vue/es/icon'
import type { StructureDefinition } from '@/types/structure'

const props = defineProps<{
  structure: StructureDefinition
}>()

const emit = defineEmits<{
  (e: 'click'): void
  (e: 'delete', structure: StructureDefinition): void
}>()

// 获取层级列表（从根到叶，需要反转显示，让根在下面）
const displayLevels = computed(() => {
  if (!props.structure.levels) return []
  return [...props.structure.levels].reverse()
})

// 处理点击
function handleClick(event: MouseEvent) {
  // 阻止删除按钮的点击冒泡
  if ((event.target as HTMLElement).closest('.delete-btn')) {
    return
  }
  emit('click')
}

// 处理删除
function handleDelete(event: MouseEvent) {
  event.stopPropagation()
  emit('delete', props.structure)
}
</script>

<template>
  <div class="structure-branch" @click="handleClick">
    <!-- 架构线名称 -->
    <div class="branch-header">
      <span class="branch-name">{{ structure.name }}</span>
      <a-button
        v-if="!structure.systemStructure"
        class="delete-btn"
        type="text"
        size="mini"
        @click="handleDelete"
      >
        <template #icon>
          <icon-delete style="color: var(--color-text-3)" />
        </template>
      </a-button>
    </div>

    <!-- 层级节点列表 -->
    <div class="levels-container">
      <div
        v-for="(level, index) in displayLevels"
        :key="level.index"
        class="level-item"
      >
        <!-- 层级节点 -->
        <div class="level-node">
          <div class="node-dot-wrapper">
            <div class="node-dot"></div>
          </div>
          <span class="node-name">{{ level.name }}</span>
        </div>

        <!-- 连接线（最后一个节点不显示） -->
        <div v-if="index < displayLevels.length - 1" class="level-connector"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.structure-branch {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  min-width: 120px;
  position: relative;
  z-index: 10;
}

.branch-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px 8px;
  background: transparent;
  margin-bottom: 24px;
  min-width: 100px;
  height: 36px;
}



.branch-name {
  font-size: 14px;
  color: var(--color-text-1);
  white-space: nowrap;
  font-weight: 500;
}

.delete-btn {
  position: absolute;
  right: -8px;
  top: -8px;
  opacity: 0;
  transition: opacity 0.2s;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.structure-branch:hover .delete-btn {
  opacity: 1;
}

.levels-container {
  display: flex;
  flex-direction: column;
  align-items: flex-start; /* Align left to keep line vertical */
  padding-left: 20px; /* Offset to center under header somewhat, or just align items */
  position: relative;
}

/* Adjust alignment to center the line relative to the header?
   Actually, the design shows the line is aligned to the left side of the text,
   but centered under the header usually looks best.
   Let's align items center.
*/
.levels-container {
  align-items: center;
  padding-left: 0;
}

.level-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start; /* Node text to the right of dot */
  position: relative;
}

.level-node {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 24px; /* Fixed height for node row */
}

.node-dot-wrapper {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.node-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #66ccff;
  position: relative;
}

/* No halo effect */
.node-dot::after {
  display: none;
}

.node-name {
  font-size: 13px;
  color: var(--color-text-2);
  white-space: nowrap;
}

.level-connector {
  width: 1px;
  height: 40px; /* Spacing between nodes */
  background: #C9CDD4;
  margin-left: 7.5px; /* Center with 16px wrapper: 8px center - 0.5px width = 7.5px */
}
</style>
