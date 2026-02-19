package dev.planka.api.card.request;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.card.CardId;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StreamId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 移动卡片请求
 * <p>
 * 用于在价值流中移动卡片状态
 *
 * @param cardId     卡片ID
 * @param streamId   价值流ID
 * @param toStatusId 目标状态ID
 */
public record MoveCardRequest(
        CardId cardId,
        StreamId streamId,
        StatusId toStatusId
) {
    @JsonCreator
    public MoveCardRequest(
            @JsonProperty("cardId") CardId cardId,
            @JsonProperty("streamId") StreamId streamId,
            @JsonProperty("toStatusId") StatusId toStatusId
    ) {
        AssertUtils.notNull(cardId, "cardId can't be null");
        AssertUtils.notNull(streamId, "streamId can't be null");
        AssertUtils.notNull(toStatusId, "toStatusId can't be null");
        this.cardId = cardId;
        this.streamId = streamId;
        this.toStatusId = toStatusId;
    }
}
