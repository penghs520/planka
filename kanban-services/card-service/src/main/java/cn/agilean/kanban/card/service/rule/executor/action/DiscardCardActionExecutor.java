package cn.agilean.kanban.card.service.rule.executor.action;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.card.service.core.CardService;
import cn.agilean.kanban.card.service.rule.executor.ActionTargetResolver;
import cn.agilean.kanban.infra.expression.TextExpressionTemplateResolver;
import cn.agilean.kanban.card.service.rule.executor.RuleExecutionContext;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.domain.schema.definition.rule.action.ActionTargetSelector;
import cn.agilean.kanban.domain.schema.definition.rule.action.DiscardCardAction;
import org.springframework.stereotype.Component;

/**
 * 丢弃卡片动作执行器
 */
@Component
public class DiscardCardActionExecutor extends AbstractRuleActionExecutor<DiscardCardAction> {

    private final CardService cardService;
    private final TextExpressionTemplateResolver templateResolver;

    public DiscardCardActionExecutor(ActionTargetResolver targetResolver, CardService cardService,
                                      TextExpressionTemplateResolver templateResolver) {
        super(targetResolver);
        this.cardService = cardService;
        this.templateResolver = templateResolver;
    }

    @Override
    public String getActionType() {
        return "DISCARD_CARD";
    }

    @Override
    protected ActionTargetSelector getTargetSelector(DiscardCardAction action) {
        return action.getTarget();
    }

    @Override
    protected CardId executeOnCard(DiscardCardAction action, CardDTO targetCard, RuleExecutionContext context) {
        CardId memberCardId = context.getOperatorId() != null ? CardId.of(context.getOperatorId()) : null;
        String reason = action.getReasonTemplate() != null
                ? templateResolver.resolve(action.getReasonTemplate(), context.getCardId(), memberCardId)
                : "由业务规则自动丢弃";
        Result<Void> result = cardService.discard(
                targetCard.getId(),
                reason,
                CardId.of(context.getOperatorId()),
                context.getSourceIp()
        );

        if (!result.isSuccess()) {
            throw new RuntimeException("丢弃卡片失败: " + result.getMessage());
        }

        return targetCard.getId();
    }
}
