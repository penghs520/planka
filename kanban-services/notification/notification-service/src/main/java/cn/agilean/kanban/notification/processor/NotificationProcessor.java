package cn.agilean.kanban.notification.processor;

import cn.agilean.kanban.domain.notification.NotificationTemplateDefinition;
import cn.agilean.kanban.event.notification.NotificationEvent;
import cn.agilean.kanban.notification.channel.ChannelDispatcher;
import cn.agilean.kanban.notification.context.NotificationContextFactory;
import cn.agilean.kanban.notification.model.AppliedTemplate;
import cn.agilean.kanban.notification.model.NotificationContext;
import cn.agilean.kanban.notification.model.NotificationSendContext;
import cn.agilean.kanban.notification.plugin.NotificationRequest.RecipientInfo;
import cn.agilean.kanban.notification.plugin.NotificationResult;
import cn.agilean.kanban.notification.recipient.RecipientResolver;
import cn.agilean.kanban.notification.template.NotificationTemplateApplier;
import cn.agilean.kanban.notification.template.NotificationTemplateSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知处理编排器
 * 处理通知事件的完整流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProcessor {

    private final NotificationContextFactory contextFactory;
    private final NotificationTemplateSelector templateSelector;
    private final NotificationTemplateApplier templateApplier;
    private final RecipientResolver recipientResolver;
    private final ChannelDispatcher channelDispatcher;
    // TODO: 注入 NotificationRecordSaver

    /**
     * 处理通知事件的完整流程
     */
    @Transactional
    public void process(NotificationEvent event) {
        try {
            // 1. 创建通知上下文
            NotificationContext context = contextFactory.createContext(event);

            // 2. 选择匹配的模板
            List<NotificationTemplateDefinition> templates = templateSelector.selectTemplates(context);

            if (templates.isEmpty()) {
                log.info("No matching templates found for event: {}", event.getEventType());
                return;
            }

            // 3. 对每个模板执行通知流程
            for (NotificationTemplateDefinition template : templates) {
                processTemplate(template, context);
            }
        } catch (Exception e) {
            log.error("Failed to process notification event: {}", event, e);
        }
    }

    /**
     * 处理单个模板
     */
    private void processTemplate(NotificationTemplateDefinition template, NotificationContext context) {
        try {
            log.info("Processing template: {} for context: {}", template.getId(), context.getSourceCardId());

            // 4. 应用模板
            AppliedTemplate appliedTemplate = templateApplier.apply(template, context);

            // 5. 解析目标用户
            List<RecipientInfo> recipients = recipientResolver.resolve(
                    template.getRecipientSelector(),
                    context
            );

            if (recipients.isEmpty()) {
                log.warn("No recipients found for template: {}", template.getId());
                return;
            }

            // 6. 构建发送上下文
            NotificationSendContext sendContext = NotificationSendContext.builder()
                    .appliedTemplate(appliedTemplate)
                    .recipients(recipients)
                    .originalContext(context)
                    .ruleId(template.getId().value())
                    .cardId(context.getSourceCardId())
                    .operatorId(context.getOperatorId())
                    .build();

            // 7. 渠道分发
            List<NotificationResult> results = channelDispatcher.dispatch(template, sendContext);

            // 8. 保存发送记录
            // TODO: recordSaver.saveRecord(sendContext, results);

            log.info("Completed processing template: {}, sent to {} recipients via {} channels",
                    template.getId(), recipients.size(), results.size());
        } catch (Exception e) {
            log.error("Failed to process template: {}", template.getId(), e);
        }
    }
}
