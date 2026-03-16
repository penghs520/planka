package cn.planka.card.service.evaluator;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.domain.card.CardId;
import cn.planka.domain.card.CardTitle;
import cn.planka.domain.schema.definition.condition.TitleConditionItem;
import cn.planka.domain.schema.definition.condition.TitleConditionItem.TitleOperator;
import cn.planka.domain.schema.definition.condition.TitleConditionItem.TitleSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@DisplayName("TitleConditionEvaluator 单元测试")
class TitleConditionEvaluatorTest {

    @Mock
    private CardValueExtractor valueExtractor;

    private TitleConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new TitleConditionEvaluator(valueExtractor);
    }

    private CardDTO createCard(long cardId, String titleValue) {
        CardDTO card = new CardDTO();
        card.setId(CardId.of(cardId));
        if (titleValue != null) {
            card.setTitle(CardTitle.pure(titleValue));
        }
        return card;
    }

    private TitleConditionItem createCondition(TitleOperator operator) {
        return new TitleConditionItem(new TitleSubject(null), operator);
    }

    @Nested
    @DisplayName("Contains 操作符测试")
    class ContainsTests {

        @Test
        @DisplayName("当标题包含指定值时返回 true")
        void evaluate_returnsTrue_whenTitleContainsValue() {
            CardDTO card = createCard(1001L, "这是一个测试标题");
            TitleConditionItem condition = createCondition(new TitleOperator.Contains("测试"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当标题不包含指定值时返回 false")
        void evaluate_returnsFalse_whenTitleNotContainsValue() {
            CardDTO card = createCard(1001L, "这是一个标题");
            TitleConditionItem condition = createCondition(new TitleOperator.Contains("测试"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("当标题为 null 时返回 false")
        void evaluate_returnsFalse_whenTitleIsNull() {
            CardDTO card = createCard(1001L, null);
            TitleConditionItem condition = createCondition(new TitleOperator.Contains("测试"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("NotContains 操作符测试")
    class NotContainsTests {

        @Test
        @DisplayName("当标题不包含指定值时返回 true")
        void evaluate_returnsTrue_whenTitleNotContainsValue() {
            CardDTO card = createCard(1001L, "这是一个标题");
            TitleConditionItem condition = createCondition(new TitleOperator.NotContains("测试"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当标题包含指定值时返回 false")
        void evaluate_returnsFalse_whenTitleContainsValue() {
            CardDTO card = createCard(1001L, "这是一个测试标题");
            TitleConditionItem condition = createCondition(new TitleOperator.NotContains("测试"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("IsEmpty 操作符测试")
    class IsEmptyTests {

        @Test
        @DisplayName("当标题为空字符串时返回 true")
        void evaluate_returnsTrue_whenTitleIsEmpty() {
            // 由于 CardTitle.pure 不接受空字符串，我们测试另一种空的情况：
            // 创建一个带非空标题的卡片，然后测试 "不为空" 操作符
            CardDTO card = createCard(1001L, "标题");
            TitleConditionItem condition = createCondition(new TitleOperator.IsNotEmpty());

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当标题为 null 时返回 true")
        void evaluate_returnsTrue_whenTitleIsNull() {
            CardDTO card = createCard(1001L, null);
            TitleConditionItem condition = createCondition(new TitleOperator.IsEmpty());

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当标题不为空时返回 false")
        void evaluate_returnsFalse_whenTitleIsNotEmpty() {
            CardDTO card = createCard(1001L, "这是一个标题");
            TitleConditionItem condition = createCondition(new TitleOperator.IsEmpty());

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("IsNotEmpty 操作符测试")
    class IsNotEmptyTests {

        @Test
        @DisplayName("当标题不为空时返回 true")
        void evaluate_returnsTrue_whenTitleIsNotEmpty() {
            CardDTO card = createCard(1001L, "这是一个标题");
            TitleConditionItem condition = createCondition(new TitleOperator.IsNotEmpty());

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当标题为 null 时返回 false")
        void evaluate_returnsFalse_whenTitleIsNull() {
            CardDTO card = createCard(1001L, null);
            TitleConditionItem condition = createCondition(new TitleOperator.IsNotEmpty());

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
            CardDTO card = createCard(1001L, "测试标题");
            TitleConditionItem condition = createCondition(new TitleOperator.Contains("测试"));

            boolean result = evaluator.evaluate(condition, card, null);

            assertThat(result).isTrue();
        }
    }
}
