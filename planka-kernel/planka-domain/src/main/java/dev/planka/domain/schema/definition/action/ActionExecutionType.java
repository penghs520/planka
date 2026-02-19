package dev.planka.domain.schema.definition.action;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 动作执行类型
 * <p>
 * 定义卡片动作的执行方式，使用 sealed interface 确保类型安全。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UpdateCardExecution.class, name = "UPDATE_CARD"),
        @JsonSubTypes.Type(value = CreateLinkedCardExecution.class, name = "CREATE_LINKED_CARD"),
        @JsonSubTypes.Type(value = CallExternalApiExecution.class, name = "CALL_EXTERNAL_API"),
        @JsonSubTypes.Type(value = NavigateToPageExecution.class, name = "NAVIGATE_TO_PAGE"),
        @JsonSubTypes.Type(value = TriggerBuiltInExecution.class, name = "TRIGGER_BUILT_IN")
})
public sealed interface ActionExecutionType
        permits UpdateCardExecution, CreateLinkedCardExecution, CallExternalApiExecution,
        NavigateToPageExecution, TriggerBuiltInExecution {

    /**
     * 获取执行类型标识
     */
    String getType();
}
