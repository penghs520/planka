package dev.planka.infra.cache.schema.query;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.CardCreatePageTemplateId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.template.CardCreatePageTemplateDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CardCreatePageTemplateCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public CardCreatePageTemplateCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<CardCreatePageTemplateDefinition> getById(CardCreatePageTemplateId id) {
        return schemaCacheService.getById(id)
                .filter(CardCreatePageTemplateDefinition.class::isInstance)
                .map(CardCreatePageTemplateDefinition.class::cast);
    }

    public List<CardCreatePageTemplateDefinition> getByIds(Set<CardCreatePageTemplateId> ids) {
        Set<String> stringIds = ids.stream()
                .map(CardCreatePageTemplateId::value)
                .collect(Collectors.toSet());
        return schemaCacheService.getByIds(stringIds).values().stream()
                .filter(CardCreatePageTemplateDefinition.class::isInstance)
                .map(CardCreatePageTemplateDefinition.class::cast)
                .toList();
    }

    public List<CardCreatePageTemplateDefinition> getByCardTypeId(CardTypeId cardTypeId) {
        return schemaCacheService.getBySecondaryIndex(cardTypeId, SchemaType.CARD_CREATE_PAGE_TEMPLATE)
                .stream()
                .filter(CardCreatePageTemplateDefinition.class::isInstance)
                .map(CardCreatePageTemplateDefinition.class::cast)
                .toList();
    }
}
