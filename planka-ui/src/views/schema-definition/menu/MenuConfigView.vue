<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { VueDraggable } from 'vue-draggable-plus'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconRefresh,
  IconApps,
  IconDragDotVertical,
} from '@arco-design/web-vue/es/icon'
import { menuApi } from '@/api/menu'
import type { MenuTreeVO, MenuTreeNodeVO, MenuGroupDefinition } from '@/types/menu'
import { createEmptyMenuGroup, MenuIconOptions } from '@/types/menu'
import { useOrgStore } from '@/stores/org'
import type { Condition } from '@/types/condition'
import ConditionEditor from '@/components/condition/ConditionEditor.vue'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import CreateButton from '@/components/common/CreateButton.vue'
import MenuTreeNode, { type NodeMovedPayload } from './components/MenuTreeNode.vue'
import { handleReferenceConflictError } from '@/utils/error-handler'

const { t } = useI18n()
const orgStore = useOrgStore()

// 数据状态
const loading = ref(false)
const menuTree = ref<MenuTreeVO | null>(null)

// 树根节点（响应式，用于本地更新）
const treeRoots = ref<MenuTreeNodeVO[]>([])
const ungroupedViews = ref<MenuTreeNodeVO[]>([])

// 深拷贝函数（避免 structuredClone 对某些对象的限制）
function deepClone<T>(obj: T): T {
  return JSON.parse(JSON.stringify(obj))
}


// 抽屉状态
const drawerVisible = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')
const editingGroup = ref<MenuGroupDefinition | null>(null)
const parentGroupId = ref<string | undefined>(undefined)
const saving = ref(false)

// 权限配置状态
const visibilityCondition = ref<Condition | null>(null)

// 左侧面板拖拽配置（克隆模式，拖出后保留原项）
const leftPanelOptions = {
  group: {
    name: 'menu',
    pull: 'clone' as const, // 克隆拖出，保留原项
    put: false, // 不允许放入
  },
  sort: false, // 禁止排序
  animation: 200,
  ghostClass: 'menu-drag-ghost',
  chosenClass: 'menu-drag-chosen',
}

// 初始化权限配置
function initVisibilityFromGroup(group: MenuGroupDefinition) {
  visibilityCondition.value = group.visibilityCondition || null
}

// 将权限配置写回分组
function applyVisibilityToGroup(group: MenuGroupDefinition) {
  group.visibilityCondition = visibilityCondition.value || undefined
}

// 加载菜单树
async function loadMenuTree() {
  loading.value = true
  try {
    const tree = await menuApi.getMenuTree()
    menuTree.value = tree
    // 直接更新本地状态
    treeRoots.value = deepClone(tree.roots || [])
    ungroupedViews.value = deepClone(tree.ungroupedViews || [])
  } catch (error) {
    console.error('Failed to load menu tree:', error)
  } finally {
    loading.value = false
  }
}

// 新建根分组
function handleCreateRootGroup() {
  if (!orgStore.currentOrgId) {
    Message.warning(t('admin.menuConfig.selectOrgFirst'))
    return
  }
  drawerMode.value = 'create'
  parentGroupId.value = undefined
  editingGroup.value = createEmptyMenuGroup(orgStore.currentOrgId)
  initVisibilityFromGroup(editingGroup.value)
  drawerVisible.value = true
}

// 新建子分组
function handleCreateSubGroup(parentId: string) {
  if (!orgStore.currentOrgId) {
    Message.warning(t('admin.menuConfig.selectOrgFirst'))
    return
  }
  drawerMode.value = 'create'
  parentGroupId.value = parentId
  const newGroup = createEmptyMenuGroup(orgStore.currentOrgId)
  newGroup.parentId = parentId
  editingGroup.value = newGroup
  initVisibilityFromGroup(newGroup)
  drawerVisible.value = true
}

// 编辑分组
async function handleEditGroup(nodeId: string) {
  drawerMode.value = 'edit'
  try {
    editingGroup.value = await menuApi.getById(nodeId)
    initVisibilityFromGroup(editingGroup.value)
    drawerVisible.value = true
  } catch (error) {
    console.error('Failed to load group:', error)
  }
}

