package dev.planka.domain.schema.definition.action.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 数值增量赋值
 * <p>
 * 将数字字段的值增加或减少指定的量。
 * 如果原值为 null，则视为 0。
 * 计算结果会受字段的最小值/最大值约束。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class IncrementAssignment implements FieldAssignment {

    /**
     * 目标字段ID（必须是数字类型字段）
     */
    @JsonProperty("fieldId")
    private String fieldId;

    /**
     * 增量值
     * <p>
     * 正数：增加
     * 负数：减少
     */
    @JsonProperty("incrementValue")
    private BigDecimal incrementValue;

    /**
     * 是否允许结果为负数
     * <p>
     * false: 结果最小为 0
     * true: 允许负数
     */
    @JsonProperty("allowNegative")
    private boolean allowNegative = false;

    @Override
    public String getAssignmentType() {
        return "INCREMENT";
    }
}
