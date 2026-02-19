package dev.planka.api.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 添加成员请求
 */
public record AddMemberRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 100, message = "昵称不能超过100个字符")
        String nickname,

        String role,  // ADMIN 或 MEMBER，默认 MEMBER

        String cardTypeId  // 成员卡片类型ID，当有多种成员类型时需要指定
) {
}
