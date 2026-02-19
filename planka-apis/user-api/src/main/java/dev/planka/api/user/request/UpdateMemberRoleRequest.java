package dev.planka.api.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 修改成员角色请求
 */
public record UpdateMemberRoleRequest(
        @NotBlank(message = "角色不能为空")
        @Pattern(regexp = "^(ADMIN|MEMBER)$", message = "角色只能是 ADMIN 或 MEMBER")
        String role
) {
}
