package cn.planka.infra.cache.schema.query;

import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.CascadeRelationId;
import cn.planka.domain.schema.definition.cascaderelation.CascadeRelationDefinition;
import cn.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CascadeRelationDefinitionCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public CascadeRelationDefinitionCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<CascadeRelationDefinition> getById(CascadeRelationId id) {
        return schemaCacheService.getById(id)
                .filter(CascadeRelationDefinition.class::isInstance)
                .map(CascadeRelationDefinition.class::cast);
    }

    public List<CascadeRelationDefinition> getByIds(Set<CascadeRelationId> ids) {
        Set<String> stringIds = ids.stream()
                .map(CascadeRelationId::value)
                .collect(Collectors.toSet());
        return schemaCacheService.getByIds(stringIds).values().stream()
                .filter(CascadeRelationDefinition.class::isInstance)
                .map(CascadeRelationDefinition.class::cast)
                .toList();
    }

    public List<CascadeRelationDefinition> getByOrgId(String orgId) {
        return schemaCacheService.getBySecondaryIndex("ORG", orgId, SchemaType.CASCADE_RELATION_DEFINITION)
                .stream()
                .filter(CascadeRelationDefinition.class::isInstance)
                .map(CascadeRelationDefinition.class::cast)
                .toList();
    }
}
