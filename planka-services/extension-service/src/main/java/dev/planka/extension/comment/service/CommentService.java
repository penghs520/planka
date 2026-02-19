package dev.planka.extension.comment.service;

import dev.planka.api.comment.dto.CommentDTO;
import dev.planka.api.comment.dto.CommentListResponse;
import dev.planka.api.comment.request.CreateCommentRequest;
import dev.planka.api.comment.request.UpdateCommentRequest;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 获取卡片评论列表
     */
    CommentListResponse listComments(String orgId, String cardId, int page, int size);

    /**
     * 创建评论
     */
    CommentDTO createComment(String orgId, String memberId, CreateCommentRequest request);

    /**
     * 更新评论（撤回后重编辑）
     */
    CommentDTO updateComment(Long id, String orgId, String memberId, UpdateCommentRequest request);

    /**
     * 撤回评论
     */
    CommentDTO withdrawComment(Long id, String orgId, String memberId);

    /**
     * 删除评论（管理员）
     */
    void deleteComment(Long id, String orgId, String memberId);
}
