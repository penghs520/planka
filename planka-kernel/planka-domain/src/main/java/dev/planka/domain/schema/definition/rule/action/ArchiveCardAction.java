package dev.planka.domain.schema.definition.rule.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 归档卡片动作
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ArchiveCardAction implements RuleAction {

    private final ActionTargetSelector target;
    private final int sortOrder;

    @JsonCreator
    public ArchiveCardAction(
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
        return "ARCHIVE_CARD";
    }

    @Override
    @JsonProperty("sortOrder")
    public int getSortOrder() {
        return sortOrder;
    }
}
