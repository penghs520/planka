package cn.planka.domain.schema.definition.workflow;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 审批人选择器（sealed interface）
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "selectorType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FixedMembersSelector.class, name = "FIXED_MEMBERS"),
    @JsonSubTypes.Type(value = RoleBasedSelector.class, name = "ROLE_BASED")
})
public sealed interface ApproverSelector permits
    FixedMembersSelector,
    RoleBasedSelector {
}
