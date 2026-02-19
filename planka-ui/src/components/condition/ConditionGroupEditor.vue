<template>
  <div
    class="condition-group"
    :class="{ 'is-nested': depth > 0 }"
  >
    <!-- 条件组头部 -->
    <div class="group-header">
      <!-- 层级标识 -->
      <span v-if="depth > 0" class="depth-badge">L{{ depth }}</span>

      <!-- 逻辑操作符选择（点击切换） -->
      <span
        class="operator-badge"
        :class="[localGroup.operator === 'AND' ? 'is-and' : 'is-or', { 'is-flipping': isFlipping }]"
        :title="localGroup.operator === 'AND' ? t('common.condition.clickToSwitchOr') : t('common.condition.clickToSwitchAnd')"
        @click="toggleOperatorWithAnimation"
      >
        {{ localGroup.operator === 'AND' ? t('common.condition.and') : t('common.condition.or') }}
      </span>

      <!-- 操作按钮 -->
      <a-space :size="4">
        <a-button type="text" size="mini" class="action-btn" @click="addConditionItem">
          <template #icon><icon-plus /></template>
          {{ t('common.condition.addCondition') }}
        </a-button>

        <a-button
          type="text"
          size="mini"
          class="action-btn"
          :disabled="depth >= 4"
          :title="depth >= 4 ? t('common.condition.maxNestingDepth', { n: 4 }) : ''"
          @click="addConditionGroup"
        >
          <template #icon><icon-plus-circle /></template>
          {{ t('common.condition.addConditionGroup') }}
        </a-button>

        <!-- 删除按钮（仅非根条件组显示） -->
        <a-button
          v-if="depth > 0"
          type="text"
          size="mini"
          class="action-btn delete-btn"
          @click="handleRemove"
        >
          <template #icon><icon-delete /></template>
        </a-button>

        <!-- 清除全部按钮（仅根条件组显示） -->
        <a-button
          v-if="depth === 0"
          type="text"
          size="mini"
          class="action-btn clear-all-btn"
          @click="handleClearAll"
        >
          <template #icon><icon-delete /></template>
          {{ t('common.condition.clearAll') }}
        </a-button>
      </a-space>
    </div>

    <!-- 子节点列表 -->
    <div v-if="localGroup.children.length > 0" class="group-children">
      <div
        v-for="(child, index) in localGroup.children"
        :key="getChildKey(child, index)"
        class="draggable-item"
        :class="{
          'drag-over': dragOverIndex === index && draggedIndex !== index,
          'dragging': draggedIndex === index
        }"
        draggable="true"
        @dragstart="handleDragStart(index, $event)"
        @dragend="handleDragEnd"
        @dragenter="handleDragEnter(index, $event)"
        @dragover="handleDragOver"
        @drop="handleDrop(index, $event)"
      >
        <!-- 递归：子条件组 -->
        <ConditionGroupEditor
          v-if="isConditionGroup(child)"
          :model-value="child"
          :depth="depth + 1"
          :available-fields="availableFields"
          :link-types="linkTypes"
          :any-trait-card-type-name="rootCardTypeName"
          :fetch-fields-by-link-field-id="fetchFieldsByLinkFieldId"
          @update:model-value="(newChild: ConditionGroup) => updateChild(index, newChild)"
          @remove="removeChild(index)"
        />

        <!-- 叶子节点：条件项 -->
        <ConditionItemEditor
          v-else
          :model-value="child"
          :available-fields="availableFields"
          :link-types="linkTypes"
          :any-trait-card-type-name="rootCardTypeName"
          :fetch-fields-by-link-field-id="fetchFieldsByLinkFieldId"
          @update:model-value="(newChild: ConditionNode) => updateChild(index, newChild)"
          @remove="removeChild(index)"
        />
      </div>
    </div>

    <!-- 空状态提示 -->
    <a-empty
      v-else
      class="empty-children"
      :class="{ 'is-incomplete': showEmptyError }"
      :description="t('common.condition.noConditions')"
      :image-style="{ width: '60px', height: '60px' }"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, inject, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  IconPlus,
  IconPlusCircle,
  IconDelete,
} from '@arco-design/web-vue/es/icon'
import type { ConditionGroup, ConditionNode } from '@/types/condition'
import { isConditionGroup, NodeType } from '@/types/condition'
import type { FieldOption } from '@/types/field-option'
import type { LinkTypeVO } from '@/types/link-type'
import { createDefaultConditionItem } from '@/utils/condition-factory'

// 递归引用自身
import ConditionGroupEditor from './ConditionGroupEditor.vue'
import ConditionItemEditor from './ConditionItemEditor.vue'

