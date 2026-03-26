package cn.planka.domain.schema.definition.workflow;

import java.util.List;

/**
 * 固定成员选择器
 */
public record FixedMembersSelector(
    List<Long> memberIds
) implements ApproverSelector {
}
