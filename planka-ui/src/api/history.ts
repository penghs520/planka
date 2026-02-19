import request from './request'
import type { PageResult } from '@/types/api'
import type {
  CardHistoryRecord,
  HistorySearchRequest,
  HistoryFilters,
} from '@/types/history'

const HISTORY_URL = '/api/v1/history'

/**
 * 操作历史 API
 */
export const historyApi = {
  /**
   * 查询卡片操作历史（简单分页）
   */
  getCardHistory(
    cardTypeId: string,
    cardId: string,
    page = 1,
    size = 20
  ): Promise<PageResult<CardHistoryRecord>> {
    return request.get(`${HISTORY_URL}/cards/${cardTypeId}/${cardId}`, {
      params: { page, size },
    })
  },

  /**
   * 搜索卡片操作历史（多维度筛选）
   */
  searchCardHistory(
    cardTypeId: string,
    cardId: string,
    searchRequest: HistorySearchRequest
  ): Promise<PageResult<CardHistoryRecord>> {
    return request.post(
      `${HISTORY_URL}/cards/${cardTypeId}/${cardId}/search`,
      searchRequest
    )
  },

  /**
   * 获取可用的筛选选项
   */
  getFilters(cardTypeId: string, cardId: string): Promise<HistoryFilters> {
    return request.get(`${HISTORY_URL}/cards/${cardTypeId}/${cardId}/filters`)
  },
}
