package dev.planka.infra.cache.schema;

import dev.planka.event.schema.SchemaCreatedEvent;
import dev.planka.event.schema.SchemaDeletedEvent;
import dev.planka.event.schema.SchemaEvent;
import dev.planka.event.schema.SchemaUpdatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Schema 缓存事件监听器
 * <p>
 * 监听 Schema 变更事件，实现缓存失效。
 * <p>
 * 使用两个不同的 Kafka consumer group：
 * - L1 失效：每个服务实例独立 group，确保所有实例都清除本地缓存
 * - L2 失效：所有实例共享同一 group，只有一个实例清除 Redis 缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaCacheEventListener {

    private static final String SCHEMA_EVENTS_TOPIC = "planka-schema-events";

    private final SchemaCacheService schemaCacheService;
    private final SecondaryIndexCache secondaryIndexCache;
    private final ObjectMapper objectMapper;

    /**
     * L1 缓存失效监听器
     * <p>
     * 每个服务实例独立消费，确保所有实例都清除本地 Caffeine 缓存。
     * GroupId 包含实例唯一标识（优先使用 Nacos Instance ID），使每个实例都能收到消息。
     * 使用 latest 策略，重启时只消费最新事件（本地缓存已清空，无需处理历史消息）。
     */
    @KafkaListener(
            topics = SCHEMA_EVENTS_TOPIC,
            groupId = "${spring.application.name}-schema-cache-l1-#{@schemaCacheInstanceId}",
            containerFactory = "schemaEventL1ListenerContainerFactory"
    )
    public void handleL1Eviction(String message) {
        SchemaEvent event = deserialize(message);
        if (event == null) {
            return;
        }

        // 缓存失效（仅 Update/Delete 需要）
        if (shouldEvict(event)) {
            String schemaId = event.getSchemaId();
            log.debug("L1 eviction: schemaId={}, type={}", schemaId, event.getSchemaType());
            schemaCacheService.evictL1(schemaId);
        }

        // 更新二级索引（Created/Updated/Deleted 都需要）
        if (shouldUpdateIndex(event)) {
            secondaryIndexCache.updateIndex(event);
        }
    }

    /**
     * L2 缓存失效监听器
     * <p>
     * 所有服务实例共享同一 group，只有一个实例清除 Redis 缓存。
     * 使用 earliest 策略，确保不丢失任何缓存失效消息。
     */
    @KafkaListener(
            topics = SCHEMA_EVENTS_TOPIC,
            groupId = "schema-cache-l2-eviction",
            containerFactory = "schemaEventL2ListenerContainerFactory"
    )
    public void handleL2Eviction(String message) {
        SchemaEvent event = deserialize(message);
        if (shouldEvict(event)) {
            String schemaId = event.getSchemaId();
            log.info("L2 eviction: schemaId={}, type={}", schemaId, event.getSchemaType());
            schemaCacheService.evictL2(schemaId);
        }
    }

    /**
     * 反序列化消息
     */
    private SchemaEvent deserialize(String message) {
        try {
            return objectMapper.readValue(message, SchemaEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize schema event: {}", message, e);
            return null;
        }
    }

    /**
     * 判断是否需要失效缓存
     */
    private boolean shouldEvict(SchemaEvent event) {
        return event instanceof SchemaUpdatedEvent || event instanceof SchemaDeletedEvent;
    }

    /**
     * 判断是否需要更新二级索引
     */
    private boolean shouldUpdateIndex(SchemaEvent event) {
        return event instanceof SchemaCreatedEvent
                || event instanceof SchemaUpdatedEvent
                || event instanceof SchemaDeletedEvent;
    }
}
