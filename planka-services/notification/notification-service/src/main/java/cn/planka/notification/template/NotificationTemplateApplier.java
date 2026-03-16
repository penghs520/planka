package cn.planka.notification.template;

import cn.planka.domain.notification.LongNotificationContent;
import cn.planka.domain.notification.NotificationContent;
import cn.planka.domain.notification.NotificationTemplateDefinition;
import cn.planka.domain.notification.ShortNotificationContent;
import cn.planka.notification.expression.ExpressionEvaluator;
import cn.planka.notification.model.AppliedTemplate;
import cn.planka.notification.model.CardSnapshot;
import cn.planka.notification.model.NotificationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知模板应用器
 * 应用模板，解析表达式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTemplateApplier {

    private final ExpressionEvaluator expressionEvaluator;

    /**
     * 应用模板，解析表达式
     */
    public AppliedTemplate apply(NotificationTemplateDefinition template, NotificationContext context) {
        try {
            // 构建表达式上下文
            Map<String, Object> expressionContext = buildExpressionContext(context);

            // 解析标题模板
            String title = expressionEvaluator.evaluate(
                    template.getTitleTemplate().template(),
                    expressionContext
            );

            // 解析内容模板
            String content = null;
            String richContent = null;

            NotificationContent notificationContent = template.getContent();
            if (notificationContent instanceof ShortNotificationContent) {
                ShortNotificationContent shortContent = (ShortNotificationContent) notificationContent;
                content = expressionEvaluator.evaluate(
                        shortContent.getTextTemplate().template(),
                        expressionContext
                );
            } else if (notificationContent instanceof LongNotificationContent) {
                LongNotificationContent longContent = (LongNotificationContent) notificationContent;
                richContent = expressionEvaluator.evaluate(
                        longContent.getRichTextTemplate().template(),
                        expressionContext
                );
            }

            return AppliedTemplate.builder()
                    .template(template)
                    .title(title)
                    .content(content)
                    .richContent(richContent)
                    .build();
        } catch (Exception e) {
            log.error("Failed to apply template: {}", template.getId(), e);
            throw new RuntimeException("Failed to apply template", e);
        }
    }

    /**
     * 构建表达式上下文
     */
    private Map<String, Object> buildExpressionContext(NotificationContext context) {
        Map<String, Object> expressionContext = new HashMap<>();

        // 添加操作人
        if (context.getOperator() != null) {
            expressionContext.put("操作人", context.getOperator());
            expressionContext.put("operator", context.getOperator());
        }

        // 添加卡片数据
        if (context.getCardSnapshot() != null) {
            CardSnapshot card = context.getCardSnapshot();

            // 添加卡片类型名称作为 key
            expressionContext.put(card.getCardTypeName(), card.getFieldValues());

            // 添加所有字段值（支持 ${字段名} 格式）
            if (card.getFieldValues() != null) {
                expressionContext.putAll(card.getFieldValues());
            }
        }

        return expressionContext;
    }
}
