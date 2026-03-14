<script setup lang="ts">
import { computed, ref, inject, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { EdgeLabelRenderer, getSmoothStepPath, Position, useVueFlow } from '@vue-flow/core'
import type { EdgeProps } from '@vue-flow/core'
import { IconEdit, IconLink, IconStop, IconDelete } from '@arco-design/web-vue/es/icon'

const { t } = useI18n()

const props = defineProps<EdgeProps>()

// Inject edge action handlers from parent
const edgeActionHandlers = inject<{
  onEdit?: (edgeId: string) => void
  onShowReferences?: (edgeId: string) => void
  onDisable?: (edgeId: string) => void
  onDelete?: (edgeId: string) => void
  onQuickUpdate?: (edgeId: string, side: 'source' | 'target', name: string, multiSelect: boolean) => Promise<void>
}>('edgeActionHandlers', {})

const { addSelectedEdges, removeSelectedEdges, getSelectedEdges } = useVueFlow()

// Context menu state (for edge double-click)
const contextMenuVisible = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })

// Inline edit state (for label double-click)
const editingSide = ref<'source' | 'target' | null>(null)
const editingName = ref('')
const editingMulti = ref(false)
const inlineEditPosition = ref({ x: 0, y: 0 })
const nameInputRef = ref<HTMLInputElement | null>(null)

const path = computed(() =>
  getSmoothStepPath({
    sourceX: props.sourceX,
    sourceY: props.sourceY,
    sourcePosition: props.sourcePosition,
    targetX: props.targetX,
    targetY: props.targetY,
    targetPosition: props.targetPosition,
  })
)

// Parse label to get source and target names
const sourceLabel = computed(() => {
  const label = props.label as string || ''
  const parts = label.split(' → ')
  return parts[0] || ''
})

const targetLabel = computed(() => {
  const label = props.label as string || ''
  const parts = label.split(' → ')
  return parts[1] || ''
})

// Get cardinality labels from edge data
const sourceCardinality = computed(() => (props.data?.sourceMulti ? 'N' : '1'))
const targetCardinality = computed(() => (props.data?.targetMulti ? 'N' : '1'))

// Check if this edge is selected
const isSelected = computed(() => props.selected)

// Calculate label positions based on handle position
const sourceLabelPosition = computed(() => {
  const offset = 60
  switch (props.sourcePosition) {
    case Position.Right:
      return { x: props.sourceX + offset, y: props.sourceY }
    case Position.Left:
      return { x: props.sourceX - offset, y: props.sourceY }
    case Position.Top:
      return { x: props.sourceX, y: props.sourceY - offset }
    case Position.Bottom:
      return { x: props.sourceX, y: props.sourceY + offset }
    default:
      return { x: props.sourceX + offset, y: props.sourceY }
  }
})

const targetLabelPosition = computed(() => {
  const offset = 60
  switch (props.targetPosition) {
    case Position.Right:
      return { x: props.targetX + offset, y: props.targetY }
    case Position.Left:
      return { x: props.targetX - offset, y: props.targetY }
    case Position.Top:
      return { x: props.targetX, y: props.targetY - offset }
    case Position.Bottom:
      return { x: props.targetX, y: props.targetY + offset }
    default:
      return { x: props.targetX - offset, y: props.targetY }
  }
})

// Select edge helper
function selectEdge() {
  const currentSelected = getSelectedEdges.value
  if (currentSelected.length > 0) {
    removeSelectedEdges(currentSelected)
  }
  addSelectedEdges([{ id: props.id } as any])
}

// Handle label click to select the edge
function onLabelClick(event: MouseEvent) {
  event.stopPropagation()
  selectEdge()
}

// Handle label double-click for inline edit
function onLabelDoubleClick(event: MouseEvent, side: 'source' | 'target') {
  event.stopPropagation()
  event.preventDefault()
  selectEdge()

  // Initialize inline edit
  editingSide.value = side
  editingName.value = side === 'source' ? sourceLabel.value : targetLabel.value
  editingMulti.value = side === 'source' ? !!props.data?.sourceMulti : !!props.data?.targetMulti
  inlineEditPosition.value = { x: event.clientX, y: event.clientY }

  // Focus the input after render
  nextTick(() => {
    nameInputRef.value?.focus()
    nameInputRef.value?.select()
  })
}

// Handle edge path double-click for context menu
function onEdgeDoubleClick(event: MouseEvent) {
  event.stopPropagation()
  event.preventDefault()
  selectEdge()

  // Show context menu at click position
  contextMenuPosition.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
}

