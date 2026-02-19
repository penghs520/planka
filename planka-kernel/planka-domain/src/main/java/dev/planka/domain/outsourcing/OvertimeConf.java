package dev.planka.domain.outsourcing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 加班配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeConf {

    /**
     * 计算方式（固定为 ACTUAL_ATTENDANCE）
     */
    @JsonProperty("calWay")
    private OvertimeCalWay calWay = OvertimeCalWay.ACTUAL_ATTENDANCE;

    /**
     * 加班起算时间（分钟，≥0）
     */
    @JsonProperty("startDuration")
    private Integer startDuration = 0;

    /**
     * 最小加班时间（分钟，≥0）
     */
    @JsonProperty("minDuration")
    private Integer minDuration;

    /**
     * 最大加班时长限制
     */
    @JsonProperty("limitRules")
    private List<OvertimeLimitRule> limitRules;

    /**
     * 非工作日加班设置
     */
    @JsonProperty("nonWorkOvertime")
    private NonWorkOvertime nonWorkOvertime;

    /**
     * 加班换算规则
     */
    @JsonProperty("calRule")
    private OvertimeDurationCalRule calRule;

    public void validate() {
        if (startDuration != null && startDuration < 0) {
            throw new IllegalArgumentException("加班起算时间必须 ≥ 0");
        }
        if (minDuration != null && minDuration < 0) {
            throw new IllegalArgumentException("最小加班时间必须 ≥ 0");
        }

        if (limitRules != null) {
            for (OvertimeLimitRule rule : limitRules) {
                rule.validate();
            }
        }

        if (nonWorkOvertime != null) {
            nonWorkOvertime.validate();
        }

        if (calRule != null) {
            calRule.validate();
        }
    }

    /**
     * 加班时长限制规则
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OvertimeLimitRule {
        @JsonProperty("range")
        private DateUnit range;

        @JsonProperty("limit")
        private Integer limit;

        public void validate() {
            if (range == null) {
                throw new IllegalArgumentException("加班限制规则的 range 不能为空");
            }
            if (limit == null || limit < 0) {
                throw new IllegalArgumentException("加班限制规则的 limit 必须 ≥ 0");
            }
        }
    }

    /**
     * 非工作日加班设置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NonWorkOvertime {
        @JsonProperty("limit")
        private Double limit;

        public void validate() {
            if (limit != null && limit < 0) {
                throw new IllegalArgumentException("非工作日加班时长上限必须 ≥ 0");
            }
        }
    }

    /**
     * 加班换算规则
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OvertimeDurationCalRule {
        @JsonProperty("rules")
        private List<CalRule> rules;

        public void validate() {
            if (rules != null) {
                for (CalRule rule : rules) {
                    rule.validate();
                }
            }
        }
    }

    /**
     * 单条换算规则
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalRule {
        @JsonProperty("type")
        private OvertimeType type;

        @JsonProperty("ratio")
        private Double ratio;

        @JsonProperty("leaveItemId")
        private String leaveItemId;

        public void validate() {
            if (type == null) {
                throw new IllegalArgumentException("换算规则的 type 不能为空");
            }
            if (ratio == null || ratio <= 0) {
                throw new IllegalArgumentException("换算规则的 ratio 必须 > 0");
            }
            if (leaveItemId == null || leaveItemId.isBlank()) {
                throw new IllegalArgumentException("换算规则的 leaveItemId 不能为空");
            }
        }
    }
}
