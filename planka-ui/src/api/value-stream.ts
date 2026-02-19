import request from './request'
import { schemaApi } from './schema'
import type { ValueStreamDefinition } from '@/types/value-stream'
import type { CreateSchemaRequest, UpdateSchemaRequest } from '@/types/schema'

/**
 * 价值流状态选项（精简版，用于下拉选择）
 */
export interface StatusOption {
  /** 状态 ID */
  id: string
  /** 状态名称 */
  name: string
  /** 阶段类型：TODO, IN_PROGRESS, DONE, CANCELLED */
  stepKind: string
}

const VALUE_STREAM_URL = '/api/v1/schemas/value-streams'

/**
 * 价值流 API
 */
export const valueStreamApi = {
  /**
   * 根据卡片类型ID获取价值流定义
   *
   * @param cardTypeId 卡片类型ID
   * @returns 价值流定义，不存在则返回 null
   */
  getByCardType(cardTypeId: string): Promise<ValueStreamDefinition | null> {
    return request.get(`${VALUE_STREAM_URL}/by-card-type/${cardTypeId}`)
  },

  /**
   * 创建价值流定义
   *
   * @param definition 价值流定义
   * @returns 创建后的价值流定义
   */
  create(
    definition: Omit<ValueStreamDefinition, 'id' | 'contentVersion' | 'state'>,
  ): Promise<ValueStreamDefinition> {
    const req: CreateSchemaRequest = { definition: definition as any }
    return schemaApi.create<ValueStreamDefinition>(req)
  },

  /**
   * 更新价值流定义
   *
   * @param streamId 价值流ID
   * @param definition 价值流定义
   * @param expectedVersion 期望的版本号（用于乐观锁）
   * @returns 更新后的价值流定义
   */
  update(
    streamId: string,
    definition: ValueStreamDefinition,
    expectedVersion?: number,
  ): Promise<ValueStreamDefinition> {
    const req: UpdateSchemaRequest = { definition, expectedVersion }
    return schemaApi.update<ValueStreamDefinition>(streamId, req)
  },

  /**
   * 删除价值流定义
   *
   * @param streamId 价值流ID
   */
  delete(streamId: string): Promise<void> {
    return schemaApi.delete(streamId)
  },
}

/**
 * 价值流分支 API
 */
export const valueStreamBranchApi = {


  /**
   * 获取价值流状态选项列表
   *
   * @param cardTypeId 卡片类型ID
   * @returns 状态选项列表
   */
  getStatusOptions(cardTypeId: string): Promise<StatusOption[]> {
    return request.get(`${VALUE_STREAM_URL}/status-options/${cardTypeId}`)
  },
}