// Close context menu
function closeContextMenu() {
  contextMenuVisible.value = false
}

// Close inline edit
function closeInlineEdit() {
  editingSide.value = null
}

// Save inline edit
async function saveInlineEdit() {
  if (!editingSide.value || !editingName.value.trim()) {
    closeInlineEdit()
    return
  }

  try {
    await edgeActionHandlers.onQuickUpdate?.(
      props.id,
      editingSide.value,
      editingName.value.trim(),
      editingMulti.value
    )
  } catch (error) {
    console.error('Failed to save:', error)
  }
  closeInlineEdit()
}

// Handle inline edit key events
function onInlineEditKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    event.preventDefault()
    saveInlineEdit()
  } else if (event.key === 'Escape') {
    closeInlineEdit()
  }
}

// Toggle multi-select in inline edit
function toggleMulti() {
  editingMulti.value = !editingMulti.value
}

// Handle menu actions
function handleEdit() {
  edgeActionHandlers.onEdit?.(props.id)
  closeContextMenu()
}

function handleShowReferences() {
  edgeActionHandlers.onShowReferences?.(props.id)
  closeContextMenu()
}

function handleDisable() {
  edgeActionHandlers.onDisable?.(props.id)
  closeContextMenu()
}

function handleDelete() {
  edgeActionHandlers.onDelete?.(props.id)
  closeContextMenu()
}
</script>

<template>
  <!-- Edge path with invisible wider stroke for easier clicking -->
  <path
    :d="path[0]"
    fill="none"
    stroke="transparent"
    stroke-width="20"
    class="edge-interaction-path"
    @dblclick="onEdgeDoubleClick"
  />
  <!-- Visible edge path -->
  <path
    :id="id"
    :d="path[0]"
    :style="style"
    fill="none"
    :stroke="isSelected ? '#fcd34d' : '#b8c5d3'"
    :stroke-width="isSelected ? 2 : 1.5"
    :marker-end="markerEnd"
    class="vue-flow__edge-path"
    :class="{ selected: isSelected }"
  />

  <EdgeLabelRenderer>
    <!-- Source side: cardinality + label -->
    <div
      v-if="editingSide !== 'source'"
      :style="{
        position: 'absolute',
        transform: `translate(-50%, -50%) translate(${sourceLabelPosition.x}px, ${sourceLabelPosition.y}px)`,
        pointerEvents: 'all',
        cursor: 'pointer',
        zIndex: isSelected ? 1000 : 1,
      }"
      class="edge-label-group nodrag nopan"
      :class="{ selected: isSelected }"
      @click="onLabelClick"
      @dblclick="(e) => onLabelDoubleClick(e, 'source')"
    >
      <span class="cardinality">{{ sourceCardinality }}</span>
      <span class="label-text">{{ sourceLabel }}</span>
    </div>

    <!-- Target side: cardinality + label -->
    <div
      v-if="editingSide !== 'target'"
      :style="{
        position: 'absolute',
        transform: `translate(-50%, -50%) translate(${targetLabelPosition.x}px, ${targetLabelPosition.y}px)`,
        pointerEvents: 'all',
        cursor: 'pointer',
        zIndex: isSelected ? 1000 : 1,
      }"
      class="edge-label-group nodrag nopan"
      :class="{ selected: isSelected }"
      @click="onLabelClick"
      @dblclick="(e) => onLabelDoubleClick(e, 'target')"
    >
      <span class="cardinality">{{ targetCardinality }}</span>
      <span class="label-text">{{ targetLabel }}</span>
    </div>

    <!-- Context Menu (for edge double-click) -->
    <Teleport to="body">
      <div
        v-if="contextMenuVisible"
        class="edge-context-menu-overlay"
        @click="closeContextMenu"
      >
        <div
          class="edge-context-menu"
          :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
          @click.stop
        >
          <div class="context-menu-item" @click="handleEdit">
            <IconEdit class="menu-icon" />
            <span>{{ t('common.action.edit') }}</span>
          </div>
          <div class="context-menu-item" @click="handleShowReferences">
            <IconLink class="menu-icon" />
            <span>{{ t('common.erDiagram.reference') }}</span>
          </div>
          <div class="context-menu-item" @click="handleDisable">
            <IconStop class="menu-icon" />
            <span>{{ t('common.erDiagram.disable') }}</span>
          </div>
          <div class="context-menu-item danger" @click="handleDelete">
            <IconDelete class="menu-icon" />
            <span>{{ t('common.action.delete') }}</span>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Inline Edit Popover (for label double-click) -->
    <Teleport to="body">
      <div
        v-if="editingSide"
        class="inline-edit-overlay"
        @click="closeInlineEdit"
      >
        <div
          class="inline-edit-popover"
          :style="{ left: inlineEditPosition.x + 'px', top: inlineEditPosition.y + 'px' }"
          @click.stop
        >
          <div class="inline-edit-header">
            {{ editingSide === 'source' ? t('common.erDiagram.sourceName') : t('common.erDiagram.targetName') }}
          </div>
          <div class="inline-edit-content">
            <input
              ref="nameInputRef"
              v-model="editingName"
              class="inline-edit-input"
              :placeholder="t('common.message.pleaseInput')"
              maxlength="20"
              @keydown="onInlineEditKeydown"
            />
            <div class="inline-edit-multi" @click="toggleMulti">
              <span class="multi-label">{{ editingMulti ? t('common.erDiagram.multiSelect') : t('common.erDiagram.singleSelect') }}</span>
              <span class="multi-toggle" :class="{ active: editingMulti }">
                {{ editingMulti ? 'N' : '1' }}
              </span>
            </div>
          </div>
          <div class="inline-edit-footer">
            <button class="inline-edit-btn cancel" @click="closeInlineEdit">{{ t('common.action.cancel') }}</button>
            <button class="inline-edit-btn confirm" @click="saveInlineEdit">{{ t('common.action.ok') }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </EdgeLabelRenderer>
</template>

<style scoped>
.edge-interaction-path {
  cursor: pointer;
}

.vue-flow__edge-path {
  transition: stroke 0.2s, stroke-width 0.2s;
}

.vue-flow__edge-path.selected {
  filter: drop-shadow(0 0 6px rgba(252, 211, 77, 0.8));
}

.edge-label-group {
  display: flex;
  align-items: center;
  gap: 4px;
  background: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  white-space: nowrap;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.15s ease;
}

.edge-label-group:hover {
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.edge-label-group.selected {
  background: #fef08a;
}

.edge-label-group .cardinality {
  background: #f1f5f9;
  color: #475569;
  font-weight: 600;
  padding: 1px 4px;
  border-radius: 2px;
  font-size: 9px;
}

.edge-label-group.selected .cardinality {
  background: #fcd34d;
  color: #78350f;
}

.edge-label-group .label-text {
  color: #64748b;
}

.edge-label-group.selected .label-text {
  color: #78350f;
  font-weight: 500;
}
</style>

<!-- Global styles for Teleported content -->
<style>
.edge-context-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
}

.edge-context-menu {
  position: fixed;
  min-width: 140px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.08);
  padding: 4px 0;
  z-index: 10000;
}

