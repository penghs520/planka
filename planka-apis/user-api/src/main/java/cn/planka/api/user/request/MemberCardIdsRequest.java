package cn.planka.api.user.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 按成员卡片 ID 批量查询的请求体
 */
public record MemberCardIdsRequest(
        @NotNull List<String> memberCardIds
) {}
