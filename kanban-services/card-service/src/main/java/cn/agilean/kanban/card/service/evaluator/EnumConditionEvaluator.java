package cn.agilean.kanban.card.service.evaluator;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.domain.field.EnumFieldValue;
import cn.agilean.kanban.domain.field.FieldValue;
import cn.agilean.kanban.domain.schema.definition.condition.EnumConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 枚举条件求值器
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>EQUAL - 等于（单选）</li>
 *     <li>NOT_EQUAL - 不等于</li>
 *     <li>IN - 包含任一</li>
 *     <li>NOT_IN - 不包含任一</li>
 *     <li>IS_EMPTY - 为空</li>
 *     <li>IS_NOT_EMPTY - 不为空</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnumConditionEvaluator extends AbstractConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估枚举条件
     */
    public boolean evaluate(EnumConditionItem item, CardDTO targetCard, CardDTO memberCard) {
        if (targetCard == null) {
            return false;
        }

        var subject = item.getSubject();
        var operator = item.getOperator();

        // 如果 path 不存在，直接评估当前卡片
        if (subject.path() == null || subject.path().linkNodes().isEmpty()) {
            return evaluateSingleCard(item, targetCard);
        }

        // 获取目标卡片集合并使用短路求值评估
        Set<CardDTO> cards = targetCard.getLinkedCards(subject.path());
        return evaluateCards(cards, card -> evaluateSingleCard(item, card), operator);
    }

    /**
     * 评估单个卡片
     */
    private boolean evaluateSingleCard(EnumConditionItem item, CardDTO card) {
        var subject = item.getSubject();
        var operator = item.getOperator();

        // 获取字段值
        List<String> actualValues = extractEnumValues(card.getFieldValue(subject.fieldId()));

        // 评估操作符
        return evaluateOperator(operator, actualValues);
    }

    /**
     * 提取枚举值集合
     */
    private List<String> extractEnumValues(FieldValue<?> fieldValue) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            return null;
        }

        if (fieldValue instanceof EnumFieldValue enumValue) {
            return enumValue.getValue();
        }

        return null;
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(EnumConditionItem.EnumOperator operator, List<String> actualValues) {
        if (operator instanceof EnumConditionItem.EnumOperator.Equal op) {
            return evaluateEqual(actualValues, op.getOptionId());
        }

        if (operator instanceof EnumConditionItem.EnumOperator.NotEqual op) {
            return !evaluateEqual(actualValues, op.getOptionId());
        }

        if (operator instanceof EnumConditionItem.EnumOperator.In op) {
            return evaluateIn(actualValues, op.getOptionIds());
        }

        if (operator instanceof EnumConditionItem.EnumOperator.NotIn op) {
            return !evaluateIn(actualValues, op.getOptionIds());
        }

        if (operator instanceof EnumConditionItem.EnumOperator.IsEmpty) {
            return actualValues == null || actualValues.isEmpty();
        }

        if (operator instanceof EnumConditionItem.EnumOperator.IsNotEmpty) {
            return actualValues != null && !actualValues.isEmpty();
        }

        log.warn("未知的枚举操作符: {}", operator.getClass().getName());
        return false;
    }

    private boolean evaluateEqual(List<String> actualValues, String expectedOptionId) {
        if (actualValues == null || actualValues.isEmpty()) {
            return false;
        }
        // 单选：只有一个值且等于期望值
        return actualValues.size() == 1 && actualValues.contains(expectedOptionId);
    }

    private boolean evaluateIn(List<String> actualValues, List<String> expectedOptionIds) {
        if (actualValues == null || actualValues.isEmpty()) {
            return false;
        }
        if (expectedOptionIds == null || expectedOptionIds.isEmpty()) {
            return false;
        }
        // 任意一个实际值在期望列表中
        for (String actualValue : actualValues) {
            if (expectedOptionIds.contains(actualValue)) {
                return true;
            }
        }
        return false;
    }
}
