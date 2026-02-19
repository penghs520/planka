<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import {
  IconSearch,
  IconApps,
  IconCalendar,
  IconClockCircle,
  IconFile,
  IconCheckCircle,
  IconDown,
} from '@arco-design/web-vue/es/icon'
import { menuApi } from '@/api/menu'
import { outsourcingConfigApi } from '@/api/outsourcing-config'
import type { MenuTreeVO, MenuTreeNodeVO } from '@/types/menu'
import MenuNode from './components/MenuNode.vue'

const { t } = useI18n()
const router = useRouter()
const orgStore = useOrgStore()

const props = defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', viewId: string): void
  (e: 'select', viewId: string): void
}>()

const route = useRoute()

// 状态
const loading = ref(false)
const menuTree = ref<MenuTreeVO | null>(null)
const searchKeyword = ref('')
const expandedGroups = ref<Set<string>>(new Set())
const pinnedMenus = ref<MenuTreeNodeVO[]>([])
const attendanceEnabled = ref(false)
const attendanceMenuExpanded = ref(true)

// 过滤后的菜单树
const filteredRoots = computed(() => {
  if (!menuTree.value) return []
  if (!searchKeyword.value) return menuTree.value.roots

  const keyword = searchKeyword.value.toLowerCase()
  return filterNodes(menuTree.value.roots, keyword)
})

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

// 加载菜单树
async function fetchMenuTree() {
  loading.value = true
  try {
    menuTree.value = await menuApi.getMenuTree()
    // 默认展开所有分组（递归处理）
    collectExpandedGroups(menuTree.value.roots)
  } catch (error) {
    console.error('Failed to fetch menu tree:', error)
  } finally {
    loading.value = false
  }
}

// 加载考勤配置
async function fetchAttendanceConfig() {
  if (!orgStore.currentOrgId) return

  try {
    const config = await outsourcingConfigApi.getByOrgId(orgStore.currentOrgId)
    attendanceEnabled.value = config?.cardAttendanceRequired ?? false
  } catch (error) {
    console.error('Failed to fetch attendance config:', error)
    attendanceEnabled.value = false
  }
}

// 考勤菜单项
const attendanceMenuItems = computed(() => [
  {
    id: 'attendance-clock',
    name: t('attendance.clock'),
    icon: IconClockCircle,
    path: '/attendance/clock',
  },
  {
    id: 'attendance-my-records',
    name: t('attendance.myRecords'),
    icon: IconCalendar,
    path: '/attendance/my-records',
  },
  {
    id: 'attendance-applications',
    name: t('attendance.applications'),
    icon: IconFile,
    path: '/attendance/applications',
  },
  {
    id: 'attendance-approvals',
    name: t('attendance.approvals'),
    icon: IconCheckCircle,
    path: '/attendance/approvals',
  },
])

// 判断考勤菜单项是否选中
function isAttendanceMenuActive(path: string): boolean {
  return route.path === path
}

// 导航到考勤页面
function navigateToAttendance(path: string) {
  router.push(path)
}

// 切换考勤菜单展开状态
function toggleAttendanceMenu() {
  attendanceMenuExpanded.value = !attendanceMenuExpanded.value
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
  fetchAttendanceConfig()
})
</script>

<template>
  <div class="workspace-menu">
    <!-- 搜索框 -->
    <div class="search-box">
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
      <div class="menu-tree">
        <!-- 考勤菜单 -->
        <div v-if="attendanceEnabled" class="menu-section attendance-section">
          <div class="section-header" @click="toggleAttendanceMenu">
            <IconCalendar class="section-icon" />
            <span class="section-title">{{ t('attendance.title') }}</span>
            <icon-down
              :class="['expand-icon', { expanded: attendanceMenuExpanded }]"
            />
          </div>
          <div v-show="attendanceMenuExpanded" class="section-content">
            <div
              v-for="item in attendanceMenuItems"
              :key="item.id"
              :class="['menu-item', 'attendance-item', { active: isAttendanceMenuActive(item.path) }]"
              @click="navigateToAttendance(item.path)"
            >
              <component :is="item.icon" class="item-icon" />
              <span class="item-name">{{ item.name }}</span>
            </div>
          </div>
        </div>

        <!-- 置顶菜单 -->
        <div v-if="pinnedMenus.length > 0" class="menu-section">
          <div class="section-header">
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

        <!-- 全部菜单 -->
        <div class="menu-section">
          <div class="section-header">
            <span class="section-title">{{ t('common.layout.allMenus') }}</span>
          </div>
          <div class="section-content">
            <template v-if="filteredRoots.length > 0">
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
            </template>
            <a-empty v-else-if="!loading" :description="t('common.layout.noMenus')" />
          </div>
        </div>
      </div>
    </a-spin>
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

.attendance-section {
  border-bottom: 1px solid var(--color-border-2);
  padding-bottom: 8px;
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
</style>
