package dev.planka.api.schema.service;

import dev.planka.api.schema.spi.SchemaDataProvider;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.domain.schema.definition.fieldconfig.SingleLineTextFieldConfig;
import dev.planka.domain.schema.definition.linkconfig.LinkFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * FieldConfigResolver 单元测试
 * <p>
 * 测试继承优先级：自身 > 显式父类 > 任意卡属性集
 */
@ExtendWith(MockitoExtension.class)
class FieldConfigResolverTest {

    @Mock
    private SchemaDataProvider schemaDataProvider;

    private FieldConfigResolver resolver;

    private static final String ORG_ID = "org-1";
    private static final String ROOT_CARD_TYPE_ID = "org-1:any-trait";

    @BeforeEach
    void setUp() {
        resolver = new FieldConfigResolver(schemaDataProvider);
    }

    @Nested
    @DisplayName("继承优先级测试")
    class InheritancePriorityTests {

        @Test
        @DisplayName("显式父类配置覆盖任意卡属性集配置")
        void parentConfigOverridesRootConfig() {
            // Given
            EntityCardType cardType = createEntityCardType("concrete-1", "需求", Set.of("abstract-1"));

            // 任意卡属性集的配置
            SingleLineTextFieldConfig rootConfig = createFieldConfig("root-config", ROOT_CARD_TYPE_ID, "field-1");
            rootConfig.setDefaultValue("顶级默认值");

            // 显式父类的配置
            SingleLineTextFieldConfig parentConfig = createFieldConfig("parent-config", "abstract-1", "field-1");
            parentConfig.setDefaultValue("父类默认值");

            when(schemaDataProvider.getAllFieldConfigsByCardTypeId(ROOT_CARD_TYPE_ID))
                    .thenReturn(List.of(rootConfig));
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("abstract-1"))
                    .thenReturn(List.of(parentConfig));
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("concrete-1"))
                    .thenReturn(Collections.emptyList());

            // 模拟空的关联类型返回
            when(schemaDataProvider.getLinkTypesByCardTypeId(anyString()))
                    .thenReturn(Collections.emptyList());

            // When
            FieldConfigResolver.ResolvedFieldConfigs result = resolver.resolve(cardType);

