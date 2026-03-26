package cn.planka.card.workflow.executor;

import cn.planka.card.workflow.engine.NodeExecutionResult;
import cn.planka.card.workflow.engine.WorkflowExecutionContext;
import cn.planka.domain.schema.definition.workflow.NodeDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行器注册表
 */
@Slf4j
@Component
public class NodeExecutorRegistry {

    private final Map<Class<?>, NodeExecutor<?>> executorMap = new HashMap<>();

    public NodeExecutorRegistry(List<NodeExecutor<?>> executors) {
        for (NodeExecutor<?> executor : executors) {
            Class<?> type = executor.getSupportedType();
            if (executorMap.containsKey(type)) {
                log.warn("节点执行器类型重复注册: type={}, existing={}, new={}",
                        type.getSimpleName(),
                        executorMap.get(type).getClass().getSimpleName(),
                        executor.getClass().getSimpleName());
            }
            executorMap.put(type, executor);
            log.debug("注册节点执行器: type={}, executor={}",
                    type.getSimpleName(), executor.getClass().getSimpleName());
        }
        log.info("节点执行器注册完成，共注册 {} 个执行器", executorMap.size());
    }

    /**
     * 执行节点
     */
    @SuppressWarnings("unchecked")
    public NodeExecutionResult executeNode(NodeDefinition node, WorkflowExecutionContext context) {
        NodeExecutor<NodeDefinition> executor = (NodeExecutor<NodeDefinition>) executorMap.get(node.getClass());
        if (executor == null) {
            log.error("未找到节点执行器: nodeType={}", node.getClass().getSimpleName());
            return NodeExecutionResult.failed("未找到节点执行器: " + node.getClass().getSimpleName());
        }

        try {
            return executor.execute(node, context);
        } catch (Exception e) {
            log.error("执行节点失败: nodeId={}, nodeType={}, error={}",
                    node.id(), node.getClass().getSimpleName(), e.getMessage(), e);
            return NodeExecutionResult.failed(e.getMessage());
        }
    }
}
