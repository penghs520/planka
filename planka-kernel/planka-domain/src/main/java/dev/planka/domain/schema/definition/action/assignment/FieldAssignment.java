package dev.planka.domain.schema.definition.action.assignment;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 字段赋值策略
 * <p>
 * 定义动作执行时如何给字段赋值，使用 sealed interface 确保类型安全。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "assignmentType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserInputAssignment.class, name = "USER_INPUT"),
        @JsonSubTypes.Type(value = FixedValueAssignment.class, name = "FIXED_VALUE"),
        @JsonSubTypes.Type(value = ReferenceFieldAssignment.class, name = "REFERENCE_FIELD"),
        @JsonSubTypes.Type(value = CurrentTimeAssignment.class, name = "CURRENT_TIME"),
        @JsonSubTypes.Type(value = ClearValueAssignment.class, name = "CLEAR_VALUE"),
        @JsonSubTypes.Type(value = IncrementAssignment.class, name = "INCREMENT")
})
public sealed interface FieldAssignment
        permits UserInputAssignment, FixedValueAssignment, ReferenceFieldAssignment,
        CurrentTimeAssignment, ClearValueAssignment, IncrementAssignment {

    /**
     * 获取目标字段ID
     */
    String getFieldId();

    /**
     * 获取赋值类型标识
     */
    String getAssignmentType();
}
