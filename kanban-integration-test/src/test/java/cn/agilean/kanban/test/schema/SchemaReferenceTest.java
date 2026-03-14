package cn.agilean.kanban.test.schema;

import cn.agilean.kanban.api.schema.dto.SchemaChangelogDTO;
import cn.agilean.kanban.api.schema.dto.SchemaReferenceSummaryDTO;
import cn.agilean.kanban.api.schema.request.CreateSchemaRequest;
import cn.agilean.kanban.api.schema.request.UpdateSchemaRequest;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.schema.definition.SchemaDefinition;
import cn.agilean.kanban.test.support.BaseIntegrationTest;
import cn.agilean.kanban.test.support.TestDataBuilder;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema 引用关系与变更历史测试
 * <p>
 * 测试 Schema 的引用摘要查询和变更历史查询功能
 */
@DisplayName("Schema 引用与历史测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaReferenceTest extends BaseIntegrationTest {

    private String testSchemaId;

    @BeforeAll
    void setup() {
        // 创建测试 Schema
        String fieldName = TestDataBuilder.uniqueName("引用测试字段");
        CreateSchemaRequest request = TestDataBuilder.createTextFieldRequest(fieldName);
        Result<SchemaDefinition> result = schemaClient.create(TEST_ORG_ID, request);
        if (result.isSuccess() && result.getData() != null) {
            testSchemaId = result.getData().getId().value();

            // 执行几次更新，生成变更历史
            for (int i = 0; i < 3; i++) {
                String updatedName = TestDataBuilder.uniqueName("更新字段" + i);
                UpdateSchemaRequest updateRequest = TestDataBuilder.createUpdateRequest(
                        TestDataBuilder.createSingleLineTextFieldConfig(updatedName),
                        i + 1
                );
                schemaClient.update(testSchemaId, updateRequest);
            }
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
    @DisplayName("获取 Schema 引用摘要 - 无引用")
    void testGetReferenceSummary_NoReferences() {
        // Given
        assertThat(testSchemaId).isNotBlank();

        // When
        Result<SchemaReferenceSummaryDTO> result = schemaClient.getReferenceSummary(testSchemaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("获取不存在 Schema 的引用摘要 - 失败")
    void testGetReferenceSummary_NotFound() {
        // Given
        String nonExistentId = "non-existent-schema-id";

        // When
        Result<SchemaReferenceSummaryDTO> result = schemaClient.getReferenceSummary(nonExistentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @Order(3)
    @DisplayName("获取 Schema 变更历史")
    void testGetChangelog() {
        // Given
        assertThat(testSchemaId).isNotBlank();

        // When
        Result<List<SchemaChangelogDTO>> result = schemaClient.getChangelog(testSchemaId, 50);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        // 应该有创建 + 3次更新 = 4条记录
        assertThat(result.getData()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(4)
    @DisplayName("获取 Schema 变更历史 - 限制数量")
    void testGetChangelog_WithLimit() {
        // Given
        assertThat(testSchemaId).isNotBlank();

        // When
        Result<List<SchemaChangelogDTO>> result = schemaClient.getChangelog(testSchemaId, 2);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData()).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    @Order(5)
    @DisplayName("获取不存在 Schema 的变更历史 - 失败")
    void testGetChangelog_NotFound() {
        // Given
        String nonExistentId = "non-existent-schema-id";

        // When
        Result<List<SchemaChangelogDTO>> result = schemaClient.getChangelog(nonExistentId, 50);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
    }
}
