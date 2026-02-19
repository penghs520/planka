package dev.planka.infra.cache.schema.query;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.template.CardDetailTemplateDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CardDetailTemplateCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public CardDetailTemplateCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public List<CardDetailTemplateDefinition> getByCardTypeId(CardTypeId cardTypeId) {
        return schemaCacheService.getBySecondaryIndex(cardTypeId, SchemaType.CARD_DETAIL_TEMPLATE)
                .stream()
                .filter(CardDetailTemplateDefinition.class::isInstance)
                .map(CardDetailTemplateDefinition.class::cast)
                .toList();
    }
}
