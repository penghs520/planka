package cn.agilean.kanban.card.service.evaluator;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.domain.field.EnumFieldValue;
import cn.agilean.kanban.domain.field.FieldValue;
import cn.agilean.kanban.domain.schema.definition.condition.EnumConditionItem;
import cn.agilean.kanban.domain.schema.definition.condition.EnumConditionItem.EnumOperator;
import cn.agilean.kanban.domain.schema.definition.condition.EnumConditionItem.EnumSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        card.setFieldValues(new HashMap<>());
        return card;
    }

    private void setFieldValue(CardDTO card, String fieldId, FieldValue<?> fieldValue) {
        card.getFieldValues().put(fieldId, fieldValue);
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

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of("high")));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当单选值不等于期望值时返回 false")
        void evaluate_returnsFalse_whenSingleValueNotEqualsExpected() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.Equal("high"));

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of("low")));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当多选值时返回 false（Equal 仅适用于单选）")
        void evaluate_returnsFalse_whenMultipleValues() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.Equal("high"));

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of("high", "medium")));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当值为空时返回 false")
        void evaluate_returnsFalse_whenValueIsEmpty() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.Equal("high"));

            // fieldValues 中不包含该字段

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

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of("low")));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当单选值等于期望值时返回 false")
        void evaluate_returnsFalse_whenSingleValueEqualsExpected() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.NotEqual("high"));

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of("high")));

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

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of("high")));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当任意一个实际值在期望列表中时返回 true（多选场景）")
        void evaluate_returnsTrue_whenAnyActualValueInExpectedList() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("tags",
                new EnumOperator.In(List.of("bug", "feature")));

            setFieldValue(card, "tags", new EnumFieldValue("tags", List.of("bug", "urgent")));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值不在期望列表中时返回 false")
        void evaluate_returnsFalse_whenActualValueNotInExpectedList() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority",
                new EnumOperator.In(List.of("high", "medium")));

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of("low")));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当实际值为空时返回 false")
        void evaluate_returnsFalse_whenActualValueIsEmpty() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority",
                new EnumOperator.In(List.of("high", "medium")));

            // fieldValues 中不包含该字段

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当期望列表为空时返回 false")
        void evaluate_returnsFalse_whenExpectedListIsEmpty() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority",
                new EnumOperator.In(List.of()));

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of("high")));

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

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of("low")));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当实际值在期望列表中时返回 false")
        void evaluate_returnsFalse_whenActualValueInExpectedList() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority",
                new EnumOperator.NotIn(List.of("high", "medium")));

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of("high")));

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

            // fieldValues 中不包含该字段

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值为空列表时返回 true")
        void evaluate_returnsTrue_whenFieldValueIsEmptyList() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.IsEmpty());

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of()));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值不为空时返回 false")
        void evaluate_returnsFalse_whenFieldValueIsNotEmpty() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.IsEmpty());

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of("high")));

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

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of("high")));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当字段值为 null 时返回 false")
        void evaluate_returnsFalse_whenFieldValueIsNull() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.IsNotEmpty());

            // fieldValues 中不包含该字段

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当字段值为空列表时返回 false")
        void evaluate_returnsFalse_whenFieldValueIsEmptyList() {
            CardDTO card = createCard(1001L);
            EnumConditionItem condition = createCondition("priority", new EnumOperator.IsNotEmpty());

            setFieldValue(card, "priority", new EnumFieldValue("priority", List.of()));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

}
