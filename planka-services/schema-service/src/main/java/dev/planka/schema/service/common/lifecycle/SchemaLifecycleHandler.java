package dev.planka.schema.service.common.lifecycle;

import dev.planka.domain.schema.definition.SchemaDefinition;

/**
 * Schema 生命周期处理器接口
 * <p>
 * 为不同类型的 Schema 提供创建、更新、删除等生命周期的扩展点。
 * 通过实现此接口并注册为 Spring Bean，可以为特定类型的 Schema 添加自定义的生命周期处理逻辑。
 *
 * @param <T> Schema 定义类型
 */
public interface SchemaLifecycleHandler<T extends SchemaDefinition<?>> {

    /**
     * 获取处理的 Schema 子类型标识
     *
     * @return SchemaSubType 常量值
     */
    String getSchemaSubType();

    /**
     * 创建前校验和处理
     *
     * @param definition 待创建的定义
     * @throws IllegalArgumentException 校验失败时抛出
     */
    default void beforeCreate(T definition) {
        // 默认无操作
    }

    /**
     * 创建后处理
     *
     * @param definition 已创建的定义
     */
    default void afterCreate(T definition) {
        // 默认无操作
    }

    /**
     * 更新前校验和处理
     *
     * @param oldDefinition 旧定义
     * @param newDefinition 新定义
     * @throws IllegalArgumentException 校验失败时抛出
     */
    default void beforeUpdate(T oldDefinition, T newDefinition) {
        // 默认无操作
    }

    /**
     * 更新后处理
     *
     * @param oldDefinition 旧定义
     * @param newDefinition 新定义（已保存）
     */
    default void afterUpdate(T oldDefinition, T newDefinition) {
        // 默认无操作
    }

    /**
     * 删除前校验和处理
     *
     * @param definition 待删除的定义
     * @throws IllegalArgumentException 校验失败时抛出
     */
    default void beforeDelete(T definition) {
        // 默认无操作
    }

    /**
     * 删除后处理
     *
     * @param definition 已删除的定义
     */
    default void afterDelete(T definition) {
        // 默认无操作
    }
}
