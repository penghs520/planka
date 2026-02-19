package dev.planka.event.publisher;

import dev.planka.event.DomainEvent;

/**
 * 事件处理器接口
 * <p>
 * 定义领域事件的处理能力
 *
 * @param <E> 事件类型
 */
public interface EventHandler<E extends DomainEvent> {

    /**
     * 处理事件
     *
     * @param event 领域事件
     */
    void handle(E event);

    /**
     * 获取处理的事件类型
     *
     * @return 事件类类型
     */
    Class<E> getEventType();

    /**
     * 获取处理器优先级（数值越小优先级越高）
     *
     * @return 优先级
     */
    default int getOrder() {
        return 0;
    }

    /**
     * 是否支持异步处理
     *
     * @return 是否异步
     */
    default boolean isAsync() {
        return false;
    }
}
