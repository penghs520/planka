import request from './request'
import type {
  RuleExecutionLogSearchRequest,
  RuleExecutionLogFilters,
  RuleExecutionLogPageResult,
} from '@/types/rule-execution-log'

const BASE_URL = '/api/v1/biz-rules/execution-logs'

/**
 * 规则执行日志 API
 */
export const ruleExecutionLogApi = {
  /**
   * 分页搜索执行日志
   */
  search(cardTypeId: string, params: RuleExecutionLogSearchRequest): Promise<RuleExecutionLogPageResult> {
    return request.post(`${BASE_URL}/${cardTypeId}/search`, params)
  },

  /**
   * 获取过滤选项
   */
  getFilters(cardTypeId: string): Promise<RuleExecutionLogFilters> {
    return request.get(`${BASE_URL}/${cardTypeId}/filters`)
  },
}
