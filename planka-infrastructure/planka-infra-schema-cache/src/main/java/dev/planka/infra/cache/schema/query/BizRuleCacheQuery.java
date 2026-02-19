package dev.planka.infra.cache.schema.query;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.BizRuleId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.rule.BizRuleDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class BizRuleCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public BizRuleCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<BizRuleDefinition> getById(BizRuleId id) {
        return schemaCacheService.getById(id)
                .filter(BizRuleDefinition.class::isInstance)
                .map(BizRuleDefinition.class::cast);
    }

    public List<BizRuleDefinition> getByIds(Set<String> ids) {
        return schemaCacheService.getByIds(ids).values().stream()
                .filter(BizRuleDefinition.class::isInstance)
                .map(BizRuleDefinition.class::cast)
                .toList();
    }

    public List<BizRuleDefinition> getByCardTypeId(CardTypeId cardTypeId) {
        return schemaCacheService.getBySecondaryIndex(cardTypeId, SchemaType.BIZ_RULE)
                .stream()
                .filter(BizRuleDefinition.class::isInstance)
                .map(BizRuleDefinition.class::cast)
                .toList();
    }
}
