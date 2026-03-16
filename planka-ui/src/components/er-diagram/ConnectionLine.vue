<script setup lang="ts">
import { computed } from 'vue'
import type { LinkTypeVO } from '@/types/link-type'

interface Props {
  sourcePos: { x: number; y: number }
  targetPos: { x: number; y: number }
  sourceCenter: { x: number; y: number }
  targetCenter: { x: number; y: number }
  linkType: LinkTypeVO
  sourceMulti?: boolean
  targetMulti?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  sourceMulti: true,
  targetMulti: true,
})

// Calculate orthogonal path with smart routing
const pathData = computed(() => {
  const sx = props.sourcePos.x
  const sy = props.sourcePos.y
  const tx = props.targetPos.x
  const ty = props.targetPos.y
  
  const dx = tx - sx
  const dy = ty - sy
  const horizontalOffset = Math.min(60, Math.abs(dx) * 0.3)
  
  if (Math.abs(dx) > Math.abs(dy)) {
    if (dx > 0) {
      const midX = sx + dx / 2
      return `M ${sx} ${sy} L ${sx + horizontalOffset} ${sy} C ${midX} ${sy}, ${midX} ${ty}, ${tx - horizontalOffset} ${ty} L ${tx} ${ty}`
    } else {
      const midX = sx + dx / 2
      return `M ${sx} ${sy} L ${sx - horizontalOffset} ${sy} C ${midX} ${sy}, ${midX} ${ty}, ${tx + horizontalOffset} ${ty} L ${tx} ${ty}`
    }
  } else {
    const offsetX = dx > 0 ? horizontalOffset : -horizontalOffset
    const midY = sy + dy / 2
    return `M ${sx} ${sy} L ${sx + offsetX} ${sy} Q ${sx + offsetX} ${midY}, ${(sx + tx) / 2} ${midY} Q ${tx - offsetX} ${midY}, ${tx - offsetX} ${ty} L ${tx} ${ty}`
  }
})

// Label position at midpoint of the connection
const labelPos = computed(() => {
  const sx = props.sourcePos.x
  const sy = props.sourcePos.y
  const tx = props.targetPos.x
  const ty = props.targetPos.y
  
  return {
    x: (sx + tx) / 2,
    y: (sy + ty) / 2,
  }
})

// Connection label text combining both names
const labelText = computed(() => {
  return `${props.linkType.sourceName} ↔ ${props.linkType.targetName}`
})

// Cardinality marker positions
const sourceMarkerPos = computed(() => {
  const dx = props.targetPos.x - props.sourcePos.x
  const dy = props.targetPos.y - props.sourcePos.y
  const isHorizontal = Math.abs(dx) > Math.abs(dy)
  const sx = props.sourcePos.x
  const sy = props.sourcePos.y
  
  if (isHorizontal) {
    return { x: dx > 0 ? sx + 12 : sx - 12, y: sy }
  } else {
    return { x: sx, y: dy > 0 ? sy + 12 : sy - 12 }
  }
})

const targetMarkerPos = computed(() => {
  const dx = props.targetPos.x - props.sourcePos.x
  const dy = props.targetPos.y - props.sourcePos.y
  const isHorizontal = Math.abs(dx) > Math.abs(dy)
  const tx = props.targetPos.x
  const ty = props.targetPos.y
  
  if (isHorizontal) {
    return { x: dx > 0 ? tx - 12 : tx + 12, y: ty }
  } else {
    return { x: tx, y: dy > 0 ? ty - 12 : ty + 12 }
  }
})
</script>

<template>
  <g class="connection-line">
    <!-- Main connection path -->
    <path
      :d="pathData"
      class="connection-path"
      fill="none"
      stroke="#b8c5d3"
      stroke-width="1.5"
    />
    
    <!-- Invisible wider path for easier hover -->
    <path
      :d="pathData"
      class="connection-path-hover"
      fill="none"
      stroke="transparent"
      stroke-width="16"
    />
    
    <!-- Centered label (shown on hover) -->
    <g class="label-group" :transform="`translate(${labelPos.x}, ${labelPos.y})`">
      <rect
        :x="-80"
        :y="-10"
        width="160"
        height="20"
        rx="4"
        class="label-bg"
      />
      <text
        text-anchor="middle"
        dominant-baseline="central"
        class="connection-label"
      >
        {{ labelText }}
      </text>
    </g>
    
    <!-- Source cardinality marker -->
    <g :transform="`translate(${sourceMarkerPos.x}, ${sourceMarkerPos.y})`" class="cardinality-marker">
      <circle r="8" fill="white" stroke="#b8c5d3" stroke-width="1" />
      <text class="cardinality-text" text-anchor="middle" dominant-baseline="central" fill="#64748b">
        {{ sourceMulti ? 'N' : '1' }}
      </text>
    </g>
    
    <!-- Target cardinality marker -->
    <g :transform="`translate(${targetMarkerPos.x}, ${targetMarkerPos.y})`" class="cardinality-marker">
      <circle r="8" fill="white" stroke="#b8c5d3" stroke-width="1" />
      <text class="cardinality-text" text-anchor="middle" dominant-baseline="central" fill="#64748b">
        {{ targetMulti ? 'N' : '1' }}
      </text>
    </g>
  </g>
</template>

<style scoped>
.connection-path {
  transition: stroke 0.2s, stroke-width 0.2s;
}

.connection-line:hover .connection-path {
  stroke: rgb(var(--primary-6));
  stroke-width: 2;
}

/* Label always visible */
.label-group {
  pointer-events: none;
}

.label-bg {
  fill: rgba(255, 255, 255, 0.95);
  stroke: #e2e8f0;
  stroke-width: 1;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.08));
}

.connection-line:hover .label-bg {
  stroke: rgb(var(--primary-5));
}

.connection-label {
  font-size: 10px;
  fill: #475569;
  font-weight: 500;
}

.connection-line:hover .connection-label {
  fill: rgb(var(--primary-6));
}

.cardinality-marker circle {
  transition: stroke 0.2s;
}

.connection-line:hover .cardinality-marker circle {
  stroke: rgb(var(--primary-6));
}

.cardinality-text {
  font-size: 9px;
  font-weight: 600;
  user-select: none;
}
</style>
