package cn.planka.api.user.dto;

import java.time.LocalDateTime;

/**
 * 组织DTO
 */
public record OrganizationDTO(
        String id,
        String name,
        String description,
        String logo,
        String memberCardTypeId,
        String status,
        String createdBy,
        LocalDateTime createdAt
) {
}
