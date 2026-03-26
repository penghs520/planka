package cn.planka.domain.schema.definition.workflow;

/**
 * 审批节点定义
 */
public record ApprovalNodeDefinition(
    String id,
    String name,
    ApproverSelector approverSelector,
    ApprovalMode approvalMode
) implements NodeDefinition {
}
