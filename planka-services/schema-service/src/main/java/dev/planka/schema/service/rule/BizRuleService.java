package dev.planka.schema.service.rule;

import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.rule.BizRuleDefinition;
import dev.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务规则服务
 * <p>
 * 提供业务规则的查询接口。
 * 创建、更新、删除操作通过 SchemaCommonService 统一处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizRuleService {

    private final SchemaQuery schemaQuery;

    /**
     * 根据卡片类型ID获取所有业务规则
     *
     * @param cardTypeId 卡片类型ID
     * @return 业务规则列表（按优先级排序）
     */
    public Result<List<BizRuleDefinition>> getByCardTypeId(CardTypeId cardTypeId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.queryByBelongTo(
                cardTypeId.value(),
                SchemaSubType.BIZ_RULE
        );

        List<BizRuleDefinition> rules = schemas.stream()
                .filter(BizRuleDefinition.class::isInstance)
                .map(BizRuleDefinition.class::cast)
                .toList();

        return Result.success(rules);
    }

    /**
     * 获取组织下所有业务规则
     *
     * @param orgId 组织ID
     * @return 业务规则列表
     */
    public Result<List<BizRuleDefinition>> getByOrgId(String orgId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.queryPaged(
                orgId,
                SchemaType.BIZ_RULE,
                0,
                Integer.MAX_VALUE
        );

        List<BizRuleDefinition> rules = schemas.stream()
                .filter(BizRuleDefinition.class::isInstance)
                .map(BizRuleDefinition.class::cast)
                .toList();

        return Result.success(rules);
    }

    /**
     * 根据卡片类型ID获取所有已启用的业务规则
     *
     * @param cardTypeId 卡片类型ID
     * @return 已启用的业务规则列表（按优先级排序）
     */
    public Result<List<BizRuleDefinition>> getEnabledByCardTypeId(CardTypeId cardTypeId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.queryByBelongTo(
                cardTypeId.value(),
                SchemaSubType.BIZ_RULE
        );

        List<BizRuleDefinition> rules = schemas.stream()
                .filter(BizRuleDefinition.class::isInstance)
                .map(BizRuleDefinition.class::cast)
                .filter(BizRuleDefinition::isEnabled)
                .toList();

        return Result.success(rules);
    }
}
