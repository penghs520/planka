package cn.agilean.kanban.card.service.evaluator;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.domain.field.FieldValue;
import cn.agilean.kanban.domain.field.WebLinkFieldValue;
import cn.agilean.kanban.domain.schema.definition.condition.WebUrlConditionItem;
import cn.agilean.kanban.domain.schema.definition.condition.WebUrlConditionItem.WebUrlOperator;
import cn.agilean.kanban.domain.schema.definition.condition.WebUrlConditionItem.WebUrlSubject;
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
@DisplayName("WebUrlConditionEvaluator 单元测试")
class WebUrlConditionEvaluatorTest {

    @Mock
    private CardValueExtractor valueExtractor;

    private WebUrlConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new WebUrlConditionEvaluator(valueExtractor);
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

    private WebUrlConditionItem createCondition(String fieldId, WebUrlOperator operator) {
        return new WebUrlConditionItem(new WebUrlSubject(null, fieldId), operator);
    }

    @Nested
    @DisplayName("Equal 操作符测试")
    class EqualTests {

        @Test
        @DisplayName("当 URL 等于期望值时返回 true")
        void evaluate_returnsTrue_whenUrlEqualsExpected() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.Equal("https://example.com"));
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "https://example.com", "Example");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当显示文本等于期望值时返回 true")
        void evaluate_returnsTrue_whenDisplayTextEqualsExpected() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.Equal("Example"));
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "https://example.com", "Example");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当 URL 和显示文本都不匹配时返回 false")
        void evaluate_returnsFalse_whenNeitherUrlNorDisplayTextMatches() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.Equal("https://example.com"));
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "https://other.com", "Other");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("NotEqual 操作符测试")
    class NotEqualTests {

        @Test
        @DisplayName("当 URL 不等于期望值时返回 true")
        void evaluate_returnsTrue_whenUrlNotEqualsExpected() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.NotEqual("https://example.com"));
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "https://other.com", "Other");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当 URL 等于期望值时返回 false")
        void evaluate_returnsFalse_whenUrlEqualsExpected() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.NotEqual("https://example.com"));
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "https://example.com", "Example");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Contains 操作符测试")
    class ContainsTests {

        @Test
        @DisplayName("当 URL 包含指定值时返回 true")
        void evaluate_returnsTrue_whenUrlContainsValue() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.Contains("example"));
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "https://example.com", "Example");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当显示文本包含指定值时返回 true")
        void evaluate_returnsTrue_whenDisplayTextContainsValue() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.Contains("Example"));
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "https://other.com", "Example Site");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当 URL 和显示文本都不包含指定值时返回 false")
        void evaluate_returnsFalse_whenNeitherContainsValue() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.Contains("nonexistent"));
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "https://example.com", "Example");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("NotContains 操作符测试")
    class NotContainsTests {

        @Test
        @DisplayName("当 URL 不包含指定值时返回 true")
        void evaluate_returnsTrue_whenUrlNotContainsValue() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.NotContains("other"));
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "https://example.com", "Example");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当 URL 包含指定值时返回 false")
        void evaluate_returnsFalse_whenUrlContainsValue() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.NotContains("example"));
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "https://example.com", "Example");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("IsEmpty 操作符测试")
    class IsEmptyTests {

        @Test
        @DisplayName("当 URL 字段为空时返回 true")
        void evaluate_returnsTrue_whenUrlIsEmpty() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.IsEmpty());

            // fieldValues 中不包含该字段

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当 URL 字段不为空时返回 false")
        void evaluate_returnsFalse_whenUrlIsNotEmpty() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.IsEmpty());
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "https://example.com", "Example");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("IsNotEmpty 操作符测试")
    class IsNotEmptyTests {

        @Test
        @DisplayName("当 URL 不为空时返回 true")
        void evaluate_returnsTrue_whenUrlIsNotEmpty() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.IsNotEmpty());
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "https://example.com", "");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当显示文本不为空时返回 true")
        void evaluate_returnsTrue_whenDisplayTextIsNotEmpty() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.IsNotEmpty());
            WebLinkFieldValue fieldValue = new WebLinkFieldValue("linkField", "", "Example");

            setFieldValue(card, "linkField", fieldValue);

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当 URL 和显示文本都为空时返回 false")
        void evaluate_returnsFalse_whenBothAreEmpty() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.IsNotEmpty());

            // fieldValues 中不包含该字段

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("字段类型测试")
    class FieldTypeTests {

        @Test
        @DisplayName("当字段类型不是 WebLinkFieldValue 时返回 false")
        void evaluate_returnsFalse_whenFieldTypeMismatch() {
            CardDTO card = createCard(1001L);
            WebUrlConditionItem condition = createCondition("linkField", new WebUrlOperator.Equal("https://example.com"));

            // fieldValues 中不包含该字段

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }
}
