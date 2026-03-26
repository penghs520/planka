package cn.planka.domain.schema.definition.workflow;

/**
 * 开始节点定义
 */
public record StartNodeDefinition(
    String id,
    String name
) implements NodeDefinition {
}
