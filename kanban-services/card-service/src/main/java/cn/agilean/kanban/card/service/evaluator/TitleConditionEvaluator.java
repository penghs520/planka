package cn.agilean.kanban.card.service.evaluator;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.domain.card.CardTitle;
import cn.agilean.kanban.domain.schema.definition.condition.TitleConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 标题条件求值器
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>CONTAINS - 包含</li>
 *     <li>NOT_CONTAINS - 不包含</li>
 *     <li>IS_EMPTY - 为空</li>
 *     <li>IS_NOT_EMPTY - 不为空</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TitleConditionEvaluator extends AbstractConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估标题条件
     */
    public boolean evaluate(TitleConditionItem item, CardDTO targetCard, CardDTO memberCard) {
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
    private boolean evaluateSingleCard(TitleConditionItem item, CardDTO card) {
        var operator = item.getOperator();

        // 获取标题值
        String actualValue = extractTitleValue(card);

        // 评估操作符
        return evaluateOperator(operator, actualValue);
    }

    /**
     * 提取标题值
     */
    private String extractTitleValue(CardDTO card) {
        CardTitle title = card.getTitle();
        if (title == null) {
            return null;
        }
        return title.getValue();
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(TitleConditionItem.TitleOperator operator, String actualValue) {
        if (operator instanceof TitleConditionItem.TitleOperator.Contains op) {
            return evaluateContains(actualValue, op.getValue());
        }

        if (operator instanceof TitleConditionItem.TitleOperator.NotContains op) {
            return !evaluateContains(actualValue, op.getValue());
        }

        if (operator instanceof TitleConditionItem.TitleOperator.IsEmpty) {
            return actualValue == null || actualValue.isEmpty();
        }

        if (operator instanceof TitleConditionItem.TitleOperator.IsNotEmpty) {
            return actualValue != null && !actualValue.isEmpty();
        }

        log.warn("未知的标题操作符: {}", operator.getClass().getName());
        return false;
    }

    private boolean evaluateContains(String actualValue, String expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue.contains(expectedValue);
    }
}
