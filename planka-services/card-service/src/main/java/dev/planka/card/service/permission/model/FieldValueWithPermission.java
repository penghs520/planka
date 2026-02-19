package dev.planka.card.service.permission.model;

import dev.planka.domain.field.FieldId;
import dev.planka.domain.field.FieldValue;

/**
 * 带权限状态的属性值
 * <p>
 * 用于查询卡片时，标识每个属性的访问权限状态
 */
public class FieldValueWithPermission {

    private final FieldId fieldId;
    private final FieldValue<?> value;
    private final FieldPermissionStatus status;
    private final String message;

    private FieldValueWithPermission(
            FieldId fieldId,
            FieldValue<?> value,
            FieldPermissionStatus status,
            String message) {
        this.fieldId = fieldId;
        this.value = value;
        this.status = status;
        this.message = message;
    }

    /**
     * 创建完整访问权限的属性值
     *
     * @param value 属性值
     * @return 带权限状态的属性值
     */
    public static FieldValueWithPermission fullAccess(FieldValue<?> value) {
        return new FieldValueWithPermission(
                FieldId.of(value.getFieldId()),
                value,
                FieldPermissionStatus.FULL_ACCESS,
                null
        );
    }

    /**
     * 创建脱敏访问的属性值
     *
     * @param original     原始属性值
     * @param desensitized 脱敏后的属性值
     * @return 带权限状态的属性值
     */
    public static FieldValueWithPermission desensitized(
            FieldValue<?> original,
            FieldValue<?> desensitized) {
        return new FieldValueWithPermission(
                FieldId.of(original.getFieldId()),
                desensitized,
                FieldPermissionStatus.DESENSITIZED,
                null
        );
    }

    /**
     * 创建无权限的属性值
     *
     * @param fieldId 属性ID
     * @param message 提示信息
     * @return 带权限状态的属性值
     */
    public static FieldValueWithPermission noPermission(FieldId fieldId, String message) {
        return new FieldValueWithPermission(
                fieldId,
                null,
                FieldPermissionStatus.NO_PERMISSION,
                message
        );
    }

    public FieldId getFieldId() {
        return fieldId;
    }

    public FieldValue<?> getValue() {
        return value;
    }

    public FieldPermissionStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "FieldValueWithPermission{" +
                "fieldId=" + fieldId +
                ", status=" + status +
                ", message='" + message + '\'' +
                '}';
    }
}
