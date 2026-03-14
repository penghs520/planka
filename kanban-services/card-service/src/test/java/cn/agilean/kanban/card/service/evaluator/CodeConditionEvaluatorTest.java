package cn.agilean.kanban.card.service.evaluator;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.domain.schema.definition.condition.CodeConditionItem;
import cn.agilean.kanban.domain.schema.definition.condition.CodeConditionItem.CodeOperator;
import cn.agilean.kanban.domain.schema.definition.condition.CodeConditionItem.CodeSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("CodeConditionEvaluator 单元测试")
class CodeConditionEvaluatorTest {

    @Mock
    private CardValueExtractor valueExtractor;

    private CodeConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CodeConditionEvaluator(valueExtractor);
    }

    private CardDTO createCard(long cardId, Long codeInOrg, String customCode) {
        CardDTO card = new CardDTO();
        card.setId(CardId.of(cardId));
        card.setCodeInOrg(codeInOrg);
        card.setCustomCode(customCode);
        return card;
    }

    private CodeConditionItem createCondition(CodeOperator operator) {
        return new CodeConditionItem(new CodeSubject(null), operator);
    }

    @Nested
    @DisplayName("Equal 操作符测试")
    class EqualTests {

        @Test
        @DisplayName("当自定义编号等于期望值时返回 true")
        void evaluate_returnsTrue_whenCustomCodeEqualsExpected() {
            CardDTO card = createCard(1001L, 1001L, "CUSTOM-001");
            CodeConditionItem condition = createCondition(new CodeOperator.Equal("CUSTOM-001"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当内置编号等于期望值时返回 true")
        void evaluate_returnsTrue_whenBuiltInCodeEqualsExpected() {
            CardDTO card = createCard(1001L, 1001L, null);
            CodeConditionItem condition = createCondition(new CodeOperator.Equal("1001"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当编号不等于期望值时返回 false")
        void evaluate_returnsFalse_whenCodeNotEqualsExpected() {
            CardDTO card = createCard(1001L, 1001L, "CUSTOM-001");
            CodeConditionItem condition = createCondition(new CodeOperator.Equal("CUSTOM-002"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("NotEqual 操作符测试")
    class NotEqualTests {

        @Test
        @DisplayName("当编号不等于期望值时返回 true")
        void evaluate_returnsTrue_whenCodeNotEqualsExpected() {
            CardDTO card = createCard(1001L, 1001L, "CUSTOM-001");
            CodeConditionItem condition = createCondition(new CodeOperator.NotEqual("CUSTOM-002"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当编号等于期望值时返回 false")
        void evaluate_returnsFalse_whenCodeEqualsExpected() {
            CardDTO card = createCard(1001L, 1001L, "CUSTOM-001");
            CodeConditionItem condition = createCondition(new CodeOperator.NotEqual("CUSTOM-001"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Contains 操作符测试")
    class ContainsTests {

        @Test
        @DisplayName("当编号包含指定值时返回 true")
        void evaluate_returnsTrue_whenCodeContainsValue() {
            CardDTO card = createCard(1001L, 1001L, "CUSTOM-001");
            CodeConditionItem condition = createCondition(new CodeOperator.Contains("001"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当编号不包含指定值时返回 false")
        void evaluate_returnsFalse_whenCodeNotContainsValue() {
            CardDTO card = createCard(1001L, 1001L, "CUSTOM-001");
            CodeConditionItem condition = createCondition(new CodeOperator.Contains("999"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("NotContains 操作符测试")
    class NotContainsTests {

        @Test
        @DisplayName("当编号不包含指定值时返回 true")
        void evaluate_returnsTrue_whenCodeNotContainsValue() {
            CardDTO card = createCard(1001L, 1001L, "CUSTOM-001");
            CodeConditionItem condition = createCondition(new CodeOperator.NotContains("999"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当编号包含指定值时返回 false")
        void evaluate_returnsFalse_whenCodeContainsValue() {
            CardDTO card = createCard(1001L, 1001L, "CUSTOM-001");
            CodeConditionItem condition = createCondition(new CodeOperator.NotContains("001"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("路径解析测试")
    class PathTests {

        @Test
        @DisplayName("当路径为空时直接评估当前卡片")
        void evaluate_returnsTrue_whenPathIsEmpty() {
            CardDTO card = createCard(1001L, 1001L, "CUSTOM-001");
            CodeConditionItem condition = createCondition(new CodeOperator.Equal("CUSTOM-001"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }
    }
}
