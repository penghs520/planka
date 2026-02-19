package dev.planka.event.card;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 卡片归档事件
 */
@Getter
public class CardArchivedEvent extends CardEvent {

    private static final String EVENT_TYPE = "card.archived";

    @JsonCreator
    public CardArchivedEvent(@JsonProperty("orgId") String orgId,
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
