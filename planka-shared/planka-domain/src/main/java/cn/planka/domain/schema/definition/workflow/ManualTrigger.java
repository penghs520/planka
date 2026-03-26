package cn.planka.domain.schema.definition.workflow;

import java.util.List;

/**
 * 手动触发器
 */
public record ManualTrigger(
    List<Long> allowedMemberIds,
    List<String> allowedRoleIds
) implements WorkflowTrigger {
}
