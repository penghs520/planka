package dev.planka.schema.service.formula;

import dev.planka.api.schema.vo.cardtype.CardTypeInfo;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaQuery;
import dev.planka.api.schema.vo.formuladefinition.*;
import dev.planka.domain.schema.definition.formula.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 计算公式定义服务
 * <p>
 * 提供公式定义的列表查询，返回 VO 对象供前端展示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormulaDefinitionService {

    private final SchemaRepository schemaRepository;
    private final SchemaQuery schemaQuery;

    /**
     * 查询公式定义列表
     *
     * @param orgId   组织 ID
     * @param spaceId 空间 ID（可选，传入则返回组织级+空间级合并结果）
     *                <p>
     *                注意：公式定义仅支持组织级配置，spaceId 参数会被忽略
     * @return 公式定义列表
     */
    public Result<List<FormulaDefinitionVO>> listFormulaDefinitions(String orgId, String spaceId) {
        // 公式定义仅支持组织级配置，忽略 spaceId 参数
        List<SchemaDefinition<?>> schemas = schemaQuery.query(orgId, SchemaType.FORMULA_DEFINITION);

        // 收集所有卡片类型 ID
        Set<String> allCardTypeIds = schemas.stream()
                .filter(s -> s instanceof AbstractFormulaDefinition)
                .map(s -> (AbstractFormulaDefinition) s)
                .filter(f -> f.getCardTypeIds() != null)
                .flatMap(f -> f.getCardTypeIds().stream())
                .map(CardTypeId::value)
                .collect(Collectors.toSet());

        // 批量查询卡片类型名称
        Map<String, String> cardTypeNameMap = getCardTypeNameMap(orgId, allCardTypeIds);

        List<FormulaDefinitionVO> voList = schemas.stream()
                .filter(s -> s instanceof AbstractFormulaDefinition)
                .map(s -> toVO((AbstractFormulaDefinition) s, cardTypeNameMap))
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 批量获取卡片类型名称映射
     */
    private Map<String, String> getCardTypeNameMap(String orgId, Set<String> cardTypeIds) {
        if (cardTypeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SchemaDefinition<?>> cardTypes = schemaRepository.findByIds(cardTypeIds);
        return cardTypes.stream()
                .collect(Collectors.toMap(
                        s -> s.getId().value(),
                        SchemaDefinition::getName,
                        (a, b) -> a
                ));
    }

    /**
     * 将公式定义转换为 VO
     */
    private FormulaDefinitionVO toVO(AbstractFormulaDefinition formula, Map<String, String> cardTypeNameMap) {
        // 构建卡片类型信息列表
        List<CardTypeInfo> cardTypes = null;
        if (formula.getCardTypeIds() != null && !formula.getCardTypeIds().isEmpty()) {
            cardTypes = formula.getCardTypeIds().stream()
                    .map(id -> CardTypeInfo.builder()
                            .id(id.value())
                            .name(cardTypeNameMap.getOrDefault(id.value(), id.value()))
                            .build())
                    .collect(Collectors.toList());
        }

        // 根据类型创建对应的 VO
        if (formula instanceof TimePointFormulaDefinition timePoint) {
            return TimePointFormulaDefinitionVO.builder()
                    .id(formula.getId().value())
                    .name(formula.getName())
                    .code(formula.getCode())
                    .enabled(formula.isEnabled())
                    .description(formula.getDescription())
                    .cardTypes(cardTypes)
                    .contentVersion(formula.getContentVersion())
                    .createdAt(formula.getCreatedAt())
                    .updatedAt(formula.getUpdatedAt())
                    .sourceType(timePoint.getSourceType() != null ? timePoint.getSourceType().name() : null)
                    .sourceFieldId(timePoint.getSourceFieldId() != null ? timePoint.getSourceFieldId().value() : null)
                    .streamId(timePoint.getStreamId() != null ? timePoint.getStreamId().value() : null)
                    .statusId(timePoint.getStatusId() != null ? timePoint.getStatusId().value() : null)
                    .build();
        } else if (formula instanceof TimeRangeFormulaDefinition timeRange) {
            return TimeRangeFormulaDefinitionVO.builder()
                    .id(formula.getId().value())
                    .name(formula.getName())
                    .code(formula.getCode())
                    .enabled(formula.isEnabled())
                    .description(formula.getDescription())
                    .cardTypes(cardTypes)
                    .contentVersion(formula.getContentVersion())
                    .createdAt(formula.getCreatedAt())
                    .updatedAt(formula.getUpdatedAt())
                    .startSourceType(timeRange.getStartSourceType() != null ? timeRange.getStartSourceType().name() : null)
                    .startFieldId(timeRange.getStartFieldId() != null ? timeRange.getStartFieldId().value() : null)
                    .startStreamId(timeRange.getStartStreamId() != null ? timeRange.getStartStreamId().value() : null)
                    .startStatusId(timeRange.getStartStatusId() != null ? timeRange.getStartStatusId().value() : null)
                    .endSourceType(timeRange.getEndSourceType() != null ? timeRange.getEndSourceType().name() : null)
                    .endFieldId(timeRange.getEndFieldId() != null ? timeRange.getEndFieldId().value() : null)
                    .endStreamId(timeRange.getEndStreamId() != null ? timeRange.getEndStreamId().value() : null)
                    .endStatusId(timeRange.getEndStatusId() != null ? timeRange.getEndStatusId().value() : null)
                    .precision(timeRange.getPrecision() != null ? timeRange.getPrecision().name() : null)
                    .build();
        } else if (formula instanceof DateCollectionFormulaDefinition dateCollection) {
            List<String> targetCardTypeIds = null;
            if (dateCollection.getTargetCardTypeIds() != null) {
                targetCardTypeIds = dateCollection.getTargetCardTypeIds().stream()
                        .map(CardTypeId::value)
                        .collect(Collectors.toList());
            }
            return DateCollectionFormulaDefinitionVO.builder()
                    .id(formula.getId().value())
                    .name(formula.getName())
                    .code(formula.getCode())
                    .enabled(formula.isEnabled())
                    .description(formula.getDescription())
                    .cardTypes(cardTypes)
                    .contentVersion(formula.getContentVersion())
                    .createdAt(formula.getCreatedAt())
                    .updatedAt(formula.getUpdatedAt())
                    .linkFieldId(dateCollection.getLinkFieldId() != null ? dateCollection.getLinkFieldId().value() : null)
                    .targetCardTypeIds(targetCardTypeIds)
                    .sourceFieldId(dateCollection.getSourceFieldId() != null ? dateCollection.getSourceFieldId().value() : null)
                    .aggregationType(dateCollection.getAggregationType() != null ? dateCollection.getAggregationType().name() : null)
                    .filterCondition(dateCollection.getFilterCondition())
                    .build();
        } else if (formula instanceof CardCollectionFormulaDefinition cardCollection) {
            List<String> targetCardTypeIds = null;
            if (cardCollection.getTargetCardTypeIds() != null) {
                targetCardTypeIds = cardCollection.getTargetCardTypeIds().stream()
                        .map(CardTypeId::value)
                        .collect(Collectors.toList());
            }
            return CardCollectionFormulaDefinitionVO.builder()
                    .id(formula.getId().value())
                    .name(formula.getName())
                    .code(formula.getCode())
                    .enabled(formula.isEnabled())
                    .description(formula.getDescription())
                    .cardTypes(cardTypes)
                    .contentVersion(formula.getContentVersion())
                    .createdAt(formula.getCreatedAt())
                    .updatedAt(formula.getUpdatedAt())
                    .linkFieldId(cardCollection.getLinkFieldId() != null ? cardCollection.getLinkFieldId().value() : null)
                    .targetCardTypeIds(targetCardTypeIds)
                    .sourceFieldId(cardCollection.getSourceFieldId() != null ? cardCollection.getSourceFieldId().value() : null)
                    .aggregationType(cardCollection.getAggregationType() != null ? cardCollection.getAggregationType().name() : null)
                    .filterCondition(cardCollection.getFilterCondition())
                    .build();
        } else if (formula instanceof NumberCalculationFormulaDefinition numberCalculation) {
            return NumberCalculationFormulaDefinitionVO.builder()
                    .id(formula.getId().value())
                    .name(formula.getName())
                    .code(formula.getCode())
                    .enabled(formula.isEnabled())
                    .description(formula.getDescription())
                    .cardTypes(cardTypes)
                    .contentVersion(formula.getContentVersion())
                    .createdAt(formula.getCreatedAt())
                    .updatedAt(formula.getUpdatedAt())
                    .expression(numberCalculation.getExpression())
                    .expressionStructure(numberCalculation.getExpressionStructure())
                    .precision(numberCalculation.getPrecision())
                    .build();
        }

        // 不应该走到这里
        throw new IllegalStateException("未支持的公式定义类型: " + formula.getClass().getName());
    }
}
