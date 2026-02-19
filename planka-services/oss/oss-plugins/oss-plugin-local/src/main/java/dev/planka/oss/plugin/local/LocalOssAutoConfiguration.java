package dev.planka.oss.plugin.local;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地存储自动配置
 */
@Configuration
@ConditionalOnProperty(prefix = "oss.local", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LocalOssAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "oss.local")
    public LocalOssProperties localOssProperties() {
        return new LocalOssProperties();
    }

    @Bean
    public LocalOssPlugin localOssPlugin(LocalOssProperties properties) {
        return new LocalOssPlugin(properties);
    }
}
