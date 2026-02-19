package dev.planka.api.card.request;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.card.CardId;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StreamId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 批量移动卡片请求
 * <p>
 * 用于批量在价值流中移动卡片状态
 *
 * @param cardIds    卡片ID列表
 * @param streamId   价值流ID
 * @param toStatusId 目标状态ID
 */
public record BatchMoveCardRequest(
        List<CardId> cardIds,
        StreamId streamId,
        StatusId toStatusId
) {
    @JsonCreator
    public BatchMoveCardRequest(
            @JsonProperty("cardIds") List<CardId> cardIds,
            @JsonProperty("streamId") StreamId streamId,
            @JsonProperty("toStatusId") StatusId toStatusId
    ) {
        AssertUtils.notEmpty(cardIds, "cardIds can't be empty");
        AssertUtils.notNull(streamId, "streamId can't be null");
        AssertUtils.notNull(toStatusId, "toStatusId can't be null");
        this.cardIds = cardIds;
        this.streamId = streamId;
        this.toStatusId = toStatusId;
    }
}
