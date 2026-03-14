package cn.agilean.kanban.schema.config;

import cn.agilean.kanban.api.schema.service.FieldConfigQueryService;
import cn.agilean.kanban.api.schema.spi.SchemaDataProvider;
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
