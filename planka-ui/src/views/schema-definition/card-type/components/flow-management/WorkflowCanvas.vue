<script setup lang="ts">
/**
 * 工作流画布编辑器（Coze 风格：浅色点阵画布 + 横向卡片节点 + 右侧属性面板）
 */
import { ref, computed, watch, markRaw, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { VueFlow, useVueFlow, MarkerType, Position } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import type { Node, Edge, NodeTypesObject } from '@vue-flow/core'
import StartNode from './nodes/StartNode.vue'
import EndNode from './nodes/EndNode.vue'
import ApprovalNode from './nodes/ApprovalNode.vue'
import AutoActionNode from './nodes/AutoActionNode.vue'
import WorkflowFlowBottomBar from './WorkflowFlowBottomBar.vue'
import ApprovalNodePanel from './panels/ApprovalNodePanel.vue'
import AutoActionNodePanel from './panels/AutoActionNodePanel.vue'
import type {
  WorkflowDefinition,
  NodeDefinition,
  ApprovalNodeDefinition,
  AutoActionNodeDefinition,
} from '@/types/workflow'
import {
  WorkflowNodeType,
  createApprovalNode,
  createAutoActionNode,
  generateEdgeId,
} from '@/types/workflow'

const { t } = useI18n()

const props = defineProps<{
  workflow: WorkflowDefinition
  cardTypeId: string
}>()

const emit = defineEmits<{
  'update:workflow': [workflow: WorkflowDefinition]
}>()

const nodeTypes: NodeTypesObject = {
  START: markRaw(StartNode) as any,
  END: markRaw(EndNode) as any,
  APPROVAL: markRaw(ApprovalNode) as any,
  AUTO_ACTION: markRaw(AutoActionNode) as any,
}

const { fitView, updateNodeInternals } = useVueFlow()

const nodes = ref<Node[]>([])
const edges = ref<Edge[]>([])
const selectedNodeId = ref<string | null>(null)
const zoomPercent = ref(100)

/** 仅当节点/边结构变化时触发 fitView，避免拖拽保存后画布跳动 */
const lastWorkflowStructureKey = ref<string>('')

const selectedNodeDef = computed<NodeDefinition | null>(() => {
  if (!selectedNodeId.value) return null
  return props.workflow.nodes.find((n) => n.id === selectedNodeId.value) ?? null
})

const panelVisible = computed(() => {
  if (!selectedNodeDef.value) return false
  const type = selectedNodeDef.value.nodeType
  return type === WorkflowNodeType.APPROVAL || type === WorkflowNodeType.AUTO_ACTION
})

function workflowStructureKey(def: WorkflowDefinition): string {
  const nodePart = def.nodes.map((n) => `${n.id}:${n.nodeType}`).join(',')
  const edgePart = def.edges
    .map((e) => `${e.id}:${e.sourceNodeId}->${e.targetNodeId}`)
    .join('|')
  return `${nodePart}||${edgePart}`
}

// ==================== 数据转换 ====================

function definitionToFlow(def: WorkflowDefinition) {
  const NODE_GAP = 300
  const NODE_Y = 80

  const orderedIds: string[] = []
  const visited = new Set<string>()
  const edgeMap = new Map<string, string>()
  def.edges.forEach((e) => edgeMap.set(e.sourceNodeId, e.targetNodeId))

  const startNode = def.nodes.find((n) => n.nodeType === WorkflowNodeType.START)
  if (startNode) {
    let currentId: string | undefined = startNode.id
    while (currentId && !visited.has(currentId)) {
      visited.add(currentId)
      orderedIds.push(currentId)
      currentId = edgeMap.get(currentId)
    }
  }

  def.nodes.forEach((n) => {
    if (!visited.has(n.id)) orderedIds.push(n.id)
  })

  const nodeMap = new Map(def.nodes.map((n) => [n.id, n]))
  const layout = def.canvasLayout ?? {}

  const flowNodes: Node[] = orderedIds.map((id, index) => {
    const nodeDef = nodeMap.get(id)!
    const saved = layout[id]
    const position = saved
      ? { x: saved.x, y: saved.y }
      : { x: index * NODE_GAP, y: NODE_Y }

    return {
      id,
      type: nodeDef.nodeType,
      position,
      data: buildNodeData(nodeDef),
      draggable: true,
    }
  })

  // 必须与 Handle 的 Position 一致；否则 BezierEdge 默认按 Bottom→Top 计算控制点，连线会脱离左右端口
  const flowEdges: Edge[] = def.edges.map((e) => ({
    id: e.id,
    source: e.sourceNodeId,
    target: e.targetNodeId,
    sourceHandle: 'wf-out',
    targetHandle: 'wf-in',
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    type: 'default',
    animated: false,
    style: { stroke: '#3370FF', strokeWidth: 2 },
    pathOptions: { curvature: 0.38 },
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 18,
      height: 18,
      color: '#3370FF',
    },
  }))

  return { flowNodes, flowEdges }
}

