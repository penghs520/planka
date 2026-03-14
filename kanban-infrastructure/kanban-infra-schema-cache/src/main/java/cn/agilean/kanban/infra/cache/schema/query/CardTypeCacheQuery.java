package cn.agilean.kanban.infra.cache.schema.query;

import cn.agilean.kanban.domain.card.CardTypeId;
import cn.agilean.kanban.domain.schema.SchemaType;
import cn.agilean.kanban.domain.schema.definition.AbstractSchemaDefinition;
import cn.agilean.kanban.domain.schema.definition.cardtype.CardTypeDefinition;
import cn.agilean.kanban.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CardTypeCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public CardTypeCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<CardTypeDefinition> getById(CardTypeId id) {
        return schemaCacheService.getById(id)
                .filter(CardTypeDefinition.class::isInstance)
                .map(CardTypeDefinition.class::cast);
    }

    public String getCardTypeName(CardTypeId id) {
        return schemaCacheService.getById(id)
                .filter(CardTypeDefinition.class::isInstance)
                .map(CardTypeDefinition.class::cast)
                .map(AbstractSchemaDefinition::getName)
                .orElse(null);
    }

    public List<CardTypeDefinition> getByIds(Set<CardTypeId> ids) {
        Set<String> stringIds = ids.stream()
                .map(CardTypeId::value)
                .collect(Collectors.toSet());
        return schemaCacheService.getByIds(stringIds).values().stream()
                .filter(CardTypeDefinition.class::isInstance)
                .map(CardTypeDefinition.class::cast)
                .toList();
    }

    public List<CardTypeDefinition> getByOrgId(String orgId) {
        return schemaCacheService.getBySecondaryIndex("ORG", orgId, SchemaType.CARD_TYPE)
                .stream()
                .filter(CardTypeDefinition.class::isInstance)
                .map(CardTypeDefinition.class::cast)
                .toList();
    }
}
