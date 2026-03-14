package cn.agilean.kanban.test.card;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.api.card.request.*;
import cn.agilean.kanban.common.result.PageResult;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.test.support.BaseIntegrationTest;
import cn.agilean.kanban.test.support.TestDataBuilder;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Card 服务集成测试
 * <p>
 * 使用 OpenFeign + Nacos 服务发现测试 Card 服务接口
 */
@DisplayName("Card 服务集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CardServiceIntegrationTest extends BaseIntegrationTest {

    private static CardId createdCardId;

    @Test
    @Order(1)
    @DisplayName("创建卡片")
    void testCreateCard() {
        // Given
        String titleValue = TestDataBuilder.uniqueName("测试卡片");
        CreateCardRequest request = TestDataBuilder.createCardRequest(titleValue);

        // When
        Result<CardId> result = cardServiceClient.create(TEST_OPERATOR_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();

        Result<CardDTO> dtoResult = cardServiceClient.findById(result.getData().asStr(), TEST_OPERATOR_ID, new Yield());

        // Then
        assertThat(dtoResult).isNotNull();
        assertThat(dtoResult.isSuccess()).isTrue();
        assertThat(dtoResult.getData()).isNotNull();

        CardDTO card = dtoResult.getData();
        assertThat(card.getId()).isNotNull();
        assertThat(card.getTitle()).isNotNull();
        assertThat(card.getTitle().getDisplayValue()).isEqualTo(titleValue);

        // 保存 ID
        createdCardId = card.getId();
    }

    @Test
    @Order(2)
    @DisplayName("根据ID查询卡片")
    void testFindById() {
        // Given
        assertThat(createdCardId).isNotNull();

        // When
        Result<CardDTO> result = cardServiceClient.findById(createdCardId.asStr(), TEST_OPERATOR_ID, Yield.all());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getId()).isEqualTo(createdCardId);
    }

    @Test
    @Order(3)
    @DisplayName("分页查询卡片")
    void testPageQuery() {
        // Given
        CardPageQueryRequest request = TestDataBuilder.createPageQueryRequest(1, 10);

        // When
        Result<PageResult<CardDTO>> result = cardServiceClient.pageQuery(TEST_OPERATOR_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getContent()).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("查询卡片列表")
    void testQuery() {
        // Given
        CardQueryRequest request = TestDataBuilder.createQueryRequest();

        // When
        Result<List<CardDTO>> result = cardServiceClient.query(TEST_OPERATOR_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("统计卡片数量")
    void testCount() {
        // Given
        CardCountRequest request = TestDataBuilder.createCountRequest();

        // When
        Result<Integer> result = cardServiceClient.count(TEST_OPERATOR_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Order(6)
    @DisplayName("根据多个ID查询卡片")
    void testFindByIds() {
        // Given
        assertThat(createdCardId).isNotNull();
        FindByIdsRequest request = new FindByIdsRequest();
        request.setCardIds(List.of(createdCardId));
        request.setYield(new Yield());
        request.setOperatorId(TEST_OPERATOR_ID);

        // When
        Result<List<CardDTO>> result = cardServiceClient.findByIds(TEST_OPERATOR_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getId()).isEqualTo(createdCardId);
    }

    @Test
    @Order(7)
    @DisplayName("归档卡片")
    void testArchiveCard() {
        // Given
        assertThat(createdCardId).isNotNull();
        BatchOperationRequest request = new BatchOperationRequest();
        request.setCardIds(List.of(createdCardId));

        // When
        Result<Void> result = cardServiceClient.batchArchive(TEST_OPERATOR_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("还原卡片")
    void testRestoreCard() {
        // Given
        assertThat(createdCardId).isNotNull();
        BatchOperationRequest request = new BatchOperationRequest();
        request.setCardIds(List.of(createdCardId));

        // When
        Result<Void> result = cardServiceClient.batchRestore(TEST_OPERATOR_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("丢弃卡片")
    void testDiscardCard() {
        // Given
        assertThat(createdCardId).isNotNull();

        // When
        Result<Void> result = cardServiceClient.discard(createdCardId.asStr(), TEST_OPERATOR_ID, "集成测试丢弃");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }
}
