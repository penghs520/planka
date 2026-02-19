package dev.planka.infra.cache.schema.query;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.CardActionId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.action.BuiltInActionType;
import dev.planka.domain.schema.definition.action.CardActionConfigDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 卡片动作配置缓存查询
 * <p>
 * 支持返回持久化的动作和默认的内置动作。
 * 内置动作使用固定 ID 格式: builtin:{cardTypeId}:{builtInActionType}
 */
@Component
public class CardActionCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public CardActionCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    /**
     * 根据ID获取卡片动作配置
     * <p>
     * 支持获取持久化的动作和默认的内置动作。
     * 如果 ID 以 builtin: 开头，则解析出默认内置动作配置。
     */
    public Optional<CardActionConfigDefinition> getById(CardActionId id) {
        String idValue = id.value();

        // 检查是否是内置动作固定 ID
        if (idValue.startsWith("builtin:")) {
            return parseBuiltInActionId(idValue)
                    .map(parsed -> parsed.actionType()
                            .createDefaultAction(CardTypeId.of(parsed.cardTypeId())));
        }

        return schemaCacheService.getById(id)
                .filter(CardActionConfigDefinition.class::isInstance)
                .map(CardActionConfigDefinition.class::cast);
    }

    /**
     * 根据ID批量获取卡片动作配置
     */
    public List<CardActionConfigDefinition> getByIds(Set<CardActionId> ids) {
        List<CardActionConfigDefinition> result = new ArrayList<>();

        Set<String> persistedIds = new HashSet<>();
        for (CardActionId id : ids) {
            String idValue = id.value();
            if (idValue.startsWith("builtin:")) {
                parseBuiltInActionId(idValue)
                        .map(parsed -> parsed.actionType()
                                .createDefaultAction(CardTypeId.of(parsed.cardTypeId())))
                        .ifPresent(result::add);
            } else {
                persistedIds.add(idValue);
            }
        }

        if (!persistedIds.isEmpty()) {
            result.addAll(schemaCacheService.getByIds(persistedIds).values().stream()
                    .filter(CardActionConfigDefinition.class::isInstance)
                    .map(CardActionConfigDefinition.class::cast)
                    .toList());
        }

        return result;
    }

    /**
     * 根据卡片类型ID获取所有动作配置
     * <p>
     * 返回已持久化的动作 + 未持久化的内置动作默认配置。
     */
    public List<CardActionConfigDefinition> getByCardTypeId(CardTypeId cardTypeId) {
        // 1. 查询已持久化的动作
        List<CardActionConfigDefinition> persistedActions =
                schemaCacheService.getBySecondaryIndex(cardTypeId, SchemaType.CARD_ACTION)
                        .stream()
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

        return allActions;
    }

    /**
     * 解析内置动作固定 ID
     * 格式: builtin:{cardTypeId}:{builtInActionType}
     */
    private Optional<ParsedBuiltInId> parseBuiltInActionId(String id) {
        if (!id.startsWith("builtin:")) {
            return Optional.empty();
        }

        String[] parts = id.split(":", 3);
        if (parts.length != 3) {
            return Optional.empty();
        }

        try {
            String cardTypeId = parts[1];
            BuiltInActionType actionType = BuiltInActionType.valueOf(parts[2]);
            return Optional.of(new ParsedBuiltInId(cardTypeId, actionType));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private record ParsedBuiltInId(String cardTypeId, BuiltInActionType actionType) {
    }
}
