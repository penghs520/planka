package dev.planka.event.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Schema 创建事件
 * <p>
 * 当新的 Schema 被创建时发布此事件。
 */
@Getter
@Setter
public class SchemaCreatedEvent extends SchemaEvent {

    private static final String EVENT_TYPE = "schema.created";

    /** Schema 名称 */
    private String name;

    /** Schema 内容（JSON） */
    private String content;

    /** 所属 Schema ID（组合关系的宿主） */
    private String belongTo;

    @JsonCreator
    public SchemaCreatedEvent(
            @JsonProperty("orgId") String orgId,
            @JsonProperty("operatorId") String operatorId,
            @JsonProperty("sourceIp") String sourceIp,
            @JsonProperty("traceId") String traceId,
            @JsonProperty("schemaId") String schemaId) {
        super(orgId, operatorId, sourceIp, traceId, schemaId);
    }

    public SchemaCreatedEvent(String orgId, String operatorId, String sourceIp, String traceId,
                              String schemaId, String name, String content) {
        super(orgId, operatorId, sourceIp, traceId, schemaId);
        this.name = name;
        this.content = content;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
