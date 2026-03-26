package cn.planka.domain.schema.definition.rule.action;

import cn.planka.domain.stream.StatusId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 移动卡片动作
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class MoveCardAction implements RuleAction {

    private final ActionTargetSelector target;
    private final StatusId toStatusId;
    private final String name;
    private final int sortOrder;

    @JsonCreator
    public MoveCardAction(
            @JsonProperty("target") ActionTargetSelector target,
            @JsonProperty("toStatusId") StatusId toStatusId,
            @JsonProperty("name") String name,
            @JsonProperty("sortOrder") Integer sortOrder) {
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.toStatusId = Objects.requireNonNull(toStatusId, "toStatusId must not be null");
        this.name = name;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    @JsonProperty("target")
    public ActionTargetSelector getTarget() {
        return target;
    }

    @JsonProperty("toStatusId")
    public StatusId getToStatusId() {
        return toStatusId;
    }

    @Override
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @Override
    public String getActionType() {
        return "MOVE_CARD";
    }

    @Override
    @JsonProperty("sortOrder")
    public int getSortOrder() {
        return sortOrder;
    }
}
