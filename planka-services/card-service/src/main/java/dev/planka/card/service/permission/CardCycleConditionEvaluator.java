package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.card.CardStyle;
import dev.planka.domain.schema.definition.condition.CardCycleConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 卡片生命周期条件求值器
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>IN - 在列表中</li>
 *     <li>NOT_IN - 不在列表中</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardCycleConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估卡片生命周期条件
     */
    public boolean evaluate(CardCycleConditionItem item, CardDTO targetCard, CardDTO memberCard) {
        var subject = item.getSubject();
        var operator = item.getOperator();

        // 获取目标卡片
        CardDTO card = valueExtractor.getCardByPath(targetCard, subject.path());
        if (card == null) {
            return false;
        }

        // 获取卡片生命周期状态
        CardStyle actualCardStyle = card.getCardStyle();

        // 评估操作符
        return evaluateOperator(operator, actualCardStyle);
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(CardCycleConditionItem.LifecycleOperator operator,
                                     CardStyle actualCardStyle) {
        if (operator instanceof CardCycleConditionItem.LifecycleOperator.In op) {
            return evaluateIn(actualCardStyle, op.getValues());
        }

        if (operator instanceof CardCycleConditionItem.LifecycleOperator.NotIn op) {
            return !evaluateIn(actualCardStyle, op.getValues());
        }

        log.warn("未知的生命周期操作符: {}", operator.getClass().getName());
        return false;
    }

    private boolean evaluateIn(CardStyle actualCardStyle, List<CardStyle> expectedValues) {
        if (actualCardStyle == null || expectedValues == null || expectedValues.isEmpty()) {
            return false;
        }
        return expectedValues.contains(actualCardStyle);
    }
}
