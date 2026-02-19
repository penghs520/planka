package dev.planka.card.service.rule.log;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 规则执行日志实体（数据库映射）
 */
@Getter
@Setter
public class RuleExecutionLogEntity {

    /** 日志ID */
    private String id;

    /** 规则ID */
    private String ruleId;

    /** 规则名称 */
    private String ruleName;

    /** 触发卡片ID */
    private String cardId;

    /** 触发事件类型 */
    private String triggerEvent;

    /** 操作人ID */
    private String operatorId;

    /** 执行时间 */
    private LocalDateTime executionTime;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** 执行状态 */
    private String status;

    /** 受影响的卡片ID列表（JSON） */
    private String affectedCardIds;

    /** 各动作执行结果（JSON） */
    private String actionResults;

    /** 错误信息 */
    private String errorMessage;

    /** 追踪ID */
    private String traceId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
