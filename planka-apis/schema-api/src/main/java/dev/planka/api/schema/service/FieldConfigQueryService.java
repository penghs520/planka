package dev.planka.api.schema.service;

import dev.planka.api.schema.dto.inheritance.FieldConfigListWithSource;
import dev.planka.api.schema.dto.inheritance.FieldSourceInfo;
import dev.planka.api.schema.spi.SchemaDataProvider;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.Result;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.cardtype.AbstractCardType;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.domain.schema.definition.linkconfig.LinkFieldConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 属性配置查询服务
 * 提供卡片类型属性配置的获取功能： 继承优先级（从高到低）：自身 > 显式父类 > 任意卡属性集
 */
public class FieldConfigQueryService {

    private final SchemaDataProvider dataProvider;
    private final FieldConfigResolver fieldConfigResolver;

    public FieldConfigQueryService(SchemaDataProvider dataProvider) {
        this.dataProvider = dataProvider;
        this.fieldConfigResolver = new FieldConfigResolver(dataProvider);
    }

    /**
     * 获取卡片类型的完整属性配置列表（包含继承和冲突信息）
     *
     * @param cardTypeId 卡片类型ID
     * @return 属性配置列表
     */
    public Result<FieldConfigListWithSource> getFieldConfigListWithSource(String cardTypeId) {
        Optional<CardTypeDefinition> schemaOpt = dataProvider.getCardTypeById(cardTypeId);
        if (schemaOpt.isEmpty()) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "卡片类型不存在");
        }

        CardTypeDefinition cardType = schemaOpt.get();
        boolean isConcrete = cardType instanceof EntityCardType;

        if (!isConcrete && !(cardType instanceof AbstractCardType)) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "指定的 Schema 不是卡片类型");
        }

        return Result.success(buildFieldConfigList(cardType, isConcrete));
    }

    // ==================== 构建配置列表 ====================

    private FieldConfigListWithSource buildFieldConfigList(CardTypeDefinition cardType, boolean isConcrete) {
        String cardTypeId = cardType.getId().value();
        FieldConfigResolver.ResolvedFieldConfigs resolved = fieldConfigResolver.resolve(cardType);
        Map<String, String> cardTypeNames = buildCardTypeNameCache(resolved, cardType);

        List<FieldConfig> resultFields = new ArrayList<>();
        Map<String, FieldSourceInfo> fieldSources = new HashMap<>();

        // 处理所有属性配置（包括普通属性和关联属性）
        processFieldConfigs(cardType, resolved, cardTypeNames, resultFields, fieldSources);

        return FieldConfigListWithSource.builder()
                .cardTypeId(cardTypeId)
                .cardTypeName(cardType.getName())
                .fields(resultFields)
                .fieldSources(fieldSources)
                .build();
    }

    // 系统内置的 LinkType ID 模式（创建人、归档人、丢弃人）
    private static final String LINK_CREATOR_PATTERN = ":link:creator:";
    private static final String LINK_ARCHIVER_PATTERN = ":link:archiver:";
    private static final String LINK_DISCARDER_PATTERN = ":link:discarder:";

    /**
     * 判断是否为系统内置的 LinkFieldConfig（创建人、归档人、丢弃人）
     * 这些字段虽然是 LinkFieldConfig，但应该被视为普通属性配置显示
     */
    private boolean isSystemBuiltInLinkField(String fieldId) {
        return fieldId.contains(LINK_CREATOR_PATTERN) ||
               fieldId.contains(LINK_ARCHIVER_PATTERN) ||
               fieldId.contains(LINK_DISCARDER_PATTERN);
    }

    private void processFieldConfigs(
            CardTypeDefinition cardType,
            FieldConfigResolver.ResolvedFieldConfigs resolved,
            Map<String, String> cardTypeNames,
            List<FieldConfig> resultFields,
            Map<String, FieldSourceInfo> fieldSources) {

        String cardTypeId = cardType.getId().value();

        for (Map.Entry<String, FieldConfig> entry : resolved.fieldConfigs().entrySet()) {
            String fieldId = entry.getKey();
            FieldConfig config = entry.getValue();
            String configSourceId = resolved.configSources().get(fieldId);

            resultFields.add(config);

            boolean isFromLinkTypeDef = resolved.fromLinkTypeDefinition().contains(fieldId) && !isSystemBuiltInLinkField(fieldId);
            boolean inherited = !cardTypeId.equals(configSourceId);
            boolean isLinkField = config instanceof LinkFieldConfig && !isSystemBuiltInLinkField(fieldId);

            fieldSources.put(fieldId, FieldSourceInfo.builder()
                    .definitionSourceCardTypeId(configSourceId)
                    .definitionSourceCardTypeName(cardTypeNames.getOrDefault(configSourceId, configSourceId))
                    .configSourceCardTypeId(isFromLinkTypeDef ? null : configSourceId)
                    .configSourceCardTypeName(isFromLinkTypeDef ? null : cardTypeNames.getOrDefault(configSourceId, configSourceId))
                    .definitionInherited(inherited && !isLinkField)
                    .configInherited(inherited)
                    .fromLinkTypeDefinition(isFromLinkTypeDef)
                    .build());
        }
    }

    // ==================== 辅助方法 ====================

    private Map<String, String> buildCardTypeNameCache(
            FieldConfigResolver.ResolvedFieldConfigs resolved, CardTypeDefinition cardType) {

        Set<String> cardTypeIds = new HashSet<>();
        cardTypeIds.add(cardType.getId().value());
        cardTypeIds.addAll(resolved.configSources().values());

        List<SchemaDefinition<?>> cardTypes = dataProvider.getSchemasByIds(cardTypeIds);
        Map<String, String> result = cardTypes.stream()
                .collect(Collectors.toMap(
                        s -> s.getId().value(),
                        SchemaDefinition::getName,
                        (a, b) -> a));
        result.put(cardType.getId().value(), cardType.getName());
        return result;
    }
}