// 删除分组
function handleDeleteGroup(node: MenuTreeNodeVO) {
  Modal.confirm({
    title: t('admin.menuConfig.confirmDelete'),
    content: t('admin.menuConfig.deleteGroupConfirm', { name: node.name }),
    okText: t('common.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await menuApi.delete(node.id)
        Message.success(t('admin.menuConfig.deleteSuccess'))
        await loadMenuTree()
      } catch (error) {
        if (!handleReferenceConflictError(error)) {
          console.error('Failed to delete group:', error)
        }
      }
    },
  })
}

// 保存分组
async function handleSaveGroup() {
  if (!editingGroup.value || !editingGroup.value.name?.trim()) {
    Message.warning(t('admin.menuConfig.groupNameRequired'))
    return
  }

  applyVisibilityToGroup(editingGroup.value)

  saving.value = true
  try {
    if (drawerMode.value === 'create') {
      await menuApi.create(editingGroup.value)
      Message.success(t('admin.menuConfig.createSuccess'))
    } else {
      await menuApi.update(editingGroup.value.id!, editingGroup.value, editingGroup.value.contentVersion)
      Message.success(t('admin.menuConfig.saveSuccess'))
    }
    drawerVisible.value = false
    await loadMenuTree()
  } catch (error) {
    console.error('Failed to save group:', error)
  } finally {
    saving.value = false
  }
}

// 从分组移除视图
async function handleRemoveViewFromGroup(groupId: string, viewId: string) {
  try {
    await menuApi.removeViewFromGroup(groupId, viewId)
    Message.success(t('admin.menuConfig.removeSuccess'))
    await loadMenuTree()
  } catch (error) {
    console.error('Failed to remove view from group:', error)
  }
}

// 收集分组内的视图ID（按当前顺序）
function collectViewIdsInGroup(groupId: string): string[] {
  function findGroup(nodes: MenuTreeNodeVO[]): MenuTreeNodeVO | undefined {
    for (const node of nodes) {
      if (node.id === groupId) return node
      if (node.children?.length) {
        const found = findGroup(node.children)
        if (found) return found
      }
    }
    return undefined
  }

  const group = findGroup(treeRoots.value)
  if (!group) return []

  return group.children?.filter((child) => child.type === 'VIEW').map((child) => child.id) || []
}

// 处理节点移动（统一入口）
async function handleNodeMoved(payload: NodeMovedPayload) {
  const { draggedNode, from, to } = payload

  // 如果位置没有变化，不处理
  if (from.parentId === to.parentId && from.oldIndex === to.newIndex) {
    return
  }

  try {
    if (draggedNode.type === 'VIEW') {
      await handleViewMoved(draggedNode, from, to)
    } else {
      await handleGroupMoved(draggedNode, from, to)
    }
    Message.success(t('admin.menuConfig.moveSuccess'))
    await loadMenuTree()
  } catch (error) {
    console.error('Move failed:', error)
    Message.error(t('admin.menuConfig.moveFailed'))
    // 失败时回滚
    await loadMenuTree()
  }
}

// 视图移动处理
async function handleViewMoved(
  view: MenuTreeNodeVO,
  from: { parentId?: string; oldIndex: number },
  to: { parentId?: string; newIndex: number },
) {
  // 情况1：从未分组列表拖到分组（from.parentId 为 undefined 且 to.parentId 有值）
  if (!from.parentId && to.parentId) {
    await menuApi.addViewToGroup(to.parentId, { viewId: view.id })
    return
  }

  // 情况2：从一个分组移到另一个分组
  if (from.parentId && to.parentId && from.parentId !== to.parentId) {
    await menuApi.removeViewFromGroup(from.parentId, view.id)
    await menuApi.addViewToGroup(to.parentId, { viewId: view.id })
    return
  }

  // 情况3：同一分组内重排序
  if (from.parentId === to.parentId && from.parentId) {
    const viewIds = collectViewIdsInGroup(from.parentId)
    await menuApi.reorderViews(from.parentId, { viewIds })
  }
}

