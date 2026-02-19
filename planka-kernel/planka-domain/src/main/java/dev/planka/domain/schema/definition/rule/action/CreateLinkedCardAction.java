package dev.planka.domain.schema.definition.rule.action;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.expression.TextExpressionTemplate;
import dev.planka.domain.schema.definition.action.assignment.FieldAssignment;
import dev.planka.domain.stream.StatusId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * 创建关联卡片动作
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CreateLinkedCardAction implements RuleAction {

    private final String linkFieldId;
    private final CardTypeId cardTypeId;
    private final TextExpressionTemplate titleTemplate;
    private final StatusId initialStatusId;
    private final List<FieldAssignment> fieldAssignments;
    private final int sortOrder;

    @JsonCreator
    public CreateLinkedCardAction(
            @JsonProperty("linkFieldId") String linkFieldId,
            @JsonProperty("cardTypeId") CardTypeId cardTypeId,
            @JsonProperty("titleTemplate") TextExpressionTemplate titleTemplate,
            @JsonProperty("initialStatusId") StatusId initialStatusId,
            @JsonProperty("fieldAssignments") List<FieldAssignment> fieldAssignments,
            @JsonProperty("sortOrder") Integer sortOrder) {
        this.linkFieldId = Objects.requireNonNull(linkFieldId, "linkFieldId must not be null");
        this.cardTypeId = Objects.requireNonNull(cardTypeId, "cardTypeId must not be null");
        this.titleTemplate = Objects.requireNonNull(titleTemplate, "titleTemplate must not be null");
        this.initialStatusId = initialStatusId;
        this.fieldAssignments = fieldAssignments;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    @JsonProperty("linkFieldId")
    public String getLinkFieldId() {
        return linkFieldId;
    }

    @JsonProperty("cardTypeId")
    public CardTypeId getCardTypeId() {
        return cardTypeId;
    }

    @JsonProperty("titleTemplate")
    public TextExpressionTemplate getTitleTemplate() {
        return titleTemplate;
    }

    @JsonProperty("initialStatusId")
    public StatusId getInitialStatusId() {
        return initialStatusId;
    }

    @JsonProperty("fieldAssignments")
    public List<FieldAssignment> getFieldAssignments() {
        return fieldAssignments;
    }

    @Override
    public String getActionType() {
        return "CREATE_LINKED_CARD";
    }

    @Override
    @JsonProperty("sortOrder")
    public int getSortOrder() {
        return sortOrder;
    }
}
