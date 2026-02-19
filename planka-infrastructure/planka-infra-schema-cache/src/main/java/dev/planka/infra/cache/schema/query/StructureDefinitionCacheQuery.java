package dev.planka.infra.cache.schema.query;

import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.StructureId;
import dev.planka.domain.schema.definition.structure.StructureDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StructureDefinitionCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public StructureDefinitionCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<StructureDefinition> getById(StructureId id) {
        return schemaCacheService.getById(id)
                .filter(StructureDefinition.class::isInstance)
                .map(StructureDefinition.class::cast);
    }

    public List<StructureDefinition> getByIds(Set<StructureId> ids) {
        Set<String> stringIds = ids.stream()
                .map(StructureId::value)
                .collect(Collectors.toSet());
        return schemaCacheService.getByIds(stringIds).values().stream()
                .filter(StructureDefinition.class::isInstance)
                .map(StructureDefinition.class::cast)
                .toList();
    }

    public List<StructureDefinition> getByOrgId(String orgId) {
        return schemaCacheService.getBySecondaryIndex("ORG", orgId, SchemaType.STRUCTURE_DEFINITION)
                .stream()
                .filter(StructureDefinition.class::isInstance)
                .map(StructureDefinition.class::cast)
                .toList();
    }
}
