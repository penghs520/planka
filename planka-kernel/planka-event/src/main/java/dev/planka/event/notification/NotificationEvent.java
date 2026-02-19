package dev.planka.event.notification;

import dev.planka.event.DomainEvent;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardChangedNotificationEvent.class, name = "card.changed"),
})
public abstract class NotificationEvent extends DomainEvent {

    private static final String EVENT_TYPE = "card.changed";

    private static final String TOPIC = "planka-notification-events";

    protected NotificationEvent(String orgId, String operatorId, String sourceIp, String traceId) {
        super(orgId, operatorId, sourceIp, traceId);
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public String getTopic() {
        return TOPIC;
    }

    @Override
    public String getPartitionKey() {
        return "Notification.Event";
    }
}
