package cn.planka.card.workflow.service;

import cn.planka.card.workflow.engine.WorkflowEngine;
import cn.planka.card.workflow.entity.ApprovalTaskStatus;
import cn.planka.card.workflow.entity.WorkflowApprovalTaskEntity;
import cn.planka.card.workflow.entity.WorkflowInstanceEntity;
import cn.planka.card.workflow.repository.WorkflowApprovalTaskMapper;
import cn.planka.card.workflow.repository.WorkflowInstanceMapper;
import cn.planka.common.util.SnowflakeIdGenerator;
import cn.planka.domain.schema.definition.workflow.ApprovalMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalTaskService {

    private final WorkflowApprovalTaskMapper approvalTaskMapper;
    private final WorkflowInstanceMapper instanceMapper;
    private final WorkflowEngine workflowEngine;

    /**
     * 审批通过
     */
    @Transactional
    public void approve(Long taskId, Long operatorId, String comment) {
        WorkflowApprovalTaskEntity task = approvalTaskMapper.selectById(taskId);
        validateTaskOperation(task, operatorId);

        task.setStatus(ApprovalTaskStatus.APPROVED.name());
        task.setComment(comment);
        task.setApprovedAt(LocalDateTime.now());
        approvalTaskMapper.updateStatus(task);

        log.info("审批通过: taskId={}, operatorId={}", taskId, operatorId);

        // 检查是否所有审批任务都完成
        checkAndAdvance(task);
    }

    /**
     * 审批拒绝
     */
    @Transactional
    public void reject(Long taskId, Long operatorId, String comment) {
        WorkflowApprovalTaskEntity task = approvalTaskMapper.selectById(taskId);
        validateTaskOperation(task, operatorId);

        task.setStatus(ApprovalTaskStatus.REJECTED.name());
        task.setComment(comment);
        task.setApprovedAt(LocalDateTime.now());
        approvalTaskMapper.updateStatus(task);

        log.info("审批拒绝: taskId={}, operatorId={}", taskId, operatorId);

        // 拒绝时取消流程
        workflowEngine.cancelWorkflow(task.getInstanceId(),
                "审批被拒绝: " + (comment != null ? comment : "无审批意见"), operatorId);
    }

    /**
     * 转交审批
     */
    @Transactional
    public void transfer(Long taskId, Long operatorId, Long transferToId) {
        WorkflowApprovalTaskEntity task = approvalTaskMapper.selectById(taskId);
        validateTaskOperation(task, operatorId);

        // 标记原任务为已转交
        approvalTaskMapper.transferTask(taskId, transferToId);

        // 创建新的审批任务
        WorkflowApprovalTaskEntity newTask = new WorkflowApprovalTaskEntity();
        newTask.setId(SnowflakeIdGenerator.generate());
        newTask.setOrgId(task.getOrgId());
        newTask.setInstanceId(task.getInstanceId());
        newTask.setNodeId(task.getNodeId());
        newTask.setCardId(task.getCardId());
        newTask.setApproverId(transferToId);
        newTask.setStatus(ApprovalTaskStatus.PENDING.name());
        newTask.setApprovalMode(task.getApprovalMode());
        approvalTaskMapper.insert(newTask);

        log.info("审批转交: taskId={}, from={}, to={}", taskId, operatorId, transferToId);
    }

    /**
     * 查询待审批任务
     */
    public List<WorkflowApprovalTaskEntity> getPendingTasks(Long approverId, Long orgId) {
        return approvalTaskMapper.selectPendingByApproverId(approverId, orgId);
    }

    private void validateTaskOperation(WorkflowApprovalTaskEntity task, Long operatorId) {
        if (task == null) {
            throw new IllegalArgumentException("审批任务不存在");
        }
        if (!ApprovalTaskStatus.PENDING.name().equals(task.getStatus())) {
            throw new IllegalStateException("审批任务状态不是待审批");
        }
        if (!task.getApproverId().equals(operatorId)) {
            throw new IllegalArgumentException("当前用户不是审批人");
        }
    }

    private void checkAndAdvance(WorkflowApprovalTaskEntity completedTask) {
        List<WorkflowApprovalTaskEntity> allTasks = approvalTaskMapper
                .selectByInstanceIdAndNodeId(completedTask.getInstanceId(), completedTask.getNodeId());

        ApprovalMode mode = ApprovalMode.valueOf(completedTask.getApprovalMode());

        boolean shouldAdvance = switch (mode) {
            case ANY_ONE -> true; // 或签：任意一人通过即推进
            case ALL_REQUIRED -> allTasks.stream()
                    .allMatch(t -> ApprovalTaskStatus.APPROVED.name().equals(t.getStatus()));
        };

        if (shouldAdvance) {
            workflowEngine.onApprovalCompleted(
                    completedTask.getInstanceId(), completedTask.getNodeId());
        }
    }
}
