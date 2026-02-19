package dev.planka.card.converter;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.CreateCardRequest;
import dev.planka.card.model.CardEntity;
import dev.planka.domain.card.CardDescription;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.card.OrgId;
import dev.planka.domain.link.LinkFieldIdUtils;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StreamId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import planka.graph.driver.proto.model.Card;
import planka.graph.driver.proto.model.CardList;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 卡片转换器
 * 双向转换 domain CardDTO 和 proto Card
 */
public class CardProtoConverter {

    private static final Logger logger = LoggerFactory.getLogger(CardProtoConverter.class);

    private CardProtoConverter() {
    }

    // ==================== Proto -> Domain ====================

    /**
     * 将 proto Card 转换为 CardDTO
     */
    public static CardDTO toCardDTO(Card protoCard) {
        if (protoCard == null) {
            return null;
        }

        CardDTO dto = new CardDTO();

        // 标识信息
        dto.setId(CardId.of(String.valueOf(protoCard.getId())));
        dto.setCodeInOrg(protoCard.getCodeInOrg());
        dto.setCustomCode(protoCard.getCustomCode().isEmpty() ? null : protoCard.getCustomCode());
        dto.setOrgId(OrgId.of(protoCard.getOrgId()));
        dto.setTypeId(CardTypeId.of(protoCard.getTypeId()));

        // 基本信息
        dto.setTitle(TitleConverter.fromProto(protoCard.getTitle()));
        dto.setDescription(fromProtoDescription(protoCard.getDescription()));

        // 状态信息
        dto.setCardStyle(QueryScopeConverter.fromProtoCardState(protoCard.getState()));
        if (!protoCard.getStreamId().isEmpty()) {
            dto.setStreamId(StreamId.of(protoCard.getStreamId()));
        }
        if (!protoCard.getStatusId().isEmpty()) {
            dto.setStatusId(StatusId.of(protoCard.getStatusId()));
        }

        // 属性值
        dto.setFieldValues(FieldValueConverter.fromProtoMap(protoCard.getCustomFieldValueMapMap()));

        // 关联卡片
        dto.setLinkedCards(toLinkedCardsMap(protoCard.getLinkCardMapMap()));

        // 审计信息
        dto.setCreatedAt(toLocalDateTime(protoCard.getCreatedAt()));
        dto.setUpdatedAt(toLocalDateTime(protoCard.getUpdatedAt()));
        dto.setAbandonedAt(protoCard.getDiscardedAt() > 0 ? toLocalDateTime(protoCard.getDiscardedAt()) : null);
        dto.setArchivedAt(protoCard.getArchivedAt() > 0 ? toLocalDateTime(protoCard.getArchivedAt()) : null);

        return dto;
    }

