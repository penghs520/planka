<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconDelete } from '@arco-design/web-vue/es/icon'
import { StatusWorkType, type StatusConfig } from '@/types/value-stream'

const { t } = useI18n()

const props = defineProps<{
  status: StatusConfig
}>()

const emit = defineEmits<{
  edit: [status: StatusConfig]
  delete: [status: StatusConfig]
}>()

// 根据 workType 获取颜色
const statusColor = computed(() => {
  const workType = props.status.workType || StatusWorkType.WORKING
  // 等待用黄色，工作中用蓝色
  return workType === StatusWorkType.WAITING ? '#ff7d00' : '#165dff'
})

// 渐变背景样式
const gradientStyle = computed(() => {
  const color = statusColor.value
  return {
    background: `linear-gradient(135deg, ${color}15 0%, ${color}08 100%)`,
    borderColor: `${color}40`,
  }
})

// 卡片样式类
const cardClass = computed(() => {
  return props.status.id === null ? 'status-card status-card-new' : 'status-card'
})

function handleEdit() {
  emit('edit', props.status)
}

function handleDelete() {
  emit('delete', props.status)
}
</script>

<template>
  <div :class="cardClass" :style="gradientStyle" @dblclick="handleEdit">
    <div class="status-indicator" :style="{ backgroundColor: statusColor }" />
    <div class="status-content">
      <div class="status-name" :title="status.name">{{ status.name }}</div>
    </div>
    <div class="status-actions">
      <a-popconfirm :content="t('admin.cardType.valueStream.confirmDeleteStatus')" @ok="handleDelete">
        <span class="action-icon action-icon-danger">
          <IconDelete />
        </span>
      </a-popconfirm>
    </div>
  </div>
</template>

<style scoped>
.status-card {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  min-height: 52px;
  box-sizing: border-box;
  border-radius: 8px;
  border-width: 1px;
  border-style: solid;
  transition: all 0.2s;
  cursor: pointer;
  user-select: none;
}

.status-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.status-card-new {
  border-style: dashed;
}

.status-indicator {
  width: 3px;
  height: 32px;
  border-radius: 2px;
  flex-shrink: 0;
  margin-right: 12px;
}

.status-content {
  flex: 1;
  min-width: 0;
}

.status-name {
  font-size: 13px;
  color: var(--color-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-work-type {
  font-size: 12px;
  margin-top: 4px;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
}

.status-actions {
  display: flex;
  gap: 8px;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.2s;
  margin-left: 4px;
}

.status-card:hover .status-actions {
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
  color: var(--color-text-3);
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
</style>
