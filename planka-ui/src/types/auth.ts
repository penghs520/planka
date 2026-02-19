import type { UserDTO } from './user'
import type { OrganizationDTO } from './member'

/**
 * 登录请求
 */
export interface LoginRequest {
  email: string
  password: string
}

/**
 * 登录响应
 */
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserDTO
  organizations: OrganizationDTO[]
  requirePasswordChange: boolean
}

/**
 * 账号激活请求
 */
export interface ActivateRequest {
  email: string
  activationCode: string
  password: string
}

/**
 * 刷新Token请求
 */
export interface RefreshTokenRequest {
  refreshToken: string
}

/**
 * 刷新Token响应
 */
export interface RefreshTokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  requirePasswordChange: boolean
}

/**
 * 切换组织请求
 */
export interface SwitchOrganizationRequest {
  orgId: string
}

/**
 * 切换组织响应
 */
export interface SwitchOrganizationResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  orgId: string
  memberCardId: string
  role: 'OWNER' | 'ADMIN' | 'MEMBER'
}
