package cn.planka.card.service.evaluator;

import cn.planka.domain.schema.definition.condition.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConditionResolver 单元测试
 * 验证动态日期解析器的各种场景
 */
@DisplayName("ConditionResolver 单元测试")
class ConditionResolverTest {

    private ConditionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ConditionResolver();
    }

    private DateConditionItem createDateCondition(String fieldId, DateConditionItem.DateOperator operator) {
        var subject = new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId);
        return new DateConditionItem(subject, operator);
    }

    private LocalDate today() {
        return LocalDate.now();
    }

    private LocalDateTime toDateTime(LocalDate date) {
        return date.atStartOfDay();
    }

    // ==================== 关键日期解析测试 ====================

    @Nested
    @DisplayName("关键日期解析测试")
    class KeyDateTests {

        @Test
        @DisplayName("Equal + TODAY 应转换为 Between [今天开始, 今天结束]")
        void shouldConvertEqualTodayToBetween() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            assertThat(resolvedItem.getOperator()).isInstanceOf(DateConditionItem.DateOperator.Between.class);

            var between = (DateConditionItem.DateOperator.Between) resolvedItem.getOperator();
            assertThat(between.getStart()).isInstanceOf(DateConditionItem.DateValue.Specific.class);
            assertThat(between.getEnd()).isInstanceOf(DateConditionItem.DateValue.Specific.class);
        }

        @Test
        @DisplayName("Equal + THIS_WEEK 应转换为 Between [本周一, 本周日]")
        void shouldConvertEqualThisWeekToBetween() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_WEEK)));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            var between = (DateConditionItem.DateOperator.Between) resolvedItem.getOperator();

            String startStr = ((DateConditionItem.DateValue.Specific) between.getStart()).getValue();
            String endStr = ((DateConditionItem.DateValue.Specific) between.getEnd()).getValue();

            LocalDate thisMonday = today().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            LocalDate thisSunday = thisMonday.plusDays(6);

            assertThat(startStr).isEqualTo(thisMonday.toString());
            // 结束时间是本周日（同一天）
            assertThat(endStr).isEqualTo(thisSunday.toString());
        }

        @Test
        @DisplayName("Equal + THIS_MONTH 应转换为 Between [本月1号, 本月最后一天]")
        void shouldConvertEqualThisMonthToBetween() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_MONTH)));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            var between = (DateConditionItem.DateOperator.Between) resolvedItem.getOperator();

            String startStr = ((DateConditionItem.DateValue.Specific) between.getStart()).getValue();
            String endStr = ((DateConditionItem.DateValue.Specific) between.getEnd()).getValue();

            LocalDate firstDay = today().with(TemporalAdjusters.firstDayOfMonth());
            LocalDate lastDay = today().with(TemporalAdjusters.lastDayOfMonth());

            assertThat(startStr).isEqualTo(firstDay.toString());
            // 结束时间是本月最后一天（同一天）
            assertThat(endStr).isEqualTo(lastDay.toString());
        }

        @Test
        @DisplayName("NotEqual + TODAY 应转换为 NotBetween [今天开始, 今天结束]")
        void shouldConvertNotEqualTodayToNotBetween() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.NotEqual(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            assertThat(resolvedItem.getOperator()).isInstanceOf(DateConditionItem.DateOperator.NotBetween.class);
        }
    }

    // ==================== 最近时间范围解析测试 ====================

    @Nested
    @DisplayName("最近时间范围解析测试")
    class RecentValueTests {

        @Test
        @DisplayName("Equal + 最近3天 应转换为 Between [3天前, 今天]")
        void shouldConvertEqualRecent3DaysToBetween() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.RecentValue(3, DateConditionItem.TimeUnit.DAY)));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            var between = (DateConditionItem.DateOperator.Between) resolvedItem.getOperator();

            String startStr = ((DateConditionItem.DateValue.Specific) between.getStart()).getValue();
            String endStr = ((DateConditionItem.DateValue.Specific) between.getEnd()).getValue();

            assertThat(startStr).isEqualTo(today().minusDays(3).toString());
            // 结束时间是今天（同一天）
            assertThat(endStr).isEqualTo(today().toString());
        }

        @Test
        @DisplayName("After + 最近7天 应转换为 After 今天")
        void shouldConvertAfterRecent7DaysToAfterToday() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.RecentValue(7, DateConditionItem.TimeUnit.DAY)));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            var after = (DateConditionItem.DateOperator.After) resolvedItem.getOperator();

            String dateStr = ((DateConditionItem.DateValue.Specific) after.getValue()).getValue();
            assertThat(dateStr).isEqualTo(today().toString());
        }

        @Test
        @DisplayName("Before + 最近1个月 应转换为 Before 1个月前")
        void shouldConvertBeforeRecent1MonthToBeforeOneMonthAgo() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Before(
                            new DateConditionItem.DateValue.RecentValue(1, DateConditionItem.TimeUnit.MONTH)));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            var before = (DateConditionItem.DateOperator.Before) resolvedItem.getOperator();

            String dateStr = ((DateConditionItem.DateValue.Specific) before.getValue()).getValue();
            assertThat(dateStr).isEqualTo(today().minusMonths(1).toString());
        }
    }

    // ==================== 未来时间范围解析测试 ====================

    @Nested
    @DisplayName("未来时间范围解析测试")
    class FutureValueTests {

        @Test
        @DisplayName("Equal + 未来7天 应转换为 Between [今天, 7天后]")
        void shouldConvertEqualFuture7DaysToBetween() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.FutureValue(7, DateConditionItem.TimeUnit.DAY)));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            var between = (DateConditionItem.DateOperator.Between) resolvedItem.getOperator();

            String startStr = ((DateConditionItem.DateValue.Specific) between.getStart()).getValue();
            String endStr = ((DateConditionItem.DateValue.Specific) between.getEnd()).getValue();

            assertThat(startStr).isEqualTo(today().toString());
            // 结束时间是7天后（同一天）
            assertThat(endStr).isEqualTo(today().plusDays(7).toString());
        }

        @Test
        @DisplayName("Before + 未来30天 应转换为 Before 今天")
        void shouldConvertBeforeFuture30DaysToBeforeToday() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Before(
                            new DateConditionItem.DateValue.FutureValue(30, DateConditionItem.TimeUnit.DAY)));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            var before = (DateConditionItem.DateOperator.Before) resolvedItem.getOperator();

            String dateStr = ((DateConditionItem.DateValue.Specific) before.getValue()).getValue();
            assertThat(dateStr).isEqualTo(today().toString());
        }

        @Test
        @DisplayName("After + 未来2周 应转换为 After 2周后")
        void shouldConvertAfterFuture2WeeksToAfterTwoWeeksLater() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.FutureValue(2, DateConditionItem.TimeUnit.WEEK)));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            var after = (DateConditionItem.DateOperator.After) resolvedItem.getOperator();

            String dateStr = ((DateConditionItem.DateValue.Specific) after.getValue()).getValue();
            assertThat(dateStr).isEqualTo(today().plusWeeks(2).toString());
        }
    }

    // ==================== Between 操作符解析测试 ====================

    @Nested
    @DisplayName("Between 操作符解析测试")
    class BetweenTests {

        @Test
        @DisplayName("Between + 动态日期 应正确解析两端")
        void shouldConvertBetweenWithDynamicDates() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.RecentValue(7, DateConditionItem.TimeUnit.DAY),
                            new DateConditionItem.DateValue.FutureValue(7, DateConditionItem.TimeUnit.DAY)));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            var between = (DateConditionItem.DateOperator.Between) resolvedItem.getOperator();

            String startStr = ((DateConditionItem.DateValue.Specific) between.getStart()).getValue();
            String endStr = ((DateConditionItem.DateValue.Specific) between.getEnd()).getValue();

            // Start: 7天前的开始时间
            assertThat(startStr).isEqualTo(today().minusDays(7).toString());
            // End: 7天后的结束时间（同一天）
            assertThat(endStr).isEqualTo(today().plusDays(7).toString());
        }

        @Test
        @DisplayName("Between + 静态日期 应保持不变")
        void shouldKeepStaticBetweenUnchanged() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.Specific("2024-03-01"),
                            new DateConditionItem.DateValue.Specific("2024-03-31")));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            var between = (DateConditionItem.DateOperator.Between) resolvedItem.getOperator();

            String startStr = ((DateConditionItem.DateValue.Specific) between.getStart()).getValue();
            String endStr = ((DateConditionItem.DateValue.Specific) between.getEnd()).getValue();

            assertThat(startStr).isEqualTo("2024-03-01");
            assertThat(endStr).isEqualTo("2024-03-31");
        }
    }

    // ==================== 条件组合解析测试 ====================

    @Nested
    @DisplayName("条件组合解析测试")
    class GroupTests {

        @Test
        @DisplayName("AND 组合应解析所有子条件")
        void shouldResolveAllChildrenInAndGroup() {
            var item1 = createDateCondition("dateField1",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)));
            var item2 = createDateCondition("dateField2",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.RecentValue(7, DateConditionItem.TimeUnit.DAY)));

            var group = new ConditionGroup(ConditionGroup.LogicOperator.AND, new java.util.ArrayList<>());
            group.getChildren().add(item1);
            group.getChildren().add(item2);

            Condition result = resolver.resolve(Condition.of(group));

            var resolvedGroup = (ConditionGroup) result.getRoot();
            assertThat(resolvedGroup.getChildren()).hasSize(2);

            var child1 = (DateConditionItem) resolvedGroup.getChildren().get(0);
            var child2 = (DateConditionItem) resolvedGroup.getChildren().get(1);

            assertThat(child1.getOperator()).isInstanceOf(DateConditionItem.DateOperator.Between.class);
            assertThat(child2.getOperator()).isInstanceOf(DateConditionItem.DateOperator.Between.class);
        }

        @Test
        @DisplayName("OR 组合应解析所有子条件")
        void shouldResolveAllChildrenInOrGroup() {
            var item1 = createDateCondition("dateField1",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)));
            var item2 = createDateCondition("dateField2",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.FutureValue(7, DateConditionItem.TimeUnit.DAY)));

            var group = new ConditionGroup(ConditionGroup.LogicOperator.OR, new java.util.ArrayList<>());
            group.getChildren().add(item1);
            group.getChildren().add(item2);

            Condition result = resolver.resolve(Condition.of(group));

            var resolvedGroup = (ConditionGroup) result.getRoot();
            assertThat(resolvedGroup.getChildren()).hasSize(2);

            var child1 = (DateConditionItem) resolvedGroup.getChildren().get(0);
            var child2 = (DateConditionItem) resolvedGroup.getChildren().get(1);

            assertThat(child1.getOperator()).isInstanceOf(DateConditionItem.DateOperator.Between.class);
            assertThat(child2.getOperator()).isInstanceOf(DateConditionItem.DateOperator.Between.class);
        }
    }

    // ==================== 边界情况测试 ====================

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("空条件应返回原条件")
        void shouldReturnEmptyConditionAsIs() {
            Condition emptyCondition = new Condition(null);
            Condition result = resolver.resolve(emptyCondition);
            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("null 条件应返回 null")
        void shouldReturnNullConditionAsIs() {
            Condition result = resolver.resolve(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("静态日期值应保持不变")
        void shouldKeepStaticValueUnchanged() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.Specific("2024-03-15")));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            var equal = (DateConditionItem.DateOperator.Equal) resolvedItem.getOperator();

            String dateStr = ((DateConditionItem.DateValue.Specific) equal.getValue()).getValue();
            assertThat(dateStr).isEqualTo("2024-03-15");
        }

        @Test
        @DisplayName("引用日期值应保持不变")
        void shouldKeepReferenceValueUnchanged() {
            var refSource = new ReferenceSource.CurrentCard(null);
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.ReferenceValue(refSource, "refField")));

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            var equal = (DateConditionItem.DateOperator.Equal) resolvedItem.getOperator();

            assertThat(equal.getValue()).isInstanceOf(DateConditionItem.DateValue.ReferenceValue.class);
        }

        @Test
        @DisplayName("IsEmpty 操作符应保持不变")
        void shouldKeepIsEmptyUnchanged() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.IsEmpty());

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            assertThat(resolvedItem.getOperator()).isInstanceOf(DateConditionItem.DateOperator.IsEmpty.class);
        }

        @Test
        @DisplayName("IsNotEmpty 操作符应保持不变")
        void shouldKeepIsNotEmptyUnchanged() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.IsNotEmpty());

            Condition result = resolver.resolve(Condition.of(item));

            var resolvedItem = (DateConditionItem) result.getRoot();
            assertThat(resolvedItem.getOperator()).isInstanceOf(DateConditionItem.DateOperator.IsNotEmpty.class);
        }
    }
}
