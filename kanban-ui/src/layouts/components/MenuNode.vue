<script setup lang="ts">
import { IconApps, IconDown, IconRight } from '@arco-design/web-vue/es/icon'
import type { MenuTreeNodeVO } from '@/types/menu'

defineProps<{
  node: MenuTreeNodeVO
  level: number
  isExpanded: (groupId: string) => boolean
  isSelected: (viewId: string) => boolean
}>()

const emit = defineEmits<{
  (e: 'toggle-group', groupId: string): void
  (e: 'select-view', viewId: string): void
}>()
</script>

<template>
  <!-- 分组节点 -->
  <div v-if="node.type === 'GROUP'" class="menu-group">
    <div
      class="group-header"
      :style="{ paddingLeft: `${level * 12 + 8}px` }"
      @click="emit('toggle-group', node.id)"
    >
      <component
        :is="isExpanded(node.id) ? IconDown : IconRight"
        class="expand-icon"
      />
      <span class="group-name">{{ node.name }}</span>
    </div>
    <div v-show="isExpanded(node.id)" class="group-children">
      <MenuNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :level="level + 1"
        :is-expanded="isExpanded"
        :is-selected="isSelected"
        @toggle-group="emit('toggle-group', $event)"
        @select-view="emit('select-view', $event)"
      />
    </div>
  </div>

  <!-- 视图节点 -->
  <div
    v-else
    :class="['menu-item', { active: isSelected(node.id) }]"
    :style="{ paddingLeft: `${level * 12 + 12}px` }"
    @click="emit('select-view', node.id)"
  >
    <IconApps class="item-icon" />
    <span class="item-name">{{ node.name }}</span>
  </div>
</template>

<style scoped lang="scss">
.menu-group {
  margin-bottom: 2px;
}

.group-header {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  cursor: pointer;
  color: var(--color-text-2);
  font-size: 12px;
  gap: 4px;

  &:hover {
    color: var(--color-text-1);
  }
}

.expand-icon {
  font-size: 12px;
  flex-shrink: 0;
}

.group-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 4px 12px;
  cursor: pointer;
  transition: all 0.2s;
  gap: 8px;
  border-radius: 4px;
  margin: 1px 4px;

  &:hover {
    background: var(--color-fill-2);
  }

  &.active {
    background: rgb(var(--primary-5));
    color: #fff;
    font-weight: 500;

    .item-icon {
      color: #fff;
    }
  }
}

.item-icon {
  font-size: 14px;
  color: var(--color-text-3);
  flex-shrink: 0;
}

.item-name {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
