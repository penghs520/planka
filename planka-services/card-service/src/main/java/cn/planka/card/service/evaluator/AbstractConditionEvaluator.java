package cn.planka.card.service.evaluator;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.domain.schema.definition.condition.NegativeOperator;

import java.util.Set;
import java.util.function.Function;

/**
 * 抽象条件求值器
 * <p>
 * 提供通用的多级字段条件评估逻辑：
 * <ul>
 *     <li>正向条件（默认）：任一目标卡片满足条件即返回 true</li>
 *     <li>非正向条件（实现 {@link NegativeOperator}）：所有目标卡片都必须满足条件才返回 true</li>
 * </ul>
 * <p>
 * 使用短路求值优化性能，避免不必要的计算。
 */
public abstract class AbstractConditionEvaluator {

    /**
     * 评估多个卡片的结果（使用短路求值优化性能）
     *
     * @param cards     目标卡片集合
     * @param evaluator 单个卡片的评估函数
     * @param operator  操作符（实现 NegativeOperator 接口表示非正向条件）
     * @return 最终评估结果
     */
    protected boolean evaluateCards(Set<CardDTO> cards, Function<CardDTO, Boolean> evaluator, Object operator) {
        if (cards == null || cards.isEmpty()) {
            // 空集合：非正向条件返回 true（空真），正向条件返回 false
            return operator instanceof NegativeOperator;
        }

        if (operator instanceof NegativeOperator) {
            // 非正向条件：必须全部满足，任一不满足则立即返回 false
            for (CardDTO card : cards) {
                if (!evaluator.apply(card)) {
                    return false;
                }
            }
            return true;
        } else {
            // 正向条件（默认）：任一满足即可，满足则立即返回 true
            for (CardDTO card : cards) {
                if (evaluator.apply(card)) {
                    return true;
                }
            }
            return false;
        }
    }
}
