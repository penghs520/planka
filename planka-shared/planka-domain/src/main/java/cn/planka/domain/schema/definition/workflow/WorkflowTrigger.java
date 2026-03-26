package cn.planka.domain.schema.definition.workflow;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 工作流触发器（sealed interface）
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "triggerType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ManualTrigger.class, name = "MANUAL")
})
public sealed interface WorkflowTrigger permits ManualTrigger {
}
