package dev.planka.api.user.dto;

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
        Boolean attendanceEnabled,
        String status,
        String createdBy,
        LocalDateTime createdAt
) {
}
