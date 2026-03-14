package cn.agilean.kanban.card.service.evaluator;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.domain.field.FieldValue;
import cn.agilean.kanban.domain.field.Url;
import cn.agilean.kanban.domain.field.WebLinkFieldValue;
import cn.agilean.kanban.domain.schema.definition.condition.WebUrlConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 链接属性条件求值器
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>EQ - 等于（匹配 URL 或显示文本）</li>
 *     <li>NE - 不等于（匹配 URL 或显示文本）</li>
 *     <li>CONTAINS - 包含（匹配 URL 或显示文本）</li>
 *     <li>NOT_CONTAINS - 不包含（匹配 URL 或显示文本）</li>
 *     <li>IS_EMPTY - 为空（URL 和显示文本都为空）</li>
 *     <li>IS_NOT_EMPTY - 不为空（URL 或显示文本不为空）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebUrlConditionEvaluator extends AbstractConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估链接属性条件
     */
    public boolean evaluate(WebUrlConditionItem item, CardDTO targetCard, CardDTO memberCard) {
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
    private boolean evaluateSingleCard(WebUrlConditionItem item, CardDTO card) {
        var subject = item.getSubject();
        var operator = item.getOperator();

        // 获取字段值
        Url urlValue = extractWebUrlValue(card.getFieldValue(subject.fieldId()));

        // 评估操作符
        return evaluateOperator(operator, urlValue);
    }

    /**
     * 提取链接值
     */
    private Url extractWebUrlValue(FieldValue<?> fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        if (fieldValue instanceof WebLinkFieldValue webUrlValue) {
            return webUrlValue.getValue();
        }

        return null;
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(WebUrlConditionItem.WebUrlOperator operator, Url urlValue) {
        String url = urlValue != null ? urlValue.url() : null;
        String displayText = urlValue != null ? urlValue.displayText() : null;

        if (operator instanceof WebUrlConditionItem.WebUrlOperator.Equal op) {
            return evaluateEqual(url, displayText, op.getUrl());
        }

        if (operator instanceof WebUrlConditionItem.WebUrlOperator.NotEqual op) {
            return !evaluateEqual(url, displayText, op.getUrl());
        }

        if (operator instanceof WebUrlConditionItem.WebUrlOperator.Contains op) {
            return evaluateContains(url, displayText, op.getValue());
        }

        if (operator instanceof WebUrlConditionItem.WebUrlOperator.NotContains op) {
            return !evaluateContains(url, displayText, op.getValue());
        }

        if (operator instanceof WebUrlConditionItem.WebUrlOperator.IsEmpty) {
            return isEmpty(url) && isEmpty(displayText);
        }

        if (operator instanceof WebUrlConditionItem.WebUrlOperator.IsNotEmpty) {
            return !isEmpty(url) || !isEmpty(displayText);
        }

        log.warn("未知的链接操作符: {}", operator.getClass().getName());
        return false;
    }

    /**
     * 评估等于：URL 或显示文本任一匹配
     */
    private boolean evaluateEqual(String url, String displayText, String expectedValue) {
        if (expectedValue == null) {
            return url == null && displayText == null;
        }
        return expectedValue.equals(url) || expectedValue.equals(displayText);
    }

    /**
     * 评估包含：URL 或显示文本任一包含
     */
    private boolean evaluateContains(String url, String displayText, String expectedValue) {
        if (expectedValue == null) {
            return false;
        }
        return contains(url, expectedValue) || contains(displayText, expectedValue);
    }

    private boolean contains(String value, String expected) {
        return value != null && value.contains(expected);
    }

    private boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
