package cn.planka.card.service.core;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.card.service.evaluator.CardValueExtractor;
import cn.planka.domain.field.FieldValue;
import cn.planka.domain.field.TextFieldValue;
import cn.planka.domain.link.Path;
import cn.planka.domain.schema.definition.cardtype.TitleCompositionRule;
import cn.planka.domain.schema.definition.cardtype.TitlePart;
import cn.planka.domain.schema.definition.condition.DateConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 标题值解析器
 * <p>
 * 负责根据 TitleCompositionRule 解析并拼接标题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TitleValueResolver {

    private final CardValueExtractor cardValueExtractor;

    /**
     * 解析标题组成部分的值
     *
     * @param card 目标卡片
     * @param rule 标题组合规则
     * @return 解析后的部分列表，如果规则为空或未启用则返回空列表
     */
    public List<String> resolveParts(CardDTO card, TitleCompositionRule rule) {
        if (rule == null || !rule.isEnabled() || rule.getParts() == null) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (TitlePart part : rule.getParts()) {
            String value = resolvePartValue(card, part);
            if (value != null && !value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private String resolvePartValue(CardDTO card, TitlePart part) {
        if (part == null || part.fieldId() == null || part.fieldId().isBlank()) {
            return null;
        }

        String fieldId = part.fieldId();
        Path path = part.path();

        CardDTO targetCard = resolveTargetCard(card, path);
        if (targetCard == null) {
            return null;
        }

        // 处理系统字段
        if ("CODE".equals(fieldId)) {
            // 优先使用 customCode，如果没有则使用 codeInOrg
            return targetCard.getCustomCode() != null ? targetCard.getCustomCode() : String.valueOf(targetCard.getCodeInOrg());
        }

        // 尝试从卡片的 fieldValues 中获取
        FieldValue<?> fieldValue = null;
        if (targetCard.getFieldValues() != null) {
            fieldValue = targetCard.getFieldValues().get(fieldId);
        }

        // 如果 fieldValues 返回 null，尝试自己处理常见的系统字段
        if (fieldValue == null) {
             if ("STATUS".equals(fieldId)) {
                 // TODO: 需要 StatusService 或缓存来获取状态名称
                 return targetCard.getStatusId() != null ? targetCard.getStatusId().toString() : null;
             }
             if (fieldId.startsWith("SYSTEM_DATE:")) {
                 String dateType = fieldId.substring("SYSTEM_DATE:".length());
                 try {
                     Object val = cardValueExtractor.getSystemDateValue(targetCard, DateConditionItem.SystemDateField.valueOf(dateType));
                     return val != null ? val.toString() : null;
                 } catch (IllegalArgumentException e) {
                     log.warn("Unknown system date field: {}", dateType);
                     return null;
                 }
             }
             return null;
        }

        // 转换为字符串
        // 目前简单处理，对于 Text 类型直接获取值，其他类型调用 toString 或特定处理
        if (fieldValue instanceof TextFieldValue textValue) {
            return textValue.getValue();
        }

        // TODO: 支持更多类型的格式化显示（如日期、枚举等）
        Object value = fieldValue.getValue();
        return value != null ? value.toString() : null;
    }

    /**
     * 解析目标卡片（根据路径获取第一个关联卡片）
     *
     * @param card 起始卡片
     * @param path 路径（可能为 null）
     * @return 目标卡片，如果路径为 null 返回起始卡片
     */
    private CardDTO resolveTargetCard(CardDTO card, Path path) {
        if (card == null) {
            return null;
        }

        if (path == null || path.linkNodes() == null || path.linkNodes().isEmpty()) {
            return card;
        }

        CardDTO current = card;
        for (String linkFieldId : path.linkNodes()) {
            Set<CardDTO> linkedCards = current.getLinkedCards(linkFieldId);
            if (linkedCards == null || linkedCards.isEmpty()) {
                return null;
            }
            current = linkedCards.iterator().next();
        }

        return current;
    }
}
