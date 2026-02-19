package dev.planka.api.user.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 切换组织请求
 */
public record SwitchOrganizationRequest(
        @NotBlank(message = "组织ID不能为空")
        String orgId
) {
}
