<script setup lang="ts">
import { ref, computed, onMounted, watch, markRaw, provide, nextTick } from 'vue'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import type { Node, Edge, VueFlowStore, GraphEdge, NodeTypesObject, EdgeTypesObject } from '@vue-flow/core'
import { MarkerType } from '@vue-flow/core'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconFullscreen,
  IconFullscreenExit,
  IconMindMapping,
  IconSearch,
  IconInfoCircle,
  IconQuestionCircle,
} from '@arco-design/web-vue/es/icon'
import CardTypeNode from '@/components/er-diagram/CardTypeNode.vue'
import CardinalityEdge from '@/components/er-diagram/CardinalityEdge.vue'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import ViewSwitcher from '@/components/link-type/ViewSwitcher.vue'
import CardTypeSelect from '@/components/card-type/CardTypeSelect.vue'
import SchemaReferenceDrawer from '@/components/schema/SchemaReferenceDrawer.vue'
import { cardTypeApi, linkTypeApi } from '@/api'
import type { CardTypeDefinition } from '@/types/card-type'
import type { FieldOption } from '@/types/field-option'
import type { LinkTypeVO, CardTypeInfo, UpdateLinkTypeRequest } from '@/types/link-type'

// Vue Flow store reference (will be set when VueFlow is ready)
const vueFlowRef = ref<VueFlowStore | null>(null)

// Data
const loading = ref(false)
const cardTypes = ref<CardTypeDefinition[]>([])
const linkTypes = ref<LinkTypeVO[]>([])
const fieldConfigs = ref<Map<string, FieldOption[]>>(new Map())

// Canvas state
const containerRef = ref<HTMLDivElement | null>(null)
const isFullscreen = ref(false)

// Filter state
const selectedCardTypeIds = ref<string[]>([])
const isFilterMode = computed(() => selectedCardTypeIds.value.length > 0)
const isSingleSelectMode = computed(() => selectedCardTypeIds.value.length === 1)
const selectPopupVisible = ref(false)

// Focus mode state
const isFocusMode = ref(false)
const focusedNodeId = ref<string | null>(null)
const savedNodePositions = ref<Map<string, { x: number; y: number }>>(new Map())

// Vue Flow nodes and edges (original data)
const allNodes = ref<Node[]>([])
const allEdges = ref<Edge[]>([])

// Visible nodes and edges (filtered)
const nodes = ref<Node[]>([])
const edges = ref<Edge[]>([])

// Node types registration
 
const nodeTypes: NodeTypesObject = {
  cardType: markRaw(CardTypeNode) as any,
}

// Edge types registration
 
const edgeTypes: EdgeTypesObject = {
  cardinality: markRaw(CardinalityEdge) as any,
}

// Build concrete type lookups
const abstractToConcreteMap = computed(() => {
  const map = new Map<string, CardTypeInfo[]>()

  for (const ct of cardTypes.value) {
    if (ct.schemaSubType === 'ENTITY_CARD_TYPE' && 'parentTypeIds' in ct && ct.parentTypeIds) {
      for (const parentId of ct.parentTypeIds) {
        if (!map.has(parentId)) {
          map.set(parentId, [])
        }
        map.get(parentId)!.push({ id: ct.id!, name: ct.name })
      }
    }
  }

  return map
})

// Card type options for filter dropdown
const cardTypeOptions = computed(() => {
  return cardTypes.value
    .map(ct => ({
      value: ct.id!,
      label: ct.name,
    }))
    .sort((a, b) => a.label.localeCompare(b.label))
})

// Helper function to generate consistent node key
function generateNodeKey(cardTypeList: CardTypeInfo[]): string {
  const sortedIds = cardTypeList.map(ct => ct.id).sort()
  return sortedIds.length > 1 ? `combo-${sortedIds.join('-')}` : sortedIds[0] ?? ''
}

// Calculate best handles based on relative node positions
function calculateHandles(
  sourcePos: { x: number; y: number },
  targetPos: { x: number; y: number },
  nodeWidth: number,
  nodeHeight: number
): { sourceHandle: string; targetHandle: string } {
  const sourceCenterX = sourcePos.x + nodeWidth / 2
  const sourceCenterY = sourcePos.y + nodeHeight / 2
  const targetCenterX = targetPos.x + nodeWidth / 2
  const targetCenterY = targetPos.y + nodeHeight / 2

  const dx = targetCenterX - sourceCenterX
  const dy = targetCenterY - sourceCenterY

  // Determine if connection is more horizontal or vertical
  const isHorizontal = Math.abs(dx) > Math.abs(dy)

  if (isHorizontal) {
    // Horizontal connection: use left/right handles
    if (dx > 0) {
      return { sourceHandle: 'right', targetHandle: 'left' }
    } else {
      return { sourceHandle: 'left', targetHandle: 'right' }
    }
  } else {
    // Vertical connection: use top/bottom handles
    if (dy > 0) {
      return { sourceHandle: 'bottom', targetHandle: 'top' }
    } else {
      return { sourceHandle: 'top', targetHandle: 'bottom' }
    }
  }
}

// Find concrete implementations for a set of abstract types
function findConcreteImplementations(abstractTypes: CardTypeInfo[]): CardTypeInfo[] {
  const concretes = new Map<string, CardTypeInfo>()

  for (const abstract of abstractTypes) {
    const impls = abstractToConcreteMap.value.get(abstract.id) || []
    for (const impl of impls) {
      concretes.set(impl.id, impl)
    }
  }

  return Array.from(concretes.values())
}

