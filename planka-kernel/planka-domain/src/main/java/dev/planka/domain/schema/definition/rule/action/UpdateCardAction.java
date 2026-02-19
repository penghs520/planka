package dev.planka.domain.schema.definition.rule.action;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.schema.definition.action.assignment.FieldAssignment;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * 修改卡片属性动作
 * <p>
 * 复用现有 FieldAssignment 框架。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class UpdateCardAction implements RuleAction {

    private final ActionTargetSelector target;
    private final List<FieldAssignment> fieldAssignments;
    private final int sortOrder;

    @JsonCreator
    public UpdateCardAction(
            @JsonProperty("target") ActionTargetSelector target,
            @JsonProperty("fieldAssignments") List<FieldAssignment> fieldAssignments,
            @JsonProperty("sortOrder") Integer sortOrder) {
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.fieldAssignments = AssertUtils.requireNotEmpty(fieldAssignments, "fieldAssignments must not be empty");
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    @JsonProperty("target")
    public ActionTargetSelector getTarget() {
        return target;
    }

    @JsonProperty("fieldAssignments")
    public List<FieldAssignment> getFieldAssignments() {
        return fieldAssignments;
    }

    @Override
    public String getActionType() {
        return "UPDATE_CARD";
    }

    @Override
    @JsonProperty("sortOrder")
    public int getSortOrder() {
        return sortOrder;
    }
}
