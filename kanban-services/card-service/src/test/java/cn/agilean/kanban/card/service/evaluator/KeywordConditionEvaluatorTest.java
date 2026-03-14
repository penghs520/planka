package cn.agilean.kanban.card.service.evaluator;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.domain.card.CardTitle;
import cn.agilean.kanban.domain.schema.definition.condition.KeywordConditionItem;
import cn.agilean.kanban.domain.schema.definition.condition.KeywordConditionItem.KeywordOperator;
import cn.agilean.kanban.domain.schema.definition.condition.KeywordConditionItem.KeywordSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordConditionEvaluator 单元测试")
class KeywordConditionEvaluatorTest {

    @Mock
    private CardValueExtractor valueExtractor;

    private KeywordConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new KeywordConditionEvaluator(valueExtractor);
    }

    private CardDTO createCard(long cardId, String title, Long codeInOrg, String customCode) {
        CardDTO card = new CardDTO();
        card.setId(CardId.of(cardId));
        if (title != null) {
            card.setTitle(CardTitle.pure(title));
        }
        card.setCodeInOrg(codeInOrg);
        card.setCustomCode(customCode);
        return card;
    }

    private KeywordConditionItem createCondition(KeywordOperator operator) {
        return new KeywordConditionItem(new KeywordSubject(), operator);
    }

    @Nested
    @DisplayName("Contains 操作符测试 - 标题匹配")
    class ContainsInTitleTests {

        @Test
        @DisplayName("当标题包含关键字时返回 true")
        void evaluate_returnsTrue_whenTitleContainsKeyword() {
            CardDTO card = createCard(1001L, "这是一个测试标题", 1001L, null);
            KeywordConditionItem condition = createCondition(new KeywordOperator.Contains("测试"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当标题不包含关键字时返回 false")
        void evaluate_returnsFalse_whenTitleNotContainsKeyword() {
            CardDTO card = createCard(1001L, "这是一个标题", 1001L, null);
            KeywordConditionItem condition = createCondition(new KeywordOperator.Contains("测试"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("关键字匹配忽略大小写")
        void evaluate_returnsTrue_whenTitleContainsKeywordIgnoreCase() {
            CardDTO card = createCard(1001L, "This is a TEST Title", 1001L, null);
            KeywordConditionItem condition = createCondition(new KeywordOperator.Contains("test"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Contains 操作符测试 - 编号匹配")
    class ContainsInCodeTests {

        @Test
        @DisplayName("当内置编号包含关键字时返回 true")
        void evaluate_returnsTrue_whenBuiltInCodeContainsKeyword() {
            CardDTO card = createCard(1001L, "标题", 12345L, null);
            KeywordConditionItem condition = createCondition(new KeywordOperator.Contains("234"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当自定义编号包含关键字时返回 true")
        void evaluate_returnsTrue_whenCustomCodeContainsKeyword() {
            CardDTO card = createCard(1001L, "标题", 1001L, "PROJ-2024-001");
            KeywordConditionItem condition = createCondition(new KeywordOperator.Contains("2024"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("关键字匹配编号忽略大小写")
        void evaluate_returnsTrue_whenCodeContainsKeywordIgnoreCase() {
            CardDTO card = createCard(1001L, "标题", 1001L, "PROJ-001");
            KeywordConditionItem condition = createCondition(new KeywordOperator.Contains("proj"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Contains 操作符测试 - 无匹配")
    class ContainsNoMatchTests {

        @Test
        @DisplayName("当标题和编号都不包含关键字时返回 false")
        void evaluate_returnsFalse_whenNeitherTitleNorCodeContainsKeyword() {
            CardDTO card = createCard(1001L, "标题", 1001L, "CODE-001");
            KeywordConditionItem condition = createCondition(new KeywordOperator.Contains("不存在的关键字"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("NotContains 操作符测试")
    class NotContainsTests {

        @Test
        @DisplayName("当标题和编号都不包含关键字时返回 true")
        void evaluate_returnsTrue_whenNeitherTitleNorCodeContainsKeyword() {
            CardDTO card = createCard(1001L, "标题", 1001L, "CODE-001");
            KeywordConditionItem condition = createCondition(new KeywordOperator.NotContains("测试"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当标题包含关键字时返回 false")
        void evaluate_returnsFalse_whenTitleContainsKeyword() {
            CardDTO card = createCard(1001L, "这是一个测试标题", 1001L, null);
            KeywordConditionItem condition = createCondition(new KeywordOperator.NotContains("测试"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当编号包含关键字时返回 false")
        void evaluate_returnsFalse_whenCodeContainsKeyword() {
            CardDTO card = createCard(1001L, "标题", 1001L, "TEST-001");
            KeywordConditionItem condition = createCondition(new KeywordOperator.NotContains("TEST"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("当卡片标题为 null 时检查编号")
        void evaluate_checksCode_whenTitleIsNull() {
            CardDTO card = createCard(1001L, null, 1001L, "TEST-001");
            KeywordConditionItem condition = createCondition(new KeywordOperator.Contains("TEST"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当卡片标题为 null 且编号不包含关键字时返回 false")
        void evaluate_returnsFalse_whenTitleIsNullAndCodeNotContainsKeyword() {
            CardDTO card = createCard(1001L, null, 1001L, "CODE-001");
            KeywordConditionItem condition = createCondition(new KeywordOperator.Contains("测试"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }
}
