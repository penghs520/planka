package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.field.FieldValue;
import dev.planka.domain.field.NumberFieldValue;
import dev.planka.domain.schema.definition.condition.NumberConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
public class NumberConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估数字条件
     */
    public boolean evaluate(NumberConditionItem item, CardDTO targetCard, CardDTO memberCard) {
        var subject = item.getSubject();
        var operator = item.getOperator();

        // 获取目标卡片
        CardDTO card = valueExtractor.getCardByPath(targetCard, subject.path());
        if (card == null) {
            return false;
        }

        // 获取字段值
        FieldValue<?> fieldValue = valueExtractor.getFieldValue(card, subject.fieldId());
        Double actualValue = extractNumberValue(fieldValue);

        // 评估操作符
        return evaluateOperator(operator, actualValue, targetCard, memberCard);
    }

    /**
     * 提取数字值
     */
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
            FieldValue<?> fieldValue = valueExtractor.getFieldValue(refCard, refValue.getFieldId());
            return extractNumberValue(fieldValue);
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
