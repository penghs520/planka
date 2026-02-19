package dev.planka.domain.schema.definition.action.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 当前时间赋值
 * <p>
 * 将目标字段设置为动作执行时的当前时间。
 * 支持时间偏移（天数）。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CurrentTimeAssignment implements FieldAssignment {

    /**
     * 目标字段ID（必须是日期类型字段）
     */
    @JsonProperty("fieldId")
    private String fieldId;

    /**
     * 时间偏移量（天数）
     * <p>
     * 0: 当前时间
     * 正数: 未来时间（如 7 表示一周后）
     * 负数: 过去时间
     */
    @JsonProperty("offsetDays")
    private int offsetDays = 0;

    @Override
    public String getAssignmentType() {
        return "CURRENT_TIME";
    }
}
