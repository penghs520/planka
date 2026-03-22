<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconApps } from '@arco-design/web-vue/es/icon'
import { Message } from '@arco-design/web-vue'

import type {
  StepConfig,
  StatusConfig,
  ValueStreamDefinition,
} from '@/types/value-stream'
import { StatusWorkType } from '@/types/value-stream'
import StepColumn from './StepColumn.vue'
import StepEditModal from './StepEditModal.vue'
import StatusEditModal from './StatusEditModal.vue'
import StatusMigrationModal from './StatusMigrationModal.vue'
import { valueStreamApi } from '@/api/value-stream'
import { useLoading } from '@/hooks/useLoading'

const { t } = useI18n()

const props = defineProps<{
  stepList?: StepConfig[]
  valueStream?: ValueStreamDefinition
}>()

const emit = defineEmits<{
  'update:stepList': [stepList: StepConfig[]]
}>()

// 连线相关
const boardContainerRef = ref<HTMLElement | null>(null)
const connectorLines = ref<Array<{ x1: number; y1: number; x2: number; y2: number; path: string }>>(
  []
)
const hoveredInsertIndex = ref<number | null>(null)
const statusInsertHoverCount = ref(0)

// 获取要显示的阶段列表
const displayStepList = computed(() => props.stepList || [])

// 计算阶段之间的连线位置
function updateConnectorLines() {
  if (!boardContainerRef.value) return

  const container = boardContainerRef.value
  const containerRect = container.getBoundingClientRect()
  const scrollLeft = container.scrollLeft

  // 获取所有 step-column
  const stepColumns = container.querySelectorAll('.step-column')
  const lines: Array<{ x1: number; y1: number; x2: number; y2: number; path: string }> = []

  stepColumns.forEach((column, index) => {
    if (index >= stepColumns.length - 1) return // 最后一个阶段不需要连线

    // 当前阶段的最后一个状态卡片
    const currentStatusCards = column.querySelectorAll('.status-card')
    const lastStatusCard = currentStatusCards[currentStatusCards.length - 1]
    if (!lastStatusCard) return

    // 下一个阶段的第一个状态卡片
    const nextColumn = stepColumns[index + 1]
    if (!nextColumn) return

    const nextStatusCards = nextColumn.querySelectorAll('.status-card')
    const firstStatusCard = nextStatusCards[0]
    if (!firstStatusCard) return

    // 计算位置（相对于 board-container）
    const lastRect = lastStatusCard.getBoundingClientRect()
    const firstRect = firstStatusCard.getBoundingClientRect()

    const x1 = lastRect.right - containerRect.left + scrollLeft
    const y1 = lastRect.top + lastRect.height / 2 - containerRect.top
    const x2 = firstRect.left - containerRect.left + scrollLeft
    const y2 = firstRect.top + firstRect.height / 2 - containerRect.top

    // 计算折线路径：水平 -> 垂直 -> 水平
    const midX = (x1 + x2) / 2
    const path = `M ${x1} ${y1} L ${midX} ${y1} L ${midX} ${y2} L ${x2} ${y2}`

    lines.push({ x1, y1, x2, y2, path })
  })

  connectorLines.value = lines
}

// 监听布局变化
let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  nextTick(() => {
    updateConnectorLines()

    // 监听容器大小变化
    if (boardContainerRef.value) {
      resizeObserver = new ResizeObserver(() => {
        updateConnectorLines()
      })
      resizeObserver.observe(boardContainerRef.value)

      // 监听滚动
      boardContainerRef.value.addEventListener('scroll', updateConnectorLines)
    }
  })
})

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
  if (boardContainerRef.value) {
    boardContainerRef.value.removeEventListener('scroll', updateConnectorLines)
  }
})

// 监听数据变化
watch(
  () => props.stepList,
  () => {
    nextTick(() => {
      updateConnectorLines()
    })
  },
  { deep: true }
)

// 阶段编辑弹窗状态
const stepModalVisible = ref(false)
const stepModalMode = ref<'create' | 'edit'>('create')
const editingStep = ref<StepConfig | null>(null)
const stepInsertIndex = ref(0)

