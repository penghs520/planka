package dev.planka.infra.cache.schema;

import dev.planka.api.schema.SecondaryIndexQueryClient;
import dev.planka.common.result.Result;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.event.schema.SchemaCreatedEvent;
import dev.planka.event.schema.SchemaDeletedEvent;
import dev.planka.event.schema.SchemaEvent;
import dev.planka.event.schema.SchemaUpdatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Schema 二级索引缓存
 * <p>
 * 维护 Schema 二级索引的本地内存缓存：
 * - 启动时从 schema-service 加载全量索引
 * - 运行时通过 Kafka 事件增量更新
 * <p>
 * 索引结构: indexType -> (indexKey -> Set<schemaId>)
 * 例如: {"CARD_TYPE": {"cardTypeId1": ["fieldDefId1", "fieldDefId2"]}}
 * <p>
 * 线程安全：
 * - 使用 ConcurrentHashMap 的原子操作保证并发安全
 * - 使用 CopyOnWriteArrayList 避免遍历时的 ConcurrentModificationException
 */
@Slf4j
@Component
public class SecondaryIndexCache {

    /** 内存索引: indexType -> (indexKey -> Set<schemaId>) */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Set<String>>> indexMap = new ConcurrentHashMap<>();

    /** 反向索引: schemaId -> List<IndexEntry>，用于快速删除 */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<IndexEntry>> reverseIndex = new ConcurrentHashMap<>();

    private final SecondaryIndexQueryClient indexQueryClient;
    private final ObjectMapper objectMapper;

    public SecondaryIndexCache(SecondaryIndexQueryClient indexQueryClient, ObjectMapper objectMapper) {
        this.indexQueryClient = indexQueryClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadAllIndexes();
    }

    /**
     * 启动时加载全量索引
     */
    private void loadAllIndexes() {
        try {
            log.info("Loading all secondary indexes from schema-service...");
            Result<Map<String, Map<String, Set<String>>>> result = indexQueryClient.getAllSecondaryIndexes();

            if (result.isSuccess() && result.getData() != null) {
                Map<String, Map<String, Set<String>>> data = result.getData();

                // 清空现有索引
                indexMap.clear();
                reverseIndex.clear();

                // 加载新索引
                for (Map.Entry<String, Map<String, Set<String>>> typeEntry : data.entrySet()) {
                    String indexType = typeEntry.getKey();
                    ConcurrentHashMap<String, Set<String>> keyMap = new ConcurrentHashMap<>();

                    for (Map.Entry<String, Set<String>> keyEntry : typeEntry.getValue().entrySet()) {
                        String indexKey = keyEntry.getKey();
                        Set<String> schemaIds = ConcurrentHashMap.newKeySet();
                        schemaIds.addAll(keyEntry.getValue());
                        keyMap.put(indexKey, schemaIds);

                        // 构建反向索引
                        for (String schemaId : keyEntry.getValue()) {
                            reverseIndex
                                    .computeIfAbsent(schemaId, k -> new CopyOnWriteArrayList<>())
                                    .add(new IndexEntry(indexType, indexKey));
                        }
                    }

                    indexMap.put(indexType, keyMap);
                }

                int totalEntries = reverseIndex.size();
                log.info("Loaded secondary indexes: {} schema entries, {} index types",
                        totalEntries, indexMap.size());
            } else {
                log.warn("Failed to load secondary indexes: {}", result.getMessage());
            }
        } catch (Exception e) {
            log.error("Error loading secondary indexes", e);
        }
    }

    /**
     * 根据索引类型和键值获取 Schema ID 集合
     *
     * @param indexType 索引类型（如 "CARD_TYPE"）
     * @param indexKey  索引值（如 cardTypeId）
     * @return Schema ID 集合（不可变）
     */
    public Set<String> getSchemaIds(String indexType, String indexKey) {
        ConcurrentHashMap<String, Set<String>> keyMap = indexMap.get(indexType);
        if (keyMap == null) {
            return Set.of();
        }
        Set<String> schemaIds = keyMap.get(indexKey);
        return schemaIds != null ? Set.copyOf(schemaIds) : Set.of();
    }

    /**
     * 更新索引（由 SchemaCacheEventListener 调用）
     *
     * @param event Schema 事件
     */
    public void updateIndex(SchemaEvent event) {
        if (event instanceof SchemaCreatedEvent createdEvent) {
            handleSchemaCreated(createdEvent);
        } else if (event instanceof SchemaUpdatedEvent updatedEvent) {
            handleSchemaUpdated(updatedEvent);
        } else if (event instanceof SchemaDeletedEvent) {
            handleSchemaDeleted(event.getSchemaId());
        }
    }

