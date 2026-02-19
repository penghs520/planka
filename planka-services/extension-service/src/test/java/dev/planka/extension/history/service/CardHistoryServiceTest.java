package dev.planka.extension.history.service;

import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.domain.history.CardHistoryId;
import dev.planka.domain.history.CardHistoryRecord;
import dev.planka.domain.history.OperationType;
import dev.planka.extension.history.dto.CardHistoryRecordVO;
import dev.planka.extension.history.repository.CardHistoryRepository;
import dev.planka.extension.history.service.CardHistoryService.CardHistoryFilters;
import dev.planka.extension.history.service.CardHistoryService.RecordHistoryCommand;
import dev.planka.extension.history.service.CardHistoryService.SearchHistoryQuery;
import dev.planka.extension.history.vo.HistoryMessageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.planka.extension.history.support.HistoryTestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CardHistoryService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CardHistoryServiceTest {

    @Mock
    private CardHistoryTableManager tableManager;

    @Mock
    private CardHistoryRepository historyRepository;

    @Mock
    private MemberNameResolver memberNameResolver;

    @Mock
    private HistoryArgumentResolver argumentResolver;

    private CardHistoryService cardHistoryService;

    private static final String TABLE_NAME = "card_history_" + TEST_CARD_TYPE_ID;

    @BeforeEach
    void setUp() {
        cardHistoryService = new CardHistoryService(
                tableManager, historyRepository, memberNameResolver, argumentResolver);
    }

    // ==================== recordHistory 测试 ====================

    @Nested
    @DisplayName("recordHistory - 记录操作历史")
    class RecordHistoryTests {

        @Test
        @DisplayName("成功记录历史返回 CardHistoryId")
        void shouldReturnHistoryIdWhenRecordSuccess() {
            // Given
            RecordHistoryCommand command = createCommand();
            CardHistoryId expectedId = new CardHistoryId("history-001");

            when(tableManager.getOrCreateTable(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.save(eq(TABLE_NAME), any(CardHistoryRecord.class))).thenReturn(expectedId);

            // When
            Result<CardHistoryId> result = cardHistoryService.recordHistory(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(expectedId);

            verify(tableManager).getOrCreateTable(TEST_CARD_TYPE_ID);
            verify(historyRepository).save(eq(TABLE_NAME), any(CardHistoryRecord.class));
        }

        @Test
        @DisplayName("TableManager 抛出异常时返回失败结果")
        void shouldReturnFailureWhenTableManagerThrows() {
            // Given
            RecordHistoryCommand command = createCommand();
            when(tableManager.getOrCreateTable(anyString()))
                    .thenThrow(new RuntimeException("数据库连接失败"));

            // When
            Result<CardHistoryId> result = cardHistoryService.recordHistory(command);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo("HISTORY_RECORD_FAILED");
            assertThat(result.getMessage()).contains("数据库连接失败");
        }

        @Test
        @DisplayName("Repository 保存失败时返回失败结果")
        void shouldReturnFailureWhenSaveFails() {
            // Given
            RecordHistoryCommand command = createCommand();
            when(tableManager.getOrCreateTable(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.save(eq(TABLE_NAME), any(CardHistoryRecord.class)))
                    .thenThrow(new RuntimeException("保存失败"));

            // When
            Result<CardHistoryId> result = cardHistoryService.recordHistory(command);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo("HISTORY_RECORD_FAILED");
        }

        @Test
        @DisplayName("不同操作类型正确传递给 Repository")
        void shouldPassOperationTypeToRepository() {
            // Given
            RecordHistoryCommand command = createCommand(OperationType.CARD_ARCHIVED);
            CardHistoryId expectedId = new CardHistoryId("history-002");

            when(tableManager.getOrCreateTable(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.save(eq(TABLE_NAME), argThat(record ->
                    record.operationType() == OperationType.CARD_ARCHIVED
            ))).thenReturn(expectedId);

            // When
            Result<CardHistoryId> result = cardHistoryService.recordHistory(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
        }
    }

    // ==================== getCardHistory 测试 ====================

    @Nested
    @DisplayName("getCardHistory - 分页查询")
    class GetCardHistoryTests {

        @Test
        @DisplayName("表不存在时返回空分页结果")
        void shouldReturnEmptyPageWhenTableNotExists() {
            // Given
            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(false);

            // When
            Result<PageResult<CardHistoryRecordVO>> result =
                    cardHistoryService.getCardHistory(TEST_CARD_TYPE_ID, TEST_CARD_ID, 1, 10);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getContent()).isEmpty();
            assertThat(result.getData().getTotal()).isEqualTo(0);

            verify(historyRepository, never()).findByCardId(anyString(), anyLong(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("正常查询返回转换后的 VO 列表")
        void shouldReturnConvertedVOList() {
            // Given
            CardHistoryRecord record = createRecord();
            List<CardHistoryRecord> records = List.of(record);
            HistoryMessageVO messageVO = HistoryMessageVO.builder()
                    .messageKey("history.test")
                    .args(List.of())
                    .build();

            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(true);
            when(tableManager.getTableName(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.findByCardId(TABLE_NAME, TEST_CARD_ID, 1, 10)).thenReturn(records);
            when(historyRepository.countByCardId(TABLE_NAME, TEST_CARD_ID)).thenReturn(1L);
            when(memberNameResolver.resolveNames(anySet())).thenReturn(Map.of(TEST_OPERATOR_ID, "张三"));
            when(memberNameResolver.getName(TEST_OPERATOR_ID, Map.of(TEST_OPERATOR_ID, "张三"))).thenReturn("张三");
            when(argumentResolver.resolveBatch(anyList(), eq(TEST_CARD_TYPE_ID))).thenReturn(List.of(messageVO));

            // When
            Result<PageResult<CardHistoryRecordVO>> result =
                    cardHistoryService.getCardHistory(TEST_CARD_TYPE_ID, TEST_CARD_ID, 1, 10);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getContent()).hasSize(1);
            assertThat(result.getData().getContent().get(0).getOperatorName()).isEqualTo("张三");
            assertThat(result.getData().getTotal()).isEqualTo(1);
        }

        @Test
        @DisplayName("批量解析操作人名称")
        void shouldBatchResolveOperatorNames() {
            // Given
            CardHistoryRecord record1 = createRecordWithId("h-1", "operator-1");
            CardHistoryRecord record2 = createRecordWithId("h-2", "operator-2");
            List<CardHistoryRecord> records = List.of(record1, record2);

            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(true);
            when(tableManager.getTableName(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.findByCardId(TABLE_NAME, TEST_CARD_ID, 1, 10)).thenReturn(records);
            when(historyRepository.countByCardId(TABLE_NAME, TEST_CARD_ID)).thenReturn(2L);
            when(argumentResolver.resolveBatch(anyList(), eq(TEST_CARD_TYPE_ID)))
                    .thenReturn(List.of(
                            HistoryMessageVO.builder().messageKey("test").args(List.of()).build(),
                            HistoryMessageVO.builder().messageKey("test").args(List.of()).build()
                    ));

            // When
            cardHistoryService.getCardHistory(TEST_CARD_TYPE_ID, TEST_CARD_ID, 1, 10);

            // Then - 验证批量查询被调用，且包含两个操作人ID
            verify(memberNameResolver).resolveNames(argThat((Set<String> ids) ->
                    ids.contains("operator-1") && ids.contains("operator-2")
            ));
        }

        @Test
        @DisplayName("空记录列表返回空 VO 列表")
        void shouldReturnEmptyListWhenNoRecords() {
            // Given
            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(true);
            when(tableManager.getTableName(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.findByCardId(TABLE_NAME, TEST_CARD_ID, 1, 10)).thenReturn(List.of());
            when(historyRepository.countByCardId(TABLE_NAME, TEST_CARD_ID)).thenReturn(0L);

            // When
            Result<PageResult<CardHistoryRecordVO>> result =
                    cardHistoryService.getCardHistory(TEST_CARD_TYPE_ID, TEST_CARD_ID, 1, 10);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getContent()).isEmpty();

            verify(memberNameResolver, never()).resolveNames(anySet());
        }

        @Test
        @DisplayName("Repository 抛异常时返回失败结果")
        void shouldReturnFailureWhenRepositoryThrows() {
            // Given
            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(true);
            when(tableManager.getTableName(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.findByCardId(TABLE_NAME, TEST_CARD_ID, 1, 10))
                    .thenThrow(new RuntimeException("数据库查询失败"));

            // When
            Result<PageResult<CardHistoryRecordVO>> result =
                    cardHistoryService.getCardHistory(TEST_CARD_TYPE_ID, TEST_CARD_ID, 1, 10);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo("HISTORY_QUERY_FAILED");
        }
    }

    // ==================== searchCardHistory 测试 ====================

    @Nested
    @DisplayName("searchCardHistory - 多条件搜索")
    class SearchCardHistoryTests {

        @Test
        @DisplayName("表不存在时返回空分页结果")
        void shouldReturnEmptyPageWhenTableNotExists() {
            // Given
            SearchHistoryQuery query = createSearchQuery();
            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(false);

            // When
            Result<PageResult<CardHistoryRecordVO>> result =
                    cardHistoryService.searchCardHistory(query);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getContent()).isEmpty();
        }

        @Test
        @DisplayName("按操作类型筛选")
        void shouldFilterByOperationTypes() {
            // Given
            List<OperationType> types = List.of(OperationType.CARD_CREATED, OperationType.CARD_ARCHIVED);
            SearchHistoryQuery query = createSearchQuery(types, null, null, null);

            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(true);
            when(tableManager.getTableName(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.search(eq(TABLE_NAME), eq(TEST_CARD_ID), eq(types),
                    isNull(), isNull(), isNull(), isNull(), eq(false), eq(1), eq(10)))
                    .thenReturn(List.of());
            when(historyRepository.countBySearch(eq(TABLE_NAME), eq(TEST_CARD_ID), eq(types),
                    isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(0L);

            // When
            cardHistoryService.searchCardHistory(query);

            // Then
            verify(historyRepository).search(eq(TABLE_NAME), eq(TEST_CARD_ID), eq(types),
                    isNull(), isNull(), isNull(), isNull(), eq(false), eq(1), eq(10));
        }

        @Test
        @DisplayName("按操作人筛选")
        void shouldFilterByOperatorIds() {
            // Given
            List<String> operatorIds = List.of("operator-1", "operator-2");
            SearchHistoryQuery query = createSearchQuery(null, operatorIds, null, null);

            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(true);
            when(tableManager.getTableName(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.search(eq(TABLE_NAME), eq(TEST_CARD_ID), isNull(),
                    eq(operatorIds), isNull(), isNull(), isNull(), eq(false), eq(1), eq(10)))
                    .thenReturn(List.of());
            when(historyRepository.countBySearch(anyString(), anyLong(), any(), any(), any(), any(), any()))
                    .thenReturn(0L);

            // When
            cardHistoryService.searchCardHistory(query);

            // Then
            verify(historyRepository).search(eq(TABLE_NAME), eq(TEST_CARD_ID), isNull(),
                    eq(operatorIds), isNull(), isNull(), isNull(), eq(false), eq(1), eq(10));
        }

        @Test
        @DisplayName("按时间范围筛选")
        void shouldFilterByTimeRange() {
            // Given
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 12, 31, 23, 59);
            SearchHistoryQuery query = createSearchQuery(null, null, startTime, endTime);

            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(true);
            when(tableManager.getTableName(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.search(eq(TABLE_NAME), eq(TEST_CARD_ID), isNull(),
                    isNull(), isNull(), eq(startTime), eq(endTime), eq(false), eq(1), eq(10)))
                    .thenReturn(List.of());
            when(historyRepository.countBySearch(anyString(), anyLong(), any(), any(), any(), any(), any()))
                    .thenReturn(0L);

            // When
            cardHistoryService.searchCardHistory(query);

            // Then
            verify(historyRepository).search(eq(TABLE_NAME), eq(TEST_CARD_ID), isNull(),
                    isNull(), isNull(), eq(startTime), eq(endTime), eq(false), eq(1), eq(10));
        }

        @Test
        @DisplayName("正序排序参数正确传递")
        void shouldPassSortAscParameter() {
            // Given
            SearchHistoryQuery query = new SearchHistoryQuery(
                    TEST_CARD_TYPE_ID, TEST_CARD_ID,
                    null, null, null, null, null,
                    true, // sortAsc = true
                    1, 10
            );

            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(true);
            when(tableManager.getTableName(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.search(eq(TABLE_NAME), eq(TEST_CARD_ID), isNull(),
                    isNull(), isNull(), isNull(), isNull(), eq(true), eq(1), eq(10)))
                    .thenReturn(List.of());
            when(historyRepository.countBySearch(anyString(), anyLong(), any(), any(), any(), any(), any()))
                    .thenReturn(0L);

            // When
            cardHistoryService.searchCardHistory(query);

            // Then
            verify(historyRepository).search(eq(TABLE_NAME), eq(TEST_CARD_ID), isNull(),
                    isNull(), isNull(), isNull(), isNull(), eq(true), eq(1), eq(10));
        }
    }

    // ==================== getAvailableFilters 测试 ====================

    @Nested
    @DisplayName("getAvailableFilters - 获取筛选选项")
    class GetAvailableFiltersTests {

        @Test
        @DisplayName("表不存在时返回空筛选选项")
        void shouldReturnEmptyFiltersWhenTableNotExists() {
            // Given
            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(false);

            // When
            Result<CardHistoryFilters> result =
                    cardHistoryService.getAvailableFilters(TEST_CARD_TYPE_ID, TEST_CARD_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().operatorIds()).isEmpty();
            assertThat(result.getData().operationTypes()).isEmpty();
            assertThat(result.getData().sourceTypes()).isEmpty();
        }

        @Test
        @DisplayName("返回去重后的操作人列表")
        void shouldReturnDistinctOperatorIds() {
            // Given
            List<String> operatorIds = List.of("op-1", "op-2", "op-3");

            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(true);
            when(tableManager.getTableName(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.findDistinctOperatorIds(TABLE_NAME, TEST_CARD_ID)).thenReturn(operatorIds);
            when(historyRepository.findDistinctOperationTypes(TABLE_NAME, TEST_CARD_ID)).thenReturn(List.of());
            when(historyRepository.findDistinctSourceTypes(TABLE_NAME, TEST_CARD_ID)).thenReturn(List.of());

            // When
            Result<CardHistoryFilters> result =
                    cardHistoryService.getAvailableFilters(TEST_CARD_TYPE_ID, TEST_CARD_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().operatorIds()).containsExactly("op-1", "op-2", "op-3");
        }

        @Test
        @DisplayName("返回去重后的操作类型列表")
        void shouldReturnDistinctOperationTypes() {
            // Given
            List<OperationType> types = List.of(
                    OperationType.CARD_CREATED,
                    OperationType.CARD_ARCHIVED,
                    OperationType.STREAM_MOVED
            );

            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(true);
            when(tableManager.getTableName(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.findDistinctOperatorIds(TABLE_NAME, TEST_CARD_ID)).thenReturn(List.of());
            when(historyRepository.findDistinctOperationTypes(TABLE_NAME, TEST_CARD_ID)).thenReturn(types);
            when(historyRepository.findDistinctSourceTypes(TABLE_NAME, TEST_CARD_ID)).thenReturn(List.of());

            // When
            Result<CardHistoryFilters> result =
                    cardHistoryService.getAvailableFilters(TEST_CARD_TYPE_ID, TEST_CARD_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().operationTypes()).containsExactlyElementsOf(types);
        }

        @Test
        @DisplayName("返回去重后的来源类型列表")
        void shouldReturnDistinctSourceTypes() {
            // Given
            List<String> sourceTypes = List.of("USER", "BIZ_RULE", "API_CALL");

            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(true);
            when(tableManager.getTableName(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.findDistinctOperatorIds(TABLE_NAME, TEST_CARD_ID)).thenReturn(List.of());
            when(historyRepository.findDistinctOperationTypes(TABLE_NAME, TEST_CARD_ID)).thenReturn(List.of());
            when(historyRepository.findDistinctSourceTypes(TABLE_NAME, TEST_CARD_ID)).thenReturn(sourceTypes);

            // When
            Result<CardHistoryFilters> result =
                    cardHistoryService.getAvailableFilters(TEST_CARD_TYPE_ID, TEST_CARD_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().sourceTypes()).containsExactly("USER", "BIZ_RULE", "API_CALL");
        }

        @Test
        @DisplayName("Repository 抛异常时返回失败结果")
        void shouldReturnFailureWhenRepositoryThrows() {
            // Given
            when(tableManager.tableExists(TEST_CARD_TYPE_ID)).thenReturn(true);
            when(tableManager.getTableName(TEST_CARD_TYPE_ID)).thenReturn(TABLE_NAME);
            when(historyRepository.findDistinctOperatorIds(TABLE_NAME, TEST_CARD_ID))
                    .thenThrow(new RuntimeException("查询失败"));

            // When
            Result<CardHistoryFilters> result =
                    cardHistoryService.getAvailableFilters(TEST_CARD_TYPE_ID, TEST_CARD_ID);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo("HISTORY_FILTERS_FAILED");
        }
    }
}
