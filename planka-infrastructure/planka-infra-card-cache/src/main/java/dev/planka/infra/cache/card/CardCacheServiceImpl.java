package dev.planka.infra.cache.card;

import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTitle;
import dev.planka.infra.cache.card.model.CardBasicInfo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 卡片缓存服务实现
 * <p>
 * 二级缓存策略：
 * - L1: Caffeine 本地缓存（快速访问）
 * - L2: Redis 分布式缓存（跨实例共享）
 * <p>
 * 注意：此类不使用 @Service 注解，由使用方通过配置类手动创建 Bean，
 * 以便注入正确的 CardBasicInfoLoader 实现。
 */
@Slf4j
public class CardCacheServiceImpl implements CardCacheService {

    private final Cache<CardId, CardBasicInfo> l1Cache;
    private final RedisTemplate<String, CardBasicInfo> redisTemplate;
    private final CardBasicInfoLoader loader;
    private final CardCacheProperties properties;

    public CardCacheServiceImpl(
            CardBasicInfoLoader loader,
            RedisTemplate<String, CardBasicInfo> redisTemplate,
            CardCacheProperties properties) {
        this.loader = loader;
        this.redisTemplate = redisTemplate;
        this.properties = properties;

        // 初始化 L1 缓存
        this.l1Cache = Caffeine.newBuilder()
            .maximumSize(properties.getL1().getMaxSize())
            .expireAfterWrite(properties.getL1().getExpireAfterWrite())
            .recordStats()
            .build();

        log.info("CardCacheService 初始化完成，L1 maxSize={}, expireAfterWrite={}",
            properties.getL1().getMaxSize(),
            properties.getL1().getExpireAfterWrite());
    }

    @Override
    public Optional<CardBasicInfo> getBasicInfoById(CardId cardId) {
        Map<CardId, CardBasicInfo> result = getBasicInfoByIds(Set.of(cardId));
        return Optional.ofNullable(result.get(cardId));
    }

    @Override
    public Map<CardId, CardBasicInfo> getBasicInfoByIds(Set<CardId> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return Map.of();
        }

        Map<CardId, CardBasicInfo> result = new HashMap<>();
        Set<CardId> missingInL1 = new HashSet<>();

        // 1. 查询 L1 缓存
        for (CardId cardId : cardIds) {
            CardBasicInfo cached = l1Cache.getIfPresent(cardId);
            if (cached != null) {
                result.put(cardId, cached);
            } else {
                missingInL1.add(cardId);
            }
        }

        if (missingInL1.isEmpty()) {
            log.debug("L1 缓存全部命中，cardIds={}", cardIds);
            return result;
        }

        log.debug("L1 缓存部分未命中，missingInL1={}", missingInL1);

        // 2. 查询 L2 缓存（Redis）
        Set<CardId> missingInL2 = new HashSet<>();
        Map<CardId, CardBasicInfo> fromL2 = queryFromRedis(missingInL1);

        for (CardId cardId : missingInL1) {
            CardBasicInfo info = fromL2.get(cardId);
            if (info != null) {
                result.put(cardId, info);
                l1Cache.put(cardId, info); // 回填 L1
            } else {
                missingInL2.add(cardId);
            }
        }

        if (missingInL2.isEmpty()) {
            log.debug("L2 缓存全部命中，missingInL1={}", missingInL1);
            return result;
        }

        log.debug("L2 缓存部分未命中，missingInL2={}", missingInL2);

        // 3. 从数据源加载
        Map<CardId, CardBasicInfo> fromLoader = loader.load(missingInL2);

        for (Map.Entry<CardId, CardBasicInfo> entry : fromLoader.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
            l1Cache.put(entry.getKey(), entry.getValue()); // 回填 L1
            putToRedis(entry.getKey(), entry.getValue());   // 回填 L2
        }

        log.debug("从数据源加载完成，loaded={}", fromLoader.keySet());

        return result;
    }

    @Override
    public void evictL1(CardId cardId) {
        l1Cache.invalidate(cardId);
        log.debug("清除 L1 缓存，cardId={}", cardId);
    }

    @Override
    public void evictL2(CardId cardId) {
        String key = buildRedisKey(cardId);
        redisTemplate.delete(key);
        log.debug("清除 L2 缓存，cardId={}", cardId);
    }

    @Override
    public void evict(CardId cardId) {
        evictL1(cardId);
        evictL2(cardId);
        log.debug("清除 L1+L2 缓存，cardId={}", cardId);
    }

    @Override
    public void evictAll(Set<CardId> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return;
        }

        // 清除 L1
        l1Cache.invalidateAll(cardIds);

        // 清除 L2
        List<String> keys = cardIds.stream()
            .map(this::buildRedisKey)
            .collect(Collectors.toList());
        redisTemplate.delete(keys);

        log.debug("批量清除缓存，cardIds={}", cardIds);
    }

    @Override
    public Map<CardId, CardTitle> queryCardNames(Set<CardId> cardIds) {
        if (CollectionUtils.isEmpty(cardIds)){
            return Map.of();
        }
        Map<CardId, CardBasicInfo> basicInfoByIds = getBasicInfoByIds(cardIds);
        Map<CardId, CardTitle> result = new HashMap<>(basicInfoByIds.size());
        basicInfoByIds.forEach((cardId,basicInfo)-> result.put(cardId,basicInfo.title()));
        return result;
    }

    /**
     * 从 Redis 批量查询
     */
    private Map<CardId, CardBasicInfo> queryFromRedis(Set<CardId> cardIds) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }

        List<String> keys = cardIds.stream()
            .map(this::buildRedisKey)
            .collect(Collectors.toList());

        List<CardBasicInfo> values = redisTemplate.opsForValue().multiGet(keys);

        if (values == null || values.isEmpty()) {
            return Map.of();
        }

        Map<CardId, CardBasicInfo> result = new HashMap<>();
        Iterator<CardId> cardIdIterator = cardIds.iterator();
        Iterator<CardBasicInfo> valueIterator = values.iterator();

        while (cardIdIterator.hasNext() && valueIterator.hasNext()) {
            CardId cardId = cardIdIterator.next();
            CardBasicInfo info = valueIterator.next();
            if (info != null) {
                result.put(cardId, info);
            }
        }

        return result;
    }

    /**
     * 写入 Redis
     */
    private void putToRedis(CardId cardId, CardBasicInfo info) {
        String key = buildRedisKey(cardId);
        redisTemplate.opsForValue().set(
            key,
            info,
            properties.getL2().getExpireAfterWrite().toMillis(),
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * 构建 Redis Key
     */
    private String buildRedisKey(CardId cardId) {
        return properties.getL2().getKeyPrefix() + cardId.value();
    }
}
