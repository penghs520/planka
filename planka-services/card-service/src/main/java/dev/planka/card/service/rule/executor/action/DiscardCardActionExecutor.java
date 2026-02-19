package dev.planka.card.service.rule.executor.action;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.service.core.CardService;
import dev.planka.card.service.rule.executor.ActionTargetResolver;
import dev.planka.infra.expression.TextExpressionTemplateResolver;
import dev.planka.card.service.rule.executor.RuleExecutionContext;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.schema.definition.rule.action.ActionTargetSelector;
import dev.planka.domain.schema.definition.rule.action.DiscardCardAction;
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
        CardId memberCardId = context.getOperatorId() != null ? CardId.of(Long.parseLong(context.getOperatorId())) : null;
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
