package cn.agilean.kanban.infra.cache.schema.query;

import cn.agilean.kanban.domain.field.FieldConfigId;
import cn.agilean.kanban.domain.schema.definition.fieldconfig.FieldConfig;
import cn.agilean.kanban.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class FieldConfigCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public FieldConfigCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<FieldConfig> getById(FieldConfigId id) {
        return schemaCacheService.getById(id)
                .filter(FieldConfig.class::isInstance)
                .map(FieldConfig.class::cast);
    }

    public List<FieldConfig> getByIds(Set<String> ids) {
        return schemaCacheService.getByIds(ids).values().stream()
                .filter(FieldConfig.class::isInstance)
                .map(FieldConfig.class::cast)
                .toList();
    }

}
