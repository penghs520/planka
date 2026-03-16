import request from './request'
import type {
  LinkTypeVO,
  LinkTypeOptionVO,
  CreateLinkTypeRequest,
  UpdateLinkTypeRequest,
  LinkPosition,
} from '@/types/link-type'

const LINK_TYPE_URL = '/api/v1/schemas/link-types'

/**
 * 关联类型 API
 */
export const linkTypeApi = {
  /**
   * 查询关联类型列表
   */
  list(): Promise<LinkTypeVO[]> {
    return request.get(LINK_TYPE_URL)
  },

  /**
   * 查询关联类型选项列表（用于下拉框）
   */
  listOptions(): Promise<LinkTypeOptionVO[]> {
    return request.get(`${LINK_TYPE_URL}/options`)
  },

  /**
   * 根据 ID 获取关联类型详情
   */
  getById(linkTypeId: string): Promise<LinkTypeVO> {
    return request.get(`${LINK_TYPE_URL}/${linkTypeId}`)
  },

  /**
   * 创建关联类型
   */
  create(data: CreateLinkTypeRequest): Promise<LinkTypeVO> {
    return request.post(LINK_TYPE_URL, data)
  },

  /**
   * 更新关联类型
   */
  update(linkTypeId: string, data: UpdateLinkTypeRequest): Promise<LinkTypeVO> {
    return request.put(`${LINK_TYPE_URL}/${linkTypeId}`, data)
  },

  /**
   * 删除关联类型
   */
  delete(linkTypeId: string): Promise<void> {
    return request.delete(`${LINK_TYPE_URL}/${linkTypeId}`)
  },

  /**
   * 获取卡片类型可用的关联类型列表
   */
  getAvailableForCardType(cardTypeId: string, position?: LinkPosition): Promise<LinkTypeOptionVO[]> {
    return request.get(`${LINK_TYPE_URL}/available-for-card-type/${cardTypeId}`, {
      params: position ? { position } : undefined,
    })
  },
}
