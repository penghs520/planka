package dev.planka.extension.config;

import dev.planka.api.card.CardServiceClient;
import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.CardQueryRequest;
import dev.planka.api.card.request.QueryScope;
import dev.planka.api.card.request.Yield;
import dev.planka.domain.card.CardId;
import dev.planka.infra.cache.card.CardBasicInfoLoader;
import dev.planka.infra.cache.card.CardCacheProperties;
import dev.planka.infra.cache.card.CardCacheService;
import dev.planka.infra.cache.card.CardCacheServiceImpl;
import dev.planka.infra.cache.card.model.CardBasicInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.*;

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

    private final Logger logger = LoggerFactory.getLogger(CardCacheConfig.class);

    /**
     * 配置卡片基础信息加载器
     * <p>
     * card-service 使用本地 repository 直接查询，性能最优
     *
     * @param cardServiceClient 卡片远程服务
     * @return 卡片基础信息加载器
     */
    @Bean
    public CardBasicInfoLoader cardBasicInfoLoader(CardServiceClient cardServiceClient) {
        return cardIds -> findBasicInfoByIds(cardIds, cardServiceClient);
    }


    public Map<CardId, CardBasicInfo> findBasicInfoByIds(Set<CardId> cardIds, CardServiceClient cardServiceClient) {
        if (cardIds == null || cardIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 构建轻量级查询请求，只查询基础字段
        CardQueryRequest request = new CardQueryRequest();

        // 设置查询范围：指定卡片ID列表
        QueryScope queryScope = new QueryScope();
        queryScope.setCardIds(cardIds.stream()
                .map(id -> String.valueOf(id.value()))
                .toList());
        request.setQueryScope(queryScope);

        // 设置 Yield：只返回基础字段，不包含自定义属性和关联卡片
        Yield yield = Yield.basic();
        request.setYield(yield);

        try {
            // 执行查询
            List<CardDTO> cards = cardServiceClient.query("system", request).getData();

            // 转换为 CardBasicInfo Map
            Map<CardId, CardBasicInfo> result = new HashMap<>();

            for (CardDTO card : cards) {
                CardBasicInfo basicInfo = convertToBasicInfo(card);
                result.put(basicInfo.cardId(), basicInfo);
            }

            logger.debug("批量查询卡片基础信息完成，请求: {}, 返回: {}", cardIds.size(), result.size());
            return result;
        } catch (Exception e) {
            logger.error("批量查询卡片基础信息失败", e);
            throw new RuntimeException("批量查询卡片基础信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 CardDTO 转换为 CardBasicInfo
     */
    private CardBasicInfo convertToBasicInfo(CardDTO card) {
        return new CardBasicInfo(
                card.getId(),
                card.getOrgId(),
                card.getTypeId(),
                card.getTitle(),
                card.getCustomCode() != null ? card.getCustomCode() : String.valueOf(card.getCodeInOrg()),
                card.getCardStyle(),
                card.getStreamId(),
                card.getStatusId()
        );
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
     * @param loader                     卡片基础信息加载器
     * @param cardBasicInfoRedisTemplate Redis 模板
     * @param properties                 缓存配置属性
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
