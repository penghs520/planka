package dev.planka.infra.cache.schema.query;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.CardFaceId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.template.CardFaceDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CardFaceCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public CardFaceCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<CardFaceDefinition> getById(CardFaceId id) {
        return schemaCacheService.getById(id)
                .filter(CardFaceDefinition.class::isInstance)
                .map(CardFaceDefinition.class::cast);
    }

    public List<CardFaceDefinition> getByIds(Set<CardFaceId> ids) {
        Set<String> stringIds = ids.stream()
                .map(CardFaceId::value)
                .collect(Collectors.toSet());
        return schemaCacheService.getByIds(stringIds).values().stream()
                .filter(CardFaceDefinition.class::isInstance)
                .map(CardFaceDefinition.class::cast)
                .toList();
    }

    public List<CardFaceDefinition> getByCardTypeId(CardTypeId cardTypeId) {
        return schemaCacheService.getBySecondaryIndex(cardTypeId, SchemaType.CARD_FACE)
                .stream()
                .filter(CardFaceDefinition.class::isInstance)
                .map(CardFaceDefinition.class::cast)
                .toList();
    }
}
