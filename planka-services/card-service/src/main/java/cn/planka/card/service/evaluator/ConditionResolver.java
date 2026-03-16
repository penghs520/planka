package cn.planka.card.service.evaluator;

import cn.planka.domain.schema.definition.condition.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * 条件解析器
 * <p>
 * 在条件评估前，将动态日期值（关键日期、最近/未来时间）转换为静态日期值，
 * 并转换相应的操作符，简化后续评估逻辑。
 * <p>
 * 转换规则：
 * <ul>
 *     <li>等于 + 动态日期 → 在范围内 + [开始, 结束]</li>
 *     <li>不等于 + 动态日期 → 不在范围内 + [开始, 结束]</li>
 *     <li>早于 + 动态日期 → 早于 + 开始时间</li>
 *     <li>晚于 + 动态日期 → 晚于 + 结束时间</li>
 *     <li>在范围内 + 动态日期(start/end) → 在范围内 + [解析后的开始, 解析后的结束]</li>
 * </ul>
 */
@Slf4j
@Component
public class ConditionResolver {

    /**
     * 解析条件，将动态日期转换为静态日期
     *
     * @param condition 原始条件
     * @return 解析后的条件
     */
    public Condition resolve(Condition condition) {
        if (condition == null || condition.isEmpty()) {
            return condition;
        }

        ConditionNode resolvedRoot = resolveNode(condition.getRoot());
        return new Condition(resolvedRoot);
    }

    /**
     * 解析条件节点
     */
    private ConditionNode resolveNode(ConditionNode node) {
        if (node == null) {
            return null;
        }

        // 处理条件组合
        if (node instanceof ConditionGroup group) {
            return resolveGroup(group);
        }

        // 处理日期条件项
        if (node instanceof DateConditionItem item) {
            return resolveDateCondition(item);
        }

        // 其他类型条件保持不变
        return node;
    }

    /**
     * 解析条件组合
     */
    private ConditionGroup resolveGroup(ConditionGroup group) {
        if (group.isEmpty()) {
            return group;
        }

        List<ConditionNode> resolvedChildren = new ArrayList<>();
        for (ConditionNode child : group.getChildren()) {
            resolvedChildren.add(resolveNode(child));
        }
        return new ConditionGroup(group.getOperator(), resolvedChildren);
    }

    /**
     * 解析日期条件
     */
    private DateConditionItem resolveDateCondition(DateConditionItem item) {
        DateConditionItem.DateSubject subject = item.getSubject();
        DateConditionItem.DateOperator operator = item.getOperator();

        // 检查操作符是否包含动态日期值
        if (!hasDynamicDateValue(operator)) {
            return item;
        }

        // 根据操作符类型进行转换
        DateConditionItem.DateOperator resolvedOperator = resolveOperator(operator);

        return new DateConditionItem(subject, resolvedOperator);
    }

    /**
     * 检查操作符是否包含动态日期值
     */
    private boolean hasDynamicDateValue(DateConditionItem.DateOperator operator) {
        if (operator instanceof DateConditionItem.DateOperator.Equal op) {
            return isDynamicDateValue(op.getValue());
        }
        if (operator instanceof DateConditionItem.DateOperator.NotEqual op) {
            return isDynamicDateValue(op.getValue());
        }
        if (operator instanceof DateConditionItem.DateOperator.Before op) {
            return isDynamicDateValue(op.getValue());
        }
        if (operator instanceof DateConditionItem.DateOperator.After op) {
            return isDynamicDateValue(op.getValue());
        }
        if (operator instanceof DateConditionItem.DateOperator.Between op) {
            return isDynamicDateValue(op.getStart()) || isDynamicDateValue(op.getEnd());
        }
        return false;
    }

    /**
     * 判断是否为动态日期值
     */
    private boolean isDynamicDateValue(DateConditionItem.DateValue value) {
        if (value == null) {
            return false;
        }
        return value instanceof DateConditionItem.DateValue.KeyDateValue
                || value instanceof DateConditionItem.DateValue.RecentValue
                || value instanceof DateConditionItem.DateValue.FutureValue;
    }

