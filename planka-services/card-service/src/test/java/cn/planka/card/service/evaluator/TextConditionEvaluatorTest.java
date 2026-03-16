package cn.planka.card.service.evaluator;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.domain.card.CardId;
import cn.planka.domain.field.FieldValue;
import cn.planka.domain.field.TextFieldValue;
import cn.planka.domain.schema.definition.condition.TextConditionItem;
import cn.planka.domain.schema.definition.condition.TextConditionItem.TextOperator;
import cn.planka.domain.schema.definition.condition.TextConditionItem.TextSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

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
        card.setFieldValues(new HashMap<>());
        return card;
    }

    private void setFieldValue(CardDTO card, String fieldId, FieldValue<?> fieldValue) {
        card.getFieldValues().put(fieldId, fieldValue);
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

            setFieldValue(card, "title", new TextFieldValue("title", "测试标题"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值不等于期望值时返回 false")
        void evaluate_returnsFalse_whenActualNotEqualsExpected() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.Equal("期望标题"));

            setFieldValue(card, "title", new TextFieldValue("title", "实际标题"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当实际值为 null 时返回 false")
        void evaluate_returnsFalse_whenActualIsNull() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.Equal("期望值"));

            // fieldValues 中不包含该字段

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当期望值和实际值都为 null 时返回 true")
        void evaluate_returnsTrue_whenBothAreNull() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.Equal(null));

            // fieldValues 中不包含该字段

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

            setFieldValue(card, "title", new TextFieldValue("title", "实际值"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值等于期望值时返回 false")
        void evaluate_returnsFalse_whenActualEqualsExpected() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.NotEqual("相同值"));

            setFieldValue(card, "title", new TextFieldValue("title", "相同值"));

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

            setFieldValue(card, "title", new TextFieldValue("title", "这是一个测试标题"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值不包含期望值时返回 false")
        void evaluate_returnsFalse_whenActualNotContainsExpected() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.Contains("测试"));

            setFieldValue(card, "title", new TextFieldValue("title", "这是一个标题"));

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

            // fieldValues 中不包含该字段

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值为空字符串时返回 true")
        void evaluate_returnsTrue_whenFieldValueIsEmptyString() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.IsEmpty());

            setFieldValue(card, "title", new TextFieldValue("title", ""));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值不为空时返回 false")
        void evaluate_returnsFalse_whenFieldValueIsNotEmpty() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.IsEmpty());

            setFieldValue(card, "title", new TextFieldValue("title", "有值"));

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

            setFieldValue(card, "title", new TextFieldValue("title", "有值"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值为 null 时返回 false")
        void evaluate_returnsFalse_whenFieldValueIsNull() {
            CardDTO card = createCard(1001L);
            TextConditionItem condition = createCondition("title", new TextOperator.IsNotEmpty());

            // fieldValues 中不包含该字段

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

}
