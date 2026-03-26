package cn.planka.card.workflow.engine;

import cn.planka.card.workflow.entity.*;
import cn.planka.card.workflow.executor.NodeExecutorRegistry;
import cn.planka.card.workflow.repository.WorkflowEventLogMapper;
import cn.planka.card.workflow.repository.WorkflowInstanceMapper;
import cn.planka.card.workflow.repository.WorkflowNodeInstanceMapper;
import cn.planka.common.util.SnowflakeIdGenerator;
import cn.planka.domain.schema.definition.workflow.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流引擎
 * <p>
 * 负责流程实例的启动、节点推进和完成。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEngine {

    private final WorkflowInstanceMapper instanceMapper;
    private final WorkflowNodeInstanceMapper nodeInstanceMapper;
    private final WorkflowEventLogMapper eventLogMapper;
    private final NodeExecutorRegistry nodeExecutorRegistry;
    private final ObjectMapper objectMapper;

    /**
     * 启动工作流实例
     */
    @Transactional
    public Long startWorkflow(WorkflowDefinition definition, Long cardId,
                               Long orgId, Long initiatorId) {
        // 校验流程定义
        definition.validate();

        Long instanceId = SnowflakeIdGenerator.generate();
        String definitionJson = serializeDefinition(definition);

        // 查找开始节点
        NodeDefinition startNode = definition.getNodes().stream()
                .filter(n -> n instanceof StartNodeDefinition)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("工作流缺少开始节点"));

        // 创建流程实例
        WorkflowInstanceEntity instance = new WorkflowInstanceEntity();
        instance.setId(instanceId);
        instance.setOrgId(orgId);
        instance.setWorkflowId(definition.getId().value());
        instance.setCardId(cardId);
        instance.setCardTypeId(definition.getCardTypeId().value());
        instance.setStatus(WorkflowInstanceStatus.RUNNING.name());
        instance.setTriggerType("MANUAL");
        instance.setCurrentNodeIds("[\"" + startNode.id() + "\"]");
        instance.setDefinitionSnapshot(definitionJson);
        instance.setStartedBy(initiatorId);
        instance.setStartedAt(LocalDateTime.now());
        instanceMapper.insert(instance);

        // 记录事件日志
        logEvent(instanceId, "WORKFLOW_STARTED", null, null, initiatorId);

        // 构建执行上下文
        WorkflowExecutionContext context = WorkflowExecutionContext.builder()
                .traceId(String.valueOf(instanceId))
                .instanceId(instanceId)
                .cardId(cardId)
                .cardTypeId(definition.getCardTypeId().value())
                .orgId(orgId)
                .initiatorId(initiatorId)
                .workflowId(definition.getId().value())
                .build();

        // 执行开始节点并推进
        advanceFromNode(instance, definition, startNode, context);

        log.info("工作流实例已启动: instanceId={}, workflowId={}, cardId={}",
                instanceId, definition.getId().value(), cardId);
        return instanceId;
    }

    /**
     * 从指定节点推进流程
     */
    @Transactional
    public void advanceFromNode(WorkflowInstanceEntity instance, WorkflowDefinition definition,
                                  NodeDefinition currentNode, WorkflowExecutionContext context) {
        // 创建节点实例
        WorkflowNodeInstanceEntity nodeInstance = createNodeInstance(
                instance.getId(), currentNode);

        // 执行当前节点
        NodeExecutionResult result = nodeExecutorRegistry.executeNode(currentNode, context);

        if (!result.isSuccess()) {
            // 节点执行失败
            nodeInstance.setStatus(NodeInstanceStatus.FAILED.name());
            nodeInstance.setErrorMessage(result.getErrorMessage());
            nodeInstance.setCompletedAt(LocalDateTime.now());
            nodeInstanceMapper.updateStatusWithError(nodeInstance);

            logEvent(instance.getId(), "NODE_FAILED", currentNode.id(), null, null);
            log.error("节点执行失败: instanceId={}, nodeId={}, error={}",
                    instance.getId(), currentNode.id(), result.getErrorMessage());
            return;
        }

        if (result.isWaitForExternal()) {
            // 等待外部操作（如审批）
            nodeInstance.setStatus(NodeInstanceStatus.ACTIVE.name());
            nodeInstanceMapper.updateStatus(nodeInstance);

            // 更新流程实例的当前节点
            instance.setCurrentNodeIds("[\"" + currentNode.id() + "\"]");
            instanceMapper.updateStatusWithVersion(instance);

            logEvent(instance.getId(), "NODE_WAITING", currentNode.id(), null, null);
            return;
        }

        // 节点执行成功，标记完成
        nodeInstance.setStatus(NodeInstanceStatus.COMPLETED.name());
        nodeInstance.setCompletedAt(LocalDateTime.now());
        nodeInstanceMapper.updateStatus(nodeInstance);

        logEvent(instance.getId(), "NODE_COMPLETED", currentNode.id(), null, null);

        // 处理结束节点
        if (currentNode instanceof EndNodeDefinition) {
            completeWorkflow(instance, context.getInitiatorId());
            return;
        }

        // 查找下一个节点并推进
        List<String> nextNodeIds = findNextNodeIds(definition, currentNode.id());
        if (nextNodeIds.isEmpty()) {
            log.warn("节点无后续节点: instanceId={}, nodeId={}", instance.getId(), currentNode.id());
            return;
        }

        // MVP 线性流程只支持一个后续节点
        String nextNodeId = nextNodeIds.get(0);
        NodeDefinition nextNode = findNodeById(definition, nextNodeId);
        if (nextNode == null) {
            log.error("后续节点不存在: instanceId={}, nextNodeId={}", instance.getId(), nextNodeId);
            return;
        }

        // 递归推进
        advanceFromNode(instance, definition, nextNode, context);
    }

    /**
     * 审批完成后推进流程
     */
    @Transactional
    public void onApprovalCompleted(Long instanceId, String nodeId) {
        WorkflowInstanceEntity instance = instanceMapper.selectById(instanceId);
        if (instance == null || !WorkflowInstanceStatus.RUNNING.name().equals(instance.getStatus())) {
            log.warn("流程实例不存在或已结束: instanceId={}", instanceId);
            return;
        }

        WorkflowDefinition definition = deserializeDefinition(instance.getDefinitionSnapshot());
        if (definition == null) {
            log.error("反序列化流程定义失败: instanceId={}", instanceId);
            return;
        }

        NodeDefinition currentNode = findNodeById(definition, nodeId);
        if (currentNode == null) {
            log.error("节点不存在: instanceId={}, nodeId={}", instanceId, nodeId);
            return;
        }

        // 标记当前节点完成
        WorkflowNodeInstanceEntity nodeInstance =
                nodeInstanceMapper.selectByInstanceIdAndNodeId(instanceId, nodeId);
        if (nodeInstance != null) {
            nodeInstance.setStatus(NodeInstanceStatus.COMPLETED.name());
            nodeInstance.setCompletedAt(LocalDateTime.now());
            nodeInstanceMapper.updateStatus(nodeInstance);
        }

        logEvent(instanceId, "NODE_COMPLETED", nodeId, null, null);

        // 查找下一个节点并推进
        List<String> nextNodeIds = findNextNodeIds(definition, nodeId);
        if (nextNodeIds.isEmpty()) {
            return;
        }

        String nextNodeId = nextNodeIds.get(0);
        NodeDefinition nextNode = findNodeById(definition, nextNodeId);
        if (nextNode == null) {
            return;
        }

        WorkflowExecutionContext context = WorkflowExecutionContext.builder()
                .traceId(String.valueOf(instanceId))
                .instanceId(instanceId)
                .cardId(instance.getCardId())
                .cardTypeId(instance.getCardTypeId())
                .orgId(instance.getOrgId())
                .initiatorId(instance.getStartedBy())
                .workflowId(instance.getWorkflowId())
                .build();

        advanceFromNode(instance, definition, nextNode, context);
    }

    /**
     * 完成工作流
     */
    private void completeWorkflow(WorkflowInstanceEntity instance, Long completedBy) {
        instance.setStatus(WorkflowInstanceStatus.COMPLETED.name());
        instance.setCompletedBy(completedBy);
        instance.setCompletedAt(LocalDateTime.now());
        instanceMapper.completeInstance(instance);

        logEvent(instance.getId(), "WORKFLOW_COMPLETED", null, null, completedBy);
        log.info("工作流实例已完成: instanceId={}", instance.getId());
    }

    /**
     * 取消工作流
     */
    @Transactional
    public void cancelWorkflow(Long instanceId, String reason, Long operatorId) {
        WorkflowInstanceEntity instance = instanceMapper.selectById(instanceId);
        if (instance == null || !WorkflowInstanceStatus.RUNNING.name().equals(instance.getStatus())) {
            throw new IllegalStateException("流程实例不存在或不在运行中");
        }
        instanceMapper.cancelInstance(instanceId, reason, instance.getVersion());
        logEvent(instanceId, "WORKFLOW_CANCELLED", null, null, operatorId);
        log.info("工作流实例已取消: instanceId={}, reason={}", instanceId, reason);
    }

    private WorkflowNodeInstanceEntity createNodeInstance(Long instanceId, NodeDefinition node) {
        WorkflowNodeInstanceEntity entity = new WorkflowNodeInstanceEntity();
        entity.setId(SnowflakeIdGenerator.generate());
        entity.setInstanceId(instanceId);
        entity.setNodeId(node.id());
        entity.setNodeType(node.getClass().getSimpleName().replace("NodeDefinition", "").toUpperCase());
        entity.setNodeName(node.name());
        entity.setStatus(NodeInstanceStatus.ACTIVE.name());
        entity.setStartedAt(LocalDateTime.now());
        nodeInstanceMapper.insert(entity);
        return entity;
    }

    private List<String> findNextNodeIds(WorkflowDefinition definition, String currentNodeId) {
        return definition.getEdges().stream()
                .filter(e -> e.sourceNodeId().equals(currentNodeId))
                .map(EdgeDefinition::targetNodeId)
                .toList();
    }

    private NodeDefinition findNodeById(WorkflowDefinition definition, String nodeId) {
        return definition.getNodes().stream()
                .filter(n -> n.id().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    private void logEvent(Long instanceId, String eventType, String nodeId,
                           Long taskId, Long operatorId) {
        WorkflowEventLogEntity logEntity = new WorkflowEventLogEntity();
        logEntity.setInstanceId(instanceId);
        logEntity.setEventType(eventType);
        logEntity.setNodeId(nodeId);
        logEntity.setTaskId(taskId);
        logEntity.setOperatorId(operatorId);
        logEntity.setOccurredAt(LocalDateTime.now());
        eventLogMapper.insert(logEntity);
    }

    private String serializeDefinition(WorkflowDefinition definition) {
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化流程定义失败", e);
        }
    }

    private WorkflowDefinition deserializeDefinition(String json) {
        try {
            return objectMapper.readValue(json, WorkflowDefinition.class);
        } catch (JsonProcessingException e) {
            log.error("反序列化流程定义失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
