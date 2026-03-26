<script setup lang="ts">
/**
 * 工作流画布编辑器（Coze 风格：浅色点阵画布 + 横向卡片节点 + 右侧属性面板）
 */
import { ref, computed, watch, markRaw, nextTick, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { VueFlow, useVueFlow, MarkerType, Position, ConnectionLineType } from '@vue-flow/core'
import type {
  Node,
  Edge,
  NodeTypesObject,
  EdgeTypesObject,
  Connection,
  OnConnectStartParams,
  EdgeChange,
} from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import StartNode from './nodes/StartNode.vue'
import EndNode from './nodes/EndNode.vue'
import ApprovalNode from './nodes/ApprovalNode.vue'
import AutoActionNode from './nodes/AutoActionNode.vue'
import WorkflowBezierEdge from './WorkflowBezierEdge.vue'
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

const edgeTypes: EdgeTypesObject = {
  workflowBezier: markRaw(WorkflowBezierEdge) as any,
}

const { fitView, updateNodeInternals } = useVueFlow()

/** 开放折线箭头（非实心三角），与主题蓝一致；strokeWidth 与边线一致（略细于 2px） */
const WORKFLOW_EDGE_MARKER_END = {
  type: MarkerType.Arrow,
  width: 14,
  height: 14,
  color: '#3370FF',
  strokeWidth: 1.5,
} as const

/** 拖线预览：同色贝塞尔 + 相同箭头 */
const workflowConnectionLineOptions = {
  type: ConnectionLineType.Bezier,
  style: { stroke: '#3370FF', strokeWidth: 1.5 },
  markerEnd: { ...WORKFLOW_EDGE_MARKER_END },
} as const

const nodes = ref<Node[]>([])
const edges = ref<Edge[]>([])
const selectedNodeId = ref<string | null>(null)
const zoomPercent = ref(100)

/** 从右侧 wf-out 拖线结束：弹出菜单插入到该节点的出边上 */
const connectDragSourceId = ref<string | null>(null)
const pendingInsertSourceId = ref<string | null>(null)
const addNodeMenuVisible = ref(false)
const addNodeMenuX = ref(0)
const addNodeMenuY = ref(0)
/** 避免拖线松手时触发的 pane-click 立刻关掉弹层 */
const addNodeMenuOpenedAt = ref(0)
/** 本次拖线是否已在「下一节点」入点上成功吸附（connect 先于 connectEnd 触发） */
const lastConnectWasValidSnap = ref(false)

function getClientPosition(event?: MouseEvent | TouchEvent): { x: number; y: number } | null {
  if (!event) return null
  if ('changedTouches' in event && event.changedTouches?.length) {
    const touch = event.changedTouches.item(0) ?? event.changedTouches[0]
    if (!touch) return null
    return { x: touch.clientX, y: touch.clientY }
  }
  if ('touches' in event && event.touches?.length) {
    const touch = event.touches.item(0) ?? event.touches[0]
    if (!touch) return null
    return { x: touch.clientX, y: touch.clientY }
  }
  if ('clientX' in event && typeof event.clientX === 'number') {
    return { x: event.clientX, y: event.clientY }
  }
  return null
}

/**
 * 允许从 wf-out 连到 wf-in（结束节点无出点、开始节点无入点）。
 * 并行：同一源可连多条出边；非结束目标仍保持单入边（新连会替换该目标的旧入边）；结束节点允许多入边（并行汇合）。
 */
function isValidWorkflowConnection(connection: Connection): boolean {
  const { source, target, sourceHandle, targetHandle } = connection
  if (!source || !target || source === target) return false
  if (sourceHandle != null && sourceHandle !== 'wf-out') return false
  if (targetHandle != null && targetHandle !== 'wf-in') return false

  const sourceDef = props.workflow.nodes.find((n) => n.id === source)
  const targetDef = props.workflow.nodes.find((n) => n.id === target)
  if (!sourceDef || !targetDef) return false
  if (sourceDef.nodeType === WorkflowNodeType.END) return false
  if (targetDef.nodeType === WorkflowNodeType.START) return false

  return true
}

/** 并行分叉：保留源节点已有其它出边；非 END 目标去掉旧入边后再接 source→target；END 允许多入边，仅禁止重复 source→END */
function applyParallelConnection(source: string, target: string) {
  if (
    props.workflow.edges.some(
      (e) => e.sourceNodeId === source && e.targetNodeId === target,
    )
  ) {
    return
  }

  const targetDef = props.workflow.nodes.find((n) => n.id === target)
  const isEnd = targetDef?.nodeType === WorkflowNodeType.END

  const newEdges = isEnd
    ? [...props.workflow.edges]
    : props.workflow.edges.filter((e) => e.targetNodeId !== target)

  newEdges.push({
    id: generateEdgeId(),
    sourceNodeId: source,
    targetNodeId: target,
  })
  emit('update:workflow', { ...props.workflow, edges: newEdges })
}

function handleConnect(connection: Connection) {
  lastConnectWasValidSnap.value = true
  connectDragSourceId.value = null
  const { source, target } = connection
  if (!source || !target) return
  applyParallelConnection(source, target)
}

function handleConnectStart(
  params: OnConnectStartParams & { event?: MouseEvent | TouchEvent },
) {
  lastConnectWasValidSnap.value = false
  connectDragSourceId.value = null
  if (!params.nodeId || params.handleId !== 'wf-out') return
  const nodeDef = props.workflow.nodes.find((n) => n.id === params.nodeId)
  if (!nodeDef || nodeDef.nodeType === WorkflowNodeType.END) return
  connectDragSourceId.value = params.nodeId
}

function handleConnectEnd(event?: MouseEvent | TouchEvent) {
  if (lastConnectWasValidSnap.value) {
    lastConnectWasValidSnap.value = false
    connectDragSourceId.value = null
    return
  }

  const sourceId = connectDragSourceId.value
  connectDragSourceId.value = null
  if (!sourceId) return

  const outgoingFromSource = props.workflow.edges.filter(
    (e) => e.sourceNodeId === sourceId,
  )
  if (outgoingFromSource.length !== 1) return

  const pos = getClientPosition(event)
  if (!pos) return

  const x = Math.min(pos.x, window.innerWidth - 220)
  const y = Math.min(pos.y, window.innerHeight - 140)

  nextTick(() => {
    pendingInsertSourceId.value = sourceId
    addNodeMenuX.value = x
    addNodeMenuY.value = y
    addNodeMenuVisible.value = true
    addNodeMenuOpenedAt.value = Date.now()
  })
}

function closeAddNodeMenu() {
  addNodeMenuVisible.value = false
  pendingInsertSourceId.value = null
}

function insertNodeAfterSource(sourceNodeId: string, nodeType: WorkflowNodeType) {
  const outgoingList = props.workflow.edges.filter((e) => e.sourceNodeId === sourceNodeId)
  if (outgoingList.length !== 1) return
  insertNodeOnEdge(outgoingList[0]!.id, nodeType)
}

function pickNodeTypeFromMenu(nodeType: WorkflowNodeType) {
  const sourceId = pendingInsertSourceId.value
  closeAddNodeMenu()
  if (!sourceId) return
  insertNodeAfterSource(sourceId, nodeType)
}

onBeforeUnmount(() => {
  closeAddNodeMenu()
})

const addNodeMenuStyle = computed(() => ({
  left: `${addNodeMenuX.value}px`,
  top: `${addNodeMenuY.value}px`,
}))

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

/** 从「开始」BFS 得到节点顺序（支持并行分叉）；未访问的节点（孤岛等）排在后面 */
function bfsOrderFromStart(def: WorkflowDefinition): string[] {
  const startNode = def.nodes.find((n) => n.nodeType === WorkflowNodeType.START)
  if (!startNode) return def.nodes.map((n) => n.id)

  const adj = new Map<string, string[]>()
  for (const e of def.edges) {
    if (!adj.has(e.sourceNodeId)) adj.set(e.sourceNodeId, [])
    adj.get(e.sourceNodeId)!.push(e.targetNodeId)
  }

  const ordered: string[] = []
  const visited = new Set<string>()
  const queue: string[] = [startNode.id]
  visited.add(startNode.id)

  while (queue.length > 0) {
    const id = queue.shift()!
    ordered.push(id)
    for (const next of adj.get(id) ?? []) {
      if (!visited.has(next)) {
        visited.add(next)
        queue.push(next)
      }
    }
  }

  for (const n of def.nodes) {
    if (!visited.has(n.id)) ordered.push(n.id)
  }
  return ordered
}

function definitionToFlow(def: WorkflowDefinition) {
  const NODE_GAP = 300
  const NODE_Y = 80

  const orderedIds = bfsOrderFromStart(def)

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
    type: 'workflowBezier',
    animated: false,
    deletable: true,
    selectable: true,
    style: { stroke: '#3370FF', strokeWidth: 1.5 },
    pathOptions: { curvature: 0.38 },
    markerEnd: { ...WORKFLOW_EDGE_MARKER_END },
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

/** 将 Vue Flow 的删边同步到 workflow.edges，否则仅改本地 ref，下次布局/保存会还原 */
function handleEdgesChange(changes: EdgeChange[]) {
  const removeIds = new Set<string>()
  for (const c of changes) {
    if (c.type === 'remove') removeIds.add(c.id)
  }
  if (removeIds.size === 0) return

  const newEdges = props.workflow.edges.filter((e) => !removeIds.has(e.id))
  emit('update:workflow', { ...props.workflow, edges: newEdges })
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

  const inEdges = props.workflow.edges.filter((e) => e.targetNodeId === nodeId)
  const outEdges = props.workflow.edges.filter((e) => e.sourceNodeId === nodeId)

  const newNodes = props.workflow.nodes.filter((n) => n.id !== nodeId)
  const newEdges = props.workflow.edges.filter(
    (e) => e.sourceNodeId !== nodeId && e.targetNodeId !== nodeId,
  )

  if (inEdges.length === 1 && outEdges.length === 1) {
    const inEdge = inEdges[0]!
    const outEdge = outEdges[0]!
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
  if (addNodeMenuVisible.value && Date.now() - addNodeMenuOpenedAt.value < 120) {
    return
  }
  closeAddNodeMenu()
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
        :edge-types="edgeTypes"
        :nodes-draggable="true"
        :nodes-connectable="true"
        :is-valid-connection="isValidWorkflowConnection"
        :connection-radius="40"
        :edges-updatable="false"
        :default-edge-options="{ deletable: true, selectable: true }"
        :connection-line-options="workflowConnectionLineOptions"
        :zoom-on-scroll="true"
        :pan-on-scroll="true"
        :fit-view-on-init="true"
        :min-zoom="0.25"
        :max-zoom="2"
        @node-click="handleNodeClick"
        @pane-click="handlePaneClick"
        @viewport-change="onViewportChange"
        @node-drag-stop="onNodeDragStop"
        @edges-change="handleEdgesChange"
        @connect="handleConnect"
        @connect-start="handleConnectStart"
        @connect-end="handleConnectEnd"
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

    <teleport to="body">
      <div
        v-if="addNodeMenuVisible"
        class="wf-add-node-overlay"
        role="presentation"
        @click="closeAddNodeMenu"
      >
        <div class="wf-add-node-menu" :style="addNodeMenuStyle" @click.stop>
          <div class="wf-add-node-menu__title">
            {{ t('admin.workflow.addNodeMenu.title') }}
          </div>
          <button
            type="button"
            class="wf-add-node-menu__btn"
            @click="pickNodeTypeFromMenu(WorkflowNodeType.APPROVAL)"
          >
            <span class="wf-add-node-menu__dot" style="background: #3370ff" />
            {{ t('admin.workflow.nodeType.APPROVAL') }}
          </button>
          <button
            type="button"
            class="wf-add-node-menu__btn"
            @click="pickNodeTypeFromMenu(WorkflowNodeType.AUTO_ACTION)"
          >
            <span class="wf-add-node-menu__dot" style="background: #ff9500" />
            {{ t('admin.workflow.nodeType.AUTO_ACTION') }}
          </button>
        </div>
      </div>
    </teleport>
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
  stroke-width: 1.5;
  fill: none;
}

:deep(.vue-flow__connection-path) {
  stroke-width: 1.5;
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

.wf-add-node-overlay {
  position: fixed;
  inset: 0;
  z-index: 2100;
  background: rgba(0, 0, 0, 0.04);
}

.wf-add-node-menu {
  position: fixed;
  z-index: 2101;
  min-width: 200px;
  padding: 8px 0;
  background: #fff;
  border: 1px solid var(--color-border-1);
  border-radius: 10px;
  box-shadow:
    0 8px 28px rgba(31, 35, 41, 0.12),
    0 0 1px rgba(31, 35, 41, 0.08);
}

.wf-add-node-menu__title {
  padding: 4px 14px 8px;
  font-size: 12px;
  color: var(--color-text-3);
  line-height: 1.4;
}

.wf-add-node-menu__btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 14px;
  margin: 0;
  border: none;
  background: transparent;
  font-size: 13px;
  color: var(--color-text-1);
  text-align: left;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: var(--color-fill-2);
  }
}

.wf-add-node-menu__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