// 分组移动处理
async function handleGroupMoved(
  group: MenuTreeNodeVO,
  _from: { parentId?: string; oldIndex: number },
  to: { parentId?: string; newIndex: number },
) {
  const groupDef = await menuApi.getById(group.id)

  // 更新父ID
  groupDef.parentId = to.parentId || undefined

  // 更新 sortOrder（根据在列表中的新位置）
  groupDef.sortOrder = to.newIndex

  await menuApi.update(groupDef.id!, groupDef, groupDef.contentVersion)
}

// 更新树根节点
function handleTreeRootsUpdate(newRoots: MenuTreeNodeVO[]) {
  treeRoots.value = newRoots
}

onMounted(() => {
  loadMenuTree()
})
</script>

<template>
  <div class="menu-config-page">
    <!-- 左侧待选视图列表 -->
    <aside class="views-panel">
      <div class="panel-header">
        <h3>{{ t('admin.menuConfig.pendingViews') }}</h3>
        <a-button size="small" @click="loadMenuTree">
          <template #icon><IconRefresh /></template>
        </a-button>
      </div>
      <div class="views-list">
        <a-spin :loading="loading" style="width: 100%">
          <VueDraggable
            v-if="ungroupedViews.length > 0"
            v-model="ungroupedViews"
            v-bind="leftPanelOptions"
            class="ungrouped-list"
          >
            <div v-for="view in ungroupedViews" :key="view.id" class="view-item">
              <IconDragDotVertical class="drag-handle" />
              <IconApps class="view-icon" />
              <div class="view-info">
                <span class="view-name">{{ view.name }}</span>
                <span v-if="view.cardTypeName" class="view-type">{{ view.cardTypeName }}</span>
              </div>
            </div>
          </VueDraggable>
          <a-empty v-else :description="t('admin.menuConfig.noPendingViews')" />
        </a-spin>
      </div>
      <div class="panel-footer">
        <p class="tip">{{ t('admin.menuConfig.dragViewHint') }}</p>
      </div>
    </aside>

    <!-- 主区域菜单树配置 -->
    <main class="menu-tree-panel">
      <div class="panel-header">
        <h3>{{ t('admin.menuConfig.menuStructure') }}</h3>
        <div class="header-actions">
          <CreateButton size="small" @click="handleCreateRootGroup">
            {{ t('admin.menuConfig.createGroup') }}
          </CreateButton>
        </div>
      </div>

      <div class="tree-wrapper">
        <a-spin :loading="loading" style="width: 100%">
          <MenuTreeNode
            v-if="treeRoots.length > 0"
            :nodes="treeRoots"
            :level="0"
            :all-nodes="treeRoots"
            @update:nodes="handleTreeRootsUpdate"
            @node-moved="handleNodeMoved"
            @edit-group="handleEditGroup"
            @delete-group="handleDeleteGroup"
            @remove-view="handleRemoveViewFromGroup"
            @create-sub-group="handleCreateSubGroup"
          />
          <div v-else class="empty-tree">
            <a-empty :description="t('admin.menuConfig.noMenuGroups')">
              <CreateButton @click="handleCreateRootGroup">
                {{ t('admin.menuConfig.createFirstGroup') }}
              </CreateButton>
            </a-empty>
          </div>
        </a-spin>
      </div>

      <div class="panel-footer">
        <p class="tip">{{ t('admin.menuConfig.dragNodeHint') }}</p>
      </div>
    </main>

    <!-- 编辑抽屉 -->
    <a-drawer
      v-model:visible="drawerVisible"
      :title="drawerMode === 'create' ? t('admin.menuConfig.createGroup') : t('admin.menuConfig.editGroup')"
      :width="900"
      :mask-closable="true"
      @cancel="drawerVisible = false"
    >
      <a-form v-if="editingGroup" :model="editingGroup" layout="vertical">
        <!-- 基本信息 -->
        <a-typography-title :heading="6" style="margin-bottom: 16px">{{ t('admin.menuConfig.basicInfo') }}</a-typography-title>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('admin.menuConfig.groupName')" required>
              <a-input v-model="editingGroup.name" :placeholder="t('admin.menuConfig.groupNamePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('admin.menuConfig.icon')">
              <a-select v-model="editingGroup.icon" :placeholder="t('admin.menuConfig.iconPlaceholder')" allow-clear>
                <a-option v-for="icon in MenuIconOptions" :key="icon.value" :value="icon.value">
                  {{ icon.label }}
                </a-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('admin.menuConfig.sortOrder')">
              <a-input-number v-model="editingGroup.sortOrder" :min="0" :placeholder="t('admin.menuConfig.sortOrderHint')" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('admin.menuConfig.defaultExpanded')">
              <a-switch v-model="editingGroup.expanded" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="t('admin.menuConfig.description')">
          <a-textarea
            v-model="editingGroup.description"
            :placeholder="t('admin.menuConfig.descriptionPlaceholder')"
            :max-length="200"
            show-word-limit
          />
        </a-form-item>

        <a-divider />

        <!-- 权限配置 -->
        <a-typography-title :heading="6" style="margin-bottom: 8px">{{ t('admin.menuConfig.permissionConfig') }}</a-typography-title>
        <p class="section-desc">{{ t('admin.menuConfig.permissionConfigHint') }}</p>
        <div class="condition-wrapper">
          <ConditionEditor
            v-model="visibilityCondition"
            root-card-type-name="成员"
          />
        </div>
      </a-form>
      <template #footer>
        <a-space>
          <CancelButton @click="drawerVisible = false" />
          <SaveButton
            :loading="saving"
            :text="drawerMode === 'create' ? '创建' : '保存'"
            @click="handleSaveGroup"
          />
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>