// Map from card type ID to node IDs (use allNodes to avoid circular dependency)
const cardTypeToNodeIds = computed(() => {
  const map = new Map<string, Set<string>>()

  for (const node of allNodes.value) {
    const data = node.data
    if (data.mode === 'single' && data.entityId) {
      if (!map.has(data.entityId)) {
        map.set(data.entityId, new Set())
      }
      map.get(data.entityId)!.add(node.id)
    } else if (data.mode === 'combo') {
      if (data.abstractTypes) {
        for (const at of data.abstractTypes) {
          if (!map.has(at.id)) {
            map.set(at.id, new Set())
          }
          map.get(at.id)!.add(node.id)
        }
      }
    }
  }

  return map
})

// Compute filtered node IDs (use allEdges to avoid circular dependency)
const filteredNodeIds = computed<Set<string>>(() => {
  if (!isFilterMode.value) return new Set()

  const selectedNodeIds = new Set<string>()
  for (const cardTypeId of selectedCardTypeIds.value) {
    const nodeIds = cardTypeToNodeIds.value.get(cardTypeId)
    if (nodeIds) {
      nodeIds.forEach(id => selectedNodeIds.add(id))
    }
  }

  if (isSingleSelectMode.value) {
    const related = new Set<string>(selectedNodeIds)

    for (const nodeId of selectedNodeIds) {
      for (const edge of allEdges.value) {
        if (edge.source === nodeId) {
          related.add(edge.target)
        } else if (edge.target === nodeId) {
          related.add(edge.source)
        }
      }
    }
    return related
  }

  return selectedNodeIds
})

// Check if a node should be highlighted (directly selected)
function isNodeHighlighted(_nodeId: string, data: Record<string, unknown>): boolean {
  if (!isFilterMode.value) return false

  if (data.mode === 'single' && data.entityId) {
    return selectedCardTypeIds.value.includes(data.entityId as string)
  }

  if (data.mode === 'combo' && data.abstractTypes) {
    return (data.abstractTypes as CardTypeInfo[]).some(at =>
      selectedCardTypeIds.value.includes(at.id)
    )
  }

  return false
}

// Build Vue Flow nodes and edges from data
function buildGraph() {
  const newNodes: Node[] = []
  const newEdges: Edge[] = []
  const processedKeys = new Set<string>()
  const nodePositions = new Map<string, { x: number; y: number }>()

  // Grid layout parameters
  const startX = 100
  const startY = 100
  const nodeWidth = 280
  const nodeHeight = 200
  const horizontalGap = 150
  const verticalGap = 100
  const nodesPerRow = 4

  let nodeIndex = 0

  // Helper to calculate and store position
  const getNodePosition = (index: number) => ({
    x: startX + (index % nodesPerRow) * (nodeWidth + horizontalGap),
    y: startY + Math.floor(index / nodesPerRow) * (nodeHeight + verticalGap),
  })

  for (const linkType of linkTypes.value) {
    // Process source side
    if (linkType.sourceCardTypes && linkType.sourceCardTypes.length > 0) {
      const nodeKey = generateNodeKey(linkType.sourceCardTypes)

      if (!processedKeys.has(nodeKey)) {
        processedKeys.add(nodeKey)
        const position = getNodePosition(nodeIndex)
        nodePositions.set(nodeKey, position)

        if (linkType.sourceCardTypes.length === 1) {
          const ct = linkType.sourceCardTypes[0]
          if (ct) {
            newNodes.push({
              id: ct.id,
              type: 'cardType',
              position,
              data: {
                mode: 'single',
                entityId: ct.id,
                entityName: ct.name,
                fields: [],
                linkFields: [],
                fieldsLoaded: false,
                fieldsLoading: false,
              },
            })
          }
        } else {
          const concretes = findConcreteImplementations(linkType.sourceCardTypes)
          newNodes.push({
            id: nodeKey,
            type: 'cardType',
            position,
            data: {
              mode: 'combo',
              entityId: nodeKey,
              abstractTypes: linkType.sourceCardTypes,
              concreteTypes: concretes,
            },
          })
        }
        nodeIndex++
      }
    }

    // Process target side
    if (linkType.targetCardTypes && linkType.targetCardTypes.length > 0) {
      const nodeKey = generateNodeKey(linkType.targetCardTypes)

      if (!processedKeys.has(nodeKey)) {
        processedKeys.add(nodeKey)
        const position = getNodePosition(nodeIndex)
        nodePositions.set(nodeKey, position)

        if (linkType.targetCardTypes.length === 1) {
          const ct = linkType.targetCardTypes[0]
          if (ct) {
            newNodes.push({
              id: ct.id,
              type: 'cardType',
              position,
              data: {
                mode: 'single',
                entityId: ct.id,
                entityName: ct.name,
                fields: [],
                linkFields: [],
                fieldsLoaded: false,
                fieldsLoading: false,
              },
            })
          }
        } else {
          const concretes = findConcreteImplementations(linkType.targetCardTypes)
          newNodes.push({
            id: nodeKey,
            type: 'cardType',
            position,
            data: {
              mode: 'combo',
              entityId: nodeKey,
              abstractTypes: linkType.targetCardTypes,
              concreteTypes: concretes,
            },
          })
        }
        nodeIndex++
      }
    }

    // Create edge with calculated handles
    if (linkType.sourceCardTypes?.length && linkType.targetCardTypes?.length) {
      const sourceKey = generateNodeKey(linkType.sourceCardTypes)
      const targetKey = generateNodeKey(linkType.targetCardTypes)

      const sourcePos = nodePositions.get(sourceKey)
      const targetPos = nodePositions.get(targetKey)

      let sourceHandle = 'right'
      let targetHandle = 'left'

      if (sourcePos && targetPos) {
        const handles = calculateHandles(sourcePos, targetPos, nodeWidth, nodeHeight)
        sourceHandle = handles.sourceHandle
        targetHandle = handles.targetHandle
      }

      newEdges.push({
        id: `edge-${linkType.id}`,
        source: sourceKey,
        target: targetKey,
        sourceHandle,
        targetHandle,
        type: 'cardinality',
        animated: false,
        label: `${linkType.sourceName} → ${linkType.targetName}`,
        style: { stroke: '#b8c5d3', strokeWidth: 1.5 },
        markerEnd: {
          type: MarkerType.ArrowClosed,
          color: '#b8c5d3',
        },
        data: {
          sourceMulti: linkType.sourceMultiSelect,
          targetMulti: linkType.targetMultiSelect,
        },
      })
    }
  }

  // Calculate handle variants for edges between same node pairs to avoid overlap
  const edgePairCount = new Map<string, number>()
  const edgePairIndex = new Map<string, number>()

  // First pass: count edges between each node pair
  for (const edge of newEdges) {
    const pairKey = [edge.source, edge.target].sort().join('|')
    edgePairCount.set(pairKey, (edgePairCount.get(pairKey) || 0) + 1)
  }

  // Handle variants: for horizontal edges use top/bottom, for vertical use left/right
  const horizontalVariants = ['-top', '', '-bottom'] // for left/right handles
  const verticalVariants = ['-left', '', '-right']   // for top/bottom handles

  // Second pass: assign different handles to edges with multiple connections
  for (let i = 0; i < newEdges.length; i++) {
    const edge = newEdges[i]
    if (!edge) continue

    const pairKey = [edge.source, edge.target].sort().join('|')
    const totalCount = edgePairCount.get(pairKey) || 1

    if (totalCount > 1) {
      const currentIndex = edgePairIndex.get(pairKey) || 0
      edgePairIndex.set(pairKey, currentIndex + 1)

      // Check if this is a vertical connection
      const isVertical = edge.sourceHandle === 'top' || edge.sourceHandle === 'bottom'
      const variants = isVertical ? verticalVariants : horizontalVariants
      const variantIndex = currentIndex % variants.length
      const suffix = variants[variantIndex]

      // Update handles with variant
      const baseSourceHandle = edge.sourceHandle || 'right'
      const baseTargetHandle = edge.targetHandle || 'left'

      newEdges[i] = {
        ...edge,
        sourceHandle: `${baseSourceHandle}${suffix}`,
        targetHandle: `${baseTargetHandle}${suffix}`,
      }
    }
  }

  // Store original data
  allNodes.value = newNodes
  allEdges.value = newEdges

  // Initialize visible nodes and edges
  nodes.value = [...newNodes]
  edges.value = [...newEdges]
}

