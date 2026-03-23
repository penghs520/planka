package cn.planka.card.service.rule.trigger;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.request.Yield;
import cn.planka.api.card.util.ConditionYieldBuilder;
import cn.planka.card.repository.CardRepository;
import cn.planka.card.service.flowrecord.ValueStreamHelper;
import cn.planka.card.service.rule.executor.BizRuleExecutionService;
import cn.planka.card.service.rule.executor.RuleExecutionContext;
import cn.planka.domain.card.CardId;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.schema.definition.condition.Condition;
import cn.planka.domain.schema.definition.rule.BizRuleDefinition;
import cn.planka.domain.schema.definition.stream.ValueStreamDefinition;
import cn.planka.domain.stream.StatusId;
import cn.planka.infra.cache.schema.query.BizRuleCacheQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 业务规则触发服务
 * <p>
 * 负责根据事件匹配规则并触发执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizRuleTriggerService {

    private final BizRuleCacheQuery bizRuleCacheQuery;
    private final BizRuleExecutionService executionService;
    private final CardRepository cardRepository;
    private final ValueStreamHelper valueStreamHelper;

    /**
     * 触发卡片创建规则
     */
    public void triggerOnCreate(CardDTO card, String operatorId) {
        trigger(card, BizRuleDefinition.TriggerEvent.ON_CREATE, operatorId, false);
    }

    /**
     * 触发卡片回收规则
     */
    public void triggerOnDiscard(CardDTO card, String operatorId) {
        trigger(card, BizRuleDefinition.TriggerEvent.ON_DISCARD, operatorId, false);
    }

    /**
     * 触发卡片存档规则
     */
    public void triggerOnArchive(CardDTO card, String operatorId) {
        trigger(card, BizRuleDefinition.TriggerEvent.ON_ARCHIVE, operatorId, false);
    }

    /**
     * 触发卡片还原规则
     */
    public void triggerOnRestore(CardDTO card, String operatorId) {
        trigger(card, BizRuleDefinition.TriggerEvent.ON_RESTORE, operatorId, false);
    }

    /**
     * 触发卡片状态移动规则
     *
     * @param card         卡片
     * @param fromStatusId 原状态ID
     * @param toStatusId   目标状态ID
     * @param operatorId   操作人ID
     * @param isRollback   是否回滚
     * @param valueStream  价值流定义
     */
    public void triggerOnStatusMove(CardDTO card, StatusId fromStatusId, StatusId toStatusId,
                                    String operatorId, boolean isRollback,
                                    ValueStreamDefinition valueStream) {
        BizRuleDefinition.TriggerEvent event = isRollback
                ? BizRuleDefinition.TriggerEvent.ON_STATUS_ROLLBACK
                : BizRuleDefinition.TriggerEvent.ON_STATUS_MOVE;

        // 获取从 from 到 to 的完整状态路径，排除 fromStatusId，作为匹配候选
        List<StatusId> targetStatusIds = resolveTargetStatusIds(valueStream, fromStatusId, toStatusId);

        // 查找匹配路径上任一状态的规则
        List<BizRuleDefinition> rules = findMatchingRulesForStatuses(
                card.getTypeId(), event, targetStatusIds);
        if (rules.isEmpty()) {
            log.debug("没有匹配的规则: cardTypeId={}, event={}, targetStatusIds={}",
                    card.getTypeId(), event, targetStatusIds);
            return;
        }

        // 从触发条件中搜集卡片所需返回的字段，提取获取当前上下文卡片和操作人卡片
        List<Condition> conditions = rules.stream()
                .map(BizRuleDefinition::getCondition)
                .filter(Objects::nonNull)
                .toList();

        Yield yieldForTriggerCard = ConditionYieldBuilder.buildYieldForCurrentCard(conditions);
        Yield yieldForOperatorCard = ConditionYieldBuilder.buildYieldForMemberCard(conditions);

        CardDTO triggerCard = cardRepository.findById(card.getId(), yieldForTriggerCard, "system")
                .orElseThrow(() -> new IllegalStateException(String.format("TriggerCard not found:%s", card.getId().toString())));
        CardDTO operatorCard = cardRepository.findById(CardId.of(operatorId), yieldForOperatorCard, "system")
                .orElseThrow(() -> new IllegalStateException(String.format("OperatorCard not found:%s", operatorId)));

        RuleExecutionContext context = RuleExecutionContext.build(triggerCard, operatorCard, event);

        executeRules(rules, context);
    }

    /**
     * 解析目标状态列表：从路径中提取所有状态（排除 fromStatusId）
     */
    private List<StatusId> resolveTargetStatusIds(ValueStreamDefinition valueStream,
                                                  StatusId fromStatusId,
                                                  StatusId toStatusId) {
        List<ValueStreamHelper.StatusNode> path = valueStreamHelper.getStatusPath(
                valueStream, fromStatusId, toStatusId);

        return path.stream()
                .map(ValueStreamHelper.StatusNode::statusId)
                .filter(statusId -> !statusId.equals(fromStatusId))
                .collect(Collectors.toList());
    }

    /**
     * 查找匹配多个目标状态的规则（去重）
     */
    private List<BizRuleDefinition> findMatchingRulesForStatuses(CardTypeId cardTypeId,
                                                                 BizRuleDefinition.TriggerEvent event,
                                                                 List<StatusId> targetStatusIds) {
        List<BizRuleDefinition> allRules = bizRuleCacheQuery.getByCardTypeId(cardTypeId);

        return allRules.stream()
                .filter(BizRuleDefinition::isEnabled)
                .filter(rule -> rule.getTriggerEvent() == event)
                .filter(rule -> matchAnyTargetStatus(rule, targetStatusIds))
                .distinct()
                .toList();
    }

    /**
     * 检查规则是否匹配目标状态列表中的任一状态
     */
    private boolean matchAnyTargetStatus(BizRuleDefinition rule, List<StatusId> targetStatusIds) {
        if (targetStatusIds == null || targetStatusIds.isEmpty()) {
            return true;
        }
        // 规则未指定目标状态，匹配所有
        if (rule.getTargetStatusId() == null) {
            return true;
        }
        return targetStatusIds.contains(rule.getTargetStatusId());
    }

    /**
     * 触发字段变更规则
     *
     * @param card            卡片
     * @param changedFieldIds 变更的字段ID列表
     * @param operatorId      操作人ID
     */
    public void triggerOnFieldChange(CardDTO card, List<String> changedFieldIds, String operatorId) {
        if (changedFieldIds == null || changedFieldIds.isEmpty()) {
            return;
        }

        // 查找监听这些字段的规则
        List<BizRuleDefinition> rules = findMatchingRules(card.getTypeId(),
                BizRuleDefinition.TriggerEvent.ON_FIELD_CHANGE, null, changedFieldIds);
        if (rules.isEmpty()) {
            log.debug("没有匹配的规则: cardTypeId={}, event={}, changedFields={}",
                    card.getTypeId(), BizRuleDefinition.TriggerEvent.ON_FIELD_CHANGE, changedFieldIds);
            return;
        }


        // 从触发条件中搜集卡片所需返回的字段，提取获取当前上下文卡片和操作人卡片
        List<Condition> conditions = rules.stream()
                .map(BizRuleDefinition::getCondition)
                .filter(Objects::nonNull)
                .toList();

        Yield yieldForTriggerCard = ConditionYieldBuilder.buildYieldForCurrentCard(conditions);
        Yield yieldForOperatorCard = ConditionYieldBuilder.buildYieldForMemberCard(conditions);

        CardDTO triggerCard = cardRepository.findById(card.getId(), yieldForTriggerCard, "system")
                .orElseThrow(() -> new IllegalStateException(String.format("TriggerCard not found:%s", card.getId().toString())));
        CardDTO operatorCard = cardRepository.findById(CardId.of(operatorId), yieldForOperatorCard, "system")
                .orElseThrow(() -> new IllegalStateException(String.format("OperatorCard not found:%s", operatorId)));

        RuleExecutionContext context = RuleExecutionContext.build(triggerCard, operatorCard, BizRuleDefinition.TriggerEvent.ON_FIELD_CHANGE);
        context.setChangedFieldIds(changedFieldIds);

        executeRules(rules, context);
    }

    /**
     * 通用触发方法
     */
    public void trigger(CardDTO card, BizRuleDefinition.TriggerEvent event,
                        String operatorId, boolean triggeredByRule) {
        if (triggeredByRule) {
            log.debug("跳过规则触发的事件以防止循环: cardId={}, event={}", card.getId(), event);
            return;
        }

        // 查找匹配的规则
        List<BizRuleDefinition> rules = findMatchingRules(card.getTypeId(), event, null, null);
        if (rules.isEmpty()) {
            log.debug("没有匹配的规则: cardTypeId={}, event={}", card.getTypeId(), event);
            return;
        }

        // 从触发条件中搜集卡片所需返回的字段，提取获取当前上下文卡片和操作人卡片
        List<Condition> conditions = rules.stream()
                .map(BizRuleDefinition::getCondition)
                .filter(Objects::nonNull)
                .toList();

        Yield yieldForTriggerCard = ConditionYieldBuilder.buildYieldForCurrentCard(conditions);
        Yield yieldForOperatorCard = ConditionYieldBuilder.buildYieldForMemberCard(conditions);

        CardDTO triggerCard = cardRepository.findById(card.getId(), yieldForTriggerCard, "system")
                .orElseThrow(() -> new IllegalStateException(String.format("TriggerCard not found:%s", card.getId().toString())));
        CardDTO operatorCard = cardRepository.findById(CardId.of(operatorId), yieldForOperatorCard, "system")
                .orElseThrow(() -> new IllegalStateException(String.format("OperatorCard not found:%s", operatorId)));

        RuleExecutionContext context = RuleExecutionContext.build(triggerCard, operatorCard, BizRuleDefinition.TriggerEvent.ON_FIELD_CHANGE);
        context.setTriggeredByRule(triggeredByRule);

        executeRules(rules, context);
    }

    /**
     * 查找匹配的规则
     */
    private List<BizRuleDefinition> findMatchingRules(CardTypeId cardTypeId,
                                                      BizRuleDefinition.TriggerEvent event,
                                                      StatusId targetStatusId,
                                                      List<String> changedFieldIds) {
        // 从缓存获取该__PLANKA_EINST__的所有规则
        List<BizRuleDefinition> allRules = bizRuleCacheQuery.getByCardTypeId(cardTypeId);

        return allRules.stream()
                .filter(BizRuleDefinition::isEnabled)
                .filter(rule -> rule.getTriggerEvent() == event)
                .filter(rule -> matchTargetStatus(rule, targetStatusId))
                .filter(rule -> matchListenFields(rule, changedFieldIds))
                .toList();
    }

    /**
     * 检查是否匹配目标状态
     */
    private boolean matchTargetStatus(BizRuleDefinition rule, StatusId targetStatusId) {
        if (targetStatusId == null) {
            return true;
        }
        if (rule.getTargetStatusId() == null) {
            return true;
        }
        return rule.getTargetStatusId().equals(targetStatusId);
    }

    /**
     * 检查是否匹配监听字段
     */
    private boolean matchListenFields(BizRuleDefinition rule, List<String> changedFieldIds) {
        if (changedFieldIds == null || changedFieldIds.isEmpty()) {
            return true;
        }
        if (rule.getListenFieldList() == null || rule.getListenFieldList().isEmpty()) {
            // 没有指定监听字段，匹配所有字段变更
            return true;
        }
        // 检查是否有交集
        return rule.getListenFieldList().stream()
                .anyMatch(changedFieldIds::contains);
    }

    /**
     * 执行规则列表
     */
    private void executeRules(List<BizRuleDefinition> rules, RuleExecutionContext context) {
        log.info("开始执行 {} 条规则: cardId={}, event={}",
                rules.size(), context.getCardId(), context.getTriggerEvent());

        for (BizRuleDefinition rule : rules) {
            try {
                // 统一异步执行
                executionService.executeAsync(rule, context);
            } catch (Exception e) {
                log.error("执行规则失败: ruleId={}, error={}", rule.getId(), e.getMessage(), e);
            }
        }
    }
}
