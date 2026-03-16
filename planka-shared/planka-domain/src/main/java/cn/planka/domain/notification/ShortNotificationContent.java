package cn.planka.domain.notification;

import cn.planka.domain.expression.TextExpressionTemplate;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 短通知内容
 * <p>
 * 用于 IM/系统通知，只包含一个文本表达式模板。
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ShortNotificationContent implements NotificationContent {

    public static final String TYPE = "SHORT";

    /**
     * 文本表达式模板
     * <p>
     * 支持表达式变量，如 ${card.title}、${operator.name}
     */
    @JsonProperty("textTemplate")
    private final TextExpressionTemplate textTemplate;

    @JsonCreator
    public ShortNotificationContent(
            @JsonProperty("textTemplate") TextExpressionTemplate textTemplate) {
        this.textTemplate = textTemplate;
    }

    @Override
    public NotificationContentType getType() {
        return NotificationContentType.SHORT;
    }

    /**
     * 工厂方法
     */
    public static ShortNotificationContent of(String template) {
        return new ShortNotificationContent(TextExpressionTemplate.of(template));
    }

    public static ShortNotificationContent of(TextExpressionTemplate template) {
        return new ShortNotificationContent(template);
    }
}
