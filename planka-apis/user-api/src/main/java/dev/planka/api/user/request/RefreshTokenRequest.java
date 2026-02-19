package dev.planka.api.user.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新Token请求
 */
public record RefreshTokenRequest(
        @NotBlank(message = "刷新令牌不能为空")
        String refreshToken
) {
}
