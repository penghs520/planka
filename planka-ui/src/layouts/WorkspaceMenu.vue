<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  IconSearch,
  IconApps,
} from '@arco-design/web-vue/es/icon'
import { Message } from '@arco-design/web-vue'
import { menuNavApi } from '@/api/menu-nav'
import { viewApi } from '@/api/view'
import { useOrgStore } from '@/stores/org'
import type { MenuTreeVO, MenuTreeNodeVO } from '@/types/menu'
import type { ListViewDefinition } from '@/types/view'
import MenuNode from './components/MenuNode.vue'
import ViewEditForm from '@/views/schema-definition/view/ViewEditForm.vue'

const { t } = useI18n()
const orgStore = useOrgStore()
const route = useRoute()

const props = withDefaults(
  defineProps<{
    modelValue?: string
    /** 嵌入 App 侧栏时使用：隐藏分区标题、紧凑样式、随侧栏滚动 */
    variant?: 'default' | 'sidebar'
    /** 架构节点页传入当前 nodeId，用于拉取 STRUCTURE_NODE 视图 */
    structureNodeId?: string
  }>(),
  { variant: 'default' },
)

const emit = defineEmits<{
  (e: 'update:modelValue', viewId: string): void
  (e: 'select', viewId: string): void
}>()

// 状态
const loading = ref(false)
const menuTree = ref<MenuTreeVO | null>(null)
const searchKeyword = ref('')
const expandedGroups = ref<Set<string>>(new Set())
const pinnedMenus = ref<MenuTreeNodeVO[]>([])
const createViewVisible = ref(false)
const createViewSaving = ref(false)

// 过滤后的菜单树
const filteredRoots = computed(() => {
  if (!menuTree.value) return []
  if (!searchKeyword.value) return menuTree.value.roots

  const keyword = searchKeyword.value.toLowerCase()
  return filterNodes(menuTree.value.roots, keyword)
})

const filteredUngrouped = computed(() => {
  const ug = menuTree.value?.ungroupedViews ?? []
  if (!searchKeyword.value) {
    return ug
  }
  const keyword = searchKeyword.value.toLowerCase()
  return ug.filter((n) => n.name.toLowerCase().includes(keyword))
})

const hasAnyNavItems = computed(
  () => filteredRoots.value.length > 0 || filteredUngrouped.value.length > 0,
)

// 递归过滤节点
function filterNodes(nodes: MenuTreeNodeVO[], keyword: string): MenuTreeNodeVO[] {
  return nodes
    .map((node) => {
      if (node.type === 'VIEW') {
        // 视图节点：匹配名称
        if (node.name.toLowerCase().includes(keyword)) {
          return node
        }
        return null
      } else {
        // 分组节点：递归过滤子节点
        const filteredChildren = filterNodes(node.children, keyword)
        if (filteredChildren.length > 0 || node.name.toLowerCase().includes(keyword)) {
          return { ...node, children: filteredChildren }
        }
        return null
      }
    })
    .filter((node): node is MenuTreeNodeVO => node !== null)
}

// 递归收集需要展开的分组
function collectExpandedGroups(nodes: MenuTreeNodeVO[]) {
  nodes.forEach((node) => {
    if (node.type === 'GROUP') {
      if (node.expanded !== false) {
        expandedGroups.value.add(node.id)
      }
      if (node.children?.length) {
        collectExpandedGroups(node.children)
      }
    }
  })
}

// 加载 Schema 菜单树（分组 + 视图，已按可见性过滤）
async function fetchMenuTree() {
  loading.value = true
  try {
    const params = props.structureNodeId ? { structureNodeId: props.structureNodeId } : undefined
    menuTree.value = await menuNavApi.nav(params)
    collectExpandedGroups(menuTree.value.roots)
    if (menuTree.value.ungroupedViews?.length) {
      collectExpandedGroups(menuTree.value.ungroupedViews)
    }
  } catch (error) {
    console.error('Failed to fetch workspace nav:', error)
  } finally {
    loading.value = false
  }
}

// 切换分组展开状态
function toggleGroup(groupId: string) {
  if (expandedGroups.value.has(groupId)) {
    expandedGroups.value.delete(groupId)
  } else {
    expandedGroups.value.add(groupId)
  }
}

// 判断分组是否展开
function isExpanded(groupId: string): boolean {
  return expandedGroups.value.has(groupId)
}

// 选择视图
function handleSelectView(viewId: string) {
  emit('update:modelValue', viewId)
  emit('select', viewId)
}

async function handleCreateViewSave(def: ListViewDefinition) {
  createViewSaving.value = true
  try {
    const created = await viewApi.create(def)
    Message.success(t('common.state.success'))
    createViewVisible.value = false
    await fetchMenuTree()
    if (created.id) {
      handleSelectView(created.id)
    }
  } catch {
    Message.error(t('sidebar.createViewFailed'))
  } finally {
    createViewSaving.value = false
  }
}

// 判断是否选中
function isSelected(viewId: string): boolean {
  return props.modelValue === viewId
}

// 从路由初始化选中的视图
watch(
  () => route.query.viewId,
  (viewId) => {
    if (viewId && typeof viewId === 'string') {
      emit('update:modelValue', viewId)
    }
  },
  { immediate: true },
)

onMounted(() => {
  fetchMenuTree()
})

