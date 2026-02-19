package dev.planka.infra.cache.schema.query;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.stream.ValueStreamDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ValueStreamCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public ValueStreamCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<ValueStreamDefinition> getValueStreamByCardTypeId(CardTypeId cardTypeId) {
        return schemaCacheService.getBySecondaryIndex(cardTypeId, SchemaType.VALUE_STREAM)
                .stream()
                .filter(ValueStreamDefinition.class::isInstance)
                .map(ValueStreamDefinition.class::cast)
                .findFirst();
    }

    public Optional<ValueStreamDefinition> getValueStreamByCardTypeId(String cardTypeId) {
        return getValueStreamByCardTypeId(CardTypeId.of(cardTypeId));
    }
}
