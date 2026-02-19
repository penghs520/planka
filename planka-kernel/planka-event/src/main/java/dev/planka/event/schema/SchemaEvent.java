package dev.planka.event.schema;

import dev.planka.event.DomainEvent;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

/**
 * Schema 事件基类
 * <p>
 * 所有 Schema 领域事件的抽象基类。
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SchemaCreatedEvent.class, name = "schema.created"),
        @JsonSubTypes.Type(value = SchemaUpdatedEvent.class, name = "schema.updated"),
        @JsonSubTypes.Type(value = SchemaDeletedEvent.class, name = "schema.deleted")
})
public abstract class SchemaEvent extends DomainEvent {

    private static final String SCHEMA_EVENTS_TOPIC = "planka-schema-events";
    private static final String KEY = "Schema.Event";


    /** Schema ID */
    private final String schemaId;

    /** Schema 类型 */
    private String schemaType;

    protected SchemaEvent(String orgId, String operatorId, String sourceIp, String traceId, String schemaId) {
        super(orgId, operatorId, sourceIp, traceId);
        this.schemaId = schemaId;
    }

    @Override
    public String getPartitionKey() {
        return KEY;
    }

    @Override
    public String getTopic() {
        return SCHEMA_EVENTS_TOPIC;
    }

    /**
     * 设置 Schema 类型并返回自身（链式调用）
     */
    public SchemaEvent withSchemaType(String schemaType) {
        this.schemaType = schemaType;
        return this;
    }
}
