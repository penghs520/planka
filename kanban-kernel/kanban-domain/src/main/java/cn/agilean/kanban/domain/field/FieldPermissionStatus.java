package cn.agilean.kanban.domain.field;

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
     * 无权限
     * <p>
     * 用户无权查看该属性，值为 null
     */
    NO_PERMISSION
}
