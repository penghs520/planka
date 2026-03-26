package cn.planka.card.workflow.executor;

import cn.planka.card.workflow.engine.NodeExecutionResult;
import cn.planka.card.workflow.engine.WorkflowExecutionContext;
import cn.planka.domain.schema.definition.workflow.StartNodeDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 开始节点执行器
 */
@Slf4j
@Component
public class StartNodeExecutor implements NodeExecutor<StartNodeDefinition> {

    @Override
    public NodeExecutionResult execute(StartNodeDefinition node, WorkflowExecutionContext context) {
        log.info("执行开始节点: instanceId={}, nodeId={}", context.getInstanceId(), node.id());
        return NodeExecutionResult.success();
    }

    @Override
    public Class<StartNodeDefinition> getSupportedType() {
        return StartNodeDefinition.class;
    }
}
