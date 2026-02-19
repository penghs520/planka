package dev.planka.card.event;

import dev.planka.event.DomainEvent;
import dev.planka.event.publisher.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka 事件发布器实现
 * <p>
 * 将领域事件发布到 Kafka，封装 Kafka 相关细节
 */
@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);


    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        if (kafkaTemplate == null) {
            logger.debug("Kafka disabled, skip publishing event: {}", event.getEventType());
            return;
        }

        String key = event.getPartitionKey();

        try {
            kafkaTemplate.send(event.getTopic(), key, event);
            logger.debug("Published event: type={}, key={}", event.getEventType(), key);
        } catch (Exception e) {
            logger.error("Failed to publish event: type={}, eventId={}", event.getEventType(), event.getEventId(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    @Override
    public void publishAll(List<? extends DomainEvent> events) {
        if (kafkaTemplate == null) {
            logger.debug("Kafka disabled, skip publishing {} events", events.size());
            return;
        }

        for (DomainEvent event : events) {
            publish(event);
        }
    }

    @Override
    public void publishAsync(DomainEvent event) {
        if (kafkaTemplate == null) {
            logger.debug("Kafka disabled, skip async publishing event: {}", event.getEventType());
            return;
        }

        String key = event.getPartitionKey();

        kafkaTemplate.send(event.getTopic(), key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to async publish event: type={}, key={}",
                                event.getEventType(), key, ex);
                    } else {
                        logger.debug("Async published event: type={}, key={}",
                                event.getEventType(), key);
                    }
                });
    }

}
