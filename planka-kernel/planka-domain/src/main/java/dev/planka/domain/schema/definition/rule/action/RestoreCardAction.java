package dev.planka.domain.schema.definition.rule.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 还原卡片动作
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class RestoreCardAction implements RuleAction {

    private final ActionTargetSelector target;
    private final int sortOrder;

    @JsonCreator
    public RestoreCardAction(
            @JsonProperty("target") ActionTargetSelector target,
            @JsonProperty("sortOrder") Integer sortOrder) {
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    @JsonProperty("target")
    public ActionTargetSelector getTarget() {
        return target;
    }

    @Override
    public String getActionType() {
        return "RESTORE_CARD";
    }

    @Override
    @JsonProperty("sortOrder")
    public int getSortOrder() {
        return sortOrder;
    }
}
