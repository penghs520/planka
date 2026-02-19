package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.card.CardId;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.condition.ConditionGroup;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConditionEvaluator 单元测试")
class ConditionEvaluatorTest {

    @Mock
    private TextConditionEvaluator textEvaluator;
    @Mock
    private NumberConditionEvaluator numberEvaluator;
    @Mock
    private DateConditionEvaluator dateEvaluator;
    @Mock
    private EnumConditionEvaluator enumEvaluator;
    @Mock
    private StatusConditionEvaluator statusEvaluator;
    @Mock
    private CardCycleConditionEvaluator cardCycleEvaluator;
    @Mock
    private SystemUserConditionEvaluator systemUserEvaluator;
    @Mock
    private LinkConditionEvaluator linkEvaluator;

    private ConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ConditionEvaluator(
            textEvaluator,
            numberEvaluator,
            dateEvaluator,
            enumEvaluator,
            statusEvaluator,
            cardCycleEvaluator,
            systemUserEvaluator,
            linkEvaluator
        );
    }

    private CardDTO createCard(long cardId) {
        CardDTO card = new CardDTO();
        card.setId(CardId.of(cardId));
        return card;
    }

    private TextConditionItem createTextCondition(String fieldId, String expectedValue) {
        return new TextConditionItem(
            new TextSubject(null, fieldId),
            new TextOperator.Equal(expectedValue)
        );
    }

    @Nested
    @DisplayName("空条件测试")
    class EmptyConditionTests {

        @Test
        @DisplayName("当条件为 null 时返回 true")
        void evaluate_returnsTrue_whenConditionIsNull() {
            CardDTO card = createCard(1001L);

            boolean result = evaluator.evaluate(null, card);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当条件根节点为 null 时返回 true")
        void evaluate_returnsTrue_whenRootIsNull() {
            CardDTO card = createCard(1001L);
            Condition condition = new Condition(null);

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当条件组为空时返回 true")
        void evaluate_returnsTrue_whenGroupIsEmpty() {
            CardDTO card = createCard(1001L);
            Condition condition = Condition.of(ConditionGroup.and());

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("单条件项测试")
    class SingleConditionTests {

        @Test
        @DisplayName("当文本条件满足时返回 true")
        void evaluate_returnsTrue_whenTextConditionMet() {
            CardDTO card = createCard(1001L);
            TextConditionItem textItem = createTextCondition("title", "测试");
            Condition condition = Condition.of(textItem);

            when(textEvaluator.evaluate(eq(textItem), eq(card), any())).thenReturn(true);

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当文本条件不满足时返回 false")
        void evaluate_returnsFalse_whenTextConditionNotMet() {
            CardDTO card = createCard(1001L);
            TextConditionItem textItem = createTextCondition("title", "测试");
            Condition condition = Condition.of(textItem);

            when(textEvaluator.evaluate(eq(textItem), eq(card), any())).thenReturn(false);

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("AND 组合条件测试")
    class AndConditionTests {

        @Test
        @DisplayName("当所有条件都满足时返回 true")
        void evaluate_returnsTrue_whenAllConditionsMet() {
            CardDTO card = createCard(1001L);
            TextConditionItem cond1 = createTextCondition("title", "测试1");
            TextConditionItem cond2 = createTextCondition("desc", "测试2");
            Condition condition = Condition.and(cond1, cond2);

            when(textEvaluator.evaluate(eq(cond1), eq(card), any())).thenReturn(true);
            when(textEvaluator.evaluate(eq(cond2), eq(card), any())).thenReturn(true);

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当任一条件不满足时返回 false")
        void evaluate_returnsFalse_whenAnyConditionNotMet() {
            CardDTO card = createCard(1001L);
            TextConditionItem cond1 = createTextCondition("title", "测试1");
            TextConditionItem cond2 = createTextCondition("desc", "测试2");
            Condition condition = Condition.and(cond1, cond2);

            when(textEvaluator.evaluate(eq(cond1), eq(card), any())).thenReturn(true);
            when(textEvaluator.evaluate(eq(cond2), eq(card), any())).thenReturn(false);

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当第一个条件不满足时短路返回 false")
        void evaluate_shortCircuits_whenFirstConditionNotMet() {
            CardDTO card = createCard(1001L);
            TextConditionItem cond1 = createTextCondition("title", "测试1");
            TextConditionItem cond2 = createTextCondition("desc", "测试2");
            Condition condition = Condition.and(cond1, cond2);

            when(textEvaluator.evaluate(eq(cond1), eq(card), any())).thenReturn(false);
            // cond2 不应该被调用（短路）

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("OR 组合条件测试")
    class OrConditionTests {

        @Test
        @DisplayName("当任一条件满足时返回 true")
        void evaluate_returnsTrue_whenAnyConditionMet() {
            CardDTO card = createCard(1001L);
            TextConditionItem cond1 = createTextCondition("title", "测试1");
            TextConditionItem cond2 = createTextCondition("desc", "测试2");
            Condition condition = Condition.or(cond1, cond2);

            when(textEvaluator.evaluate(eq(cond1), eq(card), any())).thenReturn(false);
            when(textEvaluator.evaluate(eq(cond2), eq(card), any())).thenReturn(true);

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当所有条件都不满足时返回 false")
        void evaluate_returnsFalse_whenNoConditionMet() {
            CardDTO card = createCard(1001L);
            TextConditionItem cond1 = createTextCondition("title", "测试1");
            TextConditionItem cond2 = createTextCondition("desc", "测试2");
            Condition condition = Condition.or(cond1, cond2);

            when(textEvaluator.evaluate(eq(cond1), eq(card), any())).thenReturn(false);
            when(textEvaluator.evaluate(eq(cond2), eq(card), any())).thenReturn(false);

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当第一个条件满足时短路返回 true")
        void evaluate_shortCircuits_whenFirstConditionMet() {
            CardDTO card = createCard(1001L);
            TextConditionItem cond1 = createTextCondition("title", "测试1");
            TextConditionItem cond2 = createTextCondition("desc", "测试2");
            Condition condition = Condition.or(cond1, cond2);

            when(textEvaluator.evaluate(eq(cond1), eq(card), any())).thenReturn(true);
            // cond2 不应该被调用（短路）

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("嵌套条件测试")
    class NestedConditionTests {

        @Test
        @DisplayName("嵌套 AND-OR 条件：(A AND (B OR C)) - B满足时返回 true")
        void evaluate_nestedAndOr_returnsTrue_whenBMet() {
            CardDTO card = createCard(1001L);
            TextConditionItem condA = createTextCondition("a", "a");
            TextConditionItem condB = createTextCondition("b", "b");
            TextConditionItem condC = createTextCondition("c", "c");

            ConditionGroup orGroup = ConditionGroup.or(condB, condC);
            Condition condition = Condition.and(condA, orGroup);

            when(textEvaluator.evaluate(eq(condA), eq(card), any())).thenReturn(true);
            when(textEvaluator.evaluate(eq(condB), eq(card), any())).thenReturn(true);
            // condC 不需要评估（短路）

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("嵌套 AND-OR 条件：(A AND (B OR C)) - A不满足时返回 false")
        void evaluate_nestedAndOr_returnsFalse_whenANotMet() {
            CardDTO card = createCard(1001L);
            TextConditionItem condA = createTextCondition("a", "a");
            TextConditionItem condB = createTextCondition("b", "b");
            TextConditionItem condC = createTextCondition("c", "c");

            ConditionGroup orGroup = ConditionGroup.or(condB, condC);
            Condition condition = Condition.and(condA, orGroup);

            when(textEvaluator.evaluate(eq(condA), eq(card), any())).thenReturn(false);
            // orGroup 不需要评估（短路）

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("嵌套 OR-AND 条件：(A OR (B AND C)) - BC都满足时返回 true")
        void evaluate_nestedOrAnd_returnsTrue_whenBAndCMet() {
            CardDTO card = createCard(1001L);
            TextConditionItem condA = createTextCondition("a", "a");
            TextConditionItem condB = createTextCondition("b", "b");
            TextConditionItem condC = createTextCondition("c", "c");

            ConditionGroup andGroup = ConditionGroup.and(condB, condC);
            Condition condition = Condition.or(condA, andGroup);

            when(textEvaluator.evaluate(eq(condA), eq(card), any())).thenReturn(false);
            when(textEvaluator.evaluate(eq(condB), eq(card), any())).thenReturn(true);
            when(textEvaluator.evaluate(eq(condC), eq(card), any())).thenReturn(true);

            boolean result = evaluator.evaluate(condition, card);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("双卡片模式测试（目标卡片和成员卡片）")
    class TwoCardModeTests {

        @Test
        @DisplayName("当条件为 null 时返回 true")
        void evaluate_returnsTrue_whenConditionIsNull() {
            CardDTO targetCard = createCard(1001L);
            CardDTO memberCard = createCard(2001L);

            boolean result = evaluator.evaluate(null, targetCard, memberCard);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当文本条件满足时返回 true")
        void evaluate_returnsTrue_whenConditionMet() {
            CardDTO targetCard = createCard(1001L);
            CardDTO memberCard = createCard(2001L);
            TextConditionItem textItem = createTextCondition("title", "测试");
            Condition condition = Condition.of(textItem);

            when(textEvaluator.evaluate(eq(textItem), eq(targetCard), eq(memberCard))).thenReturn(true);

            boolean result = evaluator.evaluate(condition, targetCard, memberCard);

            assertThat(result).isTrue();
        }
    }
}
