package dev.planka.schema.service.action;

import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.api.schema.request.UpdateSchemaRequest;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.action.BuiltInActionType;
import dev.planka.domain.schema.definition.action.CardActionConfigDefinition;
import dev.planka.schema.service.common.SchemaCommonService;
import dev.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 卡片动作配置服务
 * <p>
 * 提供卡片动作配置的查询接口。
 * 创建、更新、删除操作通过 SchemaCommonService 统一处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardActionConfigService {

    private final SchemaQuery schemaQuery;
    private final SchemaCommonService schemaCommonService;

    /**
     * 根据卡片类型ID获取所有动作配置
     * <p>
     * 返回已持久化的动作 + 未持久化的内置动作默认配置。
     * 内置动作如果未被持久化，使用固定 ID 格式: builtin:{cardTypeId}:{builtInActionType}
     *
     * @param cardTypeId 卡片类型ID
     * @return 动作配置列表
     */
    public Result<List<CardActionConfigDefinition>> getByCardTypeId(CardTypeId cardTypeId) {
        // 1. 查询已持久化的动作
        List<SchemaDefinition<?>> schemas = schemaQuery.queryByBelongTo(
                cardTypeId.value(),
                SchemaSubType.CARD_ACTION_CONFIG
        );

        List<CardActionConfigDefinition> persistedActions = schemas.stream()
                .filter(CardActionConfigDefinition.class::isInstance)
                .map(CardActionConfigDefinition.class::cast)
                .toList();

        // 2. 收集已持久化的内置动作类型
        Set<BuiltInActionType> persistedBuiltInTypes = persistedActions.stream()
                .filter(CardActionConfigDefinition::isBuiltIn)
                .map(CardActionConfigDefinition::getBuiltInActionType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. 生成未持久化的内置动作默认配置
        List<CardActionConfigDefinition> defaultBuiltInActions =
                Arrays.stream(BuiltInActionType.values())
                        .filter(type -> !persistedBuiltInTypes.contains(type))
                        .map(type -> type.createDefaultAction(cardTypeId))
                        .toList();

        // 4. 合并并排序
        List<CardActionConfigDefinition> allActions = new ArrayList<>();
        allActions.addAll(persistedActions);
        allActions.addAll(defaultBuiltInActions);

        allActions.sort((a, b) -> {
            // 内置动作优先
            if (a.isBuiltIn() && !b.isBuiltIn()) return -1;
            if (!a.isBuiltIn() && b.isBuiltIn()) return 1;
            Integer orderA = a.getSortOrder() != null ? a.getSortOrder() : Integer.MAX_VALUE;
            Integer orderB = b.getSortOrder() != null ? b.getSortOrder() : Integer.MAX_VALUE;
            return orderA.compareTo(orderB);
        });

        return Result.success(allActions);
    }

    /**
     * 更新动作配置（处理内置动作首次编辑场景）
     * <p>
     * 如果是默认内置动作（ID 以 builtin: 开头），首次编辑时需要创建持久化记录。
     * 已持久化的动作正常更新。
     *
     * @param actionId 动作ID
     * @param orgId 组织ID
     * @param operatorId 操作者ID
     * @param definition 动作定义
     * @param expectedVersion 期望版本（内置动作首次保存时为 null）
     * @return 更新后的动作配置
     */
    public Result<CardActionConfigDefinition> updateAction(
            String actionId,
            String orgId,
            String operatorId,
            CardActionConfigDefinition definition,
            Integer expectedVersion) {

        // 检查是否是默认内置动作 ID（未持久化）
        if (actionId.startsWith("builtin:")) {
            // 首次编辑，需要创建持久化记录
            // 创建新的定义对象（带新生成的 ID）
            CardActionConfigDefinition newDefinition = new CardActionConfigDefinition(
                    null, // null 会触发自动生成新 ID
                    orgId,
                    definition.getName()
            );
            // 复制属性
            newDefinition.setDescription(definition.getDescription());
            newDefinition.setCardTypeId(definition.getCardTypeId());
            newDefinition.setActionCategory(definition.getActionCategory());
            newDefinition.setBuiltIn(definition.isBuiltIn());
            newDefinition.setBuiltInActionType(definition.getBuiltInActionType());
            newDefinition.setIcon(definition.getIcon());
            newDefinition.setColor(definition.getColor());
            newDefinition.setExecutionType(definition.getExecutionType());
            newDefinition.setVisibilityConditions(definition.getVisibilityConditions());
            newDefinition.setExecutionConditions(definition.getExecutionConditions());
            newDefinition.setConfirmMessage(definition.getConfirmMessage());
            newDefinition.setSuccessMessage(definition.getSuccessMessage());
            newDefinition.setSortOrder(definition.getSortOrder());
            newDefinition.setEnabled(definition.isEnabled());

            CreateSchemaRequest request = new CreateSchemaRequest();
            request.setDefinition(newDefinition);

            Result<SchemaDefinition<?>> createResult = schemaCommonService.create(orgId, operatorId, request);
            if (!createResult.isSuccess()) {
                return Result.failure(createResult.getCode(), createResult.getMessage());
            }
            return Result.success((CardActionConfigDefinition) createResult.getData());
        } else {
            // 已持久化，正常更新
            UpdateSchemaRequest request = new UpdateSchemaRequest();
            request.setDefinition(definition);
            request.setExpectedVersion(expectedVersion);

            Result<SchemaDefinition<?>> updateResult = schemaCommonService.update(actionId, operatorId, request);
            if (!updateResult.isSuccess()) {
                return Result.failure(updateResult.getCode(), updateResult.getMessage());
            }
            return Result.success((CardActionConfigDefinition) updateResult.getData());
        }
    }

    /**
     * 获取组织下所有卡片动作配置
     *
     * @param orgId 组织ID
     * @return 动作配置列表
     */
    public Result<List<CardActionConfigDefinition>> getByOrgId(String orgId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.queryPaged(
                orgId,
                SchemaType.CARD_ACTION,
                0,
                Integer.MAX_VALUE
        );

        List<CardActionConfigDefinition> actions = schemas.stream()
                .filter(CardActionConfigDefinition.class::isInstance)
                .map(CardActionConfigDefinition.class::cast)
                .toList();

        return Result.success(actions);
    }
}
