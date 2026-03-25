<script setup lang="ts">
import { ref, nextTick, inject } from 'vue'
import { IconDelete, IconDragDotVertical } from '@arco-design/web-vue/es/icon'
import FieldMockPreview from './FieldMockPreview.vue'
import type {
  SectionConfig,
  FieldItemConfig,
  SelectedItem,
} from '@/types/card-detail-template'
import type { FieldConfig } from '@/types/card-type'

const props = defineProps<{
  section: SectionConfig
  selectedItem: SelectedItem | null
  // Global drag states
  draggingSectionId: string | null
  dropTargetSection: { sectionId: string; position: 'before' | 'after' } | null
  dragOverSectionId: string | null
  draggingFieldItem: { sectionId: string; fieldConfigId: string } | null
  dropTargetField: { sectionId: string; fieldConfigId: string; position: 'before' | 'after' } | null
  dropBelowFieldId: string | null
  resizingField: { sectionId: string; fieldConfigId: string } | null
  // Style config
  fieldRowGap: string
}>()

const emit = defineEmits<{
  (e: 'update:name', name: string): void
  (e: 'delete'): void
  (e: 'select', item: SelectedItem | null): void
  (e: 'section-dragstart', event: DragEvent): void
  (e: 'section-dragover', event: DragEvent): void
  (e: 'section-drop', event: DragEvent): void
  (e: 'section-dragenter', event: DragEvent): void
  (e: 'section-dragleave'): void
  // Field events
  (e: 'field-dragstart', fieldId: string, event: DragEvent): void
  (e: 'field-dragover', fieldId: string, event: DragEvent): void
  (e: 'field-dragleave', event: DragEvent): void
  (e: 'field-drop', fieldId: string, event: DragEvent): void
  (e: 'field-drop-below-over', fieldId: string, event: DragEvent): void
  (e: 'field-drop-below-leave'): void
  (e: 'field-drop-below', fieldId: string, event: DragEvent): void
  (e: 'field-dragend'): void
  (e: 'delete-field', fieldId: string): void
  (e: 'resize-start', fieldId: string, event: MouseEvent): void
}>()

// 注入字段列表 (Optional, if needed for getFieldInfo, but FieldMockPreview handles display now)
// However, 'Field Label' still uses getFieldInfo.
const fields = inject<{ value: FieldConfig[] }>('fields', { value: [] })

function getFieldInfo(fieldConfigId: string): FieldConfig | undefined {
  return fields.value.find((f) => f.fieldId === fieldConfigId)
}

// Editing Section Name
const editingName = ref(false)
const tempName = ref('')
const nameInputRef = ref<{ focus: () => void } | null>(null)

function startEditName() {
  editingName.value = true
  tempName.value = props.section.name || ''
  nextTick(() => {
    nameInputRef.value?.focus()
  })
}

function finishEditName() {
  if (editingName.value) {
    emit('update:name', tempName.value)
    editingName.value = false
  }
}

function cancelEditName() {
  editingName.value = false
}

// Helpers for selection check
function isSectionSelected() {
  return props.selectedItem?.type === 'section' && props.selectedItem.id === props.section.sectionId
}

function isFieldSelected(item: FieldItemConfig) {
  return (
    props.selectedItem?.type === 'field' &&
    props.selectedItem.id === item.fieldConfigId &&
    props.selectedItem.sectionId === props.section.sectionId
  )
}

// Ref for section content (for resizing calculation in parent)
// Parent needs access to this element?
// In LayoutCanvas, `setSectionContentRef` is used.
// We can expose the element via expose() or just use a ref and emit it?
// LayoutCanvas uses `sectionContentRefs` Map.
// We can simply emit 'mounted' with ref? No, standard way is template ref.
// But resizing logic is in parent. Parent needs to know width of THIS section content.
// Maybe we can pass the ref up via an event or use a callback prop?
// Or better: Use a simple ref here, and expose it.
const sectionContentRef = ref<HTMLElement | null>(null)

defineExpose({
  sectionContentRef
})

</script>

