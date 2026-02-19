package dev.planka.schema.service.cardtype;

import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.cardtype.AbstractCardType;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaQuery;
import dev.planka.api.schema.vo.cardtype.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 卡片类型服务
 * <p>
 * 提供卡片类型的列表查询，返回 VO 对象供前端展示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardTypeService {

    private final SchemaRepository schemaRepository;
    private final SchemaQuery schemaQuery;

    /**
     * 查询卡片类型选项列表（用于下拉框）
     * <p>
     * 只返回必要字段，不包含继承类型等详细信息
     *
     * @param orgId 组织 ID
     * @return 卡片类型选项列表
     */
    public Result<List<CardTypeOptionVO>> listCardTypeOptions(String orgId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.query(orgId, SchemaType.CARD_TYPE);

        List<CardTypeOptionVO> options = schemas.stream()
                .filter(s -> s instanceof CardTypeDefinition)
                .map(s -> (CardTypeDefinition) s)
                .map(cardType -> CardTypeOptionVO.builder()
                        .id(cardType.getId().value())
                        .name(cardType.getName())
                        .schemaSubType(cardType.getSchemaSubType())
                        .build())
                .collect(Collectors.toList());

        return Result.success(options);
    }

    /**
     * 查询卡片类型列表
     *
     * @param orgId 组织 ID
     * @return 卡片类型列表
     */
    public Result<List<CardTypeVO>> listCardTypes(String orgId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.query(orgId, SchemaType.CARD_TYPE);

        // 收集所有父类型 ID 用于批量查询名称
        Set<String> allParentTypeIds = schemas.stream()
                .filter(s -> s instanceof EntityCardType)
                .map(s -> (EntityCardType) s)
                .filter(c -> c.getParentTypeIds() != null)
                .flatMap(c -> c.getParentTypeIds().stream())
                .map(CardTypeId::value)
                .collect(Collectors.toSet());

        // 批量查询父类型名称
        Map<String, String> parentTypeNameMap = getCardTypeNameMap(allParentTypeIds);

        List<CardTypeVO> voList = schemas.stream()
                .filter(s -> s instanceof CardTypeDefinition)
                .map(s -> toVO((CardTypeDefinition) s, parentTypeNameMap))
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 根据 ID 获取卡片类型详情
     *
     * @param cardTypeId 卡片类型 ID
     * @return 卡片类型详情
     */
    public Result<CardTypeVO> getCardTypeById(String cardTypeId) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(cardTypeId);
        if (schemaOpt.isEmpty()) {
            return Result.failure("NOT_FOUND", "卡片类型不存在: " + cardTypeId);
        }

        SchemaDefinition<?> schema = schemaOpt.get();
        if (!(schema instanceof CardTypeDefinition cardType)) {
            return Result.failure("INVALID_TYPE", "非卡片类型: " + cardTypeId);
        }

        // 查询父类型名称
        Map<String, String> parentTypeNameMap = Collections.emptyMap();
        if (cardType instanceof EntityCardType concrete && concrete.getParentTypeIds() != null) {
            Set<String> parentTypeIdStrings = concrete.getParentTypeIds().stream()
                    .map(CardTypeId::value)
                    .collect(Collectors.toSet());
            parentTypeNameMap = getCardTypeNameMap(parentTypeIdStrings);
        }

        return Result.success(toVO(cardType, parentTypeNameMap));
    }

    /**
     * 批量获取卡片类型名称映射
     */
    private Map<String, String> getCardTypeNameMap(Set<String> cardTypeIds) {
        if (cardTypeIds == null || cardTypeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SchemaDefinition<?>> cardTypes = schemaRepository.findByIds(cardTypeIds);
        return cardTypes.stream()
                .collect(Collectors.toMap(
                        s -> s.getId().value(),
                        SchemaDefinition::getName,
                        (a, b) -> a));
    }

    /**
     * 将卡片类型转换为 VO
     */
    private CardTypeVO toVO(CardTypeDefinition cardType, Map<String, String> parentTypeNameMap) {
        if (cardType instanceof AbstractCardType) {
            return TraitCardTypeVO.builder()
                    .id(cardType.getId().value())
                    .orgId(cardType.getOrgId())
                    .name(cardType.getName())
                    .code(cardType.getCode())
                    .systemCardType(cardType.isSystemType())
                    .description(cardType.getDescription())
                    .enabled(cardType.isEnabled())
                    .contentVersion(cardType.getContentVersion())
                    .createdAt(cardType.getCreatedAt())
                    .updatedAt(cardType.getUpdatedAt())
                    .build();
        } else if (cardType instanceof EntityCardType concrete) {
            // 构建父类型信息列表
            List<CardTypeInfo> parentTypes = null;
            if (concrete.getParentTypeIds() != null && !concrete.getParentTypeIds().isEmpty()) {
                parentTypes = concrete.getParentTypeIds().stream()
                        .map(id -> CardTypeInfo.builder()
                                .id(id.value())
                                .name(parentTypeNameMap.getOrDefault(id.value(), id.value()))
                                .build())
                        .collect(Collectors.toList());
            }

            return EntityCardTypeVO.builder()
                    .id(cardType.getId().value())
                    .orgId(cardType.getOrgId())
                    .name(cardType.getName())
                    .code(cardType.getCode())
                    .systemCardType(cardType.isSystemType())
                    .description(cardType.getDescription())
                    .enabled(cardType.isEnabled())
                    .contentVersion(cardType.getContentVersion())
                    .createdAt(cardType.getCreatedAt())
                    .updatedAt(cardType.getUpdatedAt())
                    .parentTypeIds(concrete.getParentTypeIds())
                    .parentTypes(parentTypes)
                    .defaultCardFaceId(concrete.getDefaultCardFaceId())
                    .build();
        }

        // 默认返回属性集（不应该走到这里）
        return TraitCardTypeVO.builder()
                .id(cardType.getId().value())
                .orgId(cardType.getOrgId())
                .name(cardType.getName())
                .code(cardType.getCode())
                .systemCardType(cardType.isSystemType())
                .description(cardType.getDescription())
                .enabled(cardType.isEnabled())
                .contentVersion(cardType.getContentVersion())
                .createdAt(cardType.getCreatedAt())
                .updatedAt(cardType.getUpdatedAt())
                .build();
    }

    /**
     * 查询继承指定属性集的实体类型选项列表
     *
     * @param parentTypeId 父类型 ID（属性集）
     * @return 卡片类型选项列表
     */
    public Result<List<CardTypeOptionVO>> listConcreteTypesByParentTypeId(String parentTypeId) {
        List<SchemaDefinition<?>> schemaDefinitions = schemaQuery.queryBySecondKey(CardTypeId.of(parentTypeId), SchemaType.CARD_TYPE);

        List<CardTypeOptionVO> options = schemaDefinitions.stream()
                .filter(CardTypeDefinition.class::isInstance)
                .map(CardTypeDefinition.class::cast)
                .map(cardType -> CardTypeOptionVO.builder()
                        .id(cardType.getId().value())
                        .name(cardType.getName())
                        .schemaSubType(cardType.getSchemaSubType())
                        .build())
                .collect(Collectors.toList());

        return Result.success(options);
    }
}
