<script setup lang="ts">
import { computed, inject } from 'vue'
import { EdgeLabelRenderer, getSmoothStepPath, Position, useVueFlow } from '@vue-flow/core'
import type { EdgeProps } from '@vue-flow/core'

const props = defineProps<EdgeProps>()

const edgeActionHandlers = inject<{
  onEdit?: (edgeId: string) => void
}>('edgeActionHandlers', {})

const { addSelectedEdges, removeSelectedEdges, getSelectedEdges } = useVueFlow()

const path = computed(() =>
  getSmoothStepPath({
    sourceX: props.sourceX,
    sourceY: props.sourceY,
    sourcePosition: props.sourcePosition,
    targetX: props.targetX,
    targetY: props.targetY,
    targetPosition: props.targetPosition,
    borderRadius: 0, // 直角折线，类似 PowerDesigner
  }),
)

const sourceLabel = computed(() => {
  const label = (props.label as string) || ''
  const parts = label.split(' → ')
  return parts[0] || ''
})

const targetLabel = computed(() => {
  const label = (props.label as string) || ''
  const parts = label.split(' → ')
  return parts[1] || ''
})

const sourceCardinality = computed(() => (props.data?.sourceMulti ? 'N' : '1'))
const targetCardinality = computed(() => (props.data?.targetMulti ? 'N' : '1'))

const isSelected = computed(() => props.selected)

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

function selectEdge() {
  const currentSelected = getSelectedEdges.value
  if (currentSelected.length > 0) {
    removeSelectedEdges(currentSelected)
  }
  addSelectedEdges([{ id: props.id } as any])
}

function onEdgeOrLabelClick(event: MouseEvent) {
  event.stopPropagation()
  selectEdge()
}

/** 双击连线或两侧标签：进入关联类型编辑 */
function openEdit(event: MouseEvent) {
  event.stopPropagation()
  event.preventDefault()
  selectEdge()
  edgeActionHandlers.onEdit?.(props.id)
}
</script>

<template>
  <!-- 先画底层描边与可见线；上层透明宽路径置于最后，否则点击会落在可见 path 上无法触发交互 -->
  <path
    :d="path[0]"
    fill="none"
    stroke="white"
    :stroke-width="isSelected ? 8 : 6"
    stroke-linecap="round"
    stroke-linejoin="round"
    class="edge-background-path edge-no-pointer"
  />
  <path
    :id="id"
    :d="path[0]"
    :style="style"
    fill="none"
    :stroke="isSelected ? '#fcd34d' : '#b8c5d3'"
    :stroke-width="isSelected ? 2 : 1.5"
    :marker-end="markerEnd"
    class="vue-flow__edge-path edge-no-pointer"
    :class="{ selected: isSelected }"
  />
  <path
    :d="path[0]"
    fill="none"
    stroke="transparent"
    stroke-width="24"
    class="edge-interaction-path"
    @click="onEdgeOrLabelClick"
    @dblclick="openEdit"
  />

  <EdgeLabelRenderer>
    <div
      :style="{
        position: 'absolute',
        transform: `translate(-50%, -50%) translate(${sourceLabelPosition.x}px, ${sourceLabelPosition.y}px)`,
        pointerEvents: 'all',
        cursor: 'pointer',
        zIndex: isSelected ? 1001 : 1000,
      }"
      class="edge-label-group nodrag nopan"
      :class="{ selected: isSelected }"
      @click="onEdgeOrLabelClick"
      @dblclick="openEdit"
    >
      <span class="cardinality">{{ sourceCardinality }}</span>
      <span class="label-text">{{ sourceLabel }}</span>
    </div>

    <div
      :style="{
        position: 'absolute',
        transform: `translate(-50%, -50%) translate(${targetLabelPosition.x}px, ${targetLabelPosition.y}px)`,
        pointerEvents: 'all',
        cursor: 'pointer',
        zIndex: isSelected ? 1001 : 1000,
      }"
      class="edge-label-group nodrag nopan"
      :class="{ selected: isSelected }"
      @click="onEdgeOrLabelClick"
      @dblclick="openEdit"
    >
      <span class="cardinality">{{ targetCardinality }}</span>
      <span class="label-text">{{ targetLabel }}</span>
    </div>
  </EdgeLabelRenderer>
</template>

<style scoped>
.edge-no-pointer {
  pointer-events: none;
}

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
