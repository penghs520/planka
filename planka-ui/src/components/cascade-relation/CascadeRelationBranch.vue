<script setup lang="ts">
import { computed } from 'vue'
import { IconDelete, IconRight } from '@arco-design/web-vue/es/icon'
import type { CascadeRelationDefinition } from '@/types/cascade-relation'

const props = defineProps<{
  cascadeRelation: CascadeRelationDefinition
}>()

const emit = defineEmits<{
  (e: 'click'): void
  (e: 'delete', payload: CascadeRelationDefinition): void
}>()

/** 从根到叶，与定义顺序一致（左 → 右） */
const displayLevels = computed(() => {
  if (!props.cascadeRelation.levels) return []
  return [...props.cascadeRelation.levels]
})

function handleClick(event: MouseEvent) {
  if ((event.target as HTMLElement).closest('.delete-btn')) {
    return
  }
  emit('click')
}

function handleDelete(event: MouseEvent) {
  event.stopPropagation()
  emit('delete', props.cascadeRelation)
}
</script>

<template>
  <div class="cascade-relation-branch" @click="handleClick">
    <div class="branch-title">
      <span class="branch-name">{{ cascadeRelation.name }}</span>
      <a-button
        v-if="!cascadeRelation.systemCascadeRelation"
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

    <div class="flow-levels">
      <template v-for="(level, index) in displayLevels" :key="level.index">
        <div class="level-pill">
          <span class="level-pill-text">{{ level.name }}</span>
        </div>
        <icon-right v-if="index < displayLevels.length - 1" class="flow-sep" />
      </template>
    </div>
  </div>
</template>

<style scoped>
.cascade-relation-branch {
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px 16px;
  cursor: pointer;
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--color-border-1);
  border-radius: var(--radius-lg);
  background: var(--color-bg-2);
  box-sizing: border-box;
  transition:
    box-shadow 0.2s,
    border-color 0.2s;
}

.cascade-relation-branch:hover {
  border-color: var(--color-primary-light);
  box-shadow: 0 2px 12px rgba(var(--color-primary-rgb), 0.12);
}

.branch-title {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  min-width: 0;
  max-width: 100%;
}

.branch-name {
  font-size: 14px;
  color: var(--color-text-1);
  white-space: nowrap;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}

.cascade-relation-branch:hover .delete-btn {
  opacity: 1;
}

.flow-levels {
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.level-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: var(--radius-md);
  background: var(--color-fill-2);
  border: 1px solid var(--color-border-2);
  max-width: 100%;
}

.level-pill-text {
  font-size: 13px;
  color: var(--color-text-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flow-sep {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--color-text-4);
}
</style>
