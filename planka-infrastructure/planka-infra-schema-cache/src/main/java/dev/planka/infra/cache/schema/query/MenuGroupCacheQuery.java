package dev.planka.infra.cache.schema.query;

import dev.planka.domain.schema.MenuGroupId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.menu.MenuGroupDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MenuGroupCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public MenuGroupCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<MenuGroupDefinition> getById(MenuGroupId id) {
        return schemaCacheService.getById(id)
                .filter(MenuGroupDefinition.class::isInstance)
                .map(MenuGroupDefinition.class::cast);
    }

    public List<MenuGroupDefinition> getByIds(Set<MenuGroupId> ids) {
        Set<String> stringIds = ids.stream()
                .map(MenuGroupId::value)
                .collect(Collectors.toSet());
        return schemaCacheService.getByIds(stringIds).values().stream()
                .filter(MenuGroupDefinition.class::isInstance)
                .map(MenuGroupDefinition.class::cast)
                .toList();
    }

    public List<MenuGroupDefinition> getByOrgId(String orgId) {
        return schemaCacheService.getBySecondaryIndex("ORG", orgId, SchemaType.MENU)
                .stream()
                .filter(MenuGroupDefinition.class::isInstance)
                .map(MenuGroupDefinition.class::cast)
                .toList();
    }
}