    /**
     * 解析操作符，将动态日期转换为静态日期
     */
    private DateConditionItem.DateOperator resolveOperator(DateConditionItem.DateOperator operator) {
        if (operator instanceof DateConditionItem.DateOperator.Equal op) {
            return resolveEqualOperator(op);
        }
        if (operator instanceof DateConditionItem.DateOperator.NotEqual op) {
            return resolveNotEqualOperator(op);
        }
        if (operator instanceof DateConditionItem.DateOperator.Before op) {
            return resolveBeforeOperator(op);
        }
        if (operator instanceof DateConditionItem.DateOperator.After op) {
            return resolveAfterOperator(op);
        }
        if (operator instanceof DateConditionItem.DateOperator.Between op) {
            return resolveBetweenOperator(op);
        }
        return operator;
    }

    /**
     * 解析 Equal 操作符
     * 等于 + 动态日期 → 在范围内 + [开始, 结束]
     */
    private DateConditionItem.DateOperator resolveEqualOperator(DateConditionItem.DateOperator.Equal op) {
        DateConditionItem.DateValue value = op.getValue();

        if (value instanceof DateConditionItem.DateValue.KeyDateValue keyDateValue) {
            DateRange range = resolveKeyDateRange(keyDateValue.getKeyDate());
            return new DateConditionItem.DateOperator.Between(
                    new DateConditionItem.DateValue.Specific(range.start().toLocalDate().toString()),
                    new DateConditionItem.DateValue.Specific(range.end().toLocalDate().toString())
            );
        }

        if (value instanceof DateConditionItem.DateValue.RecentValue recentValue) {
            DateRange range = resolveRecentRange(recentValue);
            return new DateConditionItem.DateOperator.Between(
                    new DateConditionItem.DateValue.Specific(range.start().toLocalDate().toString()),
                    new DateConditionItem.DateValue.Specific(range.end().toLocalDate().toString())
            );
        }

        if (value instanceof DateConditionItem.DateValue.FutureValue futureValue) {
            DateRange range = resolveFutureRange(futureValue);
            return new DateConditionItem.DateOperator.Between(
                    new DateConditionItem.DateValue.Specific(range.start().toLocalDate().toString()),
                    new DateConditionItem.DateValue.Specific(range.end().toLocalDate().toString())
            );
        }

        return op;
    }

    /**
     * 解析 NotEqual 操作符
     * 不等于 + 动态日期 → 不在范围内 + [开始, 结束]
     */
    private DateConditionItem.DateOperator resolveNotEqualOperator(DateConditionItem.DateOperator.NotEqual op) {
        DateConditionItem.DateValue value = op.getValue();

        if (value instanceof DateConditionItem.DateValue.KeyDateValue keyDateValue) {
            DateRange range = resolveKeyDateRange(keyDateValue.getKeyDate());
            return new DateConditionItem.DateOperator.NotBetween(
                    new DateConditionItem.DateValue.Specific(range.start().toLocalDate().toString()),
                    new DateConditionItem.DateValue.Specific(range.end().toLocalDate().toString())
            );
        }

        if (value instanceof DateConditionItem.DateValue.RecentValue recentValue) {
            DateRange range = resolveRecentRange(recentValue);
            return new DateConditionItem.DateOperator.NotBetween(
                    new DateConditionItem.DateValue.Specific(range.start().toLocalDate().toString()),
                    new DateConditionItem.DateValue.Specific(range.end().toLocalDate().toString())
            );
        }

        if (value instanceof DateConditionItem.DateValue.FutureValue futureValue) {
            DateRange range = resolveFutureRange(futureValue);
            return new DateConditionItem.DateOperator.NotBetween(
                    new DateConditionItem.DateValue.Specific(range.start().toLocalDate().toString()),
                    new DateConditionItem.DateValue.Specific(range.end().toLocalDate().toString())
            );
        }

        return op;
    }

    /**
     * 解析 Before 操作符
     * 早于 + 动态日期 → 早于 + 开始时间
     */
    private DateConditionItem.DateOperator resolveBeforeOperator(DateConditionItem.DateOperator.Before op) {
        DateConditionItem.DateValue value = op.getValue();
        LocalDateTime startTime = resolveDateStart(value);

        if (startTime != null) {
            return new DateConditionItem.DateOperator.Before(
                    new DateConditionItem.DateValue.Specific(startTime.toLocalDate().toString())
            );
        }

        return op;
    }

