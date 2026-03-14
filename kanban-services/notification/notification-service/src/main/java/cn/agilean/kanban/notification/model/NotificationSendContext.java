package cn.agilean.kanban.notification.model;

import cn.agilean.kanban.notification.plugin.NotificationRequest;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 通知发送上下文
 * 包含发送通知所需的完整信息
 */
@Data
@Builder
public class NotificationSendContext {
    /**
     * 应用后的模板
     */
    private AppliedTemplate appliedTemplate;

    /**
     * 接收者列表
     */
    private List<NotificationRequest.RecipientInfo> recipients;

    /**
     * 原始通知上下文
     */
    private NotificationContext originalContext;

    /**
     * 规则ID（用于追踪）
     */
    private String ruleId;

    /**
     * 卡片ID（用于追踪）
     */
    private String cardId;

    /**
     * 操作人ID（用于追踪）
     */
    private String operatorId;
}