function buildNodeData(nodeDef: NodeDefinition) {
  const base = { label: nodeDef.name }
  switch (nodeDef.nodeType) {
    case WorkflowNodeType.APPROVAL: {
      const n = nodeDef as ApprovalNodeDefinition
      const approverCount =
        n.approverSelector.selectorType === 'FIXED_MEMBERS'
          ? n.approverSelector.memberIds.length
          : 0
      return { ...base, approvalMode: n.approvalMode, approverCount }
    }
    case WorkflowNodeType.AUTO_ACTION: {
      const n = nodeDef as AutoActionNodeDefinition
      return { ...base, actionsCount: n.actions.length }
    }
    default:
      return base
  }
}

function onViewportChange(viewport: { zoom: number }) {
  zoomPercent.value = Math.round(viewport.zoom * 100)
}

function onNodeDragStop({ node }: { node: Node }) {
  const layout = {
    ...(props.workflow.canvasLayout ?? {}),
    [node.id]: { x: node.position.x, y: node.position.y },
  }
  emit('update:workflow', { ...props.workflow, canvasLayout: layout })
}

watch(
  () => props.workflow,
  (def) => {
    const structureKey = workflowStructureKey(def)
    const structureChanged = structureKey !== lastWorkflowStructureKey.value
    lastWorkflowStructureKey.value = structureKey

    const { flowNodes, flowEdges } = definitionToFlow(def)
    nodes.value = flowNodes
    edges.value = flowEdges

    nextTick(() => {
      updateNodeInternals(nodes.value.map((n) => n.id))
      if (structureChanged) {
        fitView({ padding: 0.35 })
      }
    })
  },
  { immediate: true, deep: true },
)

// ==================== 节点操作 ====================

function insertNodeOnEdge(edgeId: string, nodeType: WorkflowNodeType) {
  const edge = props.workflow.edges.find((e) => e.id === edgeId)
  if (!edge) return

  let newNodeDef: NodeDefinition
  if (nodeType === WorkflowNodeType.APPROVAL) {
    newNodeDef = createApprovalNode(t('admin.workflow.nodeType.APPROVAL'))
  } else if (nodeType === WorkflowNodeType.AUTO_ACTION) {
    newNodeDef = createAutoActionNode(t('admin.workflow.nodeType.AUTO_ACTION'))
  } else {
    return
  }

  const newEdges = props.workflow.edges.filter((e) => e.id !== edgeId)
  newEdges.push(
    { id: generateEdgeId(), sourceNodeId: edge.sourceNodeId, targetNodeId: newNodeDef.id },
    { id: generateEdgeId(), sourceNodeId: newNodeDef.id, targetNodeId: edge.targetNodeId },
  )

  emit('update:workflow', {
    ...props.workflow,
    nodes: [...props.workflow.nodes, newNodeDef],
    edges: newEdges,
  })

  selectedNodeId.value = newNodeDef.id
}

function addNodeBeforeEnd(nodeType: WorkflowNodeType) {
  const endNode = props.workflow.nodes.find((n) => n.nodeType === WorkflowNodeType.END)
  if (!endNode) return
  const incomingEdge = props.workflow.edges.find((e) => e.targetNodeId === endNode.id)
  if (incomingEdge) {
    insertNodeOnEdge(incomingEdge.id, nodeType)
  }
}

function deleteNode(nodeId: string) {
  const nodeDef = props.workflow.nodes.find((n) => n.id === nodeId)
  if (!nodeDef) return
  if (
    nodeDef.nodeType === WorkflowNodeType.START ||
    nodeDef.nodeType === WorkflowNodeType.END
  )
    return

  const inEdge = props.workflow.edges.find((e) => e.targetNodeId === nodeId)
  const outEdge = props.workflow.edges.find((e) => e.sourceNodeId === nodeId)

  const newNodes = props.workflow.nodes.filter((n) => n.id !== nodeId)
  const newEdges = props.workflow.edges.filter(
    (e) => e.sourceNodeId !== nodeId && e.targetNodeId !== nodeId,
  )

  if (inEdge && outEdge) {
    newEdges.push({
      id: generateEdgeId(),
      sourceNodeId: inEdge.sourceNodeId,
      targetNodeId: outEdge.targetNodeId,
    })
  }

  if (selectedNodeId.value === nodeId) selectedNodeId.value = null

  const nextLayout = { ...(props.workflow.canvasLayout ?? {}) }
  delete nextLayout[nodeId]

  emit('update:workflow', {
    ...props.workflow,
    nodes: newNodes,
    edges: newEdges,
    canvasLayout: nextLayout,
  })
}

function handleNodeClick(event: { node: Node }) {
  selectedNodeId.value = event.node.id
}

function handlePaneClick() {
  selectedNodeId.value = null
}

function handleNodeUpdate(updatedNode: NodeDefinition) {
  const newNodes = props.workflow.nodes.map((n) =>
    n.id === updatedNode.id ? updatedNode : n,
  )
  emit('update:workflow', { ...props.workflow, nodes: newNodes })
}

