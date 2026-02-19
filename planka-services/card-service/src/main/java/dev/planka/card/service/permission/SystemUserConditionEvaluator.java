package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.card.CardId;
import dev.planka.domain.schema.definition.condition.NodeType;
import dev.planka.domain.schema.definition.condition.SystemUserConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统用户条件求值器
 * <p>
 * 支持的字段类型：
 * <ul>
 *     <li>CREATED_BY - 创建人</li>
 *     <li>UPDATED_BY - 更新人</li>
 * </ul>
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>EQUAL - 等于</li>
 *     <li>NOT_EQUAL - 不等于</li>
 *     <li>IN - 在列表中</li>
 *     <li>NOT_IN - 不在列表中</li>
 *     <li>IS_CURRENT_USER - 是当前用户</li>
 *     <li>IS_NOT_CURRENT_USER - 不是当前用户</li>
 *     <li>IS_EMPTY - 为空</li>
 *     <li>IS_NOT_EMPTY - 不为空</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemUserConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估系统用户条件
     */
    public boolean evaluate(SystemUserConditionItem item, CardDTO targetCard, CardDTO memberCard) {
        var nodeType = item.getNodeType();
        var subject = item.getSubject();
        var operator = item.getOperator();

        // 获取目标卡片
        CardDTO card = valueExtractor.getCardByPath(targetCard, subject.path());
        if (card == null) {
            return false;
        }

        // 获取系统用户字段值
        CardId actualUserId = extractSystemUserValue(card, nodeType);

        // 获取当前用户ID（从成员卡片）
        CardId currentUserId = memberCard != null ? memberCard.getId() : null;

        // 评估操作符
        return evaluateOperator(operator, actualUserId, currentUserId);
    }

    /**
     * 提取系统用户字段值
     */
    private CardId extractSystemUserValue(CardDTO card, String nodeType) {
        // TODO: 从卡片中提取创建人、更新人等系统字段
        // 当前简化实现，需要根据实际的 CardDTO 结构来实现
        if (NodeType.CREATED_BY.equals(nodeType)) {
            // 需要从 CardDTO 中提取 createdBy 字段
            log.warn("CREATED_BY 字段提取尚未实现");
            return null;
        }

        if (NodeType.UPDATED_BY.equals(nodeType)) {
            // 需要从 CardDTO 中提取 updatedBy 字段
            log.warn("UPDATED_BY 字段提取尚未实现");
            return null;
        }

        return null;
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(SystemUserConditionItem.UserOperator operator,
                                     CardId actualUserId,
                                     CardId currentUserId) {
        if (operator instanceof SystemUserConditionItem.UserOperator.Equal op) {
            return evaluateEqual(actualUserId, CardId.of(op.getUserId()));
        }

        if (operator instanceof SystemUserConditionItem.UserOperator.NotEqual op) {
            return !evaluateEqual(actualUserId, CardId.of(op.getUserId()));
        }

        if (operator instanceof SystemUserConditionItem.UserOperator.In op) {
            return evaluateIn(actualUserId, op.getUserIds());
        }

        if (operator instanceof SystemUserConditionItem.UserOperator.NotIn op) {
            return !evaluateIn(actualUserId, op.getUserIds());
        }

        if (operator instanceof SystemUserConditionItem.UserOperator.IsCurrentUser) {
            return evaluateEqual(actualUserId, currentUserId);
        }

        if (operator instanceof SystemUserConditionItem.UserOperator.IsNotCurrentUser) {
            return !evaluateEqual(actualUserId, currentUserId);
        }

        if (operator instanceof SystemUserConditionItem.UserOperator.IsEmpty) {
            return actualUserId == null;
        }

        if (operator instanceof SystemUserConditionItem.UserOperator.IsNotEmpty) {
            return actualUserId != null;
        }

        log.warn("未知的用户操作符: {}", operator.getClass().getName());
        return false;
    }

    private boolean evaluateEqual(CardId actualUserId, CardId expectedUserId) {
        if (actualUserId == null && expectedUserId == null) {
            return true;
        }
        if (actualUserId == null || expectedUserId == null) {
            return false;
        }
        return actualUserId.equals(expectedUserId);
    }

    private boolean evaluateIn(CardId actualUserId, List<String> expectedUserIds) {
        if (actualUserId == null || expectedUserIds == null || expectedUserIds.isEmpty()) {
            return false;
        }
        return expectedUserIds.contains(actualUserId.value());
    }
}
