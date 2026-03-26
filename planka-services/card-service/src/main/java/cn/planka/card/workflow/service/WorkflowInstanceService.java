package cn.planka.card.workflow.service;

import cn.planka.card.workflow.engine.WorkflowEngine;
import cn.planka.card.workflow.entity.WorkflowInstanceEntity;
import cn.planka.card.workflow.entity.WorkflowInstanceStatus;
import cn.planka.card.workflow.entity.WorkflowNodeInstanceEntity;
import cn.planka.card.workflow.repository.WorkflowEventLogMapper;
import cn.planka.card.workflow.repository.WorkflowInstanceMapper;
import cn.planka.card.workflow.repository.WorkflowNodeInstanceMapper;
import cn.planka.domain.schema.WorkflowId;
import cn.planka.domain.schema.definition.workflow.WorkflowDefinition;
import cn.planka.infra.cache.schema.query.WorkflowCacheQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工作流实例服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowInstanceService {

    private final WorkflowEngine workflowEngine;
    private final WorkflowCacheQuery workflowCacheQuery;
    private final WorkflowInstanceMapper instanceMapper;
    private final WorkflowNodeInstanceMapper nodeInstanceMapper;
    private final WorkflowEventLogMapper eventLogMapper;

    /**
     * 手动触发启动流程
     */
    public Long startWorkflow(String workflowId, Long cardId, Long orgId, Long initiatorId) {
        WorkflowDefinition definition = workflowCacheQuery.getById(WorkflowId.of(workflowId))
                .orElseThrow(() -> new IllegalArgumentException("工作流定义不存在: " + workflowId));

        // 检查是否已有运行中的实例
        List<WorkflowInstanceEntity> runningInstances = instanceMapper
                .selectByCardIdAndStatus(cardId, WorkflowInstanceStatus.RUNNING.name());
        if (!runningInstances.isEmpty()) {
            throw new IllegalStateException("该卡片已有运行中的流程实例");
        }

        return workflowEngine.startWorkflow(definition, cardId, orgId, initiatorId);
    }

    /**
     * 取消流程
     */
    public void cancelWorkflow(Long instanceId, String reason, Long operatorId) {
        workflowEngine.cancelWorkflow(instanceId, reason, operatorId);
    }

    /**
     * 查询流程实例
     */
    public WorkflowInstanceEntity getInstance(Long instanceId) {
        return instanceMapper.selectById(instanceId);
    }

    /**
     * 查询我发起的流程
     */
    public List<WorkflowInstanceEntity> getMyInitiated(Long initiatorId, Long orgId) {
        return instanceMapper.selectByStartedByAndOrgId(initiatorId, orgId);
    }

    /**
     * 查询流程的节点实例列表
     */
    public List<WorkflowNodeInstanceEntity> getNodeInstances(Long instanceId) {
        return nodeInstanceMapper.selectByInstanceId(instanceId);
    }

    /**
     * 查询流程的事件日志
     */
    public List<?> getEventLogs(Long instanceId) {
        return eventLogMapper.selectByInstanceId(instanceId);
    }
}
