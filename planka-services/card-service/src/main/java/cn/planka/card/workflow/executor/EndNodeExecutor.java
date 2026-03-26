package cn.planka.card.workflow.executor;

import cn.planka.card.workflow.engine.NodeExecutionResult;
import cn.planka.card.workflow.engine.WorkflowExecutionContext;
import cn.planka.domain.schema.definition.workflow.EndNodeDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 结束节点执行器
 */
@Slf4j
@Component
public class EndNodeExecutor implements NodeExecutor<EndNodeDefinition> {

    @Override
    public NodeExecutionResult execute(EndNodeDefinition node, WorkflowExecutionContext context) {
        log.info("执行结束节点: instanceId={}, nodeId={}", context.getInstanceId(), node.id());
        return NodeExecutionResult.success();
    }

    @Override
    public Class<EndNodeDefinition> getSupportedType() {
        return EndNodeDefinition.class;
    }
}
