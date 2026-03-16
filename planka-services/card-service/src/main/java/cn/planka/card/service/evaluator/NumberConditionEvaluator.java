package cn.planka.card.service.evaluator;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.domain.field.FieldValue;
import cn.planka.domain.field.NumberFieldValue;
import cn.planka.domain.schema.definition.condition.NumberConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 数字条件求值器
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>EQUAL - 等于</li>
 *     <li>NOT_EQUAL - 不等于</li>
 *     <li>GREATER_THAN - 大于</li>
 *     <li>GREATER_THAN_OR_EQUAL - 大于等于</li>
 *     <li>LESS_THAN - 小于</li>
 *     <li>LESS_THAN_OR_EQUAL - 小于等于</li>
 *     <li>BETWEEN - 在范围内</li>
 *     <li>IS_EMPTY - 为空</li>
 *     <li>IS_NOT_EMPTY - 不为空</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NumberConditionEvaluator extends AbstractConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估数字条件
     */
    public boolean evaluate(NumberConditionItem item, CardDTO targetCard, CardDTO memberCard) {
        var subject = item.getSubject();
        var operator = item.getOperator();

        // 如果 path 不存在，直接评估当前卡片
        if (subject.path() == null || subject.path().linkNodes().isEmpty()) {
            return evaluateSingleCard(targetCard, subject.fieldId(), operator, targetCard, memberCard);
        }

        // 获取目标卡片集合并评估（使用短路求值）
        Set<CardDTO> cards = targetCard.getLinkedCards(subject.path());
        return evaluateCards(cards, card -> evaluateSingleCard(card, subject.fieldId(), operator, targetCard, memberCard), operator);
    }

    /**
     * 评估单个卡片
     */
    private boolean evaluateSingleCard(CardDTO card, String fieldId, NumberConditionItem.NumberOperator operator,
                                       CardDTO targetCard, CardDTO memberCard) {
        Double actualValue = extractNumberValue(card.getFieldValue(fieldId));
        return evaluateOperator(operator, actualValue, targetCard, memberCard);
    }

    private Double extractNumberValue(FieldValue<?> fieldValue) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            return null;
        }

        if (fieldValue instanceof NumberFieldValue numberValue) {
            return numberValue.getValue();
        }

        return null;
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(NumberConditionItem.NumberOperator operator,
                                     Double actualValue,
                                     CardDTO targetCard,
                                     CardDTO memberCard) {
        if (operator instanceof NumberConditionItem.NumberOperator.Equal op) {
            Double expectedValue = resolveNumberValue(op.getValue(), targetCard, memberCard);
            return evaluateEqual(actualValue, expectedValue);
        }

        if (operator instanceof NumberConditionItem.NumberOperator.NotEqual op) {
            Double expectedValue = resolveNumberValue(op.getValue(), targetCard, memberCard);
            return !evaluateEqual(actualValue, expectedValue);
        }

        if (operator instanceof NumberConditionItem.NumberOperator.GreaterThan op) {
            Double expectedValue = resolveNumberValue(op.getValue(), targetCard, memberCard);
            return evaluateGreaterThan(actualValue, expectedValue);
        }

        if (operator instanceof NumberConditionItem.NumberOperator.GreaterThanOrEqual op) {
            Double expectedValue = resolveNumberValue(op.getValue(), targetCard, memberCard);
            return evaluateGreaterThanOrEqual(actualValue, expectedValue);
        }

        if (operator instanceof NumberConditionItem.NumberOperator.LessThan op) {
            Double expectedValue = resolveNumberValue(op.getValue(), targetCard, memberCard);
            return evaluateLessThan(actualValue, expectedValue);
        }

        if (operator instanceof NumberConditionItem.NumberOperator.LessThanOrEqual op) {
            Double expectedValue = resolveNumberValue(op.getValue(), targetCard, memberCard);
            return evaluateLessThanOrEqual(actualValue, expectedValue);
        }

        if (operator instanceof NumberConditionItem.NumberOperator.Between op) {
            Double startValue = resolveNumberValue(op.getStart(), targetCard, memberCard);
            Double endValue = resolveNumberValue(op.getEnd(), targetCard, memberCard);
            return evaluateBetween(actualValue, startValue, endValue);
        }

        if (operator instanceof NumberConditionItem.NumberOperator.IsEmpty) {
            return actualValue == null;
        }

        if (operator instanceof NumberConditionItem.NumberOperator.IsNotEmpty) {
            return actualValue != null;
        }

        log.warn("未知的数字操作符: {}", operator.getClass().getName());
        return false;
    }

    /**
     * 解析数字值（支持静态值和引用值）
     */
    private Double resolveNumberValue(NumberConditionItem.NumberValue value,
                                     CardDTO targetCard,
                                     CardDTO memberCard) {
        if (value == null) {
            return null;
        }

        if (value instanceof NumberConditionItem.NumberValue.StaticValue staticValue) {
            return staticValue.getValue();
        }

        if (value instanceof NumberConditionItem.NumberValue.ReferenceValue refValue) {
            CardDTO refCard = valueExtractor.getCardByReferenceSource(
                refValue.getSource(), targetCard, memberCard);
            if (refCard == null) {
                return null;
            }
            return extractNumberValue(refCard.getFieldValue(refValue.getFieldId()));
        }

        return null;
    }

    private boolean evaluateEqual(Double actualValue, Double expectedValue) {
        if (actualValue == null && expectedValue == null) {
            return true;
        }
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue.equals(expectedValue);
    }

    private boolean evaluateGreaterThan(Double actualValue, Double expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue > expectedValue;
    }

    private boolean evaluateGreaterThanOrEqual(Double actualValue, Double expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue >= expectedValue;
    }

    private boolean evaluateLessThan(Double actualValue, Double expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue < expectedValue;
    }

    private boolean evaluateLessThanOrEqual(Double actualValue, Double expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue <= expectedValue;
    }

    private boolean evaluateBetween(Double actualValue, Double startValue, Double endValue) {
        if (actualValue == null || startValue == null || endValue == null) {
            return false;
        }
        return actualValue >= startValue && actualValue <= endValue;
    }
}
