package dev.planka.card.service.rule.executor.action;

import dev.planka.card.event.NotificationEventPublisher;
import dev.planka.card.service.rule.executor.RuleActionExecutor;
import dev.planka.card.service.rule.executor.RuleExecutionContext;
import dev.planka.card.service.rule.executor.RuleExecutionResult;
import dev.planka.domain.notification.NotificationTemplateId;
import dev.planka.domain.schema.definition.rule.action.SendNotificationAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 发送通知动作执行器
 */
@Slf4j
@Component
public class SendNotificationActionExecutor implements RuleActionExecutor<SendNotificationAction> {

    private final NotificationEventPublisher notificationEventPublisher;

    public SendNotificationActionExecutor(NotificationEventPublisher notificationEventPublisher) {
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Override
    public String getActionType() {
        return "SEND_NOTIFICATION";
    }

    @Override
    public RuleExecutionResult.ActionExecutionResult execute(SendNotificationAction action, RuleExecutionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            if (action.getTemplateIds() == null || action.getTemplateIds().isEmpty()) {
                return RuleExecutionResult.ActionExecutionResult.success(
                        getActionType(),
                        action.getSortOrder(),
                        System.currentTimeMillis() - startTime,
                        List.of()
                );
            }

            // 遍历所有选中的模板并发送通知
            for (NotificationTemplateId templateId : action.getTemplateIds()) {
                notificationEventPublisher.publish(templateId, context.getOrgId(), context.getSourceIp(), context.getCardId(), context.getOperatorId());
                log.info("发送模板通知: templateId={}", templateId);
            }

            return RuleExecutionResult.ActionExecutionResult.success(
                    getActionType(),
                    action.getSortOrder(),
                    System.currentTimeMillis() - startTime,
                    List.of()
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("发送通知失败: error={}", e.getMessage(), e);
            return RuleExecutionResult.ActionExecutionResult.failed(
                    getActionType(),
                    action.getSortOrder(),
                    duration,
                    e.getMessage()
            );
        }
    }
}
