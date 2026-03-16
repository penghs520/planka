import request from './request'
import { schemaApi } from './schema'
import type { ListViewDefinition, ViewListItemVO } from '@/types/view'
import type { CreateSchemaRequest, UpdateSchemaRequest } from '@/types/schema'

const VIEW_URL = '/api/v1/schemas/views'

/**
 * 视图 API
 */
export const viewApi = {
  /**
   * 查询视图列表（简化版）
   */
  list(): Promise<ViewListItemVO[]> {
    return request.get(VIEW_URL)
  },

  /**
   * 根据 ID 获取视图定义
   */
  getById(viewId: string): Promise<ListViewDefinition> {
    return schemaApi.getById<ListViewDefinition>(viewId)
  },

  /**
   * 创建视图
   */
  create(definition: ListViewDefinition): Promise<ListViewDefinition> {
    const req: CreateSchemaRequest = { definition }
    return schemaApi.create<ListViewDefinition>(req)
  },

  /**
   * 更新视图
   */
  update(viewId: string, definition: ListViewDefinition, expectedVersion?: number): Promise<ListViewDefinition> {
    const req: UpdateSchemaRequest = { definition, expectedVersion }
    return schemaApi.update<ListViewDefinition>(viewId, req)
  },

  /**
   * 删除视图
   */
  delete(viewId: string): Promise<void> {
    return schemaApi.delete(viewId)
  },

  /**
   * 启用视图
   */
  activate(viewId: string): Promise<void> {
    return schemaApi.activate(viewId)
  },

  /**
   * 停用视图
   */
  disable(viewId: string): Promise<void> {
    return schemaApi.disable(viewId)
  },
}