// Apply filter when selectedCardTypeIds changes
function applyFilter() {
  if (!isFilterMode.value) {
    // No filter - show all nodes and edges
    nodes.value = allNodes.value.map(node => ({
      ...node,
      hidden: false,
      data: { ...node.data, isHighlighted: false },
    }))
    edges.value = allEdges.value.map(edge => ({
      ...edge,
      hidden: false,
    }))
  } else {
    // Apply filter
    const visibleNodeIds = filteredNodeIds.value

    nodes.value = allNodes.value.map(node => ({
      ...node,
      hidden: !visibleNodeIds.has(node.id),
      data: {
        ...node.data,
        isHighlighted: isNodeHighlighted(node.id, node.data),
      },
    }))

    edges.value = allEdges.value.map(edge => ({
      ...edge,
      hidden: !visibleNodeIds.has(edge.source) || !visibleNodeIds.has(edge.target),
    }))
  }
}

watch(selectedCardTypeIds, applyFilter, { deep: true })

// Watch nodes position changes to update edge handles
watch(
  nodes,
  () => {
    if (isFocusMode.value) return
    updateEdgeHandles()
  },
  { deep: true }
)

// Update edge handles based on current node positions
function updateEdgeHandles() {
  if (!vueFlowRef.value) return

  const nodePositions = new Map<string, { x: number; y: number }>()
  for (const node of nodes.value) {
    nodePositions.set(node.id, { ...node.position })
  }

  const nodeWidth = 280
  const nodeHeight = 200

  // Count edges between same node pairs
  const edgePairCount = new Map<string, number>()
  for (const edge of edges.value) {
    const pairKey = [edge.source, edge.target].sort().join('|')
    edgePairCount.set(pairKey, (edgePairCount.get(pairKey) || 0) + 1)
  }

  // Handle variants: for horizontal edges use top/bottom, for vertical use left/right
  const horizontalVariants = ['-top', '', '-bottom'] // for left/right handles
  const verticalVariants = ['-left', '', '-right']   // for top/bottom handles

  const edgePairIndex = new Map<string, number>()

  let hasChanges = false

  // Check and update handles
  const updatedEdges = edges.value.map(edge => {
    const sourcePos = nodePositions.get(edge.source)
    const targetPos = nodePositions.get(edge.target)

    let sourceHandle = 'right'
    let targetHandle = 'left'
    let isVertical = false

    if (sourcePos && targetPos) {
      const handles = calculateHandles(sourcePos, targetPos, nodeWidth, nodeHeight)
      sourceHandle = handles.sourceHandle
      targetHandle = handles.targetHandle
      // Check if this is a vertical connection
      isVertical = sourceHandle === 'top' || sourceHandle === 'bottom'
    }

    // Handle parallel edges
    const pairKey = [edge.source, edge.target].sort().join('|')
    const totalCount = edgePairCount.get(pairKey) || 1

    if (totalCount > 1) {
      const currentIndex = edgePairIndex.get(pairKey) || 0
      edgePairIndex.set(pairKey, currentIndex + 1)

      // Use appropriate variants based on direction
      const variants = isVertical ? verticalVariants : horizontalVariants
      const variantIndex = currentIndex % variants.length
      const suffix = variants[variantIndex]

      sourceHandle = `${sourceHandle}${suffix}`
      targetHandle = `${targetHandle}${suffix}`
    }

    // Check if handles changed
    if (edge.sourceHandle !== sourceHandle || edge.targetHandle !== targetHandle) {
      hasChanges = true
    }

    return {
      ...edge,
      sourceHandle,
      targetHandle,
    }
  })

  if (hasChanges) {
    vueFlowRef.value.setEdges(updatedEdges)
  }
}

