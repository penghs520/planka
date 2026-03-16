package cn.planka.notification.plugin.builtin;

import cn.planka.notification.plugin.NotificationRequest;

/**
 * 站内信发送器接口
 * <p>
 * 由 notification-service 实现，用于将站内信存储到数据库
 */
public interface BuiltinNotificationSender {

    /**
     * 发送站内信
     *
     * @param orgId       组织ID
     * @param userId      接收者用户ID
     * @param title       标题
     * @param content     纯文本内容
     * @param richContent 富文本内容
     * @param source      来源信息
     */
    void send(String orgId, String userId, String title, String content, String richContent,
              NotificationRequest.NotificationSource source);
}
