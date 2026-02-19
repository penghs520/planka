package dev.planka.domain.schema.definition.rule.action;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.notification.NotificationTemplateId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 发送通知动作
 * <p>
 * 通过引用通知模板来发送通知，支持多选多个模板。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SendNotificationAction implements RuleAction {

    private final List<NotificationTemplateId> templateIds;
    private final int sortOrder;

    @JsonCreator
    public SendNotificationAction(
            @JsonProperty("templateIds") List<NotificationTemplateId> templateIds,
            @JsonProperty("sortOrder") Integer sortOrder) {
        this.templateIds = AssertUtils.requireNotEmpty(templateIds, "templateIds must not be empty");
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    @JsonProperty("templateIds")
    public List<NotificationTemplateId> getTemplateIds() {
        return templateIds;
    }

    @Override
    public String getActionType() {
        return "SEND_NOTIFICATION";
    }

    @Override
    @JsonProperty("sortOrder")
    public int getSortOrder() {
        return sortOrder;
    }
}
