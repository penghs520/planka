package cn.agilean.kanban.infra.cache.schema.query;

import cn.agilean.kanban.domain.schema.SchemaType;
import cn.agilean.kanban.domain.schema.ViewId;
import cn.agilean.kanban.domain.schema.definition.view.ListViewDefinition;
import cn.agilean.kanban.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ViewCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public ViewCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<ListViewDefinition> getById(ViewId id) {
        return schemaCacheService.getById(id)
                .filter(ListViewDefinition.class::isInstance)
                .map(ListViewDefinition.class::cast);
    }

    public List<ListViewDefinition> getByIds(Set<ViewId> ids) {
        Set<String> stringIds = ids.stream()
                .map(ViewId::value)
                .collect(Collectors.toSet());
        return schemaCacheService.getByIds(stringIds).values().stream()
                .filter(ListViewDefinition.class::isInstance)
                .map(ListViewDefinition.class::cast)
                .toList();
    }

    public List<ListViewDefinition> getByOrgId(String orgId) {
        return schemaCacheService.getBySecondaryIndex("ORG", orgId, SchemaType.VIEW)
                .stream()
                .filter(ListViewDefinition.class::isInstance)
                .map(ListViewDefinition.class::cast)
                .toList();
    }
}
