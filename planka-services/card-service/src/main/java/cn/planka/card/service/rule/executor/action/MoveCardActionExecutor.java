package cn.planka.card.service.rule.executor.action;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.request.MoveCardRequest;
import cn.planka.card.service.core.CardService;
import cn.planka.card.service.rule.executor.ActionTargetResolver;
import cn.planka.card.service.rule.executor.RuleExecutionContext;
import cn.planka.common.result.Result;
import cn.planka.domain.card.CardId;
import cn.planka.domain.schema.definition.rule.action.ActionTargetSelector;
import cn.planka.domain.schema.definition.rule.action.MoveCardAction;
import cn.planka.domain.stream.StatusId;
import org.springframework.stereotype.Component;

/**
 * 移动卡片状态动作执行器
 */
@Component
public class MoveCardActionExecutor extends AbstractRuleActionExecutor<MoveCardAction> {

    private final CardService cardService;

    public MoveCardActionExecutor(ActionTargetResolver targetResolver, CardService cardService) {
        super(targetResolver);
        this.cardService = cardService;
    }

    @Override
    public String getActionType() {
        return "MOVE_CARD";
    }

    @Override
    protected ActionTargetSelector getTargetSelector(MoveCardAction action) {
        return action.getTarget();
    }

    @Override
    protected CardId executeOnCard(MoveCardAction action, CardDTO targetCard, RuleExecutionContext context) {
        StatusId targetStatusId = action.getToStatusId();
        if (targetStatusId == null) {
            throw new IllegalArgumentException("目标状态ID不能为空");
        }

        // 如果当前状态与目标状态相同，跳过
        if (targetCard.getStatusId() != null && targetCard.getStatusId().equals(targetStatusId)) {
            return null;
        }

        MoveCardRequest moveRequest = new MoveCardRequest(
                targetCard.getId(),
                targetCard.getStreamId(),
                targetStatusId
        );

        Result<Void> result = cardService.move(moveRequest, CardId.of(context.getOperatorId()));

        if (!result.isSuccess()) {
            throw new RuntimeException("移动卡片失败: " + result.getMessage());
        }

        return targetCard.getId();
    }
}
