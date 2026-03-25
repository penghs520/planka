package cn.planka.domain.schema.template.detail;

import cn.planka.domain.schema.CardDetailTemplateId;
import cn.planka.domain.schema.definition.template.CardDetailTemplateDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DefaultCardDetailTemplateBuilder}、{@link CardDetailTemplateEffectiveHelper} 行为测试
 */
@DisplayName("默认详情页模板构建与选取")
class DefaultCardDetailTemplateBuilderTest {

    @Test
    @DisplayName("空字段列表时生成内存默认模板")
    void build_emptyFieldConfigs() {
        CardDetailTemplateDefinition t = DefaultCardDetailTemplateBuilder.build(Collections.emptyList());
        assertThat(t.getId().value()).isEqualTo("default_template");
        assertThat(t.isDefault()).isTrue();
        assertThat(t.isSystemTemplate()).isTrue();
        assertThat(t.getTabs()).hasSize(3);
        assertThat(t.getTabs().get(0).getTabId()).isEqualTo("basic_info");
        assertThat(t.getTabs().get(0).getSections()).isNotEmpty();
    }

    @Test
    @DisplayName("selectFromList 优先 isDefault 为 true 的模板")
    void selectFromList_prefersDefault() {
        CardDetailTemplateDefinition first = new CardDetailTemplateDefinition(
                CardDetailTemplateId.of("t_first"), "org", "first");
        first.setDefault(false);
        CardDetailTemplateDefinition def = new CardDetailTemplateDefinition(
                CardDetailTemplateId.of("t_def"), "org", "def");
        def.setDefault(true);

        List<CardDetailTemplateDefinition> list = List.of(first, def);
        Optional<CardDetailTemplateDefinition> selected = CardDetailTemplateEffectiveHelper.selectFromList(list);
        assertThat(selected).contains(def);
    }

    @Test
    @DisplayName("selectFromList 无默认时取第一个")
    void selectFromList_fallsBackToFirst() {
        CardDetailTemplateDefinition a = new CardDetailTemplateDefinition(
                CardDetailTemplateId.of("t_a"), "org", "a");
        a.setDefault(false);
        CardDetailTemplateDefinition b = new CardDetailTemplateDefinition(
                CardDetailTemplateId.of("t_b"), "org", "b");
        b.setDefault(false);

        Optional<CardDetailTemplateDefinition> selected = CardDetailTemplateEffectiveHelper.selectFromList(List.of(a, b));
        assertThat(selected).contains(a);
    }

    @Test
    @DisplayName("selectFromList 空列表返回 empty")
    void selectFromList_empty() {
        assertThat(CardDetailTemplateEffectiveHelper.selectFromList(Collections.emptyList())).isEmpty();
        assertThat(CardDetailTemplateEffectiveHelper.selectFromList(null)).isEmpty();
    }
}