defineExpose({ addNodeBeforeEnd, deleteNode, selectedNodeId })
</script>

<template>
  <div class="wf-canvas">
    <div class="wf-canvas__main" :class="{ 'panel-open': panelVisible }">
      <VueFlow
        v-model:nodes="nodes"
        v-model:edges="edges"
        :node-types="nodeTypes"
        :nodes-draggable="true"
        :nodes-connectable="false"
        :edges-updatable="false"
        :zoom-on-scroll="true"
        :pan-on-scroll="true"
        :fit-view-on-init="true"
        :min-zoom="0.25"
        :max-zoom="2"
        @node-click="handleNodeClick"
        @pane-click="handlePaneClick"
        @viewport-change="onViewportChange"
        @node-drag-stop="onNodeDragStop"
      >
        <Background
          variant="dots"
          :gap="20"
          :size="1"
          pattern-color="rgba(31, 35, 41, 0.12)"
        />
        <WorkflowFlowBottomBar :zoom-percent="zoomPercent" @add-node="addNodeBeforeEnd" />
      </VueFlow>
    </div>

    <!-- 右侧属性面板 -->
    <transition name="slide-right">
      <div v-if="panelVisible && selectedNodeDef" class="wf-panel">
        <div class="wf-panel__header">
          <span class="wf-panel__title">{{ t('admin.workflow.panel.title') }}</span>
          <a-button type="text" size="mini" @click="selectedNodeId = null">
            <template #icon><icon-close /></template>
          </a-button>
        </div>
        <a-divider :margin="0" />
        <div class="wf-panel__body">
          <div class="wf-panel__danger-zone">
            <a-button
              type="text"
              status="danger"
              size="small"
              @click="deleteNode(selectedNodeDef!.id)"
            >
              <template #icon><icon-delete /></template>
              {{ t('admin.workflow.panel.deleteNode') }}
            </a-button>
          </div>

          <ApprovalNodePanel
            v-if="selectedNodeDef.nodeType === WorkflowNodeType.APPROVAL"
            :node="(selectedNodeDef as ApprovalNodeDefinition)"
            @update:node="handleNodeUpdate"
          />
          <AutoActionNodePanel
            v-if="selectedNodeDef.nodeType === WorkflowNodeType.AUTO_ACTION"
            :node="(selectedNodeDef as AutoActionNodeDefinition)"
            :card-type-id="cardTypeId"
            @update:node="handleNodeUpdate"
          />
        </div>
      </div>
    </transition>
  </div>
</template>

<style lang="scss">
/*
 * Vue Flow 官方样式必须未 scoped：在 scoped 内 @import 会给选择器加 data-v-*，无法匹配库内 DOM，
 * 导致 .vue-flow__handle-left/right 等不生效，端口叠在同一侧、连线错位。与 ERDiagramView.vue 一致。
 */
@import '@vue-flow/core/dist/style.css';
@import '@vue-flow/core/dist/theme-default.css';
</style>

<style scoped lang="scss">
.wf-canvas {
  display: flex;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.wf-canvas__main {
  flex: 1;
  min-width: 0;
  transition: margin-right 0.25s ease;

  &.panel-open {
    margin-right: 300px;
  }
}

// ===== 浅色画布 =====
:deep(.vue-flow) {
  direction: ltr;
  background: #f5f6f8;
}

// 选中高亮（节点卡片类名见各节点组件）
:deep(.vue-flow__node.selected .wf-node-card) {
  border-color: var(--color-primary);
  box-shadow:
    0 0 0 2px rgba(51, 112, 255, 0.2),
    0 8px 24px rgba(31, 35, 41, 0.08);
}

// Handle（节点内可覆盖，此处为默认）
:deep(.wf-handle) {
  width: 10px;
  height: 10px;
  background: #3370ff;
  border: 2px solid #fff;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(31, 35, 41, 0.12);

  &:hover {
    background: var(--color-primary-hover);
  }
}

// 仅绘制一条可见边线，避免与主题叠加产生「加粗」观感
:deep(.vue-flow__edge-path) {
  stroke: #3370ff;
  stroke-width: 2;
  fill: none;
}

:deep(.vue-flow__edge.selected .vue-flow__edge-path) {
  stroke: var(--color-primary-hover);
}

// ===== 属性面板 =====
.wf-panel {
  position: absolute;
  top: 0;
  right: 0;
  width: 300px;
  height: 100%;
  background: #fff;
  border-left: 1px solid var(--color-border-1);
  box-shadow: -4px 0 24px rgba(31, 35, 41, 0.06);
  display: flex;
  flex-direction: column;
  z-index: 10;
}

.wf-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  flex-shrink: 0;
}

.wf-panel__title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
}

.wf-panel__body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.wf-panel__danger-zone {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

// 滑入动画
.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.25s ease;
}

.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
}
</style>
