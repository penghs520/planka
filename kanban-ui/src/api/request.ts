import axios, {
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
  type AxiosError,
} from 'axios'
import { Message } from '@arco-design/web-vue'
import type { Result } from '@/types/api'

/**
 * Token 存储键名
 *
 * Access Token：短期有效的访问令牌，携带用户和组织信息，每次 API 请求都需要携带
 * Refresh Token：长期有效的刷新令牌，当 Access Token 过期时用于获取新的 Access Token
 *
 * @see stores/user.ts 中有详细的双 Token 机制说明
 */
const TOKEN_KEY = 'token'
const REFRESH_TOKEN_KEY = 'refreshToken'
const TOKEN_EXPIRES_KEY = 'tokenExpiresAt'
const ORG_ID_KEY = 'orgId'

// 是否正在刷新 Token（防止并发刷新）
let isRefreshing = false
// 等待刷新 Token 的请求队列（Access Token 过期时，多个请求会排队等待刷新完成）
let refreshSubscribers: ((token: string) => void)[] = []

/**
 * 订阅 Token 刷新完成事件
 */
function subscribeTokenRefresh(callback: (token: string) => void) {
  refreshSubscribers.push(callback)
}

/**
 * 通知所有订阅者 Token 已刷新
 */
function onTokenRefreshed(token: string) {
  refreshSubscribers.forEach((callback) => callback(token))
  refreshSubscribers = []
}

/**
 * 刷新 Token 失败，跳转登录
 */
function handleRefreshFailed() {
  refreshSubscribers = []
  isRefreshing = false
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(TOKEN_EXPIRES_KEY)
  localStorage.removeItem(ORG_ID_KEY)
  window.location.href = '/login'
}

/**
 * 创建 Axios 实例
 */
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * 获取当前组织 ID
 */
function getOrgId(): string | null {
  return localStorage.getItem(ORG_ID_KEY)
}

/**
 * 获取 Token
 */
function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 请求拦截器
 */
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 添加组织 ID 请求头
    const orgId = getOrgId()
    if (orgId) {
      config.headers['X-Org-Id'] = orgId
    }

    // 添加认证 Token（如果有）
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

/**
 * 响应拦截器
 */
request.interceptors.response.use(
  (response: AxiosResponse<Result<unknown>>) => {
    const result = response.data

    // 处理统一响应格式
    if (result.success) {
      return result.data as unknown as AxiosResponse
    }

    // 业务错误
    // 如果是 409 引用冲突错误，交由业务层处理（通过 handleReferenceConflictError），不在此处弹出提示
    if (result.code === '409' && (result.message?.includes('引用') || result.message?.includes('无法删除'))) {
      return Promise.reject(new Error(result.message))
    }

    Message.error(result.message || '请求失败')
    return Promise.reject(new Error(result.message || '请求失败'))
  },
  async (error: AxiosError<Result<unknown>>) => {
    const originalRequest = error.config

    // HTTP 错误
    if (error.response) {
      const status = error.response.status

      /**
       * 401 未授权处理 - Token 刷新流程
       *
       * 当 Access Token 过期时，使用 Refresh Token 获取新的 Access Token：
       * 1. 检查是否有 Refresh Token，没有则跳转登录
       * 2. 如果正在刷新，将当前请求加入等待队列
       * 3. 发起刷新请求，获取新的 Access Token（后端会从数据库读取组织上下文）
       * 4. 刷新成功后，通知所有等待的请求使用新 Token 重试
       *
       * 注意：Refresh Token 在数据库中保存了组织上下文（orgId、memberCardId），
       * 所以刷新后的新 Access Token 会包含正确的组织信息。
       */
      if (status === 401 && originalRequest) {
        const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)

        // 如果没有 Refresh Token 或者是刷新请求本身失败，直接跳转登录
        if (!refreshToken || originalRequest.url?.includes('/auth/refresh')) {
          handleRefreshFailed()
          return Promise.reject(error)
        }

        // 如果正在刷新 Token，将请求加入队列等待（避免并发刷新）
        if (isRefreshing) {
          return new Promise((resolve) => {
            subscribeTokenRefresh((token: string) => {
              originalRequest.headers['Authorization'] = `Bearer ${token}`
              resolve(request(originalRequest))
            })
          })
        }

        // 开始刷新 Token
        isRefreshing = true

        try {
          // 使用 Refresh Token 获取新的 Access Token
          // 后端会从 sys_refresh_token 表中读取组织上下文，生成包含 orgId、memberCardId 的新 Token
          const response = await axios.post(
            `${import.meta.env.VITE_API_BASE_URL || ''}/api/v1/auth/refresh`,
            { refreshToken },
            { headers: { 'Content-Type': 'application/json' } },
          )

          if (response.data.success) {
            const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data.data
            const expiresAt = Date.now() + expiresIn * 1000

            // 保存新 Token
            localStorage.setItem(TOKEN_KEY, accessToken)
            localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken)
            localStorage.setItem(TOKEN_EXPIRES_KEY, expiresAt.toString())

            // 通知所有等待的请求
            onTokenRefreshed(accessToken)
            isRefreshing = false

            // 重试原请求
            originalRequest.headers['Authorization'] = `Bearer ${accessToken}`
            return request(originalRequest)
          } else {
            handleRefreshFailed()
            return Promise.reject(error)
          }
        } catch {
          handleRefreshFailed()
          return Promise.reject(error)
        }
      }

      switch (status) {
        case 400: {
          const data = error.response.data
          console.error('[API 400]', data?.message || '请求参数错误')
          Message.error('操作失败，请稍后重试')
          return Promise.reject(new Error('操作失败，请稍后重试'))
        }
        case 403:
          Message.error('权限不足')
          break
        case 404:
          Message.error('请求的资源不存在')
          break
        case 409:
          // 引用冲突，不弹出默认错误提示，交由上层处理
          return Promise.reject(error)
        case 500:
          Message.error('服务器内部错误')
          break
        default:
          Message.error(error.response.data?.message || '请求失败')
      }
    } else if (error.request) {
      Message.error('网络错误，请检查网络连接')
    } else {
      Message.error(error.message || '请求失败')
    }

    return Promise.reject(error)
  },
)

export default request
