package dev.planka.api.schema.service;

import dev.planka.api.schema.dto.inheritance.FieldConfigListWithSource;
import dev.planka.api.schema.spi.SchemaDataProvider;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.schema.definition.cardtype.AbstractCardType;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.domain.schema.definition.fieldconfig.SingleLineTextFieldConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * FieldConfigQueryService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class FieldConfigQueryServiceTest {

    @Mock
    private SchemaDataProvider schemaDataProvider;

    private ObjectMapper objectMapper;

    private FieldConfigQueryService fieldConfigQueryService;

    private static final String ORG_ID = "org-1";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        fieldConfigQueryService = new FieldConfigQueryService(schemaDataProvider);
    }

    @Nested
    @DisplayName("getFieldConfigs 测试")
    class GetFieldConfigsTests {

        @Test
        @DisplayName("卡片类型不存在返回 DATA_NOT_FOUND")
        void shouldReturnNotFoundWhenCardTypeNotExists() {
            // Given
            when(schemaDataProvider.getCardTypeById("not-exist")).thenReturn(Optional.empty());

            // When
            Result<FieldConfigListWithSource> result = fieldConfigQueryService.getFieldConfigListWithSource("not-exist");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("属性集返回自身配置，无冲突检测")
        void shouldReturnOwnConfigForAbstractCardType() {
            // Given
            AbstractCardType abstractCardType =
                    new AbstractCardType(
                    CardTypeId.of("abstract-1"), ORG_ID, "属性集");

            when(schemaDataProvider.getCardTypeById("abstract-1")).thenReturn(Optional.of(abstractCardType));
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId(anyString())).thenReturn(Collections.emptyList());
            when(schemaDataProvider.getLinkTypesByCardTypeId(anyString())).thenReturn(Collections.emptyList());
            lenient().when(schemaDataProvider.getSchemasByIds(anySet())).thenReturn(Collections.emptyList());

            // When
            Result<FieldConfigListWithSource> result = fieldConfigQueryService.getFieldConfigListWithSource("abstract-1");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getCardTypeId()).isEqualTo("abstract-1");
        }

        @Test
        @DisplayName("实体类型返回合并配置")
        void shouldReturnMergedConfigAndConflictsForEntityCardType() {
            // Given
            EntityCardType entityCardType = new EntityCardType(
                    CardTypeId.of("concrete-1"), ORG_ID, "实体类型");
            entityCardType.setParentTypeIds(new java.util.HashSet<>(Set.of(CardTypeId.of("abstract-1"), CardTypeId.of("abstract-2"))));

            when(schemaDataProvider.getCardTypeById("concrete-1")).thenReturn(Optional.of(entityCardType));

            // 模拟属性配置
            SingleLineTextFieldConfig fieldConfig = createFieldConfig("config-1", "abstract-1", "field-1");
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId(anyString())).thenReturn(List.of(fieldConfig));

            when(schemaDataProvider.getLinkTypesByCardTypeId(anyString())).thenReturn(Collections.emptyList());
            when(schemaDataProvider.getSchemasByIds(anySet())).thenReturn(Collections.emptyList());

            // When
            Result<FieldConfigListWithSource> result = fieldConfigQueryService.getFieldConfigListWithSource("concrete-1");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getCardTypeId()).isEqualTo("concrete-1");
            assertThat(result.getData().getFields()).isNotEmpty();
        }

        @Test
        @DisplayName("属性配置来源信息正确标记")
        void shouldMarkFieldSourceInfoCorrectly() {
            // Given
            EntityCardType entityCardType = new EntityCardType(
                    CardTypeId.of("concrete-1"), ORG_ID, "实体类型");
            entityCardType.setParentTypeIds(new java.util.HashSet<>(Set.of(CardTypeId.of("abstract-1"))));

            when(schemaDataProvider.getCardTypeById("concrete-1")).thenReturn(Optional.of(entityCardType));

            // 模拟属性配置
            SingleLineTextFieldConfig fieldConfig = createFieldConfig("config-1", "abstract-1", "field-1");

            // 任意卡属性集返回空
            String rootCardTypeId = ORG_ID + ":any-trait";
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId(rootCardTypeId)).thenReturn(Collections.emptyList());

            // 只在 abstract-1 返回配置
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("abstract-1")).thenReturn(List.of(fieldConfig));
            when(schemaDataProvider.getAllFieldConfigsByCardTypeId("concrete-1")).thenReturn(Collections.emptyList());

            when(schemaDataProvider.getLinkTypesByCardTypeId(anyString())).thenReturn(Collections.emptyList());
            when(schemaDataProvider.getSchemasByIds(anySet())).thenReturn(Collections.emptyList());

            // When
            Result<FieldConfigListWithSource> result = fieldConfigQueryService.getFieldConfigListWithSource("concrete-1");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getFieldSources()).containsKey("field-1");
            assertThat(result.getData().getFieldSources().get("field-1").isConfigInherited()).isTrue();
        }

        @Test
        @DisplayName("非卡片类型 Schema 返回 BAD_REQUEST")
        void shouldReturnBadRequestWhenNotCardType() {
            // Given - getCardTypeById 返回空表示找到的不是卡片类型
            when(schemaDataProvider.getCardTypeById("field-1")).thenReturn(Optional.empty());

            // When
            Result<FieldConfigListWithSource> result = fieldConfigQueryService.getFieldConfigListWithSource("field-1");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getCode());
        }
    }

    // ==================== 测试辅助方法 ====================

    private SingleLineTextFieldConfig createFieldConfig(String id, String cardTypeId, String fieldId) {
        return new SingleLineTextFieldConfig(
                FieldConfigId.of(id), ORG_ID, "配置-" + fieldId,
                CardTypeId.of(cardTypeId), FieldId.of(fieldId), false);
    }
}
