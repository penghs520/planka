package cn.planka.card.service.evaluator;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.domain.card.CardId;
import cn.planka.domain.field.DateFieldValue;
import cn.planka.domain.field.FieldValue;
import cn.planka.domain.schema.definition.condition.Condition;
import cn.planka.domain.schema.definition.condition.DateConditionItem;
import cn.planka.domain.schema.definition.condition.ReferenceSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * DateConditionEvaluator 单元测试
 * 验证日期条件评估器的各种场景
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DateConditionEvaluator 单元测试")
class DateConditionEvaluatorTest {

    @Mock
    private CardValueExtractor valueExtractor;

    private DateConditionEvaluator evaluator;
    private ConditionResolver conditionResolver;

    @BeforeEach
    void setUp() {
        evaluator = new DateConditionEvaluator(valueExtractor);
        conditionResolver = new ConditionResolver();
    }

    /**
     * 使用 ConditionResolver 解析日期条件，将动态日期转换为静态日期
     */
    private DateConditionItem resolveDateCondition(DateConditionItem item) {
        Condition condition = new Condition(item);
        Condition resolved = conditionResolver.resolve(condition);
        return (DateConditionItem) resolved.getRoot();
    }

    // ==================== 辅助方法 ====================

    private CardDTO createCard(long cardId) {
        CardDTO card = new CardDTO();
        card.setId(CardId.of(cardId));
        card.setFieldValues(new HashMap<>());
        return card;
    }

    private void setFieldValue(CardDTO card, String fieldId, FieldValue<?> fieldValue) {
        card.getFieldValues().put(fieldId, fieldValue);
    }

    private void mockFieldValue(CardDTO card, String fieldId, LocalDateTime dateTime) {
        DateFieldValue fieldValue = new DateFieldValue(fieldId,
                dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        setFieldValue(card, fieldId, fieldValue);
    }

    private DateConditionItem createDateCondition(String fieldId, DateConditionItem.DateOperator operator) {
        var subject = new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId);
        return new DateConditionItem(subject, operator);
    }

