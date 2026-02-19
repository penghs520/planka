package dev.planka.card.converter;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.field.FieldValue;
import dev.planka.domain.field.StructureFieldValue;
import dev.planka.domain.field.StructureItem;
import dev.planka.domain.link.LinkFieldId;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.schema.definition.fieldconfig.StructureFieldConfig;
import dev.planka.domain.schema.definition.structure.StructureLevelBinding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StructureFieldValueBuilder 单元测试")
class StructureFieldValueBuilderTest {

    @Test
    @DisplayName("build - 从源卡片 linkedCards 构建完整的 StructureItem 链")
    void build_createsCompleteChain_fromLinkedCards() {
        // 准备架构属性定义（两层）
        StructureFieldConfig definition = createStructureFieldConfig(
                "struct_001",
                List.of(
                        new StructureLevelBinding(0, LinkFieldId.of("link_001", LinkPosition.SOURCE), true),
                        new StructureLevelBinding(1, LinkFieldId.of("link_002", LinkPosition.SOURCE), false)
                )
        );

        // 准备第一层关联卡片（小队）
        CardDTO squadCard = new CardDTO();
        squadCard.setId(CardId.of(1001L));
        squadCard.setTitle(CardTitle.pure("小队Alpha"));

        // 准备第二层关联卡片（部落）
        CardDTO tribeCard = new CardDTO();
        tribeCard.setId(CardId.of(1002L));
        tribeCard.setTitle(CardTitle.pure("部落A"));

        // 准备源卡片（所有层级的关联都直接挂在源卡片上）
        CardDTO sourceCard = new CardDTO();
        sourceCard.setId(CardId.of(1000L));
        sourceCard.setLinkedCards(Map.of(
                "link_001:SOURCE", Set.of(squadCard),
                "link_002:SOURCE", Set.of(tribeCard)
        ));

        // 执行
        StructureFieldValue result = StructureFieldValueBuilder.build(sourceCard, definition);

        // 验证
        assertThat(result).isNotNull();
        assertThat(result.getFieldId()).isEqualTo("struct_001");
        assertThat(result.isReadable()).isTrue();

        StructureItem firstItem = result.getValue();
        assertThat(firstItem).isNotNull();
        assertThat(firstItem.getId()).isEqualTo("1001");
        assertThat(firstItem.getName()).isEqualTo("小队Alpha");

        StructureItem secondItem = firstItem.getNext();
        assertThat(secondItem).isNotNull();
        assertThat(secondItem.getId()).isEqualTo("1002");
        assertThat(secondItem.getName()).isEqualTo("部落A");
        assertThat(secondItem.getNext()).isNull();
    }

    @Test
    @DisplayName("build - 当关联卡片缺失时返回部分链")
    void build_createsPartialChain_whenLinkMissing() {
        // 准备架构属性定义（两层）
        StructureFieldConfig definition = createStructureFieldConfig(
                "struct_001",
                List.of(
                        new StructureLevelBinding(0, LinkFieldId.of("link_001", LinkPosition.SOURCE), true),
                        new StructureLevelBinding(1, LinkFieldId.of("link_002", LinkPosition.SOURCE), false)
                )
        );

        // 准备第一层关联卡片
        CardDTO squadCard = new CardDTO();
        squadCard.setId(CardId.of(1001L));
        squadCard.setTitle(CardTitle.pure("小队Alpha"));

        // 准备源卡片（只有第一层关联，没有第二层）
        CardDTO sourceCard = new CardDTO();
        sourceCard.setId(CardId.of(1000L));
        sourceCard.setLinkedCards(Map.of("link_001:SOURCE", Set.of(squadCard)));
        // 没有 link_002:SOURCE，第二层缺失

        // 执行
        StructureFieldValue result = StructureFieldValueBuilder.build(sourceCard, definition);

        // 验证 - 应该返回只有第一层的部分链
        assertThat(result).isNotNull();
        assertThat(result.getValue()).isNotNull();
        assertThat(result.getValue().getId()).isEqualTo("1001");
        assertThat(result.getValue().getNext()).isNull(); // 第二层缺失
    }

    @Test
    @DisplayName("build - 当没有任何关联时返回空值")
    void build_returnsEmptyValue_whenNoLinks() {
        StructureFieldConfig definition = createStructureFieldConfig(
                "struct_001",
                List.of(
                        new StructureLevelBinding(0, LinkFieldId.of("link_001", LinkPosition.SOURCE), true)
                )
        );

        CardDTO sourceCard = new CardDTO();
        sourceCard.setId(CardId.of(1000L));
        sourceCard.setLinkedCards(new HashMap<>());

        StructureFieldValue result = StructureFieldValueBuilder.build(sourceCard, definition);

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isNull();
    }

    @Test
    @DisplayName("buildAll - 批量构建多个架构属性值")
    void buildAll_createsMultipleValues() {
        StructureFieldConfig def1 = createStructureFieldConfig(
                "struct_001",
                List.of(new StructureLevelBinding(0, LinkFieldId.of("link_001", LinkPosition.SOURCE), true))
        );
        StructureFieldConfig def2 = createStructureFieldConfig(
                "struct_002",
                List.of(new StructureLevelBinding(0, LinkFieldId.of("link_002", LinkPosition.TARGET), true))
        );

        CardDTO linkedCard1 = new CardDTO();
        linkedCard1.setId(CardId.of(2001L));
        linkedCard1.setTitle(CardTitle.pure("关联卡片1"));

        CardDTO linkedCard2 = new CardDTO();
        linkedCard2.setId(CardId.of(2002L));
        linkedCard2.setTitle(CardTitle.pure("关联卡片2"));

        CardDTO sourceCard = new CardDTO();
        sourceCard.setId(CardId.of(1000L));
        sourceCard.setLinkedCards(Map.of(
                "link_001:SOURCE", Set.of(linkedCard1),
                "link_002:TARGET", Set.of(linkedCard2)
        ));

        Map<String, FieldValue<?>> result = StructureFieldValueBuilder.buildAll(
                sourceCard,
                List.of(def1, def2)
        );

        assertThat(result).hasSize(2);
        assertThat(result).containsKey("struct_001");
        assertThat(result).containsKey("struct_002");
    }

    private StructureFieldConfig createStructureFieldConfig(String id, List<StructureLevelBinding> bindings) {
        StructureFieldConfig def = new StructureFieldConfig(
                FieldConfigId.of(id),
                "org_001",
                "测试架构属性",
                null,
                FieldId.of(id),
                false
        );
        def.setLevelBindings(bindings);
        return def;
    }
}
