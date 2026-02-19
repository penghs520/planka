<script setup lang="ts">
/**
 * 架构属性编辑器
 *
 * 以树形下拉框的形式展示架构线的所有节点，支持搜索和选择。
 * 选中任意节点后，会自动构造完整的路径链（从根到选中节点）。
 * 搜索过滤在前端完成。
 */
import { ref, computed, onMounted } from 'vue'
import { structureOptionsApi, type StructureNodeDTO } from '@/api/structure-options'
import type { FieldValue } from '@/types/card'

interface StructureItem {
  id: string
  name: string
  next: StructureItem | null
}

const props = defineProps<{
  /** 架构属性定义 ID */
  fieldId: string
  /** 当前值 */
  modelValue?: FieldValue | null
  /** 占位符 */
  placeholder?: string
  /** 是否禁用 */
  disabled?: boolean
  /** 是否在挂载时自动打开下拉框（用于行内编辑场景） */
  autoOpen?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: FieldValue | null]
  change: [value: FieldValue | null]
  /** 取消编辑（在 autoOpen 模式下，下拉框关闭时触发） */
  cancel: []
}>()

// 状态
const loading = ref(false)
const treeData = ref<StructureNodeDTO[]>([])
const searchKeyword = ref('')
const popupVisible = ref(false)

// 当前选中的节点路径（用于显示）
const displayValue = computed(() => {
  if (!props.modelValue?.value) return ''
  return formatStructurePath(props.modelValue.value as StructureItem)
})

// 格式化架构路径为显示文本
function formatStructurePath(item: StructureItem | null): string {
  if (!item) return ''
  const names: string[] = []
  let current: StructureItem | null = item
  while (current) {
    if (current.name) {
      names.push(current.name)
    }
    current = current.next || null
  }
  return names.join(' / ')
}

// 加载树形数据
async function loadTreeData() {
  if (treeData.value.length > 0) return // 已加载过，不重复加载

  loading.value = true
  try {
    const data = await structureOptionsApi.queryOptions({
      structureFieldId: props.fieldId,
    })
    treeData.value = data
  } catch (error) {
    console.error('加载架构属性选项失败:', error)
    treeData.value = []
  } finally {
    loading.value = false
  }
}

// 前端过滤树形数据
function filterTree(nodes: StructureNodeDTO[], keyword: string): StructureNodeDTO[] {
  if (!keyword) return nodes

  const lowerKeyword = keyword.toLowerCase()
  const filtered: StructureNodeDTO[] = []

  for (const node of nodes) {
    const filteredNode = filterNode(node, lowerKeyword)
    if (filteredNode) {
      filtered.push(filteredNode)
    }
  }

  return filtered
}

// 递归过滤节点
function filterNode(node: StructureNodeDTO, lowerKeyword: string): StructureNodeDTO | null {
  const selfMatch = node.name?.toLowerCase().includes(lowerKeyword)

  // 递归过滤子节点
  let filteredChildren: StructureNodeDTO[] | undefined
  if (node.children && node.children.length > 0) {
    filteredChildren = []
    for (const child of node.children) {
      const filteredChild = filterNode(child, lowerKeyword)
      if (filteredChild) {
        filteredChildren.push(filteredChild)
      }
    }
    if (filteredChildren.length === 0) {
      filteredChildren = undefined
    }
  }

  const hasMatchingChildren = filteredChildren && filteredChildren.length > 0

  if (selfMatch || hasMatchingChildren) {
    return {
      ...node,
      // 如果自身匹配，保留所有子节点；否则只保留匹配的子节点
      children: selfMatch ? node.children : filteredChildren,
    }
  }

  return null
}

// 过滤后的树形数据
const filteredTreeData = computed(() => {
  return filterTree(treeData.value, searchKeyword.value)
})

// 构建从根到指定节点的完整路径
function buildPathToNode(nodeId: string, nodes: StructureNodeDTO[]): StructureItem | null {
  for (const node of nodes) {
    if (node.id === nodeId) {
      // 找到目标节点，返回单节点
      return {
        id: node.id,
        name: node.name,
        next: null,
      }
    }
    if (node.children && node.children.length > 0) {
      const childPath = buildPathToNode(nodeId, node.children)
      if (childPath) {
        // 找到了目标节点在子树中，构建链表
        return {
          id: node.id,
          name: node.name,
          next: childPath,
        }
      }
    }
  }
  return null
}

// 处理节点选择
function handleSelect(selectedKeys: string[]) {
  if (selectedKeys.length === 0) {
    const emptyValue = buildEmptyFieldValue()
    emit('update:modelValue', emptyValue)
    emit('change', emptyValue)
    popupVisible.value = false
    return
  }

  const selectedId = selectedKeys[0] as string
  // 使用原始数据构建路径，而不是过滤后的数据
  const path = buildPathToNode(selectedId, treeData.value)

  if (path) {
    const newValue: FieldValue = {
      fieldId: props.fieldId,
      type: 'STRUCTURE',
      value: path,
      readable: true,
    }
    emit('update:modelValue', newValue)
    emit('change', newValue)
  }

  popupVisible.value = false
  searchKeyword.value = '' // 清空搜索
}

