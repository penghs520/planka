package dev.planka.card.config;

import dev.planka.card.repository.CardRepository;
import dev.planka.infra.cache.card.CardBasicInfoLoader;
import dev.planka.infra.cache.card.CardCacheProperties;
import dev.planka.infra.cache.card.CardCacheService;
import dev.planka.infra.cache.card.CardCacheServiceImpl;
import dev.planka.infra.cache.card.model.CardBasicInfo;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 卡片缓存配置
 * <p>
 * 配置卡片缓存相关的 Bean：
 * <ul>
 *     <li>CardBasicInfoLoader - 从本地 repository 加载卡片基础信息</li>
 *     <li>CardCacheProperties - 缓存配置属性</li>
 *     <li>RedisTemplate - 用于 L2 缓存的 Redis 模板</li>
 *     <li>CardCacheService - 二级缓存服务</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(CardCacheProperties.class)
public class CardCacheConfig {

    /**
     * 配置卡片基础信息加载器
     * <p>
     * card-service 使用本地 repository 直接查询，性能最优
     *
     * @param cardRepository 卡片仓储
     * @return 卡片基础信息加载器
     */
    @Bean
    public CardBasicInfoLoader cardBasicInfoLoader(CardRepository cardRepository) {
        return cardRepository::findBasicInfoByIds;
    }

    /**
     * 配置用于卡片基础信息缓存的 RedisTemplate
     * <p>
     * 使用 JSON 序列化，便于调试和跨服务兼容
     *
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, CardBasicInfo> cardBasicInfoRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, CardBasicInfo> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置卡片缓存服务
     * <p>
     * 实现二级缓存：L1 (Caffeine) + L2 (Redis)
     *
     * @param loader        卡片基础信息加载器
     * @param redisTemplate Redis 模板
     * @param properties    缓存配置属性
     * @return 卡片缓存服务
     */
    @Bean
    public CardCacheService cardCacheService(
            CardBasicInfoLoader loader,
            RedisTemplate<String, CardBasicInfo> cardBasicInfoRedisTemplate,
            CardCacheProperties properties) {
        return new CardCacheServiceImpl(loader, cardBasicInfoRedisTemplate, properties);
    }
}
