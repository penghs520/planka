<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconDelete } from '@arco-design/web-vue/es/icon'
import {
  StepStatusKindConfig,
  type StepConfig,
  type StatusConfig,
} from '@/types/value-stream'
import StatusCard from './StatusCard.vue'

const { t } = useI18n()

const props = defineProps<{
  step: StepConfig
}>()

const emit = defineEmits<{
  'edit-step': [step: StepConfig]
  'delete-step': [step: StepConfig]
  'add-status': [stepId: string, insertIndex: number]
  'edit-status': [status: StatusConfig]
  'delete-status': [stepId: string, status: StatusConfig]
  'status-insert-hover': [hovered: boolean]
}>()

const kindConfig = computed(() => StepStatusKindConfig[props.step.kind])

// 头部背景渐变样式
const headerStyle = computed(() => {
  const color = kindConfig.value.defaultStatusColor
  return {
    background: `linear-gradient(180deg, ${color}18 0%, ${color}08 100%)`,
  }
})

// 阶段列左边框样式
const columnStyle = computed(() => {
  const color = kindConfig.value.defaultStatusColor
  return {
    borderLeftColor: color,
  }
})

// 插入按钮样式 - 使用阶段主题色
const insertBtnStyle = computed(() => {
  const color = kindConfig.value.defaultStatusColor
  return {
    '--insert-btn-bg': `${color}15`,
    '--insert-btn-bg-hover': `${color}25`,
    '--insert-btn-color': color,
    '--insert-btn-border': `${color}20`,
  }
})

// 状态列表
const displayStatusList = computed(() => {
  return props.step.statusList.map((status, index, arr) => ({
    status,
    isLast: index === arr.length - 1,
  }))
})

function handleEditStep() {
  emit('edit-step', props.step)
}

function handleDeleteStep() {
  emit('delete-step', props.step)
}

function handleAddStatus(insertIndex: number) {
  emit('add-status', props.step.id, insertIndex)
}

function handleEditStatus(status: StatusConfig) {
  emit('edit-status', status)
}

function handleDeleteStatus(status: StatusConfig) {
  emit('delete-status', props.step.id, status)
}
</script>

<template>
  <div class="step-column" :style="columnStyle">
    <!-- 阶段头部 -->
    <div
      class="step-header"
      :style="headerStyle"
      @dblclick="handleEditStep"
    >
      <div class="step-title">
        <a-tag :color="kindConfig.color" size="small">{{ kindConfig.label }}</a-tag>
        <span class="step-name">{{ step.name }}</span>
        <span class="status-count">({{ step.statusList.length }})</span>
      </div>
      <div class="step-actions">
        <a-popconfirm content="删除阶段将同时删除其下所有状态，确定要删除吗？" @ok="handleDeleteStep">
          <span class="action-icon action-icon-danger">
            <IconDelete />
          </span>
        </a-popconfirm>
      </div>
    </div>

    <!-- 状态列表 -->
    <div class="status-list" :style="insertBtnStyle">
      <!-- 顶部插入按钮 -->
      <div
        v-if="displayStatusList.length > 0"
        class="insert-btn-wrapper"
        @mouseenter="emit('status-insert-hover', true)"
        @mouseleave="emit('status-insert-hover', false)"
      >
        <a-button
          type="text"
          size="mini"
          class="insert-btn"
          @click="handleAddStatus(0)"
        >
          {{ t('admin.cardType.valueStream.addStatus') }}
        </a-button>
      </div>

      <template v-for="(item, index) in displayStatusList" :key="item.status.id">
        <div class="status-wrapper">
          <StatusCard
            :status="item.status"
            @edit="handleEditStatus"
            @delete="handleDeleteStatus"
          />
          <!-- 状态之间的垂直连接线 -->
          <div v-if="!item.isLast" class="status-connector vertical">
            <div class="connector-line" />
          </div>
        </div>

        <!-- 每个状态后面的插入按钮 -->
        <div
          class="insert-btn-wrapper"
          @mouseenter="emit('status-insert-hover', true)"
          @mouseleave="emit('status-insert-hover', false)"
        >
          <a-button
            type="text"
            size="mini"
            class="insert-btn"
            @click="handleAddStatus(index + 1)"
          >
            {{ t('admin.cardType.valueStream.addStatus') }}
          </a-button>
        </div>
      </template>

      <!-- 空状态提示 -->
      <div v-if="displayStatusList.length === 0" class="empty-status">
        <span>{{ t('admin.cardType.valueStream.emptyStatusList') }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.step-column {
  display: flex;
  flex-direction: column;
  min-width: 220px;
  width: fit-content;
  flex-shrink: 0;
  background-color: white;
  border-radius: 8px;
  border: 1px solid var(--color-border-2);
  border-left-width: 3px;
  overflow: visible;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.step-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px;
  border-bottom: 1px solid var(--color-border-2);
  border-radius: 6px 8px 0 0;
  user-select: none;
  cursor: pointer;
}

.step-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-name {
  font-weight: 500;
  color: var(--color-text-1);
  white-space: nowrap;
}

.status-count {
  color: var(--color-text-2);
  font-size: 12px;
  flex-shrink: 0;
  font-weight: 500;
}

.step-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.2s;
}

