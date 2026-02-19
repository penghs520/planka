package dev.planka.schema.service.permission;

import dev.planka.common.result.Result;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.permission.PermissionConfigDefinition;
import dev.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限配置服务
 * <p>
 * 提供权限配置的查询功能。增删改查统一使用 SchemaCommonService。
 */
@Service
@RequiredArgsConstructor
public class PermissionConfigService {

    private final SchemaQuery schemaQuery;

    /**
     * 查询卡片类型的权限配置列表
     *
     * @param cardTypeId 卡片类型ID
     * @return 权限配置列表
     */
    public Result<List<PermissionConfigDefinition>> listByCardType(String cardTypeId) {
        // 按 belongTo 查询，只查询 CARD_PERMISSION 类型
        List<SchemaDefinition<?>> definitions = schemaQuery.queryByBelongTo(
                cardTypeId, "CARD_PERMISSION");

        List<PermissionConfigDefinition> permissionConfigs = definitions.stream()
                .filter(d -> d instanceof PermissionConfigDefinition)
                .map(d -> (PermissionConfigDefinition) d)
                .collect(Collectors.toList());

        return Result.success(permissionConfigs);
    }

    /**
     * 获取权限配置
     *
     * @param cardTypeId 卡片类型ID
     * @return 权限配置（可能为空）
     */
    public Result<PermissionConfigDefinition> getConfig(String cardTypeId) {
        // 查询 CARD_PERMISSION 类型
        List<SchemaDefinition<?>> definitions = schemaQuery.queryByBelongTo(
                cardTypeId, "CARD_PERMISSION");

        PermissionConfigDefinition config = definitions.stream()
                .filter(d -> d instanceof PermissionConfigDefinition)
                .map(d -> (PermissionConfigDefinition) d)
                .findFirst()
                .orElse(null);

        return Result.success(config);
    }
}
