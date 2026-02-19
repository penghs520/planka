package dev.planka.event.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Schema 更新事件
 * <p>
 * 当 Schema 的内容或名称被更新时发布此事件。
 */
@Getter
@Setter
public class SchemaUpdatedEvent extends SchemaEvent {

    private static final String EVENT_TYPE = "schema.updated";

    /** 更新前的内容（JSON） */
    private String beforeContent;

    /** 更新后的内容（JSON） */
    private String afterContent;

    /** 更新后的版本号 */
    private int newVersion;

    /** 变更摘要（人可读） */
    private String changeSummary;

    @JsonCreator
    public SchemaUpdatedEvent(
            @JsonProperty("orgId") String orgId,
            @JsonProperty("operatorId") String operatorId,
            @JsonProperty("sourceIp") String sourceIp,
            @JsonProperty("traceId") String traceId,
            @JsonProperty("schemaId") String schemaId) {
        super(orgId, operatorId, sourceIp, traceId, schemaId);
    }

    public SchemaUpdatedEvent(String orgId, String operatorId, String sourceIp, String traceId,
                              String schemaId, String beforeContent, String afterContent, int newVersion) {
        super(orgId, operatorId, sourceIp, traceId, schemaId);
        this.beforeContent = beforeContent;
        this.afterContent = afterContent;
        this.newVersion = newVersion;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
