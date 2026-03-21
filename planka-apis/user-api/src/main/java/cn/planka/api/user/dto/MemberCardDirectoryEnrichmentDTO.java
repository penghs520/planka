package cn.planka.api.user.dto;

import java.time.LocalDateTime;

/**
 * 工作区成员目录：按成员卡片补齐的组织角色与上次登录时间
 *
 * @param memberCardId 成员卡片 ID
 * @param role         组织内角色（OWNER/ADMIN/MEMBER）
 * @param lastLoginAt  用户上次登录时间，可能为空
 */
public record MemberCardDirectoryEnrichmentDTO(
        String memberCardId,
        String role,
        LocalDateTime lastLoginAt
) {}
