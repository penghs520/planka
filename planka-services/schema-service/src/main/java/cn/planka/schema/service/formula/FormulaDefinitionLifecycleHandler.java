package cn.planka.schema.service.formula;

import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.schema.SchemaSubType;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.cardtype.AbstractCardType;
import cn.planka.domain.schema.definition.cardtype.EntityCardType;
import cn.planka.domain.schema.definition.formula.AbstractFormulaDefinition;
import cn.planka.schema.repository.SchemaRepository;
import cn.planka.schema.service.common.lifecycle.SchemaLifecycleHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 计算公式定义生命周期处理器基类
 * <p>
 * 提供通用的校验逻辑，供各个具体公式定义类型的处理器使用。
 */
@Slf4j
@RequiredArgsConstructor
abstract class AbstractFormulaDefinitionLifecycleHandler implements SchemaLifecycleHandler<AbstractFormulaDefinition> {

    protected final SchemaRepository schemaRepository;

    @Override
    public void beforeCreate(AbstractFormulaDefinition definition) {
        validateCardTypeAssociation(definition);
        validateSpaceLevel(definition);
    }

    @Override
    public void beforeUpdate(AbstractFormulaDefinition oldDefinition, AbstractFormulaDefinition newDefinition) {
        validateCardTypeAssociation(newDefinition);
        validateSpaceLevel(newDefinition);
    }

    /**
     * 校验实体类型关联规则
     * <p>
     * 规则：
     * <ul>
     *   <li>cardTypeIds 如果存在，必须全部为特征类型或全部为实体类型</li>
     *   <li>如果全部为实体类型，只能有一个</li>
     *   <li>如果全部为特征类型，可以有多个</li>
     * </ul>
     */
    protected void validateCardTypeAssociation(AbstractFormulaDefinition definition) {
        List<CardTypeId> cardTypeIds = definition.getCardTypeIds();
        if (cardTypeIds == null || cardTypeIds.isEmpty()) {
            // 允许为空（不关联任何实体类型）
            return;
        }

        // 批量查询实体类型定义
        Set<String> cardTypeIdStrings = cardTypeIds.stream()
                .map(CardTypeId::value)
                .collect(Collectors.toSet());
        List<SchemaDefinition<?>> cardTypes = schemaRepository.findByIds(cardTypeIdStrings);

        // 检查是否所有实体类型都存在
        if (cardTypes.size() != cardTypeIds.size()) {
            Set<String> foundIds = cardTypes.stream()
                    .map(s -> s.getId().value())
                    .collect(Collectors.toSet());
            Set<String> missingIds = cardTypeIdStrings.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new IllegalArgumentException("关联的实体类型不存在: " + missingIds);
        }

        // 检查实体类型是抽象还是具体
        long abstractCount = cardTypes.stream()
                .filter(c -> c instanceof AbstractCardType)
                .count();
        long concreteCount = cardTypes.stream()
                .filter(c -> c instanceof EntityCardType)
                .count();

        // 必须全部为特征类型或全部为实体类型
        if (abstractCount > 0 && concreteCount > 0) {
            throw new IllegalArgumentException("cardTypeIds 不能同时包含特征类型和实体类型");
        }

        // 如果全部为实体类型，只能有一个
        if (concreteCount > 0 && cardTypeIds.size() > 1) {
            throw new IllegalArgumentException("当关联实体类型时，只能关联一个实体类型");
        }

        // 如果全部为特征类型，可以有多个（无需额外校验）
    }

    /**
     * 校验空间级别配置
     * <p>
     * 公式定义仅支持组织级配置。
     * 由于 AbstractSchemaDefinition 中没有 spaceId 字段，所有公式定义都是组织级配置，无需额外验证。
     */
    protected void validateSpaceLevel(AbstractFormulaDefinition definition) {
        // 公式定义仅支持组织级配置，由于没有 spaceId 字段，无需验证
    }
}

/**
 * 时间点公式定义生命周期处理器
 */
@Component
class TimePointFormulaDefinitionLifecycleHandler extends AbstractFormulaDefinitionLifecycleHandler {
    public TimePointFormulaDefinitionLifecycleHandler(SchemaRepository schemaRepository) {
        super(schemaRepository);
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.TIME_POINT_FORMULA_DEFINITION;
    }
}

/**
 * 时间段公式定义生命周期处理器
 */
@Component
class TimeRangeFormulaDefinitionLifecycleHandler extends AbstractFormulaDefinitionLifecycleHandler {
    public TimeRangeFormulaDefinitionLifecycleHandler(SchemaRepository schemaRepository) {
        super(schemaRepository);
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.TIME_RANGE_FORMULA_DEFINITION;
    }
}

/**
 * 日期汇集公式定义生命周期处理器
 */
@Component
class DateCollectionFormulaDefinitionLifecycleHandler extends AbstractFormulaDefinitionLifecycleHandler {
    public DateCollectionFormulaDefinitionLifecycleHandler(SchemaRepository schemaRepository) {
        super(schemaRepository);
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION;
    }
}

/**
 * 卡片汇集公式定义生命周期处理器
 */
@Component
class CardCollectionFormulaDefinitionLifecycleHandler extends AbstractFormulaDefinitionLifecycleHandler {
    public CardCollectionFormulaDefinitionLifecycleHandler(SchemaRepository schemaRepository) {
        super(schemaRepository);
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.CARD_COLLECTION_FORMULA_DEFINITION;
    }
}

/**
 * 数值运算公式定义生命周期处理器
 */
@Component
class NumberCalculationFormulaDefinitionLifecycleHandler extends AbstractFormulaDefinitionLifecycleHandler {
    public NumberCalculationFormulaDefinitionLifecycleHandler(SchemaRepository schemaRepository) {
        super(schemaRepository);
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION;
    }
}
