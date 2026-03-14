/**
 * 评论相关类型定义
 */

/**
 * 评论状态
 */
export type CommentStatus = 'ACTIVE' | 'WITHDRAWN' | 'DELETED'

/**
 * 操作来源类型
 */
export interface OperationSource {
  type: 'USER' | 'BIZ_RULE' | 'API_CALL'
  displayName: string
  ruleId?: string
  ruleName?: string
  appName?: string
}

/**
 * @提及 DTO
 */
export interface MentionDTO {
  id: string
  commentId: string
  mentionedMemberId: string
  mentionedMemberName: string | null
  startOffset: number
  endOffset: number
}

/**
 * #卡片引用 DTO
 */
export interface CardRefDTO {
  id: string
  commentId: string
  refCardId: string
  refCardCode: string
  refCardTitle: string | null
  startOffset: number
  endOffset: number
}

/**
 * 评论 DTO
 */
export interface CommentDTO {
  id: string
  orgId: string
  cardId: string
  cardTypeId: string
  parentId: string | null
  rootId: string | null
  replyToMemberId: string | null
  replyToMemberName: string | null
  content: string
  status: CommentStatus
  editCount: number
  lastEditedAt: string | null
  authorId: string
  authorName: string | null
  authorAvatar: string | null
  createdAt: string
  updatedAt: string
  mentions: MentionDTO[]
  cardRefs: CardRefDTO[]
  replies: CommentDTO[]
  operationSource: OperationSource | null
}

/**
 * 评论列表响应
 */
export interface CommentListResponse {
  comments: CommentDTO[]
  total: number
  page: number
  size: number
  hasMore: boolean
}

/**
 * @提及输入
 */
export interface MentionInput {
  mentionedMemberId: string
  startOffset: number
  endOffset: number
}

/**
 * #卡片引用输入
 */
export interface CardRefInput {
  refCardId: string
  refCardCode: string
  startOffset: number
  endOffset: number
}

/**
 * 创建评论请求
 */
export interface CreateCommentRequest {
  cardId: string
  cardTypeId: string
  parentId?: string
  replyToMemberId?: string
  content: string
  mentions?: MentionInput[]
  cardRefs?: CardRefInput[]
}

/**
 * 更新评论请求
 */
export interface UpdateCommentRequest {
  content: string
  mentions?: MentionInput[]
  cardRefs?: CardRefInput[]
}

/**
 * 成员建议
 */
export interface MemberSuggestion {
  id: string
  name: string
  avatar: string | null
}

/**
 * 卡片建议
 */
export interface CardSuggestion {
  id: string
  code: string
  title: string
  cardTypeId: string
}
