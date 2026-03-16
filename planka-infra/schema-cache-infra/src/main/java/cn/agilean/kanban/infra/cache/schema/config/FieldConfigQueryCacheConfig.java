package cn.planka.infra.cache.schema.config;

import cn.planka.api.schema.service.FieldConfigQueryService;
import cn.planka.api.schema.spi.SchemaDataProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FieldConfigQueryService 缓存配置
 */
@Configuration
public class FieldConfigQueryCacheConfig {

    @Bean
    @ConditionalOnMissingBean(FieldConfigQueryService.class)
    public FieldConfigQueryService fieldConfigQueryService(SchemaDataProvider schemaDataProvider) {
        return new FieldConfigQueryService(schemaDataProvider);
    }
}
