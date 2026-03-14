<script setup lang="ts">
import { ref, computed, onMounted, onUpdated } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import { structureApi } from '@/api'
import CreateButton from '@/components/common/CreateButton.vue'
import StructureBranch from '@/components/structure/StructureBranch.vue'
import StructureEditDrawer from '@/components/structure/StructureEditDrawer.vue'
import type { StructureDefinition } from '@/types/structure'

const { t } = useI18n()

// 数据状态
const loading = ref(false)
const structures = ref<StructureDefinition[]>([])

// 抽屉状态
const drawerVisible = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')
const editingStructure = ref<StructureDefinition | null>(null)

// 引用
const containerRef = ref<HTMLElement | null>(null)
const branchRefs = ref<HTMLElement[]>([])
const circleRef = ref<HTMLElement | null>(null)
const paths = ref<string[]>([])

// 重置引用数组
const setBranchRef = (el: any, index: number) => {
  if (el) {
    branchRefs.value[index] = el.$el // 获取组件根元素
  }
}

// 计算属性：按更新时间排序的架构线列表
const sortedStructures = computed(() => {
  return [...structures.value].sort((a, b) => {
    const timeA = a.updatedAt ? new Date(a.updatedAt).getTime() : 0
    const timeB = b.updatedAt ? new Date(b.updatedAt).getTime() : 0
    return timeB - timeA
  })
})

