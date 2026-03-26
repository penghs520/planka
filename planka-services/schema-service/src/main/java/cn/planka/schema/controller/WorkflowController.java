package cn.planka.schema.controller;

import cn.planka.common.result.Result;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.schema.definition.workflow.WorkflowDefinition;
import cn.planka.schema.service.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流控制器
 */
@RestController
@RequestMapping("/api/v1/schemas/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping("/by-card-type/{cardTypeId}")
    public Result<List<WorkflowDefinition>> getByCardTypeId(
            @PathVariable("cardTypeId") String cardTypeId) {
        return workflowService.getByCardTypeId(CardTypeId.of(cardTypeId));
    }

    @GetMapping
    public Result<List<WorkflowDefinition>> getByOrgId(
            @RequestHeader("X-Org-Id") String orgId) {
        return workflowService.getByOrgId(orgId);
    }
}
