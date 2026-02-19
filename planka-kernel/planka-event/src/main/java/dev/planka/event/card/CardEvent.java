package dev.planka.event.card;

import dev.planka.domain.history.OperationSource;
import dev.planka.domain.history.source.UserOperationSource;
import dev.planka.event.DomainEvent;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

/**
 * 卡片事件基类
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardCreatedEvent.class, name = "card.created"),
        @JsonSubTypes.Type(value = CardUpdatedEvent.class, name = "card.updated"),
        @JsonSubTypes.Type(value = CardMovedEvent.class, name = "card.moved"),
        @JsonSubTypes.Type(value = CardArchivedEvent.class, name = "card.archived"),
        @JsonSubTypes.Type(value = CardAbandonedEvent.class, name = "card.abandoned"),
        @JsonSubTypes.Type(value = CardRestoredEvent.class, name = "card.restored"),
        @JsonSubTypes.Type(value = CardLinkUpdatedEvent.class, name = "card.link.updated")
})
public abstract class CardEvent extends DomainEvent {

    /** 卡片ID */
    private final String cardId;

    /** 卡片类型ID */
    private final String cardTypeId;

    /** 操作来源（用户操作、业务规则、API调用等） */
    private OperationSource operationSource;

    protected CardEvent(String orgId, String operatorId, String sourceIp, String traceId, String cardId, String cardTypeId) {
        super(orgId, operatorId, sourceIp, traceId);
        this.cardId = cardId;
        this.cardTypeId = cardTypeId;
        this.operationSource = UserOperationSource.INSTANCE;
    }

    /**
     * 设置操作来源并返回当前事件（便于链式调用）
     */
    @SuppressWarnings("unchecked")
    public <T extends CardEvent> T withOperationSource(OperationSource source) {
        this.operationSource = source != null ? source : UserOperationSource.INSTANCE;
        return (T) this;
    }

    /**
     * 获取操作来源，默认为用户操作
     */
    public OperationSource getOperationSource() {
        return operationSource != null ? operationSource : UserOperationSource.INSTANCE;
    }

    private static final String TOPIC = "kanban-card-events";

    @Override
    public String getPartitionKey() {
        return "Card.Event";
    }

    @Override
    public String getTopic() {
        return TOPIC;
    }

}
