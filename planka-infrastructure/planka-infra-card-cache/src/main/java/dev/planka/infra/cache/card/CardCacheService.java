package dev.planka.infra.cache.card;

import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTitle;
import dev.planka.infra.cache.card.model.CardBasicInfo;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 卡片缓存服务
 * <p>
 * 提供卡片基础信息的二级缓存（L1: Caffeine, L2: Redis）
 */
public interface CardCacheService {

    /**
     * 根据卡片ID获取基础信息
     *
     * @param cardId 卡片ID
     * @return 卡片基础信息，不存在返回 Optional.empty()
     */
    Optional<CardBasicInfo> getBasicInfoById(CardId cardId);

    /**
     * 批量获取卡片基础信息
     *
     * @param cardIds 卡片ID集合
     * @return 卡片ID -> 基础信息的映射（不存在的卡片不在结果中）
     */
    Map<CardId, CardBasicInfo> getBasicInfoByIds(Set<CardId> cardIds);

    /**
     * 清除 L1 缓存（本地 Caffeine）
     */
    void evictL1(CardId cardId);

    /**
     * 清除 L2 缓存（Redis）
     */
    void evictL2(CardId cardId);

    /**
     * 清除 L1 + L2 缓存
     */
    void evict(CardId cardId);

    /**
     * 批量清除缓存
     */
    void evictAll(Set<CardId> cardIds);

    Map<CardId, CardTitle> queryCardNames(Set<CardId> cardIds);

}
