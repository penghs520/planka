package cn.planka.card.service.rule.executor.action;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.card.service.core.CardService;
import cn.planka.card.service.rule.executor.ActionTargetResolver;
import cn.planka.card.service.rule.executor.RuleExecutionContext;
import cn.planka.common.result.Result;
import cn.planka.domain.card.CardId;
import cn.planka.domain.schema.definition.rule.action.ActionTargetSelector;
import cn.planka.domain.schema.definition.rule.action.RestoreCardAction;
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
