package cn.planka.card.service.evaluator;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.domain.schema.definition.condition.CodeConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 卡片编号条件求值器
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>EQUAL - 等于</li>
 *     <li>NOT_EQUAL - 不等于</li>
 *     <li>CONTAINS - 包含</li>
 *     <li>NOT_CONTAINS - 不包含</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeConditionEvaluator extends AbstractConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估卡片编号条件
     */
    public boolean evaluate(CodeConditionItem item, CardDTO targetCard, CardDTO memberCard) {
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
    private boolean evaluateSingleCard(CodeConditionItem item, CardDTO card) {
        var operator = item.getOperator();

        // 获取编号值
        String actualValue = card.getCode();

        // 评估操作符
        return evaluateOperator(operator, actualValue);
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(CodeConditionItem.CodeOperator operator, String actualValue) {
        if (operator instanceof CodeConditionItem.CodeOperator.Equal op) {
            return evaluateEqual(actualValue, op.getValue());
        }

        if (operator instanceof CodeConditionItem.CodeOperator.NotEqual op) {
            return !evaluateEqual(actualValue, op.getValue());
        }

        if (operator instanceof CodeConditionItem.CodeOperator.Contains op) {
            return evaluateContains(actualValue, op.getValue());
        }

        if (operator instanceof CodeConditionItem.CodeOperator.NotContains op) {
            return !evaluateContains(actualValue, op.getValue());
        }

        log.warn("未知的编号操作符: {}", operator.getClass().getName());
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
}
