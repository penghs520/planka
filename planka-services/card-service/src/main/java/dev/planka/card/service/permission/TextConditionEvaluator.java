package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.field.FieldValue;
import dev.planka.domain.field.TextFieldValue;
import dev.planka.domain.schema.definition.condition.TextConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文本条件求值器
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>EQUAL - 等于</li>
 *     <li>NOT_EQUAL - 不等于</li>
 *     <li>CONTAINS - 包含</li>
 *     <li>NOT_CONTAINS - 不包含</li>
 *     <li>STARTS_WITH - 以...开始</li>
 *     <li>ENDS_WITH - 以...结束</li>
 *     <li>IS_EMPTY - 为空</li>
 *     <li>IS_NOT_EMPTY - 不为空</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估文本条件
     */
    public boolean evaluate(TextConditionItem item, CardDTO targetCard, CardDTO memberCard) {
        var subject = item.getSubject();
        var operator = item.getOperator();

        // 获取目标卡片
        CardDTO card = valueExtractor.getCardByPath(targetCard, subject.path());
        if (card == null) {
            return false;
        }

        // 获取字段值
        FieldValue<?> fieldValue = valueExtractor.getFieldValue(card, subject.fieldId());
        String actualValue = extractTextValue(fieldValue);

        // 评估操作符
        return evaluateOperator(operator, actualValue);
    }

    /**
     * 提取文本值
     */
    private String extractTextValue(FieldValue<?> fieldValue) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            return null;
        }

        if (fieldValue instanceof TextFieldValue textValue) {
            return textValue.getValue();
        }

        return null;
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(TextConditionItem.TextOperator operator, String actualValue) {
        if (operator instanceof TextConditionItem.TextOperator.Equal op) {
            return evaluateEqual(actualValue, op.getValue());
        }

        if (operator instanceof TextConditionItem.TextOperator.NotEqual op) {
            return !evaluateEqual(actualValue, op.getValue());
        }

        if (operator instanceof TextConditionItem.TextOperator.Contains op) {
            return evaluateContains(actualValue, op.getValue());
        }

        if (operator instanceof TextConditionItem.TextOperator.NotContains op) {
            return !evaluateContains(actualValue, op.getValue());
        }

        if (operator instanceof TextConditionItem.TextOperator.StartsWith op) {
            return evaluateStartsWith(actualValue, op.getValue());
        }

        if (operator instanceof TextConditionItem.TextOperator.EndsWith op) {
            return evaluateEndsWith(actualValue, op.getValue());
        }

        if (operator instanceof TextConditionItem.TextOperator.IsEmpty) {
            return actualValue == null || actualValue.isEmpty();
        }

        if (operator instanceof TextConditionItem.TextOperator.IsNotEmpty) {
            return actualValue != null && !actualValue.isEmpty();
        }

        log.warn("未知的文本操作符: {}", operator.getClass().getName());
        return false;
    }

    private boolean evaluateEqual(String actualValue, String expectedValue) {
        if (actualValue == null && expectedValue == null) {
            return true;
        }
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue.equals(expectedValue);
    }

    private boolean evaluateContains(String actualValue, String expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue.contains(expectedValue);
    }

    private boolean evaluateStartsWith(String actualValue, String expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue.startsWith(expectedValue);
    }

    private boolean evaluateEndsWith(String actualValue, String expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue.endsWith(expectedValue);
    }
}
