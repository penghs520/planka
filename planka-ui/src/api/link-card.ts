import request from './request'
import type { LinkedCard } from '@/types/card'

const LINK_URL = '/api/v1/links'

/**
 * 可关联卡片查询请求
 */
export interface LinkableCardsRequest {
  /**
   * 关联属性 ID
   * 格式为 "{linkTypeId}:{SOURCE|TARGET}"
   */
  linkFieldId: string
  /** 搜索关键字 */
  keyword?: string
  /** 页码（从 0 开始） */
  page?: number
  /** 每页大小 */
  size?: number
}

/**
 * 更新关联关系请求
 */
export interface UpdateLinkRequest {
  /** 卡片 ID */
  cardId: string
  /**
   * 关联属性 ID
   * 格式为 "{linkTypeId}:{SOURCE|TARGET}"
   */
  linkFieldId: string
  /** 目标卡片 ID 列表 */
  targetCardIds: string[]
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  /** 数据列表 */
  content: T[]
  /** 当前页码 */
  page: number
  /** 每页大小 */
  size: number
  /** 总记录数 */
  total: number
  /** 总页数 */
  totalPages: number
  /** 是否有上一页 */
  hasPrevious: boolean
  /** 是否有下一页 */
  hasNext: boolean
}

/**
 * 关联卡片 API
 */
export const linkCardApi = {
  /**
   * 查询可关联的卡片列表
   */
  queryLinkableCards(data: LinkableCardsRequest): Promise<PageResult<LinkedCard>> {
    return request.post(`${LINK_URL}/linkable-cards`, data)
  },

  /**
   * 更新关联关系
   */
  updateLink(data: UpdateLinkRequest): Promise<void> {
    return request.put(LINK_URL, data)
  },
}
