package dev.planka.api.user.dto;

import java.time.LocalDateTime;

/**
 * 成员DTO
 */
public record MemberDTO(
        String id,
        String userId,
        String orgId,
        String memberCardId,
        String role,
        String status,
        String invitedBy,
        LocalDateTime joinedAt,
        // 关联的用户信息
        String email,
        String nickname,
        String avatar
) {
}