// 状态编辑弹窗状态
const statusModalVisible = ref(false)
const statusModalMode = ref<'create' | 'edit'>('create')
const editingStatus = ref<StatusConfig | null>(null)
/** 新建状态要插入到的阶段（未保存阶段 id 均为 null，必须用引用定位） */
const statusTargetStep = ref<StepConfig | null>(null)
const statusInsertIndex = ref(0)
/** 编辑状态打开时记录所在阶段与下标（多个 id 为 null 时不能仅靠 id 或异步确认后的引用） */
const statusEditContext = ref<{ step: StepConfig; index: number } | null>(null)

// 状态迁移弹窗状态
const migrationModalVisible = ref(false)
const migrationMode = ref<'single' | 'multiple'>('single')
const deletingStatuses = ref<StatusConfig[]>([])
const deletingStepId = ref<string | null>(null)
const { withLoading: withMigrationLoading } = useLoading()

// 获取所有可用的目标状态（排除被删除的状态）
const availableTargetStatuses = computed(() => {
  if (!props.stepList) return []
  const deletingStatusIds = new Set(deletingStatuses.value.map((s) => s.id))
  const allStatuses: StatusConfig[] = []
  props.stepList.forEach((step) => {
    step.statusList.forEach((status) => {
      if (!deletingStatusIds.has(status.id)) {
        allStatuses.push(status)
      }
    })
  })
  return allStatuses
})

// 阶段操作
function handleAddStep(insertIndex: number) {
  stepInsertIndex.value = insertIndex
  stepModalMode.value = 'create'
  editingStep.value = null
  stepModalVisible.value = true
}

function handleEditStep(step: StepConfig) {
  stepModalMode.value = 'edit'
  editingStep.value = step
  stepModalVisible.value = true
}

function handleDeleteStep(step: StepConfig) {
  if (!props.stepList || !props.valueStream) return

  // 如果是新建未保存的阶段（id 为 null），直接删除
  if (step.id === null) {
    const newStepList = props.stepList.filter((s) => s !== step)
    emit('update:stepList', newStepList)
    return
  }

  // 收集该阶段下的所有状态
  deletingStatuses.value = step.statusList || []
  deletingStepId.value = step.id

  // 如果阶段下没有状态，直接删除
  if (deletingStatuses.value.length === 0) {
    const newStepList = props.stepList.filter((s) => s.id !== step.id)
    emit('update:stepList', newStepList)
    return
  }

  // 弹出迁移模态框
  migrationMode.value = 'multiple'
  migrationModalVisible.value = true
}

function handleStepConfirm(step: StepConfig) {
  if (!props.stepList) return
  if (stepModalMode.value === 'create') {
    // 插入新阶段
    const newStepList = [...props.stepList]
    newStepList.splice(stepInsertIndex.value, 0, step)
    emit('update:stepList', newStepList)
  } else {
    // 更新阶段（id 为 null 的未保存阶段不能仅用 id 匹配，否则全部 null 会一起被替换）
    const orig = editingStep.value
    const newStepList = props.stepList.map((s) => {
      if (step.id != null && s.id === step.id) return step
      if (step.id == null && orig != null && s === orig) return step
      return s
    })
    emit('update:stepList', newStepList)
  }
}

// 状态操作
function handleAddStatus(step: StepConfig, insertIndex: number) {
  if (!props.stepList) return
  if (!props.stepList.includes(step)) return

  statusTargetStep.value = step
  statusEditContext.value = null
  statusInsertIndex.value = insertIndex
  statusModalMode.value = 'create'
  editingStatus.value = {
    id: null,
    name: '',
    workType: StatusWorkType.WORKING,
    sortOrder: 0,
  }
  statusModalVisible.value = true
}

function handleEditStatus(step: StepConfig, status: StatusConfig) {
  if (!props.stepList) return
  statusModalMode.value = 'edit'
  editingStatus.value = status
  const idx = step.statusList.indexOf(status)
  statusEditContext.value = idx >= 0 ? { step, index: idx } : null
  statusModalVisible.value = true
}

function handleDeleteStatus(step: StepConfig, status: StatusConfig) {
  if (!props.stepList) return

  // 如果是新建未保存的状态（id 为 null），直接从阶段中删除
  if (status.id === null) {
    const newStepList = props.stepList.map((s) => {
      if (s === step) {
        return {
          ...s,
          statusList: s.statusList.filter((st) => st !== status),
        }
      }
      return s
    })
    emit('update:stepList', newStepList)
    return
  }

  // 已保存的状态需要迁移卡片
  if (!props.valueStream) return

  // 设置要删除的状态
  deletingStatuses.value = [status]
  deletingStepId.value = null

  // 弹出迁移模态框
  migrationMode.value = 'single'
  migrationModalVisible.value = true
}

