import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserDTO, UpdateUserRequest, ChangePasswordRequest } from '@/types/user'
import type { LoginRequest, LoginResponse, ActivateRequest } from '@/types/auth'
import type { OrganizationDTO } from '@/types/member'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'

const TOKEN_KEY = 'token'
const REFRESH_TOKEN_KEY = 'refreshToken'
const TOKEN_EXPIRES_KEY = 'tokenExpiresAt'

/**
 * 用户状态管理
 *
 * 本系统使用双 Token 机制进行身份认证：
 *
 * **Access Token（访问令牌）**
 * - 用途：访问 API 资源的凭证，每次请求都需要携带在 Authorization 头中
 * - 有效期：较短（默认 2 小时），减少泄露风险
 * - 携带信息：用户基本信息 + 组织上下文（orgId、memberCardId、role）
 * - 验证方式：网关通过 JWT 签名验证，无需查询数据库
 *
 * **Refresh Token（刷新令牌）**
 * - 用途：当 Access Token 过期时（401），用于获取新的 Access Token，避免用户重新登录
 * - 有效期：较长（默认 7 天），提供更好的用户体验
 * - 存储位置：前端 localStorage + 后端数据库（存储哈希值和组织上下文）
 * - 重要：切换组织时必须同时更新 refreshToken，否则刷新后会丢失组织上下文
 */
export const useUserStore = defineStore('user', () => {
  // 状态
  const user = ref<UserDTO | null>(null)
  /** Access Token：短期有效，携带用户和组织信息，每次请求都需要 */
  const accessToken = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  /** Refresh Token：长期有效，用于在 Access Token 过期后获取新 Token */
  const refreshToken = ref<string | null>(localStorage.getItem(REFRESH_TOKEN_KEY))
  const tokenExpiresAt = ref<number | null>(
    localStorage.getItem(TOKEN_EXPIRES_KEY)
      ? parseInt(localStorage.getItem(TOKEN_EXPIRES_KEY)!)
      : null,
  )
  /** 是否需要修改密码（使用默认密码时） */
  const requirePasswordChange = ref<boolean>(false)

  // 计算属性
  const isLoggedIn = computed(() => !!accessToken.value)
  const isSuperAdmin = computed(() => user.value?.superAdmin ?? false)

  // 保存 Token 到本地存储
  function saveTokens(access: string, refresh: string, expiresIn: number) {
    const expiresAt = Date.now() + expiresIn * 1000
    accessToken.value = access
    refreshToken.value = refresh
    tokenExpiresAt.value = expiresAt
    localStorage.setItem(TOKEN_KEY, access)
    localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
    localStorage.setItem(TOKEN_EXPIRES_KEY, expiresAt.toString())
  }

  // 更新 Access Token（切换组织时使用，不更新 refresh token）
  function updateAccessToken(access: string, expiresIn: number) {
    const expiresAt = Date.now() + expiresIn * 1000
    accessToken.value = access
    tokenExpiresAt.value = expiresAt
    localStorage.setItem(TOKEN_KEY, access)
    localStorage.setItem(TOKEN_EXPIRES_KEY, expiresAt.toString())
  }

  // 清除 Token
  function clearTokens() {
    accessToken.value = null
    refreshToken.value = null
    tokenExpiresAt.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(TOKEN_EXPIRES_KEY)
  }

  // 处理登录成功后的响应
  function handleLoginSuccess(response: LoginResponse): OrganizationDTO[] {
    saveTokens(response.accessToken, response.refreshToken, response.expiresIn)
    user.value = response.user
    requirePasswordChange.value = response.requirePasswordChange
    return response.organizations
  }

  /**
   * 清除需要修改密码的状态
   */
  function clearRequirePasswordChange(): void {
    requirePasswordChange.value = false
  }

  /**
   * 用户登录
   */
  async function login(data: LoginRequest): Promise<OrganizationDTO[]> {
    const response = await authApi.login(data)
    return handleLoginSuccess(response)
  }

  /**
   * 账号激活
   */
  async function activate(data: ActivateRequest): Promise<OrganizationDTO[]> {
    const response = await authApi.activate(data)
    return handleLoginSuccess(response)
  }

  /**
   * 刷新 Token
   */
  async function doRefreshToken(): Promise<void> {
    if (!refreshToken.value) {
      throw new Error('No refresh token')
    }
    const response = await authApi.refresh({ refreshToken: refreshToken.value })
    saveTokens(response.accessToken, response.refreshToken, response.expiresIn)
    requirePasswordChange.value = response.requirePasswordChange
  }

  /**
   * 登出
   */
  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // 忽略登出错误
    } finally {
      user.value = null
      clearTokens()
      localStorage.removeItem('orgId')
    }
  }

  /**
   * 获取当前用户信息
   */
  async function fetchMe(): Promise<UserDTO> {
    const userData = await userApi.getMe()
    user.value = userData
    return userData
  }

  /**
   * 更新用户信息
   */
  async function updateProfile(data: UpdateUserRequest): Promise<UserDTO> {
    const userData = await userApi.updateMe(data)
    user.value = userData
    return userData
  }

  /**
   * 修改密码
   */
  async function changePassword(data: ChangePasswordRequest): Promise<void> {
    await userApi.changePassword(data)
  }

  /**
   * 检查 Token 是否即将过期（5分钟内）
   */
  function isTokenExpiringSoon(): boolean {
    if (!tokenExpiresAt.value) return true
    return Date.now() > tokenExpiresAt.value - 5 * 60 * 1000
  }

  return {
    // 状态
    user,
    accessToken,
    refreshToken,
    tokenExpiresAt,
    requirePasswordChange,
    // 计算属性
    isLoggedIn,
    isSuperAdmin,
    // 方法
    login,
    activate,
    doRefreshToken,
    logout,
    fetchMe,
    updateProfile,
    changePassword,
    clearRequirePasswordChange,
    isTokenExpiringSoon,
    saveTokens,
    updateAccessToken,
    clearTokens,
  }
})
