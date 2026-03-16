package cn.planka.schema.config;

import cn.planka.api.schema.service.FieldConfigQueryService;
import cn.planka.api.schema.spi.SchemaDataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FieldConfigQueryService 配置
 */
@Configuration
public class FieldConfigQueryConfig {

    @Bean
    public FieldConfigQueryService fieldConfigQueryService(SchemaDataProvider schemaDataProvider) {
        return new FieldConfigQueryService(schemaDataProvider);
    }
}
