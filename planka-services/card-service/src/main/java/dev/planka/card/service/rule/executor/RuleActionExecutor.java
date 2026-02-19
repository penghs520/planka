package dev.planka.card.service.rule.executor;

import dev.planka.domain.schema.definition.rule.action.RuleAction;

/**
 * 规则动作执行器接口
 * <p>
 * 每种动作类型对应一个执行器实现。
 *
 * @param <T> 动作类型
 */
public interface RuleActionExecutor<T extends RuleAction> {

    /**
     * 获取支持的动作类型
     */
    String getActionType();

    /**
     * 执行动作
     *
     * @param action  动作定义
     * @param context 执行上下文
     * @return 执行结果
     */
    RuleExecutionResult.ActionExecutionResult execute(T action, RuleExecutionContext context);

}
