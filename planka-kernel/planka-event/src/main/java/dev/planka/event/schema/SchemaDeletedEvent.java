package dev.planka.event.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Schema 删除事件
 * <p>
 * 当 Schema 被删除（软删除）时发布此事件。
 */
@Getter
@Setter
public class SchemaDeletedEvent extends SchemaEvent {

    private static final String EVENT_TYPE = "schema.deleted";

    /** 删除前的最后内容（JSON） */
    private String lastContent;

    /** 删除前的最后版本号 */
    private int lastVersion;

    @JsonCreator
    public SchemaDeletedEvent(
            @JsonProperty("orgId") String orgId,
            @JsonProperty("operatorId") String operatorId,
            @JsonProperty("sourceIp") String sourceIp,
            @JsonProperty("traceId") String traceId,
            @JsonProperty("schemaId") String schemaId) {
        super(orgId, operatorId, sourceIp, traceId, schemaId);
    }

    public SchemaDeletedEvent(String orgId, String operatorId, String sourceIp, String traceId,
                              String schemaId, String lastContent, int lastVersion) {
        super(orgId, operatorId, sourceIp, traceId, schemaId);
        this.lastContent = lastContent;
        this.lastVersion = lastVersion;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
