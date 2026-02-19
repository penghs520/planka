package dev.planka.api.card.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 规则执行日志过滤选项DTO
 */
@Getter
@Setter
public class RuleExecutionLogFiltersDTO {

    /** 可选的规则列表 */
    private List<RuleOption> rules;

    /** 可选的状态列表 */
    private List<String> statuses;

    /**
     * 规则选项
     */
    @Getter
    @Setter
    public static class RuleOption {
        /** 规则ID */
        private String ruleId;

        /** 规则名称 */
        private String ruleName;

        public RuleOption() {}

        public RuleOption(String ruleId, String ruleName) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
        }
    }
}