// 加载架构线列表
async function fetchData() {
  loading.value = true
  try {
    const result = await structureApi.list()
    structures.value = result.content || []
    // 数据加载完成后更新连接线
    setTimeout(updateConnections, 100)
  } catch (error) {
    console.error('Failed to fetch structures:', error)
    Message.error(t('admin.structure.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 更新连接线路径
function updateConnections() {
  if (!containerRef.value || !circleRef.value || branchRefs.value.length === 0) return

  const containerRect = containerRef.value.getBoundingClientRect()
  const circleRect = circleRef.value.getBoundingClientRect()
  
  // 汇聚点：加号按钮的顶部中心
  const endX = circleRect.left + circleRect.width / 2 - containerRect.left
  const endY = circleRect.top - containerRect.top

  const newPaths: string[] = []

  sortedStructures.value.forEach((_, index) => {
    const branchEl = branchRefs.value[index]
    if (!branchEl) return

    // 找到该分支下最后一个节点圆点
    const dots = branchEl.querySelectorAll('.node-dot')
    if (!dots || dots.length === 0) return
    const lastDot = dots[dots.length - 1] as HTMLElement
    const dotRect = lastDot.getBoundingClientRect()

    // 起始点：最后一个节点圆点的底部中心，并保留一定间距
    const startX = dotRect.left + dotRect.width / 2 - containerRect.left
    const startY = dotRect.bottom - containerRect.top + 5 // 5px gap

    // 计算三次贝塞尔曲线控制点
    const diffY = endY - startY
    const cp1x = startX
    const cp1y = startY + diffY * 0.8 // 控制点权重：增加使得起初更垂直
    const cp2x = endX
    const cp2y = endY - diffY * 0.2 // 减少使得末端更平滑

    const d = `M ${startX} ${startY} C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${endX} ${endY}`
    newPaths.push(d)
  })

  paths.value = newPaths
}

// 打开新建抽屉
function handleCreate() {
  drawerMode.value = 'create'
  editingStructure.value = null
  drawerVisible.value = true
}

// 打开编辑抽屉
function handleEdit(structure: StructureDefinition) {
  drawerMode.value = 'edit'
  editingStructure.value = structure
  drawerVisible.value = true
}

import { handleReferenceConflictError } from '@/utils/error-handler'

// 删除架构线
async function handleDelete(structure: StructureDefinition) {
  if (!structure.id) return

  Modal.confirm({
    title: t('admin.structure.confirmDelete'),
    content: t('admin.structure.deleteConfirmContent', { name: structure.name }),
    okText: t('common.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple', // 使用全局优化样式
    async onOk() {
      try {
        await structureApi.delete(structure.id!)
        Message.success(t('admin.structure.deleteSuccess'))
        await fetchData()
      } catch (error) {
        if (!handleReferenceConflictError(error)) {
          console.error('Failed to delete:', error)
          Message.error(t('admin.structure.deleteFailed'))
        }
      }
    },
  })
}

// 保存成功回调
async function handleSaveSuccess() {
  drawerVisible.value = false
  await fetchData()
}

onMounted(() => {
  fetchData()
  
  // 监听窗口大小变化
  const observer = new ResizeObserver(() => {
    updateConnections()
  })
  if (containerRef.value) {
    observer.observe(containerRef.value)
  }
  
  window.addEventListener('resize', updateConnections)
  
  // 清理
  return () => {
    observer.disconnect()
    window.removeEventListener('resize', updateConnections)
  }
})

onUpdated(() => {
  // 确保 DOM 更新后重绘连接线
  setTimeout(updateConnections, 50)
})
</script>

<template>
  <div class="structure-graph-page">
    <!-- 图形化展示区域 -->
    <div class="graph-container">
      <a-spin :loading="loading" class="graph-spin">
        <!-- 有数据时显示架构线分支 -->
        <template v-if="structures.length > 0">
          <div ref="containerRef" class="branches-wrapper">
            <!-- 连接线层 -->
            <svg class="connections-layer">
              <path
                v-for="(path, index) in paths"
                :key="index"
                :d="path"
                class="connection-path"
              />
            </svg>

            <div class="branches-container">
              <!-- 各架构线分支 -->
              <StructureBranch
                v-for="(structure, index) in sortedStructures"
                :key="structure.id"
                :ref="(el) => setBranchRef(el, index)"
                :structure="structure"
                @click="handleEdit(structure)"
                @delete="handleDelete(structure)"
              />
            </div>

            <!-- 底部汇聚点 -->
            <div class="convergence-point">
              <div ref="circleRef" class="convergence-circle" @click="handleCreate">
                <IconPlus />
              </div>
            </div>
          </div>
        </template>

        <!-- 空状态 -->
        <div v-else-if="!loading" class="empty-state">
          <a-empty :description="t('admin.structure.emptyDescription')">
            <template #image>
              <div class="empty-icon">
                <svg viewBox="0 0 100 100" width="80" height="80">
                  <circle cx="50" cy="80" r="8" fill="#e5e6eb" />
                  <line x1="50" y1="72" x2="50" y2="40" stroke="#e5e6eb" stroke-width="2" />
                  <circle cx="50" cy="35" r="5" fill="#e5e6eb" />
                  <line x1="50" y1="30" x2="30" y2="15" stroke="#e5e6eb" stroke-width="2" />
                  <line x1="50" y1="30" x2="70" y2="15" stroke="#e5e6eb" stroke-width="2" />
                  <circle cx="30" cy="12" r="4" fill="#e5e6eb" />
                  <circle cx="70" cy="12" r="4" fill="#e5e6eb" />
                </svg>
              </div>
            </template>
            <CreateButton @click="handleCreate">
              {{ t('admin.structure.createButton') }}
            </CreateButton>
          </a-empty>
        </div>
      </a-spin>
    </div>

    <!-- 编辑抽屉 -->
    <StructureEditDrawer
      v-model:visible="drawerVisible"
      :mode="drawerMode"
      :structure="editingStructure"
      @success="handleSaveSuccess"
    />
  </div>
</template>

<style scoped>
.structure-graph-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
}

.graph-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.graph-spin {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.branches-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 160px 40px 0;
  overflow-x: auto;
  overflow-y: hidden;
}



.convergence-point {
  margin-top: 80px;
  display: flex;
  justify-content: center;
  position: relative;
  z-index: 10;
}

.convergence-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #4f5fd9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(79, 95, 217, 0.3);
  transition: transform 0.2s, box-shadow 0.2s;
}

.convergence-circle:hover {
  transform: scale(1.1);
  background: #4050c8;
  box-shadow: 0 4px 12px rgba(79, 95, 217, 0.4);
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.empty-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.connections-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.connection-path {
  fill: none;
  stroke: #C9CDD4;
  stroke-width: 1;
  stroke-linecap: round;
}

.branches-container {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 60px;
  padding-bottom: 0;
  min-width: min-content;
  position: relative;
  z-index: 10;
}
</style>
