package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.schema.definition.condition.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 条件求值器
 * <p>
 * 负责评估权限条件是否满足，支持两种评估模式：
 * <ul>
 *     <li>仅操作人条件：evaluate(condition, card)</li>
 *     <li>卡片条件（支持 ReferenceValue）：evaluate(condition, targetCard, memberCard)</li>
 * </ul>
 * <p>
 * 支持的条件类型：
 * <ul>
 *     <li>TextConditionItem - 文本字段条件</li>
 *     <li>NumberConditionItem - 数字字段条件</li>
 *     <li>DateConditionItem - 日期字段条件</li>
 *     <li>EnumConditionItem - 枚举字段条件</li>
 *     <li>StatusConditionItem - 价值流状态条件</li>
 *     <li>CardCycleConditionItem - 生命周期条件</li>
 *     <li>SystemUserConditionItem - 系统用户条件</li>
 *     <li>LinkConditionItem - 关联条件</li>
 *     <li>ConditionGroup - 条件组合（AND/OR）</li>
 * </ul>
 */
@Slf4j
@Component
public class ConditionEvaluator {

    private final TextConditionEvaluator textEvaluator;
    private final NumberConditionEvaluator numberEvaluator;
    private final DateConditionEvaluator dateEvaluator;
    private final EnumConditionEvaluator enumEvaluator;
    private final StatusConditionEvaluator statusEvaluator;
    private final CardCycleConditionEvaluator cardCycleEvaluator;
    private final SystemUserConditionEvaluator systemUserEvaluator;
    private final LinkConditionEvaluator linkEvaluator;

    public ConditionEvaluator(
            TextConditionEvaluator textEvaluator,
            NumberConditionEvaluator numberEvaluator,
            DateConditionEvaluator dateEvaluator,
            EnumConditionEvaluator enumEvaluator,
            StatusConditionEvaluator statusEvaluator,
            CardCycleConditionEvaluator cardCycleEvaluator,
            SystemUserConditionEvaluator systemUserEvaluator,
            LinkConditionEvaluator linkEvaluator) {
        this.textEvaluator = textEvaluator;
        this.numberEvaluator = numberEvaluator;
        this.dateEvaluator = dateEvaluator;
        this.enumEvaluator = enumEvaluator;
        this.statusEvaluator = statusEvaluator;
        this.cardCycleEvaluator = cardCycleEvaluator;
        this.systemUserEvaluator = systemUserEvaluator;
        this.linkEvaluator = linkEvaluator;
    }

    /**
     * 评估条件（仅操作人条件）
     *
     * @param condition 条件
     * @param card      卡片（操作人成员卡片）
     * @return 条件是否满足
     */
    public boolean evaluate(Condition condition, CardDTO card) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        return evaluateNode(condition.getRoot(), card, null);
    }

    /**
     * 评估条件（卡片条件，支持 ReferenceValue）
     *
     * @param condition   条件
     * @param targetCard  目标卡片
     * @param memberCard  操作人成员卡片
     * @return 条件是否满足
     */
    public boolean evaluate(Condition condition, CardDTO targetCard, CardDTO memberCard) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        return evaluateNode(condition.getRoot(), targetCard, memberCard);
    }

    /**
     * 评估条件节点
     *
     * @param node        条件节点
     * @param targetCard  目标卡片
     * @param memberCard  操作人成员卡片（可能为 null）
     * @return 条件是否满足
     */
    private boolean evaluateNode(ConditionNode node, CardDTO targetCard, CardDTO memberCard) {
        if (node == null) {
            return true;
        }

        // 处理条件组合
        if (node instanceof ConditionGroup group) {
            return evaluateGroup(group, targetCard, memberCard);
        }

        // 处理具体条件项
        if (node instanceof TextConditionItem item) {
            return textEvaluator.evaluate(item, targetCard, memberCard);
        }

        if (node instanceof NumberConditionItem item) {
            return numberEvaluator.evaluate(item, targetCard, memberCard);
        }

        if (node instanceof DateConditionItem item) {
            return dateEvaluator.evaluate(item, targetCard, memberCard);
        }

        if (node instanceof EnumConditionItem item) {
            return enumEvaluator.evaluate(item, targetCard, memberCard);
        }

        if (node instanceof StatusConditionItem item) {
            return statusEvaluator.evaluate(item, targetCard, memberCard);
        }

        if (node instanceof CardCycleConditionItem item) {
            return cardCycleEvaluator.evaluate(item, targetCard, memberCard);
        }

        if (node instanceof SystemUserConditionItem item) {
            return systemUserEvaluator.evaluate(item, targetCard, memberCard);
        }

        if (node instanceof LinkConditionItem item) {
            return linkEvaluator.evaluate(item, targetCard, memberCard);
        }

        // 未知类型的条件节点，记录警告并返回 false
        log.warn("未知的条件节点类型: {}", node.getClass().getName());
        return false;
    }

    /**
     * 评估条件组合
     *
     * @param group       条件组合
     * @param targetCard  目标卡片
     * @param memberCard  操作人成员卡片
     * @return 条件是否满足
     */
    private boolean evaluateGroup(ConditionGroup group, CardDTO targetCard, CardDTO memberCard) {
        if (group.isEmpty()) {
            return true;
        }

        var operator = group.getOperator();
        var children = group.getChildren();

        if (children == null || children.isEmpty()) {
            return true;
        }

        // AND 逻辑：所有子条件都满足
        if (operator == ConditionGroup.LogicOperator.AND) {
            for (ConditionNode child : children) {
                if (!evaluateNode(child, targetCard, memberCard)) {
                    return false;
                }
            }
            return true;
        }

        // OR 逻辑：任意一个子条件满足
        if (operator == ConditionGroup.LogicOperator.OR) {
            for (ConditionNode child : children) {
                if (evaluateNode(child, targetCard, memberCard)) {
                    return true;
                }
            }
            return false;
        }

        // 未知的逻辑运算符
        log.warn("未知的逻辑运算符: {}", operator);
        return false;
    }
}
