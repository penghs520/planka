package cn.agilean.kanban.card.service.permission;

import cn.agilean.kanban.domain.card.CardTypeId;
import cn.agilean.kanban.domain.schema.SchemaType;
import cn.agilean.kanban.domain.schema.definition.SchemaDefinition;
import cn.agilean.kanban.domain.schema.definition.permission.PermissionConfig.CardOperation;
import cn.agilean.kanban.domain.schema.definition.permission.PermissionConfigDefinition;
import cn.agilean.kanban.infra.cache.schema.SchemaCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限配置缓存服务实现
 * <p>
 * 基于 SchemaCacheService 的二级索引查询实现权限配置查询。
 * 使用 "CARD_TYPE" 索引类型，以 cardTypeId 为索引键查询。
 */
@Slf4j
@Service
public class PermissionConfigCacheServiceImpl implements PermissionConfigCacheService {


    private final SchemaCacheService schemaCacheService;

    public PermissionConfigCacheServiceImpl(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    @Override
    public List<PermissionConfigDefinition> getPermissionConfigs(CardTypeId cardTypeId) {

        return schemaCacheService.getBySecondaryIndex(
                        cardTypeId,
                        SchemaType.CARD_PERMISSION
                )
                .stream()
                .filter(PermissionConfigDefinition.class::isInstance)
                .map(PermissionConfigDefinition.class::cast)
                .toList();
    }

    @Override
    public boolean hasCardOperationPermissionConfig(CardTypeId cardTypeId, CardOperation operation) {
        List<PermissionConfigDefinition> configs = getPermissionConfigs(cardTypeId);
        return hasOperationRule(configs, operation);
    }

    /**
     * 检查权限配置列表中是否有指定操作的规则
     */
    private boolean hasOperationRule(List<PermissionConfigDefinition> configs, CardOperation operation) {
        if (operation == null) {
            return !configs.isEmpty();
        }

        for (PermissionConfigDefinition config : configs) {
            if (config.getCardOperations() == null) {
                continue;
            }
            for (var cardOp : config.getCardOperations()) {
                if (cardOp.getOperations() != null && cardOp.getOperations().contains(operation)) {
                    return true;
                }
            }
        }
        return false;
    }
}
