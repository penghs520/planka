package dev.planka.event.card;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 卡片丢弃事件
 */
@Getter
public class CardAbandonedEvent extends CardEvent {

    private static final String EVENT_TYPE = "card.abandoned";

    /**
     * 丢弃原因
     */
    @Setter
    private String reason;

    @JsonCreator
    public CardAbandonedEvent(@JsonProperty("orgId") String orgId,
                              @JsonProperty("operatorId") String operatorId,
                              @JsonProperty("sourceIp") String sourceIp,
                              @JsonProperty("traceId") String traceId,
                              @JsonProperty("cardTypeId") String cardTypeId,
                              @JsonProperty("cardId") String cardId,
                              @JsonProperty("reason") String reason) {
        super(orgId, operatorId, sourceIp, traceId, cardId, cardTypeId);
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
