package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.schema.definition.condition.LinkConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关联条件求值器
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>IN - 关联的卡片在列表中</li>
 *     <li>NOT_IN - 关联的卡片不在列表中</li>
 *     <li>HAS_ANY - 有任何关联</li>
 *     <li>IS_EMPTY - 没有关联</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估关联条件
     */
    public boolean evaluate(LinkConditionItem item, CardDTO targetCard, CardDTO memberCard) {
        var subject = item.getSubject();
        var operator = item.getOperator();

        // 获取目标卡片
        CardDTO card = valueExtractor.getCardByPath(targetCard, subject.path());
        if (card == null) {
            return false;
        }

        // 获取关联卡片集合
        Set<CardDTO> linkedCards = valueExtractor.getLinkedCards(card, subject.linkFieldId().value());

        // 评估操作符
        return evaluateOperator(operator, linkedCards, targetCard, memberCard);
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(LinkConditionItem.LinkOperator operator,
                                     Set<CardDTO> linkedCards,
                                     CardDTO targetCard,
                                     CardDTO memberCard) {
        if (operator instanceof LinkConditionItem.LinkOperator.In op) {
            List<String> expectedCardIds = resolveLinkValue(op.getValue(), targetCard, memberCard);
            return evaluateIn(linkedCards, expectedCardIds);
        }

        if (operator instanceof LinkConditionItem.LinkOperator.NotIn op) {
            List<String> expectedCardIds = resolveLinkValue(op.getValue(), targetCard, memberCard);
            return !evaluateIn(linkedCards, expectedCardIds);
        }

        if (operator instanceof LinkConditionItem.LinkOperator.HasAny) {
            return linkedCards != null && !linkedCards.isEmpty();
        }

        if (operator instanceof LinkConditionItem.LinkOperator.IsEmpty) {
            return linkedCards == null || linkedCards.isEmpty();
        }

        log.warn("未知的关联操作符: {}", operator.getClass().getName());
        return false;
    }

    /**
     * 解析关联值（支持静态值和引用值）
     */
    private List<String> resolveLinkValue(LinkConditionItem.LinkValue value,
                                         CardDTO targetCard,
                                         CardDTO memberCard) {
        if (value == null) {
            return List.of();
        }

        if (value instanceof LinkConditionItem.LinkValue.StaticValue staticValue) {
            return staticValue.getCardIds();
        }

        if (value instanceof LinkConditionItem.LinkValue.ReferenceValue refValue) {
            CardDTO refCard = valueExtractor.getCardByReferenceSource(
                refValue.getSource(), targetCard, memberCard);
            if (refCard == null) {
                return List.of();
            }
            // 返回引用卡片的ID（转换为字符串）
            return List.of(String.valueOf(refCard.getId().value()));
        }

        return List.of();
    }

    private boolean evaluateIn(Set<CardDTO> linkedCards, List<String> expectedCardIds) {
        if (linkedCards == null || linkedCards.isEmpty()) {
            return false;
        }
        if (expectedCardIds == null || expectedCardIds.isEmpty()) {
            return false;
        }

        // 提取关联卡片的ID集合
        Set<String> linkedCardIds = linkedCards.stream()
            .map(card -> String.valueOf(card.getId().value()))
            .collect(Collectors.toSet());

        // 判断是否有交集
        for (String expectedCardId : expectedCardIds) {
            if (linkedCardIds.contains(expectedCardId)) {
                return true;
            }
        }

        return false;
    }
}
