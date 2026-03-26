package cn.planka.domain.schema.definition.workflow;

/**
 * 结束节点定义
 */
public record EndNodeDefinition(
    String id,
    String name
) implements NodeDefinition {
}
