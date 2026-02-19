package dev.planka.card.service.permission.model;

/**
 * 属性权限状态
 * <p>
 * 用于标识属性值的访问权限状态
 */
public enum FieldPermissionStatus {
    /**
     * 完整访问权限
     * <p>
     * 用户可以查看属性的完整值
     */
    FULL_ACCESS,

    /**
     * 脱敏访问
     * <p>
     * 用户只能查看脱敏后的属性值
     */
    DESENSITIZED,

    /**
     * 无权限
     * <p>
     * 用户无权查看该属性，值为 null
     */
    NO_PERMISSION
}
