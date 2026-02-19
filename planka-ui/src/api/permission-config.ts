import request from './request'
import { schemaApi } from './schema'
import type { PermissionConfigDefinition } from '@/types/permission'
import type { CreateSchemaRequest, UpdateSchemaRequest } from '@/types/schema'
import { SchemaType } from '@/types/schema'

const BASE_URL = '/api/v1/schemas/common'

/**
 * 权限配置 API
 *
 * 权限配置独立存储，支持组织级配置。
 */
export const permissionConfigApi = {
  /**
   * 查询卡片类型的权限配置列表
   *
   * @param cardTypeId 卡片类型ID
   * @returns 权限配置列表
   */
  async listByCardType(cardTypeId: string): Promise<PermissionConfigDefinition[]> {
    const params: Record<string, string | undefined> = { type: SchemaType.CARD_PERMISSION }
    const result = await request.get<PermissionConfigDefinition[]>(`${BASE_URL}/by-belong-to/${cardTypeId}`, {
      params,
    })
    // result 可能是 AxiosResponse 或者已处理为 data
    const data = (result as any)?.data ?? result
    return Array.isArray(data) ? data : []
  },

  /**
   * 获取组织级权限配置
   *
   * @param cardTypeId 卡片类型ID
   * @returns 组织级权限配置（可能为空）
   */
  async getOrgLevel(cardTypeId: string): Promise<PermissionConfigDefinition | null> {
    const configs = await this.listByCardType(cardTypeId)
    return configs[0] || null
  },

  /**
   * 根据 ID 获取权限配置
   */
  getById(id: string): Promise<PermissionConfigDefinition> {
    return schemaApi.getById<PermissionConfigDefinition>(id)
  },

  /**
   * 创建权限配置
   */
  create(definition: PermissionConfigDefinition): Promise<PermissionConfigDefinition> {
    const req: CreateSchemaRequest = { definition }
    return schemaApi.create<PermissionConfigDefinition>(req)
  },

  /**
   * 更新权限配置
   */
  update(
    id: string,
    definition: PermissionConfigDefinition,
    expectedVersion?: number,
  ): Promise<PermissionConfigDefinition> {
    const req: UpdateSchemaRequest = { definition, expectedVersion }
    return schemaApi.update<PermissionConfigDefinition>(id, req)
  },

  /**
   * 删除权限配置
   */
  delete(id: string): Promise<void> {
    return schemaApi.delete(id)
  },
}