const { t } = useI18n()

/**
 * Props定义
 */
const props = withDefaults(
  defineProps<{
    /** 条件组值 */
    modelValue: ConditionGroup

    /** 嵌套深度（根节点为0） */
    depth: number

    /** 可用字段列表 */
    availableFields: FieldOption[]

    /** 关联类型列表 */
    linkTypes?: LinkTypeVO[]

    /** 根卡片类型名称 */
    rootCardTypeName?: string

    /** 根据关联字段ID获取级联字段的函数 */
    fetchFieldsByLinkFieldId?: (linkFieldId: string) => Promise<FieldOption[]>
  }>(),
  {
    depth: 0,
    availableFields: () => [],
    linkTypes: () => [],
    rootCardTypeName: '',
    fetchFieldsByLinkFieldId: undefined,
  }
)

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: ConditionGroup]
  'remove': []
  'clearAll': []
}>()

/**
 * 本地条件组状态
 */
const localGroup = ref<ConditionGroup>({ ...props.modelValue })

/**
 * 翻转动画状态
 */
const isFlipping = ref(false)

/**
 * 拖拽状态
 */
const draggedIndex = ref(-1)
const dragOverIndex = ref(-1)

/**
 * 从父组件注入高亮状态
 */
const highlightIncomplete = inject<Ref<boolean>>('highlightIncomplete', ref(false))

/**
 * 是否显示空条件组的错误状态
 */
const showEmptyError = computed(() => highlightIncomplete.value && localGroup.value.children.length === 0)

/**
 * 监听props变化
 */
watch(
  () => props.modelValue,
  (newVal) => {
    localGroup.value = JSON.parse(JSON.stringify(newVal))
  },
  { deep: true }
)

/**
 * 添加条件项
 */
function addConditionItem() {
  // 使用工厂函数创建默认的文本条件项
  const newItem = createDefaultConditionItem(NodeType.TEXT)

  const newChildren = [...localGroup.value.children, newItem]
  localGroup.value = {
    ...localGroup.value,
    children: newChildren,
  }
  emitChange()
}

/**
 * 添加条件组
 */
function addConditionGroup() {
  const newGroup: ConditionGroup = {
    nodeType: 'GROUP',
    operator: 'AND',
    children: [],
  }

  const newChildren = [...localGroup.value.children, newGroup]
  localGroup.value = {
    ...localGroup.value,
    children: newChildren,
  }
  emitChange()
}

/**
 * 更新子节点
 */
function updateChild(index: number, newChild: ConditionNode) {
  const newChildren = [...localGroup.value.children]
  newChildren[index] = newChild
  localGroup.value = {
    ...localGroup.value,
    children: newChildren,
  }
  emitChange()
}

/**
 * 删除子节点
 */
function removeChild(index: number) {
  const newChildren = localGroup.value.children.filter((_, i) => i !== index)
  localGroup.value = {
    ...localGroup.value,
    children: newChildren,
  }
  emitChange()
}

/**
 * 处理删除当前条件组
 */
function handleRemove() {
  emit('remove')
}

/**
 * 处理清除全部条件
 */
function handleClearAll() {
  emit('clearAll')
}

/**
 * 切换操作符（带翻转动画）
 */
function toggleOperatorWithAnimation() {
  isFlipping.value = true
  setTimeout(() => {
    localGroup.value.operator = localGroup.value.operator === 'AND' ? 'OR' : 'AND'
    emitChange()
  }, 150)
  setTimeout(() => {
    isFlipping.value = false
  }, 300)
}

/**
 * 发送变更事件
 */
function emitChange() {
  emit('update:modelValue', JSON.parse(JSON.stringify(localGroup.value)))
}

/**
 * 生成子节点key
 */
function getChildKey(child: ConditionNode, index: number): string {
  return `${child.nodeType}-${index}`
}

// ==================== 拖拽事件处理 ====================

/**
 * 拖拽开始
 */
function handleDragStart(index: number, event: DragEvent) {
  draggedIndex.value = index
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(index))
  }
}

/**
 * 拖拽结束
 */
function handleDragEnd() {
  draggedIndex.value = -1
  dragOverIndex.value = -1
}

/**
 * 拖拽进入目标区域
 */
function handleDragEnter(index: number, event: DragEvent) {
  event.preventDefault()
  if (draggedIndex.value !== -1 && index !== draggedIndex.value) {
    dragOverIndex.value = index
  }
}

/**
 * 拖拽经过目标区域
 */
function handleDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

/**
 * 放置
 */
