package cn.agilean.kanban.test.schema;

import cn.agilean.kanban.api.schema.request.CreateSchemaRequest;
import cn.agilean.kanban.common.result.PageResult;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.schema.definition.SchemaDefinition;
import cn.agilean.kanban.test.support.BaseIntegrationTest;
import cn.agilean.kanban.test.support.TestDataBuilder;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema 查询测试
 * <p>
 * 测试 Schema 的各种查询功能：分页查询、按二级键查询、按所属关系查询
 */
@DisplayName("Schema 查询测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaQueryTest extends BaseIntegrationTest {

    private final List<String> createdSchemaIds = new ArrayList<>();

    @BeforeAll
    void setupTestData() {
        // 创建测试数据
        for (int i = 0; i < 5; i++) {
            String fieldName = TestDataBuilder.uniqueName("查询测试字段" + i);
            CreateSchemaRequest request = TestDataBuilder.createTextFieldRequest(fieldName);
            Result<SchemaDefinition> result = schemaClient.create(TEST_ORG_ID, request);
            if (result.isSuccess() && result.getData() != null) {
                createdSchemaIds.add(result.getData().getId().value());
            }
        }
    }

    @AfterAll
    void cleanupTestData() {
        // 清理测试数据
        for (String id : createdSchemaIds) {
            schemaClient.delete(id);
        }
        createdSchemaIds.clear();
    }

    @Test
    @Order(1)
    @DisplayName("按组织和类型分页查询 - 第一页")
    void testListByOrgAndType_FirstPage() {
        // When
        Result<PageResult<SchemaDefinition>> result = schemaClient.listByOrgAndType(
                TEST_ORG_ID, "CUSTOMIZED_FIELD", 1, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();

        PageResult<SchemaDefinition> pageResult = result.getData();
        assertThat(pageResult.getContent()).isNotEmpty();
        assertThat(pageResult.getPage()).isGreaterThanOrEqualTo(0);
        assertThat(pageResult.getSize()).isEqualTo(10);
    }

    @Test
    @Order(2)
    @DisplayName("按组织和类型分页查询 - 指定页大小")
    void testListByOrgAndType_CustomPageSize() {
        // When
        Result<PageResult<SchemaDefinition>> result = schemaClient.listByOrgAndType(
                TEST_ORG_ID, "CUSTOMIZED_FIELD", 1, 3);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getSize()).isEqualTo(3);
    }

    @Test
    @Order(3)
    @DisplayName("按组织和类型分页查询 - 空结果类型")
    void testListByOrgAndType_NoResults() {
        // Given - 查询不存在的类型
        String nonExistentType = "NON_EXISTENT_TYPE";

        // When
        Result<PageResult<SchemaDefinition>> result = schemaClient.listByOrgAndType(
                TEST_ORG_ID, nonExistentType, 1, 10);

        // Then - 无效类型应该返回失败
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @Order(4)
    @DisplayName("批量查询 - 多个ID")
    void testGetByIds_MultipleIds() {
        // Given
        assertThat(createdSchemaIds).hasSizeGreaterThanOrEqualTo(3);
        List<String> ids = createdSchemaIds.subList(0, 3);

        // When
        Result<List<SchemaDefinition>> result = schemaClient.getByIds(ids);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData()).hasSize(3);
    }

    @Test
    @Order(5)
    @DisplayName("批量查询 - 空列表")
    void testGetByIds_EmptyList() {
        // When
        Result<List<SchemaDefinition>> result = schemaClient.getByIds(List.of());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @Order(6)
    @DisplayName("批量查询 - 包含不存在的ID")
    void testGetByIds_WithNonExistentId() {
        // Given
        assertThat(createdSchemaIds).isNotEmpty();
        List<String> ids = new ArrayList<>();
        ids.add(createdSchemaIds.get(0));
        ids.add("non-existent-id");

        // When
        Result<List<SchemaDefinition>> result = schemaClient.getByIds(ids);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        // 只返回存在的 Schema
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    @Order(7)
    @DisplayName("按所属 Schema 查询 - 无从属")
    void testGetByBelongTo_NoChildren() {
        // Given
        assertThat(createdSchemaIds).isNotEmpty();
        String schemaId = createdSchemaIds.get(0);

        // When
        Result<List<SchemaDefinition>> result = schemaClient.getByBelongTo(schemaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }
}
