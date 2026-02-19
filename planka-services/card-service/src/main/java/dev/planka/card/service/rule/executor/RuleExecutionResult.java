package dev.planka.card.service.rule.executor;

import dev.planka.domain.card.CardId;
import dev.planka.domain.schema.definition.rule.RuleExecutionLog;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则执行结果
 * <p>
 * 记录单个规则的执行结果，包括各动作的执行情况。
 */
@Getter
@Setter
@Builder
public class RuleExecutionResult {

    /** 执行状态 */
    private ExecutionStatus status;

    /** 执行耗时（毫秒） */
    private long durationMs;

    /** 错误信息（失败时） */
    private String errorMessage;

    /** 受影响的卡片ID列表 */
    @Builder.Default
    private List<CardId> affectedCardIds = new ArrayList<>();

    /** 各动作执行结果 */
    @Builder.Default
    private List<ActionExecutionResult> actionResults = new ArrayList<>();

    /** 重试次数 */
    @Builder.Default
    private int retryCount = 0;

    /**
     * 执行状态枚举
     */
    public enum ExecutionStatus {
        /** 执行成功 */
        SUCCESS,
        /** 执行失败 */
        FAILED,
        /** 跳过（条件不满足） */
        SKIPPED,
        /** 部分成功 */
        PARTIAL_SUCCESS
    }

    /**
     * 创建成功结果
     */
    public static RuleExecutionResult success(long durationMs, List<ActionExecutionResult> actionResults) {
        List<CardId> affectedIds = new ArrayList<>();
        for (ActionExecutionResult actionResult : actionResults) {
            if (actionResult.getAffectedCardIds() != null) {
                affectedIds.addAll(actionResult.getAffectedCardIds());
            }
        }
        return RuleExecutionResult.builder()
                .status(ExecutionStatus.SUCCESS)
                .durationMs(durationMs)
                .actionResults(actionResults)
                .affectedCardIds(affectedIds)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static RuleExecutionResult failed(long durationMs, String errorMessage) {
        return RuleExecutionResult.builder()
                .status(ExecutionStatus.FAILED)
                .durationMs(durationMs)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建跳过结果
     */
    public static RuleExecutionResult skipped(String reason) {
        return RuleExecutionResult.builder()
                .status(ExecutionStatus.SKIPPED)
                .durationMs(0)
                .errorMessage(reason)
                .build();
    }

    /**
     * 创建部分成功结果
     */
    public static RuleExecutionResult partialSuccess(long durationMs, List<ActionExecutionResult> actionResults,
                                                      String errorMessage) {
        List<CardId> affectedIds = new ArrayList<>();
        for (ActionExecutionResult actionResult : actionResults) {
            if (actionResult.isSuccess() && actionResult.getAffectedCardIds() != null) {
                affectedIds.addAll(actionResult.getAffectedCardIds());
            }
        }
        return RuleExecutionResult.builder()
                .status(ExecutionStatus.PARTIAL_SUCCESS)
                .durationMs(durationMs)
                .actionResults(actionResults)
                .affectedCardIds(affectedIds)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS;
    }

    /**
     * 是否跳过
     */
    public boolean isSkipped() {
        return status == ExecutionStatus.SKIPPED;
    }

    /**
     * 转换为日志实体的ActionResult列表
     */
    public List<RuleExecutionLog.ActionResult> toLogActionResults() {
        List<RuleExecutionLog.ActionResult> logResults = new ArrayList<>();
        for (ActionExecutionResult actionResult : actionResults) {
            RuleExecutionLog.ActionResult logResult = new RuleExecutionLog.ActionResult();
            logResult.setActionType(actionResult.getActionType());
            logResult.setSortOrder(actionResult.getSortOrder());
            logResult.setSuccess(actionResult.isSuccess());
            logResult.setDurationMs(actionResult.getDurationMs());
            logResult.setErrorMessage(actionResult.getErrorMessage());
            if (actionResult.getAffectedCardIds() != null) {
                logResult.setAffectedCardIds(
                        actionResult.getAffectedCardIds().stream()
                                .map(CardId::value)
                                .toList()
                );
            }
            logResults.add(logResult);
        }
        return logResults;
    }

    /**
     * 动作执行结果
     */
    @Getter
    @Setter
    @Builder
    public static class ActionExecutionResult {
        /** 动作类型 */
        private String actionType;

        /** 执行顺序 */
        private int sortOrder;

        /** 是否成功 */
        private boolean success;

        /** 执行耗时（毫秒） */
        private long durationMs;

        /** 错误信息 */
        private String errorMessage;

        /** 受影响的卡片ID列表 */
        private List<CardId> affectedCardIds;

        /** 附加数据 */
        private Object data;

        /**
         * 创建成功结果
         */
        public static ActionExecutionResult success(String actionType, int sortOrder,
                                                     long durationMs, List<CardId> affectedCardIds) {
            return ActionExecutionResult.builder()
                    .actionType(actionType)
                    .sortOrder(sortOrder)
                    .success(true)
                    .durationMs(durationMs)
                    .affectedCardIds(affectedCardIds)
                    .build();
        }

        /**
         * 创建失败结果
         */
        public static ActionExecutionResult failed(String actionType, int sortOrder,
                                                    long durationMs, String errorMessage) {
            return ActionExecutionResult.builder()
                    .actionType(actionType)
                    .sortOrder(sortOrder)
                    .success(false)
                    .durationMs(durationMs)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}