function handleStatusConfirm(status: StatusConfig) {
  if (!props.stepList) return
  if (statusModalMode.value === 'create') {
    const target = statusTargetStep.value
    if (!target) return
    // 插入新状态（目标阶段可能 id 为 null，用引用匹配）
    const newStepList = props.stepList.map((step) => {
      if (step === target) {
        const newStatusList = [...step.statusList]
        newStatusList.splice(statusInsertIndex.value, 0, status)
        return { ...step, statusList: newStatusList }
      }
      return step
    })
    emit('update:stepList', newStepList)
  } else {
    const ctx = statusEditContext.value
    const canUseContext =
      ctx != null &&
      ctx.index >= 0 &&
      props.stepList.some((st) => st === ctx.step)

    let newStepList: StepConfig[]
    if (canUseContext && ctx) {
      newStepList = props.stepList.map((st) => {
        if (st !== ctx.step) return st
        return {
          ...st,
          statusList: st.statusList.map((s, i) => (i === ctx.index ? status : s)),
        }
      })
    } else {
      const orig = editingStatus.value
      newStepList = props.stepList.map((st) => ({
        ...st,
        statusList: st.statusList.map((s) => {
          if (status.id != null && s.id === status.id) return status
          if (status.id == null && orig != null && s === orig) return status
          return s
        }),
      }))
    }

    statusEditContext.value = null
    emit('update:stepList', newStepList)
  }
}

// 处理状态插入按钮悬停
function handleStatusInsertHover(hovered: boolean) {
  if (hovered) {
    statusInsertHoverCount.value++
  } else {
    statusInsertHoverCount.value = Math.max(0, statusInsertHoverCount.value - 1)
  }
}

// 处理迁移确认
async function handleMigrationConfirm(migrationMap: Record<string, string>) {
  if (!props.valueStream) return
  const streamId = props.valueStream.id
  if (streamId == null) return

  await withMigrationLoading(async () => {
    try {
      if (migrationMode.value === 'single') {
        // 单个状态删除
        const status = deletingStatuses.value[0]
        const statusId = status?.id
        if (statusId == null) return
        const targetStatusId = migrationMap[statusId]
        if (targetStatusId == null) return

        await valueStreamApi.deleteStatusWithMigration(streamId, statusId, targetStatusId)

        // 从本地状态中删除该状态
        if (props.stepList) {
          const newStepList = props.stepList.map((step) => ({
            ...step,
            statusList: step.statusList.filter((s) => s.id !== statusId),
          }))
          emit('update:stepList', newStepList)
        }

        Message.success(t('admin.cardType.valueStream.statusMigration.migrationSuccess'))
      } else {
        // 阶段删除
        const stepId = deletingStepId.value
        if (stepId == null) return
        await valueStreamApi.deleteStepWithMigration(streamId, stepId, migrationMap)

        // 从本地状态中删除该阶段
        if (props.stepList) {
          const newStepList = props.stepList.filter((s) => s.id !== stepId)
          emit('update:stepList', newStepList)
        }

        Message.success(t('admin.cardType.valueStream.statusMigration.migrationSuccess'))
      }
    } catch (error: any) {
      console.error('Migration failed:', error)
      Message.error(
        error.message || t('admin.cardType.valueStream.statusMigration.migrationFailed'),
      )
    }
  })
}
</script>

