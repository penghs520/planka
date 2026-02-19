package dev.planka.domain.schema.definition.rule.action;

import dev.planka.domain.expression.TextExpressionTemplate;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * 生成用户行为数据动作
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TrackUserBehaviorAction implements RuleAction {

    private final String behaviorType;
    private final Map<String, TextExpressionTemplate> properties;
    private final int sortOrder;

    @JsonCreator
    public TrackUserBehaviorAction(
            @JsonProperty("behaviorType") String behaviorType,
            @JsonProperty("properties") Map<String, TextExpressionTemplate> properties,
            @JsonProperty("sortOrder") Integer sortOrder) {
        this.behaviorType = Objects.requireNonNull(behaviorType, "behaviorType must not be null");
        this.properties = properties;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    @JsonProperty("behaviorType")
    public String getBehaviorType() {
        return behaviorType;
    }

    @JsonProperty("properties")
    public Map<String, TextExpressionTemplate> getProperties() {
        return properties;
    }

    @Override
    public String getActionType() {
        return "TRACK_USER_BEHAVIOR";
    }

    @Override
    @JsonProperty("sortOrder")
    public int getSortOrder() {
        return sortOrder;
    }
}
