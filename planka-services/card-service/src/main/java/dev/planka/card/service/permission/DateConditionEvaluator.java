package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.field.DateFieldValue;
import dev.planka.domain.field.FieldValue;
import dev.planka.domain.schema.definition.condition.DateConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 日期条件求值器
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>EQUAL - 等于</li>
 *     <li>NOT_EQUAL - 不等于</li>
 *     <li>BEFORE - 早于</li>
 *     <li>AFTER - 晚于</li>
 *     <li>BETWEEN - 在范围内</li>
 *     <li>IS_EMPTY - 为空</li>
 *     <li>IS_NOT_EMPTY - 不为空</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DateConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估日期条件
     */
    public boolean evaluate(DateConditionItem item, CardDTO targetCard, CardDTO memberCard) {
        var subject = item.getSubject();
        var operator = item.getOperator();

        // 获取实际日期值
        LocalDateTime actualValue = extractDateValue(subject, targetCard, memberCard);

        // 评估操作符
        return evaluateOperator(operator, actualValue, targetCard, memberCard);
    }

    /**
     * 提取日期值（支持自定义字段和系统字段）
     */
    private LocalDateTime extractDateValue(DateConditionItem.DateSubject subject,
                                          CardDTO targetCard,
                                          CardDTO memberCard) {
        if (subject instanceof DateConditionItem.DateSubject.FieldDateSubject fieldSubject) {
            CardDTO card = valueExtractor.getCardByPath(targetCard, fieldSubject.getPath());
            if (card == null) {
                return null;
            }
            FieldValue<?> fieldValue = valueExtractor.getFieldValue(card, fieldSubject.getFieldId());
            return extractDateFromFieldValue(fieldValue);
        }

        if (subject instanceof DateConditionItem.DateSubject.SystemDateSubject systemSubject) {
            CardDTO card = valueExtractor.getCardByPath(targetCard, systemSubject.getPath());
            if (card == null) {
                return null;
            }
            return valueExtractor.getSystemDateValue(card, systemSubject.getSystemField());
        }

        return null;
    }

    /**
     * 从字段值中提取日期
     */
    private LocalDateTime extractDateFromFieldValue(FieldValue<?> fieldValue) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            return null;
        }

        if (fieldValue instanceof DateFieldValue dateValue) {
            Long timestamp = dateValue.getValue();
            if (timestamp == null) {
                return null;
            }
            return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
            );
        }

        return null;
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(DateConditionItem.DateOperator operator,
                                     LocalDateTime actualValue,
                                     CardDTO targetCard,
                                     CardDTO memberCard) {
        if (operator instanceof DateConditionItem.DateOperator.Equal op) {
            LocalDateTime expectedValue = resolveDateValue(op.getValue(), targetCard, memberCard);
            return evaluateEqual(actualValue, expectedValue);
        }

        if (operator instanceof DateConditionItem.DateOperator.NotEqual op) {
            LocalDateTime expectedValue = resolveDateValue(op.getValue(), targetCard, memberCard);
            return !evaluateEqual(actualValue, expectedValue);
        }

        if (operator instanceof DateConditionItem.DateOperator.Before op) {
            LocalDateTime expectedValue = resolveDateValue(op.getValue(), targetCard, memberCard);
            return evaluateBefore(actualValue, expectedValue);
        }

        if (operator instanceof DateConditionItem.DateOperator.After op) {
            LocalDateTime expectedValue = resolveDateValue(op.getValue(), targetCard, memberCard);
            return evaluateAfter(actualValue, expectedValue);
        }

        if (operator instanceof DateConditionItem.DateOperator.Between op) {
            LocalDateTime startValue = resolveDateValue(op.getStart(), targetCard, memberCard);
            LocalDateTime endValue = resolveDateValue(op.getEnd(), targetCard, memberCard);
            return evaluateBetween(actualValue, startValue, endValue);
        }

        if (operator instanceof DateConditionItem.DateOperator.IsEmpty) {
            return actualValue == null;
        }

        if (operator instanceof DateConditionItem.DateOperator.IsNotEmpty) {
            return actualValue != null;
        }

        log.warn("未知的日期操作符: {}", operator.getClass().getName());
        return false;
    }

    /**
     * 解析日期值（支持具体日期、关键日期和引用值）
     */
    private LocalDateTime resolveDateValue(DateConditionItem.DateValue value,
                                          CardDTO targetCard,
                                          CardDTO memberCard) {
        if (value == null) {
            return null;
        }

        if (value instanceof DateConditionItem.DateValue.Specific specific) {
            return parseSpecificDate(specific.getValue());
        }

        if (value instanceof DateConditionItem.DateValue.KeyDateValue keyDateValue) {
            return resolveKeyDate(keyDateValue.getKeyDate());
        }

        if (value instanceof DateConditionItem.DateValue.ReferenceValue refValue) {
            CardDTO refCard = valueExtractor.getCardByReferenceSource(
                refValue.getSource(), targetCard, memberCard);
            if (refCard == null) {
                return null;
            }
            FieldValue<?> fieldValue = valueExtractor.getFieldValue(refCard, refValue.getFieldId());
            return extractDateFromFieldValue(fieldValue);
        }

        return null;
    }

    /**
     * 解析具体日期字符串
     */
    private LocalDateTime parseSpecificDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            // 尝试解析为 LocalDateTime
            if (dateStr.contains(" ")) {
                return LocalDateTime.parse(dateStr.replace(" ", "T"));
            }
            // 解析为 LocalDate，转换为当天开始时间
            LocalDate date = LocalDate.parse(dateStr);
            return date.atStartOfDay();
        } catch (Exception e) {
            log.warn("无法解析日期字符串: {}", dateStr, e);
            return null;
        }
    }

    /**
     * 解析关键日期
     */
    private LocalDateTime resolveKeyDate(DateConditionItem.KeyDate keyDate) {
        if (keyDate == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        LocalDate result = switch (keyDate) {
            case TODAY -> today;
            case YESTERDAY -> today.minusDays(1);
            case TOMORROW -> today.plusDays(1);
            case THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            case LAST_WEEK -> today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            case NEXT_WEEK -> today.plusWeeks(1).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            case THIS_MONTH -> today.with(TemporalAdjusters.firstDayOfMonth());
            case LAST_MONTH -> today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
            case NEXT_MONTH -> today.plusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
            case THIS_QUARTER -> today.with(today.getMonth().firstMonthOfQuarter()).with(TemporalAdjusters.firstDayOfMonth());
            case THIS_YEAR -> today.with(TemporalAdjusters.firstDayOfYear());
            case LAST_7_DAYS -> today.minusDays(7);
            case LAST_30_DAYS -> today.minusDays(30);
            case NEXT_7_DAYS -> today.plusDays(7);
            case NEXT_30_DAYS -> today.plusDays(30);
        };

        return result.atStartOfDay();
    }

    private boolean evaluateEqual(LocalDateTime actualValue, LocalDateTime expectedValue) {
        if (actualValue == null && expectedValue == null) {
            return true;
        }
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue.isEqual(expectedValue);
    }

    private boolean evaluateBefore(LocalDateTime actualValue, LocalDateTime expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue.isBefore(expectedValue);
    }

    private boolean evaluateAfter(LocalDateTime actualValue, LocalDateTime expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue.isAfter(expectedValue);
    }

    private boolean evaluateBetween(LocalDateTime actualValue, LocalDateTime startValue, LocalDateTime endValue) {
        if (actualValue == null || startValue == null || endValue == null) {
            return false;
        }
        return !actualValue.isBefore(startValue) && !actualValue.isAfter(endValue);
    }
}
