import request from './request'
import type {
  CardDTO,
  CreateCardRequest,
  UpdateCardRequest,
  BatchOperationRequest,
  MoveCardRequest,
  Yield,
  CardDetailResponse,
  CardPageQueryRequest,
  PageResult,
} from '@/types/card'

const CARD_URL = '/api/v1/cards'

/**
 * 卡片 API
 */
export const cardApi = {
  /**
   * 创建卡片
   */
  create(data: CreateCardRequest): Promise<{ value: string }> {
    return request.post(CARD_URL, data)
  },

  /**
   * 更新卡片
   */
  update(data: UpdateCardRequest): Promise<void> {
    return request.put(CARD_URL, data)
  },

  /**
   * 获取卡片详情（含模板和字段配置）
   * 使用 GET 方法，返回完整的详情数据
   */
  getDetail(cardId: string): Promise<CardDetailResponse> {
    return request.get(`${CARD_URL}/${cardId}/detail`)
  },

  /**
   * 获取卡片数据（仅卡片数据，使用 POST 方法）
   */
  getById(cardId: string, yieldConfig?: Yield): Promise<CardDTO> {
    return request.post(`${CARD_URL}/${cardId}/detail`, yieldConfig || {})
  },

  /**
   * 分页查询卡片
   */
  pageQuery(data: CardPageQueryRequest): Promise<PageResult<CardDTO>> {
    return request.post(`${CARD_URL}/page-query`, data)
  },

  /**
   * 丢弃卡片（放入回收站）
   */
  discard(cardId: string, reason?: string): Promise<void> {
    const params = reason ? { discardReason: reason } : {}
    return request.delete(`${CARD_URL}/${cardId}`, { params })
  },

  /**
   * 批量归档卡片
   */
  batchArchive(data: BatchOperationRequest): Promise<void> {
    return request.post(`${CARD_URL}/archive`, data)
  },

  /**
   * 批量还原卡片
   */
  batchRestore(data: BatchOperationRequest): Promise<void> {
    return request.post(`${CARD_URL}/restore`, data)
  },

  /**
   * 批量丢弃卡片
   */
  batchDiscard(data: BatchOperationRequest): Promise<void> {
    return request.delete(`${CARD_URL}/batch`, { data })
  },

  /**
   * 移动卡片到新状态
   */
  move(data: MoveCardRequest): Promise<void> {
    return request.post(`${CARD_URL}/move`, data)
  },
}
