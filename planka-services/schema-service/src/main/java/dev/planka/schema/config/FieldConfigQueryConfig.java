package dev.planka.schema.config;

import dev.planka.api.schema.service.FieldConfigQueryService;
import dev.planka.api.schema.spi.SchemaDataProvider;
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
