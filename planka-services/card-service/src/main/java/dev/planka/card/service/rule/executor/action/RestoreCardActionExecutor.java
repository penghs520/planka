package dev.planka.card.service.rule.executor.action;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.service.core.CardService;
import dev.planka.card.service.rule.executor.ActionTargetResolver;
import dev.planka.card.service.rule.executor.RuleExecutionContext;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.schema.definition.rule.action.ActionTargetSelector;
import dev.planka.domain.schema.definition.rule.action.RestoreCardAction;
import org.springframework.stereotype.Component;

/**
 * 还原卡片动作执行器
 */
@Component
public class RestoreCardActionExecutor extends AbstractRuleActionExecutor<RestoreCardAction> {

    private final CardService cardService;

    public RestoreCardActionExecutor(ActionTargetResolver targetResolver, CardService cardService) {
        super(targetResolver);
        this.cardService = cardService;
    }

    @Override
    public String getActionType() {
        return "RESTORE_CARD";
    }

    @Override
    protected ActionTargetSelector getTargetSelector(RestoreCardAction action) {
        return action.getTarget();
    }

    @Override
    protected CardId executeOnCard(RestoreCardAction action, CardDTO targetCard, RuleExecutionContext context) {
        Result<Void> result = cardService.restore(
                targetCard.getId(),
                CardId.of(context.getOperatorId()),
                context.getSourceIp()
        );

        if (!result.isSuccess()) {
            throw new RuntimeException("还原卡片失败: " + result.getMessage());
        }

        return targetCard.getId();
    }
}
