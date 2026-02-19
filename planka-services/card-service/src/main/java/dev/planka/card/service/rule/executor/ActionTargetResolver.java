package dev.planka.card.service.rule.executor;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.Yield;
import dev.planka.card.repository.CardRepository;
import dev.planka.card.service.permission.ConditionEvaluator;
import dev.planka.domain.card.CardId;
import dev.planka.domain.link.Path;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.rule.action.ActionTargetSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 动作目标解析器
 * <p>
 * 根据目标选择器解析需要操作的卡片列表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActionTargetResolver {

    private final CardRepository cardRepository;
    private final ConditionEvaluator conditionEvaluator;

    /**
     * 解析目标卡片列表
     *
     * @param selector 目标选择器
     * @param context  执行上下文
     * @return 目标卡片列表
     */
    public List<CardDTO> resolveTargets(ActionTargetSelector selector, RuleExecutionContext context) {
        if (selector == null) {
            // 默认返回当前卡片
            return context.getTriggerCard() != null
                    ? Collections.singletonList(context.getTriggerCard())
                    : Collections.emptyList();
        }

        return switch (selector.getTargetType()) {
            case CURRENT_CARD -> resolveCurrentCard(context);
            case LINKED_CARD -> resolveLinkedCards(selector, context);
        };
    }

    /**
     * 解析当前卡片
     */
    private List<CardDTO> resolveCurrentCard(RuleExecutionContext context) {
        if (context.getTriggerCard() != null) {
            return Collections.singletonList(context.getTriggerCard());
        }

        if (context.getCardId() != null) {
            return cardRepository.findById(context.getCardId(), null, context.getOperatorId())
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        }

        return Collections.emptyList();
    }

    /**
     * 解析关联卡片
     */
    private List<CardDTO> resolveLinkedCards(ActionTargetSelector selector, RuleExecutionContext context) {
        CardDTO triggerCard = context.getTriggerCard();
        if (triggerCard == null) {
            log.warn("触发卡片为空，无法解析关联卡片");
            return Collections.emptyList();
        }

        Path linkPath = selector.getLinkPath();
        if (linkPath == null || linkPath.linkNodes() == null || linkPath.linkNodes().isEmpty()) {
            log.warn("关联路径为空，无法解析关联卡片");
            return Collections.emptyList();
        }

        // 获取关联卡片
        List<CardDTO> linkedCards = resolveLinkedCardsByPath(triggerCard.getId(), linkPath);

        // 应用过滤条件
        Condition filterCondition = selector.getFilterCondition();
        if (filterCondition != null && !linkedCards.isEmpty()) {
            linkedCards = filterByCondition(linkedCards, filterCondition, context);
        }

        return linkedCards;
    }

    /**
     * 根据路径解析关联卡片
     * <p>
     * 根据 Path 的 linkNodes 构建嵌套 Yield 查询触发卡片，然后逐层遍历 linkedCards 获取目标卡片。
     * Path.linkNodes() 返回 List<String>，每个元素是 linkFieldId
     */
    private List<CardDTO> resolveLinkedCardsByPath(CardId cardId, Path path) {
        // 构建嵌套 Yield，重新查询触发卡片以获取关联数据
        Yield yield = Yield.forLinkPath(path);
        CardDTO cardWithLinks = cardRepository
                .findById(cardId, yield, "system")
                .orElse(null);
        if (cardWithLinks == null) {
            log.warn("重新查询卡片失败: cardId={}", cardId.value());
            return Collections.emptyList();
        }

        // 沿路径获取末端关联卡片
        return new ArrayList<>(cardWithLinks.getLinkedCards(path));
    }

    /**
     * 根据条件过滤卡片
     */
    private List<CardDTO> filterByCondition(List<CardDTO> cards, Condition condition,
                                            RuleExecutionContext context) {
        List<CardDTO> filtered = new ArrayList<>();
        for (CardDTO card : cards) {
            try {
                boolean match = conditionEvaluator.evaluate(condition, card);
                if (match) {
                    filtered.add(card);
                }
            } catch (Exception e) {
                log.warn("评估条件失败，跳过该卡片: cardId={}, error={}", card.getId(), e.getMessage());
            }
        }
        return filtered;
    }
}
