package dev.planka.infra.cache.schema;

import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Schema 缓存服务
 * <p>
 * 提供二级缓存（L1 Caffeine + L2 Redis）的 Schema 查询服务。
 * 通过 Kafka 事件监听实现缓存失效。
 */
public interface SchemaCacheService {

    /**
     * 根据 ID 获取 Schema 定义
     *
     * @param schemaId Schema ID
     * @return Schema 定义，不存在时返回 Optional.empty()
     */
    Optional<SchemaDefinition<?>> getById(String schemaId);

    Optional<SchemaDefinition<?>> getById(SchemaId schemaId);

    /**
     * 批量获取 Schema 定义
     *
     * @param schemaIds Schema ID 集合
     * @return schemaId -> SchemaDefinition 映射，不存在的 ID 不会出现在结果中
     */
    Map<String, SchemaDefinition<?>> getByIds(Set<String> schemaIds);

    /**
     * 失效指定 Schema 的 L1 本地缓存
     * <p>
     * 每个服务实例都需要调用此方法清除本地缓存。
     *
     * @param schemaId Schema ID
     */
    void evictL1(String schemaId);

    /**
     * 失效指定 Schema 的 L2 Redis 缓存
     * <p>
     * 只需要一个节点调用此方法清除 Redis 缓存。
     *
     * @param schemaId Schema ID
     */
    void evictL2(String schemaId);

    /**
     * 失效指定 Schema 的 L1 + L2 缓存
     *
     * @param schemaId Schema ID
     */
    void evict(String schemaId);

    /**
     * 批量失效 Schema 缓存
     *
     * @param schemaIds Schema ID 集合
     */
    void evictAll(Set<String> schemaIds);

    /**
     * 清空所有 Schema 缓存
     * <p>
     * 注意：此操作会清空所有缓存，慎用。
     */
    void clearAll();

    // ==================== 二级索引查询 ====================

    /**
     * 根据二级索引查询 Schema ID 集合
     *
     * @param indexType 索引类型（如 "CARD_TYPE"）
     * @param indexKey  索引值（如 cardTypeId）
     * @return Schema ID 集合
     */
    Set<String> getIdsBySecondaryIndex(String indexType, String indexKey);

    /**
     * 根据二级索引查询 Schema 定义列表
     *
     * @param indexType  索引类型（如 "CARD_TYPE"）
     * @param indexKey   索引值（如 cardTypeId）
     * @param targetSchemaType 目标 Schema 类型过滤（可选，null 表示不过滤）
     * @return Schema 定义列表
     */
    @Deprecated //使用 getBySecondaryIndex(SchemaId index, SchemaType schemaType)重载
    List<SchemaDefinition<?>> getBySecondaryIndex(String indexType, String indexKey, SchemaType targetSchemaType);


    List<SchemaDefinition<?>> getBySecondaryIndex(SchemaId index, SchemaType targetSchemaType);

}
