package dev.planka.infra.expression;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.Yield;
import dev.planka.api.card.request.YieldField;
import dev.planka.api.card.request.YieldLink;
import dev.planka.domain.card.CardId;
import dev.planka.domain.expression.TextExpressionTemplate;
import dev.planka.domain.field.FieldValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公共文本表达式模板解析器
 * <p>
 * 解析 {@link TextExpressionTemplate} 中的占位符，替换为实际值。
 * <p>
 * 支持的表达式格式（与前端 TextExpressionTemplateEditor 保持一致）：
 * <ul>
 *   <li>${card} - 当前卡片标题</li>
 *   <li>${card.title} - 当前卡片标题</li>
 *   <li>${card.code} - 当前卡片编号</li>
 *   <li>${card.fieldId} - 当前卡片字段值，如 ${card.title}</li>
 *   <li>${card.linkFieldId.fieldId} - 关联卡片字段值（多级路径）</li>
 *   <li>${member} - 当前成员（操作人）卡片标题</li>
 *   <li>${member.fieldId} - 当前成员字段值</li>
 *   <li>${system.currentTime} - 系统变量（currentYear, currentMonth, currentDate, currentTime）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextExpressionTemplateResolver {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private static final String SOURCE_CARD = "card";
    private static final String SOURCE_MEMBER = "member";
    private static final String SOURCE_SYSTEM = "system";

    private final CardDataProvider cardDataProvider;

    /**
     * 解析模板，替换 ${xxx} 占位符
     *
     * @param template     文本表达式模板
     * @param cardId       当前卡片ID
     * @param memberCardId 当前操作人的成员卡ID
     * @return 解析后的字符串
     */
    public String resolve(TextExpressionTemplate template, CardId cardId, CardId memberCardId) {
        if (template == null || template.template() == null) {
            return null;
        }
        return resolve(template.template(), cardId, memberCardId);
    }

    /**
     * 解析模板字符串
     *
     * @param templateString 模板字符串
     * @param cardId         当前卡片ID
     * @param memberCardId   当前操作人的成员卡ID
     * @return 解析后的字符串
     */
    public String resolve(String templateString, CardId cardId, CardId memberCardId) {
        if (templateString == null || templateString.isEmpty()) {
            return templateString;
        }

        if (!EXPRESSION_PATTERN.matcher(templateString).find()) {
            return templateString;
        }

        YieldPair yieldPair = buildYieldsFromTemplate(templateString);

        // 懒加载：仅在需要时查询
        CardDTO currentCard = null;
        CardDTO memberCard = null;

        if (yieldPair.needsCard() && cardId != null) {
            currentCard = cardDataProvider.findCardById(cardId, yieldPair.cardYield());
        }
        if (yieldPair.needsMember() && memberCardId != null) {
            memberCard = cardDataProvider.findCardById(memberCardId, yieldPair.memberYield());
        }

        return replaceExpressions(templateString, currentCard, memberCard);
    }

    private String replaceExpressions(String templateString, CardDTO currentCard, CardDTO memberCard) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = EXPRESSION_PATTERN.matcher(templateString);

        while (matcher.find()) {
            String expression = matcher.group(1);
            String replacement = evaluateExpression(expression, currentCard, memberCard);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement != null ? replacement : ""));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String evaluateExpression(String expression, CardDTO currentCard, CardDTO memberCard) {
        if (expression == null || expression.isEmpty()) {
            return "";
        }

        String[] parts = expression.split("\\.");
        if (parts.length == 0) {
            return "${" + expression + "}";
        }

        String source = parts[0];

        try {
            return switch (source) {
                case SOURCE_CARD -> evaluateCardExpression(parts, currentCard);
                case SOURCE_MEMBER -> evaluateMemberExpression(parts, memberCard);
                case SOURCE_SYSTEM -> evaluateSystemExpression(parts);
                default -> {
                    log.warn("未知的表达式源: {}", source);
                    yield "${" + expression + "}";
                }
            };
        } catch (Exception e) {
            log.error("解析表达式失败: expression={}, error={}", expression, e.getMessage());
            return "${" + expression + "}";
        }
    }

    /**
     * 解析 card 源表达式
     * ${card} -> 卡片标题
     * ${card.title} -> 字段值
     * ${card.linkFieldId.fieldId} -> 关联卡片字段值
     */
    private String evaluateCardExpression(String[] parts, CardDTO currentCard) {
        if (parts.length == 1) {
            // ${card}
            return getCardTitle(currentCard);
        }

        if (currentCard == null) {
            return "";
        }

        if (parts.length == 2) {
            // ${card.fieldId}
            return getCardFieldValue(currentCard, parts[1]);
        }

        // ${card.linkFieldId.fieldId} - 多级路径，获取关联卡片字段
        String linkFieldId = parts[1];
        String targetFieldId = parts[2];

        if (currentCard.getLinkedCards() == null) {
            return "";
        }

        Set<CardDTO> linkedCards = currentCard.getLinkedCards().get(linkFieldId);
        if (linkedCards == null || linkedCards.isEmpty()) {
            return "";
        }

        CardDTO firstLinkedCard = linkedCards.iterator().next();
        return getCardFieldValue(firstLinkedCard, targetFieldId);
    }

    /**
     * 解析 member 源表达式
     * ${member} -> 成员卡片标题
     * ${member.name} -> 成员字段值
     */
    private String evaluateMemberExpression(String[] parts, CardDTO memberCard) {
        if (parts.length == 1) {
            // ${member}
            return getCardTitle(memberCard);
        }

        // ${member.fieldId}
        return getCardFieldValue(memberCard, parts[1]);
    }

    /**
     * 解析 system 源表达式
     * ${system.currentYear} -> 当前年份
     * ${system.currentMonth} -> 当前月份
     * ${system.currentDate} -> 当前日期
     * ${system.currentTime} -> 当前时间
     */
    private String evaluateSystemExpression(String[] parts) {
        if (parts.length < 2) {
            return "";
        }

        String variable = parts[1];
        return switch (variable) {
            case "currentTime" -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            case "currentDate" -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            case "currentYear" -> String.valueOf(LocalDate.now().getYear());
            case "currentMonth" -> String.valueOf(LocalDate.now().getMonthValue());
            default -> "";
        };
    }

    /**
     * 从模板中解析出需要的 Yield 对
     */
    private YieldPair buildYieldsFromTemplate(String templateString) {
        Set<String> cardFieldIds = new HashSet<>();
        Set<String> memberFieldIds = new HashSet<>();
        Map<String, Set<String>> linkedCardFields = new HashMap<>();
        boolean needsCard = false;
        boolean needsMember = false;

        Matcher matcher = EXPRESSION_PATTERN.matcher(templateString);
        while (matcher.find()) {
            String expression = matcher.group(1);
            String[] parts = expression.split("\\.");
            if (parts.length == 0) {
                continue;
            }

            String source = parts[0];

            switch (source) {
                case SOURCE_CARD -> {
                    needsCard = true;
                    if (parts.length == 2) {
                        // ${card.fieldId}
                        cardFieldIds.add(parts[1]);
                    } else if (parts.length >= 3) {
                        // ${card.linkFieldId.fieldId}
                        String linkFieldId = parts[1];
                        String fieldId = parts[2];
                        linkedCardFields.computeIfAbsent(linkFieldId, k -> new HashSet<>()).add(fieldId);
                    }
                }
                case SOURCE_MEMBER -> {
                    needsMember = true;
                    if (parts.length >= 2) {
                        memberFieldIds.add(parts[1]);
                    }
                }
                case SOURCE_SYSTEM -> {
                    // 系统变量不需要查询卡片数据
                }
                default -> log.warn("未知的表达式源: {}", source);
            }
        }

        Yield cardYield = needsCard ? buildYield(cardFieldIds, linkedCardFields) : null;
        Yield memberYield = needsMember ? buildYield(memberFieldIds, Map.of()) : null;

        return new YieldPair(cardYield, memberYield, needsCard, needsMember);
    }

    private Yield buildYield(Set<String> fieldIds, Map<String, Set<String>> linkedCardFields) {
        Yield yield = new Yield();

        if (fieldIds.isEmpty()) {
            yield.setField(YieldField.basic());
        } else {
            YieldField yieldField = new YieldField();
            yieldField.setFieldIds(fieldIds);
            yieldField.setIncludeDescription(fieldIds.contains("description"));
            yield.setField(yieldField);
        }

        if (!linkedCardFields.isEmpty()) {
            List<YieldLink> links = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : linkedCardFields.entrySet()) {
                YieldLink link = new YieldLink();
                link.setLinkFieldId(entry.getKey());
                Yield targetYield = new Yield();
                YieldField targetField = new YieldField();
                targetField.setFieldIds(entry.getValue());
                targetYield.setField(targetField);
                link.setTargetYield(targetYield);
                links.add(link);
            }
            yield.setLinks(links);
        }

        return yield;
    }

    private String getCardTitle(CardDTO card) {
        if (card == null || card.getTitle() == null) {
            return "";
        }
        return card.getTitle().getValue() != null ? card.getTitle().getValue() : "";
    }

    private String getCardFieldValue(CardDTO card, String fieldId) {
        if (card == null) {
            return "";
        }

        return switch (fieldId) {
            case "id" -> card.getId() != null ? card.getId().value() : "";
            case "code" -> card.getCode();
            case "title" -> getCardTitle(card);
            case "description" -> card.getDescription() != null ? card.getDescription().getValue() : "";
            case "statusId" -> card.getStatusId() != null ? card.getStatusId().value() : "";
            case "createdAt" -> formatDateTime(card.getCreatedAt());
            case "updatedAt" -> formatDateTime(card.getUpdatedAt());
            default -> getCustomFieldValue(card, fieldId);
        };
    }

    private String getCustomFieldValue(CardDTO card, String fieldId) {
        if (card.getFieldValues() == null) {
            return "";
        }
        FieldValue<?> fieldValue = card.getFieldValues().get(fieldId);
        if (fieldValue == null || fieldValue.getValue() == null) {
            return "";
        }
        return String.valueOf(fieldValue.getValue());
    }

    private String formatDateTime(Object dateTime) {
        if (dateTime == null) {
            return "";
        }
        if (dateTime instanceof LocalDateTime ldt) {
            return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (dateTime instanceof Long timestamp) {
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(timestamp),
                    java.time.ZoneId.systemDefault()
            ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return String.valueOf(dateTime);
    }

    private record YieldPair(Yield cardYield, Yield memberYield, boolean needsCard, boolean needsMember) {}
}
