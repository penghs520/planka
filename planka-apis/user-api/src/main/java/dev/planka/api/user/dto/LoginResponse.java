package dev.planka.api.user.dto;

import java.util.List;

/**
 * 登录响应
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserDTO user,
        List<OrganizationDTO> organizations,
        boolean requirePasswordChange
) {
}
