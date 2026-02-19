package dev.planka.domain.schema.definition.rule.action;

import dev.planka.domain.expression.TextExpressionTemplate;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 丢弃卡片动作
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DiscardCardAction implements RuleAction {

    private final ActionTargetSelector target;
    private final TextExpressionTemplate reasonTemplate;
    private final int sortOrder;

    @JsonCreator
    public DiscardCardAction(
            @JsonProperty("target") ActionTargetSelector target,
            @JsonProperty("reasonTemplate") TextExpressionTemplate reasonTemplate,
            @JsonProperty("sortOrder") Integer sortOrder) {
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.reasonTemplate = reasonTemplate;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    @JsonProperty("target")
    public ActionTargetSelector getTarget() {
        return target;
    }

    @JsonProperty("reasonTemplate")
    public TextExpressionTemplate getReasonTemplate() {
        return reasonTemplate;
    }

    @Override
    public String getActionType() {
        return "DISCARD_CARD";
    }

    @Override
    @JsonProperty("sortOrder")
    public int getSortOrder() {
        return sortOrder;
    }
}
