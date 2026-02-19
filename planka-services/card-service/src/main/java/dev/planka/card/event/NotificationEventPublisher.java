package dev.planka.card.event;

import dev.planka.domain.card.CardId;
import dev.planka.domain.notification.NotificationTemplateId;
import dev.planka.event.notification.CardChangedNotificationEvent;
import dev.planka.event.publisher.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 卡片事件发布服务
 * <p>
 * 负责发布卡片相关的领域事件，包括创建、更新、移动等事件。
 */
@Component
public class NotificationEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final EventPublisher eventPublisher;

    public NotificationEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }


    /**
     * 发送卡片变更通知事件
     */
    public void publish(NotificationTemplateId notificationTemplateId, String orgId, String sourceIp, CardId currentCardId, String operatorId) {
        CardChangedNotificationEvent notificationEvent = new CardChangedNotificationEvent(orgId, operatorId, sourceIp, null, notificationTemplateId, currentCardId);
        eventPublisher.publishAsync(notificationEvent);
    }

}