// Track loading state for field configs
const fieldsLoadingSet = ref<Set<string>>(new Set())

// Lazy load field configs for a specific card type
async function loadFieldConfigs(cardTypeId: string) {
  if (fieldConfigs.value.has(cardTypeId) || fieldsLoadingSet.value.has(cardTypeId)) {
    return
  }

  fieldsLoadingSet.value.add(cardTypeId)
  updateNodeFieldsState(cardTypeId, true)

  try {
    const fields = await cardTypeApi.getFieldOptions(cardTypeId)
    fieldConfigs.value.set(cardTypeId, fields)
    updateNodeFieldsData(cardTypeId, fields)
  } catch {
    console.warn(`Failed to fetch fields for ${cardTypeId}`)
  } finally {
    fieldsLoadingSet.value.delete(cardTypeId)
    updateNodeFieldsState(cardTypeId, false)
  }
}

// Update node's data helper
function updateNodeData(cardTypeId: string, dataUpdater: (data: Record<string, unknown>) => Record<string, unknown>) {
  const updateMapper = (node: Node) => {
    if (node.data?.entityId === cardTypeId) {
      return {
        ...node,
        data: dataUpdater(node.data)
      }
    }
    return node
  }

  // Update allNodes (source of truth)
  allNodes.value = allNodes.value.map(updateMapper)

  // Update visible nodes
  const updatedNodes = nodes.value.map(updateMapper)

  // Use vue-flow API in focus mode to avoid state sync issues
  if (isFocusMode.value && vueFlowRef.value) {
    vueFlowRef.value.setNodes(updatedNodes)
  } else {
    nodes.value = updatedNodes
  }
}

// Update node's loading state
function updateNodeFieldsState(cardTypeId: string, isLoading: boolean) {
  updateNodeData(cardTypeId, (data) => ({
    ...data,
    fieldsLoading: isLoading,
    fieldsLoaded: !isLoading && fieldConfigs.value.has(cardTypeId)
  }))
}

// Update node's field data after loading
function updateNodeFieldsData(cardTypeId: string, fields: FieldOption[]) {
  const linkFields = fields.filter(f => f.fieldType === 'LINK')
  updateNodeData(cardTypeId, (data) => ({
    ...data,
    fields,
    linkFields,
    fieldsLoaded: true,
    fieldsLoading: false
  }))
}

// Provide loadFieldConfigs for child components
provide('loadFieldConfigs', loadFieldConfigs)

// Edit drawer state
const editDrawerVisible = ref(false)
const editingLinkType = ref<LinkTypeVO | null>(null)
const editingVersion = ref<number>(1)
const submitting = ref(false)
const formData = ref<UpdateLinkTypeRequest & { id: string }>({
  id: '',
  sourceName: '',
  targetName: '',
  sourceVisible: true,
  targetVisible: true,
  sourceMultiSelect: true,
  targetMultiSelect: true,
})

// Reference drawer state
const referenceDrawerVisible = ref(false)
const currentReferenceSchemaId = ref<string | undefined>(undefined)

// Open edit drawer
async function openEditDrawer(linkTypeId: string) {
  try {
    const detail = await linkTypeApi.getById(linkTypeId)
    editingLinkType.value = detail as LinkTypeVO
    editingVersion.value = detail.contentVersion
    formData.value = {
      id: detail.id,
      name: detail.name,
      description: detail.description,
      sourceName: detail.sourceName,
      targetName: detail.targetName,
      sourceVisible: detail.sourceVisible,
      targetVisible: detail.targetVisible,
      sourceCardTypeIds: detail.sourceCardTypes?.map((ct) => ct.id),
      targetCardTypeIds: detail.targetCardTypes?.map((ct) => ct.id),
      sourceMultiSelect: detail.sourceMultiSelect,
      targetMultiSelect: detail.targetMultiSelect,
      enabled: detail.enabled,
      expectedVersion: detail.contentVersion,
    }
    editDrawerVisible.value = true
  } catch (error) {
    console.error('Failed to fetch link type:', error)
    Message.error('获取关联类型详情失败')
  }
}

// Submit edit form
async function handleEditSubmit() {
  if (!formData.value.sourceName || !formData.value.targetName) {
    Message.error('请填写源端和目标端名称')
    return
  }
  if (!formData.value.sourceCardTypeIds || formData.value.sourceCardTypeIds.length === 0) {
    Message.error('请选择源端卡片类型')
    return
  }
  if (!formData.value.targetCardTypeIds || formData.value.targetCardTypeIds.length === 0) {
    Message.error('请选择目标端卡片类型')
    return
  }

  submitting.value = true
  try {
    await linkTypeApi.update(formData.value.id, {
      ...formData.value,
      expectedVersion: editingVersion.value,
    } as UpdateLinkTypeRequest)
    Message.success('保存成功')
    editDrawerVisible.value = false
    await fetchData()
  } catch (error) {
    console.error('Failed to update:', error)
  } finally {
    submitting.value = false
  }
}

// Edge action handlers
function handleEdgeEdit(edgeId: string) {
  const linkTypeId = edgeId.replace('edge-', '')
  openEditDrawer(linkTypeId)
}

