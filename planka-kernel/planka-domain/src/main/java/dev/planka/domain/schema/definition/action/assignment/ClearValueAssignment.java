package dev.planka.domain.schema.definition.action.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 清空字段值
 * <p>
 * 将目标字段的值清空为 null 或空集合。
 * 适用于所有非必填字段。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ClearValueAssignment implements FieldAssignment {

    /**
     * 目标字段ID
     */
    @JsonProperty("fieldId")
    private String fieldId;

    @Override
    public String getAssignmentType() {
        return "CLEAR_VALUE";
    }
}
