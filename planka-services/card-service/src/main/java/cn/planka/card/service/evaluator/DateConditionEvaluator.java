package cn.planka.card.service.evaluator;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.domain.field.DateFieldValue;
import cn.planka.domain.field.FieldValue;
import cn.planka.domain.schema.definition.condition.DateConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 日期条件求值器
 * <p>
 * 只处理已解析的静态日期值（Specific）和引用值（ReferenceValue）。
 * 动态日期（KeyDate、Recent、Future）由 {@link ConditionResolver} 在评估前解析为静态值。
 * <p>
 * 支持的操作符：
 * <ul>
 *     <li>EQUAL - 等于</li>
 *     <li>NOT_EQUAL - 不等于</li>
 *     <li>BEFORE - 早于</li>
 *     <li>AFTER - 晚于</li>
 *     <li>BETWEEN - 在范围内</li>
 *     <li>NOT_BETWEEN - 不在范围内</li>
 *     <li>IS_EMPTY - 为空</li>
 *     <li>IS_NOT_EMPTY - 不为空</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DateConditionEvaluator extends AbstractConditionEvaluator {

    private final CardValueExtractor valueExtractor;

    /**
     * 评估日期条件
     */
    public boolean evaluate(DateConditionItem item, CardDTO targetCard, CardDTO memberCard) {
        var subject = item.getSubject();
        var operator = item.getOperator();

        // 根据 subject 类型分别处理
        if (subject instanceof DateConditionItem.DateSubject.FieldDateSubject fieldSubject) {
            return evaluateFieldDate(fieldSubject, operator, targetCard, memberCard);
        }

        if (subject instanceof DateConditionItem.DateSubject.SystemDateSubject systemSubject) {
            return evaluateSystemDate(systemSubject, operator, targetCard, memberCard);
        }

        return false;
    }

    /**
     * 评估自定义字段日期
     */
    private boolean evaluateFieldDate(DateConditionItem.DateSubject.FieldDateSubject subject,
                                      DateConditionItem.DateOperator operator,
                                      CardDTO targetCard, CardDTO memberCard) {
        // 如果 path 不存在，直接评估当前卡片
        if (subject.getPath() == null || subject.getPath().linkNodes().isEmpty()) {
            LocalDate actualValue = extractDateFromFieldValue(targetCard.getFieldValue(subject.getFieldId()));
            return evaluateOperator(operator, actualValue, targetCard, memberCard);
        }

        // 获取目标卡片集合并评估（使用短路求值）
        Set<CardDTO> cards = targetCard.getLinkedCards(subject.getPath());
        return evaluateCards(cards,
            card -> {
                LocalDate actualValue = extractDateFromFieldValue(card.getFieldValue(subject.getFieldId()));
                return evaluateOperator(operator, actualValue, targetCard, memberCard);
            },
            operator);
    }

    /**
     * 评估系统字段日期
     */
    private boolean evaluateSystemDate(DateConditionItem.DateSubject.SystemDateSubject subject,
                                       DateConditionItem.DateOperator operator,
                                       CardDTO targetCard, CardDTO memberCard) {
        // 如果 path 不存在，直接评估当前卡片
        if (subject.getPath() == null || subject.getPath().linkNodes().isEmpty()) {
            LocalDate actualValue = extractSystemDateFromCard(targetCard, subject.getSystemField());
            return evaluateOperator(operator, actualValue, targetCard, memberCard);
        }

        // 获取目标卡片集合并评估（使用短路求值）
        Set<CardDTO> cards = targetCard.getLinkedCards(subject.getPath());
        return evaluateCards(cards,
            card -> {
                LocalDate actualValue = extractSystemDateFromCard(card, subject.getSystemField());
                return evaluateOperator(operator, actualValue, targetCard, memberCard);
            },
            operator);
    }

    /**
     * 从卡片提取系统字段日期
     */
    private LocalDate extractSystemDateFromCard(CardDTO card, DateConditionItem.SystemDateField systemField) {
        LocalDateTime dateTime = valueExtractor.getSystemDateValue(card, systemField);
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    /**
     * 从字段值中提取日期（精度到天）
     */
    private LocalDate extractDateFromFieldValue(FieldValue<?> fieldValue) {
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
            ).toLocalDate();
        }

        return null;
    }

    /**
     * 评估操作符
     */
    private boolean evaluateOperator(DateConditionItem.DateOperator operator,
                                     LocalDate actualValue,
                                     CardDTO targetCard,
                                     CardDTO memberCard) {
        if (operator instanceof DateConditionItem.DateOperator.Equal op) {
            LocalDate expectedValue = resolveDateValue(op.getValue(), targetCard, memberCard);
            return evaluateEqual(actualValue, expectedValue);
        }
        if (operator instanceof DateConditionItem.DateOperator.NotEqual op) {
            LocalDate expectedValue = resolveDateValue(op.getValue(), targetCard, memberCard);
            return !evaluateEqual(actualValue, expectedValue);
        }
        if (operator instanceof DateConditionItem.DateOperator.Before op) {
            LocalDate expectedValue = resolveDateValue(op.getValue(), targetCard, memberCard);
            return evaluateBefore(actualValue, expectedValue);
        }
        if (operator instanceof DateConditionItem.DateOperator.After op) {
            LocalDate expectedValue = resolveDateValue(op.getValue(), targetCard, memberCard);
            return evaluateAfter(actualValue, expectedValue);
        }
        if (operator instanceof DateConditionItem.DateOperator.Between op) {
            LocalDate startValue = resolveDateValue(op.getStart(), targetCard, memberCard);
            LocalDate endValue = resolveDateValue(op.getEnd(), targetCard, memberCard);
            return evaluateBetween(actualValue, startValue, endValue);
        }
        if (operator instanceof DateConditionItem.DateOperator.NotBetween op) {
            LocalDate startValue = resolveDateValue(op.getStart(), targetCard, memberCard);
            LocalDate endValue = resolveDateValue(op.getEnd(), targetCard, memberCard);
            return !evaluateBetween(actualValue, startValue, endValue);
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
     * 解析日期值（只支持 Specific 和 ReferenceValue）
     * 动态日期值（KeyDate、Recent、Future）应由 ConditionResolver 预先解析为 Specific
     */
    private LocalDate resolveDateValue(DateConditionItem.DateValue value,
                                          CardDTO targetCard,
                                          CardDTO memberCard) {
        if (value == null) {
            return null;
        }

        if (value instanceof DateConditionItem.DateValue.Specific specific) {
            return specific.getDate();
        }

        if (value instanceof DateConditionItem.DateValue.ReferenceValue refValue) {
            CardDTO refCard = valueExtractor.getCardByReferenceSource(
                refValue.getSource(), targetCard, memberCard);
            if (refCard == null) {
                return null;
            }
            FieldValue<?> fieldValue = refCard.getFieldValue(refValue.getFieldId());
            return extractDateFromFieldValue(fieldValue);
        }

        // 动态日期值应该已经被 ConditionResolver 解析为 Specific
        log.warn("未解析的动态日期值类型: {}", value.getClass().getSimpleName());
        return null;
    }

    private boolean evaluateEqual(LocalDate actualValue, LocalDate expectedValue) {
        if (actualValue == null && expectedValue == null) {
            return true;
        }
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue.isEqual(expectedValue);
    }

    private boolean evaluateBefore(LocalDate actualValue, LocalDate expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue.isBefore(expectedValue);
    }

    private boolean evaluateAfter(LocalDate actualValue, LocalDate expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        return actualValue.isAfter(expectedValue);
    }

    private boolean evaluateBetween(LocalDate actualValue, LocalDate startValue, LocalDate endValue) {
        if (actualValue == null || startValue == null || endValue == null) {
            return false;
        }
        return !actualValue.isBefore(startValue) && !actualValue.isAfter(endValue);
    }
}