    /**
     * 将 proto CardList 转换为 CardDTO 列表
     */
    public static List<CardDTO> toCardDTOList(List<Card> protoCards) {
        if (protoCards == null) {
            return new ArrayList<>();
        }
        return protoCards.stream()
                .map(CardProtoConverter::toCardDTO)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 转换关联卡片 Map
     * <p>
     * proto 的 key 格式为 "ltId:Src" 或 "ltId:Dest"，
     * 转换后的 key 格式为 "ltId:SOURCE" 或 "ltId:TARGET"
     */
    private static Map<String, Set<CardDTO>> toLinkedCardsMap(Map<String, CardList> linkCardMap) {
        if (linkCardMap == null || linkCardMap.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Set<CardDTO>> result = new HashMap<>();
        for (Map.Entry<String, CardList> entry : linkCardMap.entrySet()) {
            // key 格式: "ltId:Src" 或 "ltId:Dest"
            String linkFieldId = parseLinkFieldId(entry.getKey());
            if (linkFieldId != null) {
                Set<CardDTO> cards = new HashSet<>();
                for (Card card : entry.getValue().getCardsList()) {
                    CardDTO cardDTO = toCardDTO(card);
                    if (cardDTO != null) {
                        cards.add(cardDTO);
                    }
                }
                result.put(linkFieldId, cards);
            }
        }
        return result;
    }

    /**
     * 解析 linkFieldId 从 proto 格式的字符串 key
     * <p>
     * proto 格式: "ltId:Src" 或 "ltId:Dest"
     * 返回格式: "ltId:SOURCE" 或 "ltId:TARGET"
     */
    private static String parseLinkFieldId(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        // proto key 格式为 "ltId:Src" 或 "ltId:Dest"
        int colonIdx = key.lastIndexOf(':');
        if (colonIdx > 0 && colonIdx < key.length() - 1) {
            String linkTypeId = key.substring(0, colonIdx);
            String positionStr = key.substring(colonIdx + 1);
            LinkPosition position = PathConverter.fromProtoLinkPositionString(positionStr);
            return LinkFieldIdUtils.build(linkTypeId, position);
        }
        // 如果解析失败，尝试作为 linkFieldId 直接返回
        if (LinkFieldIdUtils.isValidFormat(key)) {
            return key;
        }
        // 默认 SOURCE
        return LinkFieldIdUtils.build(key, LinkPosition.SOURCE);
    }

    /**
     * 时间戳转 LocalDateTime
     */
    private static LocalDateTime toLocalDateTime(long timestamp) {
        if (timestamp <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    // ==================== Domain -> Proto ====================

    /**
     * 将 CreateCardRequest 转换为 proto Card
     */
    public static Card toProtoCard(CreateCardRequest request, long cardId, long codeInOrg, String operatorId) {
        if (request == null) {
            return null;
        }

        Card.Builder builder = Card.newBuilder();

        builder.setId(cardId);
        builder.setCodeInOrg(codeInOrg);
        builder.setOrgId(request.orgId().value());
        builder.setTypeId(request.typeId().value());
        builder.setTitle(TitleConverter.toProto(request.title()));

        if (request.description() != null) {
            builder.setDescription(request.description());
        }

        // 默认状态为 Active
        builder.setState(planka.graph.driver.proto.common.CardState.Active);

        // 属性值
        if (request.fieldValues() != null) {
            builder.putAllCustomFieldValueMap(FieldValueConverter.toProtoMap(request.fieldValues()));
        }

        // 审计信息
        long now = System.currentTimeMillis();
        builder.setCreatedAt(now);
        builder.setUpdatedAt(now);

        return builder.build();
    }

    /**
     * 将 UpdateCardRequest 转换为 proto Card
     */
    //TODO 缺少丢弃时间等字段
    public static Card toProtoCard(CardEntity cardEntity) {
        if (cardEntity == null) {
            return null;
        }

        Card.Builder builder = Card.newBuilder();

        builder.setId(Long.parseLong(cardEntity.getId().value()));
        builder.setCodeInOrg(cardEntity.getCodeInOrg());
        if (cardEntity.getCustomCode() != null) {
            builder.setCustomCode(cardEntity.getCustomCode());
        }
        builder.setOrgId(cardEntity.getOrgId().value());
        builder.setTypeId(cardEntity.getTypeId().value());
        builder.setState(QueryScopeConverter.toProtoCardState(cardEntity.getCardStyle()));
        builder.setTitle(TitleConverter.toProto(cardEntity.getTitle()));

        if (cardEntity.getDescription() != null) {
            builder.setDescription(cardEntity.getDescription());
        }

        if (cardEntity.getStreamId() != null) {
            builder.setStreamId(cardEntity.getStreamId().value());
        }

        if (cardEntity.getStatusId() != null) {
            builder.setStatusId(cardEntity.getStatusId().value());
        }

        // 属性值
        if (cardEntity.getFieldValues() != null) {
            builder.putAllCustomFieldValueMap(FieldValueConverter.toProtoMap(cardEntity.getFieldValues()));
        }

        // 审计信息
        if (cardEntity.getCreatedAt() != null) {
            builder.setCreatedAt(cardEntity.getCreatedAt());
        }
        if (cardEntity.getUpdatedAt() != null) {
            builder.setUpdatedAt(cardEntity.getUpdatedAt());
        }

        return builder.build();
    }

    /**
     * 从 proto Description 转换为 CardDescription
     */
    private static CardDescription fromProtoDescription(String protoDescription) {
        if (protoDescription == null || protoDescription.isEmpty()) {
            return null;
        }
        return CardDescription.of(protoDescription);
    }

    /**
     * LocalDateTime 转时间戳
     */
    private static long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
