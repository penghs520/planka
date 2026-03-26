package cn.planka.domain.schema.definition.workflow;

import cn.planka.domain.schema.definition.rule.action.RuleAction;
import java.util.List;

/**
 * 自动执行节点定义
 */
public record AutoActionNodeDefinition(
    String id,
    String name,
    List<RuleAction> actions,
    FailureStrategy failureStrategy
) implements NodeDefinition {
}
