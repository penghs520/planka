import { schemaApi } from './schema'
import request from './request'
import type { FormulaDefinition } from '@/types/formula'

const BASE_URL = '/api/v1/schemas/formula-definitions'

/**
 * 计算公式定义 API
 */
export const formulaDefinitionApi = {
  /**
   * 获取公式定义
   */
  getById(formulaId: string): Promise<FormulaDefinition> {
    return schemaApi.getById<FormulaDefinition>(formulaId)
  },

  /**
   * 批量获取公式定义
   */
  getByIds(formulaIds: string[]): Promise<FormulaDefinition[]> {
    return schemaApi.getByIds<FormulaDefinition>(formulaIds)
  },

  /**
   * 创建公式定义
   */
  create(definition: FormulaDefinition): Promise<FormulaDefinition> {
    return schemaApi.create<FormulaDefinition>({ definition })
  },

  /**
   * 更新公式定义
   */
  update(
    formulaId: string,
    definition: FormulaDefinition,
    expectedVersion?: number,
  ): Promise<FormulaDefinition> {
    return schemaApi.update<FormulaDefinition>(formulaId, { definition, expectedVersion })
  },

  /**
   * 删除公式定义
   */
  delete(formulaId: string): Promise<void> {
    return schemaApi.delete(formulaId)
  },

  /**
   * 启用公式定义
   */
  activate(formulaId: string): Promise<void> {
    return schemaApi.activate(formulaId)
  },

  /**
   * 停用公式定义
   */
  disable(formulaId: string): Promise<void> {
    return schemaApi.disable(formulaId)
  },

  /**
   * 查询公式定义列表（返回 VO）
   */
  list(): Promise<FormulaDefinition[]> {
    return request.get(BASE_URL)
  },

  /**
   * 获取公式定义的引用摘要
   */
  getReferenceSummary(formulaId: string) {
    return schemaApi.getReferenceSummary(formulaId)
  },

  /**
   * 获取公式定义的变更历史
   */
  getChangelog(formulaId: string, limit = 50) {
    return schemaApi.getChangelog(formulaId, limit)
  },
}
