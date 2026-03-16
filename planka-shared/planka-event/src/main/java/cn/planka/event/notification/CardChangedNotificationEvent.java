package cn.planka.event.notification;

import cn.planka.common.util.AssertUtils;
import cn.planka.domain.card.CardId;
import cn.planka.domain.notification.NotificationTemplateId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CardChangedNotificationEvent extends NotificationEvent {

    private final NotificationTemplateId notificationTemplateId;
    private final CardId currentCardId;

    @JsonCreator
    public CardChangedNotificationEvent(@JsonProperty("orgId") String orgId,
                                        @JsonProperty("operatorId") String operatorId,
                                        @JsonProperty("sourceIp") String sourceIp,
                                        @JsonProperty("traceId") String traceId,
                                        @JsonProperty("notificationTemplateId") NotificationTemplateId notificationTemplateId,
                                        @JsonProperty("currentCardId") CardId currentCardId) {
        super(orgId, operatorId, sourceIp, traceId);
        this.notificationTemplateId = AssertUtils.requireNotNull(notificationTemplateId, "notificationTemplateId can't be null");
        this.currentCardId = AssertUtils.requireNotNull(currentCardId, "currentCardId can't be null");;
    }
}
