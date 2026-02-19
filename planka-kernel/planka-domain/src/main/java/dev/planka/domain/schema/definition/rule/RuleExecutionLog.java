package dev.planka.domain.schema.definition.rule;

import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.BizRuleId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 规则执行日志实体
 * <p>
 * 记录业务规则的执行历史，便于审计和排查问题。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleExecutionLog {

    /** 日志ID */
    @JsonProperty("id")
    private String id;

    /** 规则ID */
    @JsonProperty("ruleId")
    private BizRuleId ruleId;

    /** 规则名称 */
    @JsonProperty("ruleName")
    private String ruleName;

    /** 卡片类型ID（用于分表） */
    @JsonProperty("cardTypeId")
    private CardTypeId cardTypeId;

    /** 触发卡片ID */
    @JsonProperty("cardId")
    private CardId cardId;

    /** 触发事件类型 */
    @JsonProperty("triggerEvent")
    private BizRuleDefinition.TriggerEvent triggerEvent;

    /** 操作人ID */
    @JsonProperty("operatorId")
    private String operatorId;

    /** 执行时间 */
    @JsonProperty("executionTime")
    private LocalDateTime executionTime;

    /** 执行耗时（毫秒） */
    @JsonProperty("durationMs")
    private long durationMs;

    /** 执行状态 */
    @JsonProperty("status")
    private ExecutionStatus status;

    /** 受影响的卡片ID列表 */
    @JsonProperty("affectedCardIds")
    private List<String> affectedCardIds;

    /** 各动作执行结果 */
    @JsonProperty("actionResults")
    private List<ActionResult> actionResults;

    /** 错误信息 */
    @JsonProperty("errorMessage")
    private String errorMessage;

    /** 追踪ID */
    @JsonProperty("traceId")
    private String traceId;

    /** 重试次数 */
    @JsonProperty("retryCount")
    private int retryCount;

    /** 创建时间 */
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    /**
     * 执行状态枚举
     */
    public enum ExecutionStatus {
        /** 执行成功 */
        SUCCESS,
        /** 执行失败 */
        FAILED,
        /** 跳过（条件不满足） */
        SKIPPED
    }

    /**
     * 动作执行结果
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionResult {
        /** 动作类型 */
        @JsonProperty("actionType")
        private String actionType;

        /** 执行顺序 */
        @JsonProperty("sortOrder")
        private int sortOrder;

        /** 是否成功 */
        @JsonProperty("success")
        private boolean success;

        /** 执行耗时（毫秒） */
        @JsonProperty("durationMs")
        private long durationMs;

        /** 错误信息 */
        @JsonProperty("errorMessage")
        private String errorMessage;

        /** 受影响的卡片ID列表 */
        @JsonProperty("affectedCardIds")
        private List<String> affectedCardIds;
    }

    /**
     * 生成新的日志ID
     */
    public static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 创建成功日志
     */
    public static RuleExecutionLog success(BizRuleId ruleId, String ruleName, CardTypeId cardTypeId,
                                            CardId cardId, BizRuleDefinition.TriggerEvent triggerEvent,
                                            String operatorId, long durationMs,
                                            List<ActionResult> actionResults, String traceId) {
        RuleExecutionLog log = new RuleExecutionLog();
        log.setId(generateId());
        log.setRuleId(ruleId);
        log.setRuleName(ruleName);
        log.setCardTypeId(cardTypeId);
        log.setCardId(cardId);
        log.setTriggerEvent(triggerEvent);
        log.setOperatorId(operatorId);
        log.setExecutionTime(LocalDateTime.now());
        log.setDurationMs(durationMs);
        log.setStatus(ExecutionStatus.SUCCESS);
        log.setActionResults(actionResults);
        log.setTraceId(traceId);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    /**
     * 创建失败日志
     */
    public static RuleExecutionLog failed(BizRuleId ruleId, String ruleName, CardTypeId cardTypeId,
                                           CardId cardId, BizRuleDefinition.TriggerEvent triggerEvent,
                                           String operatorId, long durationMs,
                                           String errorMessage, String traceId) {
        RuleExecutionLog log = new RuleExecutionLog();
        log.setId(generateId());
        log.setRuleId(ruleId);
        log.setRuleName(ruleName);
        log.setCardTypeId(cardTypeId);
        log.setCardId(cardId);
        log.setTriggerEvent(triggerEvent);
        log.setOperatorId(operatorId);
        log.setExecutionTime(LocalDateTime.now());
        log.setDurationMs(durationMs);
        log.setStatus(ExecutionStatus.FAILED);
        log.setErrorMessage(errorMessage);
        log.setTraceId(traceId);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    /**
     * 创建跳过日志
     */
    public static RuleExecutionLog skipped(BizRuleId ruleId, String ruleName, CardTypeId cardTypeId,
                                            CardId cardId, BizRuleDefinition.TriggerEvent triggerEvent,
                                            String operatorId, String reason, String traceId) {
        RuleExecutionLog log = new RuleExecutionLog();
        log.setId(generateId());
        log.setRuleId(ruleId);
        log.setRuleName(ruleName);
        log.setCardTypeId(cardTypeId);
        log.setCardId(cardId);
        log.setTriggerEvent(triggerEvent);
        log.setOperatorId(operatorId);
        log.setExecutionTime(LocalDateTime.now());
        log.setDurationMs(0);
        log.setStatus(ExecutionStatus.SKIPPED);
        log.setErrorMessage(reason);
        log.setTraceId(traceId);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}
