package dev.planka.api.user.request;

import jakarta.validation.constraints.Size;

/**
 * 更新组织请求
 */
public record UpdateOrganizationRequest(
        @Size(max = 200, message = "组织名称不能超过200个字符")
        String name,

        @Size(max = 1000, message = "组织描述不能超过1000个字符")
        String description,

        String logo,

        Boolean attendanceEnabled
) {
}
