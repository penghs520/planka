package dev.planka.domain.history.source;

import dev.planka.domain.history.OperationSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 业务规则触发来源
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BizRuleOperationSource implements OperationSource {

    public static final String TYPE = "BIZ_RULE";
    private static final String MESSAGE_KEY = "history.source.bizrule";

    /**
     * 业务规则ID
     */
    private final String ruleId;

    /**
     * 业务规则名称
     */
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
    public String getMessageKey() {
        return MESSAGE_KEY;
    }
}
