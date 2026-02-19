package dev.planka.card.service.permission;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;
import dev.planka.domain.schema.definition.permission.PermissionConfigDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
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

    /**
     * 二级索引类型：卡片类型
     */
    private static final String INDEX_TYPE_CARD_TYPE = "CARD_TYPE";

    private final SchemaCacheService schemaCacheService;

    public PermissionConfigCacheServiceImpl(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    @Override
    public List<PermissionConfigDefinition> getPermissionConfigs(CardTypeId cardTypeId) {

        List<SchemaDefinition<?>> schemas = schemaCacheService.getBySecondaryIndex(
                cardTypeId,
                SchemaType.CARD_PERMISSION
        );

        // 过滤出组织级配置
        return filterPermissionConfigs(schemas);
    }

    @Override
    public boolean hasPermissionConfig(CardTypeId cardTypeId, CardOperation operation) {
        List<PermissionConfigDefinition> configs = getPermissionConfigs(cardTypeId);
        return hasOperationRule(configs, operation);
    }

    /**
     * 过滤出 PermissionConfigDefinition 类型的配置
     *
     * @param schemas Schema 定义列表
     * @return 过滤后的权限配置列表
     */
    private List<PermissionConfigDefinition> filterPermissionConfigs(List<SchemaDefinition<?>> schemas) {
        List<PermissionConfigDefinition> result = new ArrayList<>();
        for (SchemaDefinition<?> schema : schemas) {
            if (schema instanceof PermissionConfigDefinition permConfig) {
                result.add(permConfig);
            }
        }
        return result;
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
                if (cardOp.getOperation() == operation) {
                    return true;
                }
            }
        }
        return false;
    }
}
