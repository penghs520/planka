package cn.agilean.kanban.api.comment.dto;

import java.util.List;

/**
 * 评论列表响应
 */
public record CommentListResponse(
        List<CommentDTO> comments,
        long total,
        int page,
        int size,
        boolean hasMore
) {
}
