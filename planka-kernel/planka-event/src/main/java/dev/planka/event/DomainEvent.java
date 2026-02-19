package dev.planka.event;

import dev.planka.common.util.AssertUtils;
import dev.planka.common.util.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;

/**
 * 领域事件基类
 * <p>
 * 所有领域事件的抽象基类，包含事件的通用元数据
 */
@Getter
@Setter
public abstract class DomainEvent {

    /**
     * 事件ID
     */
    private final String eventId;

    /**
     * 事件发生时间
     */
    private final Instant occurredAt;

    /**
     * 组织ID
     */
    private final String orgId;

    /**
     * 操作人ID：成员卡片ID
     */
    private final String operatorId;

    /**
     * 语言环境
     */
    private Locale locale;

    /**
     * 操作来源IP
     */
    private final String sourceIp;

    /**
     * 追踪ID（用于分布式追踪）
     */
    private final String traceId;


    protected DomainEvent(String orgId, String operatorId, String sourceIp, String traceId) {
        this.orgId = AssertUtils.requireNotBlank(orgId, "orgId can't be blank");
        this.operatorId = AssertUtils.requireNotBlank(operatorId, "operatorId can't be blank");
        this.sourceIp = sourceIp;
        this.traceId = traceId;
        this.eventId = StringUtils.uuid();
        this.occurredAt = Instant.now();
    }

    /**
     * 获取事件类型
     *
     * @return 事件类型标识：用于反序列化标识
     */
    public abstract String getEventType();

    /**
     * 事件主题
     */
    public abstract String getTopic();

    /**
     * kafka分区键
     * <p>
     * Kafka 使用 key 的哈希值来决定消息发送到哪个分区
     * 相同 key 的消息会被发送到同一个分区，保证这些消息的顺序性
     * 如果 key 为 null，消息会轮询（round-robin）发送到各个分区
     */
    public abstract String getPartitionKey();

}
