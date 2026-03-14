/**
 * 统一 API 响应结果
 */
export interface Result<T> {
  code: string
  message: string
  data: T
  success: boolean
  timestamp: number
}

/**
 * 分页响应结果
 */
export interface PageResult<T> {
  page: number
  size: number
  total: number
  totalPages: number
  content: T[]
  hasPrevious: boolean
  hasNext: boolean
  first: boolean
  last: boolean
}

/**
 * 分页请求参数
 */
export interface PageRequest {
  page?: number
  size?: number
}
