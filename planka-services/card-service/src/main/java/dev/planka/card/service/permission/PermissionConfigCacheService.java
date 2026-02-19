package dev.planka.card.service.permission;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;
import dev.planka.domain.schema.definition.permission.PermissionConfigDefinition;

import java.util.List;

/**
 * 权限配置缓存服务
 * <p>
 * 封装权限配置查询逻辑，基于 SchemaCacheService 提供按卡片类型查询权限配置的能力。
 */
public interface PermissionConfigCacheService {

    /**
     * 查询组织级权限配置
     * <p>
     * 查询指定卡片类型的组织级权限配置列表。
     *
     * @param cardTypeId 卡片类型ID
     * @return 权限配置列表（可能为空）
     */
    List<PermissionConfigDefinition> getPermissionConfigs(CardTypeId cardTypeId);

    /**
     * 检查是否有组织级权限配置
     * <p>
     * 快速检查指定卡片类型是否存在组织级权限配置，用于卫语句优化。
     *
     * @param cardTypeId 卡片类型ID
     * @param operation  卡片操作（可选，null 表示不限制操作类型）
     * @return 是否存在权限配置
     */
    boolean hasPermissionConfig(CardTypeId cardTypeId, CardOperation operation);
}
