package cn.agilean.kanban.api.comment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 更新评论请求
 */
public record UpdateCommentRequest(
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 10000, message = "评论内容不能超过10000字符")
        String content,

        List<CreateCommentRequest.MentionInput> mentions,

        List<CreateCommentRequest.CardRefInput> cardRefs
) {
}
