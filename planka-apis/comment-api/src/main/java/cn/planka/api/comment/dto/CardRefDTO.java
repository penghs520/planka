package cn.planka.api.comment.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * #卡片引用 DTO
 */
public record CardRefDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,
        @JsonSerialize(using = ToStringSerializer.class)
        Long commentId,
        String refCardId,
        String refCardCode,
        String refCardTitle,
        int startOffset,
        int endOffset
) {
}
