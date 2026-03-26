package cn.planka.card.workflow.executor;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.card.repository.CardRepository;
import cn.planka.card.service.rule.executor.RuleActionExecutorRegistry;
import cn.planka.card.service.rule.executor.RuleExecutionContext;
import cn.planka.card.service.rule.executor.RuleExecutionResult;
import cn.planka.card.workflow.engine.NodeExecutionResult;
import cn.planka.card.workflow.engine.WorkflowExecutionContext;
import cn.planka.domain.card.CardId;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.schema.definition.rule.BizRuleDefinition;
import cn.planka.domain.schema.definition.rule.action.RuleAction;
import cn.planka.domain.schema.definition.workflow.AutoActionNodeDefinition;
import cn.planka.domain.schema.definition.workflow.FailureStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import cn.planka.api.card.request.Yield;

/**
 * 自动执行节点执行器
 * <p>
 * 本地调用 RuleActionExecutorRegistry 执行 RuleAction，零网络开销。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoActionNodeExecutor implements NodeExecutor<AutoActionNodeDefinition> {

    private final RuleActionExecutorRegistry executorRegistry;
    private final CardRepository cardRepository;

    @Override
    public NodeExecutionResult execute(AutoActionNodeDefinition node, WorkflowExecutionContext context) {
        log.info("执行自动执行节点: instanceId={}, nodeId={}, nodeName={}, actions={}",
                context.getInstanceId(), node.id(), node.name(),
                node.actions() != null ? node.actions().size() : 0);

        if (node.actions() == null || node.actions().isEmpty()) {
            log.info("自动执行节点无动作，跳过: nodeId={}", node.id());
            return NodeExecutionResult.success();
        }

        // 查询触发卡片
        CardDTO triggerCard = cardRepository.findById(
                CardId.of(String.valueOf(context.getCardId())),
                Yield.all(),
                String.valueOf(context.getInitiatorId())
        ).orElse(null);

        if (triggerCard == null) {
            log.error("触发卡片不存在: cardId={}", context.getCardId());
            return NodeExecutionResult.failed("触发卡片不存在: " + context.getCardId());
        }

        // 构建 RuleExecutionContext（桥接）
        RuleExecutionContext ruleContext = RuleExecutionContext.builder()
                .traceId(context.getTraceId())
                .triggerCard(triggerCard)
                .cardId(triggerCard.getId())
                .cardTypeId(CardTypeId.of(context.getCardTypeId()))
                .operatorId(String.valueOf(context.getInitiatorId()))
                .orgId(String.valueOf(context.getOrgId()))
                .triggerEvent(BizRuleDefinition.TriggerEvent.ON_FIELD_CHANGE)
                .build();

        // 执行所有动作
        for (RuleAction action : node.actions()) {
            RuleExecutionResult.ActionExecutionResult result =
                    executorRegistry.executeAction(action, ruleContext);

            if (!result.isSuccess()) {
                if (node.failureStrategy() == FailureStrategy.BLOCK_WORKFLOW) {
                    log.error("动作执行失败，阻塞流程: nodeId={}, actionType={}, error={}",
                            node.id(), action.getActionType(), result.getErrorMessage());
                    return NodeExecutionResult.failed(result.getErrorMessage());
                }
                // CONTINUE 策略：记录错误但继续
                log.warn("动作执行失败但继续: nodeId={}, actionType={}, error={}",
                        node.id(), action.getActionType(), result.getErrorMessage());
            }
        }

        return NodeExecutionResult.success();
    }

    @Override
    public Class<AutoActionNodeDefinition> getSupportedType() {
        return AutoActionNodeDefinition.class;
    }
}
