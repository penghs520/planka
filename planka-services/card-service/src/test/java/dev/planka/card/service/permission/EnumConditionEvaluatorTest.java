package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.card.CardId;
import dev.planka.domain.field.EnumFieldValue;
import dev.planka.domain.schema.definition.condition.EnumConditionItem;
import dev.planka.domain.schema.definition.condition.EnumConditionItem.EnumOperator;
import dev.planka.domain.schema.definition.condition.EnumConditionItem.EnumSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnumConditionEvaluator 单元测试")
class EnumConditionEvaluatorTest {

    @Mock
    private CardValueExtractor valueExtractor;

    private EnumConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new EnumConditionEvaluator(valueExtractor);
    }

    private CardDTO createCard(long cardId) {
        CardDTO card = new CardDTO();
        card.setId(CardId.of(cardId));
        return card;
    }

    private EnumConditionItem createCondition(String fieldId, EnumOperator operator) {
        return new EnumConditionItem(new EnumSubject(null, fieldId), operator);
    }

    @Nested
    @DisplayName("Equal 操作符测试")
    class EqualTests {

        @Test
        @DisplayName("当单选值等于期望值时返回 true")
        void evaluate_returnsTrue_whenSingleValueEqualsExpected() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.Equal("high"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of("high"))).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当单选值不等于期望值时返回 false")
        void evaluate_returnsFalse_whenSingleValueNotEqualsExpected() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.Equal("high"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of("low"))).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当多选值时返回 false（Equal 仅适用于单选）")
        void evaluate_returnsFalse_whenMultipleValues() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.Equal("high"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of("high", "medium"))).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当值为空时返回 false")
        void evaluate_returnsFalse_whenValueIsEmpty() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.Equal("high"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("NotEqual 操作符测试")
    class NotEqualTests {

        @Test
        @DisplayName("当单选值不等于期望值时返回 true")
        void evaluate_returnsTrue_whenSingleValueNotEqualsExpected() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.NotEqual("high"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of("low"))).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当单选值等于期望值时返回 false")
        void evaluate_returnsFalse_whenSingleValueEqualsExpected() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.NotEqual("high"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of("high"))).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("In 操作符测试")
    class InTests {

        @Test
        @DisplayName("当实际值在期望列表中时返回 true")
        void evaluate_returnsTrue_whenActualValueInExpectedList() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority",
                new EnumOperator.In(List.of("high", "medium")));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of("high"))).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当任意一个实际值在期望列表中时返回 true（多选场景）")
        void evaluate_returnsTrue_whenAnyActualValueInExpectedList() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("tags",
                new EnumOperator.In(List.of("bug", "feature")));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("tags", List.of("bug", "urgent"))).when(valueExtractor).getFieldValue(card, "tags");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值不在期望列表中时返回 false")
        void evaluate_returnsFalse_whenActualValueNotInExpectedList() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority",
                new EnumOperator.In(List.of("high", "medium")));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of("low"))).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当实际值为空时返回 false")
        void evaluate_returnsFalse_whenActualValueIsEmpty() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority",
                new EnumOperator.In(List.of("high", "medium")));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当期望列表为空时返回 false")
        void evaluate_returnsFalse_whenExpectedListIsEmpty() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority",
                new EnumOperator.In(List.of()));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of("high"))).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("NotIn 操作符测试")
    class NotInTests {

        @Test
        @DisplayName("当实际值不在期望列表中时返回 true")
        void evaluate_returnsTrue_whenActualValueNotInExpectedList() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority",
                new EnumOperator.NotIn(List.of("high", "medium")));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of("low"))).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值在期望列表中时返回 false")
        void evaluate_returnsFalse_whenActualValueInExpectedList() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority",
                new EnumOperator.NotIn(List.of("high", "medium")));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of("high"))).when(valueExtractor).getFieldValue(card, "priority");

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
            EnumConditionItem condition = createCondition("priority", new EnumOperator.IsEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值为空列表时返回 true")
        void evaluate_returnsTrue_whenFieldValueIsEmptyList() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.IsEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of())).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值不为空时返回 false")
        void evaluate_returnsFalse_whenFieldValueIsNotEmpty() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.IsEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of("high"))).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("IsNotEmpty 操作符测试")
    class IsNotEmptyTests {

        @Test
        @DisplayName("当字段值不为空时返回 true")
        void evaluate_returnsTrue_whenFieldValueIsNotEmpty() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.IsNotEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of("high"))).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值为 null 时返回 false")
        void evaluate_returnsFalse_whenFieldValueIsNull() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.IsNotEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "priority");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当字段值为空列表时返回 false")
        void evaluate_returnsFalse_whenFieldValueIsEmptyList() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.IsNotEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new EnumFieldValue("priority", List.of())).when(valueExtractor).getFieldValue(card, "priority");

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
            EnumConditionItem condition = createCondition("priority", new EnumOperator.Equal("high"));

            when(valueExtractor.getCardByPath(any(), any())).thenReturn(null);

            boolean result = evaluator.evaluate(condition, null, null);

            assertThat(result).isFalse();
        }
    }
}
