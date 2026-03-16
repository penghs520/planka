package cn.planka.notification.event;

import cn.planka.event.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 通知事件监听器
 * 监听 Kafka 通知事件主题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationEventDispatcher eventDispatcher;

    /**
     * 监听通知事件
     */
    @KafkaListener(
            topics = "planka-notification-events",
            groupId = "notification-service-group",
            containerFactory = "notificationEventKafkaListenerContainerFactory"
    )
    public void handleEvent(NotificationEvent event) {
        log.info("Received notification event: type={}, orgId={}, operatorId={}",
                event.getClass().getSimpleName(),
                event.getOrgId(),
                event.getOperatorId());

        try {
            eventDispatcher.dispatch(event);
        } catch (Exception e) {
            log.error("Failed to dispatch notification event: {}", event, e);
            // TODO: 发送到死信队列
        }
    }
}
