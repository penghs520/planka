import request from './request'
import { schemaApi } from './schema'
import type {
  MenuGroupDefinition,
  MenuTreeVO,
  MenuGroupVO,
  AddViewToGroupRequest,
  ReorderViewsRequest,
} from '@/types/menu'
import type { ViewListItemVO } from '@/types/view'
import type { CreateSchemaRequest, UpdateSchemaRequest } from '@/types/schema'

const MENU_URL = '/api/v1/schemas/menus'

/**
 * 菜单分组 API
 */
export const menuApi = {
  /**
   * 获取菜单树
   * @param userId 当前用户ID（可选，用于权限过滤）
   */
  getMenuTree(userId?: string): Promise<MenuTreeVO> {
    return request.get(`${MENU_URL}/tree`, { params: { userId } })
  },

  /**
   * 获取所有分组列表（平铺）
   */
  listGroups(): Promise<MenuGroupVO[]> {
    return request.get(`${MENU_URL}/groups`)
  },

  /**
   * 根据 ID 获取分组详情
   */
  getById(groupId: string): Promise<MenuGroupDefinition> {
    return schemaApi.getById<MenuGroupDefinition>(groupId)
  },

  /**
   * 创建分组
   */
  create(definition: MenuGroupDefinition): Promise<MenuGroupDefinition> {
    const req: CreateSchemaRequest = { definition }
    return schemaApi.create<MenuGroupDefinition>(req)
  },

  /**
   * 更新分组
   */
  update(groupId: string, definition: MenuGroupDefinition, expectedVersion?: number): Promise<MenuGroupDefinition> {
    const req: UpdateSchemaRequest = { definition, expectedVersion }
    return schemaApi.update<MenuGroupDefinition>(groupId, req)
  },

  /**
   * 删除分组
   */
  delete(groupId: string): Promise<void> {
    return schemaApi.delete(groupId)
  },

  /**
   * 启用分组
   */
  activate(groupId: string): Promise<void> {
    return schemaApi.activate(groupId)
  },

  /**
   * 停用分组
   */
  disable(groupId: string): Promise<void> {
    return schemaApi.disable(groupId)
  },

  /**
   * 添加视图到分组
   */
  addViewToGroup(groupId: string, data: AddViewToGroupRequest): Promise<void> {
    return request.post(`${MENU_URL}/groups/${groupId}/views`, data)
  },

  /**
   * 从分组移除视图
   */
  removeViewFromGroup(groupId: string, viewId: string): Promise<void> {
    return request.delete(`${MENU_URL}/groups/${groupId}/views/${viewId}`)
  },

  /**
   * 重新排序分组内的视图
   */
  reorderViews(groupId: string, req: ReorderViewsRequest): Promise<void> {
    return request.put(`${MENU_URL}/groups/${groupId}/views/reorder`, req)
  },

  /**
   * 获取未分组视图列表
   */
  getUngroupedViews(): Promise<ViewListItemVO[]> {
    return request.get(`${MENU_URL}/ungrouped-views`)
  },
}
