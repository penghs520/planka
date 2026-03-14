package cn.agilean.kanban.domain.notification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 通知内容接口
 * <p>
 * 通知模板只能选择短内容模板或长内容模板之一，不能同时选择。
 * <ul>
 *   <li>短内容模板：用于 IM/系统通知，只包含一个文本表达式模板</li>
 *   <li>长内容模板：用于邮件通知，包含抄送人和富文本表达式模板</li>
 * </ul>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ShortNotificationContent.class, name = "SHORT"),
        @JsonSubTypes.Type(value = LongNotificationContent.class, name = "LONG")
})
public sealed interface NotificationContent permits ShortNotificationContent, LongNotificationContent {

    /**
     * 获取内容类型
     */
    NotificationContentType getType();
}