// 处理搜索输入
function handleSearch(keyword: string) {
  searchKeyword.value = keyword
}

// 构建空的 FieldValue（用于清空选择）
function buildEmptyFieldValue(): FieldValue {
  return {
    fieldId: props.fieldId,
    type: 'STRUCTURE',
    value: null,
    readable: true,
  }
}

// 清空选择
function handleClear() {
  const emptyValue = buildEmptyFieldValue()
  emit('update:modelValue', emptyValue)
  emit('change', emptyValue)
}

// 下拉框可见性变化
function handlePopupVisibleChange(visible: boolean) {
  popupVisible.value = visible
  if (visible) {
    loadTreeData()
  } else {
    searchKeyword.value = '' // 关闭时清空搜索
    // autoOpen 模式下，下拉框关闭时触发取消（用于退出行内编辑状态）
    if (props.autoOpen) {
      emit('cancel')
    }
  }
}

// 转换为 TreeSelect 需要的数据格式
function convertToTreeSelectData(nodes: StructureNodeDTO[]): any[] {
  return nodes.map(node => ({
    key: node.id,
    title: node.name,
    children: node.children ? convertToTreeSelectData(node.children) : undefined,
    isLeaf: node.leaf,
    // 额外信息
    levelName: node.levelName,
    levelIndex: node.levelIndex,
  }))
}

const treeSelectData = computed(() => convertToTreeSelectData(filteredTreeData.value))

// 获取当前选中的 key
const selectedKeys = computed(() => {
  if (!props.modelValue?.value) return []
  // 找到链表的最后一个节点（当前选中的节点）
  let current = props.modelValue.value as StructureItem | null
  while (current?.next) {
    current = current.next
  }
  return current ? [current.id] : []
})

// 自动打开下拉框（用于行内编辑场景）
onMounted(() => {
  if (props.autoOpen && !props.disabled) {
    popupVisible.value = true
    loadTreeData()
  }
})
</script>

<template>
  <a-tree-select
    :model-value="selectedKeys"
    :data="treeSelectData"
    :loading="loading"
    :disabled="disabled"
    :placeholder="placeholder || '请选择'"
    :popup-visible="popupVisible"
    :allow-clear="true"
    :allow-search="true"
    :filter-tree-node="false"
    tree-checkable="false"
    :class="{ 'compact-mode': autoOpen }"
    @update:model-value="handleSelect"
    @popup-visible-change="handlePopupVisibleChange"
    @search="handleSearch"
    @clear="handleClear"
  >
    <template v-if="!autoOpen" #trigger>
      <div class="structure-trigger" :class="{ disabled }">
        <span v-if="displayValue" class="structure-value">{{ displayValue }}</span>
        <span v-else class="structure-placeholder">{{ placeholder || '请选择' }}</span>
        <span class="structure-arrow">
          <svg viewBox="0 0 16 16" fill="currentColor" width="12" height="12">
            <path d="M4.5 6L8 9.5L11.5 6" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
      </div>
    </template>
    <!-- 自定义标签显示，显示完整路径而不是只显示节点名称 -->
    <template #label="{ data }">
      <span class="structure-label">{{ displayValue || data?.title || '' }}</span>
    </template>
    <template #tree-slot-title="{ title, levelName }">
      <span class="node-title">
        {{ title }}
        <span v-if="levelName" class="node-level-name">{{ levelName }}</span>
      </span>
    </template>
  </a-tree-select>
</template>

<style scoped lang="scss">
// 根元素样式 - 确保组件响应外部宽度设置
:deep(.arco-tree-select) {
  width: 100%;
}

// 紧凑模式（用于行内编辑场景）
.compact-mode {
  :deep(.arco-tree-select-view) {
    min-height: 24px;
    padding: 0 6px;
    font-size: 13px;
  }

  :deep(.arco-tree-select-view-single) {
    padding: 0 6px;
  }
}

.structure-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  padding: 4px 8px;
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  background: var(--color-bg-2);
  cursor: pointer;
  transition: border-color 0.2s;

  &:hover:not(.disabled) {
    border-color: rgb(var(--primary-6));
  }

  &.disabled {
    background: var(--color-fill-2);
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.structure-value {
  flex: 1;
  color: var(--color-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.structure-placeholder {
  flex: 1;
  color: var(--color-text-4);
}

.structure-arrow {
  color: var(--color-text-3);
  margin-left: 8px;
}

.node-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-level-name {
  font-size: 12px;
  color: var(--color-text-3);
  padding: 0 4px;
  background: var(--color-fill-2);
  border-radius: 2px;
}

.structure-label {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
