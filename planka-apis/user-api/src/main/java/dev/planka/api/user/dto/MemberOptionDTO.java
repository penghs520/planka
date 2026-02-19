package dev.planka.api.user.dto;

/**
 * 成员卡片选项 DTO
 * <p>
 * 用于下拉选择器等场景，返回简洁的成员信息
 *
 * @param memberCardId 成员卡片ID（用于筛选）
 * @param name         成员名称（用于显示）
 */
public record MemberOptionDTO(
        String memberCardId,
        String name
) {}
