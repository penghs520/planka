package dev.planka.schema.service;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.schema.ReferenceType;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.ViewId;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import dev.planka.domain.schema.definition.fieldconfig.SingleLineTextFieldConfig;
import dev.planka.domain.schema.definition.view.ListViewDefinition;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.reference.SchemaReferenceAnalyzer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

/**
 * SchemaReferenceAnalyzer 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SchemaReferenceAnalyzerTest {

    @Mock
    private SchemaRepository schemaRepository;

    @InjectMocks
    private SchemaReferenceAnalyzer analyzer;

    @Nested
    @DisplayName("基础场景测试")
    class BasicScenarios {

        @Test
        @DisplayName("空定义返回空列表")
        void analyze_nullDefinition_returnsEmptyList() {
            List<SchemaReferenceAnalyzer.ReferenceInfo> result = analyzer.analyze(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(schemaRepository);
        }

        @Test
        @DisplayName("无引用的定义返回空列表")
        void analyze_definitionWithoutReferences_returnsEmptyList() {
            // 使用没有任何SchemaId字段的简单定义类
            TestDefinitionNoRefs testDef = new TestDefinitionNoRefs(
                    new CardTypeId("1234567890123456789"), "org-1", "测试");

            List<SchemaReferenceAnalyzer.ReferenceInfo> result = analyzer.analyze(testDef);

            assertThat(result).isEmpty();
            verifyNoInteractions(schemaRepository);
        }
    }

    @Nested
    @DisplayName("SchemaId 类型引用测试")
    class SchemaIdReferences {

        @Test
        @DisplayName("分析 CardTypeId 类型引用（不是 belongTo，是聚合引用）")
        void analyze_cardTypeIdReference_returnsAggregationRef() {
            // ListViewDefinition.belongTo() 返回 null，cardTypeId 是聚合引用
            ListViewDefinition viewDef = new ListViewDefinition(new ViewId("1234567890123456789"), "org-1", "测试视图");
            viewDef.setCardTypeId(new CardTypeId("9876543210987654321"));

            List<SchemaReferenceAnalyzer.ReferenceInfo> result = analyzer.analyze(viewDef);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).targetId()).isEqualTo("9876543210987654321");
            assertThat(result.get(0).targetType()).isEqualTo(SchemaType.CARD_TYPE);
            assertThat(result.get(0).referenceType()).isEqualTo(ReferenceType.AGGREGATION);
            verifyNoInteractions(schemaRepository);
        }


        @Test
        @DisplayName("同时存在组合引用和聚合引用")
        void analyze_mixedReferences_returnsCorrectTypes() {
            // 使用测试定义类，同时包含 belongTo（组合）和非 belongTo（聚合）的 SchemaId 字段
            TestDefinitionWithMixedIds testDef = new TestDefinitionWithMixedIds(new ViewId("1234567890123456789"), "org-1", "测试");
            // cardTypeId 是 belongTo（组合引用）
            testDef.setCardTypeId(new CardTypeId("1111111111111111111"));
            // otherFieldId 不是 belongTo（聚合引用）
            testDef.setOtherFieldId(new FieldId("2222222222222222222"));

            List<SchemaReferenceAnalyzer.ReferenceInfo> result = analyzer.analyze(testDef);

            assertThat(result).hasSize(2);

            // 验证组合引用
            var compositionRef = result.stream()
                    .filter(r -> r.targetId().equals("1111111111111111111"))
                    .findFirst().orElseThrow();
            assertThat(compositionRef.referenceType()).isEqualTo(ReferenceType.COMPOSITION);
            assertThat(compositionRef.targetType()).isEqualTo(SchemaType.CARD_TYPE);

            // 验证聚合引用
            var aggregationRef = result.stream()
                    .filter(r -> r.targetId().equals("2222222222222222222"))
                    .findFirst().orElseThrow();
            assertThat(aggregationRef.referenceType()).isEqualTo(ReferenceType.AGGREGATION);
            assertThat(aggregationRef.targetType()).isEqualTo(SchemaType.FIELD_CONFIG);

            verifyNoInteractions(schemaRepository);
        }
    }

    @Nested
    @DisplayName("String 类型 ID 批量查询测试")
    class StringIdBatchQuery {

        @Test
        @DisplayName("非雪花ID格式的字符串不触发查询")
        void analyze_nonSnowflakeString_noQuery() {
            SingleLineTextFieldConfig fieldConfig = new SingleLineTextFieldConfig(
                    new FieldConfigId("1234567890123456789"), "org-1", "测试文本字段",
                    null, new FieldId("f-1"), false);
            fieldConfig.setCode("TEXT_FIELD"); // 非雪花 ID 格式

            analyzer.analyze(fieldConfig);

            // code 不是雪花 ID 格式，不会触发查询
            verifyNoInteractions(schemaRepository);
        }

        @Test
        @DisplayName("雪花ID格式的字符串触发批量查询")
        void analyze_snowflakeString_triggersBatchQuery() {
            // 准备一个包含雪花 ID 格式字符串的定义
            TestDefinitionWithStringIds testDef = new TestDefinitionWithStringIds(new CardTypeId("1234567890123456789"), "org-1", "测试");
            testDef.setRefId1("1111111111111111111");
            testDef.setRefId2("2222222222222222222");

            // Mock 批量查询结果
            SingleLineTextFieldConfig field1 = new SingleLineTextFieldConfig(
                    new FieldConfigId("1111111111111111111"), "org-1", "字段1",
                    null, new FieldId("f-1"), false);
            SingleLineTextFieldConfig field2 = new SingleLineTextFieldConfig(
                    new FieldConfigId("2222222222222222222"), "org-1", "字段2",
                    null, new FieldId("f-2"), false);

            when(schemaRepository.findByIds(anySet()))
                    .thenReturn(List.of(field1, field2));

            List<SchemaReferenceAnalyzer.ReferenceInfo> result = analyzer.analyze(testDef);

            // 验证批量查询被调用一次
            verify(schemaRepository, times(1)).findByIds(anySet());
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("批量查询仅查询未知类型的ID")
        void analyze_mixedIds_onlyQuerysUnknownTypes() {
            // 准备一个同时包含 SchemaId 和 String ID 的定义
            TestDefinitionWithMixedIds testDef = new TestDefinitionWithMixedIds(new ViewId("1234567890123456789"), "org-1", "测试");
            testDef.setCardTypeId(new CardTypeId("1111111111111111111")); // 已知类型
            testDef.setStringRefId("2222222222222222222"); // 需要查询

            // Mock 批量查询结果
            SingleLineTextFieldConfig field = new SingleLineTextFieldConfig(
                    new FieldConfigId("2222222222222222222"), "org-1", "字段",
                    null, new FieldId("f-2"), false);
            when(schemaRepository.findByIds(Set.of("2222222222222222222")))
                    .thenReturn(List.of(field));

            List<SchemaReferenceAnalyzer.ReferenceInfo> result = analyzer.analyze(testDef);

            // 验证只查询了 String ID
            verify(schemaRepository, times(1)).findByIds(Set.of("2222222222222222222"));
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("查询不到的ID不加入结果")
        void analyze_unknownId_notIncluded() {
            TestDefinitionWithStringIds testDef = new TestDefinitionWithStringIds(new CardTypeId("1234567890123456789"), "org-1", "测试");
            testDef.setRefId1("1111111111111111111");

            // Mock 批量查询返回空
            when(schemaRepository.findByIds(anySet()))
                    .thenReturn(Collections.emptyList());

            List<SchemaReferenceAnalyzer.ReferenceInfo> result = analyzer.analyze(testDef);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("集合类型引用测试")
    class CollectionReferences {

        @Test
        @DisplayName("分析 SchemaId 集合")
        void analyze_schemaIdCollection_returnsAllRefs() {
            TestDefinitionWithIdCollection testDef = new TestDefinitionWithIdCollection(new CardTypeId("1234567890123456789"), "org-1", "测试");
            testDef.setFieldIds(List.of(
                    new FieldId("1111111111111111111"),
                    new FieldId("2222222222222222222")
            ));

            List<SchemaReferenceAnalyzer.ReferenceInfo> result = analyzer.analyze(testDef);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(SchemaReferenceAnalyzer.ReferenceInfo::targetId)
                    .containsExactlyInAnyOrder("1111111111111111111", "2222222222222222222");
            verifyNoInteractions(schemaRepository);
        }

        @Test
        @DisplayName("分析 String ID 集合触发批量查询")
        void analyze_stringIdCollection_triggersBatchQuery() {
            TestDefinitionWithStringCollection testDef = new TestDefinitionWithStringCollection(new CardTypeId("1234567890123456789"), "org-1", "测试");
            testDef.setRefIds(List.of("1111111111111111111", "2222222222222222222"));

            // Mock
            SingleLineTextFieldConfig field1 = new SingleLineTextFieldConfig(
                    new FieldConfigId("1111111111111111111"), "org-1", "字段1",
                    null, new FieldId("f-1"), false);
            SingleLineTextFieldConfig field2 = new SingleLineTextFieldConfig(
                    new FieldConfigId("2222222222222222222"), "org-1", "字段2",
                    null, new FieldId("f-2"), false);
            when(schemaRepository.findByIds(anySet()))
                    .thenReturn(List.of(field1, field2));

            List<SchemaReferenceAnalyzer.ReferenceInfo> result = analyzer.analyze(testDef);

            verify(schemaRepository, times(1)).findByIds(anySet());
            assertThat(result).hasSize(2);
        }
    }

    // ==================== 测试用辅助类 ====================

    /**
     * 测试用定义类：没有任何引用字段
     */
    static class TestDefinitionNoRefs extends AbstractSchemaDefinition<CardTypeId> {

        public TestDefinitionNoRefs(CardTypeId id, String orgId, String name) {
            super(id, orgId, name);
        }

        @Override
        public String getSchemaSubType() {
            return "TestDefinitionNoRefs";
        }

        @Override
        public SchemaType getSchemaType() {
            return SchemaType.CARD_TYPE;
        }

        @Override
        public SchemaId belongTo() {
            return null;
        }

        @Override
        public Set<SchemaId> secondKeys() {
            return Set.of();
        }

        @Override
        protected CardTypeId newId() {
            return CardTypeId.generate();
        }
    }

    /**
     * 测试用定义类：包含两个 String 类型的 ID 字段
     */
    static class TestDefinitionWithStringIds extends AbstractSchemaDefinition<CardTypeId> {
        private String refId1;
        private String refId2;

        public TestDefinitionWithStringIds(CardTypeId id, String orgId, String name) {
            super(id, orgId, name);
        }

        @Override
        public String getSchemaSubType() {
            return "TestDefinitionWithStringIds";
        }

        @Override
        public SchemaType getSchemaType() {
            return SchemaType.CARD_TYPE;
        }

        @Override
        public SchemaId belongTo() {
            return null;
        }

        @Override
        public Set<SchemaId> secondKeys() {
            return Set.of();
        }

        @Override
        protected CardTypeId newId() {
            return CardTypeId.generate();
        }

        public String getRefId1() { return refId1; }
        public void setRefId1(String refId1) { this.refId1 = refId1; }
        public String getRefId2() { return refId2; }
        public void setRefId2(String refId2) { this.refId2 = refId2; }
    }

    /**
     * 测试用定义类：同时包含 belongTo 的 SchemaId、非 belongTo 的 SchemaId、和 String 类型 ID
     */
    static class TestDefinitionWithMixedIds extends AbstractSchemaDefinition<ViewId> {
        private CardTypeId cardTypeId;       // belongTo
        private FieldId otherFieldId;        // 非 belongTo 的 SchemaId（聚合引用）
        private String stringRefId;

        public TestDefinitionWithMixedIds(ViewId id, String orgId, String name) {
            super(id, orgId, name);
        }

        @Override
        public String getSchemaSubType() {
            return "TestDefinitionWithMixedIds";
        }

        @Override
        public SchemaType getSchemaType() {
            return SchemaType.VIEW;
        }

        @Override
        public SchemaId belongTo() {
            return cardTypeId;
        }

        @Override
        public Set<SchemaId> secondKeys() {
            return Set.of();
        }

        @Override
        protected ViewId newId() {
            return ViewId.generate();
        }

        public CardTypeId getCardTypeId() { return cardTypeId; }
        public void setCardTypeId(CardTypeId cardTypeId) { this.cardTypeId = cardTypeId; }
        public FieldId getOtherFieldId() { return otherFieldId; }
        public void setOtherFieldId(FieldId otherFieldId) { this.otherFieldId = otherFieldId; }
        public String getStringRefId() { return stringRefId; }
        public void setStringRefId(String stringRefId) { this.stringRefId = stringRefId; }
    }

    /**
     * 测试用定义类：包含 SchemaId 集合
     */
    static class TestDefinitionWithIdCollection extends AbstractSchemaDefinition<CardTypeId> {
        private List<FieldId> fieldIds;

        public TestDefinitionWithIdCollection(CardTypeId id, String orgId, String name) {
            super(id, orgId, name);
        }

        @Override
        public String getSchemaSubType() {
            return "TestDefinitionWithIdCollection";
        }

        @Override
        public SchemaType getSchemaType() {
            return SchemaType.CARD_TYPE;
        }

        @Override
        public SchemaId belongTo() {
            return null;
        }

        @Override
        public Set<SchemaId> secondKeys() {
            return Set.of();
        }

        @Override
        protected CardTypeId newId() {
            return CardTypeId.generate();
        }

        public List<FieldId> getFieldIds() { return fieldIds; }
        public void setFieldIds(List<FieldId> fieldIds) { this.fieldIds = fieldIds; }
    }

    /**
     * 测试用定义类：包含 String ID 集合
     */
    static class TestDefinitionWithStringCollection extends AbstractSchemaDefinition<CardTypeId> {
        private List<String> refIds;

        public TestDefinitionWithStringCollection(CardTypeId id, String orgId, String name) {
            super(id, orgId, name);
        }

        @Override
        public String getSchemaSubType() {
            return "TestDefinitionWithStringCollection";
        }

        @Override
        public SchemaType getSchemaType() {
            return SchemaType.CARD_TYPE;
        }

        @Override
        public SchemaId belongTo() {
            return null;
        }

        @Override
        public Set<SchemaId> secondKeys() {
            return Set.of();
        }

        @Override
        protected CardTypeId newId() {
            return CardTypeId.generate();
        }

        public List<String> getRefIds() { return refIds; }
        public void setRefIds(List<String> refIds) { this.refIds = refIds; }
    }
}
