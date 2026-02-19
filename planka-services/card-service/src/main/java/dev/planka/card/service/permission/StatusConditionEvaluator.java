package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.schema.definition.condition.StatusConditionItem;
import dev.planka.domain.stream.StatusId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 价值流状态条件求值器
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>EQUAL - 等于</li>
 *     <li>NOT_EQUAL - 不等于</li>
 *     <li>IN - 在列表中</li>
 *     <li>NOT_IN - 不在列表中</li>
 *     <li>REACHED - 已到达</li>
 *     <li>NOT_REACHED - 未到达</li>
 *     <li>PASSED - 已超过</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatusConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估状态条件
     */
    public boolean evaluate(StatusConditionItem item, CardDTO targetCard, CardDTO memberCard) {
        var subject = item.getSubject();
        var operator = item.getOperator();

        // 获取目标卡片
        CardDTO card = valueExtractor.getCardByPath(targetCard, subject.path());
        if (card == null) {
            return false;
        }

        // 获取卡片当前状态
        StatusId actualStatusId = card.getStatusId();

        // 评估操作符
        return evaluateOperator(operator, actualStatusId, subject.streamId());
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(StatusConditionItem.StatusOperator operator,
                                     StatusId actualStatusId,
                                     String streamId) {
        if (operator instanceof StatusConditionItem.StatusOperator.Equal op) {
            return evaluateEqual(actualStatusId, op.getStatusId());
        }

        if (operator instanceof StatusConditionItem.StatusOperator.NotEqual op) {
            return !evaluateEqual(actualStatusId, op.getStatusId());
        }

        if (operator instanceof StatusConditionItem.StatusOperator.In op) {
            return evaluateIn(actualStatusId, op.getStatusIds());
        }

        if (operator instanceof StatusConditionItem.StatusOperator.NotIn op) {
            return !evaluateIn(actualStatusId, op.getStatusIds());
        }

        if (operator instanceof StatusConditionItem.StatusOperator.Reached op) {
            return evaluateReached(actualStatusId, op.getStatusId(), streamId);
        }

        if (operator instanceof StatusConditionItem.StatusOperator.NotReached op) {
            return !evaluateReached(actualStatusId, op.getStatusId(), streamId);
        }

        if (operator instanceof StatusConditionItem.StatusOperator.Passed op) {
            return evaluatePassed(actualStatusId, op.getStatusId(), streamId);
        }

        log.warn("未知的状态操作符: {}", operator.getClass().getName());
        return false;
    }

    private boolean evaluateEqual(StatusId actualStatusId, String expectedStatusId) {
        if (actualStatusId == null) {
            return false;
        }
        return actualStatusId.value().equals(expectedStatusId);
    }

    private boolean evaluateIn(StatusId actualStatusId, List<String> expectedStatusIds) {
        if (actualStatusId == null || expectedStatusIds == null || expectedStatusIds.isEmpty()) {
            return false;
        }
        return expectedStatusIds.contains(actualStatusId.value());
    }

    /**
     * 评估"已到达"条件
     * <p>
     * 注意：此方法需要查询价值流定义来判断状态顺序，当前简化实现仅判断相等
     * TODO: 实现完整的价值流状态顺序判断逻辑
     */
    private boolean evaluateReached(StatusId actualStatusId, String targetStatusId, String streamId) {
        if (actualStatusId == null) {
            return false;
        }
        // 简化实现：仅判断是否等于目标状态
        // 完整实现需要查询价值流定义，判断当前状态是否在目标状态之后或等于目标状态
        log.warn("REACHED 操作符当前为简化实现，仅判断相等");
        return actualStatusId.value().equals(targetStatusId);
    }

    /**
     * 评估"已超过"条件
     * <p>
     * 注意：此方法需要查询价值流定义来判断状态顺序，当前简化实现返回 false
     * TODO: 实现完整的价值流状态顺序判断逻辑
     */
    private boolean evaluatePassed(StatusId actualStatusId, String targetStatusId, String streamId) {
        if (actualStatusId == null) {
            return false;
        }
        // 简化实现：返回 false
        // 完整实现需要查询价值流定义，判断当前状态是否在目标状态之后
        log.warn("PASSED 操作符当前为简化实现，返回 false");
        return false;
    }
}
