package cn.planka.infra.cache.schema.query;

import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.WorkflowId;
import cn.planka.domain.schema.definition.workflow.WorkflowDefinition;
import cn.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class WorkflowCacheQuery {

    private final SchemaCacheService schemaCacheService;

    public WorkflowCacheQuery(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    public Optional<WorkflowDefinition> getById(WorkflowId id) {
        return schemaCacheService.getById(id)
                .filter(WorkflowDefinition.class::isInstance)
                .map(WorkflowDefinition.class::cast);
    }

    public List<WorkflowDefinition> getByIds(Set<String> ids) {
        return schemaCacheService.getByIds(ids).values().stream()
                .filter(WorkflowDefinition.class::isInstance)
                .map(WorkflowDefinition.class::cast)
                .toList();
    }

    public List<WorkflowDefinition> getByCardTypeId(CardTypeId cardTypeId) {
        return schemaCacheService.getBySecondaryIndex(cardTypeId, SchemaType.WORKFLOW)
                .stream()
                .filter(WorkflowDefinition.class::isInstance)
                .map(WorkflowDefinition.class::cast)
                .toList();
    }
}
