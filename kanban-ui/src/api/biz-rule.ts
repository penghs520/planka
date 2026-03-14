import request from './request'
import { schemaApi } from './schema'
import type { BizRuleDefinition } from '@/types/biz-rule'

const BIZ_RULE_URL = '/api/v1/schemas/biz-rules'
const SCHEMA_URL = '/api/v1/schemas/common'

/**
 * 业务规则 API
 */
export const bizRuleApi = {
  /**
   * 获取单个规则
   */
  getById(ruleId: string): Promise<BizRuleDefinition> {
    return request.get(`${SCHEMA_URL}/${ruleId}`)
  },

  /**
   * 批量获取规则
   */
  getByIds(ruleIds: string[]): Promise<BizRuleDefinition[]> {
    return request.post(`${SCHEMA_URL}/batch`, ruleIds)
  },

  /**
   * 根据卡片类型 ID 获取规则列表
   */
  getByCardTypeId(cardTypeId: string): Promise<BizRuleDefinition[]> {
    return request.get(`${BIZ_RULE_URL}/by-card-type/${cardTypeId}`)
  },

  /**
   * 获取组织下所有规则
   */
  list(): Promise<BizRuleDefinition[]> {
    return request.get(BIZ_RULE_URL)
  },

  /**
   * 创建规则
   */
  create(definition: BizRuleDefinition): Promise<BizRuleDefinition> {
    return request.post(SCHEMA_URL, { definition })
  },

  /**
   * 更新规则
   */
  update(
    ruleId: string,
    definition: BizRuleDefinition,
    expectedVersion?: number,
  ): Promise<BizRuleDefinition> {
    return request.put(`${SCHEMA_URL}/${ruleId}`, { definition, expectedVersion })
  },

  /**
   * 删除规则
   */
  delete(ruleId: string): Promise<void> {
    return schemaApi.delete(ruleId)
  },

  /**
   * 启用规则
   */
  enable(ruleId: string): Promise<void> {
    return schemaApi.activate(ruleId)
  },

  /**
   * 停用规则
   */
  disable(ruleId: string): Promise<void> {
    return schemaApi.disable(ruleId)
  },

  /**
   * 复制规则
   */
  copy(ruleId: string): Promise<BizRuleDefinition> {
    return request.post(`${BIZ_RULE_URL}/${ruleId}/copy`)
  },

  /**
   * 获取规则的引用摘要
   */
  getReferenceSummary(ruleId: string) {
    return schemaApi.getReferenceSummary(ruleId)
  },

  /**
   * 获取规则的变更历史
   */
  getChangelog(ruleId: string, limit = 50) {
    return schemaApi.getChangelog(ruleId, limit)
  },
}
