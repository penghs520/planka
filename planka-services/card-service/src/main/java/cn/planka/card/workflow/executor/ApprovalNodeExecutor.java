package cn.planka.card.workflow.executor;

import cn.planka.card.workflow.engine.NodeExecutionResult;
import cn.planka.card.workflow.engine.WorkflowExecutionContext;
import cn.planka.card.workflow.entity.ApprovalTaskStatus;
import cn.planka.card.workflow.entity.WorkflowApprovalTaskEntity;
import cn.planka.card.workflow.repository.WorkflowApprovalTaskMapper;
import cn.planka.common.util.SnowflakeIdGenerator;
import cn.planka.domain.schema.definition.workflow.ApprovalNodeDefinition;
import cn.planka.domain.schema.definition.workflow.ApproverSelector;
import cn.planka.domain.schema.definition.workflow.FixedMembersSelector;
import cn.planka.domain.schema.definition.workflow.RoleBasedSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 审批节点执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalNodeExecutor implements NodeExecutor<ApprovalNodeDefinition> {

    private final WorkflowApprovalTaskMapper approvalTaskMapper;

    @Override
    public NodeExecutionResult execute(ApprovalNodeDefinition node, WorkflowExecutionContext context) {
        log.info("执行审批节点: instanceId={}, nodeId={}, nodeName={}",
                context.getInstanceId(), node.id(), node.name());

        List<Long> approverIds = resolveApproverIds(node.approverSelector(), context);

        if (approverIds.isEmpty()) {
            log.warn("审批节点无审批人: nodeId={}", node.id());
            return NodeExecutionResult.failed("审批节点未配置审批人");
        }

        // 为每个审批人创建审批任务
        for (Long approverId : approverIds) {
            WorkflowApprovalTaskEntity task = new WorkflowApprovalTaskEntity();
            task.setId(SnowflakeIdGenerator.generate());
            task.setOrgId(context.getOrgId());
            task.setInstanceId(context.getInstanceId());
            task.setNodeId(node.id());
            task.setCardId(context.getCardId());
            task.setApproverId(approverId);
            task.setStatus(ApprovalTaskStatus.PENDING.name());
            task.setApprovalMode(node.approvalMode().name());
            approvalTaskMapper.insert(task);
            log.debug("创建审批任务: taskId={}, approverId={}", task.getId(), approverId);
        }

        // 审批节点需要等待外部操作
        return NodeExecutionResult.waitForExternal();
    }

    @Override
    public Class<ApprovalNodeDefinition> getSupportedType() {
        return ApprovalNodeDefinition.class;
    }

    private List<Long> resolveApproverIds(ApproverSelector selector, WorkflowExecutionContext context) {
        if (selector instanceof FixedMembersSelector fixed) {
            return fixed.memberIds();
        } else if (selector instanceof RoleBasedSelector roleBased) {
            return resolveByRoles(roleBased, context);
        }
        throw new IllegalArgumentException("不支持的审批人选择器类型: " + selector.getClass().getName());
    }

    private List<Long> resolveByRoles(RoleBasedSelector selector, WorkflowExecutionContext context) {
        // TODO: Phase 3 实现基于角色的审批人查询
        log.warn("基于角色的审批人选择器暂未实现，roleIds={}", selector.roleIds());
        return List.of();
    }
}
