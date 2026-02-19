package dev.planka.api.user.request;

import jakarta.validation.constraints.Size;

/**
 * 更新用户信息请求
 */
public record UpdateUserRequest(
        @Size(max = 100, message = "昵称不能超过100个字符")
        String nickname,

        String avatar,

        @Size(max = 20, message = "手机号不能超过20个字符")
        String phone
) {
}
