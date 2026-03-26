/**
 * 工作流类型定义
 */
import type { RuleAction } from './biz-rule'

// ==================== 节点类型 ====================

export enum WorkflowNodeType {
  START = 'START',
  END = 'END',
  APPROVAL = 'APPROVAL',
  AUTO_ACTION = 'AUTO_ACTION',
}

// ==================== 审批模式 ====================

export enum ApprovalMode {
  /** 或��：任意一人通过即推进 */
  ANY_ONE = 'ANY_ONE',
  /** 会签：所有人通过才推进 */
  ALL_REQUIRED = 'ALL_REQUIRED',
}

// ==================== 失败策略 ====================

export enum FailureStrategy {
  /** 阻断流程 */
  BLOCK_WORKFLOW = 'BLOCK_WORKFLOW',
  /** 继续执行 */
  CONTINUE = 'CONTINUE',
}

// ==================== 审批人选择器 ====================

export type ApproverSelectorType = 'FIXED_MEMBERS' | 'ROLE_BASED'

export interface FixedMembersSelector {
  selectorType: 'FIXED_MEMBERS'
  memberIds: string[]
}

export interface RoleBasedSelector {
  selectorType: 'ROLE_BASED'
  roleIds: string[]
}

export type ApproverSelector = FixedMembersSelector | RoleBasedSelector

// ==================== 触发器 ====================

export interface ManualTrigger {
  triggerType: 'MANUAL'
  allowedMemberIds?: string[]
  allowedRoleIds?: string[]
}

export type WorkflowTrigger = ManualTrigger

// ==================== 节点定义 ====================

export interface StartNodeDefinition {
  nodeType: WorkflowNodeType.START
  id: string
  name: string
}

export interface EndNodeDefinition {
  nodeType: WorkflowNodeType.END
  id: string
  name: string
}

export interface ApprovalNodeDefinition {
  nodeType: WorkflowNodeType.APPROVAL
  id: string
  name: string
  approverSelector: ApproverSelector
  approvalMode: ApprovalMode
}

export interface AutoActionNodeDefinition {
  nodeType: WorkflowNodeType.AUTO_ACTION
  id: string
  name: string
  actions: RuleAction[]
  failureStrategy: FailureStrategy
}

export type NodeDefinition =
  | StartNodeDefinition
  | EndNodeDefinition
  | ApprovalNodeDefinition
  | AutoActionNodeDefinition

// ==================== 边定义 ====================

export interface EdgeDefinition {
  id: string
  sourceNodeId: string
  targetNodeId: string
}

// ==================== 工作流定义 ====================

/** 画布节点坐标（与后端 CanvasNodePosition 对应） */
export interface CanvasNodePosition {
  x: number
  y: number
}

export interface WorkflowDefinition {
  schemaSubType?: string
  id?: string
  orgId?: string
  name: string
  description?: string
  state?: string
  enabled?: boolean
  contentVersion?: number
  cardTypeId: string
  trigger: WorkflowTrigger
  nodes: NodeDefinition[]
  edges: EdgeDefinition[]
  /** 画布节点坐标，拖拽布局持久化 */
  canvasLayout?: Record<string, CanvasNodePosition>
  createdAt?: string
  updatedAt?: string
}

// ==================== 工厂函数 ====================

let nodeIdCounter = 0

export function generateNodeId(): string {
  return `node_${Date.now()}_${++nodeIdCounter}`
}

export function generateEdgeId(): string {
  return `edge_${Date.now()}_${++nodeIdCounter}`
}

export function createEmptyWorkflow(cardTypeId: string, orgId: string): WorkflowDefinition {
  const startId = generateNodeId()
  const endId = generateNodeId()
  const edgeId = generateEdgeId()

  return {
    orgId,
    name: '',
    schemaSubType: 'WORKFLOW_DEFINITION',
    cardTypeId,
    trigger: { triggerType: 'MANUAL' },
    nodes: [
      { nodeType: WorkflowNodeType.START, id: startId, name: '开始' },
      { nodeType: WorkflowNodeType.END, id: endId, name: '结束' },
    ],
    edges: [
      { id: edgeId, sourceNodeId: startId, targetNodeId: endId },
    ],
    enabled: true,
  }
}

export function createApprovalNode(name = ''): ApprovalNodeDefinition {
  return {
    nodeType: WorkflowNodeType.APPROVAL,
    id: generateNodeId(),
    name,
    approverSelector: { selectorType: 'FIXED_MEMBERS', memberIds: [] },
    approvalMode: ApprovalMode.ANY_ONE,
  }
}

export function createAutoActionNode(name = ''): AutoActionNodeDefinition {
  return {
    nodeType: WorkflowNodeType.AUTO_ACTION,
    id: generateNodeId(),
    name,
    actions: [],
    failureStrategy: FailureStrategy.BLOCK_WORKFLOW,
  }
}

// ==================== 节点显示配置 ====================

export interface NodeTypeConfig {
  label: string
  color: string
  icon: string
  description: string
}

export const NODE_TYPE_CONFIGS: Record<WorkflowNodeType, NodeTypeConfig> = {
  [WorkflowNodeType.START]: {
    label: '开始',
    color: '#34C759',
    icon: 'icon-play-circle',
    description: '流程起点',
  },
  [WorkflowNodeType.END]: {
    label: '结束',
    color: '#F54A45',
    icon: 'icon-stop',
    description: '流程终点',
  },
  [WorkflowNodeType.APPROVAL]: {
    label: '人工审批',
    color: '#3370FF',
    icon: 'icon-user',
    description: '需要人工审批通过',
  },
  [WorkflowNodeType.AUTO_ACTION]: {
    label: '自动执行',
    color: '#FF9500',
    icon: 'icon-thunderbolt',
    description: '自动执行配置的动作',
  },
}
