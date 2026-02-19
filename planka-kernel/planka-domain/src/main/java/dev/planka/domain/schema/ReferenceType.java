package dev.planka.domain.schema;

/**
 * Schema 引用类型枚举
 * <p>
 * 用于描述 Schema 之间的引用关系类型。
 */
public enum ReferenceType {

    /**
     * 组合关系（Composition）
     * <p>
     * 特点：整体与部分的强依赖关系，部分不能独立存在
     * <p>
     * Schema 场景：
     *   - belongTo 字段标识的关系
     *   - 例：价值流定义、卡面定义、卡片详情页定义 属于 卡片类型，卡片类型删除时会级联删除价值流定义、卡面定义、卡片详情页定义
     */
    COMPOSITION,

    /**
     * 聚合关系（Aggregation）
     * <p>
     * 特点：整体与部分的弱依赖关系，部分可以独立存在
     * <p>
     * Schema 场景：
     *   - 普通的 xxxId 引用字段
     *   - 例：视图 引用了 卡片类型，但卡片类型不属于视图
     * <p>
     *   删除行为：阻止删除 - 如果被其他 Schema 聚合引用，则不允许删除
     * <p>
     */
    AGGREGATION
}
