package cn.agilean.kanban.domain.notification;

import cn.agilean.kanban.domain.expression.TextExpressionTemplate;
import cn.agilean.kanban.domain.schema.definition.rule.action.RecipientSelector;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 长通知内容
 * <p>
 * 用于邮件通知，包含抄送人和富文本表达式模板。
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class LongNotificationContent implements NotificationContent {

    public static final String TYPE = "LONG";

    /**
     * 抄送人选择器
     * <p>
     * 定义邮件的抄送人选择策略
     */
    @JsonProperty("ccSelector")
    private final RecipientSelector ccSelector;

    /**
     * 富文本表达式模板
     * <p>
     * HTML 格式，支持表达式变量，如 ${card.title}、${operator.name}
     */
    @JsonProperty("richTextTemplate")
    private final TextExpressionTemplate richTextTemplate;

    @JsonCreator
    public LongNotificationContent(
            @JsonProperty("ccSelector") RecipientSelector ccSelector,
            @JsonProperty("richTextTemplate") TextExpressionTemplate richTextTemplate) {
        this.ccSelector = ccSelector;
        this.richTextTemplate = richTextTemplate;
    }

    @Override
    public NotificationContentType getType() {
        return NotificationContentType.LONG;
    }

    /**
     * 工厂方法 - 无抄送人
     */
    public static LongNotificationContent of(String template) {
        return new LongNotificationContent(null, TextExpressionTemplate.of(template));
    }

    /**
     * 工厂方法 - 带抄送人
     */
    public static LongNotificationContent of(RecipientSelector ccSelector, String template) {
        return new LongNotificationContent(ccSelector, TextExpressionTemplate.of(template));
    }
}
