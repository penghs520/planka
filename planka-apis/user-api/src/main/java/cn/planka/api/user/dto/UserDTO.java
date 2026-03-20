package cn.planka.api.user.dto;

import java.time.LocalDateTime;

/**
 * 用户DTO
 */
public record UserDTO(
        String id,
        String email,
        String nickname,
        String avatar,
        String phone,
        String locale,
        boolean superAdmin,
        String status,
        boolean usingDefaultPassword,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
