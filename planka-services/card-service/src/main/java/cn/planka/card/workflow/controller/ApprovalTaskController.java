package cn.planka.card.workflow.controller;

import cn.planka.card.workflow.entity.WorkflowApprovalTaskEntity;
import cn.planka.card.workflow.service.ApprovalTaskService;
import cn.planka.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批任务控制器
 */
@RestController
@RequestMapping("/api/v1/workflow/approval-tasks")
@RequiredArgsConstructor
public class ApprovalTaskController {

    private final ApprovalTaskService approvalTaskService;

    /**
     * 查询我的待审批任务
     */
    @GetMapping("/pending")
    public Result<List<WorkflowApprovalTaskEntity>> getPendingTasks(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return Result.success(approvalTaskService.getPendingTasks(
                Long.valueOf(operatorId), Long.valueOf(orgId)));
    }

    /**
     * 审批通过
     */
    @PostMapping("/{taskId}/approve")
    public Result<Void> approve(
            @PathVariable Long taskId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody(required = false) ApprovalRequest request) {
        approvalTaskService.approve(taskId, Long.valueOf(operatorId),
                request != null ? request.comment() : null);
        return Result.success(null);
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/{taskId}/reject")
    public Result<Void> reject(
            @PathVariable Long taskId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody(required = false) ApprovalRequest request) {
        approvalTaskService.reject(taskId, Long.valueOf(operatorId),
                request != null ? request.comment() : null);
        return Result.success(null);
    }

    /**
     * 转交审批
     */
    @PostMapping("/{taskId}/transfer")
    public Result<Void> transfer(
            @PathVariable Long taskId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody TransferRequest request) {
        approvalTaskService.transfer(taskId, Long.valueOf(operatorId), request.transferToId());
        return Result.success(null);
    }

    public record ApprovalRequest(String comment) {}
    public record TransferRequest(Long transferToId) {}
}
