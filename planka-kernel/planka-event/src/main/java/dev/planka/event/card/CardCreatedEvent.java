package dev.planka.event.card;

import dev.planka.domain.card.CardStyle;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.field.FieldValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 卡片创建事件
 * <p>
 * 包含完整的卡片创建信息，供多个消费者使用（如流动记录服务、操作历史服务等）
 */
@Getter
@Setter
public class CardCreatedEvent extends CardEvent {

    private static final String EVENT_TYPE = "card.created";

    /**
     * 卡片内置编号
     */
    private long codeInOrg;

    /**
     * 卡片自定义编号
     */
    private String customCode;

    /**
     * 卡片标题
     */
    private CardTitle title;

    /**
     * 卡片描述
     */
    private String description;

    /**
     * 生命周期状态
     */
    private CardStyle cardStyle;

    /**
     * 价值流ID
     */
    private String streamId;

    /**
     * 价值流状态ID
     */
    private String statusId;


    /**
     * 自定义属性值
     */
    private Map<String, FieldValue<?>> fieldValues;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createdAt;

    @JsonCreator
    public CardCreatedEvent(@JsonProperty("orgId") String orgId,
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
