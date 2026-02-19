package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.card.CardId;
import dev.planka.domain.field.TextFieldValue;
import dev.planka.domain.schema.definition.condition.TextConditionItem;
import dev.planka.domain.schema.definition.condition.TextConditionItem.TextOperator;
import dev.planka.domain.schema.definition.condition.TextConditionItem.TextSubject;
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
@DisplayName("TextConditionEvaluator 单元测试")
class TextConditionEvaluatorTest {

    @Mock
    private CardValueExtractor valueExtractor;

    private TextConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new TextConditionEvaluator(valueExtractor);
    }

    private CardDTO createCard(long cardId) {
        CardDTO card = new CardDTO();
        card.setId(CardId.of(cardId));
        return card;
    }

    private TextConditionItem createCondition(String fieldId, TextOperator operator) {
        return new TextConditionItem(new TextSubject(null, fieldId), operator);
    }

    @Nested
    @DisplayName("Equal 操作符测试")
    class EqualTests {

        @Test
        @DisplayName("当实际值等于期望值时返回 true")
        void evaluate_returnsTrue_whenActualEqualsExpected() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.Equal("测试标题"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new TextFieldValue("title", "测试标题")).when(valueExtractor).getFieldValue(card, "title");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值不等于期望值时返回 false")
        void evaluate_returnsFalse_whenActualNotEqualsExpected() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.Equal("期望标题"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new TextFieldValue("title", "实际标题")).when(valueExtractor).getFieldValue(card, "title");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当实际值为 null 时返回 false")
        void evaluate_returnsFalse_whenActualIsNull() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.Equal("期望值"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "title");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当期望值和实际值都为 null 时返回 true")
        void evaluate_returnsTrue_whenBothAreNull() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.Equal(null));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "title");

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
            TextConditionItem condition = createCondition("title", new TextOperator.NotEqual("期望值"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new TextFieldValue("title", "实际值")).when(valueExtractor).getFieldValue(card, "title");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值等于期望值时返回 false")
        void evaluate_returnsFalse_whenActualEqualsExpected() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.NotEqual("相同值"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new TextFieldValue("title", "相同值")).when(valueExtractor).getFieldValue(card, "title");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Contains 操作符测试")
    class ContainsTests {

        @Test
        @DisplayName("当实际值包含期望值时返回 true")
        void evaluate_returnsTrue_whenActualContainsExpected() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.Contains("测试"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new TextFieldValue("title", "这是一个测试标题")).when(valueExtractor).getFieldValue(card, "title");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值不包含期望值时返回 false")
        void evaluate_returnsFalse_whenActualNotContainsExpected() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.Contains("测试"));

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new TextFieldValue("title", "这是一个标题")).when(valueExtractor).getFieldValue(card, "title");

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
            TextConditionItem condition = createCondition("title", new TextOperator.IsEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "title");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值为空字符串时返回 true")
        void evaluate_returnsTrue_whenFieldValueIsEmptyString() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.IsEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new TextFieldValue("title", "")).when(valueExtractor).getFieldValue(card, "title");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值不为空时返回 false")
        void evaluate_returnsFalse_whenFieldValueIsNotEmpty() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.IsEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new TextFieldValue("title", "有值")).when(valueExtractor).getFieldValue(card, "title");

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
            TextConditionItem condition = createCondition("title", new TextOperator.IsNotEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(new TextFieldValue("title", "有值")).when(valueExtractor).getFieldValue(card, "title");

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值为 null 时返回 false")
        void evaluate_returnsFalse_whenFieldValueIsNull() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.IsNotEmpty());

            when(valueExtractor.getCardByPath(eq(card), any())).thenReturn(card);
            doReturn(null).when(valueExtractor).getFieldValue(card, "title");

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
            TextConditionItem condition = createCondition("title", new TextOperator.Equal("test"));

            when(valueExtractor.getCardByPath(any(), any())).thenReturn(null);

            boolean result = evaluator.evaluate(condition, null, null);

            assertThat(result).isFalse();
        }
    }
}
