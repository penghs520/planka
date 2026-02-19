package dev.planka.event.card;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 卡片还原事件
 */
@Getter
public class CardRestoredEvent extends CardEvent {

    private static final String EVENT_TYPE = "card.restored";

    @JsonCreator
    public CardRestoredEvent(@JsonProperty("orgId") String orgId,
                             @JsonProperty("operatorId") String operatorId,
                             @JsonProperty("sourceIp") String sourceIp,
                             @JsonProperty("traceId") String traceId,
                             @JsonProperty("cardTypeId") String cardTypeId,
                             @JsonProperty("cardId") String cardId) {
        super(orgId, operatorId, sourceIp, traceId, cardId, cardTypeId);
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
