package cn.planka.domain.schema.definition.workflow;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 工作流节点定义（sealed interface）
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "nodeType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = StartNodeDefinition.class, name = "START"),
    @JsonSubTypes.Type(value = EndNodeDefinition.class, name = "END"),
    @JsonSubTypes.Type(value = ApprovalNodeDefinition.class, name = "APPROVAL"),
    @JsonSubTypes.Type(value = AutoActionNodeDefinition.class, name = "AUTO_ACTION")
})
public sealed interface NodeDefinition permits
    StartNodeDefinition,
    EndNodeDefinition,
    ApprovalNodeDefinition,
    AutoActionNodeDefinition {

    String id();
    String name();
}
