package dev.planka.domain.schema.definition.rule.action;

import dev.planka.domain.expression.TextExpressionTemplate;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 评论卡片动作
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CommentCardAction implements RuleAction {

    private final ActionTargetSelector target;
    private final TextExpressionTemplate contentTemplate;
    private final int sortOrder;

    @JsonCreator
    public CommentCardAction(
            @JsonProperty("target") ActionTargetSelector target,
            @JsonProperty("contentTemplate") TextExpressionTemplate contentTemplate,
            @JsonProperty("sortOrder") Integer sortOrder) {
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.contentTemplate = Objects.requireNonNull(contentTemplate, "contentTemplate must not be null");
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    @JsonProperty("target")
    public ActionTargetSelector getTarget() {
        return target;
    }

    @JsonProperty("contentTemplate")
    public TextExpressionTemplate getContentTemplate() {
        return contentTemplate;
    }

    @Override
    public String getActionType() {
        return "COMMENT_CARD";
    }

    @Override
    @JsonProperty("sortOrder")
    public int getSortOrder() {
        return sortOrder;
    }
}
