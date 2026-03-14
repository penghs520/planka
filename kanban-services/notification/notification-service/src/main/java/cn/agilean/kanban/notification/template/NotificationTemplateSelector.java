package cn.agilean.kanban.notification.template;

import cn.agilean.kanban.domain.notification.NotificationTemplateDefinition;
import cn.agilean.kanban.domain.notification.CardTypeDefinitionParameter;
import cn.agilean.kanban.domain.notification.DefinitionParameter;
import cn.agilean.kanban.domain.schema.definition.rule.BizRuleDefinition.TriggerEvent;
import cn.agilean.kanban.notification.model.NotificationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知模板选择器
 * 根据触发事件和参数类型选择匹配的模板
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTemplateSelector {

    // TODO: 注入 Schema Cache 或 NotificationTemplateRepository

    /**
     * 根据触发事件和参数类型选择匹配的模板
     */
    public List<NotificationTemplateDefinition> selectTemplates(NotificationContext context) {
        try {
            // TODO: 从 Schema Cache 查询启用的通知模板
            // List<NotificationTemplateDefinition> templates = schemaCache.getNotificationTemplates(
            //     context.getOrgId(),
            //     context.getTriggerEvent(),
            //     true
            // );

            // 临时返回空列表
            List<NotificationTemplateDefinition> templates = Collections.emptyList();

            // 过滤匹配参数类型的模板
            return templates.stream()
                    .filter(template -> matchesParameter(template, context))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to select templates for context: {}", context, e);
            return Collections.emptyList();
        }
    }

    /**
     * 检查模板参数是否匹配上下文
     */
    private boolean matchesParameter(NotificationTemplateDefinition template, NotificationContext context) {
        DefinitionParameter param = template.getDefinitionParameter();

        if (param instanceof CardTypeDefinitionParameter) {
            CardTypeDefinitionParameter cardParam = (CardTypeDefinitionParameter) param;
            if (context.getCardSnapshot() == null) {
                return false;
            }
            return cardParam.getCardTypeId().equals(context.getCardSnapshot().getCardTypeId());
        }

        // 其他参数类型默认匹配
        return true;
    }
}
