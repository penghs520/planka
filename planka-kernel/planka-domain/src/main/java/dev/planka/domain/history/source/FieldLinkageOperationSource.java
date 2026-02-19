package dev.planka.domain.history.source;

import dev.planka.domain.history.OperationSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 字段联动更新来源
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FieldLinkageOperationSource implements OperationSource {

    public static final String TYPE = "FIELD_LINKAGE";
    private static final String MESSAGE_KEY = "history.source.fieldlinkage";

    /**
     * 触发联动的源字段ID
     */
    private final String triggerFieldId;

    /**
     * 触发联动的源字段名称
     */
    private final String triggerFieldName;

    @JsonCreator
    public FieldLinkageOperationSource(
            @JsonProperty("triggerFieldId") String triggerFieldId,
            @JsonProperty("triggerFieldName") String triggerFieldName) {
        this.triggerFieldId = triggerFieldId;
        this.triggerFieldName = triggerFieldName;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getMessageKey() {
        return MESSAGE_KEY;
    }
}