function handleDrop(targetIndex: number, event: DragEvent) {
  event.preventDefault()
  
  if (draggedIndex.value === -1 || draggedIndex.value === targetIndex) {
    return
  }
  
  const newChildren = [...localGroup.value.children]
  const draggedItem = newChildren[draggedIndex.value]
  
  // 确保 draggedItem 存在
  if (!draggedItem) {
    draggedIndex.value = -1
    dragOverIndex.value = -1
    return
  }
  
  // 移除原位置
  newChildren.splice(draggedIndex.value, 1)
  
  // 计算新位置（如果原位置在目标位置之前，需要减1）
  const finalIndex = draggedIndex.value < targetIndex ? targetIndex - 1 : targetIndex
  
  // 插入新位置
  newChildren.splice(finalIndex, 0, draggedItem)
  
  localGroup.value = {
    ...localGroup.value,
    children: newChildren,
  }
  
  // 清理状态
  draggedIndex.value = -1
  dragOverIndex.value = -1
  emitChange()
}
</script>

<style scoped lang="scss">
.condition-group {
  // 根条件组样式
  padding: 8px;
  background-color: transparent;
  border-radius: 4px;
  width: 100%;
  box-sizing: border-box;

  // 嵌套条件组
  &.is-nested {
    margin-left: 8px;
    margin-top: 2px;
    padding: 4px;
    background-color: transparent;
    border-radius: 4px;
    width: calc(100% - 8px);
  }
}

.group-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;

  .operator-label {
    font-weight: 500;
  }

  .depth-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 20px;
    height: 18px;
    padding: 0 4px;
    font-size: 10px;
    font-weight: 500;
    color: var(--color-text-3);
    background-color: var(--color-fill-2);
    border-radius: 4px;
  }

  .operator-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    font-size: 12px;
    font-weight: 600;
    border-radius: 4px;
    cursor: pointer;
    transition: transform 0.15s ease-in-out, background-color 0.2s, border-color 0.2s;

    &.is-flipping {
      transform: rotateY(90deg);
    }

    &.is-and {
      color: var(--color-text-2);
      background-color: rgb(var(--primary-1));

      &:hover {
        background-color: rgb(var(--primary-2));
      }
    }

    &.is-or {
      color: var(--color-text-2);
      background-color: rgb(var(--primary-1));

      &:hover {
        background-color: rgb(var(--primary-2));
      }
    }
  }

  .action-btn {
    color: var(--color-text-2);
    font-size: 12px;
    padding: 0 6px;
    height: 24px;

    &:hover:not(:disabled) {
      color: rgb(var(--primary-6));
      background-color: var(--color-fill-2);
    }

    &:disabled {
      color: var(--color-text-4);
      cursor: not-allowed;
      opacity: 0.5;
    }

    &.delete-btn {
      color: var(--color-text-3);
      opacity: 0;
      transition: opacity 0.2s;

      &:hover {
        color: rgb(var(--danger-6));
        background-color: rgb(var(--danger-1));
      }
    }

    &.clear-all-btn {
      color: var(--color-text-3);
      opacity: 0;
      transition: opacity 0.2s;

      &:hover {
        color: rgb(var(--danger-6));
        background-color: rgb(var(--danger-1));
      }
    }
  }

  &:hover .delete-btn,
  &:hover .clear-all-btn {
    opacity: 1;
  }
}

.group-children {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-left: 8px;
  padding-left: 10px;
  width: calc(100% - 18px);
  box-sizing: border-box;

  // 竖线
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 12px;
    width: 1px;
    background-color: var(--color-border-2);
  }

  // 每个子项的横线
  > :deep(*) {
    position: relative;
    width: 100%;

    &::before {
      content: '';
      position: absolute;
      left: -10px;
      top: 12px;
      width: 8px;
      height: 1px;
      background-color: var(--color-border-2);
    }
  }
}

.empty-children {
  padding: 12px 0;
  margin-top: 4px;
  background-color: var(--color-bg-1);
  border: 1px dashed var(--color-border-2);
  border-radius: 4px;

  &.is-incomplete {
    background-color: rgba(var(--danger-6), 0.06);
    border-color: rgb(var(--danger-6));
  }
}

// 可拖拽项容器
.draggable-item {
  cursor: move;
  transition: opacity 0.15s ease-out, transform 0.15s ease-out;
  width: 100%;
  box-sizing: border-box;

  &.dragging {
    opacity: 0.5;
    transform: scale(0.98);
  }

  &.drag-over {
    position: relative;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      top: -1px;
      height: 2px;
      background-color: rgb(var(--primary-6));
      z-index: 10;
    }
  }
}
</style>
