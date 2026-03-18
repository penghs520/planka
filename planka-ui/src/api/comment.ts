import request from './request'
import type {
  CommentDTO,
  CommentListResponse,
  CreateCommentRequest,
  UpdateCommentRequest,
  MemberSuggestion,
  CardSuggestion,
} from '@/types/comment'

const BASE_URL = '/api/v1/comments'
const SUGGESTION_URL = '/api/v1/suggestions'

/**
 * 评论服务 API
 */
export const commentApi = {
  /**
   * 获取卡片评论列表
   */
  listComments(cardId: string, page = 1, size = 10): Promise<CommentListResponse> {
    return request.get(BASE_URL, {
      params: { cardId, page, size },
    })
  },

  /**
   * 创建评论
   */
  createComment(data: CreateCommentRequest): Promise<CommentDTO> {
    return request.post(BASE_URL, data)
  },

  /**
   * 更新评论（撤回后重编辑）
   */
  updateComment(id: string, data: UpdateCommentRequest): Promise<CommentDTO> {
    return request.put(`${BASE_URL}/${id}`, data)
  },

  /**
   * 撤回评论
   */
  withdrawComment(id: string): Promise<CommentDTO> {
    return request.post(`${BASE_URL}/${id}/withdraw`)
  },

  /**
   * 删除评论（管理员）
   */
  deleteComment(id: string): Promise<void> {
    return request.delete(`${BASE_URL}/${id}`)
  },

  /**
   * 获取成员建议（@自动补全）
   */
  suggestMembers(keyword = ''): Promise<MemberSuggestion[]> {
    return request.get(`${SUGGESTION_URL}/members`, {
      params: { keyword },
    })
  },

  /**
   * 获取卡片建议（#自动补全）
   */
  suggestCards(keyword = ''): Promise<CardSuggestion[]> {
    return request.get(`${SUGGESTION_URL}/cards`, {
      params: { keyword },
    })
  },
}