.edge-context-menu .context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  color: var(--color-text-1, #1d2129);
  transition: background 0.15s;
}

.edge-context-menu .context-menu-item:hover {
  background: var(--color-fill-2, #f2f3f5);
}

.edge-context-menu .context-menu-item.danger {
  color: #f53f3f;
}

.edge-context-menu .context-menu-item.danger:hover {
  background: #ffece8;
}

.edge-context-menu .context-menu-item .menu-icon {
  font-size: 16px;
}

/* Inline Edit Popover Styles */
.inline-edit-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
}

.inline-edit-popover {
  position: fixed;
  width: 220px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15), 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 10000;
  transform: translate(-50%, 10px);
  box-sizing: border-box;
}

.inline-edit-header {
  padding: 10px 12px 6px;
  font-size: 12px;
  font-weight: 500;
  color: #86909c;
  border-bottom: 1px solid #f2f3f5;
}

.inline-edit-content {
  padding: 12px;
}

.inline-edit-input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.inline-edit-input:focus {
  border-color: #165dff;
}

.inline-edit-multi {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding: 8px 10px;
  background: #f7f8fa;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
}

.inline-edit-multi:hover {
  background: #f2f3f5;
}

.multi-label {
  font-size: 12px;
  color: #4e5969;
}

.multi-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #e5e6eb;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #86909c;
  transition: all 0.2s;
}

.multi-toggle.active {
  background: #165dff;
  color: white;
}

.inline-edit-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px 12px 12px;
}

.inline-edit-btn {
  padding: 5px 12px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  border: none;
}

.inline-edit-btn.cancel {
  background: #f2f3f5;
  color: #4e5969;
}

.inline-edit-btn.cancel:hover {
  background: #e5e6eb;
}

.inline-edit-btn.confirm {
  background: #165dff;
  color: white;
}

.inline-edit-btn.confirm:hover {
  background: #0e42d2;
}
</style>
