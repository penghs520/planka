package dev.planka.card.service;

import dev.planka.api.card.request.Yield;
import dev.planka.api.card.request.YieldField;
import dev.planka.api.card.request.YieldLink;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.link.LinkFieldId;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.schema.definition.fieldconfig.StructureFieldConfig;
import dev.planka.domain.schema.definition.structure.StructureLevelBinding;
import dev.planka.infra.cache.schema.SchemaCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("YieldEnhancer 单元测试")
class YieldEnhancerTest {

    @Mock
    private SchemaCacheService schemaCacheService;

    private YieldEnhancer yieldEnhancer;

    @BeforeEach
    void setUp() {
        yieldEnhancer = new YieldEnhancer(schemaCacheService);
    }

    @Test
    @DisplayName("enhance - 当 Yield 为 null 时返回 null")
    void enhance_returnsNull_whenYieldIsNull() {
        Yield result = yieldEnhancer.enhance(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("enhance - 当 YieldField 为 null 时不添加额外的 YieldLink")
    void enhance_noAdditionalLinks_whenFieldIsNull() {
        Yield yield = new Yield();
        yield.setField(null);

        Yield result = yieldEnhancer.enhance(yield);

        assertThat(result).isNotNull();
        assertThat(result.getLinks()).isNull();
    }

    @Test
    @DisplayName("enhance - 当 allFields=true 时不添加架构属性的 YieldLink")
    void enhance_noAdditionalLinks_whenAllFieldsIsTrue() {
        Yield yield = new Yield();
        YieldField field = new YieldField();
        field.setAllFields(true);
        yield.setField(field);

        Yield result = yieldEnhancer.enhance(yield);

        assertThat(result).isNotNull();
        assertThat(result.getLinks()).isNull();
    }

    @Test
    @DisplayName("enhance - 当 fieldIds 包含架构属性时补充平铺的 YieldLink 列表")
    void enhance_addsFlatYieldLinks_whenFieldIdsContainStructureField() {
        // 准备架构属性定义（两层）
        StructureFieldConfig structureDef = createStructureFieldConfig(
                "struct_001",
                List.of(
                        new StructureLevelBinding(0, LinkFieldId.of("link_001", LinkPosition.SOURCE), true),
                        new StructureLevelBinding(1, LinkFieldId.of("link_002", LinkPosition.SOURCE), false)
                )
        );

        when(schemaCacheService.getByIds(anySet()))
                .thenReturn(Map.of("struct_001", structureDef));

        // 准备 Yield
        Yield yield = new Yield();
        YieldField field = new YieldField();
        field.setFieldIds(Set.of("struct_001"));
        yield.setField(field);

        // 执行
        Yield result = yieldEnhancer.enhance(yield);

        // 验证 - 应该是平铺的两个 YieldLink，不是嵌套的链式结构
        assertThat(result).isNotNull();
        assertThat(result.getLinks()).isNotNull().hasSize(2);

        // 验证两个 YieldLink 都是平铺的，没有嵌套
        Set<String> linkFieldIds = new java.util.HashSet<>();
        for (YieldLink link : result.getLinks()) {
            linkFieldIds.add(link.getLinkFieldId());
            assertThat(link.getTargetYield()).isNull(); // 平铺结构没有嵌套
        }
        assertThat(linkFieldIds).containsExactlyInAnyOrder("link_001:SOURCE", "link_002:SOURCE");
    }

    @Test
    @DisplayName("enhance - 已存在的 YieldLink 保留（包括其 targetYield），不被覆盖")
    void enhance_mergesLinks_preservesExistingWithTargetYield() {
        // 准备架构属性定义
        StructureFieldConfig structureDef = createStructureFieldConfig(
                "struct_001",
                List.of(
                        new StructureLevelBinding(0, LinkFieldId.of("link_001", LinkPosition.SOURCE), true)
                )
        );

        when(schemaCacheService.getByIds(anySet()))
                .thenReturn(Map.of("struct_001", structureDef));

        // 准备 Yield（已经有一个相同的 YieldLink，但带有 targetYield）
        Yield yield = new Yield();
        YieldField field = new YieldField();
        field.setFieldIds(Set.of("struct_001"));
        yield.setField(field);

        // 已存在的 YieldLink 带有 targetYield
        YieldLink existingLink = new YieldLink();
        existingLink.setLinkFieldId("link_001:SOURCE");
        Yield targetYield = new Yield();
        YieldField targetField = new YieldField();
        targetField.setFieldIds(Set.of("title", "status"));
        targetYield.setField(targetField);
        existingLink.setTargetYield(targetYield);

        yield.setLinks(List.of(existingLink));

        // 执行
        Yield result = yieldEnhancer.enhance(yield);

        // 验证 - 应该只有一个 link，且是原来的（带有 targetYield）
        assertThat(result.getLinks()).hasSize(1);
        YieldLink resultLink = result.getLinks().get(0);
        assertThat(resultLink.getLinkFieldId()).isEqualTo("link_001:SOURCE");
        // 关键：保留了原有的 targetYield，而不是被架构属性生成的空 targetYield 覆盖
        assertThat(resultLink.getTargetYield()).isNotNull();
        assertThat(resultLink.getTargetYield().getField()).isNotNull();
        assertThat(resultLink.getTargetYield().getField().getFieldIds())
                .containsExactlyInAnyOrder("title", "status");
    }

    @Test
    @DisplayName("extractStructureFieldDefs - 递归收集所有层级的架构属性定义")
    void extractStructureFieldDefs_collectsFromAllLevels() {
        // 准备架构属性定义
        StructureFieldConfig structureDef = createStructureFieldConfig("struct_001", List.of());

        when(schemaCacheService.getByIds(anySet()))
                .thenReturn(Map.of("struct_001", structureDef));

        // 准备嵌套的 Yield
        Yield yield = new Yield();
        YieldField field = new YieldField();
        field.setFieldIds(Set.of("struct_001"));
        yield.setField(field);

        // 执行
        List<StructureFieldConfig> result = yieldEnhancer.extractStructureFieldDefs(yield);

        // 验证
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId().value()).isEqualTo("struct_001");
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
