package dev.planka.infra.cache.card;

import dev.planka.domain.card.CardId;
import dev.planka.infra.cache.card.model.CardBasicInfo;

import java.util.Map;
import java.util.Set;

/**
 * 卡片基础信息加载器
 * <p>
 * 由使用方提供实现，用于从数据源加载卡片基础信息
 */
@FunctionalInterface
public interface CardBasicInfoLoader {

    /**
     * 批量加载卡片基础信息
     *
     * @param cardIds 卡片ID集合
     * @return 卡片ID -> 基础信息的映射
     */
    Map<CardId, CardBasicInfo> load(Set<CardId> cardIds);
}
