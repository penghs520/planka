package dev.planka.infra.cache.schema;

import dev.planka.domain.schema.definition.SchemaDefinition;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Schema 缓存配置类
 * <p>
 * 配置 Redis 模板和 Kafka 消费者工厂。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SchemaCacheProperties.class)
public class SchemaCacheConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.application.name:schema-cache}")
    private String applicationName;

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * 服务实例唯一标识
     * <p>
     * 用于 L1 缓存失效的 Kafka consumer group，确保每个实例都能收到消息。
     * <p>
     * 优先使用 Nacos 配置构造 Instance ID（格式：ip#port#clusterName#serviceName），
     * 若 Nacos 未启用则降级使用 hostname:port。
     * <p>
     * 注意：nacosProperties.getPort() 在 Bean 创建时可能为 -1（默认值），
     * 因此使用 server.port 配置值替代。
     */
    @Bean
    public String schemaCacheInstanceId(ObjectProvider<NacosDiscoveryProperties> nacosPropertiesProvider) {
        NacosDiscoveryProperties nacosProperties = nacosPropertiesProvider.getIfAvailable();

        if (nacosProperties != null) {
            // 构造与 Nacos Instance ID 相同的格式
            // 使用 serverPort 替代 nacosProperties.getPort()，因为后者在 Bean 创建时可能为 -1
            String instanceId = String.format("%s#%d#%s#%s",
                    nacosProperties.getIp(),
                    serverPort,
                    nacosProperties.getClusterName(),
                    nacosProperties.getService());
            log.info("Using Nacos instance ID for schema cache: {}", instanceId);
            return instanceId;
        }

        // 降级方案：使用 hostname:port
        String fallbackId = getHostname() + ":" + serverPort;
        log.info("Nacos not available, using fallback instance ID: {}", fallbackId);
        return fallbackId;
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Failed to get hostname, using 'localhost'", e);
            return "localhost";
        }
    }

    /**
     * Schema 缓存专用 RedisTemplate
     * <p>
     * 使用 Jackson 序列化，支持 SchemaDefinition 多态类型。
     */
    @Bean
    public RedisTemplate<String, SchemaDefinition<?>> schemaRedisTemplate(
            RedisConnectionFactory factory,
            ObjectMapper objectMapper) {

        RedisTemplate<String, SchemaDefinition<?>> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 使用 String 序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 使用 Jackson 序列化（复用已配置的 ObjectMapper，支持多态）
        Jackson2JsonRedisSerializer<SchemaDefinition<?>> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper,
                        objectMapper.getTypeFactory().constructType(SchemaDefinition.class));
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * L1 缓存事件消费者工厂
     * <p>
     * 使用 latest 策略，重启时只消费最新事件（本地缓存已清空，无需处理历史消息）。
     * 使用 StringDeserializer，在监听器中手动反序列化。
     */
    @Bean
    public ConsumerFactory<String, String> schemaEventL1ConsumerFactory() {
        Map<String, Object> props = createCommonConsumerProps();
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new StringDeserializer());
    }

    /**
     * L2 缓存事件消费者工厂
     * <p>
     * 使用 earliest 策略，确保不丢失任何缓存失效消息。
     * 使用 StringDeserializer，在监听器中手动反序列化。
     */
    @Bean
    public ConsumerFactory<String, String> schemaEventL2ConsumerFactory() {
        Map<String, Object> props = createCommonConsumerProps();
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new StringDeserializer());
    }

    private Map<String, Object> createCommonConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // 连接保活配置
        props.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 300000);
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10000);

        return props;
    }

    /**
     * L1 缓存事件监听器容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> schemaEventL1ListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(schemaEventL1ConsumerFactory());
        return factory;
    }

    /**
     * L2 缓存事件监听器容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> schemaEventL2ListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(schemaEventL2ConsumerFactory());
        return factory;
    }
}
