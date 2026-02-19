package dev.planka.domain.schema;

/**
 * Schema 实体状态枚举
 * <p>
 * 用于标识 Schema 元素的生命周期状态。
 */
public enum EntityState {

    /**
     * 活跃状态 - 正常使用中
     */
    ACTIVE,

    /**
     * 停用状态 - 已停用，不再可选，但历史数据保留
     */
    DISABLED,

    /**
     * 已删除状态 - 软删除，不可恢复
     */
    DELETED
}
