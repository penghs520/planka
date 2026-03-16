package cn.planka.notification.model;

import cn.planka.domain.schema.definition.rule.BizRuleDefinition.TriggerEvent;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * 通知上下文
 * 包含处理通知所需的所有上下文信息
 */
@Data
@Builder
public class NotificationContext {
    /**
     * 组织ID
     */
    private String orgId;

    /**
     * 触发事件
     */
    private TriggerEvent triggerEvent;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 源卡片ID
     */
    private String sourceCardId;

    /**
     * 事件数据
     */
    private Map<String, Object> eventData;

    /**
     * 事件发生时间
     */
    private Instant occurredAt;

    /**
     * 卡片快照（当前状态）
     */
    private CardSnapshot cardSnapshot;

    /**
     * 卡片快照（变更前状态）
     */
    private CardSnapshot previousCardSnapshot;

    /**
     * 操作人信息
     */
    private UserInfo operator;
}