    /**
     * 解析 After 操作符
     * 晚于 + 动态日期 → 晚于 + 结束时间
     */
    private DateConditionItem.DateOperator resolveAfterOperator(DateConditionItem.DateOperator.After op) {
        DateConditionItem.DateValue value = op.getValue();
        LocalDateTime endTime = resolveDateEnd(value);

        if (endTime != null) {
            return new DateConditionItem.DateOperator.After(
                    new DateConditionItem.DateValue.Specific(endTime.toLocalDate().toString())
            );
        }

        return op;
    }

    /**
     * 解析 Between 操作符
     * 在范围内 + 动态日期(start/end) → 在范围内 + [解析后的开始, 解析后的结束]
     */
    private DateConditionItem.DateOperator resolveBetweenOperator(DateConditionItem.DateOperator.Between op) {
        DateConditionItem.DateValue startValue = op.getStart();
        DateConditionItem.DateValue endValue = op.getEnd();

        LocalDateTime resolvedStart = resolveDateStart(startValue);
        LocalDateTime resolvedEnd = resolveDateEnd(endValue);

        if (resolvedStart != null || resolvedEnd != null) {
            // 如果一边不是动态日期，保留原值
            LocalDateTime finalStart = resolvedStart != null ? resolvedStart : resolveDateValue(startValue);
            LocalDateTime finalEnd = resolvedEnd != null ? resolvedEnd : resolveDateValue(endValue);

            return new DateConditionItem.DateOperator.Between(
                    new DateConditionItem.DateValue.Specific(finalStart.toLocalDate().toString()),
                    new DateConditionItem.DateValue.Specific(finalEnd.toLocalDate().toString())
            );
        }

        return op;
    }

    /**
     * 获取日期范围的开始时间
     */
    private LocalDateTime resolveDateStart(DateConditionItem.DateValue value) {
        if (value instanceof DateConditionItem.DateValue.KeyDateValue keyDateValue) {
            return resolveKeyDateStart(keyDateValue.getKeyDate());
        }
        if (value instanceof DateConditionItem.DateValue.RecentValue recentValue) {
            return resolveRecentStart(recentValue);
        }
        if (value instanceof DateConditionItem.DateValue.FutureValue futureValue) {
            return LocalDate.now().atStartOfDay();
        }
        return null;
    }

    /**
     * 获取日期范围的结束时间
     */
    private LocalDateTime resolveDateEnd(DateConditionItem.DateValue value) {
        if (value instanceof DateConditionItem.DateValue.KeyDateValue keyDateValue) {
            return resolveKeyDateEnd(keyDateValue.getKeyDate());
        }
        if (value instanceof DateConditionItem.DateValue.RecentValue recentValue) {
            return LocalDate.now().atTime(23, 59, 59);
        }
        if (value instanceof DateConditionItem.DateValue.FutureValue futureValue) {
            return resolveFutureEnd(futureValue);
        }
        return null;
    }

