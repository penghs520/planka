package dev.planka.infra.cache.schema.query;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.link.LinkTypeId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LinkTypeCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public LinkTypeCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<LinkTypeDefinition> getById(LinkTypeId id) {
        return schemaCacheService.getById(id)
                .filter(LinkTypeDefinition.class::isInstance)
                .map(LinkTypeDefinition.class::cast);
    }

    public List<LinkTypeDefinition> getByIds(Set<LinkTypeId> ids) {
        Set<String> stringIds = ids.stream()
                .map(LinkTypeId::value)
                .collect(Collectors.toSet());
        return schemaCacheService.getByIds(stringIds).values().stream()
                .filter(LinkTypeDefinition.class::isInstance)
                .map(LinkTypeDefinition.class::cast)
                .toList();
    }

    public List<LinkTypeDefinition> getByCardTypeId(CardTypeId cardTypeId) {
        return schemaCacheService.getBySecondaryIndex(cardTypeId, SchemaType.LINK_TYPE)
                .stream()
                .filter(LinkTypeDefinition.class::isInstance)
                .map(LinkTypeDefinition.class::cast)
                .toList();
    }
}
