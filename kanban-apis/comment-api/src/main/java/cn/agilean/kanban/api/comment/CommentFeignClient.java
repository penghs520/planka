package cn.agilean.kanban.api.comment;

import cn.agilean.kanban.api.comment.dto.CommentDTO;
import cn.agilean.kanban.api.comment.dto.CommentListResponse;
import cn.agilean.kanban.api.comment.request.CreateCommentRequest;
import cn.agilean.kanban.api.comment.request.UpdateCommentRequest;
import cn.agilean.kanban.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 评论服务 Feign 客户端
 */
@FeignClient(name = "comment-service", path = "/api/v1")
public interface CommentFeignClient {

    /**
     * 获取卡片评论列表
     */
    @GetMapping("/comments")
    Result<CommentListResponse> listComments(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam("cardId") String cardId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size);

    /**
     * 创建评论
     */
    @PostMapping("/comments")
    Result<CommentDTO> createComment(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreateCommentRequest request);

    /**
     * 更新评论（撤回后重编辑）
     */
    @PutMapping("/comments/{id}")
    Result<CommentDTO> updateComment(
            @PathVariable("id") Long id,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UpdateCommentRequest request);

    /**
     * 撤回评论
     */
    @PostMapping("/comments/{id}/withdraw")
    Result<CommentDTO> withdrawComment(
            @PathVariable("id") Long id,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-User-Id") String userId);

    /**
     * 删除评论（管理员）
     */
    @DeleteMapping("/comments/{id}")
    Result<Void> deleteComment(
            @PathVariable("id") Long id,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-User-Id") String userId);
}
