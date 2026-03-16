import request from './request'
import type {
  OrganizationDTO,
  CreateOrganizationRequest,
  UpdateOrganizationRequest,
} from '@/types/member'

const BASE_URL = '/api/v1/organizations'

/**
 * 组织相关 API
 */
export const orgApi = {
  /**
   * 创建组织
   */
  create(data: CreateOrganizationRequest): Promise<OrganizationDTO> {
    return request.post(BASE_URL, data)
  },

  /**
   * 获取组织详情
   */
  getById(orgId: string): Promise<OrganizationDTO> {
    return request.get(`${BASE_URL}/${orgId}`)
  },

  /**
   * 更新组织信息
   */
  update(orgId: string, data: UpdateOrganizationRequest): Promise<OrganizationDTO> {
    return request.put(`${BASE_URL}/${orgId}`, data)
  },

  /**
   * 删除组织
   */
  delete(orgId: string): Promise<void> {
    return request.delete(`${BASE_URL}/${orgId}`)
  },
}
