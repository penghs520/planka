package dev.planka.card.service.rule.executor.action;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.service.rule.executor.ActionTargetResolver;
import dev.planka.card.service.rule.executor.RuleExecutionContext;
import dev.planka.domain.card.CardId;
import dev.planka.event.comment.BizRuleOperationSource;
import dev.planka.event.comment.OperationSource;
import dev.planka.domain.schema.definition.rule.BizRuleDefinition;
import dev.planka.domain.schema.definition.rule.action.ActionTargetSelector;
import dev.planka.domain.schema.definition.rule.action.CommentCardAction;
import dev.planka.event.comment.CommentCreationRequestedEvent;
import dev.planka.event.publisher.EventPublisher;
import dev.planka.infra.expression.TextExpressionTemplateResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 评论卡片动作执行器
 */
@Slf4j
@Component
public class CommentCardActionExecutor extends AbstractRuleActionExecutor<CommentCardAction> {

    private final TextExpressionTemplateResolver templateResolver;
    private final EventPublisher eventPublisher;

    public CommentCardActionExecutor(ActionTargetResolver targetResolver,
                                      TextExpressionTemplateResolver templateResolver,
                                      EventPublisher eventPublisher) {
        super(targetResolver);
        this.templateResolver = templateResolver;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String getActionType() {
        return "COMMENT_CARD";
    }

    @Override
    protected ActionTargetSelector getTargetSelector(CommentCardAction action) {
        return action.getTarget();
    }

    @Override
    protected CardId executeOnCard(CommentCardAction action, CardDTO targetCard, RuleExecutionContext context) {
        // 解析评论内容模板
        CardId memberCardId = context.getOperatorId() != null ? CardId.of(Long.parseLong(context.getOperatorId())) : null;
        String content = templateResolver.resolve(action.getContentTemplate(), context.getCardId(), memberCardId);

        if (content == null || content.trim().isEmpty()) {
            log.warn("评论内容为空，跳过: cardId={}", targetCard.getId());
            return null;
        }

        // 构建操作来源信息（业务规则）
        OperationSource operationSource = buildOperationSource(context);

        // 发布评论创建请求事件，由评论服务监听并处理
        CommentCreationRequestedEvent event = new CommentCreationRequestedEvent(
            context.getOrgId(),
            context.getOperatorId(),
            context.getSourceIp(),
            context.getTraceId(),
            targetCard.getId().toString(),
            targetCard.getTypeId().toString(),
            content,
            operationSource
        );

        eventPublisher.publishAsync(event);
        log.debug("已发布评论创建请求事件: cardId={}, contentLength={}", targetCard.getId(), content.length());

        return targetCard.getId();
    }

    /**
     * 构建操作来源信息
     *
     * @param context 规则执行上下文
     * @return 业务规则操作来源，如果不是规则触发则返回 null
     */
    private OperationSource buildOperationSource(RuleExecutionContext context) {
        BizRuleDefinition currentRule = context.getCurrentRule();
        if (currentRule == null) {
            return null;
        }
        return new BizRuleOperationSource(
            currentRule.getId() != null ? currentRule.getId().toString() : null,
            currentRule.getName()
        );
    }
}
