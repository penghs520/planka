import request from './request'
import type {
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
  RefreshTokenResponse,
  SwitchOrganizationRequest,
  SwitchOrganizationResponse,
} from '@/types/auth'

const BASE_URL = '/api/v1/auth'

/**
 * 认证相关 API
 */
export const authApi = {
  /**
   * 用户登录
   */
  login(data: LoginRequest): Promise<LoginResponse> {
    return request.post(`${BASE_URL}/login`, data)
  },

  /**
   * 刷新 Token
   */
  refresh(data: RefreshTokenRequest): Promise<RefreshTokenResponse> {
    return request.post(`${BASE_URL}/refresh`, data)
  },

  /**
   * 登出
   */
  logout(): Promise<void> {
    return request.post(`${BASE_URL}/logout`)
  },

  /**
   * 切换组织
   * 验证成员卡状态并返回包含组织信息的新 Token
   */
  switchOrganization(data: SwitchOrganizationRequest): Promise<SwitchOrganizationResponse> {
    return request.post(`${BASE_URL}/switch-organization`, data)
  },
}
