package dev.planka.event.card;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 卡片移动事件（状态流转）
 */
@Getter
@Setter
public class CardMovedEvent extends CardEvent {

    private static final String EVENT_TYPE = "card.moved";

    /**
     * 价值流ID
     */
    private String streamId;

    /**
     * 原阶段ID
     */
    private String fromStepId;

    /**
     * 原状态ID
     */
    private String fromStatusId;

    /**
     * 原状态工作类型
     */
    private String fromStatusWorkType;

    /**
     * 目标阶段ID
     */
    private String toStepId;

    /**
     * 目标状态ID
     */
    private String toStatusId;

    /**
     * 目标状态工作类型
     */
    private String toStatusWorkType;

    /**
     * 是否为回滚操作
     */
    private boolean rollback;

    /**
     * 原状态名称
     */
    private String fromStatusName;

    /**
     * 目标状态名称
     */
    private String toStatusName;

    @JsonCreator
    public CardMovedEvent(@JsonProperty("orgId") String orgId,
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

    /**
     * 构建器方法：设置状态变更信息（完整版）
     */
    public CardMovedEvent withStatusChange(String fromStepId, String fromStatusId, String fromStatusName, String fromStatusWorkType,
                                           String toStepId, String toStatusId, String toStatusName, String toStatusWorkType,
                                           boolean rollback) {
        this.fromStepId = fromStepId;
        this.fromStatusId = fromStatusId;
        this.fromStatusName = fromStatusName;
        this.fromStatusWorkType = fromStatusWorkType;
        this.toStepId = toStepId;
        this.toStatusId = toStatusId;
        this.toStatusName = toStatusName;
        this.toStatusWorkType = toStatusWorkType;
        this.rollback = rollback;
        return this;
    }
}
