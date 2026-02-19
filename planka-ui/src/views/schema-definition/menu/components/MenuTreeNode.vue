<script setup lang="ts">
import { computed, ref } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import {
  IconFolder,
  IconApps,
  IconDragDotVertical,
  IconMore,
} from '@arco-design/web-vue/es/icon'
import type { MenuTreeNodeVO } from '@/types/menu'

// 节点移动事件 payload
export interface NodeMovedPayload {
  draggedNode: MenuTreeNodeVO
  from: { parentId?: string; oldIndex: number }
  to: { parentId?: string; newIndex: number }
}

const props = defineProps<{
  nodes: MenuTreeNodeVO[]
  parentId?: string // 父节点 ID，根节点为 undefined
  level: number // 嵌套层级，用于缩进
  allNodes: MenuTreeNodeVO[] // 整棵树的根节点，用于查找祖先
}>()

const emit = defineEmits<{
  (e: 'update:nodes', value: MenuTreeNodeVO[]): void
  (e: 'node-moved', payload: NodeMovedPayload): void
  (e: 'edit-group', groupId: string): void
  (e: 'delete-group', node: MenuTreeNodeVO): void
  (e: 'remove-view', groupId: string, viewId: string): void
  (e: 'create-sub-group', parentId: string): void
}>()

// 本地节点副本（支持双向绑定）
const localNodes = computed({
  get: () => props.nodes,
  set: (val) => emit('update:nodes', val),
})

// 每个空分组有自己的空列表（避免共享状态问题）
const emptyListMap = ref<Map<string, MenuTreeNodeVO[]>>(new Map())

// 获取指定分组的空列表
function getEmptyList(groupId: string): MenuTreeNodeVO[] {
  if (!emptyListMap.value.has(groupId)) {
    emptyListMap.value.set(groupId, [])
  }
  return emptyListMap.value.get(groupId)!
}

// 拖拽配置
const dragOptions = computed(() => ({
  group: 'menu', // 统一分组名，允许跨容器拖拽
  animation: 200,
  ghostClass: 'menu-drag-ghost', // 拖拽时的占位样式
  chosenClass: 'menu-drag-chosen', // 选中时的样式
  dragClass: 'menu-drag-active', // 拖拽中的样式
  fallbackOnBody: true,
  swapThreshold: 0.65,
}))

// 检查是否是某节点的子孙（防止将分组拖到自己的子孙下）
function isDescendant(nodeId: string, targetParentId: string): boolean {
  // 从 allNodes 递归查找
  function findNode(nodes: MenuTreeNodeVO[], id: string): MenuTreeNodeVO | undefined {
    for (const node of nodes) {
      if (node.id === id) return node
      if (node.children?.length) {
        const found = findNode(node.children, id)
        if (found) return found
      }
    }
    return undefined
  }

  function checkDescendant(node: MenuTreeNodeVO, targetId: string): boolean {
    if (node.id === targetId) return true
    if (node.children?.length) {
      return node.children.some((child) => checkDescendant(child, targetId))
    }
    return false
  }

  const draggedNode = findNode(props.allNodes, nodeId)
  if (!draggedNode) return false
  return checkDescendant(draggedNode, targetParentId)
}

// 检查拖拽是否允许
function checkMove(evt: any): boolean {
  const draggedNode = evt.draggedContext?.element as MenuTreeNodeVO
  if (!draggedNode) return true

  const toContainer = evt.to as HTMLElement
  const toParentId = toContainer?.dataset?.parentId || undefined
  const toLevel = parseInt(toContainer?.dataset?.level || '0')

  // 视图不能放到根级别
  if (draggedNode.type === 'VIEW' && toLevel === 0 && !toParentId) {
    return false
  }

  // 防止将分组拖到自己的子孙节点下
  if (draggedNode.type === 'GROUP' && toParentId) {
    if (isDescendant(draggedNode.id, toParentId)) {
      return false
    }
  }

  return true
}

// 从事件中获取被拖拽的节点数据
function getDraggedNode(evt: any): MenuTreeNodeVO | undefined {
  // vue-draggable-plus 使用 data 或 clonedData 属性
  return (evt.data || evt.clonedData) as MenuTreeNodeVO | undefined
}