function handleEdgeShowReferences(edgeId: string) {
  const linkTypeId = edgeId.replace('edge-', '')
  currentReferenceSchemaId.value = linkTypeId
  referenceDrawerVisible.value = true
}

async function handleEdgeDisable(edgeId: string) {
  const linkTypeId = edgeId.replace('edge-', '')
  const linkType = linkTypes.value.find(lt => lt.id === linkTypeId)
  if (linkType) {
    try {
      await linkTypeApi.update(linkTypeId, {
        enabled: !linkType.enabled,
        expectedVersion: linkType.contentVersion,
      })
      Message.success(linkType.enabled ? '已停用' : '已启用')
      await fetchData()
    } catch (error) {
      console.error('Failed to toggle enabled:', error)
    }
  }
}

import { handleReferenceConflictError } from '@/utils/error-handler'

function handleEdgeDelete(edgeId: string) {
  const linkTypeId = edgeId.replace('edge-', '')
  const linkType = linkTypes.value.find(lt => lt.id === linkTypeId)
  if (linkType) {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除关联类型「${linkType.sourceName} → ${linkType.targetName}」吗？删除后不可恢复。`,
      okText: '删除',
      okButtonProps: { status: 'danger' },
      modalClass: 'arco-modal-simple', // 使用全局优化样式
      async onOk() {
        try {
          await linkTypeApi.delete(linkTypeId)
          Message.success('删除成功')
          await fetchData()
        } catch (error) {
          if (!handleReferenceConflictError(error)) {
            console.error('Failed to delete:', error)
          }
        }
      },
    })
  }
}

// Quick update for inline edit (name + multiSelect)
async function handleEdgeQuickUpdate(
  edgeId: string,
  side: 'source' | 'target',
  name: string,
  multiSelect: boolean
) {
  const linkTypeId = edgeId.replace('edge-', '')
  const linkType = linkTypes.value.find(lt => lt.id === linkTypeId)
  if (!linkType) return

  try {
    const updateData: UpdateLinkTypeRequest = {
      expectedVersion: linkType.contentVersion,
    }

    if (side === 'source') {
      updateData.sourceName = name
      updateData.sourceMultiSelect = multiSelect
    } else {
      updateData.targetName = name
      updateData.targetMultiSelect = multiSelect
    }

    await linkTypeApi.update(linkTypeId, updateData)
    Message.success('保存成功')
    await fetchData()
  } catch (error) {
    console.error('Failed to quick update:', error)
    Message.error('保存失败')
  }
}

// Provide edge action handlers for CardinalityEdge component
provide('edgeActionHandlers', {
  onEdit: handleEdgeEdit,
  onShowReferences: handleEdgeShowReferences,
  onDisable: handleEdgeDisable,
  onDelete: handleEdgeDelete,
  onQuickUpdate: handleEdgeQuickUpdate,
})

// Fetch data from APIs
async function fetchData() {
  loading.value = true
  try {
    const [types, links] = await Promise.all([cardTypeApi.list(), linkTypeApi.list()])

    cardTypes.value = types
    linkTypes.value = links

    buildGraph()

    // Fit view after graph is built
    setTimeout(() => {
      vueFlowRef.value?.fitView({ padding: 0.2 })
    }, 100)
  } catch (error) {
    console.error('Failed to fetch data:', error)
    Message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// Clear filter
function clearFilter() {
  selectedCardTypeIds.value = []
}

// Toggle fullscreen
function toggleFullscreen() {
  if (!containerRef.value) return

  if (!isFullscreen.value) {
    containerRef.value.requestFullscreen?.()
    isFullscreen.value = true
  } else {
    document.exitFullscreen?.()
    isFullscreen.value = false
  }
}

// Handle pane ready - store the vue flow instance
function onPaneReady(instance: VueFlowStore) {
  vueFlowRef.value = instance
}

// Handle node drag stop - recalculate edge handles
function onNodeDragStop() {
  if (!vueFlowRef.value || isFocusMode.value) return

  const { getNodes, getEdges } = vueFlowRef.value
  const currentNodes = getNodes ?? []
  const currentEdges = getEdges ?? []

  if (!currentNodes.length || !currentEdges.length) return

  // Build position map from current nodes
  const nodePositions = new Map<string, { x: number; y: number }>()
  for (const node of currentNodes) {
    nodePositions.set(node.id, { ...node.position })
  }

  const nodeWidth = 280
  const nodeHeight = 200

  // Count edges between same node pairs
  const edgePairCount = new Map<string, number>()
  for (const edge of currentEdges) {
    const pairKey = [edge.source, edge.target].sort().join('|')
    edgePairCount.set(pairKey, (edgePairCount.get(pairKey) || 0) + 1)
  }

  const handleVariants = ['top', '', 'bottom']
  const edgePairIndex = new Map<string, number>()

  // Recalculate handles for all edges
  const updatedEdges = currentEdges.map((edge: GraphEdge) => {
    const sourcePos = nodePositions.get(edge.source)
    const targetPos = nodePositions.get(edge.target)

    let sourceHandle = 'right'
    let targetHandle = 'left'

    if (sourcePos && targetPos) {
      const handles = calculateHandles(sourcePos, targetPos, nodeWidth, nodeHeight)
      sourceHandle = handles.sourceHandle
      targetHandle = handles.targetHandle
    }

    // Handle parallel edges
    const pairKey = [edge.source, edge.target].sort().join('|')
    const totalCount = edgePairCount.get(pairKey) || 1

    if (totalCount > 1) {
      const currentIndex = edgePairIndex.get(pairKey) || 0
      edgePairIndex.set(pairKey, currentIndex + 1)

      const variantIndex = currentIndex % handleVariants.length
      const variant = handleVariants[variantIndex]
      const suffix = variant ? `-${variant}` : ''

      sourceHandle = `${sourceHandle}${suffix}`
      targetHandle = `${targetHandle}${suffix}`
    }

    return {
      ...edge,
      sourceHandle,
      targetHandle,
    }
  })

  // Update edges directly
  edges.value = updatedEdges
}

// Auto layout
function handleAutoLayout() {
  if (isFocusMode.value) {
    exitFocusMode()
  }
  buildGraph()
  setTimeout(() => {
    vueFlowRef.value?.fitView({ padding: 0.2 })
  }, 100)
  Message.success('已重新排列布局')
}

// Handle node double click - enter focus mode
function onNodeDoubleClick({ node }: { node: Node }) {
  if (isFocusMode.value && focusedNodeId.value === node.id) {
    // Double click on same node exits focus mode
    exitFocusMode()
    return
  }

  enterFocusMode(node.id)
}

// Enter focus mode
function enterFocusMode(nodeId: string) {
  // Check if we have data
  if (allNodes.value.length === 0) return

  // Clear filter mode first
  selectedCardTypeIds.value = []

  // Save current positions from allNodes (the source of truth)
  savedNodePositions.value.clear()
  for (const node of allNodes.value) {
    savedNodePositions.value.set(node.id, { ...node.position })
  }

  isFocusMode.value = true
  focusedNodeId.value = nodeId

  // Find related nodes (connected via edges)
  const relatedNodeIds = new Set<string>()
  relatedNodeIds.add(nodeId)

  for (const edge of allEdges.value) {
    if (edge.source === nodeId) {
      relatedNodeIds.add(edge.target)
    } else if (edge.target === nodeId) {
      relatedNodeIds.add(edge.source)
    }
  }

  // Layout parameters
  const nodeWidth = 280
  const nodeHeight = 200
  const horizontalGap = 200
  const verticalGap = 40
  const startX = 100
  const startY = 100

  // Calculate positions: focused node on left, related nodes on right vertically
  const newPositions = new Map<string, { x: number; y: number }>()

  // Center the focused node vertically based on number of related nodes
  const relatedNodes = Array.from(relatedNodeIds).filter(id => id !== nodeId)
  const totalHeight = relatedNodes.length * (nodeHeight + verticalGap) - verticalGap
  const focusedY = startY + Math.max(0, (totalHeight - nodeHeight) / 2)

  newPositions.set(nodeId, { x: startX, y: focusedY })

  // Position related nodes vertically on the right
  relatedNodes.forEach((id, index) => {
    newPositions.set(id, {
      x: startX + nodeWidth + horizontalGap,
      y: startY + index * (nodeHeight + verticalGap),
    })
  })

  // Update nodes: hide non-related, reposition related
  const updatedNodes = allNodes.value.map(node => {
    const isRelated = relatedNodeIds.has(node.id)
    const newPos = newPositions.get(node.id)

    return {
      ...node,
      hidden: !isRelated,
      position: newPos || node.position,
      data: {
        ...node.data,
        isHighlighted: node.id === nodeId,
      },
    }
  })

  // Update edges: recalculate handles and hide non-related
  const updatedEdges = allEdges.value.map(edge => {
    const isVisible = relatedNodeIds.has(edge.source) && relatedNodeIds.has(edge.target)

    if (!isVisible) {
      return { ...edge, hidden: true }
    }

    // Recalculate handles for visible edges
    const sourcePos = newPositions.get(edge.source)
    const targetPos = newPositions.get(edge.target)

    let sourceHandle = 'right'
    let targetHandle = 'left'

    if (sourcePos && targetPos) {
      const handles = calculateHandles(sourcePos, targetPos, nodeWidth, nodeHeight)
      sourceHandle = handles.sourceHandle
      targetHandle = handles.targetHandle
    }

    return {
      ...edge,
      hidden: false,
      sourceHandle,
      targetHandle,
    }
  })

  // Update via vue-flow API if available, otherwise use v-model
  if (vueFlowRef.value) {
    vueFlowRef.value.setNodes(updatedNodes)
    vueFlowRef.value.setEdges(updatedEdges)
  } else {
    nodes.value = updatedNodes
    edges.value = updatedEdges
  }

  // Fit view to show focused content after DOM update
  nextTick(() => {
    setTimeout(() => {
      vueFlowRef.value?.fitView({ padding: 0.3 })
    }, 50)
  })
}

// Exit focus mode - restore original layout
function exitFocusMode() {
  if (!isFocusMode.value) return

  isFocusMode.value = false
  focusedNodeId.value = null

  // Restore original positions
  const restoredNodes = allNodes.value.map(node => {
    const savedPos = savedNodePositions.value.get(node.id)
    return {
      ...node,
      hidden: false,
      position: savedPos || node.position,
      data: {
        ...node.data,
        isHighlighted: false,
      },
    }
  })

  // Update via v-model bound ref
  nodes.value = restoredNodes

  // Restore edges with recalculated handles
  const nodeWidth = 280
  const nodeHeight = 200

  const restoredEdges = allEdges.value.map(edge => {
    const sourcePos = savedNodePositions.value.get(edge.source)
    const targetPos = savedNodePositions.value.get(edge.target)

    let sourceHandle = 'right'
    let targetHandle = 'left'

    if (sourcePos && targetPos) {
      const handles = calculateHandles(sourcePos, targetPos, nodeWidth, nodeHeight)
      sourceHandle = handles.sourceHandle
      targetHandle = handles.targetHandle
    }

    return {
      ...edge,
      hidden: false,
      sourceHandle,
      targetHandle,
    }
  })

  // Update via vue-flow API if available, otherwise use v-model
  if (vueFlowRef.value) {
    vueFlowRef.value.setNodes(restoredNodes)
    vueFlowRef.value.setEdges(restoredEdges)
  } else {
    nodes.value = restoredNodes
    edges.value = restoredEdges
  }

  // Fit view after DOM update
  nextTick(() => {
    setTimeout(() => {
      vueFlowRef.value?.fitView({ padding: 0.2 })
    }, 50)
  })

  savedNodePositions.value.clear()
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div ref="containerRef" class="er-diagram-page">
    <!-- Top Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <ViewSwitcher />
        <div class="toolbar-divider"></div>
        <!-- Filter Dropdown -->
        <div class="filter-box">
          <a-select
            v-model="selectedCardTypeIds"
            v-model:popup-visible="selectPopupVisible"
            placeholder="选择卡片类型筛选..."
            size="small"
            multiple
            allow-clear
            allow-search
            :style="{ width: '280px' }"
            :options="cardTypeOptions"
            :max-tag-count="2"
            @clear="clearFilter"
          >
            <template #prefix>
              <IconSearch />
            </template>
          </a-select>
        </div>
        <!-- Filter Mode Indicator -->
        <a-tag v-if="isFilterMode" color="green" closable class="mode-tag" @close="clearFilter">
          {{ isSingleSelectMode ? '显示关联节点' : '仅显示所选节点' }} ({{ selectedCardTypeIds.length }} 个)
        </a-tag>
        <!-- Focus Mode Indicator -->
        <a-tag v-if="isFocusMode" color="arcoblue" closable class="mode-tag" @close="exitFocusMode">
          聚焦模式
        </a-tag>
      </div>
      <div class="toolbar-right">
        <a-popover position="bottom" trigger="click" :content-style="{ padding: '16px', maxWidth: '320px' }">
          <a-button size="small">
            <template #icon><IconQuestionCircle /></template>
          </a-button>
          <template #content>
            <div class="help-content">
              <div class="help-title">操作说明</div>
              <div class="help-section">
                <div class="help-subtitle">节点操作</div>
                <ul class="help-list">
                  <li><span class="help-action">拖拽节点</span>移动位置</li>
                  <li><span class="help-action">双击节点</span>进入聚焦模式，查看关联关系</li>
                </ul>
              </div>
              <div class="help-section">
                <div class="help-subtitle">连线操作</div>
                <ul class="help-list">
                  <li><span class="help-action">单击连线/标签</span>选中连线</li>
                  <li><span class="help-action">双击连线</span>弹出操作菜单（编辑、引用关系、停用、删除）</li>
                  <li><span class="help-action">双击标签</span>快捷编辑名称和多重性</li>
                </ul>
              </div>
              <div class="help-section">
                <div class="help-subtitle">画布操作</div>
                <ul class="help-list">
                  <li><span class="help-action">滚轮</span>缩放画布</li>
                  <li><span class="help-action">拖拽空白区域</span>平移画布</li>
                </ul>
              </div>
            </div>
          </template>
        </a-popover>
        <a-divider direction="vertical" />
        <a-button size="small" @click="handleAutoLayout">
          <template #icon><IconMindMapping /></template>
        </a-button>
        <a-divider direction="vertical" />
        <a-button size="small" @click="toggleFullscreen">
          <template #icon>
            <IconFullscreenExit v-if="isFullscreen" />
            <IconFullscreen v-else />
          </template>
        </a-button>
      </div>
    </div>

    <!-- Vue Flow Canvas -->
    <div class="flow-container" @click="selectPopupVisible = false">
      <VueFlow
        v-model:nodes="nodes"
        v-model:edges="edges"
        :node-types="nodeTypes"
        :edge-types="edgeTypes"
        :default-viewport="{ zoom: 1, x: 0, y: 0 }"
        :min-zoom="0.2"
        :max-zoom="2"
        :auto-pan-on-node-drag="false"
        fit-view-on-init
        class="vue-flow-wrapper"
        @pane-ready="onPaneReady"
        @node-double-click="onNodeDoubleClick"
        @node-drag-stop="onNodeDragStop"
      >
        <Background pattern-color="#e2e8f0" :gap="20" />
        <Controls position="bottom-right" />
        <MiniMap position="bottom-left" />
      </VueFlow>

      <!-- Loading Overlay -->
      <div v-if="loading" class="loading-overlay">
        <a-spin :loading="true" tip="加载中..." />
      </div>

      <!-- Empty State -->
      <div v-if="!loading && nodes.length === 0" class="empty-state">
        <a-empty description="暂无关联类型数据" />
      </div>
    </div>

    <!-- Edit Drawer -->
    <a-drawer
      v-model:visible="editDrawerVisible"
      title="编辑关联类型"
      :width="600"
      :mask-closable="true"
      :esc-to-close="true"
      unmount-on-close
    >
      <a-form :model="formData" layout="vertical">
        <!-- 关联关系定义 -->
        <div class="form-section-header">
          <span>关联关系定义</span>
          <a-tooltip content="定义两端卡片类型之间的关联关系">
            <IconInfoCircle class="help-icon" />
          </a-tooltip>
        </div>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="源端名称" required>
              <a-input v-model="formData.sourceName" placeholder="如：所属史诗" :max-length="20" />
              <template #extra>
                <div class="form-extra-text">从源端卡片看向目标端的关系名称</div>
              </template>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="目标端名称" required>
              <a-input v-model="formData.targetName" placeholder="如：关联用户故事" :max-length="20" />
              <template #extra>
                <div class="form-extra-text">从目标端卡片看向源端的关系名称</div>
              </template>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="源端卡片类型" required>
              <CardTypeSelect
                v-model="formData.sourceCardTypeIds"
                placeholder="请选择卡片类型"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="目标端卡片类型" required>
              <CardTypeSelect
                v-model="formData.targetCardTypeIds"
                placeholder="请选择卡片类型"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider>显示设置</a-divider>

        <div class="setting-options">
          <div class="setting-item">
            <div class="setting-label">
              <span class="setting-title">源端显示</span>
              <span class="setting-desc">在源端显示关联关系</span>
            </div>
            <a-switch v-model="formData.sourceVisible" />
          </div>
          <div class="setting-item">
            <div class="setting-label">
              <span class="setting-title">目标端显示</span>
              <span class="setting-desc">在目标端显示关联关系</span>
            </div>
            <a-switch v-model="formData.targetVisible" />
          </div>
          <div class="setting-item">
            <div class="setting-label">
              <span class="setting-title">源端多选</span>
              <span class="setting-desc">源端可选择多个目标</span>
            </div>
            <a-switch v-model="formData.sourceMultiSelect" />
          </div>
          <div class="setting-item">
            <div class="setting-label">
              <span class="setting-title">目标端多选</span>
              <span class="setting-desc">目标端可选择多个源</span>
            </div>
            <a-switch v-model="formData.targetMultiSelect" />
          </div>
        </div>

        <a-divider />

        <a-form-item label="描述">
          <a-textarea
            v-model="formData.description"
            placeholder="请输入描述"
            :max-length="200"
            :auto-size="{ minRows: 2, maxRows: 4 }"
          />
        </a-form-item>
      </a-form>

      <template #footer>
        <a-space>
          <CancelButton @click="editDrawerVisible = false" />
          <SaveButton
            :loading="submitting"
            text="保存"
            @click="handleEditSubmit"
          />
        </a-space>
      </template>
    </a-drawer>

    <!-- Reference Drawer -->
    <SchemaReferenceDrawer
      v-model:visible="referenceDrawerVisible"
      :schema-id="currentReferenceSchemaId"
    />
  </div>
</template>

<style scoped>
.er-diagram-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(135deg, #f8fbfd 0%, #fafcfd 50%, #f5f9fc 100%);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  z-index: 10;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: var(--color-border-2);
  margin: 0 4px;
}

.toolbar-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.filter-box {
  margin-left: 16px;
  position: relative;
  z-index: 100;
}

.mode-tag {
  margin-left: 12px;
  cursor: pointer;
}

.flow-container {
  flex: 1;
  position: relative;
}

.vue-flow-wrapper {
  width: 100%;
  height: 100%;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  z-index: 100;
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

/* Fullscreen styles */
.er-diagram-page:fullscreen {
  background: linear-gradient(135deg, #f8fbfd 0%, #fafcfd 50%, #f5f9fc 100%);
}

.er-diagram-page:fullscreen .toolbar {
  position: sticky;
  top: 0;
}

/* Edit Drawer Styles */
.form-section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
  margin-bottom: 12px;
}

.help-icon {
  font-size: 14px;
  color: rgb(var(--primary-6));
  cursor: help;
}

.form-extra-text {
  font-size: 11px;
  color: var(--color-text-3);
  line-height: 1.4;
}

.setting-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background: var(--color-fill-1);
  border-radius: 4px;
  transition: background-color 0.2s;
}

.setting-item:hover {
  background: var(--color-fill-2);
}

.setting-label {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.setting-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
}

.setting-desc {
  font-size: 11px;
  color: var(--color-text-3);
}

/* Help Content Styles */
.help-content {
  font-size: 13px;
  color: var(--color-text-1);
}

.help-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border-2);
}

.help-section {
  margin-bottom: 12px;
}

.help-section:last-child {
  margin-bottom: 0;
}

.help-subtitle {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-2);
  margin-bottom: 6px;
}

.help-list {
  margin: 0;
  padding-left: 0;
  list-style: none;
}

.help-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: var(--color-text-2);
  line-height: 1.6;
  margin-bottom: 4px;
}

.help-list li:last-child {
  margin-bottom: 0;
}

.help-action {
  flex-shrink: 0;
  background: var(--color-fill-2);
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
  color: var(--color-text-1);
  font-weight: 500;
}
</style>

<style>
/* Vue Flow global styles */
@import '@vue-flow/core/dist/style.css';
@import '@vue-flow/core/dist/theme-default.css';
@import '@vue-flow/controls/dist/style.css';
@import '@vue-flow/minimap/dist/style.css';

.vue-flow__edge-path {
  stroke: #b8c5d3;
  stroke-width: 1.5;
  cursor: pointer;
  transition: stroke 0.2s, stroke-width 0.2s;
}

.vue-flow__edge.selected .vue-flow__edge-path,
.vue-flow .vue-flow__edge.selected .vue-flow__edge-path,
.vue-flow__edges .vue-flow__edge.selected .vue-flow__edge-path {
  stroke: #fcd34d !important;
  filter: drop-shadow(0 0 6px rgba(252, 211, 77, 0.8)) drop-shadow(0 0 12px rgba(252, 211, 77, 0.4));
}

.vue-flow__edge-text {
  font-size: 10px;
  cursor: pointer;
}

.vue-flow__edge.selected .vue-flow__edge-textbg {
  fill: #fef08a !important;
  stroke: none !important;
  filter: drop-shadow(0 0 4px rgba(252, 211, 77, 0.6));
}

.vue-flow__edge.selected .vue-flow__edge-text {
  fill: #78350f !important;
  font-weight: 600;
  font-size: 11px;
}

/* 选中边的箭头也变色 */
.vue-flow__edge.selected marker path {
  fill: #f59e0b;
}

.vue-flow__controls {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
}

.vue-flow__minimap {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
}
</style>
