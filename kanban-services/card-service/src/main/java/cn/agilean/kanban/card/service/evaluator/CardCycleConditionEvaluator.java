package cn.agilean.kanban.card.service.evaluator;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.domain.card.CardCycle;
import cn.agilean.kanban.domain.schema.definition.condition.CardCycleConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 卡片生命周期条件求值器
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>IN - 包含任一</li>
 *     <li>NOT_IN - 不包含任一</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardCycleConditionEvaluator extends AbstractConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估卡片生命周期条件
     */
    public boolean evaluate(CardCycleConditionItem item, CardDTO targetCard, CardDTO memberCard) {
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
    private boolean evaluateSingleCard(CardCycleConditionItem item, CardDTO card) {
        var operator = item.getOperator();

        // 获取卡片生命周期状态
        CardCycle actualCardCycle = card.getCardCycle();

        // 评估操作符
        return evaluateOperator(operator, actualCardCycle);
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(CardCycleConditionItem.LifecycleOperator operator,
                                     CardCycle actualCardCycle) {
        if (operator instanceof CardCycleConditionItem.LifecycleOperator.In op) {
            return evaluateIn(actualCardCycle, op.getValues());
        }

        if (operator instanceof CardCycleConditionItem.LifecycleOperator.NotIn op) {
            return !evaluateIn(actualCardCycle, op.getValues());
        }

        log.warn("未知的生命周期操作符: {}", operator.getClass().getName());
        return false;
    }

    private boolean evaluateIn(CardCycle actualCardCycle, List<CardCycle> expectedValues) {
        if (actualCardCycle == null || expectedValues == null || expectedValues.isEmpty()) {
            return false;
        }
        return expectedValues.contains(actualCardCycle);
    }
}
