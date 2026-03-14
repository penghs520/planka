package cn.agilean.kanban.notification.config;

import cn.agilean.kanban.event.notification.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 消费者配置
 */
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;
    private final ObjectMapper objectMapper;

    /**
     * 通知事件消费者工厂
     */
    @Bean
    public ConsumerFactory<String, NotificationEvent> notificationEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        JsonDeserializer<NotificationEvent> jsonDeserializer = new JsonDeserializer<>(NotificationEvent.class, objectMapper);
        jsonDeserializer.addTrustedPackages("cn.agilean.kanban.event.*");
        jsonDeserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer)
        );
    }

    /**
     * 通知事件监听器容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> notificationEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationEventConsumerFactory());
        factory.setConcurrency(3); // 并发消费者数量
        factory.getContainerProperties().setPollTimeout(3000);
        return factory;
    }
}
