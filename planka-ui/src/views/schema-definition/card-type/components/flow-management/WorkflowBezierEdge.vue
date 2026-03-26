<script setup lang="ts">
/**
 * 在目标端沿贝塞尔终点切线方向收短（与 vue-flow getBezierPath 同源控制点），
 * 使箭头 orient 与曲线一致，且尖端落在入点圆外侧。
 */
import { computed, useAttrs } from 'vue'
import { BaseEdge, getBezierPath, Position } from '@vue-flow/core'
import type { EdgeProps } from '@vue-flow/core'

defineOptions({ inheritAttrs: false })

/** 箭头尖端与 wf-in 圆点外沿的间距（像素） */
const TARGET_END_GAP = 4

function calculateControlOffset(distance2: number, curvature: number): number {
  if (distance2 >= 0) {
    return 0.5 * distance2
  }
  return curvature * 25 * Math.sqrt(-distance2)
}

function getControlWithCurvature(
  pos: Position,
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  c: number,
): [number, number] {
  let ctX: number
  let ctY: number
  switch (pos) {
    case Position.Left:
      ctX = x1 - calculateControlOffset(x1 - x2, c)
      ctY = y1
      break
    case Position.Right:
      ctX = x1 + calculateControlOffset(x2 - x1, c)
      ctY = y1
      break
    case Position.Top:
      ctX = x1
      ctY = y1 - calculateControlOffset(y1 - y2, c)
      break
    case Position.Bottom:
      ctX = x1
      ctY = y1 + calculateControlOffset(y2 - y1, c)
      break
    default:
      ctX = x1
      ctY = y1
  }
  return [ctX, ctY]
}

const props = defineProps<EdgeProps>()
const attrs = useAttrs()

const bezier = computed(() => {
  const {
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourcePosition = Position.Bottom,
    targetPosition = Position.Top,
    curvature = 0.25,
  } = props

  const chordDx = targetX - sourceX
  const chordDy = targetY - sourceY
  const chordLen = Math.hypot(chordDx, chordDy)

  const baseParams = {
    sourceX,
    sourceY,
    sourcePosition,
    targetPosition,
    curvature,
  }

  if (chordLen < 1e-6) {
    const [path, labelX, labelY] = getBezierPath({
      ...baseParams,
      targetX,
      targetY,
    })
    return { path, labelX, labelY }
  }

  const [sourceControlX, sourceControlY] = getControlWithCurvature(
    sourcePosition,
    sourceX,
    sourceY,
    targetX,
    targetY,
    curvature,
  )
  const [targetControlX, targetControlY] = getControlWithCurvature(
    targetPosition,
    targetX,
    targetY,
    sourceX,
    sourceY,
    curvature,
  )

  // 终点处切线方向 B'(1) ∝ (P3 - P2)，沿该方向回缩比沿弦方向更贴合 orient="auto"
  const tx = targetX - targetControlX
  const ty = targetY - targetControlY
  const tlen = Math.hypot(tx, ty)

  let endX = targetX
  let endY = targetY

  if (tlen >= 1e-6) {
    const ux = tx / tlen
    const uy = ty / tlen
    const maxGap = Math.min(TARGET_END_GAP, chordLen * 0.32)
    endX = targetX - ux * maxGap
    endY = targetY - uy * maxGap
  }

  const path = `M${sourceX},${sourceY} C${sourceControlX},${sourceControlY} ${targetControlX},${targetControlY} ${endX},${endY}`

  const labelX =
    sourceX * 0.125 +
    sourceControlX * 0.375 +
    targetControlX * 0.375 +
    endX * 0.125
  const labelY =
    sourceY * 0.125 +
    sourceControlY * 0.375 +
    targetControlY * 0.375 +
    endY * 0.125

  return { path, labelX, labelY }
})
</script>

<template>
  <BaseEdge
    v-bind="{
      ...attrs,
      ...props,
      path: bezier.path,
      labelX: bezier.labelX,
      labelY: bezier.labelY,
    }"
  />
</template>