// 拖拽结束处理（仅处理同一容器内的重排序）
function handleDragEnd(evt: any) {
  console.log('handleDragEnd', evt)

  const fromContainer = evt.from as HTMLElement
  const toContainer = evt.to as HTMLElement

  const fromParentId = fromContainer?.dataset?.parentId || undefined
  const toParentId = toContainer?.dataset?.parentId || undefined

  // 跨容器拖拽由目标容器的 @add 处理，这里只处理同容器内的重排序
  if (fromParentId !== toParentId) {
    console.log('跳过：跨容器拖拽，由 @add 处理')
    return
  }

  // 确保是从当前容器触发的事件
  if (fromParentId !== props.parentId) {
    console.log('跳过：不是当前容器的事件')
    return
  }

  const draggedNode = getDraggedNode(evt)
  console.log('draggedNode', draggedNode, 'fromParentId', fromParentId, 'toParentId', toParentId)

  if (!draggedNode) {
    console.warn('无法获取被拖拽的节点')
    return
  }

  emit('node-moved', {
    draggedNode,
    from: {
      parentId: fromParentId,
      oldIndex: evt.oldIndex,
    },
    to: {
      parentId: toParentId,
      newIndex: evt.newIndex,
    },
  })
}

// 处理从其他容器拖入的项目（跨容器拖拽）
function handleAdd(evt: any) {
  console.log('handleAdd', evt)

  const fromContainer = evt.from as HTMLElement
  const toContainer = evt.to as HTMLElement

  // 从源容器和目标容器获取 parentId
  const fromParentId = fromContainer?.dataset?.parentId || undefined
  const toParentId = toContainer?.dataset?.parentId || undefined

  const draggedNode = getDraggedNode(evt)
  console.log('handleAdd draggedNode', draggedNode, 'fromParentId', fromParentId, 'toParentId', toParentId, 'newIndex', evt.newIndex)

  if (!draggedNode) {
    console.warn('handleAdd: 无法获取被拖拽的节点')
    return
  }

  emit('node-moved', {
    draggedNode,
    from: {
      parentId: fromParentId, // 可能是其他分组，也可能是左侧未分组列表(undefined)
      oldIndex: evt.oldIndex,
    },
    to: {
      parentId: toParentId,
      newIndex: evt.newIndex,
    },
  })
}

// 处理空分组接收拖入的项目
function handleEmptyGroupAdd(groupId: string, evt: any) {
  console.log('handleEmptyGroupAdd', groupId, evt)

  const fromContainer = evt.from as HTMLElement
  const fromParentId = fromContainer?.dataset?.parentId || undefined

  const draggedNode = getDraggedNode(evt)
  console.log('handleEmptyGroupAdd draggedNode', draggedNode, 'fromParentId', fromParentId, 'toGroupId', groupId)

  if (!draggedNode) {
    console.warn('handleEmptyGroupAdd: 无法获取被拖拽的节点')
    return
  }

  emit('node-moved', {
    draggedNode,
    from: {
      parentId: fromParentId, // 可能是其他分组，也可能是左侧未分组列表(undefined)
      oldIndex: evt.oldIndex,
    },
    to: {
      parentId: groupId,
      newIndex: 0,
    },
  })
}

// 更新子节点
function handleChildNodesUpdate(nodeId: string, newChildren: MenuTreeNodeVO[]) {
  const updatedNodes = localNodes.value.map((node) => {
    if (node.id === nodeId) {
      return { ...node, children: newChildren }
    }
    return node
  })
  emit('update:nodes', updatedNodes)
}

// 处理子节点移动事件（向上冒泡）
function handleChildNodeMoved(payload: NodeMovedPayload) {
  emit('node-moved', payload)
}
</script>

