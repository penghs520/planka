package dev.planka.extension.comment.controller;

import dev.planka.api.comment.dto.CommentDTO;
import dev.planka.api.comment.dto.CommentListResponse;
import dev.planka.api.comment.request.CreateCommentRequest;
import dev.planka.api.comment.request.UpdateCommentRequest;
import dev.planka.common.result.Result;
import dev.planka.extension.comment.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 获取卡片评论列表
     */
    @GetMapping
    public Result<CommentListResponse> listComments(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam("cardId") String cardId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        CommentListResponse response = commentService.listComments(orgId, cardId, page, size);
        return Result.success(response);
    }

    /**
     * 创建评论
     */
    @PostMapping
    public Result<CommentDTO> createComment(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String memberId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentDTO comment = commentService.createComment(orgId, memberId, request);
        return Result.success(comment);
    }

    /**
     * 更新评论（撤回后重编辑）
     */
    @PutMapping("/{id}")
    public Result<CommentDTO> updateComment(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String memberId,
            @Valid @RequestBody UpdateCommentRequest request) {
        CommentDTO comment = commentService.updateComment(id, orgId, memberId, request);
        return Result.success(comment);
    }

    /**
     * 撤回评论
     */
    @PostMapping("/{id}/withdraw")
    public Result<CommentDTO> withdrawComment(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String memberId) {
        CommentDTO comment = commentService.withdrawComment(id, orgId, memberId);
        return Result.success(comment);
    }

    /**
     * 删除评论（管理员）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String memberId) {
        commentService.deleteComment(id, orgId, memberId);
        return Result.success(null);
    }
}
