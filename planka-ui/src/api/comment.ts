import request from './request'
import type {
  dto.cn.planka.api.comment.CommentDTO,
  dto.cn.planka.api.comment.CommentListResponse,
  request.cn.planka.api.comment.CreateCommentRequest,
  request.cn.planka.api.comment.UpdateCommentRequest,
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
  listComments(cardId: string, page = 1, size = 10): Promise<dto.cn.planka.api.comment.CommentListResponse> {
    return request.get(BASE_URL, {
      params: { cardId, page, size },
    })
  },

  /**
   * 创建评论
   */
  createComment(data: request.cn.planka.api.comment.CreateCommentRequest): Promise<dto.cn.planka.api.comment.CommentDTO> {
    return request.post(BASE_URL, data)
  },

  /**
   * 更新评论（撤回后重编辑）
   */
  updateComment(id: string, data: request.cn.planka.api.comment.UpdateCommentRequest): Promise<dto.cn.planka.api.comment.CommentDTO> {
    return request.put(`${BASE_URL}/${id}`, data)
  },

  /**
   * 撤回评论
   */
  withdrawComment(id: string): Promise<dto.cn.planka.api.comment.CommentDTO> {
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
