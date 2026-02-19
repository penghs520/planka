package dev.planka.domain.schema.definition.action.assignment;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 固定值
 * <p>
 * 使用 sealed interface 限定值类型，确保类型安全。
 * 人员字段使用 LinkValue（人员也是一种关联）。
 * <p>
 * 注意：与 dev.planka.domain.field.FieldValue 区分，
 * 这里是用于动作赋值的固定值配置。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "valueType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextValue.class, name = "TEXT"),
        @JsonSubTypes.Type(value = NumberValue.class, name = "NUMBER"),
        @JsonSubTypes.Type(value = DateValue.class, name = "DATE"),
        @JsonSubTypes.Type(value = EnumValue.class, name = "ENUM"),
        @JsonSubTypes.Type(value = LinkValue.class, name = "LINK")
})
public sealed interface FixedValue
        permits TextValue, NumberValue, DateValue, EnumValue, LinkValue {

    /**
     * 获取值类型标识
     */
    String getValueType();
}
