<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconDelete, IconFullscreen, IconFullscreenExit } from '@arco-design/web-vue/es/icon'
import type { CardCreatePageTemplateDefinition, CreatePageFieldItemConfig } from '@/types/card-create-page-template'
import type { FieldConfig } from '@/types/card-type'
import { createEmptyCreatePageFieldItem } from '@/types/card-create-page-template'
import { VueDraggable } from 'vue-draggable-plus'

const props = defineProps<{
  modelValue: CardCreatePageTemplateDefinition
  fields: FieldConfig[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: CardCreatePageTemplateDefinition): void
  (e: 'change'): void
}>()

const { locale } = useI18n()

// 获取字段名称，标题字段根据语言环境显示
function getFieldName(fieldId: string): string {
  // 标题字段特殊处理
  if (fieldId === '$title') {
    return locale.value === 'zh-CN' ? '标题' : 'Title'
  }
  const field = props.fields.find(f => f.fieldId === fieldId)
  return field ? field.name : fieldId
}

// 判断字段是否必填
function isFieldRequired(fieldId: string): boolean {
  // 标题字段始终必填
  if (fieldId === '$title') return true
  const field = props.fields.find(f => f.fieldId === fieldId)
  return field?.required === true
}

// 拖拽状态
const isDragOver = ref(false)

function handleDragEnter(event: DragEvent): void {
  event.preventDefault()
  isDragOver.value = true
}

function handleDragLeave(event: DragEvent): void {
  const relatedTarget = event.relatedTarget as HTMLElement | null
  const currentTarget = event.currentTarget as HTMLElement
  if (!relatedTarget || !currentTarget.contains(relatedTarget)) {
    isDragOver.value = false
  }
}

function handleDragOver(event: DragEvent): void {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy'
  }
}

function handleDrop(event: DragEvent): void {
  event.preventDefault()
  event.stopPropagation()
  isDragOver.value = false

  const data = event.dataTransfer?.getData('application/json')
  if (!data) return

  try {
    const payload = JSON.parse(data)
    if (payload.type === 'field' && payload.field) {
      const field = payload.field as FieldConfig
      const existing = props.modelValue.fieldItems.find(item => item.fieldId === field.fieldId)
      if (existing) return

      const newItem = createEmptyCreatePageFieldItem(field.fieldId)
      props.modelValue.fieldItems.push(newItem)
      emit('change')
    }
  } catch (e) {
    console.error('Drop parse error:', e)
  }
}

function handleGlobalDragEnd(): void {
  isDragOver.value = false
}

onMounted(() => {
  document.addEventListener('dragend', handleGlobalDragEnd)
})

onUnmounted(() => {
  document.removeEventListener('dragend', handleGlobalDragEnd)
})

function handleRemoveField(index: number): void {
  const item = props.modelValue.fieldItems[index]
  // 标题字段不允许删除
  if (item?.fieldId === '$title') return
  props.modelValue.fieldItems.splice(index, 1)
  emit('change')
}

function handleToggleFullWidth(item: CreatePageFieldItemConfig): void {
  if (item.widthPercent === 100) {
    item.widthPercent = 50
  } else {
    item.widthPercent = 100
  }
  item.startNewRow = false
  emit('change')
}

function getItemStyle(item: CreatePageFieldItemConfig): { gridColumn: string } {
  if (item.widthPercent === 100) {
    return { gridColumn: '1 / -1' }
  }
  if (item.startNewRow) {
    return { gridColumn: '1 / span 1' }
  }
  return { gridColumn: 'auto / span 1' }
}

function onSortChange(): void {
  emit('change')
}

const resizingItem = ref<{ item: CreatePageFieldItemConfig; startX: number; originalWidth: number } | null>(null)

function onResizeStart(event: MouseEvent, item: CreatePageFieldItemConfig): void {
  event.preventDefault()
  resizingItem.value = {
    item,
    startX: event.clientX,
    originalWidth: item.widthPercent
  }
  window.addEventListener('mousemove', onResizing)
  window.addEventListener('mouseup', onResizeEnd)
}

function onResizing(event: MouseEvent): void {
  if (!resizingItem.value) return
  const dx = event.clientX - resizingItem.value.startX

  if (dx > 50) {
    resizingItem.value.item.widthPercent = 100
    resizingItem.value.item.startNewRow = false
  } else if (dx < -50) {
    resizingItem.value.item.widthPercent = 50
  }
}

function onResizeEnd(): void {
  resizingItem.value = null
  window.removeEventListener('mousemove', onResizing)
  window.removeEventListener('mouseup', onResizeEnd)
  emit('change')
}
</script>

