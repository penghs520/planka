package dev.planka.domain.schema.definition.action.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 固定值赋值
 * <p>
 * 将字段设置为预先配置的固定值。
 * 使用多态 FixedValue 表示不同类型的值。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FixedValueAssignment implements FieldAssignment {

    /**
     * 目标字段ID
     */
    @JsonProperty("fieldId")
    private String fieldId;

    /**
     * 固定值（多态类型）
     */
    @JsonProperty("value")
    private FixedValue value;

    @Override
    public String getAssignmentType() {
        return "FIXED_VALUE";
    }
}
