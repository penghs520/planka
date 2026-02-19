package dev.planka.infra.cache.schema;

import dev.planka.api.schema.SchemaServiceClient;
import dev.planka.common.result.Result;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Schema 缓存服务实现
 * <p>
 * 二级缓存策略：
 * - L1: Caffeine 本地缓存（默认 5 分钟过期，10000 条上限）
 * - L2: Redis 分布式缓存（默认 30 分钟过期）
 * <p>
 * 查询流程：L1 命中 → 返回；L1 未命中 → L2 查询 → L2 命中回填 L1；L2 未命中 → Feign 调用回填 L1+L2
 */
@Slf4j
@Service
public class SchemaCacheServiceImpl implements SchemaCacheService {

    private final Cache<String, SchemaDefinition<?>> l1Cache;
    private final RedisTemplate<String, SchemaDefinition<?>> redisTemplate;
    private final SchemaServiceClient schemaServiceClient;
    private final SchemaCacheProperties properties;
    private final SecondaryIndexCache secondaryIndexCache;

    public SchemaCacheServiceImpl(
            RedisTemplate<String, SchemaDefinition<?>> redisTemplate,
            SchemaServiceClient schemaServiceClient,
            SchemaCacheProperties properties,
            SecondaryIndexCache secondaryIndexCache) {

        this.redisTemplate = redisTemplate;
        this.schemaServiceClient = schemaServiceClient;
        this.properties = properties;
        this.secondaryIndexCache = secondaryIndexCache;

        // 构建 L1 Caffeine 缓存
        this.l1Cache = Caffeine.newBuilder()
                .maximumSize(properties.getL1().getMaxSize())
                .expireAfterAccess(properties.getL1().getExpireAfterWrite())
                .recordStats()
                .build();

        log.info("Schema cache initialized: L1[maxSize={}, ttl={}], L2[ttl={}]",
                properties.getL1().getMaxSize(),
                properties.getL1().getExpireAfterWrite(),
                properties.getL2().getExpireAfterWrite());
    }

    @Override
    public Optional<SchemaDefinition<?>> getById(String schemaId) {
        if (schemaId == null || schemaId.isBlank()) {
            return Optional.empty();
        }

        // L1 查询
        SchemaDefinition<?> cached = l1Cache.getIfPresent(schemaId);
        if (cached != null) {
            log.debug("L1 cache hit: schemaId={}", schemaId);
            return Optional.of(cached);
        }

        // L2 查询
        String redisKey = getRedisKey(schemaId);
        try {
            cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                log.debug("L2 cache hit: schemaId={}", schemaId);
                l1Cache.put(schemaId, cached);
                return Optional.of(cached);
            }
        } catch (Exception e) {
            log.warn("L2 cache query failed: schemaId={}, error={}", schemaId, e.getMessage());
        }

