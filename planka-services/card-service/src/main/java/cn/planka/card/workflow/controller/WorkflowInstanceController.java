package cn.planka.card.workflow.controller;

import cn.planka.card.workflow.entity.WorkflowApprovalTaskEntity;
import cn.planka.card.workflow.entity.WorkflowInstanceEntity;
import cn.planka.card.workflow.entity.WorkflowNodeInstanceEntity;
import cn.planka.card.workflow.service.ApprovalTaskService;
import cn.planka.card.workflow.service.WorkflowInstanceService;
import cn.planka.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流实例控制器
 */
@RestController
@RequestMapping("/api/v1/workflow/instances")
@RequiredArgsConstructor
public class WorkflowInstanceController {

    private final WorkflowInstanceService workflowInstanceService;

    /**
     * 手动启动流程
     */
    @PostMapping("/start")
    public Result<Long> startWorkflow(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody StartWorkflowRequest request) {
        Long instanceId = workflowInstanceService.startWorkflow(
                request.workflowId(),
                request.cardId(),
                Long.valueOf(orgId),
                Long.valueOf(operatorId));
        return Result.success(instanceId);
    }

    /**
     * 取消流程
     */
    @PostMapping("/{instanceId}/cancel")
    public Result<Void> cancelWorkflow(
            @PathVariable Long instanceId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody(required = false) CancelWorkflowRequest request) {
        workflowInstanceService.cancelWorkflow(
                instanceId,
                request != null ? request.reason() : null,
                Long.valueOf(operatorId));
        return Result.success(null);
    }

    /**
     * 查询流程实例详情
     */
    @GetMapping("/{instanceId}")
    public Result<WorkflowInstanceEntity> getInstance(@PathVariable Long instanceId) {
        return Result.success(workflowInstanceService.getInstance(instanceId));
    }

    /**
     * 查询我发起的流程
     */
    @GetMapping("/my-initiated")
    public Result<List<WorkflowInstanceEntity>> getMyInitiated(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return Result.success(workflowInstanceService.getMyInitiated(
                Long.valueOf(operatorId), Long.valueOf(orgId)));
    }

    /**
     * 查询流程节点实例
     */
    @GetMapping("/{instanceId}/nodes")
    public Result<List<WorkflowNodeInstanceEntity>> getNodeInstances(@PathVariable Long instanceId) {
        return Result.success(workflowInstanceService.getNodeInstances(instanceId));
    }

    /**
     * 查询流程事件日志
     */
    @GetMapping("/{instanceId}/events")
    public Result<List<?>> getEventLogs(@PathVariable Long instanceId) {
        return Result.success(workflowInstanceService.getEventLogs(instanceId));
    }

    public record StartWorkflowRequest(String workflowId, Long cardId) {}
    public record CancelWorkflowRequest(String reason) {}
}