.step-header:hover .step-actions {
  opacity: 1;
}

.action-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--color-text-2);
  transition: all 0.2s;
}

.action-icon:hover {
  color: rgb(var(--primary-6));
  background-color: var(--color-fill-2);
}

.action-icon-danger:hover {
  color: rgb(var(--danger-6));
  background-color: var(--color-danger-light-1);
}

.status-list {
  display: flex;
  flex-direction: column;
  padding: 16px 12px;
  gap: 12px;
  min-height: 100px;
}

/* 状态卡片容器 */
.status-wrapper {
  position: relative;
}

/* 插入按钮容器 */
.insert-btn-wrapper {
  display: flex;
  justify-content: center;
  padding: 0;
  opacity: 0;
  height: 2px;
  overflow: hidden;
  transition: opacity 0.2s, height 0.2s, padding 0.2s;
  position: relative;
}

/* 默认显示细线提示 */
.insert-btn-wrapper::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 1px;
  background: var(--color-border-3);
  transform: translateX(-50%);
  transition: opacity 0.2s;
}

.insert-btn-wrapper:hover {
  opacity: 1;
  height: auto;
  padding: 4px 0;
}

.insert-btn-wrapper:hover::before {
  opacity: 0;
}

/* 当插入按钮区域 hover 时，隐藏前一个状态的连接线 */
.status-wrapper:has(+ .insert-btn-wrapper:hover) .status-connector.vertical {
  opacity: 0;
}

.insert-btn {
  width: 100%;
  height: 28px;
  border: 1px solid var(--color-border-2);
  border-radius: 6px !important;
  background-color: var(--insert-btn-bg, var(--color-fill-2));
  color: var(--insert-btn-color, var(--color-text-3));
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
}

.insert-btn:hover {
  background-color: var(--insert-btn-bg-hover, var(--color-fill-3)) !important;
  border-color: var(--insert-btn-color, var(--color-text-3)) !important;
  color: var(--insert-btn-color, var(--color-text-3)) !important;
}

.empty-status {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 0;
  color: var(--color-text-3);
  font-size: 13px;
  background: var(--color-fill-2);
  border-radius: 6px;
  border: 1px dashed var(--color-border-2);
}

/* 连接线容器 */
.status-connector {
  position: absolute;
  pointer-events: none;
  z-index: 100;
}

/* 垂直连接线 */
.status-connector.vertical {
  left: 50%;
  top: calc(100% + 4px);
  transform: translateX(-50%);
  width: 1.5px;
  height: 20px;
}

.status-connector.vertical .connector-line {
  width: 100%;
  height: 100%;
  background: repeating-linear-gradient(
    to bottom,
    var(--color-text-3) 0px,
    var(--color-text-3) 4px,
    transparent 4px,
    transparent 10px
  );
  opacity: 0.4;
  animation: flow-down 1.2s linear infinite;
}

/* 流动动画 - 向下 */
@keyframes flow-down {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 0 10px;
  }
}
</style>
