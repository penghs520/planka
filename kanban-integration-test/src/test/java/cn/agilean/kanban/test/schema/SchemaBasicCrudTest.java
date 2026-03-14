package cn.agilean.kanban.test.schema;

import cn.agilean.kanban.api.schema.request.CreateSchemaRequest;
import cn.agilean.kanban.api.schema.request.UpdateSchemaRequest;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.schema.definition.SchemaDefinition;
import cn.agilean.kanban.domain.schema.definition.fieldconfig.MultiLineTextFieldConfig;
import cn.agilean.kanban.domain.schema.definition.fieldconfig.SingleLineTextFieldConfig;
import cn.agilean.kanban.test.support.BaseIntegrationTest;
import cn.agilean.kanban.test.support.TestDataBuilder;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema 基础 CRUD 测试
 * <p>
 * 测试 Schema 的创建、读取、更新、删除功能
 */
@DisplayName("Schema 基础 CRUD 测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SchemaBasicCrudTest extends BaseIntegrationTest {

    private static String createdSchemaId;

    @Test
    @Order(1)
    @DisplayName("创建 Schema - 文本字段定义")
    void testCreateSchema() {
        // Given
        String fieldName = TestDataBuilder.uniqueName("测试字段");
        CreateSchemaRequest request = TestDataBuilder.createTextFieldRequest(fieldName);

        // When
        Result<SchemaDefinition> result = schemaClient.create(TEST_ORG_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();

        SchemaDefinition schema = result.getData();
        assertThat(schema.getId().value()).isNotBlank();
        assertThat(schema.getOrgId()).isEqualTo(TEST_ORG_ID);
        assertThat(schema.getName()).isEqualTo(fieldName);
        assertThat(schema.getSchemaType().name()).isEqualTo("CUSTOMIZED_FIELD");

        // 保存 ID 供后续测试使用
        createdSchemaId = schema.getId().value();
    }

    @Test
    @Order(2)
    @DisplayName("根据 ID 查询 Schema")
    void testGetById() {
        // Given
        assertThat(createdSchemaId).isNotBlank();

        // When
        Result<SchemaDefinition> result = schemaClient.getById(createdSchemaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getId().value()).isEqualTo(createdSchemaId);
    }

    @Test
    @Order(3)
    @DisplayName("批量查询 Schema")
    void testGetByIds() {
        // Given
        assertThat(createdSchemaId).isNotBlank();
        List<String> ids = List.of(createdSchemaId);

        // When
        Result<List<SchemaDefinition>> result = schemaClient.getByIds(ids);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getId().value()).isEqualTo(createdSchemaId);
    }

    @Test
    @Order(4)
    @DisplayName("更新 Schema")
    void testUpdateSchema() {
        // Given
        assertThat(createdSchemaId).isNotBlank();

        String updatedName = TestDataBuilder.uniqueName("更新后字段");
        SingleLineTextFieldConfig definition = TestDataBuilder.createSingleLineTextFieldConfig(updatedName);
        definition.setMaxLength(1000);  // 修改最大长度

        UpdateSchemaRequest request = TestDataBuilder.createUpdateRequest(definition, 1);

        // When
        Result<SchemaDefinition> result = schemaClient.update(createdSchemaId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();

        SchemaDefinition schema = result.getData();
        assertThat(schema.getId().value()).isEqualTo(createdSchemaId);
        assertThat(schema.getName()).isEqualTo(updatedName);
    }

    @Test
    @Order(5)
    @DisplayName("查询不存在的 Schema")
    void testGetByIdNotFound() {
        // Given
        String nonExistentId = "non-existent-schema-id";

        // When
        Result<SchemaDefinition> result = schemaClient.getById(nonExistentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @Order(6)
    @DisplayName("删除 Schema")
    void testDeleteSchema() {
        // Given
        assertThat(createdSchemaId).isNotBlank();

        // When
        Result<Void> result = schemaClient.delete(createdSchemaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();

        // 验证已删除
        Result<SchemaDefinition> getResult = schemaClient.getById(createdSchemaId);
        assertThat(getResult.isSuccess()).isFalse();
    }

    @Test
    @Order(7)
    @DisplayName("创建多行文本字段 Schema")
    void testCreateTextAreaSchema() {
        // Given
        String fieldName = TestDataBuilder.uniqueName("多行文本");
        MultiLineTextFieldConfig definition = TestDataBuilder.createMultiLineTextFieldConfig(fieldName);
        CreateSchemaRequest request = TestDataBuilder.createSchemaRequest(definition);

        // When
        Result<SchemaDefinition> result = schemaClient.create(TEST_ORG_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getName()).isEqualTo(fieldName);

        // 清理测试数据
        schemaClient.delete(result.getData().getId().value());
    }
}