            // Then - 父类配置应该覆盖任意卡属性集配置
            assertThat(result.fieldConfigs()).containsKey("field-1");
            SingleLineTextFieldConfig effectiveConfig = (SingleLineTextFieldConfig) result.fieldConfigs().get("field-1");
            assertThat(effectiveConfig.getDefaultValue()).isEqualTo("父类默认值");
            assertThat(result.configSources().get("field-1")).isEqualTo("abstract-1");
        }

        @Test
        @DisplayName("自身配置覆盖显式父类配置")
        void ownConfigOverridesParentConfig() {
            // Given
            EntityCardType cardType = createEntityCardType("concrete-1", "需求", Set.of("abstract-1"));

            // 显式父类的配置
            SingleLineTextFieldConfig parentConfig = createFieldConfig("parent-config", "abstract-1", "field-1");
            parentConfig.setDefaultValue("父类默认值");

            // 自身配置
            SingleLineTextFieldConfig ownConfig = createFieldConfig("own-config", "concrete-1", "field-1");
            ownConfig.setDefaultValue("自身默认值");

            when(schemaDataProvider.getAllFieldConfigsByCardTypeId(ROOT_CARD_TYPE_ID))
                    .thenReturn(Collections.emptyList());
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("abstract-1"))
                    .thenReturn(List.of(parentConfig));
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("concrete-1"))
                    .thenReturn(List.of(ownConfig));

            when(schemaDataProvider.getLinkTypesByCardTypeId(anyString()))
                    .thenReturn(Collections.emptyList());

            // When
            FieldConfigResolver.ResolvedFieldConfigs result = resolver.resolve(cardType);

            // Then - 自身配置应该覆盖父类配置
            assertThat(result.fieldConfigs()).containsKey("field-1");
            SingleLineTextFieldConfig effectiveConfig = (SingleLineTextFieldConfig) result.fieldConfigs().get("field-1");
            assertThat(effectiveConfig.getDefaultValue()).isEqualTo("自身默认值");
            assertThat(result.configSources().get("field-1")).isEqualTo("concrete-1");
        }

        @Test
        @DisplayName("后声明的父类覆盖先声明的父类")
        void laterParentOverridesEarlierParent() {
            // Given - parentTypeIds 是有序的，后面的覆盖前面的
            EntityCardType cardType = createEntityCardType("concrete-1", "需求",
                    new java.util.LinkedHashSet<>(List.of("abstract-1", "abstract-2")));

            SingleLineTextFieldConfig parent1Config = createFieldConfig("config-1", "abstract-1", "field-1");
            parent1Config.setDefaultValue("父类1默认值");

            SingleLineTextFieldConfig parent2Config = createFieldConfig("config-2", "abstract-2", "field-1");
            parent2Config.setDefaultValue("父类2默认值");

            when(schemaDataProvider.getAllFieldConfigsByCardTypeId(ROOT_CARD_TYPE_ID))
                    .thenReturn(Collections.emptyList());
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("abstract-1"))
                    .thenReturn(List.of(parent1Config));
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("abstract-2"))
                    .thenReturn(List.of(parent2Config));
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("concrete-1"))
                    .thenReturn(Collections.emptyList());

            when(schemaDataProvider.getLinkTypesByCardTypeId(anyString()))
                    .thenReturn(Collections.emptyList());

            // When
            FieldConfigResolver.ResolvedFieldConfigs result = resolver.resolve(cardType);

            // Then - abstract-2 的配置应该覆盖 abstract-1 的配置
            assertThat(result.fieldConfigs()).containsKey("field-1");
            SingleLineTextFieldConfig effectiveConfig = (SingleLineTextFieldConfig) result.fieldConfigs().get("field-1");
            assertThat(effectiveConfig.getDefaultValue()).isEqualTo("父类2默认值");
            assertThat(result.configSources().get("field-1")).isEqualTo("abstract-2");
        }

        @Test
        @DisplayName("混合继承优先级正确（顶级+多父类+自身）")
        void mixedInheritancePriorityCorrect() {
            // Given
            EntityCardType cardType = createEntityCardType("concrete-1", "需求",
                    new java.util.LinkedHashSet<>(List.of("abstract-1", "abstract-2")));

            // 三个来源都有 field-1 的配置
            SingleLineTextFieldConfig rootConfig = createFieldConfig("root-config", ROOT_CARD_TYPE_ID, "field-1");
            rootConfig.setDefaultValue("顶级默认值");

            SingleLineTextFieldConfig parent1Config = createFieldConfig("config-1", "abstract-1", "field-1");
            parent1Config.setDefaultValue("父类1默认值");

            SingleLineTextFieldConfig parent2Config = createFieldConfig("config-2", "abstract-2", "field-1");
            parent2Config.setDefaultValue("父类2默认值");

            // 顶级有 field-2，父类1 有 field-3
            SingleLineTextFieldConfig rootConfig2 = createFieldConfig("root-config-2", ROOT_CARD_TYPE_ID, "field-2");
            SingleLineTextFieldConfig parent1Config3 = createFieldConfig("config-3", "abstract-1", "field-3");

            when(schemaDataProvider.getAllFieldConfigsByCardTypeId(ROOT_CARD_TYPE_ID))
                    .thenReturn(List.of(rootConfig, rootConfig2));
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("abstract-1"))
                    .thenReturn(List.of(parent1Config, parent1Config3));
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("abstract-2"))
                    .thenReturn(List.of(parent2Config));
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("concrete-1"))
                    .thenReturn(Collections.emptyList());

            when(schemaDataProvider.getLinkTypesByCardTypeId(anyString()))
                    .thenReturn(Collections.emptyList());

            // When
            FieldConfigResolver.ResolvedFieldConfigs result = resolver.resolve(cardType);

            // Then
            assertThat(result.fieldConfigs()).hasSize(3);
            // field-1: 父类2 覆盖父类1，父类1 覆盖顶级
            assertThat(((SingleLineTextFieldConfig) result.fieldConfigs().get("field-1")).getDefaultValue())
                    .isEqualTo("父类2默认值");
            assertThat(result.configSources().get("field-1")).isEqualTo("abstract-2");
            // field-2: 来自顶级
            assertThat(result.configSources().get("field-2")).isEqualTo(ROOT_CARD_TYPE_ID);
            // field-3: 来自父类1
            assertThat(result.configSources().get("field-3")).isEqualTo("abstract-1");
        }
    }

    @Nested
    @DisplayName("关联配置解析测试")
    class LinkConfigResolveTests {

        @Test
        @DisplayName("继承任意卡属性集关联配置")
        void inheritRootLinkConfig() {
            // Given
            EntityCardType cardType = createEntityCardType("concrete-1", "需求", null);

            LinkFieldConfig rootLinkConfig = createLinkFieldConfig("root-link-config", ROOT_CARD_TYPE_ID,
                    "link-creator", LinkPosition.SOURCE);

            when(schemaDataProvider.getAllFieldConfigsByCardTypeId(ROOT_CARD_TYPE_ID))
                    .thenReturn(List.of(rootLinkConfig));
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("concrete-1"))
                    .thenReturn(Collections.emptyList());
            // Mock 关联类型查询
            when(schemaDataProvider.getLinkTypesByCardTypeId(anyString()))
                    .thenReturn(Collections.emptyList());

            // When
            FieldConfigResolver.ResolvedFieldConfigs result = resolver.resolve(cardType);

            // Then
            String key = "link-creator:SOURCE";
            assertThat(result.fieldConfigs()).containsKey(key);
            assertThat(result.configSources().get(key)).isEqualTo(ROOT_CARD_TYPE_ID);
        }

        @Test
        @DisplayName("自身关联配置覆盖任意卡属性集")
        void ownLinkConfigOverridesRoot() {
            // Given
            EntityCardType cardType = createEntityCardType("concrete-1", "需求", null);

            LinkFieldConfig rootLinkConfig = createLinkFieldConfig("root-link-config", ROOT_CARD_TYPE_ID,
                    "link-creator", LinkPosition.SOURCE);
            rootLinkConfig.setDisplayName("顶级创建人");

            LinkFieldConfig ownLinkConfig = createLinkFieldConfig("own-link-config", "concrete-1",
                    "link-creator", LinkPosition.SOURCE);
            ownLinkConfig.setDisplayName("自定义创建人");

            when(schemaDataProvider.getAllFieldConfigsByCardTypeId(ROOT_CARD_TYPE_ID))
                    .thenReturn(List.of(rootLinkConfig));
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("concrete-1"))
                    .thenReturn(List.of(ownLinkConfig));
            // Mock 关联类型查询
            when(schemaDataProvider.getLinkTypesByCardTypeId(anyString()))
                    .thenReturn(Collections.emptyList());

            // When
            FieldConfigResolver.ResolvedFieldConfigs result = resolver.resolve(cardType);

            // Then
            String key = "link-creator:SOURCE";
            assertThat(result.fieldConfigs()).containsKey(key);
            assertThat(((LinkFieldConfig) result.fieldConfigs().get(key)).getDisplayName()).isEqualTo("自定义创建人");
            assertThat(result.configSources().get(key)).isEqualTo("concrete-1");
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("空 parentTypeIds 视为无父类")
        void emptyParentTypeIdsTreatedAsNoParent() {
            // Given
            EntityCardType cardType = createEntityCardType("concrete-1", "需求", Collections.emptySet());

            SingleLineTextFieldConfig ownConfig = createFieldConfig("own-config", "concrete-1", "field-1");

            when(schemaDataProvider.getAllFieldConfigsByCardTypeId(ROOT_CARD_TYPE_ID))
                    .thenReturn(Collections.emptyList());
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("concrete-1"))
                    .thenReturn(List.of(ownConfig));
            when(schemaDataProvider.getLinkTypesByCardTypeId(anyString()))
                    .thenReturn(Collections.emptyList());

            // When
            FieldConfigResolver.ResolvedFieldConfigs result = resolver.resolve(cardType);

            // Then
            assertThat(result.fieldConfigs()).containsKey("field-1");
            assertThat(result.configSources().get("field-1")).isEqualTo("concrete-1");
        }

        @Test
        @DisplayName("null parentTypeIds 视为无父类")
        void nullParentTypeIdsTreatedAsNoParent() {
            // Given
            EntityCardType cardType = createEntityCardType("concrete-1", "需求", null);

            SingleLineTextFieldConfig ownConfig = createFieldConfig("own-config", "concrete-1", "field-1");

            when(schemaDataProvider.getAllFieldConfigsByCardTypeId(ROOT_CARD_TYPE_ID))
                    .thenReturn(Collections.emptyList());
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("concrete-1"))
                    .thenReturn(List.of(ownConfig));
            when(schemaDataProvider.getLinkTypesByCardTypeId(anyString()))
                    .thenReturn(Collections.emptyList());

            // When
            FieldConfigResolver.ResolvedFieldConfigs result = resolver.resolve(cardType);

            // Then
            assertThat(result.fieldConfigs()).containsKey("field-1");
        }

        @Test
        @DisplayName("任意卡属性集自身不继承任意卡属性集配置")
        void rootCardTypeDoesNotInheritFromItself() {
            // Given
            EntityCardType rootCardType = createEntityCardType(ROOT_CARD_TYPE_ID, "任意卡属性集", null);

            SingleLineTextFieldConfig rootConfig = createFieldConfig("root-config", ROOT_CARD_TYPE_ID, "field-1");

            when(schemaDataProvider.getAllFieldConfigsByCardTypeId(ROOT_CARD_TYPE_ID))
                    .thenReturn(List.of(rootConfig));
            when(schemaDataProvider.getLinkTypesByCardTypeId(ROOT_CARD_TYPE_ID))
                    .thenReturn(Collections.emptyList());

            // When
            FieldConfigResolver.ResolvedFieldConfigs result = resolver.resolve(rootCardType);

            // Then - 应该只查询一次（自身），不会重复查询任意卡属性集
            verify(schemaDataProvider, times(1))
                    .getAllFieldConfigsByCardTypeId(ROOT_CARD_TYPE_ID);
            assertThat(result.fieldConfigs()).containsKey("field-1");
        }
    }

    // ==================== 测试辅助方法 ====================

    private EntityCardType createEntityCardType(String id, String name, Set<String> parentTypeIdStrings) {
        EntityCardType cardType = new EntityCardType(CardTypeId.of(id), ORG_ID, name);
        if (parentTypeIdStrings != null) {
            cardType.setParentTypeIds(parentTypeIdStrings.stream()
                    .map(CardTypeId::of)
                    .collect(java.util.stream.Collectors.toSet()));
        }
        return cardType;
    }

    private SingleLineTextFieldConfig createFieldConfig(String id, String cardTypeId, String fieldId) {
        return new SingleLineTextFieldConfig(
                new FieldConfigId(id), ORG_ID, "配置-" + fieldId,
                new CardTypeId(cardTypeId), new FieldId(fieldId), false);
    }

    private LinkFieldConfig createLinkFieldConfig(String id, String cardTypeId, String linkTypeId, LinkPosition position) {
        String linkFieldId = linkTypeId + ":" + position.name();
        return new LinkFieldConfig(
                new FieldConfigId(id), ORG_ID, "关联配置",
                new CardTypeId(cardTypeId), FieldId.of(linkFieldId), false);
    }
}
