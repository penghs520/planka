package dev.planka.card.service.rule.executor;

import dev.planka.card.service.permission.ConditionEvaluator;
import dev.planka.card.service.rule.log.RuleExecutionLogService;
import dev.planka.domain.card.CardId;
import dev.planka.domain.history.OperationSourceContext;
import dev.planka.domain.history.source.BizRuleOperationSource;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.rule.BizRuleDefinition;
import dev.planka.domain.schema.definition.rule.RetryConfig;
import dev.planka.domain.schema.definition.rule.RuleExecutionLog;
import dev.planka.domain.schema.definition.rule.action.RuleAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 业务规则执行服务
 * <p>
 * 负责执行业务规则，包括条件评估和动作执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizRuleExecutionService {

    private final RuleActionExecutorRegistry executorRegistry;
    private final ConditionEvaluator conditionEvaluator;
    private final RuleExecutionLogService logService;

    /**
     * 同步执行规则
     *
     * @param rule    规则定义
     * @param context 执行上下文
     * @return 执行结果
     */
    public RuleExecutionResult execute(BizRuleDefinition rule, RuleExecutionContext context) {
        context.setCurrentRule(rule);
        context.setCurrentRuleId(rule.getId());
        long startTime = System.currentTimeMillis();

        try {
            // 1. 检查规则是否启用
            if (!rule.isEnabled()) {
                log.debug("规则已禁用，跳过执行: ruleId={}", rule.getId());
                return RuleExecutionResult.skipped("规则已禁用");
            }

            // 2. 评估触发条件
            if (!evaluateCondition(rule.getCondition(), context)) {
                log.debug("规则条件不满足，跳过执行: ruleId={}", rule.getId());
                return RuleExecutionResult.skipped("条件不满足");
            }

            // 3. 执行所有动作（含重试）
            RuleExecutionResult result = executeActionsWithRetry(rule, context);

            // 4. 记录执行日志
            recordLog(rule, context, result);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("执行规则失败: ruleId={}, error={}", rule.getId(), e.getMessage(), e);
            RuleExecutionResult result = RuleExecutionResult.failed(duration, e.getMessage());
            recordLog(rule, context, result);
            return result;
        }
    }

    /**
     * 异步执行规则
     *
     * @param rule    规则定义
     * @param context 执行上下文
     */
    @Async("bizRuleExecutor")
    public void executeAsync(BizRuleDefinition rule, RuleExecutionContext context) {
        log.debug("异步执行规则: ruleId={}, traceId={}", rule.getId(), context.getTraceId());
        execute(rule, context);
    }

    /**
     * 执行所有动作（含重试逻辑）
     */
    private RuleExecutionResult executeActionsWithRetry(BizRuleDefinition rule, RuleExecutionContext context) {
        RetryConfig retryConfig = rule.getRetryConfig();
        int maxRetries = (retryConfig != null) ? retryConfig.getMaxRetries() : 0;

        RuleExecutionResult result = executeActions(rule, context);
        int retryCount = 0;

        while (hasActionFailure(result) && retryCount < maxRetries) {
            retryCount++;
            long delay = retryConfig.calculateDelay(retryCount);
            log.info("规则动作执行失败，准备第 {} 次重试（延迟 {}ms）: ruleId={}", retryCount, delay, rule.getId());

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("重试等待被中断: ruleId={}, retryCount={}", rule.getId(), retryCount);
                break;
            }

            result = executeActions(rule, context);
        }

        result.setRetryCount(retryCount);
        return result;
    }

    /**
     * 判断执行结果是否包含动作失败
     */
    private boolean hasActionFailure(RuleExecutionResult result) {
        return result.getStatus() == RuleExecutionResult.ExecutionStatus.FAILED
                || result.getStatus() == RuleExecutionResult.ExecutionStatus.PARTIAL_SUCCESS;
    }

    /**
     * 执行动作（不使用上下文）- 作为上下文设置失败时的降级方案
     */
    private RuleExecutionResult executeActionsWithoutContext(BizRuleDefinition rule,
                                                              RuleExecutionContext context,
                                                              List<RuleAction> sortedActions) {
        List<RuleExecutionResult.ActionExecutionResult> actionResults = new ArrayList<>();
        boolean hasFailure = false;
        String lastError = null;

        for (RuleAction action : sortedActions) {
            RuleExecutionResult.ActionExecutionResult actionResult = executorRegistry.executeAction(action, context);
            actionResults.add(actionResult);

            if (!actionResult.isSuccess()) {
                hasFailure = true;
                lastError = actionResult.getErrorMessage();
                log.warn("动作执行失败，继续执行后续动作: actionType={}, error={}",
                        action.getActionType(), actionResult.getErrorMessage());
            }
        }

        long duration = context.getDuration();
        if (hasFailure) {
            return RuleExecutionResult.partialSuccess(duration, actionResults, lastError);
        }
        return RuleExecutionResult.success(duration, actionResults);
    }

    /**
     * 评估条件
     */
    private boolean evaluateCondition(Condition condition, RuleExecutionContext context) {
        if (condition == null) {
            return true;
        }

        try {
            return conditionEvaluator.evaluate(
                    condition,
                    context.getTriggerCard()
            );
        } catch (Exception e) {
            log.error("评估条件失败: error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 执行所有动作
     */
    private RuleExecutionResult executeActions(BizRuleDefinition rule, RuleExecutionContext context) {
        List<RuleAction> actions = rule.getActions();
        if (actions == null || actions.isEmpty()) {
            log.debug("规则没有配置动作: ruleId={}", rule.getId());
            return RuleExecutionResult.success(context.getDuration(), List.of());
        }

        // 按执行顺序排序
        List<RuleAction> sortedActions = actions.stream()
                .sorted(Comparator.comparingInt(RuleAction::getSortOrder))
                .toList();

        List<RuleExecutionResult.ActionExecutionResult> actionResults = new ArrayList<>();
        boolean hasFailure = false;
        String lastError = null;

        // 设置操作来源为业务规则，确保所有动作触发的卡片变更都记录正确的来源
        BizRuleOperationSource operationSource = new BizRuleOperationSource(
                rule.getId().value(),
                rule.getName()
        );

        try (AutoCloseable ignored = OperationSourceContext.with(operationSource)) {
            for (RuleAction action : sortedActions) {
                RuleExecutionResult.ActionExecutionResult actionResult = executorRegistry.executeAction(action, context);
                actionResults.add(actionResult);

                if (!actionResult.isSuccess()) {
                    hasFailure = true;
                    lastError = actionResult.getErrorMessage();
                    log.warn("动作执行失败，继续执行后续动作: actionType={}, error={}",
                            action.getActionType(), actionResult.getErrorMessage());
                }
            }
        } catch (Exception e) {
            // 区分是上下文设置失败还是动作执行失败
            if (actionResults.isEmpty()) {
                // 上下文设置失败，记录错误但继续执行（不使用上下文）
                log.error("设置操作来源上下文失败: ruleId={}, error={}", rule.getId(), e.getMessage());
                // 重新执行，不使用上下文
                return executeActionsWithoutContext(rule, context, sortedActions);
            } else {
                // 动作执行过程中抛出异常，需要传播到外层
                throw new RuntimeException("动作执行失败: " + e.getMessage(), e);
            }
        }

        long duration = context.getDuration();
        if (hasFailure) {
            // 检查是否全部失败
            boolean allFailed = actionResults.stream().noneMatch(RuleExecutionResult.ActionExecutionResult::isSuccess);
            if (allFailed) {
                return RuleExecutionResult.failed(duration, lastError);
            }
            // 部分成功
            return RuleExecutionResult.partialSuccess(duration, actionResults, lastError);
        }

        return RuleExecutionResult.success(duration, actionResults);
    }

    /**
     * 记录执行日志
     */
    private void recordLog(BizRuleDefinition rule, RuleExecutionContext context, RuleExecutionResult result) {
        try {
            RuleExecutionLog executionLog;

            if (result.isSuccess()) {
                executionLog = RuleExecutionLog.success(
                        rule.getId(),
                        rule.getName(),
                        context.getCardTypeId(),
                        context.getCardId(),
                        context.getTriggerEvent(),
                        context.getOperatorId(),
                        result.getDurationMs(),
                        result.toLogActionResults(),
                        context.getTraceId()
                );
                // 设置受影响的卡片ID
                if (result.getAffectedCardIds() != null) {
                    executionLog.setAffectedCardIds(
                            result.getAffectedCardIds().stream()
                                    .map(CardId::value)
                                    .toList()
                    );
                }
            } else if (result.isSkipped()) {
                executionLog = RuleExecutionLog.skipped(
                        rule.getId(),
                        rule.getName(),
                        context.getCardTypeId(),
                        context.getCardId(),
                        context.getTriggerEvent(),
                        context.getOperatorId(),
                        result.getErrorMessage(),
                        context.getTraceId()
                );
            } else {
                executionLog = RuleExecutionLog.failed(
                        rule.getId(),
                        rule.getName(),
                        context.getCardTypeId(),
                        context.getCardId(),
                        context.getTriggerEvent(),
                        context.getOperatorId(),
                        result.getDurationMs(),
                        result.getErrorMessage(),
                        context.getTraceId()
                );
            }

            executionLog.setRetryCount(result.getRetryCount());
            logService.save(executionLog);
        } catch (Exception e) {
            log.error("记录执行日志失败: ruleId={}, error={}", rule.getId(), e.getMessage(), e);
        }
    }
}
