import type { SchemaDefinition } from './schema'
import { SchemaSubType } from './schema'

// Re-export for convenience
export { SchemaSubType }

/**
 * 状态类别枚举（用于阶段分类）
 */
export enum StepStatusKind {
  /** 待办 */
  TODO = 'TODO',
  /** 进行中 */
  IN_PROGRESS = 'IN_PROGRESS',
  /** 已完成 */
  DONE = 'DONE',
  /** 已取消 */
  CANCELLED = 'CANCELLED',
}

/**
 * 状态工作类型枚举
 */
export enum StatusWorkType {
  /** 等待状态 */
  WAITING = 'WAITING',
  /** 工作中状态 */
  WORKING = 'WORKING',
}

/**
 * 状态工作类型配置（用于显示）
 */
export const StatusWorkTypeConfig: Record<StatusWorkType, { label: string; color: string }> = {
  [StatusWorkType.WAITING]: {
    label: '等待',
    color: 'orangered',
  },
  [StatusWorkType.WORKING]: {
    label: '工作中',
    color: 'arcoblue',
  },
}

/**
 * 状态类别配置（用于显示）
 */
export const StepStatusKindConfig: Record<
  StepStatusKind,
  { label: string; color: string; defaultStatusColor: string }
> = {
  [StepStatusKind.TODO]: {
    label: '待办',
    color: 'gray',
    defaultStatusColor: '#86909c',
  },
  [StepStatusKind.IN_PROGRESS]: {
    label: '进行中',
    color: 'arcoblue',
    defaultStatusColor: '#165dff',
  },
  [StepStatusKind.DONE]: {
    label: '已完成',
    color: 'green',
    defaultStatusColor: '#00b42a',
  },
  [StepStatusKind.CANCELLED]: {
    label: '已取消',
    color: 'red',
    defaultStatusColor: '#f53f3f',
  },
}

/**
 * 状态配置
 */
export interface StatusConfig {
  /** 状态ID */
  id: string
  /** 状态名称 */
  name: string
  /** 状态描述 */
  desc?: string
  /** 工作类型（等待/工作中） */
  workType?: StatusWorkType
  /** 排序号 */
  sortOrder: number
}

/**
 * 阶段配置
 */
export interface StepConfig {
  /** 阶段ID */
  id: string
  /** 阶段名称 */
  name: string
  /** 阶段描述 */
  desc?: string
  /** 阶段类别 */
  kind: StepStatusKind
  /** 排序号 */
  sortOrder: number
  /** 状态列表 */
  statusList: StatusConfig[]
}

/**
 * 价值流定义
 */
export interface ValueStreamDefinition extends SchemaDefinition {
  /** 所属卡片类型ID */
  cardTypeId: string
  /** 阶段列表 */
  stepList: StepConfig[]
}

/**
 * 状态分支（从基线状态衍生的分支状态列表）
 */
export interface StatusBranch {
  /** 关联的基线状态ID */
  baseLineStatusId: string
  /** 分支状态列表 */
  statusList: StatusConfig[]
}

/**
 * 价值流分支定义
 */
export interface ValueStreamBranchDefinition extends SchemaDefinition {
  /** 所属卡片类型ID */
  cardTypeId: string
  /** 关联的基线ID */
  baseLineId: string
  /** 状态分支列表 */
  statusBranches: StatusBranch[]
}

/**
 * 状态配置 DTO（用于展示）
 */
export interface StatusConfigDTO {
  id: string
  name: string
  desc?: string
  workType?: StatusWorkType
  sortOrder: number
  /** 是否为基线状态 */
  baseline: boolean
  /** 是否为分支状态 */
  branch: boolean
  /** 如果是分支状态，关联的基线状态ID */
  parentBaselineStatusId?: string
}

/**
 * 阶段配置 DTO
 */
export interface StepConfigDTO {
  id: string
  name: string
  desc?: string
  kind: StepStatusKind
  sortOrder: number
  /** 状态列表 */
  statusList: StatusConfigDTO[]
}

/**
 * 价值流 DTO
 */
export interface ValueStreamDTO {
  id: string
  name: string
  branchId?: string
  branchName?: string
  branchContentVersion?: number
  cardTypeId: string
  orgId: string
  hasBranch: boolean
  stepList: StepConfigDTO[]
}

/**
 * 创建默认的状态配置
 */
export function createDefaultStatusConfig(
  kind: StepStatusKind,
  sortOrder: number,
): StatusConfig {
  const kindConfig = StepStatusKindConfig[kind]
  return {
    id: generateId(),
    name: kindConfig.label,
    workType: StatusWorkType.WORKING,
    sortOrder,
  }
}

/**
 * 创建默认的阶段配置
 */
export function createDefaultStepConfig(
  kind: StepStatusKind,
  sortOrder: number,
): StepConfig {
  const kindConfig = StepStatusKindConfig[kind]
  return {
    id: generateId(),
    name: kindConfig.label,
    kind,
    sortOrder,
    statusList: [createDefaultStatusConfig(kind, 0)],
  }
}

/**
 * 创建默认的价值流定义（包含四个默认阶段）
 */
export function createDefaultValueStream(
  orgId: string,
  cardTypeId: string,
): Omit<ValueStreamDefinition, 'id' | 'contentVersion' | 'state'> {
  return {
    schemaSubType: SchemaSubType.VALUE_STREAM,
    orgId,
    cardTypeId,
    name: '价值流',
    enabled: true,
    stepList: [
      createDefaultStepConfig(StepStatusKind.TODO, 0),
      createDefaultStepConfig(StepStatusKind.IN_PROGRESS, 1),
      createDefaultStepConfig(StepStatusKind.DONE, 2),
      createDefaultStepConfig(StepStatusKind.CANCELLED, 3),
    ],
  }
}

/**
 * 生成唯一ID（简单实现，实际应使用雪花算法）
 */
function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
}
