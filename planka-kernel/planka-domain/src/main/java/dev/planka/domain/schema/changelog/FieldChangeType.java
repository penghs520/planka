package dev.planka.domain.schema.changelog;

/**
 * 字段变更类型
 */
public enum FieldChangeType {
    /** 新增：从null变为有值 */
    ADDED,
    /** 修改：值发生变化 */
    MODIFIED,
    /** 移除：从有值变为null */
    REMOVED
}
