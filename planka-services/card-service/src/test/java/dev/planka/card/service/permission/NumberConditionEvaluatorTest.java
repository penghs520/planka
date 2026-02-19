package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.card.CardId;
import dev.planka.domain.field.NumberFieldValue;
import dev.planka.domain.schema.definition.condition.NumberConditionItem;
import dev.planka.domain.schema.definition.condition.NumberConditionItem.NumberOperator;
import dev.planka.domain.schema.definition.condition.NumberConditionItem.NumberSubject;
import dev.planka.domain.schema.definition.condition.NumberConditionItem.NumberValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NumberConditionEvaluator 单元测试")
class NumberConditionEvaluatorTest {

    @Mock
    private CardValueExtractor valueExtractor;

    private NumberConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new NumberConditionEvaluator(valueExtractor);
    }

    private CardDTO createCard(long cardId) {
        CardDTO card = new CardDTO();
        card.setId(CardId.of(cardId));
        return card;
    }

    private NumberConditionItem createCondition(String fieldId, NumberOperator operator) {
        return new NumberConditionItem(new NumberSubject(null, fieldId), operator);
    }

    private NumberValue.StaticValue staticValue(Double value) {
        return new NumberValue.StaticValue(value);
    }

    @Nested
    @DisplayName("Equal 操作符测试")
    class EqualTests {

        @Test
        @DisplayName("当实际值等于期望值时返回 true")
        void evaluate_returnsTrue_whenActualEqualsExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.Equal(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值不等于期望值时返回 false")
        void evaluate_returnsFalse_whenActualNotEqualsExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.Equal(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 5.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当实际值为 null 时返回 false")
        void evaluate_returnsFalse_whenActualIsNull() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.Equal(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当期望值和实际值都为 null 时返回 true")
        void evaluate_returnsTrue_whenBothAreNull() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.Equal(staticValue(null)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("NotEqual 操作符测试")
    class NotEqualTests {

        @Test
        @DisplayName("当实际值不等于期望值时返回 true")
        void evaluate_returnsTrue_whenActualNotEqualsExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.NotEqual(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 5.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值等于期望值时返回 false")
        void evaluate_returnsFalse_whenActualEqualsExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.NotEqual(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("GreaterThan 操作符测试")
    class GreaterThanTests {

        @Test
        @DisplayName("当实际值大于期望值时返回 true")
        void evaluate_returnsTrue_whenActualGreaterThanExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.GreaterThan(staticValue(5.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值等于期望值时返回 false")
        void evaluate_returnsFalse_whenActualEqualsExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.GreaterThan(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当实际值小于期望值时返回 false")
        void evaluate_returnsFalse_whenActualLessThanExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.GreaterThan(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 5.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当实际值为 null 时返回 false")
        void evaluate_returnsFalse_whenActualIsNull() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.GreaterThan(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("GreaterThanOrEqual 操作符测试")
    class GreaterThanOrEqualTests {

        @Test
        @DisplayName("当实际值大于期望值时返回 true")
        void evaluate_returnsTrue_whenActualGreaterThanExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.GreaterThanOrEqual(staticValue(5.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值等于期望值时返回 true")
        void evaluate_returnsTrue_whenActualEqualsExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.GreaterThanOrEqual(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值小于期望值时返回 false")
        void evaluate_returnsFalse_whenActualLessThanExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.GreaterThanOrEqual(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 5.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("LessThan 操作符测试")
    class LessThanTests {

        @Test
        @DisplayName("当实际值小于期望值时返回 true")
        void evaluate_returnsTrue_whenActualLessThanExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.LessThan(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 5.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值等于期望值时返回 false")
        void evaluate_returnsFalse_whenActualEqualsExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.LessThan(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当实际值大于期望值时返回 false")
        void evaluate_returnsFalse_whenActualGreaterThanExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.LessThan(staticValue(5.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("LessThanOrEqual 操作符测试")
    class LessThanOrEqualTests {

        @Test
        @DisplayName("当实际值小于期望值时返回 true")
        void evaluate_returnsTrue_whenActualLessThanExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.LessThanOrEqual(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 5.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值等于期望值时返回 true")
        void evaluate_returnsTrue_whenActualEqualsExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.LessThanOrEqual(staticValue(10.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值大于期望值时返回 false")
        void evaluate_returnsFalse_whenActualGreaterThanExpected() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.LessThanOrEqual(staticValue(5.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Between 操作符测试")
    class BetweenTests {

        @Test
        @DisplayName("当实际值在范围内时返回 true")
        void evaluate_returnsTrue_whenActualWithinRange() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.Between(staticValue(5.0), staticValue(15.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值等于下边界时返回 true")
        void evaluate_returnsTrue_whenActualEqualsLowerBound() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.Between(staticValue(5.0), staticValue(15.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 5.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值等于上边界时返回 true")
        void evaluate_returnsTrue_whenActualEqualsUpperBound() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.Between(staticValue(5.0), staticValue(15.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 15.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值小于下边界时返回 false")
        void evaluate_returnsFalse_whenActualBelowLowerBound() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.Between(staticValue(5.0), staticValue(15.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 3.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当实际值大于上边界时返回 false")
        void evaluate_returnsFalse_whenActualAboveUpperBound() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.Between(staticValue(5.0), staticValue(15.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 20.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当实际值为 null 时返回 false")
        void evaluate_returnsFalse_whenActualIsNull() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.Between(staticValue(5.0), staticValue(15.0)));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("IsEmpty 操作符测试")
    class IsEmptyTests {

        @Test
        @DisplayName("当字段值为 null 时返回 true")
        void evaluate_returnsTrue_whenFieldValueIsNull() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points", new NumberOperator.IsEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值不为 null 时返回 false")
        void evaluate_returnsFalse_whenFieldValueIsNotNull() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points", new NumberOperator.IsEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("IsNotEmpty 操作符测试")
    class IsNotEmptyTests {

        @Test
        @DisplayName("当字段值不为 null 时返回 true")
        void evaluate_returnsTrue_whenFieldValueIsNotNull() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points", new NumberOperator.IsNotEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new NumberFieldValue("points", 10.0)).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值为 null 时返回 false")
        void evaluate_returnsFalse_whenFieldValueIsNull() {
            CardDTO card = createCard(1001L);
            NumberConditionItem condition = createCondition("points", new NumberOperator.IsNotEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "points");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("当卡片为 null 时返回 false")
        void evaluate_returnsFalse_whenCardIsNull() {
            NumberConditionItem condition = createCondition("points",
                new NumberOperator.Equal(staticValue(10.0)));

            when(valueExtractor.getCardByPath(any(), any())).thenReturn(null);

            boolean result = evaluator.evaluate(condition, null, null);

            assertThat(result).isFalse();
        }
    }
}
