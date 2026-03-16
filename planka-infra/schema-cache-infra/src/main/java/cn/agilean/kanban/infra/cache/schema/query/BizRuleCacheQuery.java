package cn.planka.infra.cache.schema.query;

import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.schema.BizRuleId;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.rule.BizRuleDefinition;
import cn.planka.infra.cache.schema.SchemaCacheService;
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