<template>
  <div
    class="section-card"
    :class="{ 
      selected: isSectionSelected(), 
      'drag-over': dragOverSectionId === section.sectionId,
      'dragging': draggingSectionId === section.sectionId,
      'drop-before': dropTargetSection?.sectionId === section.sectionId && dropTargetSection?.position === 'before',
      'drop-after': dropTargetSection?.sectionId === section.sectionId && dropTargetSection?.position === 'after'
    }"
    @click="emit('select', { type: 'section', id: section.sectionId, tabId: '' })" 
    @dragenter="emit('section-dragenter', $event)"
    @dragleave="emit('section-dragleave')"
    @dragover="emit('section-dragover', $event)"
    @drop="emit('section-drop', $event)"
  >
    <!-- 区域标题 -->
    <div class="section-header">
      <div class="section-title-area">
        <div
          class="section-drag-handle"
          tabindex="0"
          draggable="true"
          aria-label="拖拽调整区域顺序"
          @click.stop
          @dragstart="emit('section-dragstart', $event)"
        >
          <IconDragDotVertical />
        </div>
        <div class="section-title-wrapper">
          <a-input
            v-if="editingName"
            ref="nameInputRef"
            v-model="tempName"
            size="small"
            class="section-name-input"
            @blur="finishEditName"
            @keyup.enter="finishEditName"
            @keyup.escape="cancelEditName"
            @click.stop
          />
          <span v-else class="section-title" @click.stop="startEditName">
            {{ section.name || '未命名区域' }}
          </span>
        </div>
      </div>
      <div class="section-actions">
        <a-button type="text" size="mini" status="danger" @click.stop="emit('delete')">
          <template #icon><IconDelete /></template>
        </a-button>
      </div>
    </div>

    <!-- 字段列表 -->
    <div
      ref="sectionContentRef"
      class="section-content"
    >
      <div v-if="section.fieldItems.length > 0" class="field-flex" :style="{ rowGap: fieldRowGap }">
        <template v-for="item in section.fieldItems" :key="item.fieldConfigId">
          <!-- 换行分隔符 -->
          <div v-if="item.startNewRow" class="row-break"></div>
          <div
            class="field-item"
            :class="{
              selected: isFieldSelected(item),
              resizing: resizingField?.fieldConfigId === item.fieldConfigId,
              dragging: draggingFieldItem?.fieldConfigId === item.fieldConfigId,
              'drop-before': dropTargetField?.fieldConfigId === item.fieldConfigId && dropTargetField?.position === 'before',
              'drop-after': dropTargetField?.fieldConfigId === item.fieldConfigId && dropTargetField?.position === 'after',
            }"
            :style="{ width: `${item.widthPercent}%` }"
            draggable="true"
            @click.stop="emit('select', { type: 'field', id: item.fieldConfigId, sectionId: section.sectionId, tabId: '' })"
            @dragstart="emit('field-dragstart', item.fieldConfigId, $event)"
            @dragover="emit('field-dragover', item.fieldConfigId, $event)"
            @dragleave="emit('field-dragleave', $event)"
            @drop="emit('field-drop', item.fieldConfigId, $event)"
            @dragend="emit('field-dragend')"
          >
            <div class="field-label">
              <span class="label-text">{{ item.customLabel || getFieldInfo(item.fieldConfigId)?.name || '字段' }}</span>
            </div>
            <div class="field-control">
              <FieldMockPreview :field-config-id="item.fieldConfigId" />
            </div>
            <a-button
              type="text"
              size="mini"
              class="field-delete"
              @click.stop="emit('delete-field', item.fieldConfigId)"
            >
              <template #icon><IconDelete /></template>
            </a-button>
            <!-- 宽度拖拽手柄 -->
            <div
              class="resize-handle"
              @mousedown.stop="emit('resize-start', item.fieldConfigId, $event)"
            >
              <div class="resize-bar"></div>
            </div>
            <!-- 宽度提示 -->
            <div v-if="resizingField?.fieldConfigId === item.fieldConfigId" class="width-tooltip">
              {{ item.widthPercent }}%
            </div>
            <!-- 拖到字段下方的放置区 -->
            <div
              v-if="draggingFieldItem && draggingFieldItem.fieldConfigId !== item.fieldConfigId"
              class="drop-below-zone"
              :class="{ active: dropBelowFieldId === item.fieldConfigId }"
              @dragover="emit('field-drop-below-over', item.fieldConfigId, $event)"
              @dragleave="emit('field-drop-below-leave')"
              @drop="emit('field-drop-below', item.fieldConfigId, $event)"
            >
              <div class="drop-below-line"></div>
            </div>
          </div>
        </template>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-section">
        <span>拖拽左侧字段到此处</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.section-card {
  background: #fff;
  border-radius: 8px;
  border: none;
  /* 允许区域拖拽手柄伸入 .detail-body 左侧留白，避免裁切 */
  overflow: visible;
  transition: all 0.2s;

  &:hover {
    .section-actions {
      opacity: 1;
    }
  }

  &.selected {
    box-shadow: 0 0 0 1px rgb(var(--primary-6));
  }

  &.drag-over {
    border: 2px dashed rgb(var(--primary-6));
    background: rgb(var(--primary-1));
  }

  &.dragging {
    opacity: 0.5;
    outline: 1px dashed var(--color-border-3);
    outline-offset: -1px;
  }

  &.drop-before {
    position: relative;
    &::before {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      top: 0;
      height: 4px;
      background: rgb(var(--primary-6));
      z-index: 10;
      border-radius: 2px;
    }
  }

  &.drop-after {
    position: relative;
    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: 0;
      height: 4px;
      background: rgb(var(--primary-6));
      z-index: 10;
      border-radius: 2px;
    }
  }
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px 2px 0;
  background: var(--color-bg-1);
  border-bottom: none;
  border-radius: 6px 6px 0 0;
}

