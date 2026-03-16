package cn.planka.card.converter;

import cn.planka.api.card.request.QueryScope;
import cn.planka.domain.card.CardCycle;
import zgraph.driver.proto.query.QueryScope.Builder;

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
    public static zgraph.driver.proto.query.QueryScope toProto(QueryScope queryScope) {
        if (queryScope == null) {
            return zgraph.driver.proto.query.QueryScope.getDefaultInstance();
        }

        Builder builder = zgraph.driver.proto.query.QueryScope.newBuilder();

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
        if (queryScope.getCardCycles() != null) {
            List<zgraph.driver.proto.common.CardState> protoStates = queryScope.getCardCycles().stream()
                    .map(QueryScopeConverter::toProtoCardState)
                    .toList();
            builder.addAllStates(protoStates);
        }

        return builder.build();
    }

    /**
     * 转换卡片状态
     */
    public static zgraph.driver.proto.common.CardState toProtoCardState(CardCycle state) {
        return switch (state) {
            case ACTIVE -> zgraph.driver.proto.common.CardState.Active;
            case DISCARDED -> zgraph.driver.proto.common.CardState.Discarded;
            case ARCHIVED -> zgraph.driver.proto.common.CardState.Archived;
        };
    }

    /**
     * 从 protobuf 卡片状态转换为域卡片状态
     */
    public static CardCycle fromProtoCardState(zgraph.driver.proto.common.CardState protoState) {
        return switch (protoState) {
            case Active -> CardCycle.ACTIVE;
            case Discarded -> CardCycle.DISCARDED;
            case Archived -> CardCycle.ARCHIVED;
            case UNRECOGNIZED -> CardCycle.ACTIVE; // 默认值
        };
    }
}
