package cn.agilean.kanban.infra.cache.schema.query;

import cn.agilean.kanban.domain.card.CardTypeId;
import cn.agilean.kanban.domain.schema.SchemaType;
import cn.agilean.kanban.domain.schema.definition.template.CardDetailTemplateDefinition;
import cn.agilean.kanban.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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