    /**
     * 解析日期值为具体时间
     */
    private LocalDateTime resolveDateValue(DateConditionItem.DateValue value) {
        if (value instanceof DateConditionItem.DateValue.Specific specific) {
            String dateStr = specific.getValue();
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    return LocalDate.parse(dateStr).atStartOfDay();
                } catch (Exception e) {
                    log.warn("无法解析日期: {}", dateStr);
                }
            }
        }
        return null;
    }

    // ==================== 关键日期解析 ====================

    private DateRange resolveKeyDateRange(DateConditionItem.KeyDate keyDate) {
        LocalDateTime start = resolveKeyDateStart(keyDate);
        LocalDateTime end = resolveKeyDateEnd(keyDate);
        return new DateRange(start, end);
    }

    private LocalDateTime resolveKeyDateStart(DateConditionItem.KeyDate keyDate) {
        LocalDate today = LocalDate.now();
        switch (keyDate) {
            case TODAY: return today.atStartOfDay();
            case YESTERDAY: return today.minusDays(1).atStartOfDay();
            case TOMORROW: return today.plusDays(1).atStartOfDay();
            case THIS_WEEK: return today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
            case LAST_WEEK: return today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
            case NEXT_WEEK: return today.plusWeeks(1).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
            case THIS_MONTH: return today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
            case LAST_MONTH: return today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
            case NEXT_MONTH: return today.plusMonths(1).with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
            case THIS_QUARTER: return today.with(today.getMonth().firstMonthOfQuarter())
                    .with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
            case THIS_YEAR: return today.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
            default: throw new IllegalArgumentException("Unknown KeyDate: " + keyDate);
        }
    }

    private LocalDateTime resolveKeyDateEnd(DateConditionItem.KeyDate keyDate) {
        LocalDate today = LocalDate.now();
        switch (keyDate) {
            case TODAY: return today.atTime(23, 59, 59);
            case YESTERDAY: return today.minusDays(1).atTime(23, 59, 59);
            case TOMORROW: return today.plusDays(1).atTime(23, 59, 59);
            case THIS_WEEK: return today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)).atTime(23, 59, 59);
            case LAST_WEEK: return today.minusWeeks(1).with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)).atTime(23, 59, 59);
            case NEXT_WEEK: return today.plusWeeks(1).with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)).atTime(23, 59, 59);
            case THIS_MONTH: return today.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
            case LAST_MONTH: return today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
            case NEXT_MONTH: return today.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
            case THIS_QUARTER: return today.with(today.getMonth().firstMonthOfQuarter()).plusMonths(2)
                    .with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
            case THIS_YEAR: return today.with(TemporalAdjusters.lastDayOfYear()).atTime(23, 59, 59);
            default: throw new IllegalArgumentException("Unknown KeyDate: " + keyDate);
        }
    }

    // ==================== 最近时间解析 ====================

    private DateRange resolveRecentRange(DateConditionItem.DateValue.RecentValue recentValue) {
        LocalDateTime start = resolveRecentStart(recentValue);
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);
        return new DateRange(start, end);
    }

    private LocalDateTime resolveRecentStart(DateConditionItem.DateValue.RecentValue recentValue) {
        Integer amount = recentValue.getAmount();
        DateConditionItem.TimeUnit unit = recentValue.getUnit();

        if (amount == null || amount < 0 || unit == null) {
            return null;
        }

        LocalDate now = LocalDate.now();
        LocalDate result;
        switch (unit) {
            case DAY: result = now.minusDays(amount); break;
            case WEEK: result = now.minusWeeks(amount); break;
            case MONTH: result = now.minusMonths(amount); break;
            case QUARTER: result = now.minusMonths(amount * 3L); break;
            case YEAR: result = now.minusYears(amount); break;
            default: throw new IllegalArgumentException("Unknown TimeUnit: " + unit);
        }

        return result.atStartOfDay();
    }

    // ==================== 未来时间解析 ====================

    private DateRange resolveFutureRange(DateConditionItem.DateValue.FutureValue futureValue) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = resolveFutureEnd(futureValue);
        return new DateRange(start, end);
    }

    private LocalDateTime resolveFutureEnd(DateConditionItem.DateValue.FutureValue futureValue) {
        Integer amount = futureValue.getAmount();
        DateConditionItem.TimeUnit unit = futureValue.getUnit();

        if (amount == null || amount < 0 || unit == null) {
            return null;
        }

        LocalDate now = LocalDate.now();
        LocalDate result;
        switch (unit) {
            case DAY: result = now.plusDays(amount); break;
            case WEEK: result = now.plusWeeks(amount); break;
            case MONTH: result = now.plusMonths(amount); break;
            case QUARTER: result = now.plusMonths(amount * 3L); break;
            case YEAR: result = now.plusYears(amount); break;
            default: throw new IllegalArgumentException("Unknown TimeUnit: " + unit);
        }

        return result.atTime(23, 59, 59);
    }

    /**
     * 日期范围记录
     */
    private record DateRange(LocalDateTime start, LocalDateTime end) {
    }
}
