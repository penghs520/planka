package dev.planka.domain.schema.changelog;

/**
 * 语义变更操作类型
 */
public enum SemanticChangeType {
    /** 新增项 */
    ADDED,
    /** 修改项 */
    MODIFIED,
    /** 移除项 */
    REMOVED,
    /** 重排序 */
    REORDERED
}
