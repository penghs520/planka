package cn.planka.card.service.evaluator;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.domain.field.FieldValue;
import cn.planka.domain.field.TextFieldValue;
import cn.planka.domain.schema.definition.condition.TextConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

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
public class TextConditionEvaluator extends AbstractConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估文本条件
     */
    public boolean evaluate(TextConditionItem item, CardDTO targetCard, CardDTO memberCard) {
        var subject = item.getSubject();
        var operator = item.getOperator();

        // 如果 path 不存在，直接评估当前卡片
        if (subject.path() == null || subject.path().linkNodes().isEmpty()) {
            return evaluateSingleCard(targetCard, subject.fieldId(), operator);
        }

        // 获取目标卡片集合并评估（使用短路求值）
        Set<CardDTO> cards = targetCard.getLinkedCards(subject.path());
        return evaluateCards(cards, card -> evaluateSingleCard(card, subject.fieldId(), operator), operator);
    }

    /**
     * 评估单个卡片
     */
    private boolean evaluateSingleCard(CardDTO card, String fieldId, TextConditionItem.TextOperator operator) {
        String actualValue = extractTextValue(card.getFieldValue(fieldId));
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