watch(
  () => props.structureNodeId,
  () => {
    void fetchMenuTree()
  },
)
</script>

<template>
  <div
    class="workspace-menu"
    :class="{ 'workspace-menu--sidebar': props.variant === 'sidebar' }"
  >
    <!-- 搜索框（仅独立面板；侧栏嵌入不展示） -->
    <div v-if="props.variant === 'default'" class="search-box">
      <a-input
        v-model="searchKeyword"
        size="small"
        allow-clear
      >
        <template #suffix>
          <IconSearch />
        </template>
      </a-input>
    </div>

    <!-- 菜单树 -->
    <a-spin :loading="loading" class="menu-tree-container">
      <div class="menu-tree" :class="{ 'menu-tree--sidebar': props.variant === 'sidebar' }">
        <!-- 置顶菜单 -->
        <div v-if="pinnedMenus.length > 0" class="menu-section">
          <div v-if="props.variant === 'default'" class="section-header">
            <span class="section-title">{{ t('common.layout.pinnedMenus') }}</span>
          </div>
          <div class="section-content">
            <div
              v-for="item in pinnedMenus"
              :key="item.id"
              :class="['menu-item', { active: isSelected(item.id) }]"
              @click="handleSelectView(item.id)"
            >
              <IconApps class="item-icon" />
              <span class="item-name">{{ item.name }}</span>
            </div>
          </div>
        </div>

        <!-- 全部菜单（侧栏嵌入时不重复「全部菜单」标题，节点紧跟在「视图」项下） -->
        <div class="menu-section">
          <div v-if="props.variant === 'default'" class="section-header">
            <span class="section-title">{{ t('common.layout.allMenus') }}</span>
          </div>
          <div class="section-content">
            <template v-if="hasAnyNavItems">
              <MenuNode
                v-for="node in filteredRoots"
                :key="node.id"
                :node="node"
                :level="0"
                :is-expanded="isExpanded"
                :is-selected="isSelected"
                @toggle-group="toggleGroup"
                @select-view="handleSelectView"
              />
              <MenuNode
                v-for="node in filteredUngrouped"
                :key="`ug-${node.id}`"
                :node="node"
                :level="0"
                :is-expanded="isExpanded"
                :is-selected="isSelected"
                @toggle-group="toggleGroup"
                @select-view="handleSelectView"
              />
            </template>
            <a-empty v-else-if="!loading" :description="t('common.layout.noMenus')" />
          </div>
        </div>
      </div>
    </a-spin>

    <button
      type="button"
      class="workspace-create-view-btn"
      :disabled="createViewSaving || !orgStore.currentOrgId"
      @click="createViewVisible = true"
    >
      + {{ t('sidebar.createView') }}
    </button>

    <ViewEditForm
      v-model:visible="createViewVisible"
      mode="create"
      :structure-node-context-id="props.structureNodeId"
      :save-loading="createViewSaving"
      @save="handleCreateViewSave"
    />
  </div>
</template>

<style scoped lang="scss">
.workspace-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.search-box {
  padding: 4px 4px;

  :deep(.arco-input-wrapper) {
    height: 24px;
  }

  :deep(.arco-input) {
    color: #666 !important;
  }
}

.menu-tree-container {
  flex: 1;
  overflow: hidden;
}

.menu-tree {
  height: 100%;
  overflow-y: auto;
  padding: 4px 0;
}

.menu-section {
  margin-bottom: 8px;
}

.section-header {
  padding: 4px 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;

  &:hover {
    background: var(--color-fill-2);
  }
}

.section-icon {
  font-size: 14px;
  color: var(--color-text-3);
}

.expand-icon {
  margin-left: auto;
  font-size: 12px;
  color: var(--color-text-3);
  transition: transform 0.2s;

  &.expanded {
    transform: rotate(180deg);
  }
}

.section-title {
  font-size: 12px;
  color: var(--color-text-3);
  font-weight: 500;
}

.section-content {
  min-height: 32px;
}

.empty-tip {
  padding: 8px 12px;
  font-size: 12px;
  color: var(--color-text-3);
}

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

.group-children {
  padding-left: 8px;
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

.root-item {
  margin-left: 4px;
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

.workspace-menu--sidebar {
  flex: none;
  overflow: visible;
  min-height: 0;

  .menu-tree-container {
    overflow: visible;
  }

  .menu-tree--sidebar {
    overflow: visible;
    max-height: none;
    padding: 2px 0 4px;
  }
}

.workspace-create-view-btn {
  display: flex;
  align-items: center;
  width: 100%;
  height: 28px;
  margin-top: 4px;
  padding: 0 8px;
  border: none;
  border-radius: 5px;
  background: transparent;
  font-family: var(--sidebar-nav-font-family, inherit);
  font-size: var(--sidebar-nav-font-size, 13px);
  font-weight: var(--sidebar-nav-font-weight, 400);
  color: var(--sidebar-text-secondary, var(--color-text-2));
  cursor: pointer;
  text-align: left;
  flex-shrink: 0;
}

.workspace-create-view-btn:hover:not(:disabled) {
  background: var(--sidebar-bg-hover, var(--color-fill-2));
  color: var(--sidebar-accent, rgb(var(--primary-6)));
}

.workspace-create-view-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

</style>
