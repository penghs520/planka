package cn.planka.card.service.permission;

import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;
import cn.planka.domain.schema.definition.permission.PermissionConfigDefinition;

import java.util.List;

/**
 * 权限配置缓存服务
 * <p>
 * 封装权限配置查询逻辑，基于 SchemaCacheService 提供按__PLANKA_EINST__查询权限配置的能力。
 */
public interface PermissionConfigCacheService {

    /**
     * 查询权限配置
     * <p>
     * 查询指定__PLANKA_EINST__的权限配置列表。
     *
     * @param cardTypeId __PLANKA_EINST__ID
     * @return 权限配置列表（可能为空）
     */
    List<PermissionConfigDefinition> getPermissionConfigs(CardTypeId cardTypeId);

    /**
     * 检查是否有权限配置
     * <p>
     * 快速检查指定__PLANKA_EINST__是否存在权限配置，用于卫语句优化。
     *
     * @param cardTypeId __PLANKA_EINST__ID
     * @param operation  卡片操作（可选，null 表示不限制操作类型）
     * @return 是否存在权限配置
     */
    boolean hasCardOperationPermissionConfig(CardTypeId cardTypeId, CardOperation operation);
}