<template>
  <VueDraggable
    v-model="localNodes"
    v-bind="dragOptions"
    :move="checkMove"
    :data-level="level"
    :data-parent-id="parentId"
    class="menu-tree-container"
    :class="{ 'is-root': level === 0 }"
    @end="handleDragEnd"
    @add="handleAdd"
  >
    <div v-for="node in localNodes" :key="node.id" class="menu-tree-node" :class="{ 'is-group': node.type === 'GROUP' }">
      <!-- 节点内容行 -->
      <div class="node-content" :style="{ paddingLeft: `${level * 24 + 8}px` }">
        <IconDragDotVertical class="drag-handle" />
        <IconFolder v-if="node.type === 'GROUP'" class="node-icon folder-icon" />
        <IconApps v-else class="node-icon view-icon" />
        <span class="node-title">{{ node.name }}</span>
        <span v-if="node.type === 'VIEW'" class="node-tag">视图</span>
        <span v-if="node.cardTypeName" class="node-card-type">{{ node.cardTypeName }}</span>

        <!-- 操作按钮 -->
        <div class="node-actions" @click.stop>
          <a-dropdown trigger="click">
            <a-button type="text" size="mini">
              <template #icon><IconMore /></template>
            </a-button>
            <template #content>
              <template v-if="node.type === 'GROUP'">
                <a-doption @click="emit('create-sub-group', node.id)">添加子分组</a-doption>
                <a-doption @click="emit('edit-group', node.id)">编辑</a-doption>
                <a-doption class="danger-option" @click="emit('delete-group', node)">删除</a-doption>
              </template>
              <template v-else>
                <a-doption
                  v-if="parentId"
                  class="danger-option"
                  @click="emit('remove-view', parentId, node.id)"
                >从分组移除</a-doption>
              </template>
            </template>
          </a-dropdown>
        </div>
      </div>

      <!-- 子节点区域（仅分组有） -->
      <template v-if="node.type === 'GROUP'">
        <!-- 有子节点时递归渲染 -->
        <MenuTreeNode
          v-if="node.children?.length"
          :nodes="node.children"
          :parent-id="node.id"
          :level="level + 1"
          :all-nodes="allNodes"
          @update:nodes="handleChildNodesUpdate(node.id, $event)"
          @node-moved="handleChildNodeMoved"
          @edit-group="emit('edit-group', $event)"
          @delete-group="emit('delete-group', $event)"
          @remove-view="(gid, vid) => emit('remove-view', gid, vid)"
          @create-sub-group="emit('create-sub-group', $event)"
        />
        <!-- 空分组的占位区域 -->
        <VueDraggable
          v-else
          :model-value="getEmptyList(node.id)"
          v-bind="dragOptions"
          :data-level="level + 1"
          :data-parent-id="node.id"
          class="empty-drop-zone"
          :style="{ marginLeft: `${(level + 1) * 24 + 8}px` }"
          @add="handleEmptyGroupAdd(node.id, $event)"
        >
          <div class="empty-hint">拖拽视图到此处</div>
        </VueDraggable>
      </template>
    </div>
  </VueDraggable>
</template>

<style scoped lang="scss">
.menu-tree-container {
  min-height: 20px;

  &.is-root {
    min-height: 100px;
  }
}

.menu-tree-node {
  margin-bottom: 2px;
}

.node-content {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px 8px 8px;
  border-radius: 6px;
  transition: all 0.2s;
  cursor: default;

  &:hover {
    background: var(--color-fill-2);

    .drag-handle {
      opacity: 1;
    }

    .node-actions {
      opacity: 1;
    }
  }
}

.drag-handle {
  cursor: grab;
  color: var(--color-text-4);
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;

  &:active {
    cursor: grabbing;
  }
}

.node-icon {
  flex-shrink: 0;
  font-size: 16px;

  &.folder-icon {
    color: rgb(var(--arcoblue-6));
  }

  &.view-icon {
    color: var(--color-text-3);
  }
}

.node-title {
  flex: 1;
  font-size: 14px;
  color: var(--color-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  .is-group > .node-content > & {
    font-weight: 500;
  }
}

.node-tag {
  font-size: 11px;
  padding: 1px 6px;
  background: var(--color-fill-2);
  border-radius: 2px;
  color: var(--color-text-3);
  flex-shrink: 0;
}

.node-card-type {
  font-size: 11px;
  color: var(--color-text-3);
  flex-shrink: 0;
}

.node-actions {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}

// 空分组的拖放区域 - 默认隐藏，拖拽时显示
.empty-drop-zone {
  min-height: 8px;
  margin-right: 8px;
  margin-bottom: 2px;
  border-radius: 4px;
  transition: all 0.2s;

  .empty-hint {
    display: none;
  }

  // 拖拽悬停时显示
  &:has(.menu-drag-ghost),
  &.sortable-ghost {
    min-height: 36px;
    background: rgb(var(--primary-1));
    border: 1px dashed rgb(var(--primary-6));

    .empty-hint {
      display: block;
    }
  }
}

.empty-hint {
  font-size: 12px;
  color: var(--color-text-3);
}

:deep(.danger-option) {
  color: rgb(var(--danger-6)) !important;
}
</style>
