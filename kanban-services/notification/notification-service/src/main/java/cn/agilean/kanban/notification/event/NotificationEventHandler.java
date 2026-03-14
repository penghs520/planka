package cn.agilean.kanban.notification.event;

import cn.agilean.kanban.event.notification.NotificationEvent;

/**
 * 通知事件处理器接口
 */
public interface NotificationEventHandler<T extends NotificationEvent> {
    /**
     * 处理事件
     */
    void handle(T event);

    /**
     * 获取支持的事件类型
     */
    Class<T> getSupportedEventType();
}