        // Feign 调用
        try {
            Result<SchemaDefinition<?>> result = schemaServiceClient.getById(schemaId);
            if (result.isSuccess() && result.getData() != null) {
                SchemaDefinition<?> def = result.getData();
                putToCache(schemaId, def);
                log.debug("Loaded from remote: schemaId={}", schemaId);
                return Optional.of(def);
            }
        } catch (Exception e) {
            log.error("Failed to load schema from remote: schemaId={}", schemaId, e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<SchemaDefinition<?>> getById(SchemaId schemaId) {
        return getById(schemaId.value());
    }

    @Override
    public Map<String, SchemaDefinition<?>> getByIds(Set<String> schemaIds) {
        if (schemaIds == null || schemaIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, SchemaDefinition<?>> result = new HashMap<>();
        Set<String> l1MissIds = new HashSet<>();

        // Step 1: L1 批量查询
        for (String id : schemaIds) {
            SchemaDefinition<?> cached = l1Cache.getIfPresent(id);
            if (cached != null) {
                result.put(id, cached);
            } else {
                l1MissIds.add(id);
            }
        }

        if (l1MissIds.isEmpty()) {
            log.debug("L1 cache hit all: count={}", schemaIds.size());
            return result;
        }

        // Step 2: L2 批量查询（Redis MGET）
        Set<String> l2MissIds = new HashSet<>();
        try {
            List<String> l1MissList = new ArrayList<>(l1MissIds);
            List<String> redisKeys = l1MissList.stream()
                    .map(this::getRedisKey)
                    .toList();
            List<SchemaDefinition<?>> l2Results = redisTemplate.opsForValue().multiGet(redisKeys);
            if (l2Results != null) {
                for (int i = 0; i < l1MissList.size(); i++) {
                    String id = l1MissList.get(i);
                    SchemaDefinition<?> def = l2Results.get(i);
                    if (def != null) {
                        result.put(id, def);
                        l1Cache.put(id, def);  // 回填 L1
                    } else {
                        l2MissIds.add(id);
                    }
                }
            } else {
                l2MissIds.addAll(l1MissIds);
            }
        } catch (Exception e) {
            log.warn("L2 cache batch query failed: error={}", e.getMessage());
            l2MissIds.addAll(l1MissIds);
        }

        if (l2MissIds.isEmpty()) {
            log.debug("L2 cache hit remaining: l1Miss={}, l2Hit={}",
                    l1MissIds.size(), l1MissIds.size());
            return result;
        }

        // Step 3: Feign 批量查询
        try {
            Result<List<SchemaDefinition<?>>> feignResult =
                    schemaServiceClient.getByIds(new ArrayList<>(l2MissIds));

            if (feignResult.isSuccess() && feignResult.getData() != null) {
                for (SchemaDefinition<?> def : feignResult.getData()) {
                    String id = def.getId().value();
                    result.put(id, def);
                    putToCache(id, def);
                }
                log.debug("Loaded from remote: count={}", feignResult.getData().size());
            }
        } catch (Exception e) {
            log.error("Failed to batch load schemas from remote: ids={}", l2MissIds, e);
        }

        return result;
    }

    @Override
    public void evictL1(String schemaId) {
        if (schemaId == null || schemaId.isBlank()) {
            return;
        }
        l1Cache.invalidate(schemaId);
        log.debug("L1 cache evicted: schemaId={}", schemaId);
    }

    @Override
    public void evictL2(String schemaId) {
        if (schemaId == null || schemaId.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(getRedisKey(schemaId));
            log.debug("L2 cache evicted: schemaId={}", schemaId);
        } catch (Exception e) {
            log.warn("Failed to evict L2 cache: schemaId={}, error={}", schemaId, e.getMessage());
        }
    }

    @Override
    public void evict(String schemaId) {
        evictL1(schemaId);
        evictL2(schemaId);
    }

    @Override
    public void evictAll(Set<String> schemaIds) {
        if (schemaIds == null || schemaIds.isEmpty()) {
            return;
        }

        l1Cache.invalidateAll(schemaIds);
        try {
            List<String> redisKeys = schemaIds.stream()
                    .map(this::getRedisKey)
                    .toList();
            redisTemplate.delete(redisKeys);
        } catch (Exception e) {
            log.warn("Failed to batch evict L2 cache: error={}", e.getMessage());
        }
        log.info("Cache batch evicted: count={}", schemaIds.size());
    }

    @Override
    public void clearAll() {
        l1Cache.invalidateAll();
        log.warn("L1 cache cleared. Note: L2 (Redis) not cleared, will expire by TTL.");
    }

    // ==================== 二级索引查询 ====================

    @Override
    public Set<String> getIdsBySecondaryIndex(String indexType, String indexKey) {
        if (indexType == null || indexType.isBlank() || indexKey == null || indexKey.isBlank()) {
            return Set.of();
        }
        return secondaryIndexCache.getSchemaIds(indexType, indexKey);
    }

    @Override
    public List<SchemaDefinition<?>> getBySecondaryIndex(String indexType, String indexKey, SchemaType targetSchemaType) {
        Set<String> schemaIds = getIdsBySecondaryIndex(indexType, indexKey);
        if (schemaIds.isEmpty()) {
            return List.of();
        }

        // 批量获取 Schema 定义
        Map<String, SchemaDefinition<?>> schemas = getByIds(schemaIds);
        if (schemas.isEmpty()) {
            return List.of();
        }

        // 按类型过滤（如果指定了类型）
        if (targetSchemaType == null) {
            return new ArrayList<>(schemas.values());
        }

        return schemas.values().stream()
                .filter(def -> def.getSchemaType() == targetSchemaType)
                .toList();
    }

    @Override
    public List<SchemaDefinition<?>> getBySecondaryIndex(SchemaId index, SchemaType targetSchemaType) {
        return getBySecondaryIndex(index.schemaType().name(), index.value(), targetSchemaType);
    }

    /**
     * 获取 Redis Key
     */
    private String getRedisKey(String schemaId) {
        return properties.getL2().getKeyPrefix() + schemaId;
    }

    /**
     * 写入 L1 + L2 缓存
     */
    private void putToCache(String schemaId, SchemaDefinition<?> def) {
        l1Cache.put(schemaId, def);
        try {
            redisTemplate.opsForValue().set(
                    getRedisKey(schemaId),
                    def,
                    properties.getL2().getExpireAfterWrite().toSeconds(),
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.warn("Failed to write L2 cache: schemaId={}, error={}", schemaId, e.getMessage());
        }
    }
}
