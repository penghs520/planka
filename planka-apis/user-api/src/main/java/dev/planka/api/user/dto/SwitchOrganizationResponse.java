package dev.planka.api.user.dto;

/**
 * 切换组织响应
 */
public record SwitchOrganizationResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String orgId,
        String memberCardId,
        String role
) {
}
