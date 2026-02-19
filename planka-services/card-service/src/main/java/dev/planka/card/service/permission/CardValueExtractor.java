package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.card.CardId;
import dev.planka.domain.field.FieldValue;
import dev.planka.domain.link.Path;
import dev.planka.domain.schema.definition.condition.DateConditionItem;
import dev.planka.domain.schema.definition.condition.ReferenceSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 卡片值提取器
 * <p>
 * 负责从卡片中提取字段值，支持：
 * <ul>
 *     <li>直接字段值提取</li>
 *     <li>通过 Path 多级关联提取</li>
 *     <li>系统字段提取（创建时间、更新时间等）</li>
 *     <li>ReferenceValue 引用值提取</li>
 * </ul>
 */
@Slf4j
@Component
public class CardValueExtractor {

    /**
     * 根据路径获取目标卡片
     *
     * @param card 起始卡片
     * @param path 路径（可能为 null）
     * @return 目标卡片，如果路径为 null 返回起始卡片，如果路径无效返回 null
     */
    public CardDTO getCardByPath(CardDTO card, Path path) {
        if (card == null) {
            return null;
        }

        if (path == null || path.linkNodes() == null || path.linkNodes().isEmpty()) {
            return card;
        }

        CardDTO current = card;
        for (String linkFieldId : path.linkNodes()) {
            current = getLinkedCard(current, linkFieldId);
            if (current == null) {
                return null;
            }
        }

        return current;
    }

    /**
     * 获取关联卡片（取第一个）
     *
     * @param card        卡片
     * @param linkFieldId 关联字段ID
     * @return 关联的第一个卡片，如果没有关联返回 null
     */
    private CardDTO getLinkedCard(CardDTO card, String linkFieldId) {
        if (card == null || card.getLinkedCards() == null) {
            return null;
        }

        Set<CardDTO> linkedCards = card.getLinkedCards().get(linkFieldId);
        if (linkedCards == null || linkedCards.isEmpty()) {
            return null;
        }

        return linkedCards.iterator().next();
    }

    /**
     * 获取字段值
     *
     * @param card    卡片
     * @param fieldId 字段ID
     * @return 字段值，如果不存在返回 null
     */
    public FieldValue<?> getFieldValue(CardDTO card, String fieldId) {
        if (card == null || card.getFieldValues() == null) {
            return null;
        }
        return card.getFieldValues().get(fieldId);
    }

    /**
     * 获取系统日期字段值
     *
     * @param card        卡片
     * @param systemField 系统字段类型
     * @return 日期值，如果不存在返回 null
     */
    public LocalDateTime getSystemDateValue(CardDTO card, DateConditionItem.SystemDateField systemField) {
        if (card == null || systemField == null) {
            return null;
        }

        return switch (systemField) {
            case CREATED_AT -> card.getCreatedAt();
            case UPDATED_AT -> card.getUpdatedAt();
            case DISCARDED_AT -> card.getAbandonedAt();
            case ARCHIVED_AT -> card.getArchivedAt();
        };
    }

    /**
     * 获取关联卡片集合
     *
     * @param card        卡片
     * @param linkFieldId 关联字段ID
     * @return 关联卡片集合，如果不存在返回 null
     */
    public Set<CardDTO> getLinkedCards(CardDTO card, String linkFieldId) {
        if (card == null || card.getLinkedCards() == null) {
            return null;
        }
        return card.getLinkedCards().get(linkFieldId);
    }

    /**
     * 根据引用源获取卡片
     *
     * @param source      引用源
     * @param targetCard  目标卡片
     * @param memberCard  成员卡片
     * @return 引用的卡片，如果无法解析返回 null
     */
    public CardDTO getCardByReferenceSource(ReferenceSource source, CardDTO targetCard, CardDTO memberCard) {
        if (source == null) {
            return null;
        }

        if (source instanceof ReferenceSource.CurrentCard currentCard) {
            return getCardByPath(targetCard, currentCard.getPath());
        }

        if (source instanceof ReferenceSource.Member member) {
            return getCardByPath(memberCard, member.getPath());
        }

        // ParameterCard 和 ContextualCard 暂不支持
        log.warn("不支持的引用源类型: {}", source.getClass().getName());
        return null;
    }

    /**
     * 提取卡片ID（用于系统用户条件）
     *
     * @param card 卡片
     * @return 卡片ID，如果卡片为 null 返回 null
     */
    public CardId getCardId(CardDTO card) {
        return card != null ? card.getId() : null;
    }
}
