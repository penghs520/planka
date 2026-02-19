import request from './request'
import type { UserDTO, UpdateUserRequest, ChangePasswordRequest } from '@/types/user'
import type { OrganizationDTO } from '@/types/member'

const BASE_URL = '/api/v1/users'

/**
 * 用户相关 API
 */
export const userApi = {
  /**
   * 获取当前用户信息
   */
  getMe(): Promise<UserDTO> {
    return request.get(`${BASE_URL}/me`)
  },

  /**
   * 更新当前用户信息
   */
  updateMe(data: UpdateUserRequest): Promise<UserDTO> {
    return request.put(`${BASE_URL}/me`, data)
  },

  /**
   * 修改密码
   */
  changePassword(data: ChangePasswordRequest): Promise<void> {
    return request.put(`${BASE_URL}/me/password`, data)
  },

  /**
   * 获取当前用户的组织列表
   */
  getMyOrganizations(): Promise<OrganizationDTO[]> {
    return request.get(`${BASE_URL}/me/organizations`)
  },
}
