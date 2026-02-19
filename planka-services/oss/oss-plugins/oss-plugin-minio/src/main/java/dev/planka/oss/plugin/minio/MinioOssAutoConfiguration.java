package dev.planka.oss.plugin.minio;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 存储自动配置
 */
@Configuration
@ConditionalOnProperty(prefix = "oss.minio", name = "enabled", havingValue = "true")
public class MinioOssAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "oss.minio")
    public MinioOssProperties minioOssProperties() {
        return new MinioOssProperties();
    }

    @Bean
    public MinioOssPlugin minioOssPlugin(MinioOssProperties properties) {
        return new MinioOssPlugin(properties);
    }
}
