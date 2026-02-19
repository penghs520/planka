package dev.planka.domain.outsourcing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 请假配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveConf {

    /**
     * 请假限制规则列表
     */
    @JsonProperty("limitRules")
    private List<LeaveLimitRule> limitRules;

    /**
     * 最小请假单位（HALF_DAY/DAY）
     */
    @JsonProperty("leaveUnit")
    private LeaveUnit leaveUnit = LeaveUnit.HALF_DAY;

    /**
     * 启用的请假类型列表
     */
    @JsonProperty("enabledLeaveTypes")
    private List<LeaveType> enabledLeaveTypes;

    public void validate() {
        if (limitRules != null) {
            for (LeaveLimitRule rule : limitRules) {
                rule.validate();
            }
        }
    }

    /**
     * 请假限制规则
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaveLimitRule {

        /**
         * 周期范围（DAY/WEEK/MONTH）
         */
        @JsonProperty("range")
        private DateUnit range;

        /**
         * 请假类型（枚举值）
         */
        @JsonProperty("leaveType")
        private LeaveType leaveType;

        /**
         * 请假类型ID（自定义枚举属性，兼容旧数据）
         * @deprecated 使用 leaveType 代替
         */
        @JsonProperty("itemId")
        @Deprecated
        private String itemId;

        /**
         * 该周期内的请假上限天数（≥0）
         */
        @JsonProperty("limit")
        private Integer limit;

        public void validate() {
            if (range == null) {
                throw new IllegalArgumentException("请假限制规则的 range 不能为空");
            }
            // leaveType 和 itemId 至少有一个不为空
            if (leaveType == null && (itemId == null || itemId.isBlank())) {
                throw new IllegalArgumentException("请假限制规则的 leaveType 或 itemId 不能同时为空");
            }
            if (limit == null || limit < 0) {
                throw new IllegalArgumentException("请假限制规则的 limit 必须 ≥ 0");
            }
        }
    }
}
