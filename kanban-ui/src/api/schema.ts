import request from './request'
import type { PageResult } from '@/types/api'
import type {
  SchemaDefinition,
  CreateSchemaRequest,
  UpdateSchemaRequest,
  SchemaChangelogDTO,
  SchemaReferenceSummaryDTO,
  SchemaType,
} from '@/types/schema'

const BASE_URL = '/api/v1/schemas/common'

/**
 * Schema 通用 API
 */
export const schemaApi = {
  /**
   * 根据 ID 获取 Schema
   */
  getById<T extends SchemaDefinition>(schemaId: string): Promise<T> {
    return request.get(`${BASE_URL}/${schemaId}`)
  },

  /**
   * 批量获取 Schema
   */
  getByIds<T extends SchemaDefinition>(schemaIds: string[]): Promise<T[]> {
    return request.post(`${BASE_URL}/batch`, schemaIds)
  },

  /**
   * 创建 Schema
   */
  create<T extends SchemaDefinition>(req: CreateSchemaRequest): Promise<T> {
    return request.post(BASE_URL, req)
  },

  /**
   * 更新 Schema
   */
  update<T extends SchemaDefinition>(schemaId: string, req: UpdateSchemaRequest): Promise<T> {
    return request.put(`${BASE_URL}/${schemaId}`, req)
  },

  /**
   * 删除 Schema
   */
  delete(schemaId: string): Promise<void> {
    return request.delete(`${BASE_URL}/${schemaId}`)
  },

  /**
   * 启用 Schema
   */
  activate(schemaId: string): Promise<void> {
    return request.post(`${BASE_URL}/${schemaId}/activate`)
  },

  /**
   * 停用 Schema
   */
  disable(schemaId: string): Promise<void> {
    return request.post(`${BASE_URL}/${schemaId}/disable`)
  },

  /**
   * 分页查询 Schema
   */
  listByType<T extends SchemaDefinition>(
    type: SchemaType,
    page = 1,
    size = 20,
  ): Promise<PageResult<T>> {
    return request.get(BASE_URL, {
      params: { type, page, size },
    })
  },

  /**
   * 根据二级键查询
   */
  getBySecondKey<T extends SchemaDefinition>(
    secondKey: string,
    type?: SchemaType,
  ): Promise<T[]> {
    return request.get(`${BASE_URL}/by-second-key/${secondKey}`, {
      params: { type },
    })
  },

  /**
   * 根据所属 Schema 查询
   */
  getByBelongTo<T extends SchemaDefinition>(belongTo: string): Promise<T[]> {
    return request.get(`${BASE_URL}/by-belong-to/${belongTo}`)
  },

  /**
   * 获取 Schema 引用摘要
   */
  getReferenceSummary(schemaId: string): Promise<SchemaReferenceSummaryDTO> {
    return request.get(`${BASE_URL}/${schemaId}/reference-summary`)
  },

  /**
   * 获取全局 Schema 变更历史（分页）
   * @param page 页码（从0开始）
   * @param size 每页数量
   * @param keyword 搜索关键字（可选，匹配变更摘要、Schema名称）
   * @param schemaType Schema类型（可选，用于筛选特定类型的变更日志）
   * @param changedBy 操作人ID（可选，用于筛选特定操作人的变更日志）
   */
  getGlobalChangelog(
    page = 0,
    size = 20,
    keyword?: string,
    schemaType?: string,
    changedBy?: string,
  ): Promise<PageResult<SchemaChangelogDTO>> {
    return request.get(`${BASE_URL}/changelog`, {
      params: { page, size, keyword, schemaType, changedBy },
    })
  },

  /**
   * 获取 Schema 变更历史（分页）
   * @param schemaId Schema ID
   * @param page 页码（从0开始）
   * @param size 每页数量
   * @param keyword 搜索关键字（可选，匹配变更摘要）
   * @param includeChildren 是否包含附属Schema的变更日志（可选，默认false）
   * @param changedBy 操作人ID（可选，用于筛选特定操作人的变更日志）
   */
  getChangelog(
    schemaId: string,
    page = 0,
    size = 20,
    keyword?: string,
    includeChildren = false,
    changedBy?: string,
  ): Promise<PageResult<SchemaChangelogDTO>> {
    return request.get(`${BASE_URL}/${schemaId}/changelog`, {
      params: { page, size, keyword, includeChildren, changedBy },
    })
  },

  /**
   * 还原至指定版本
   */
  restoreToVersion<T extends SchemaDefinition>(schemaId: string, changelogId: number): Promise<T> {
    return request.post(`${BASE_URL}/${schemaId}/changelog/${changelogId}/restore`)
  },
}
