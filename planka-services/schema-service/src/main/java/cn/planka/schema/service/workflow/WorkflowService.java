package cn.planka.schema.service.workflow;

import cn.planka.common.result.Result;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.schema.SchemaSubType;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.workflow.WorkflowDefinition;
import cn.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工作流服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final SchemaQuery schemaQuery;

    public Result<List<WorkflowDefinition>> getByCardTypeId(CardTypeId cardTypeId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.queryByBelongTo(
                cardTypeId.value(),
                SchemaSubType.WORKFLOW_DEFINITION
        );

        List<WorkflowDefinition> workflows = schemas.stream()
                .filter(WorkflowDefinition.class::isInstance)
                .map(WorkflowDefinition.class::cast)
                .toList();

        return Result.success(workflows);
    }

    public Result<List<WorkflowDefinition>> getByOrgId(String orgId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.queryPaged(
                orgId,
                SchemaType.WORKFLOW,
                0,
                Integer.MAX_VALUE
        );

        List<WorkflowDefinition> workflows = schemas.stream()
                .filter(WorkflowDefinition.class::isInstance)
                .map(WorkflowDefinition.class::cast)
                .toList();

        return Result.success(workflows);
    }
}
