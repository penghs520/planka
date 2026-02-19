package dev.planka.event.publisher;

import dev.planka.event.DomainEvent;

import java.util.List;

/**
 * 事件发布器接口
 * <p>
 * 定义领域事件的发布能力，具体实现由基础设施层提供（如Kafka）
 */
public interface EventPublisher {

    /**
     * 发布单个事件
     *
     * @param event 领域事件
     */
    void publish(DomainEvent event);

    /**
     * 批量发布事件
     *
     * @param events 领域事件列表
     */
    void publishAll(List<? extends DomainEvent> events);

    /**
     * 异步发布单个事件
     *
     * @param event 领域事件
     */
    default void publishAsync(DomainEvent event) {
        publish(event);
    }

    /**
     * 异步批量发布事件
     *
     * @param events 领域事件列表
     */
    default void publishAllAsync(List<? extends DomainEvent> events) {
        publishAll(events);
    }
}
