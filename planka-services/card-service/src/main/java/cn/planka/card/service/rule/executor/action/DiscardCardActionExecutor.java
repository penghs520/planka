package cn.planka.card.service.rule.executor.action;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.card.service.core.CardService;
import cn.planka.card.service.rule.executor.ActionTargetResolver;
import cn.planka.infra.expression.TextExpressionTemplateResolver;
import cn.planka.card.service.rule.executor.RuleExecutionContext;
import cn.planka.common.result.Result;
import cn.planka.domain.card.CardId;
import cn.planka.domain.schema.definition.rule.action.ActionTargetSelector;
import cn.planka.domain.schema.definition.rule.action.DiscardCardAction;
import org.springframework.stereotype.Component;

/**
 * 回收卡片动作执行器
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
                : "由业务规则自动回收";
        Result<Void> result = cardService.discard(
                targetCard.getId(),
                reason,
                CardId.of(context.getOperatorId()),
                context.getSourceIp()
        );

        if (!result.isSuccess()) {
            throw new RuntimeException("回收卡片失败: " + result.getMessage());
        }

        return targetCard.getId();
    }
}
