package dev.planka.card.converter;

import dev.planka.api.card.request.QueryScope;
import dev.planka.domain.card.CardStyle;
import planka.graph.driver.proto.query.QueryScope.Builder;

import java.util.List;

/**
 * 查询范围转换器
 * 将 card-api 的 QueryScope 转换为 zgraph protobuf 的 QueryScope
 */
public class QueryScopeConverter {

    private QueryScopeConverter() {
    }

    /**
     * 转换为 protobuf QueryScope
     */
    public static planka.graph.driver.proto.query.QueryScope toProto(QueryScope queryScope) {
        if (queryScope == null) {
            return planka.graph.driver.proto.query.QueryScope.getDefaultInstance();
        }

        Builder builder = planka.graph.driver.proto.query.QueryScope.newBuilder();

        if (queryScope.getCardTypeIds() != null) {
            builder.addAllCardTypeIds(queryScope.getCardTypeIds());
        }
        if (queryScope.getCardIds() != null) {
            // 将 String 类型的 cardId 转换为 u64
            List<Long> cardIds = queryScope.getCardIds().stream()
                    .map(Long::parseLong)
                    .toList();
            builder.addAllCardIds(cardIds);
        }
        if (queryScope.getCardStyles() != null) {
            List<planka.graph.driver.proto.common.CardState> protoStates = queryScope.getCardStyles().stream()
                    .map(QueryScopeConverter::toProtoCardState)
                    .toList();
            builder.addAllStates(protoStates);
        }

        return builder.build();
    }

    /**
     * 转换卡片状态
     */
    public static planka.graph.driver.proto.common.CardState toProtoCardState(CardStyle state) {
        return switch (state) {
            case ACTIVE -> planka.graph.driver.proto.common.CardState.Active;
            case DISCARDED -> planka.graph.driver.proto.common.CardState.Discarded;
            case ARCHIVED -> planka.graph.driver.proto.common.CardState.Archived;
        };
    }

    /**
     * 从 protobuf 卡片状态转换为域卡片状态
     */
    public static CardStyle fromProtoCardState(planka.graph.driver.proto.common.CardState protoState) {
        return switch (protoState) {
            case Active -> CardStyle.ACTIVE;
            case Discarded -> CardStyle.DISCARDED;
            case Archived -> CardStyle.ARCHIVED;
            case UNRECOGNIZED -> CardStyle.ACTIVE; // 默认值
        };
    }
}
