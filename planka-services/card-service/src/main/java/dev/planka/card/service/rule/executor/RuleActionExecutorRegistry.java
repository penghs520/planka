package dev.planka.card.service.rule.executor;

import dev.planka.domain.schema.definition.rule.action.RuleAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 规则动作执行器注册表
 * <p>
 * 管理所有动作执行器，根据动作类型分发到对应的执行器。
 */
@Slf4j
@Component
public class RuleActionExecutorRegistry {

    private final Map<String, RuleActionExecutor<?>> executorMap = new HashMap<>();

    public RuleActionExecutorRegistry(List<RuleActionExecutor<?>> executors) {
        for (RuleActionExecutor<?> executor : executors) {
            String actionType = executor.getActionType();
            if (executorMap.containsKey(actionType)) {
                log.warn("动作执行器类型重复注册: actionType={}, existing={}, new={}",
                        actionType,
                        executorMap.get(actionType).getClass().getSimpleName(),
                        executor.getClass().getSimpleName());
            }
            executorMap.put(actionType, executor);
            log.debug("注册动作执行器: actionType={}, executor={}",
                    actionType, executor.getClass().getSimpleName());
        }
        log.info("动作执行器注册完成，共注册 {} 个执行器", executorMap.size());
    }

    /**
     * 获取动作执行器
     */
    @SuppressWarnings("unchecked")
    public <T extends RuleAction> Optional<RuleActionExecutor<T>> getExecutor(String actionType) {
        RuleActionExecutor<?> executor = executorMap.get(actionType);
        return Optional.ofNullable((RuleActionExecutor<T>) executor);
    }

    /**
     * 获取动作执行器
     */
    @SuppressWarnings("unchecked")
    public <T extends RuleAction> Optional<RuleActionExecutor<T>> getExecutor(T action) {
        return getExecutor(action.getActionType());
    }

    /**
     * 执行动作
     */
    @SuppressWarnings("unchecked")
    public RuleExecutionResult.ActionExecutionResult executeAction(RuleAction action, RuleExecutionContext context) {
        String actionType = action.getActionType();
        Optional<RuleActionExecutor<RuleAction>> executorOpt = getExecutor(actionType);

        if (executorOpt.isEmpty()) {
            log.warn("未找到动作执行器: actionType={}", actionType);
            return RuleExecutionResult.ActionExecutionResult.failed(
                    actionType,
                    action.getSortOrder(),
                    0,
                    "未找到动作执行器: " + actionType
            );
        }

        RuleActionExecutor<RuleAction> executor = executorOpt.get();
        long startTime = System.currentTimeMillis();

        try {
            return executor.execute(action, context);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("执行动作失败: actionType={}, error={}", actionType, e.getMessage(), e);
            return RuleExecutionResult.ActionExecutionResult.failed(
                    actionType,
                    action.getSortOrder(),
                    duration,
                    e.getMessage()
            );
        }
    }

    /**
     * 是否存在执行器
     */
    public boolean hasExecutor(String actionType) {
        return executorMap.containsKey(actionType);
    }

    /**
     * 获取所有已注册的动作类型
     */
    public java.util.Set<String> getRegisteredActionTypes() {
        return executorMap.keySet();
    }
}
