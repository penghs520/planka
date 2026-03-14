/**
 * 规则执行日志相关类型
 */

/** 执行状态 */
export type ExecutionStatus = 'SUCCESS' | 'FAILED' | 'SKIPPED'

/** 动作执行结果 */
export interface ActionResult {
  actionType: string
  sortOrder: number
  success: boolean
  durationMs: number
  errorMessage?: string
  affectedCardIds?: string[]
}

/** 规则执行日志 */
export interface RuleExecutionLog {
  id: string
  ruleId: string
  ruleName: string
  cardId?: string
  cardTitle?: string
  triggerEvent: string
  operatorId?: string
  operatorName?: string
  executionTime: string
  durationMs: number
  status: ExecutionStatus
  errorMessage?: string
  affectedCardIds?: string[]
  traceId?: string
  actionResults?: ActionResult[]
}

/** 搜索请求 */
export interface RuleExecutionLogSearchRequest {
  ruleIds?: string[]
  statuses?: ExecutionStatus[]
  startTime?: string
  endTime?: string
  page?: number
  size?: number
  sortAsc?: boolean
}

/** 规则选项 */
export interface RuleOption {
  ruleId: string
  ruleName: string
}

/** 过滤选项 */
export interface RuleExecutionLogFilters {
  rules: RuleOption[]
  statuses: ExecutionStatus[]
}

/** 分页结果 */
export interface RuleExecutionLogPageResult {
  content: RuleExecutionLog[]
  page: number
  size: number
  total: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}
