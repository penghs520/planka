package cn.planka.domain.schema.definition.rule.action;

import cn.planka.domain.expression.TextExpressionTemplate;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 回收卡片动作
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DiscardCardAction implements RuleAction {

    private final ActionTargetSelector target;
    private final TextExpressionTemplate reasonTemplate;
    private final String name;
    private final int sortOrder;

    @JsonCreator
    public DiscardCardAction(
            @JsonProperty("target") ActionTargetSelector target,
            @JsonProperty("reasonTemplate") TextExpressionTemplate reasonTemplate,
            @JsonProperty("name") String name,
            @JsonProperty("sortOrder") Integer sortOrder) {
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.reasonTemplate = reasonTemplate;
        this.name = name;
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
    @JsonProperty("name")
    public String getName() {
        return name;
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
