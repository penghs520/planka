package dev.planka.domain.outsourcing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 结算配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementConf {

    /**
     * 结算方式（AUTO/MANUAL）
     */
    @JsonProperty("method")
    private SettlementMethod method = SettlementMethod.MANUAL;

    /**
     * 实际缺勤时间抵扣系数（≥0）
     */
    @JsonProperty("absenteeismDeductionCoefficient")
    private Integer absenteeismDeductionCoefficient = 2;

    /**
     * 结算单位（Day/Hour/Minute）
     */
    @JsonProperty("durationUnit")
    private DurationUnit durationUnit = DurationUnit.MINUTE;

    /**
     * 结算结果保留小数位数（≥0）
     */
    @JsonProperty("decimalScale")
    private Integer decimalScale = 0;

    /**
     * 需要单独计算的请假类型ID列表
     */
    @JsonProperty("specialLeaveItemIds")
    private List<String> specialLeaveItemIds;

    /**
     * 成员卡片类型ID列表
     */
    @JsonProperty("vutIds")
    private List<String> vutIds;

    /**
     * 离职日期字段ID
     */
    @JsonProperty("leaveDateFieldId")
    private String leaveDateFieldId;

    /**
     * 个人结算费用配置
     */
    @JsonProperty("personalServiceFeeConf")
    private PersonalServiceFeeConf personalServiceFeeConf;

    /**
     * 维度分摊配置
     */
    @JsonProperty("projectServiceFeeConf")
    private ProjectServiceFeeConf projectServiceFeeConf;

    public void validate() {
        if (absenteeismDeductionCoefficient != null && absenteeismDeductionCoefficient < 0) {
            throw new IllegalArgumentException("缺勤抵扣系数必须 ≥ 0");
        }
        if (decimalScale != null && decimalScale < 0) {
            throw new IllegalArgumentException("小数位数必须 ≥ 0");
        }
        if (vutIds == null || vutIds.isEmpty()) {
            throw new IllegalArgumentException("成员卡片类型ID列表不能为空");
        }

        if (personalServiceFeeConf != null) {
            personalServiceFeeConf.validate();
        }
        if (projectServiceFeeConf != null) {
            projectServiceFeeConf.validate();
        }
    }

    /**
     * 个人服务费配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalServiceFeeConf {
        @JsonProperty("baseFeeFieldId")
        private String baseFeeFieldId;

        @JsonProperty("overtimeFeeFieldId")
        private String overtimeFeeFieldId;

        @JsonProperty("subsidyFieldId")
        private String subsidyFieldId;

        public void validate() {
            // 可选字段，无需校验
        }
    }

    /**
     * 项目服务费配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectServiceFeeConf {
        @JsonProperty("columns")
        private List<Column> columns;

        public void validate() {
            if (columns != null) {
                for (Column column : columns) {
                    column.validate();
                }
            }
        }
    }

    /**
     * 分摊维度列
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Column {
        @JsonProperty("column")
        private String column;

        @JsonProperty("active")
        private Boolean active;

        public void validate() {
            if (column == null || column.isBlank()) {
                throw new IllegalArgumentException("维度列的 column 不能为空");
            }
            if (active == null) {
                throw new IllegalArgumentException("维度列的 active 不能为空");
            }
        }
    }
}
