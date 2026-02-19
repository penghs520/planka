package dev.planka.event.card;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 卡片关联更新事件
 * <p>
 * 记录卡片关联关系的变更（添加、删除关联卡片）
 */
@Getter
@Setter
public class CardLinkUpdatedEvent extends CardEvent {

    private static final String EVENT_TYPE = "card.link.updated";

    /**
     * 关联属性ID（格式：{linkTypeId}:{SOURCE|TARGET}）
     */
    private String linkFieldId;

    /**
     * 关联属性名称（备份，用于 Schema 被删除后显示）
     */
    private String linkFieldName;

    /**
     * 是否为主动关联方
     * <p>
     * true: 当前卡片是主动发起关联操作的一方
     * false: 当前卡片是被动关联的一方（对端卡片）
     */
    private boolean initiator;

    /**
     * 新增的关联卡片
     */
    private List<LinkedCardRef> addedCards;

    /**
     * 删除的关联卡片
     */
    private List<LinkedCardRef> removedCards;

    @JsonCreator
    public CardLinkUpdatedEvent(@JsonProperty("orgId") String orgId,
                                @JsonProperty("operatorId") String operatorId,
                                @JsonProperty("sourceIp") String sourceIp,
                                @JsonProperty("traceId") String traceId,
                                @JsonProperty("cardId") String cardId,
                                @JsonProperty("cardTypeId") String cardTypeId) {
        super(orgId, operatorId, sourceIp, traceId, cardId, cardTypeId);
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    /**
     * 设置关联属性信息
     *
     * @param linkFieldId   关联属性ID
     * @param linkFieldName 关联属性名称
     * @param initiator     是否为主动关联方
     */
    public CardLinkUpdatedEvent withLinkField(String linkFieldId, String linkFieldName, boolean initiator) {
        this.linkFieldId = linkFieldId;
        this.linkFieldName = linkFieldName;
        this.initiator = initiator;
        return this;
    }

    /**
     * 设置新增的关联卡片
     */
    public CardLinkUpdatedEvent withAddedCards(List<LinkedCardRef> cards) {
        this.addedCards = cards;
        return this;
    }

    /**
     * 设置删除的关联卡片
     */
    public CardLinkUpdatedEvent withRemovedCards(List<LinkedCardRef> cards) {
        this.removedCards = cards;
        return this;
    }

    /**
     * 判断是否有变更
     */
    public boolean hasChanges() {
        return (addedCards != null && !addedCards.isEmpty()) ||
                (removedCards != null && !removedCards.isEmpty());
    }

    /**
     * 关联卡片引用
     */
    @Getter
    public static class LinkedCardRef {
        /**
         * 卡片ID
         */
        private final String cardId;

        /**
         * 卡片标题（备份，用于卡片被删除后显示）
         */
        private final String cardTitle;

        /**
         * 卡片类型ID
         */
        private final String cardTypeId;

        @JsonCreator
        public LinkedCardRef(@JsonProperty("cardId") String cardId,
                             @JsonProperty("cardTitle") String cardTitle,
                             @JsonProperty("cardTypeId") String cardTypeId) {
            this.cardId = cardId;
            this.cardTitle = cardTitle;
            this.cardTypeId = cardTypeId;
        }
    }
}