.section-title-area {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  gap: 2px;
}

/* 手柄叠在 body 左留白内，不推动区域名称，与详情标题左缘对齐 */
.section-drag-handle {
  position: absolute;
  left: -14px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 3px;
  color: var(--color-text-3);
  cursor: grab;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.18s ease;
  user-select: none;

  &:hover {
    color: var(--color-text-2);
    background: var(--color-fill-2);
  }

  &:active {
    cursor: grabbing;
  }

  :deep(svg) {
    font-size: 12px;
  }
}

.section-header:hover .section-drag-handle,
.section-card.dragging .section-drag-handle,
.section-drag-handle:focus-visible {
  opacity: 1;
  pointer-events: auto;
}

.section-title-wrapper {
  flex: 1;
  min-width: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
  cursor: pointer;
  padding: 2px 8px 2px 0;
  margin: -2px 0;
  border-radius: 4px;
  transition: background 0.2s;

  &:hover {
    background: transparent;
  }
}

.section-name-input {
  width: 200px;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.section-content {
  padding: 0 4px 6px 0;
}

// 字段 flex 布局（无额外 padding；行距由内联 rowGap / Tab 字段行间距控制）
.field-flex {
  display: flex;
  flex-wrap: wrap;
  margin: 0;
  padding: 0;
  column-gap: 0;
  row-gap: 8px; /* 无内联 rowGap 时的兜底，通常由 FieldRowSpacingConfig 覆盖 */
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 8px 2px 0;
  border-radius: 4px;
  border: 1px solid transparent;
  position: relative;
  cursor: grab;
  transition: background 0.2s, border-color 0.2s;
  box-sizing: border-box;
  min-width: 25%;

  &:hover {
    background: transparent;

    .field-delete {
      opacity: 1;
    }

    .resize-handle {
      opacity: 1;
    }
  }

  &.selected {
    background: rgb(var(--primary-1));
    border-color: rgb(var(--primary-6));

    .resize-handle {
      opacity: 1;
    }
  }

  &.resizing {
    background: rgb(var(--primary-1));
    user-select: none;
  }

  &.dragging {
    opacity: 0.5;
    cursor: grabbing;
  }

  &.drop-before {
    &::before {
      content: '';
      position: absolute;
      left: -2px;
      top: 4px;
      bottom: 4px;
      width: 3px;
      background: rgb(var(--primary-6));
      border-radius: 2px;
    }
  }

  &.drop-after {
    &::after {
      content: '';
      position: absolute;
      right: -2px;
      top: 4px;
      bottom: 4px;
      width: 3px;
      background: rgb(var(--primary-6));
      border-radius: 2px;
    }
  }
}

// 换行分隔符 - 强制在 flex 容器中换行
.row-break {
  flex-basis: 100%;
  width: 100%;
  height: 0;
}

.resize-handle {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 10px;
  cursor: col-resize;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;

  .resize-bar {
    width: 2px;
    height: 16px;
    background: var(--color-border-3);
    border-radius: 1px;
  }

  &:hover {
    .resize-bar {
      background: rgb(var(--primary-6));
    }
  }
}

.width-tooltip {
  position: absolute;
  top: -24px;
  right: 0;
  background: var(--color-bg-5);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--color-text-1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  pointer-events: none;
  z-index: 20;
}

.drop-below-zone {
  position: absolute;
  left: 0;
  right: 12px; // Match padding-right of field-item
  bottom: 0;
  height: 10px;
  z-index: 5;
  
  &.active {
    .drop-below-line {
      opacity: 1;
      transform: scaleY(1);
    }
  }

  .drop-below-line {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 3px;
    background: rgb(var(--primary-6));
    border-radius: 1.5px;
    opacity: 0;
    transform: scaleY(0);
    transition: all 0.2s;
  }
}

.field-label {
  position: relative;
  font-size: 12px;
  color: var(--color-text-3);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  align-items: center;
}

.empty-section {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  border: none;
  border-radius: 6px;
  color: var(--color-text-3);
  font-size: 14px;
  background: transparent;
}

.field-delete {
  position: absolute;
  top: 4px;
  right: 14px; // Avoid overlap with resize handle
  opacity: 0;
  transition: opacity 0.2s;
  color: var(--color-text-3);
  
  &:hover {
    color: rgb(var(--danger-6));
    background: rgba(var(--danger-6), 0.1);
  }
}
</style>
