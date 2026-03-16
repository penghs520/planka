package cn.planka.api.comment.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * @提及 DTO
 */
public record MentionDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,
        @JsonSerialize(using = ToStringSerializer.class)
        Long commentId,
        String mentionedMemberId,
        String mentionedMemberName,
        int startOffset,
        int endOffset
) {
}
