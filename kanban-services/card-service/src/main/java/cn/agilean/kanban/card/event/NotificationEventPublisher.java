package cn.agilean.kanban.card.event;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.api.card.request.MoveCardRequest;
import cn.agilean.kanban.api.card.request.UpdateCardRequest;
import cn.agilean.kanban.card.model.CardEntity;
import cn.agilean.kanban.card.service.flowrecord.ValueStreamHelper;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.domain.card.CardTypeId;
import cn.agilean.kanban.domain.card.OrgId;
import cn.agilean.kanban.domain.field.FieldValue;
import cn.agilean.kanban.domain.history.OperationSourceContext;
import cn.agilean.kanban.domain.notification.NotificationTemplateId;
import cn.agilean.kanban.domain.schema.definition.stream.ValueStreamDefinition;
import cn.agilean.kanban.domain.stream.StatusId;
import cn.agilean.kanban.domain.stream.StatusWorkType;
import cn.agilean.kanban.domain.stream.StepId;
import cn.agilean.kanban.event.card.*;
import cn.agilean.kanban.event.notification.CardChangedNotificationEvent;
import cn.agilean.kanban.event.publisher.EventPublisher;
import cn.agilean.kanban.infra.cache.schema.query.ValueStreamCacheQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
