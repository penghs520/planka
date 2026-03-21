<script setup lang="ts">
import { ref, onMounted } from 'vue'
import SidebarHeader from './SidebarHeader.vue'
import SidebarQuickActions from './SidebarQuickActions.vue'
import SidebarAdmin from './SidebarAdmin.vue'
import SidebarFooter from './SidebarFooter.vue'
import { useSidebarTheme } from '@/composables/useSidebarTheme'

// 初始化主题（确保 DOM 属性同步）
useSidebarTheme()

defineEmits<{
  'open-command-palette': []
}>()

// 侧边栏宽度拖拽调整
const DEFAULT_WIDTH = 220
const MIN_WIDTH = 200
const MAX_WIDTH = 360
const SIDEBAR_WIDTH_KEY = 'app-sidebar-width'

const sidebarWidth = ref(DEFAULT_WIDTH)
const isResizing = ref(false)

onMounted(() => {
  const savedWidth = localStorage.getItem(SIDEBAR_WIDTH_KEY)
  if (savedWidth) {
    sidebarWidth.value = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, parseInt(savedWidth, 10)))
  }
})

function startResize(e: MouseEvent) {
  isResizing.value = true
  const startX = e.clientX
  const startWidth = sidebarWidth.value

  function handleMouseMove(e: MouseEvent) {
    const delta = e.clientX - startX
    const newWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, startWidth + delta))
    sidebarWidth.value = newWidth
  }

  function handleMouseUp() {
    isResizing.value = false
    localStorage.setItem(SIDEBAR_WIDTH_KEY, sidebarWidth.value.toString())
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}
</script>

<template>
  <aside
    class="app-sidebar"
    :style="{ width: sidebarWidth + 'px' }"
  >
    <!-- 顶部：组织切换 -->
    <SidebarHeader />

    <!-- 快捷入口 -->
    <SidebarQuickActions @open-command-palette="$emit('open-command-palette')" />

    <!-- 可滚动的主内容区 -->
    <nav class="sidebar-scroll">
      <!-- TODO: Phase 3 - SidebarTeams 占位 -->

      <!-- 管理入口 -->
      <SidebarAdmin />
    </nav>

    <!-- 底部：用户 + 设置 -->
    <SidebarFooter />

    <!-- 拖拽手柄 -->
    <div
      class="resize-handle"
      :class="{ 'is-resizing': isResizing }"
      @mousedown="startResize"
    />
  </aside>
</template>

<style scoped>
.app-sidebar {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--sidebar-bg);
  position: relative;
  flex-shrink: 0;
  overflow: hidden;
  user-select: none;
  transition: background-color 0.2s ease;
}

.sidebar-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0;
}

.sidebar-scroll::-webkit-scrollbar {
  width: 4px;
}

.sidebar-scroll::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 2px;
}

.sidebar-scroll:hover::-webkit-scrollbar-thumb {
  background: var(--sidebar-bg-active);
}

/* 拖拽手柄 */
.resize-handle {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  cursor: col-resize;
  background-color: transparent;
  transition: background-color 0.15s;
  z-index: 100;
}

.resize-handle:hover,
.resize-handle.is-resizing {
  background-color: var(--sidebar-accent);
}
</style>