<style scoped lang="scss">
.menu-config-page {
  display: flex;
  height: 100%;
  background: var(--color-bg-1);
}

// 左侧待选视图面板
.views-panel {
  width: 280px;
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  background: var(--color-bg-2);

  .panel-header {
    padding: 16px;
    border-bottom: 1px solid var(--color-border);
    display: flex;
    align-items: center;
    justify-content: space-between;

    h3 {
      margin: 0;
      font-size: 14px;
      font-weight: 500;
    }
  }

  .views-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
  }

  .panel-footer {
    padding: 12px 16px;
    border-top: 1px solid var(--color-border);

    .tip {
      margin: 0;
      font-size: 12px;
      color: var(--color-text-3);
    }
  }
}

.ungrouped-list {
  min-height: 100px;
}

.view-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  margin-bottom: 4px;
  background: var(--color-bg-1);
  border-radius: 6px;
  cursor: default;
  transition: all 0.2s;
  border: 1px solid transparent;

  &:hover {
    background: var(--color-fill-2);

    .drag-handle {
      opacity: 1;
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

  .view-icon {
    color: var(--color-text-3);
    flex-shrink: 0;
  }

  .view-info {
    flex: 1;
    min-width: 0;

    .view-name {
      display: block;
      font-size: 13px;
      color: var(--color-text-1);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .view-type {
      display: block;
      font-size: 11px;
      color: var(--color-text-3);
      margin-top: 2px;
    }
  }
}

// 主区域菜单树面板
.menu-tree-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-1);

  .panel-header {
    padding: 16px 24px;
    border-bottom: 1px solid var(--color-border);
    display: flex;
    align-items: center;
    justify-content: space-between;

    h3 {
      margin: 0;
      font-size: 16px;
    }

    .header-actions {
      display: flex;
      gap: 8px;
    }
  }

  .tree-wrapper {
    flex: 1;
    overflow-y: auto;
    padding: 16px 24px;
    min-height: 200px;
  }

  .panel-footer {
    padding: 12px 24px;
    border-top: 1px solid var(--color-border);

    .tip {
      margin: 0;
      font-size: 12px;
      color: var(--color-text-3);
    }
  }
}

.empty-tree {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

// 权限配置样式
.section-desc {
  font-size: 13px;
  color: var(--color-text-3);
  margin: 0 0 16px 0;
}

.condition-wrapper {
  background: var(--color-fill-1);
  border-radius: 8px;
  padding: 16px;
  min-height: 120px;
}

// 拖拽视觉反馈（全局样式）
:deep(.menu-drag-ghost) {
  opacity: 0.5;
  background: rgb(var(--primary-1)) !important;
  border: 2px dashed rgb(var(--primary-6)) !important;
  border-radius: 6px;
}

:deep(.menu-drag-chosen) {
  background: rgb(var(--primary-1));
}

:deep(.menu-drag-active) {
  opacity: 0.9;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
</style>