    /**
     * 处理 Schema 创建事件
     */
    private void handleSchemaCreated(SchemaCreatedEvent event) {
        String schemaId = event.getSchemaId();
        String content = event.getContent();

        Set<IndexEntry> newEntries = parseSecondaryKeys(schemaId, content);
        if (newEntries == null || newEntries.isEmpty()) {
            return;
        }

        CopyOnWriteArrayList<IndexEntry> entries = new CopyOnWriteArrayList<>();
        for (IndexEntry entry : newEntries) {
            addToIndexMap(entry.indexType(), entry.indexKey(), schemaId);
            entries.add(entry);
        }
        reverseIndex.put(schemaId, entries);

        log.debug("Created index for schema {}: {} entries", schemaId, entries.size());
    }

    /**
     * 处理 Schema 更新事件
     * <p>
     * 使用 ConcurrentHashMap.compute 原子操作确保删除和添加的一致性
     */
    private void handleSchemaUpdated(SchemaUpdatedEvent event) {
        String schemaId = event.getSchemaId();
        String afterContent = event.getAfterContent();

        // 解析新的二级索引
        Set<IndexEntry> newEntries = parseSecondaryKeys(schemaId, afterContent);

        // 使用 compute 原子操作：删除旧索引并添加新索引
        reverseIndex.compute(schemaId, (id, oldEntries) -> {
            // 1. 删除旧索引
            if (oldEntries != null) {
                for (IndexEntry entry : oldEntries) {
                    removeFromIndexMap(entry.indexType(), entry.indexKey(), schemaId);
                }
            }

            // 2. 添加新索引
            if (newEntries == null || newEntries.isEmpty()) {
                return null; // 返回 null 会从 map 中移除该 key
            }

            CopyOnWriteArrayList<IndexEntry> entries = new CopyOnWriteArrayList<>();
            for (IndexEntry entry : newEntries) {
                addToIndexMap(entry.indexType(), entry.indexKey(), schemaId);
                entries.add(entry);
            }

            log.debug("Updated index for schema {}: {} entries", schemaId, entries.size());
            return entries;
        });
    }

    /**
     * 解析 Schema 内容获取二级索引键
     */
    private Set<IndexEntry> parseSecondaryKeys(String schemaId, String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        try {
            SchemaDefinition<?> schema = objectMapper.readValue(content, SchemaDefinition.class);
            if (schema == null) {
                return null;
            }

            var secondKeys = schema.secondKeys();
            if (secondKeys == null || secondKeys.isEmpty()) {
                return null;
            }

            Set<IndexEntry> entries = new HashSet<>();
            for (var secondKey : secondKeys) {
                String indexType = secondKey.schemaType().name();
                String indexKey = secondKey.value();
                entries.add(new IndexEntry(indexType, indexKey));
            }
            return entries;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse schema content for indexing: schemaId={}", schemaId, e);
            return null;
        }
    }

    /**
     * 处理 Schema 删除事件
     */
    private void handleSchemaDeleted(String schemaId) {
        // 使用 compute 原子操作删除索引
        reverseIndex.compute(schemaId, (id, entries) -> {
            if (entries != null) {
                for (IndexEntry entry : entries) {
                    removeFromIndexMap(entry.indexType(), entry.indexKey(), schemaId);
                }
            }
            log.debug("Removed index for deleted schema: {}", schemaId);
            return null; // 返回 null 会从 map 中移除该 key
        });
    }

    /**
     * 从 indexMap 中移除指定的 schemaId
     */
    private void removeFromIndexMap(String indexType, String indexKey, String schemaId) {
        ConcurrentHashMap<String, Set<String>> keyMap = indexMap.get(indexType);
        if (keyMap != null) {
            // 使用 compute 原子操作，避免 check-then-act 竞态
            keyMap.compute(indexKey, (key, schemaIds) -> {
                if (schemaIds == null) {
                    return null;
                }
                schemaIds.remove(schemaId);
                return schemaIds.isEmpty() ? null : schemaIds;
            });
        }
    }

    /**
     * 向 indexMap 中添加 schemaId
     */
    private void addToIndexMap(String indexType, String indexKey, String schemaId) {
        indexMap
                .computeIfAbsent(indexType, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(indexKey, k -> ConcurrentHashMap.newKeySet())
                .add(schemaId);
    }

    /**
     * 索引条目记录
     */
    private record IndexEntry(String indexType, String indexKey) {}
}
