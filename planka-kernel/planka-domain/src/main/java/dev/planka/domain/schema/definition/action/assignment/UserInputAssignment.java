package dev.planka.domain.schema.definition.action.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户输入赋值
 * <p>
 * 执行动作时弹窗让用户手动输入字段值。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class UserInputAssignment implements FieldAssignment {

    /**
     * 目标字段ID
     */
    @JsonProperty("fieldId")
    private String fieldId;

    /**
     * 输入提示文字（可选）
     */
    @JsonProperty("placeholder")
    private String placeholder;

    /**
     * 是否必填（默认 true）
     */
    @JsonProperty("required")
    private Boolean required = true;

    @Override
    public String getAssignmentType() {
        return "USER_INPUT";
    }
}
