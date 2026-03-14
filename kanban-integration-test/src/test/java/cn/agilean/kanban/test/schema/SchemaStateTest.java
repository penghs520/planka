package cn.agilean.kanban.test.schema;

import cn.agilean.kanban.api.schema.request.CreateSchemaRequest;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.schema.definition.SchemaDefinition;
import cn.agilean.kanban.test.support.BaseIntegrationTest;
import cn.agilean.kanban.test.support.TestDataBuilder;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema 状态变更测试
 * <p>
 * 测试 Schema 的启用、停用状态变更功能
 */
@DisplayName("Schema 状态变更测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaStateTest extends BaseIntegrationTest {

    private String testSchemaId;

    @BeforeAll
    void setup() {
        // 创建测试 Schema
        String fieldName = TestDataBuilder.uniqueName("状态测试字段");
        CreateSchemaRequest request = TestDataBuilder.createTextFieldRequest(fieldName);
        Result<SchemaDefinition> result = schemaClient.create(TEST_ORG_ID, request);
        if (result.isSuccess() && result.getData() != null) {
            testSchemaId = result.getData().getId().value();
        }
    }

    @AfterAll
    void cleanup() {
        if (testSchemaId != null) {
            schemaClient.delete(testSchemaId);
        }
    }

    @Test
    @Order(1)
    @DisplayName("新创建的 Schema 默认状态为 ACTIVE")
    void testInitialState() {
        // Given
        assertThat(testSchemaId).isNotBlank();

        // When
        Result<SchemaDefinition> result = schemaClient.getById(testSchemaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getState().name()).isEqualTo("ACTIVE");
    }

    @Test
    @Order(2)
    @DisplayName("启用 Schema")
    void testActivateSchema() {
        // Given
        assertThat(testSchemaId).isNotBlank();

        // When
        Result<Void> result = schemaClient.activate(testSchemaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();

        // 验证状态已变更
        Result<SchemaDefinition> getResult = schemaClient.getById(testSchemaId);
        assertThat(getResult.getData().getState().name()).isEqualTo("ACTIVE");
    }

    @Test
    @Order(3)
    @DisplayName("停用 Schema")
    void testDisableSchema() {
        // Given
        assertThat(testSchemaId).isNotBlank();

        // When
        Result<Void> result = schemaClient.disable(testSchemaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();

        // 验证状态已变更
        Result<SchemaDefinition> getResult = schemaClient.getById(testSchemaId);
        assertThat(getResult.getData().getState().name()).isEqualTo("DISABLED");
    }

    @Test
    @Order(4)
    @DisplayName("重新启用 Schema")
    void testReactivateSchema() {
        // Given
        assertThat(testSchemaId).isNotBlank();

        // When
        Result<Void> result = schemaClient.activate(testSchemaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();

        // 验证状态已变更
        Result<SchemaDefinition> getResult = schemaClient.getById(testSchemaId);
        assertThat(getResult.getData().getState().name()).isEqualTo("ACTIVE");
    }

    @Test
    @Order(5)
    @DisplayName("启用不存在的 Schema - 失败")
    void testActivateNonExistentSchema() {
        // Given
        String nonExistentId = "non-existent-schema-id";

        // When
        Result<Void> result = schemaClient.activate(nonExistentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @Order(6)
    @DisplayName("停用不存在的 Schema - 失败")
    void testDisableNonExistentSchema() {
        // Given
        String nonExistentId = "non-existent-schema-id";

        // When
        Result<Void> result = schemaClient.disable(nonExistentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
    }
}
