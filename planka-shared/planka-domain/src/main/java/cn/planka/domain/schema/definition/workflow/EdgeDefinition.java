package cn.planka.domain.schema.definition.workflow;

/**
 * 工作流边定义
 */
public record EdgeDefinition(
    String id,
    String sourceNodeId,
    String targetNodeId
) {
}
