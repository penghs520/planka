package cn.planka.card.workflow.executor;

import cn.planka.card.workflow.engine.NodeExecutionResult;
import cn.planka.card.workflow.engine.WorkflowExecutionContext;
import cn.planka.domain.schema.definition.workflow.NodeDefinition;

/**
 * 节点执行器接口
 */
public interface NodeExecutor<T extends NodeDefinition> {

    /**
     * 执行节点
     */
    NodeExecutionResult execute(T node, WorkflowExecutionContext context);

    /**
     * 获取支持的节点类型
     */
    Class<T> getSupportedType();
}