<template>
  <div class="value-stream-board">
    <!-- 阶段列表 -->
    <div ref="boardContainerRef" class="board-container">
      <!-- 连线覆盖层 -->
      <svg class="connector-overlay">
        <path
          v-for="(line, index) in connectorLines"
          :key="index"
          :d="line.path"
          class="connector-line"
          :class="{ hidden: hoveredInsertIndex !== null || statusInsertHoverCount > 0 }"
        />
      </svg>

      <!-- 首位插入按钮 -->
      <div
        class="insert-step-wrapper"
        @mouseenter="hoveredInsertIndex = 0"
        @mouseleave="hoveredInsertIndex = null"
      >
        <a-button type="text" class="insert-step-btn" @click="handleAddStep(0)">
          {{ t('admin.cardType.valueStream.addStep') }}
        </a-button>
      </div>

      <template v-for="(step, index) in displayStepList" :key="step.id ?? `new-step-${index}`">
        <StepColumn
          :step="step"
          @edit-step="handleEditStep"
          @delete-step="handleDeleteStep"
          @add-status="handleAddStatus"
          @edit-status="handleEditStatus"
          @delete-status="handleDeleteStatus"
          @status-insert-hover="handleStatusInsertHover"
        />

        <!-- 每个阶段后面的插入按钮 -->
        <div
          class="insert-step-wrapper"
          @mouseenter="hoveredInsertIndex = index + 1"
          @mouseleave="hoveredInsertIndex = null"
        >
          <a-button type="text" class="insert-step-btn" @click="handleAddStep(index + 1)">
            {{ t('admin.cardType.valueStream.addStep') }}
          </a-button>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-if="displayStepList.length === 0" class="empty-board">
        <a-empty :description="t('admin.cardType.valueStream.emptyStepList')">
          <template #image>
            <IconApps style="font-size: 48px; color: var(--color-text-4)" />
          </template>
        </a-empty>
      </div>
    </div>

    <!-- 阶段编辑弹窗 -->
    <StepEditModal
      v-model:visible="stepModalVisible"
      :step="editingStep"
      :mode="stepModalMode"
      @confirm="handleStepConfirm"
    />

    <!-- 状态编辑弹窗 -->
    <StatusEditModal
      v-model:visible="statusModalVisible"
      :status="editingStatus"
      :mode="statusModalMode"
      @confirm="handleStatusConfirm"
    />

    <!-- 状态迁移弹窗 -->
    <StatusMigrationModal
      v-if="valueStream"
      v-model:visible="migrationModalVisible"
      :mode="migrationMode"
      :deleting-statuses="deletingStatuses"
      :available-target-statuses="availableTargetStatuses"
      :value-stream="valueStream"
      @confirm="handleMigrationConfirm"
    />
  </div>
</template>

<style scoped>
.value-stream-board {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.board-container {
  display: flex;
  gap: 16px;
  padding: 20px 4px;
  overflow-x: auto;
  height: 100%;
  align-items: flex-start;
  position: relative;
  border-radius: 12px;
}

/* 连线覆盖层 */
.connector-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1000;
  overflow: visible;
}

.connector-line {
  fill: none;
  stroke: var(--color-text-3);
  stroke-width: 1.5;
  stroke-dasharray: 4 6;
  stroke-linejoin: round;
  stroke-linecap: round;
  opacity: 0.4;
  animation: dash-flow 1.2s linear infinite;
  transition: opacity 0.2s;
}

.connector-line.hidden {
  opacity: 0;
}

@keyframes dash-flow {
  to {
    stroke-dashoffset: -10;
  }
}

.insert-step-wrapper {
  display: flex;
  align-items: stretch;
  flex-shrink: 0;
  opacity: 0.5;
  width: 4px;
  position: relative;
  transition: all 0.2s;
  cursor: pointer;
}

/* 默认显示细线提示 */
.insert-step-wrapper::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 8px;
  bottom: 8px;
  width: 1px;
  background: var(--color-border-3);
  transform: translateX(-50%);
  transition: opacity 0.2s;
  opacity: 0;
}

/* 悬停展开 */
.insert-step-wrapper:hover {
  opacity: 1;
  width: 32px;
}

.insert-step-wrapper:hover::before {
  opacity: 0;
}

.insert-step-btn {
  width: 100% !important;
  height: auto !important;
  min-height: 160px;
  border-radius: 6px !important;
  border: 1px solid #e0e7ff !important;
  background-color: #edf2ff !important;
  color: var(--color-text-2) !important;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: opacity 0.2s;
  opacity: 0;
  font-size: 12px;
  writing-mode: vertical-rl;
  letter-spacing: 2px;
  padding: 12px 0 !important;
}

.insert-step-wrapper:hover .insert-step-btn {
  opacity: 1;
}

.insert-step-btn:hover {
  background-color: #e0e7ff !important;
  border-color: #c7d5ff !important;
  color: var(--color-primary) !important;
}

.empty-board {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  min-height: 200px;
  background: white;
  border-radius: 8px;
  border: 1px dashed var(--color-border-3);
  margin: 20px;
}
</style>