    private LocalDateTime toDateTime(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDate today() {
        return LocalDate.now();
    }

    // ==================== 操作符测试 ====================

    @Nested
    @DisplayName("Equal 操作符测试")
    class EqualTests {

        @Test
        @DisplayName("当日期相等时返回 true")
        void shouldReturnTrueWhenDatesEqual() {
            CardDTO card = createCard(1001L);
            LocalDateTime cardDate = toDateTime(today());
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.Specific(today().toString())));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当日期不相等时返回 false")
        void shouldReturnFalseWhenDatesNotEqual() {
            CardDTO card = createCard(1001L);
            LocalDateTime cardDate = toDateTime(today());
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.Specific(today().plusDays(1).toString())));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当实际值为 null 时返回 false")
        void shouldReturnFalseWhenActualValueIsNull() {
            CardDTO card = createCard(1001L);
            // fieldValues 中不包含该字段

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.Specific(today().toString())));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("NotEqual 操作符测试")
    class NotEqualTests {

        @Test
        @DisplayName("当日期不相等时返回 true")
        void shouldReturnTrueWhenDatesNotEqual() {
            CardDTO card = createCard(1001L);
            LocalDateTime cardDate = toDateTime(today());
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.NotEqual(
                            new DateConditionItem.DateValue.Specific(today().plusDays(1).toString())));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当日期相等时返回 false")
        void shouldReturnFalseWhenDatesEqual() {
            CardDTO card = createCard(1001L);
            LocalDateTime cardDate = toDateTime(today());
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.NotEqual(
                            new DateConditionItem.DateValue.Specific(today().toString())));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Before 操作符测试")
    class BeforeTests {

        @Test
        @DisplayName("当日期早于期望值时返回 true")
        void shouldReturnTrueWhenDateIsBefore() {
            CardDTO card = createCard(1001L);
            LocalDateTime cardDate = toDateTime(today().minusDays(1));
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Before(
                            new DateConditionItem.DateValue.Specific(today().toString())));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当日期晚于期望值时返回 false")
        void shouldReturnFalseWhenDateIsAfter() {
            CardDTO card = createCard(1001L);
            LocalDateTime cardDate = toDateTime(today().plusDays(1));
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Before(
                            new DateConditionItem.DateValue.Specific(today().toString())));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Before TODAY 应匹配今天之前的日期")
        void shouldMatchBeforeToday() {
            CardDTO card = createCard(1001L);
            // 昨天在今天之前
            mockFieldValue(card, "dateField", toDateTime(today().minusDays(1)));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Before(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Before THIS_WEEK 应匹配本周开始之前的日期")
        void shouldMatchBeforeThisWeek() {
            CardDTO card = createCard(1001L);
            // 上周日在本周开始之前
            LocalDate lastSunday = today().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).minusDays(1);
            mockFieldValue(card, "dateField", toDateTime(lastSunday));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Before(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_WEEK)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Before 最近7天 应匹配7天前的日期")
        void shouldMatchBeforeRecent7Days() {
            CardDTO card = createCard(1001L);
            // 8天前在"最近7天"开始之前
            mockFieldValue(card, "dateField", toDateTime(today().minusDays(8)));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Before(
                            new DateConditionItem.DateValue.RecentValue(7, DateConditionItem.TimeUnit.DAY)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Before 未来7天 应匹配今天之前的日期")
        void shouldMatchBeforeFuture7Days() {
            CardDTO card = createCard(1001L);
            // 昨天在"未来7天"开始之前
            mockFieldValue(card, "dateField", toDateTime(today().minusDays(1)));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Before(
                            new DateConditionItem.DateValue.FutureValue(7, DateConditionItem.TimeUnit.DAY)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("After 操作符测试")
    class AfterTests {

        @Test
        @DisplayName("当日期晚于期望值时返回 true")
        void shouldReturnTrueWhenDateIsAfter() {
            CardDTO card = createCard(1001L);
            LocalDateTime cardDate = toDateTime(today().plusDays(1));
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.Specific(today().toString())));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当日期早于期望值时返回 false")
        void shouldReturnFalseWhenDateIsBefore() {
            CardDTO card = createCard(1001L);
            LocalDateTime cardDate = toDateTime(today().minusDays(1));
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.Specific(today().toString())));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("After TODAY 应匹配今天之后的日期")
        void shouldMatchAfterToday() {
            CardDTO card = createCard(1001L);
            // 明天在今天之后
            mockFieldValue(card, "dateField", toDateTime(today().plusDays(1)));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("After THIS_WEEK 应匹配本周结束之后的日期")
        void shouldMatchAfterThisWeek() {
            CardDTO card = createCard(1001L);
            // 下周日在本周结束之后
            LocalDate nextSunday = today().with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)).plusDays(1);
            mockFieldValue(card, "dateField", toDateTime(nextSunday));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_WEEK)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("After 最近7天 应匹配明天的日期")
        void shouldMatchAfterRecent7Days() {
            CardDTO card = createCard(1001L);
            // 明天在"最近7天"结束时间（今天）之后
            mockFieldValue(card, "dateField", toDateTime(today().plusDays(1)));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.RecentValue(7, DateConditionItem.TimeUnit.DAY)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("After 未来7天 应匹配7天之后的日期")
        void shouldMatchAfterFuture7Days() {
            CardDTO card = createCard(1001L);
            // 8天后在"未来7天"结束之后
            mockFieldValue(card, "dateField", toDateTime(today().plusDays(8)));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.FutureValue(7, DateConditionItem.TimeUnit.DAY)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Between 操作符测试")
    class BetweenTests {

        @Test
        @DisplayName("当日期在范围内时返回 true")
        void shouldReturnTrueWhenDateIsInRange() {
            CardDTO card = createCard(1001L);
            LocalDateTime cardDate = toDateTime(today());
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.Specific(today().minusDays(1).toString()),
                            new DateConditionItem.DateValue.Specific(today().plusDays(1).toString())));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当日期在范围外时返回 false")
        void shouldReturnFalseWhenDateIsOutOfRange() {
            CardDTO card = createCard(1001L);
            mockFieldValue(card, "dateField", toDateTime(today().plusDays(5)));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.Specific(today().minusDays(1).toString()),
                            new DateConditionItem.DateValue.Specific(today().plusDays(1).toString())));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当边界值为 null 时返回 false")
        void shouldReturnFalseWhenBoundaryIsNull() {
            CardDTO card = createCard(1001L);
            mockFieldValue(card, "dateField", toDateTime(today()));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.Specific(today().minusDays(1).toString()),
                            null));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Between 最近7天和最近1天 应匹配2天前的日期")
        void shouldMatchBetweenRecent7AndRecent1() {
            CardDTO card = createCard(1001L);
            // 2天前在"最近7天"和"最近1天"之间
            mockFieldValue(card, "dateField", toDateTime(today().minusDays(2)));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.RecentValue(7, DateConditionItem.TimeUnit.DAY),
                            new DateConditionItem.DateValue.RecentValue(1, DateConditionItem.TimeUnit.DAY)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Between 未来1天和未来7天 应匹配3天后的日期")
        void shouldMatchBetweenFuture1AndFuture7() {
            CardDTO card = createCard(1001L);
            // 3天后在"未来1天"和"未来7天"之间
            mockFieldValue(card, "dateField", toDateTime(today().plusDays(3)));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.FutureValue(1, DateConditionItem.TimeUnit.DAY),
                            new DateConditionItem.DateValue.FutureValue(7, DateConditionItem.TimeUnit.DAY)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Between THIS_WEEK和NEXT_WEEK 应匹配下周的日期")
        void shouldMatchBetweenThisWeekAndNextWeek() {
            CardDTO card = createCard(1001L);
            // 下周一在"本周"和"下周"之间
            LocalDate nextMonday = today().plusWeeks(1).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            mockFieldValue(card, "dateField", toDateTime(nextMonday));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_WEEK),
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.NEXT_WEEK)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Between THIS_MONTH和NEXT_MONTH 应匹配下月的日期")
        void shouldMatchBetweenThisMonthAndNextMonth() {
            CardDTO card = createCard(1001L);
            // 下月1号在"本月"和"下月"之间
            LocalDate nextMonthDay1 = today().plusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
            mockFieldValue(card, "dateField", toDateTime(nextMonthDay1));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_MONTH),
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.NEXT_MONTH)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("IsEmpty 操作符测试")
    class IsEmptyTests {

        @Test
        @DisplayName("当日期值为 null 时返回 true")
        void shouldReturnTrueWhenValueIsNull() {
            CardDTO card = createCard(1001L);
            // fieldValues 中不包含该字段

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.IsEmpty());

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当日期值不为 null 时返回 false")
        void shouldReturnFalseWhenValueIsNotNull() {
            CardDTO card = createCard(1001L);
            mockFieldValue(card, "dateField", toDateTime(today()));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.IsEmpty());

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("IsNotEmpty 操作符测试")
    class IsNotEmptyTests {

        @Test
        @DisplayName("当日期值不为 null 时返回 true")
        void shouldReturnTrueWhenValueIsNotNull() {
            CardDTO card = createCard(1001L);
            mockFieldValue(card, "dateField", toDateTime(today()));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.IsNotEmpty());

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当日期值为 null 时返回 false")
        void shouldReturnFalseWhenValueIsNull() {
            CardDTO card = createCard(1001L);
            // fieldValues 中不包含该字段

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.IsNotEmpty());

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isFalse();
        }
    }

    // ==================== 关键日期测试 ====================

    @Nested
    @DisplayName("关键日期解析测试")
    class KeyDateTests {

        @Test
        @DisplayName("TODAY 应正确解析")
        void shouldResolveToday() {
            CardDTO card = createCard(1001L);
            LocalDateTime cardDate = toDateTime(today());
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)));

            // 使用 ConditionResolver 预先解析动态日期
            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("YESTERDAY 应正确解析")
        void shouldResolveYesterday() {
            CardDTO card = createCard(1001L);
            LocalDateTime cardDate = toDateTime(today().minusDays(1));
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.YESTERDAY)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("THIS_WEEK 应匹配本周内任意一天")
        void shouldResolveThisWeek() {
            CardDTO card = createCard(1001L);
            // 测试本周三
            LocalDate thisWednesday = today().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).plusDays(2);
            mockFieldValue(card, "dateField", toDateTime(thisWednesday));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_WEEK)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("THIS_WEEK 不应匹配上周的日期")
        void shouldNotResolveLastWeekAsThisWeek() {
            CardDTO card = createCard(1001L);
            // 上周三
            LocalDate lastWednesday = today().minusWeeks(1).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).plusDays(2);
            mockFieldValue(card, "dateField", toDateTime(lastWednesday));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_WEEK)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("THIS_MONTH 应匹配本月内任意一天")
        void shouldResolveThisMonth() {
            CardDTO card = createCard(1001L);
            // 本月15号
            LocalDate day15 = today().with(TemporalAdjusters.firstDayOfMonth()).plusDays(14);
            mockFieldValue(card, "dateField", toDateTime(day15));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_MONTH)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("THIS_MONTH 不应匹配上月的日期")
        void shouldNotResolveLastMonthAsThisMonth() {
            CardDTO card = createCard(1001L);
            // 上月15号
            LocalDate lastMonthDay15 = today().minusMonths(1).with(TemporalAdjusters.firstDayOfMonth()).plusDays(14);
            mockFieldValue(card, "dateField", toDateTime(lastMonthDay15));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_MONTH)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isFalse();
        }
    }

    // ==================== 最近/未来时间测试 ====================

    @Nested
    @DisplayName("最近时间范围测试")
    class RecentValueTests {

        @Test
        @DisplayName("Between 最近3天和今天 应匹配2天前的日期")
        void shouldResolveRecent3Days() {
            CardDTO card = createCard(1001L);
            // 卡片日期是2天前（在"最近3天"范围内）
            LocalDateTime cardDate = toDateTime(today().minusDays(2));
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.RecentValue(3, DateConditionItem.TimeUnit.DAY),
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Between 最近1个月和今天 应匹配15天前的日期")
        void shouldResolveRecent1Month() {
            CardDTO card = createCard(1001L);
            // 卡片日期是15天前（在"最近1个月"范围内）
            LocalDateTime cardDate = toDateTime(today().minusDays(15));
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.RecentValue(1, DateConditionItem.TimeUnit.MONTH),
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("未来时间范围测试")
    class FutureValueTests {

        @Test
        @DisplayName("Between 今天和未来7天 应匹配6天后的日期")
        void shouldResolveFuture7Days() {
            CardDTO card = createCard(1001L);
            // 卡片日期是6天后（在"未来7天"范围内）
            LocalDateTime cardDate = toDateTime(today().plusDays(6));
            mockFieldValue(card, "dateField", cardDate);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Between(
                            new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY),
                            new DateConditionItem.DateValue.FutureValue(7, DateConditionItem.TimeUnit.DAY)));

            var resolvedItem = resolveDateCondition(item);
            boolean result = evaluator.evaluate(resolvedItem, card, null);

            assertThat(result).isTrue();
        }
    }

    // ==================== 引用日期测试 ====================

    @Nested
    @DisplayName("引用日期测试")
    class ReferenceValueTests {

        @Test
        @DisplayName("引用日期应正确解析")
        void shouldResolveReferenceValue() {
            CardDTO targetCard = createCard(1001L);
            CardDTO refCard = createCard(2001L);
            LocalDateTime today = toDateTime(today());

            var refSource = new ReferenceSource.CurrentCard(null);
            var refValue = new DateConditionItem.DateValue.ReferenceValue(refSource, "refDateField");

            // 设置 targetCard 的字段值
            DateFieldValue targetFieldValue = new DateFieldValue("dateField",
                    today.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            setFieldValue(targetCard, "dateField", targetFieldValue);

            // 设置 refCard 的字段值（与 targetCard 相同）
            when(valueExtractor.getCardByReferenceSource(refSource, targetCard, null))
                    .thenReturn(refCard);
            DateFieldValue refFieldValue = new DateFieldValue("refDateField",
                    today.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            setFieldValue(refCard, "refDateField", refFieldValue);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(refValue));

            boolean result = evaluator.evaluate(item, targetCard, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当引用卡片为 null 时返回 false")
        void shouldReturnFalseWhenRefCardIsNull() {
            CardDTO targetCard = createCard(1001L);

            var refSource = new ReferenceSource.CurrentCard(null);
            var refValue = new DateConditionItem.DateValue.ReferenceValue(refSource, "refDateField");

            // 设置 targetCard 的字段值
            DateFieldValue targetFieldValue = new DateFieldValue("dateField",
                    toDateTime(today()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            setFieldValue(targetCard, "dateField", targetFieldValue);

            when(valueExtractor.getCardByReferenceSource(refSource, targetCard, null))
                    .thenReturn(null);

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(refValue));

            boolean result = evaluator.evaluate(item, targetCard, null);

            assertThat(result).isFalse();
        }
    }

    // ==================== 系统日期字段测试 ====================

    @Nested
    @DisplayName("系统日期字段测试")
    class SystemDateFieldTests {

        @Test
        @DisplayName("CREATED_AT 系统字段应正确提取")
        void shouldExtractCreatedAt() {
            CardDTO card = createCard(1001L);
            LocalDateTime createdAt = toDateTime(today());

            var subject = new DateConditionItem.DateSubject.SystemDateSubject(
                    null, DateConditionItem.SystemDateField.CREATED_AT);
            var item = new DateConditionItem(subject,
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.Specific(today().toString())));

            when(valueExtractor.getSystemDateValue(card, DateConditionItem.SystemDateField.CREATED_AT))
                    .thenReturn(createdAt);

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isTrue();
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("无效的具体日期格式应返回 false")
        void shouldReturnFalseForInvalidDateFormat() {
            CardDTO card = createCard(1001L);
            mockFieldValue(card, "dateField", toDateTime(today()));

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.Equal(
                            new DateConditionItem.DateValue.Specific("invalid-date")));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("KeyDateValue 构造函数应拒绝 null 值")
        void shouldRejectNullKeyDate() {
            assertThatThrownBy(() -> new DateConditionItem.DateValue.KeyDateValue(null))
                    .hasMessageContaining("keyDate can't be null");
        }

        @Test
        @DisplayName("负数的 RecentValue 应返回 false")
        void shouldReturnFalseForNegativeRecentValue() {
            CardDTO card = createCard(1001L);
            // fieldValues 中不包含该字段

            var item = createDateCondition("dateField",
                    new DateConditionItem.DateOperator.After(
                            new DateConditionItem.DateValue.RecentValue(-1, DateConditionItem.TimeUnit.DAY)));

            boolean result = evaluator.evaluate(item, card, null);

            assertThat(result).isFalse();
        }
    }
}