<template>
  <div 
    class="create-page-canvas"
    :class="{ 'drag-over': isDragOver }"
    @dragenter="handleDragEnter"
    @dragleave="handleDragLeave"
    @dragover="handleDragOver"
    @drop="handleDrop"
  >

    <!-- 字段列表 -->
    <div class="fields-container">
      <VueDraggable
        v-model="modelValue.fieldItems"
        class="field-list"
        group="fields"
        :animation="150"
        ghost-class="ghost-card"
        @update="onSortChange"
        @add="onSortChange"
        @remove="onSortChange"
      >
        <div 
          v-for="(item, index) in modelValue.fieldItems"
          :key="item.fieldId"
          class="field-item-row"
          :style="getItemStyle(item)"
        > 
          <div class="field-content">
            <div class="field-label">
              <!-- 必填字段显示星号 -->
              <span v-if="isFieldRequired(item.fieldId)" class="field-required">*</span>
              <span class="field-name-text">{{ getFieldName(item.fieldId) }}</span>
              
              <div class="field-actions">
                <a-tooltip :content="item.widthPercent === 100 ? '恢复默认宽度' : '独占一行'">
                  <a-button 
                    class="action-btn" 
                    :class="{ 'is-active': item.widthPercent === 100 }"
                    type="text" 
                    size="mini" 
                    @click.stop="handleToggleFullWidth(item)"
                  >
                    <template #icon>
                      <IconFullscreenExit v-if="item.widthPercent === 100" />
                      <IconFullscreen v-else />
                    </template>
                  </a-button>
                </a-tooltip>
                <!-- 标题字段不允许删除 -->
                <a-button 
                  v-if="item.fieldId !== '$title'"
                  class="action-btn delete-btn" 
                  type="text" 
                  size="mini" 
                  status="danger" 
                  @click.stop="handleRemoveField(index)"
                >
                  <template #icon><IconDelete /></template>
                </a-button>
              </div>
            </div>
            <div class="field-mock-input">请输入...</div>
          </div>
          
          <!-- Resize Handle -->
          <div class="resize-handle" @mousedown.stop="onResizeStart($event, item)"></div>
        </div>
        
        <div v-if="modelValue.fieldItems.length === 0" class="empty-section-placeholder">
          拖拽字段到此处
        </div>
      </VueDraggable>
    </div>
  </div>
</template>

<style scoped lang="scss">
.create-page-canvas {
  padding: 16px;
  height: 100%;
  overflow-y: auto;
  background-color: var(--color-fill-1);
  transition: box-shadow 0.2s;

  &.drag-over {
    box-shadow: inset 0 0 0 2px rgba(var(--primary-6), 0.3);
  }
}

.fields-container {
  background: var(--color-bg-2);
  border-radius: 4px;
  border: 1px solid var(--color-border);
  min-height: 100px;
}

.field-list {
  padding: 16px; 
  min-height: 100px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px; 
}

.field-item-row {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 6px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 4px;
  cursor: move;
  position: relative; /* For resize handle */
  
  &:hover {
    background: var(--color-fill-2);
    
    .action-btn {
      opacity: 1;
    }
    .resize-handle {
        opacity: 1;
    }
  }
}

.ghost-card {
  background: var(--color-fill-3);
  opacity: 0.5;
  border: 1px dashed var(--color-primary-light-4);
}

.field-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
}

.field-label {
  font-size: 12px;
  color: var(--color-text-2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.field-name-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-required {
  color: rgb(var(--danger-6));
  margin-right: 2px;
}

.field-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.action-btn {
  opacity: 0;
  transition: opacity 0.2s, color 0.2s;
  padding: 0 4px;
  height: 20px;
  
  &.is-active {
    color: rgb(var(--primary-6));
  }
}



.field-mock-input {
  height: 28px;
  line-height: 28px;
  background: var(--color-bg-1);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0 8px;
  font-size: 12px;
  color: var(--color-text-4);
}

.resize-handle {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  cursor: col-resize;
  opacity: 0;
  transition: opacity 0.2s;
  
  &:hover {
    background: var(--color-primary-light-4);
    opacity: 1;
  }
  
  &::after {
      content: '';
      position: absolute;
      top: 50%;
      left: 2px;
      transform: translateY(-50%);
      height: 12px;
      width: 2px;
      background-color: var(--color-text-4);
      border-radius: 2px;
  }
}

.empty-section-placeholder {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  color: var(--color-text-4);
  font-size: 12px;
  border: 1px dashed var(--color-border-3);
  border-radius: 4px;
}
</style>
