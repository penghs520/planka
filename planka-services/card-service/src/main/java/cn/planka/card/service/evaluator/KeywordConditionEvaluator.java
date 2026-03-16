package cn.planka.card.service.evaluator;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.domain.card.CardTitle;
import cn.planka.domain.schema.definition.condition.KeywordConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 关键字条件求值器
 * <p>
 * 在卡片的可搜索字段中查找关键字，包括：
 * <ul>
 *     <li>卡片标题</li>
 *     <li>卡片编号</li>
 * </ul>
 * 支持的操作符：
 * <ul>
 *     <li>CONTAINS - 包含关键字</li>
 *     <li>NOT_CONTAINS - 不包含关键字</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估关键字条件
     */
    public boolean evaluate(KeywordConditionItem item, CardDTO targetCard, CardDTO memberCard) {
        var operator = item.getOperator();

        // 评估操作符
        return evaluateOperator(operator, targetCard);
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(KeywordConditionItem.KeywordOperator operator, CardDTO card) {
        if (operator instanceof KeywordConditionItem.KeywordOperator.Contains op) {
            return evaluateContains(card, op.getKeyword());
        }

        if (operator instanceof KeywordConditionItem.KeywordOperator.NotContains op) {
            return !evaluateContains(card, op.getKeyword());
        }

        log.warn("未知的关键字操作符: {}", operator.getClass().getName());
        return false;
    }

    /**
     * 评估是否包含关键字
     * <p>
     * 在以下可搜索字段中查找：
     * 1. 卡片标题
     * 2. 卡片编号（内置编号和自定义编号）
     */
    private boolean evaluateContains(CardDTO card, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return false;
        }

        // 检查编号
        if (containsInCode(card, keyword)) {
            return true;
        }

        // 检查标题
        return containsInTitle(card, keyword);
    }

    /**
     * 在标题中查找关键字
     */
    private boolean containsInTitle(CardDTO card, String keyword) {
        CardTitle title = card.getTitle();
        if (title == null) {
            return false;
        }
        String titleValue = title.getValue();
        return titleValue != null && titleValue.toLowerCase().contains(keyword.toLowerCase());
    }

    /**
     * 在编号中查找关键字
     */
    private boolean containsInCode(CardDTO card, String keyword) {
        String code = card.getCode();
        if (code == null) {
            return false;
        }
        return code.toLowerCase().contains(keyword.toLowerCase());
    }

}
