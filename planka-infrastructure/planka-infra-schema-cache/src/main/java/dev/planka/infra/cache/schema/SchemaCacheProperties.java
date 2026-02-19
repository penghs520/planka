package dev.planka.infra.cache.schema;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Schema 缓存配置属性
 * <p>
 * 支持 L1 (Caffeine) 和 L2 (Redis) 二级缓存的配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "kanban.cache.schema")
public class SchemaCacheProperties {

    /**
     * L1 本地缓存配置
     */
    private L1Properties l1 = new L1Properties();

    /**
     * L2 Redis 缓存配置
     */
    private L2Properties l2 = new L2Properties();

    /**
     * L1 Caffeine 缓存配置
     */
    @Getter
    @Setter
    public static class L1Properties {
        /**
         * 最大缓存条目数
         */
        private int maxSize = 10000;

        /**
         * 写入后过期时间
         */
        private Duration expireAfterWrite = Duration.ofMinutes(60 * 24 * 7);
    }

    /**
     * L2 Redis 缓存配置
     */
    @Getter
    @Setter
    public static class L2Properties {
        /**
         * 写入后过期时间
         */
        private Duration expireAfterWrite = Duration.ofMinutes(60 * 24);

        /**
         * Redis Key 前缀
         */
        private String keyPrefix = "schema:def:";
    }
}
