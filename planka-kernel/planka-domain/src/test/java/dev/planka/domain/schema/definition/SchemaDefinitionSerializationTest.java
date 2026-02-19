package dev.planka.domain.schema.definition;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.link.LinkFieldId;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.schema.EntityState;
import dev.planka.domain.schema.definition.cardtype.AbstractCardType;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.domain.schema.definition.fieldconfig.*;
import dev.planka.domain.schema.definition.fieldconfig.DateFieldConfig.DateFormat;
import dev.planka.domain.schema.definition.fieldconfig.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SchemaDefinition 序列化/反序列化测试
 * <p>
 * 测试所有 SchemaDefinition 子类的 Jackson 序列化和反序列化是否正常工作，
 * 包括：
 * - 卡片类型定义（AbstractCardType, EntityCardType）
 * - 属性定义（SingleLineTextFieldDefinition, MultiLineTextFieldDefinition, MarkdownFieldDefinition, NumberFieldDefinition 等 9 种）
 * - 属性配置（SingleLineTextFieldConfig, MultiLineTextFieldConfig, MarkdownFieldConfig, NumberFieldConfig 等 9 种）
 */
@DisplayName("SchemaDefinition 序列化/反序列化测试")
class SchemaDefinitionSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ==================== 卡片类型定义测试 ====================

    @Nested
    @DisplayName("卡片类型定义测试")
    class CardTypeDefinitionTest {

        @Test
        @DisplayName("AbstractCardType 序列化/反序列化")
        void testAbstractCardType() throws Exception {
            // Given
            AbstractCardType cardType = new AbstractCardType(
                    CardTypeId.of("ct_001"),
                    "org_001",
                    "工作项"
            );
            cardType.setCode("WORK_ITEM");
            cardType.setDescription("属性集-工作项");
            cardType.setSortOrder(1);
            cardType.setEnabled(true);
            cardType.setState(EntityState.ACTIVE);
            cardType.setContentVersion(1);
            cardType.setStructureVersion("1.0.0");
            cardType.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
            cardType.setCreatedBy("user_001");
            cardType.setUpdatedAt(LocalDateTime.of(2024, 1, 16, 14, 20, 0));
            cardType.setUpdatedBy("user_002");

            // When
            String json = objectMapper.writeValueAsString(cardType);
            System.out.println("AbstractCardType JSON:\n" + json);

            CardTypeDefinition deserialized = objectMapper.readValue(json, CardTypeDefinition.class);

            // Then
            assertThat(deserialized).isInstanceOf(AbstractCardType.class);
            AbstractCardType result = (AbstractCardType) deserialized;

            // 验证基础字段
            assertThat(result.getId().value()).isEqualTo("ct_001");
            assertThat(result.getOrgId()).isEqualTo("org_001");
            assertThat(result.getName()).isEqualTo("工作项");
            assertThat(result.getCode()).isEqualTo("WORK_ITEM");
            assertThat(result.getDescription()).isEqualTo("属性集-工作项");
            assertThat(result.getSortOrder()).isEqualTo(1);
            assertThat(result.isEnabled()).isTrue();
            assertThat(result.getState()).isEqualTo(EntityState.ACTIVE);
            assertThat(result.getContentVersion()).isEqualTo(1);
            assertThat(result.getStructureVersion()).isEqualTo("1.0.0");
            assertThat(result.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
            assertThat(result.getCreatedBy()).isEqualTo("user_001");
            assertThat(result.getUpdatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 16, 14, 20, 0));
            assertThat(result.getUpdatedBy()).isEqualTo("user_002");
        }

        @Test
        @DisplayName("EntityCardType 序列化/反序列化 - 包含所有字段")
        void testEntityCardType() throws Exception {
            // Given
            EntityCardType cardType = new EntityCardType(
                    CardTypeId.of("ct_002"),
                    "org_001",
                    "需求"
            );
            cardType.setCode("REQUIREMENT");
            cardType.setDescription("实体类型-需求");
            cardType.setSortOrder(2);
            cardType.setEnabled(true);
            cardType.setState(EntityState.ACTIVE);
            cardType.setContentVersion(2);
            cardType.setStructureVersion("1.1.0");
            cardType.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
            cardType.setCreatedBy("user_001");

            // EntityCardType 特有字段
            cardType.setParentTypeIds(Set.of(CardTypeId.of("ct_001"), CardTypeId.of("ct_003")));
            cardType.setDefaultCardFaceId("face_001");

            // 快速创建关联配置
            EntityCardType.QuickCreateLinkConfig linkConfig = new EntityCardType.QuickCreateLinkConfig();
            linkConfig.setName("拆分任务");
            linkConfig.setDescription("从需求拆分任务");
            linkConfig.setLinkFieldId(LinkFieldId.of("lt_001", LinkPosition.SOURCE));
            linkConfig.setTargetCardTypeIdsLimit(List.of("ct_task"));
            cardType.setQuickCreateLinkConfigs(List.of(linkConfig));

            // When
            String json = objectMapper.writeValueAsString(cardType);
            System.out.println("EntityCardType JSON:\n" + json);

            CardTypeDefinition deserialized = objectMapper.readValue(json, CardTypeDefinition.class);

            // Then
            assertThat(deserialized).isInstanceOf(EntityCardType.class);
            EntityCardType result = (EntityCardType) deserialized;

            // 验证基础字段
            assertThat(result.getId().value()).isEqualTo("ct_002");
            assertThat(result.getOrgId()).isEqualTo("org_001");
            assertThat(result.getName()).isEqualTo("需求");
            assertThat(result.getCode()).isEqualTo("REQUIREMENT");
            assertThat(result.getDescription()).isEqualTo("实体类型-需求");

            // 验证 EntityCardType 特有字段
            assertThat(result.getParentTypeIds())
                    .extracting(CardTypeId::value)
                    .containsExactlyInAnyOrder("ct_001", "ct_003");
            assertThat(result.getDefaultCardFaceId()).isEqualTo("face_001");

            // 验证快速创建关联配置
            assertThat(result.getQuickCreateLinkConfigs()).hasSize(1);
            EntityCardType.QuickCreateLinkConfig resultConfig = result.getQuickCreateLinkConfigs().get(0);
            assertThat(resultConfig.getName()).isEqualTo("拆分任务");
            assertThat(resultConfig.getDescription()).isEqualTo("从需求拆分任务");
            assertThat(resultConfig.getLinkFieldId().getLinkTypeId()).isEqualTo("lt_001");
            assertThat(resultConfig.getLinkFieldId().getPosition()).isEqualTo(LinkPosition.SOURCE);
            assertThat(resultConfig.getTargetCardTypeIdsLimit()).containsExactly("ct_task");

            // 验证类型标识
        }
    }

    // ==================== 属性配置测试 ====================

    @Nested
    @DisplayName("属性配置测试")
    class FieldConfigTests {

        @Test
        @DisplayName("SingleLineTextFieldConfig 序列化/反序列化")
        void testSingleLineTextFieldConfig() throws Exception {
            // Given
            SingleLineTextFieldConfig config = new SingleLineTextFieldConfig(
                    FieldConfigId.of("fc_001"),
                    "org_001",
                    "标题",
                    CardTypeId.of("ct_001"),
                    FieldId.of("f_001"),
                    true
            );
            config.setDescription("需求标题配置");
            config.setRequired(true);
            config.setReadOnly(false);
            config.setAreaOrder(1);
            config.setMaxLength(100);
            config.setDefaultValue("新需求");
            config.setPlaceholder("请输入需求标题");
            config.setState(EntityState.ACTIVE);
            config.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
            config.setCreatedBy("user_001");

            // When
            String json = objectMapper.writeValueAsString(config);
            System.out.println("SingleLineTextFieldConfig JSON:\n" + json);

            FieldConfig deserialized = objectMapper.readValue(json, FieldConfig.class);

            // Then
            assertThat(deserialized).isInstanceOf(SingleLineTextFieldConfig.class);
            SingleLineTextFieldConfig result = (SingleLineTextFieldConfig) deserialized;

            // 验证基础配置字段
            assertThat(result.getId().value()).isEqualTo("fc_001");
            assertThat(result.getOrgId()).isEqualTo("org_001");
            assertThat(result.getName()).isEqualTo("标题");
            assertThat(result.getCardTypeId().value()).isEqualTo("ct_001");
            assertThat(result.getFieldId().value()).isEqualTo("f_001");
            assertThat(result.isSystemField()).isTrue();
            assertThat(result.getRequired()).isTrue();
            assertThat(result.getReadOnly()).isFalse();
            assertThat(result.getAreaOrder()).isEqualTo(1);

            // 验证文本特有字段
            assertThat(result.getMaxLength()).isEqualTo(100);
            assertThat(result.getDefaultValue()).isEqualTo("新需求");
            assertThat(result.getPlaceholder()).isEqualTo("请输入需求标题");
        }

        @Test
        @DisplayName("NumberFieldConfig 序列化/反序列化")
        void testNumberFieldConfig() throws Exception {
            // Given
            NumberFieldConfig config = new NumberFieldConfig(
                    FieldConfigId.of("fc_002"),
                    "org_001",
                    "预估工时",
                    CardTypeId.of("ct_001"),
                    FieldId.of("f_003"),
                    false
            );
            config.setRequired(false);
            config.setMinValue(0.5);
            config.setMaxValue(100.0);
            config.setPrecision(1);
            config.setDefaultValue(4.0);
            config.setUnit("人天");
            config.setShowThousandSeparator(false);
            config.setState(EntityState.ACTIVE);

            // When
            String json = objectMapper.writeValueAsString(config);
            System.out.println("NumberFieldConfig JSON:\n" + json);

            FieldConfig deserialized = objectMapper.readValue(json, FieldConfig.class);

            // Then
            assertThat(deserialized).isInstanceOf(NumberFieldConfig.class);
            NumberFieldConfig result = (NumberFieldConfig) deserialized;

            assertThat(result.getId().value()).isEqualTo("fc_002");
            assertThat(result.getCardTypeId().value()).isEqualTo("ct_001");
            assertThat(result.getFieldId().value()).isEqualTo("f_003");
            assertThat(result.getMinValue()).isEqualTo(0.5);
            assertThat(result.getMaxValue()).isEqualTo(100.0);
            assertThat(result.getPrecision()).isEqualTo(1);
            assertThat(result.getDefaultValue()).isEqualTo(4.0);
            assertThat(result.getUnit()).isEqualTo("人天");
            assertThat(result.isShowThousandSeparator()).isFalse();
        }

        @Test
        @DisplayName("DateFieldConfig 序列化/反序列化")
        void testDateFieldConfig() throws Exception {
            // Given
            DateFieldConfig config = new DateFieldConfig(
                    FieldConfigId.of("fc_003"),
                    "org_001",
                    "计划开始日期",
                    CardTypeId.of("ct_001"),
                    FieldId.of("f_004"),
                    false
            );
            config.setRequired(true);
            config.setDateFormat(DateFormat.DATE);
            config.setUseNowAsDefault(true);
            config.setState(EntityState.ACTIVE);

            // When
            String json = objectMapper.writeValueAsString(config);
            System.out.println("DateFieldConfig JSON:\n" + json);

            FieldConfig deserialized = objectMapper.readValue(json, FieldConfig.class);

            // Then
            assertThat(deserialized).isInstanceOf(DateFieldConfig.class);
            DateFieldConfig result = (DateFieldConfig) deserialized;

            assertThat(result.getId().value()).isEqualTo("fc_003");
            assertThat(result.getDateFormat()).isEqualTo(DateFormat.DATE);
            assertThat(result.isUseNowAsDefault()).isTrue();
        }

        @Test
        @DisplayName("EnumFieldConfig 序列化/反序列化")
        void testEnumFieldConfig() throws Exception {
            // Given
            EnumFieldConfig config = new EnumFieldConfig(
                    FieldConfigId.of("fc_004"),
                    "org_001",
                    "优先级",
                    CardTypeId.of("ct_001"),
                    FieldId.of("f_006"),
                    true
            );
            config.setRequired(true);
            config.setMultiSelect(false);
            config.setOptions(List.of(
                    new EnumFieldConfig.EnumOptionDefinition("p1", "紧急", "紧急", true, "#FF0000", 1),
                    new EnumFieldConfig.EnumOptionDefinition("p2", "高", "高", true, "#FFA500", 2),
                    new EnumFieldConfig.EnumOptionDefinition("p3", "中", "高", true, "#FFFF00", 3)
            ));
            config.setDefaultOptionIds(List.of("p2"));
            config.setState(EntityState.ACTIVE);

            // When
            String json = objectMapper.writeValueAsString(config);
            System.out.println("EnumFieldConfig JSON:\n" + json);

            FieldConfig deserialized = objectMapper.readValue(json, FieldConfig.class);

            // Then
            assertThat(deserialized).isInstanceOf(EnumFieldConfig.class);
            EnumFieldConfig result = (EnumFieldConfig) deserialized;

            assertThat(result.getId().value()).isEqualTo("fc_004");
            assertThat(result.isMultiSelect()).isFalse();
            assertThat(result.getOptions()).hasSize(3);
            assertThat(result.getDefaultOptionIds()).containsExactly("p2");
        }

        @Test
        @DisplayName("AttachmentFieldConfig 序列化/反序列化")
        void testAttachmentFieldConfig() throws Exception {
            // Given
            AttachmentFieldConfig config = new AttachmentFieldConfig(
                    FieldConfigId.of("fc_005"),
                    "org_001",
                    "附件",
                    CardTypeId.of("ct_001"),
                    FieldId.of("f_008"),
                    false
            );
            config.setRequired(false);
            config.setAllowedFileTypes(List.of("pdf", "png", "jpg"));
            config.setMaxFileSize(5 * 1024 * 1024L); // 5MB
            config.setMaxFileCount(3);
            config.setState(EntityState.ACTIVE);

            // When
            String json = objectMapper.writeValueAsString(config);
            System.out.println("AttachmentFieldConfig JSON:\n" + json);

            FieldConfig deserialized = objectMapper.readValue(json, FieldConfig.class);

            // Then
            assertThat(deserialized).isInstanceOf(AttachmentFieldConfig.class);
            AttachmentFieldConfig result = (AttachmentFieldConfig) deserialized;

            assertThat(result.getId().value()).isEqualTo("fc_005");
            assertThat(result.getAllowedFileTypes()).containsExactly("pdf", "png", "jpg");
            assertThat(result.getMaxFileSize()).isEqualTo(5 * 1024 * 1024L);
            assertThat(result.getMaxFileCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("WebUrlFieldConfig 序列化/反序列化")
        void testWebUrlFieldConfig() throws Exception {
            // Given
            WebUrlFieldConfig config = new WebUrlFieldConfig(
                    FieldConfigId.of("fc_006"),
                    "org_001",
                    "参考链接",
                    CardTypeId.of("ct_001"),
                    FieldId.of("f_009"),
                    false
            );
            config.setRequired(false);
            config.setValidateUrl(true);
            config.setShowPreview(false);
            config.setDefaultUrl("https://docs.example.com");
            config.setDefaultLinkText("文档链接");
            config.setState(EntityState.ACTIVE);

            // When
            String json = objectMapper.writeValueAsString(config);
            System.out.println("WebUrlFieldConfig JSON:\n" + json);

            FieldConfig deserialized = objectMapper.readValue(json, FieldConfig.class);

            // Then
            assertThat(deserialized).isInstanceOf(WebUrlFieldConfig.class);
            WebUrlFieldConfig result = (WebUrlFieldConfig) deserialized;

            assertThat(result.getId().value()).isEqualTo("fc_006");
            assertThat(result.isValidateUrl()).isTrue();
            assertThat(result.isShowPreview()).isFalse();
            assertThat(result.getDefaultUrl()).isEqualTo("https://docs.example.com");
            assertThat(result.getDefaultLinkText()).isEqualTo("文档链接");
        }

        @Test
        @DisplayName("StructureFieldConfig 序列化/反序列化")
        void testStructureFieldConfig() throws Exception {
            // Given
            StructureFieldConfig config = new StructureFieldConfig(
                    FieldConfigId.of("fc_007"),
                    "org_001",
                    "所属部门",
                    CardTypeId.of("ct_001"),
                    FieldId.of("f_010"),
                    false
            );
            config.setRequired(true);
            config.setLeafOnly(true);
            config.setState(EntityState.ACTIVE);

            // When
            String json = objectMapper.writeValueAsString(config);
            System.out.println("StructureFieldConfig JSON:\n" + json);

            FieldConfig deserialized = objectMapper.readValue(json, FieldConfig.class);

            // Then
            assertThat(deserialized).isInstanceOf(StructureFieldConfig.class);
            StructureFieldConfig result = (StructureFieldConfig) deserialized;

            assertThat(result.getId().value()).isEqualTo("fc_007");
            assertThat(result.isLeafOnly()).isTrue();
        }
    }

    // ==================== 多态反序列化测试 ====================

    @Nested
    @DisplayName("多态反序列化测试")
    class PolymorphicDeserializationTest {

        @Test
        @DisplayName("通过 JSON 字符串反序列化不同类型的 FieldDefinition")
        void testFieldDefinitionPolymorphicDeserialization() throws Exception {
            // TEXT 类型 - 使用 schemaSubType 作为类型标识
            String singleLineTextJson = """
                    {
                        "schemaSubType": "TEXT_FIELD",
                        "id": "fc_100",
                        "fieldId": "f_100",
                        "orgId": "org_001",
                        "name": "测试单行文本",
                        "systemField": false,
                        "maxLength": 200
                    }
                    """;
            SchemaDefinition<?> singleLineTextField = objectMapper.readValue(singleLineTextJson, SchemaDefinition.class);
            assertThat(singleLineTextField).isInstanceOf(SingleLineTextFieldConfig.class);
            assertThat(((SingleLineTextFieldConfig) singleLineTextField).getMaxLength()).isEqualTo(200);

            // MARKDOWN 类型
            String markdownJson = """
                    {
                        "schemaSubType": "MARKDOWN_FIELD",
                        "id": "fc_100b",
                        "fieldId": "f_100b",
                        "orgId": "org_001",
                        "name": "测试Markdown",
                        "systemField": false,
                        "maxLength": 10000
                    }
                    """;
            SchemaDefinition<?> markdownField = objectMapper.readValue(markdownJson, SchemaDefinition.class);
            assertThat(markdownField).isInstanceOf(MarkdownFieldConfig.class);
            assertThat(((MarkdownFieldConfig) markdownField).getMaxLength()).isEqualTo(10000);

            // NUMBER 类型
            String numberJson = """
                    {
                        "schemaSubType": "NUMBER_FIELD",
                        "id": "fc_101",
                        "fieldId": "f_101",
                        "orgId": "org_001",
                        "name": "测试数字",
                        "systemField": false,
                        "precision": 2
                    }
                    """;
            SchemaDefinition<?> numberField = objectMapper.readValue(numberJson, SchemaDefinition.class);
            assertThat(numberField).isInstanceOf(NumberFieldConfig.class);
            assertThat(((NumberFieldConfig) numberField).getPrecision()).isEqualTo(2);

            // DATE 类型
            String dateJson = """
                    {
                        "schemaSubType": "DATE_FIELD",
                        "id": "fc_102",
                        "fieldId": "f_102",
                        "orgId": "org_001",
                        "name": "测试日期",
                        "systemField": false,
                        "dateFormat": "YEAR_MONTH"
                    }
                    """;
            SchemaDefinition<?> dateField = objectMapper.readValue(dateJson, SchemaDefinition.class);
            assertThat(dateField).isInstanceOf(DateFieldConfig.class);
            assertThat(((DateFieldConfig) dateField).getDateFormat()).isEqualTo(DateFormat.YEAR_MONTH);
        }

        @Test
        @DisplayName("通过 JSON 字符串反序列化不同类型的 CardTypeDefinition")
        void testCardTypePolymorphicDeserialization() throws Exception {
            // ABSTRACT 类型 - 使用 schemaType 作为类型标识
            String abstractJson = """
                    {
                        "schemaSubType": "TRAIT_CARD_TYPE",
                        "id": "ct_100",
                        "orgId": "org_001",
                        "name": "属性集"
                    }
                    """;
            SchemaDefinition<?> abstractType = objectMapper.readValue(abstractJson, SchemaDefinition.class);
            assertThat(abstractType).isInstanceOf(AbstractCardType.class);

            // ENTITY 类型
            String concreteJson = """
                    {
                        "schemaSubType": "ENTITY_CARD_TYPE",
                        "id": "ct_101",
                        "orgId": "org_001",
                        "name": "实体类型",
                        "parentTypeIds": ["ct_100"]
                    }
                    """;
            SchemaDefinition<?> concreteType = objectMapper.readValue(concreteJson, SchemaDefinition.class);
            assertThat(concreteType).isInstanceOf(EntityCardType.class);
            assertThat(((EntityCardType) concreteType).getParentTypeIds())
                    .extracting(CardTypeId::value)
                    .containsExactly("ct_100");
        }
    }
}
