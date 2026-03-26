import request from './request'
import { schemaApi } from './schema'
import type { WorkflowDefinition } from '@/types/workflow'

const WORKFLOW_URL = '/api/v1/schemas/workflows'
const SCHEMA_URL = '/api/v1/schemas/common'

/**
 * 工作流定义 API
 */
export const workflowApi = {
  /**
   * 获取单个工作流定义
   */
  getById(workflowId: string): Promise<WorkflowDefinition> {
    return request.get(`${SCHEMA_URL}/${workflowId}`)
  },

  /**
   * 根据实体类型 ID 获取工作流列表
   */
  getByCardTypeId(cardTypeId: string): Promise<WorkflowDefinition[]> {
    return request.get(`${WORKFLOW_URL}/by-card-type/${cardTypeId}`)
  },

  /**
   * 创建工作流定义
   */
  create(definition: WorkflowDefinition): Promise<WorkflowDefinition> {
    return request.post(SCHEMA_URL, { definition })
  },

  /**
   * 更新工作流定义
   */
  update(
    workflowId: string,
    definition: WorkflowDefinition,
    expectedVersion?: number,
  ): Promise<WorkflowDefinition> {
    return request.put(`${SCHEMA_URL}/${workflowId}`, { definition, expectedVersion })
  },

  /**
   * 删除工作流定义
   */
  delete(workflowId: string): Promise<void> {
    return schemaApi.delete(workflowId)
  },

  /**
   * 启用工作流
   */
  enable(workflowId: string): Promise<void> {
    return schemaApi.activate(workflowId)
  },

  /**
   * 停用工作流
   */
  disable(workflowId: string): Promise<void> {
    return schemaApi.disable(workflowId)
  },
}
