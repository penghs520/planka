package cn.agilean.kanban.domain.field;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 属性值基类
 * <p>
 * 使用 sealed interface 保证类型安全，所有属性值类型必须是已知的。
 * 使用 Jackson 多态序列化支持。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextFieldValue.class, name = "TEXT"),
        @JsonSubTypes.Type(value = NumberFieldValue.class, name = "NUMBER"),
        @JsonSubTypes.Type(value = DateFieldValue.class, name = "DATE"),
        @JsonSubTypes.Type(value = EnumFieldValue.class, name = "ENUM"),
        @JsonSubTypes.Type(value = StructureFieldValue.class, name = "STRUCTURE"),
        @JsonSubTypes.Type(value = WebLinkFieldValue.class, name = "WEB_URL"),
        @JsonSubTypes.Type(value = AttachmentFieldValue.class, name = "ATTACHMENT"),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public sealed interface FieldValue<T>
        permits TextFieldValue, NumberFieldValue, DateFieldValue,
        EnumFieldValue, StructureFieldValue,
        WebLinkFieldValue, AttachmentFieldValue {

    /**
     * 获取属性ID
     */
    String getFieldId();

    /**
     * 获取属性值（可能返回 null）
     */
    T getValue();

    /**
     * 获取权限状态（null 表示完全权限）
     */
    FieldPermissionStatus getPermissionStatus();

    /**
     * 设置权限状态，同时直接重置 value：
     * <ul>
     *     <li>NO_PERMISSION → value 置为 null</li>
     *     <li>FULL_ACCESS 或 null → 不修改 value</li>
     * </ul>
     */
    void setPermissionStatus(FieldPermissionStatus status);

    /**
     * 判断值是否为空
     */
    boolean isEmpty();

}

// ==================== 辅助数据类型 ====================

// ==================== FieldValue 实现类 ====================

