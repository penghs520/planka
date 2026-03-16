package cn.planka.notification.event;

import cn.planka.event.notification.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知事件分发器
 * 根据事件类型路由到对应的处理器
 */
@Slf4j
@Component
public class NotificationEventDispatcher {

    private final Map<Class<? extends NotificationEvent>, NotificationEventHandler> handlers = new HashMap<>();

    @Autowired
    public NotificationEventDispatcher(List<NotificationEventHandler> handlerList) {
        for (NotificationEventHandler handler : handlerList) {
            handlers.put(handler.getSupportedEventType(), handler);
            log.info("Registered notification event handler: {} for type: {}",
                    handler.getClass().getSimpleName(),
                    handler.getSupportedEventType().getSimpleName());
        }
    }

    /**
     * 分发事件到对应的处理器
     */
    public void dispatch(NotificationEvent event) {
        if (event == null) {
            log.warn("Received null notification event");
            return;
        }

        NotificationEventHandler handler = handlers.get(event.getClass());
        if (handler == null) {
            log.warn("No handler found for event type: {}", event.getClass().getSimpleName());
            return;
        }

        try {
            log.info("Dispatching event: {} to handler: {}",
                    event.getClass().getSimpleName(),
                    handler.getClass().getSimpleName());
            handler.handle(event);
        } catch (Exception e) {
            log.error("Failed to handle event: {}", event, e);
        }
    }
}
