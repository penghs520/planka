package dev.planka.infra.cache.schema.query;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.PermissionConfigId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.permission.PermissionConfigDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CardPermissionCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public CardPermissionCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<PermissionConfigDefinition> getById(PermissionConfigId id) {
        return schemaCacheService.getById(id)
                .filter(PermissionConfigDefinition.class::isInstance)
                .map(PermissionConfigDefinition.class::cast);
    }

    public List<PermissionConfigDefinition> getByIds(Set<PermissionConfigId> ids) {
        Set<String> stringIds = ids.stream()
                .map(PermissionConfigId::value)
                .collect(Collectors.toSet());
        return schemaCacheService.getByIds(stringIds).values().stream()
                .filter(PermissionConfigDefinition.class::isInstance)
                .map(PermissionConfigDefinition.class::cast)
                .toList();
    }

    public List<PermissionConfigDefinition> getByCardTypeId(CardTypeId cardTypeId) {
        return schemaCacheService.getBySecondaryIndex(cardTypeId, SchemaType.CARD_PERMISSION)
                .stream()
                .filter(PermissionConfigDefinition.class::isInstance)
                .map(PermissionConfigDefinition.class::cast)
                .toList();
    }
}
