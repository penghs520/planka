package dev.planka.card.service.rule.executor.action;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.service.rule.executor.ActionTargetResolver;
import dev.planka.card.service.rule.executor.RuleActionExecutor;
import dev.planka.card.service.rule.executor.RuleExecutionContext;
import dev.planka.card.service.rule.executor.RuleExecutionResult;
import dev.planka.domain.card.CardId;
import dev.planka.domain.schema.definition.rule.action.ActionTargetSelector;
import dev.planka.domain.schema.definition.rule.action.RuleAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象规则动作执行器
 * <p>
 * 提供通用的执行逻辑，包括目标解析和结果封装。
 *
 * @param <T> 动作类型
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractRuleActionExecutor<T extends RuleAction> implements RuleActionExecutor<T> {

    protected final ActionTargetResolver targetResolver;

    @Override
    public RuleExecutionResult.ActionExecutionResult execute(T action, RuleExecutionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            // 解析目标卡片
            List<CardDTO> targetCards = resolveTargets(action, context);
            if (targetCards.isEmpty()) {
                log.debug("没有目标卡片，跳过动作执行: actionType={}", getActionType());
                return RuleExecutionResult.ActionExecutionResult.success(
                        getActionType(),
                        action.getSortOrder(),
                        System.currentTimeMillis() - startTime,
                        List.of()
                );
            }

            // 执行动作
            List<CardId> affectedCardIds = new ArrayList<>();
            for (CardDTO targetCard : targetCards) {
                try {
                    CardId affectedId = executeOnCard(action, targetCard, context);
                    if (affectedId != null) {
                        affectedCardIds.add(affectedId);
                    }
                } catch (Exception e) {
                    log.error("对卡片执行动作失败: cardId={}, actionType={}, error={}",
                            targetCard.getId(), getActionType(), e.getMessage());
                    throw e;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            return RuleExecutionResult.ActionExecutionResult.success(
                    getActionType(),
                    action.getSortOrder(),
                    duration,
                    affectedCardIds
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("执行动作失败: actionType={}, error={}", getActionType(), e.getMessage(), e);
            return RuleExecutionResult.ActionExecutionResult.failed(
                    getActionType(),
                    action.getSortOrder(),
                    duration,
                    e.getMessage()
            );
        }
    }

    /**
     * 解析目标卡片
     */
    protected List<CardDTO> resolveTargets(T action, RuleExecutionContext context) {
        ActionTargetSelector selector = getTargetSelector(action);
        return targetResolver.resolveTargets(selector, context);
    }

    /**
     * 获取目标选择器
     */
    protected ActionTargetSelector getTargetSelector(T action) {
        return null;  // 默认返回null，由targetResolver处理为当前卡片
    }

    /**
     * 对单个卡片执行动作
     *
     * @param action     动作定义
     * @param targetCard 目标卡片
     * @param context    执行上下文
     * @return 受影响的卡片ID
     */
    protected abstract CardId executeOnCard(T action, CardDTO targetCard, RuleExecutionContext context);
}
