package cn.agilean.kanban.api.comment.request;

import cn.agilean.kanban.event.comment.OperationSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建评论请求
 */
public record CreateCommentRequest(
        @NotBlank(message = "卡片ID不能为空")
        String cardId,

        @NotBlank(message = "卡片类型ID不能为空")
        String cardTypeId,

        String parentId,

        String replyToMemberId,

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 10000, message = "评论内容不能超过10000字符")
        String content,

        List<MentionInput> mentions,

        List<CardRefInput> cardRefs,

        /**
         * 操作来源
         * 用户直接操作时可为空，业务规则触发时传入规则信息
         */
        OperationSource operationSource
) {
    /**
     * 获取 parentId 的 Long 值
     */
    public Long getParentIdAsLong() {
        return parentId != null && !parentId.isBlank() ? Long.parseLong(parentId) : null;
    }

    public record MentionInput(
            String mentionedMemberId,
            int startOffset,
            int endOffset
    ) {
    }

    public record CardRefInput(
            String refCardId,
            String refCardCode,
            int startOffset,
            int endOffset
    ) {
    }
}
