package dev.planka.event.comment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 业务规则触发来源
 */
public class BizRuleOperationSource implements OperationSource {

    public static final String TYPE = "BIZ_RULE";

    private final String ruleId;
    private final String ruleName;

    @JsonCreator
    public BizRuleOperationSource(
            @JsonProperty("ruleId") String ruleId,
            @JsonProperty("ruleName") String ruleName) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getDisplayName() {
        return ruleName;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }
}
