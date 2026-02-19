package dev.planka.infra.cache.card.model;

import dev.planka.domain.card.*;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StreamId;

/**
 * 卡片基础信息（用于权限校验快速查询）
 * <p>
 * 设计原则：
 * 1. 仅包含权限校验必需的字段
 * 2. 支持快速判断是否需要完整权限校验
 * 3. 可序列化到 Redis
 */
public record CardBasicInfo(
        // ========== 标识信息 ==========
        CardId cardId,
        OrgId orgId,
        CardTypeId cardTypeId,

        // ========== 基本信息 ==========
        CardTitle title,              // 卡片标题
        String code,                  // 卡片编号（已处理 customCode 逻辑）

        // ========== 状态信息 ==========
        CardStyle cardStyle,          // 生命周期（ACTIVE/ARCHIVED/DISCARDED）
        StreamId streamId,            // 价值流ID（可能为 null）
        StatusId statusId             // 价值流状态ID（可能为 null）
) {

    /**
     * 从 CardDTO 转换
     * <p>
     * 注意：此方法需要 CardDTO 参数，但 planka-infra-card-cache 模块不依赖 card-api 模块。
     * 因此，此方法的实际实现应该在使用方（如 card-service）中提供。
     * 这里提供一个静态方法签名作为约定。
     */
    public static CardBasicInfo fromDTO(Object dto) {
        throw new UnsupportedOperationException(
            "此方法应该在使用方实现，传入 CardDTO 并转换为 CardBasicInfo"
        );
    }
}
