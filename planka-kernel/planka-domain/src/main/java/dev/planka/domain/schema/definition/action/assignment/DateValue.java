package dev.planka.domain.schema.definition.action.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 日期值
 * <p>
 * 支持绝对日期和相对日期（基于执行时间的偏移）
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DateValue implements FixedValue {

    /**
     * 日期模式
     */
    @JsonProperty("mode")
    private DateMode mode = DateMode.ABSOLUTE;

    /**
     * 绝对日期值（mode=ABSOLUTE 时使用）
     */
    @JsonProperty("absoluteDate")
    private LocalDateTime absoluteDate;

    /**
     * 相对偏移天数（mode=RELATIVE 时使用）
     */
    @JsonProperty("offsetDays")
    private Integer offsetDays;

    @Override
    public String getValueType() {
        return "DATE";
    }

    /**
     * 日期模式枚举
     */
    public enum DateMode {
        /**
         * 绝对日期
         */
        ABSOLUTE,

        /**
         * 相对日期（基于执行时间偏移）
         */
        RELATIVE
    }
}
