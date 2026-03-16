package cn.planka.card.converter;

import cn.planka.card.service.evaluator.ConditionResolver;
import cn.planka.domain.schema.definition.condition.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConditionConverter 日期条件转换单元测试
 * 验证关键日期、最近/未来时间范围的正确转换
 */
@DisplayName("ConditionConverter 日期条件转换测试")
class ConditionConverterDateTest {

    private ConditionResolver conditionResolver;

    @BeforeEach
    void setUp() {
        conditionResolver = new ConditionResolver();
    }

    // ==================== 辅助方法 ====================

    private DateConditionItem createDateCondition(String fieldId, DateConditionItem.DateOperator operator) {
        var subject = new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId);
        return new DateConditionItem(subject, operator);
    }

    /**
     * 使用 ConditionResolver 解析条件，将动态日期转换为静态日期
     */
    private Condition resolveCondition(Condition condition) {
        return conditionResolver.resolve(condition);
    }

    private long toMillis(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private long toEndOfDayMillis(LocalDate date) {
        return date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private LocalDate today() {
        return LocalDate.now();
    }

    // ==================== 关键日期测试 ====================

    @Nested
    @DisplayName("关键日期转换测试")
    class KeyDateTests {

        @Test
        @DisplayName("等于 TODAY 应转换为 Between [今天开始, 今天最后一秒]")
        void shouldConvertEqualTodayToBetweenTodayStartAndTodayEnd() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            assertThat(proto.getRoot().hasDate()).isTrue();
            var dateProto = proto.getRoot().getDate();
            assertThat(dateProto.getOperator().hasBetween()).isTrue();

            long expectedStartMillis = toMillis(today());
            long expectedEndMillis = toEndOfDayMillis(today());
            assertThat(dateProto.getOperator().getBetween().getStaticStartValue()).isEqualTo(expectedStartMillis);
            assertThat(dateProto.getOperator().getBetween().getStaticEndValue()).isEqualTo(expectedEndMillis);
        }

        @Test
        @DisplayName("等于 YESTERDAY 应转换为 Between [昨天开始, 昨天最后一秒]")
        void shouldConvertEqualYesterdayToBetweenYesterdayStartAndYesterdayEnd() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.YESTERDAY)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            long expectedStartMillis = toMillis(today().minusDays(1));
            long expectedEndMillis = toEndOfDayMillis(today().minusDays(1));
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticStartValue())
                    .isEqualTo(expectedStartMillis);
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticEndValue())
                    .isEqualTo(expectedEndMillis);
        }

        @Test
        @DisplayName("等于 TOMORROW 应转换为 Between [明天开始, 明天最后一秒]")
        void shouldConvertEqualTomorrowToBetweenTomorrowStartAndTomorrowEnd() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TOMORROW)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            long expectedStartMillis = toMillis(today().plusDays(1));
            long expectedEndMillis = toEndOfDayMillis(today().plusDays(1));
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticStartValue())
                    .isEqualTo(expectedStartMillis);
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticEndValue())
                    .isEqualTo(expectedEndMillis);
        }

        @Test
        @DisplayName("等于 THIS_WEEK 应转换为 Between [本周一, 本周日最后一秒]")
        void shouldConvertEqualThisWeekToBetweenMondayAndSundayEnd() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_WEEK)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            LocalDate thisMonday = today().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            LocalDate thisSunday = thisMonday.plusDays(6);
            long expectedStartMillis = toMillis(thisMonday);
            long expectedEndMillis = toEndOfDayMillis(thisSunday);
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticStartValue())
                    .isEqualTo(expectedStartMillis);
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticEndValue())
                    .isEqualTo(expectedEndMillis);
        }

        @Test
        @DisplayName("等于 THIS_MONTH 应转换为 Between [本月1号, 本月最后一日最后一秒]")
        void shouldConvertEqualThisMonthToBetweenFirstDayAndLastDayEnd() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_MONTH)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            LocalDate firstDayOfMonth = today().with(TemporalAdjusters.firstDayOfMonth());
            LocalDate lastDayOfMonth = today().with(TemporalAdjusters.lastDayOfMonth());
            long expectedStartMillis = toMillis(firstDayOfMonth);
            long expectedEndMillis = toEndOfDayMillis(lastDayOfMonth);
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticStartValue())
                    .isEqualTo(expectedStartMillis);
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticEndValue())
                    .isEqualTo(expectedEndMillis);
        }

        @Test
        @DisplayName("等于 THIS_YEAR 应转换为 Between [本年1月1号, 本年最后一日最后一秒]")
        void shouldConvertEqualThisYearToBetweenFirstDayAndLastDayEnd() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_YEAR)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            LocalDate firstDayOfYear = today().with(TemporalAdjusters.firstDayOfYear());
            LocalDate lastDayOfYear = today().with(TemporalAdjusters.lastDayOfYear());
            long expectedStartMillis = toMillis(firstDayOfYear);
            long expectedEndMillis = toEndOfDayMillis(lastDayOfYear);
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticStartValue())
                    .isEqualTo(expectedStartMillis);
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticEndValue())
                    .isEqualTo(expectedEndMillis);
        }
    }

    // ==================== 最近时间范围测试 ====================

    @Nested
    @DisplayName("最近时间范围转换测试")
    class RecentValueTests {

        @Test
        @DisplayName("After 最近3天应转换为今天的日期（最近3天的结束时间）")
        void shouldConvertAfterRecent3Days() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.RecentValue(3, DateConditionItem.TimeUnit.DAY)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            // After + RecentValue -> After 结束时间（今天23:59:59）
            long expectedMillis = toEndOfDayMillis(today());
            assertThat(proto.getRoot().getDate().getOperator().getAfter().getStaticValue())
                    .isEqualTo(expectedMillis);
        }

        @Test
        @DisplayName("After 最近2周应转换为今天的最后一秒")
        void shouldConvertAfterRecent2Weeks() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.RecentValue(2, DateConditionItem.TimeUnit.WEEK)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            long expectedMillis = toEndOfDayMillis(today());
            assertThat(proto.getRoot().getDate().getOperator().getAfter().getStaticValue())
                    .isEqualTo(expectedMillis);
        }

        @Test
        @DisplayName("After 最近1个月应转换为今天的最后一秒")
        void shouldConvertAfterRecent1Month() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.RecentValue(1, DateConditionItem.TimeUnit.MONTH)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            long expectedMillis = toEndOfDayMillis(today());
            assertThat(proto.getRoot().getDate().getOperator().getAfter().getStaticValue())
                    .isEqualTo(expectedMillis);
        }

        @Test
        @DisplayName("After 最近3个季度应转换为今天的最后一秒")
        void shouldConvertAfterRecent3Quarters() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.RecentValue(3, DateConditionItem.TimeUnit.QUARTER)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            long expectedMillis = toEndOfDayMillis(today());
            assertThat(proto.getRoot().getDate().getOperator().getAfter().getStaticValue())
                    .isEqualTo(expectedMillis);
        }

        @Test
        @DisplayName("After 最近1年应转换为今天的最后一秒")
        void shouldConvertAfterRecent1Year() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.RecentValue(1, DateConditionItem.TimeUnit.YEAR)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            long expectedMillis = toEndOfDayMillis(today());
            assertThat(proto.getRoot().getDate().getOperator().getAfter().getStaticValue())
                    .isEqualTo(expectedMillis);
        }
    }

    // ==================== 未来时间范围测试 ====================

    @Nested
    @DisplayName("未来时间范围转换测试")
    class FutureValueTests {

        @Test
        @DisplayName("Before 未来7天应转换为今天的日期（未来7天的开始时间）")
        void shouldConvertBeforeFuture7Days() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Before(
                            new DateConditionItem.DateValue.FutureValue(7, DateConditionItem.TimeUnit.DAY)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            // Before + FutureValue -> Before 开始时间（今天）
            long expectedMillis = toMillis(today());
            assertThat(proto.getRoot().getDate().getOperator().getBefore().getStaticValue())
                    .isEqualTo(expectedMillis);
        }

        @Test
        @DisplayName("Before 未来30天应转换为今天的日期")
        void shouldConvertBeforeFuture30Days() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Before(
                            new DateConditionItem.DateValue.FutureValue(30, DateConditionItem.TimeUnit.DAY)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            long expectedMillis = toMillis(today());
            assertThat(proto.getRoot().getDate().getOperator().getBefore().getStaticValue())
                    .isEqualTo(expectedMillis);
        }

        @Test
        @DisplayName("Before 未来2个月应转换为今天的日期")
        void shouldConvertBeforeFuture2Months() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Before(
                            new DateConditionItem.DateValue.FutureValue(2, DateConditionItem.TimeUnit.MONTH)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            long expectedMillis = toMillis(today());
            assertThat(proto.getRoot().getDate().getOperator().getBefore().getStaticValue())
                    .isEqualTo(expectedMillis);
        }
    }

    // ==================== Between 操作符测试 ====================

    @Nested
    @DisplayName("Between 操作符转换测试")
    class BetweenTests {

        @Test
        @DisplayName("Between 两个 KeyDate 应正确转换")
        void shouldConvertBetweenTwoKeyDates() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_WEEK),
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.NEXT_WEEK)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            var betweenOp = proto.getRoot().getDate().getOperator().getBetween();

            LocalDate thisMonday = today().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            // ConditionResolver 使用 resolveDateEnd(NEXT_WEEK) = 下周日最后一秒
            LocalDate nextWeekSunday = today().plusWeeks(1).with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));

            assertThat(betweenOp.getStaticStartValue()).isEqualTo(toMillis(thisMonday));
            assertThat(betweenOp.getStaticEndValue()).isEqualTo(toEndOfDayMillis(nextWeekSunday));
        }

        @Test
        @DisplayName("Between 最近7天和未来7天应正确转换")
        void shouldConvertBetweenRecentAndFuture() {
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.RecentValue(7, DateConditionItem.TimeUnit.DAY),
                            new DateConditionItem.DateValue.FutureValue(7, DateConditionItem.TimeUnit.DAY)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            var betweenOp = proto.getRoot().getDate().getOperator().getBetween();

            LocalDate sevenDaysAgo = today().minusDays(7);
            // ConditionResolver 解析 FutureValue(7, DAY) 为未来第7天的最后一秒
            LocalDate sevenDaysLater = today().plusDays(7);

            assertThat(betweenOp.getStaticStartValue()).isEqualTo(toMillis(sevenDaysAgo));
            assertThat(betweenOp.getStaticEndValue()).isEqualTo(toEndOfDayMillis(sevenDaysLater));
        }
    }

    // ==================== 具体日期测试 ====================

    @Nested
    @DisplayName("具体日期转换测试")
    class SpecificDateTests {

        @Test
        @DisplayName("具体日期字符串应转换为 Between 包含完整一天")
        void shouldParseSpecificDateString() {
            String dateStr = "2024-03-15";
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.Specific(dateStr)));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            // Equal + 静态日期值 转换为 Between [当天开始, 当天最后一毫秒]
            LocalDate date = LocalDate.parse(dateStr);
            long expectedStartMillis = toMillis(date);
            long expectedEndMillis = toEndOfDayMillis(date);
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticStartValue())
                    .isEqualTo(expectedStartMillis);
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticEndValue())
                    .isEqualTo(expectedEndMillis);
        }

        @Test
        @DisplayName("毫秒时间戳字符串应转换为 Between 包含完整一天")
        void shouldParseMillisTimestamp() {
            long timestamp = 1710000000000L;
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.Specific(String.valueOf(timestamp))));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            // Equal + 毫秒时间戳 转换为 Between [当天开始, 当天最后一毫秒]
            // timestamp 被解析为当天的开始时间
            LocalDate date = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            long expectedStartMillis = toMillis(date);
            long expectedEndMillis = toEndOfDayMillis(date);
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticStartValue())
                    .isEqualTo(expectedStartMillis);
            assertThat(proto.getRoot().getDate().getOperator().getBetween().getStaticEndValue())
                    .isEqualTo(expectedEndMillis);
        }
    }

    // ==================== 引用日期测试 ====================

    @Nested
    @DisplayName("引用日期转换测试")
    class ReferenceDateTests {

        @Test
        @DisplayName("引用日期应转换为 ReferDate proto")
        void shouldConvertReferenceValueToReferDate() {
            var refSource = new ReferenceSource.CurrentCard(null);
            var refValue = new DateConditionItem.DateValue.ReferenceValue(refSource, "created_at");
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(refValue));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            var equalOp = proto.getRoot().getDate().getOperator().getEqual();
            assertThat(equalOp.hasReferDate()).isTrue();
            assertThat(equalOp.getReferDate().getFieldId()).isEqualTo("created_at");
            assertThat(equalOp.getReferDate().hasReferOnCurrentCard()).isTrue();
        }

        @Test
        @DisplayName("Before 操作符的引用日期应正确转换")
        void shouldConvertBeforeWithReferenceValue() {
            var refSource = new ReferenceSource.Member(null);
            var refValue = new DateConditionItem.DateValue.ReferenceValue(refSource, "deadline");
            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Before(refValue));

            var proto = ConditionConverter.toProto(resolveCondition(Condition.of(item)));

            var beforeOp = proto.getRoot().getDate().getOperator().getBefore();
            assertThat(beforeOp.hasReferDate()).isTrue();
            assertThat(beforeOp.getReferDate().getFieldId()).isEqualTo("deadline");
            assertThat(beforeOp.getReferDate().hasReferOnMember()).isTrue();
        }
    }
}
