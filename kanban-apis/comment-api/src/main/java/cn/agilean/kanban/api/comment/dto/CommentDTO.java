package cn.agilean.kanban.api.comment.dto;

import cn.agilean.kanban.event.comment.OperationSource;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论 DTO
 */
public record CommentDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,
        String orgId,
        String cardId,
        String cardTypeId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long parentId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long rootId,
        String replyToMemberId,
        String replyToMemberName,
        String content,
        String status,
        int editCount,
        LocalDateTime lastEditedAt,
        String authorId,
        String authorName,
        String authorAvatar,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<MentionDTO> mentions,
        List<CardRefDTO> cardRefs,
        List<CommentDTO> replies,

        /**
         * 操作来源
         * 用户直接操作时为空，业务规则触发时包含规则信息
         */
        OperationSource operationSource
) {
}
