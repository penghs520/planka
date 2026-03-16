package cn.planka.notification.event.handler;

import cn.planka.event.notification.CardChangedNotificationEvent;
import cn.planka.notification.event.NotificationEventHandler;
import cn.planka.notification.processor.NotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 卡片变更通知事件处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardChangedNotificationEventHandler implements NotificationEventHandler<CardChangedNotificationEvent> {

    private final NotificationProcessor notificationProcessor;

    @Override
    public void handle(CardChangedNotificationEvent event) {
        log.info("Handling card changed notification event: cardId={}, templateId={}",
                event.getCurrentCardId(),
                event.getNotificationTemplateId());

        notificationProcessor.process(event);
    }

    @Override
    public Class<CardChangedNotificationEvent> getSupportedEventType() {
        return CardChangedNotificationEvent.class;
    }
}
