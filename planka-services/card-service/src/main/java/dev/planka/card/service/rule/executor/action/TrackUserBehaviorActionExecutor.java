package dev.planka.card.service.rule.executor.action;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.service.rule.executor.ActionTargetResolver;
import dev.planka.card.service.rule.executor.RuleExecutionContext;
import dev.planka.card.service.rule.executor.RuleExecutionResult;
import dev.planka.domain.card.CardId;
import dev.planka.domain.schema.definition.rule.action.TrackUserBehaviorAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户行为追踪动作执行器
 */
@Slf4j
@Component
public class TrackUserBehaviorActionExecutor extends AbstractRuleActionExecutor<TrackUserBehaviorAction> {

    // TODO: 注入行为追踪服务
    // private final UserBehaviorTrackingService trackingService;

    public TrackUserBehaviorActionExecutor(ActionTargetResolver targetResolver) {
        super(targetResolver);
    }

    @Override
    public String getActionType() {
        return "TRACK_USER_BEHAVIOR";
    }

    @Override
    public RuleExecutionResult.ActionExecutionResult execute(TrackUserBehaviorAction action, RuleExecutionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            String behaviorType = action.getBehaviorType();
            Map<String, Object> eventData = buildEventData(action, context);

            // TODO: 调用行为追踪服务
            log.info("追踪用户行为: behaviorType={}, userId={}, cardId={}, data={}",
                    behaviorType,
                    context.getOperatorId(),
                    context.getCardId(),
                    eventData);

            // 模拟行为追踪
            // trackingService.track(eventName, context.getOperatorId(), eventData);

            long duration = System.currentTimeMillis() - startTime;
            return RuleExecutionResult.ActionExecutionResult.success(
                    getActionType(),
                    action.getSortOrder(),
                    duration,
                    List.of()
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("追踪用户行为失败: error={}", e.getMessage(), e);
            return RuleExecutionResult.ActionExecutionResult.failed(
                    getActionType(),
                    action.getSortOrder(),
                    duration,
                    e.getMessage()
            );
        }
    }

    @Override
    protected CardId executeOnCard(TrackUserBehaviorAction action, CardDTO targetCard, RuleExecutionContext context) {
        // 此方法不使用
        return null;
    }

    private Map<String, Object> buildEventData(TrackUserBehaviorAction action, RuleExecutionContext context) {
        Map<String, Object> data = new HashMap<>();

        // 基础信息
        data.put("userId", context.getOperatorId());
        data.put("orgId", context.getOrgId());
        data.put("traceId", context.getTraceId());
        data.put("timestamp", System.currentTimeMillis());

        // 卡片相关信息
        if (context.getCardId() != null) {
            data.put("cardId", context.getCardId().value());
        }
        if (context.getCardTypeId() != null) {
            data.put("cardTypeId", context.getCardTypeId().value());
        }
        if (context.getTriggerEvent() != null) {
            data.put("triggerEvent", context.getTriggerEvent().name());
        }

        // 规则信息
        if (context.getCurrentRuleId() != null) {
            data.put("ruleId", context.getCurrentRuleId().value());
        }

        // 合并自定义属性
        if (action.getProperties() != null) {
            data.putAll(action.getProperties());
        }

        return data;
    }
}
