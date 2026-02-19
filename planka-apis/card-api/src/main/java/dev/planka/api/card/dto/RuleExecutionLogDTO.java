package dev.planka.api.card.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 规则执行日志DTO
 */
@Getter
@Setter
public class RuleExecutionLogDTO {

    /** 日志ID */
    private String id;

    /** 规则ID */
    private String ruleId;

    /** 规则名称 */
    private String ruleName;

    /** 触发卡片ID */
    private String cardId;

    /** 触发卡片标题 */
    private String cardTitle;

    /** 触发事件类型 */
    private String triggerEvent;

    /** 操作人ID */
    private String operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 执行时间 */
    private LocalDateTime executionTime;

    /** 执行耗时（毫秒） */
    private long durationMs;

    /** 执行状态 */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 受影响的卡片ID列表 */
    private List<String> affectedCardIds;

    /** 追踪ID */
    private String traceId;

    /** 动作执行结果 */
    private List<ActionResultDTO> actionResults;

    /**
     * 动作执行结果DTO
     */
    @Getter
    @Setter
    public static class ActionResultDTO {
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
        private List<String> affectedCardIds;
    }
}
