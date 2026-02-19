package dev.planka.extension.history.service;

import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.extension.history.dto.CardHistoryRecordVO;
import dev.planka.extension.history.repository.CardHistoryRepository;
import dev.planka.extension.history.vo.HistoryMessageVO;
import dev.planka.domain.history.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 卡片操作历史服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardHistoryService {

    private final CardHistoryTableManager tableManager;
    private final CardHistoryRepository historyRepository;
    private final MemberNameResolver memberNameResolver;
    private final HistoryArgumentResolver argumentResolver;

    /**
     * 记录操作历史
     */
    public Result<CardHistoryId> recordHistory(RecordHistoryCommand command) {
        try {
            // 获取或创建表
            String tableName = tableManager.getOrCreateTable(command.cardTypeId());

            // 创建历史记录
            CardHistoryRecord record = CardHistoryRecord.create(
                    command.orgId(),
                    command.cardId(),
                    command.cardTypeId(),
                    command.operationType(),
                    command.operatorId(),
                    command.operatorIp(),
                    command.operationSource(),
                    command.message(),
                    command.traceId()
            );

            // 保存记录
            CardHistoryId historyId = historyRepository.save(tableName, record);
            log.debug("记录操作历史成功: cardId={}, type={}", command.cardId(), command.operationType());

            return Result.success(historyId);
        } catch (Exception e) {
            log.error("记录操作历史失败", e);
            return Result.failure("HISTORY_RECORD_FAILED", "记录操作历史失败: " + e.getMessage());
        }
    }

    /**
     * 查询卡片操作历史（简单分页），返回 VO
     */
    public Result<PageResult<CardHistoryRecordVO>> getCardHistory(String cardTypeId, Long cardId, int page, int size) {
        try {
            // 检查表是否存在
            if (!tableManager.tableExists(cardTypeId)) {
                return Result.success(PageResult.empty(page, size));
            }

            String tableName = tableManager.getTableName(cardTypeId);
            List<CardHistoryRecord> records = historyRepository.findByCardId(tableName, cardId, page, size);
            long total = historyRepository.countByCardId(tableName, cardId);

            // 批量解析操作人名称和消息参数
            List<CardHistoryRecordVO> voList = convertToVOList(records, cardTypeId);

            return Result.success(PageResult.of(voList, page, size, total));
        } catch (Exception e) {
            log.error("查询卡片历史失败", e);
            return Result.failure("HISTORY_QUERY_FAILED", "查询卡片历史失败: " + e.getMessage());
        }
    }

    /**
     * 多条件搜索卡片操作历史，返回 VO
     */
    public Result<PageResult<CardHistoryRecordVO>> searchCardHistory(SearchHistoryQuery query) {
        try {
            // 检查表是否存在
            if (!tableManager.tableExists(query.cardTypeId())) {
                return Result.success(PageResult.empty(query.page(), query.size()));
            }

            String tableName = tableManager.getTableName(query.cardTypeId());

            List<CardHistoryRecord> records = historyRepository.search(
                    tableName,
                    query.cardId(),
                    query.operationTypes(),
                    query.operatorIds(),
                    query.sourceTypes(),
                    query.startTime(),
                    query.endTime(),
                    query.sortAsc(),
                    query.page(),
                    query.size()
            );

            long total = historyRepository.countBySearch(
                    tableName,
                    query.cardId(),
                    query.operationTypes(),
                    query.operatorIds(),
                    query.sourceTypes(),
                    query.startTime(),
                    query.endTime()
            );

            // 批量解析操作人名称和消息参数
            List<CardHistoryRecordVO> voList = convertToVOList(records, query.cardTypeId());

            return Result.success(PageResult.of(voList, query.page(), query.size(), total));
        } catch (Exception e) {
            log.error("搜索卡片历史失败", e);
            return Result.failure("HISTORY_SEARCH_FAILED", "搜索卡片历史失败: " + e.getMessage());
        }
    }

    /**
     * 批量转换历史记录为 VO（包含操作人名称解析和国际化标签）
     */
    private List<CardHistoryRecordVO> convertToVOList(List<CardHistoryRecord> records, String cardTypeId) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        // 收集所有操作人ID
        Set<String> operatorIds = records.stream()
                .map(CardHistoryRecord::operatorId)
                .collect(Collectors.toSet());

        // 批量查询操作人名称
        Map<String, String> nameMap = memberNameResolver.resolveNames(operatorIds);

        // 批量解析历史消息参数
        List<HistoryMessage> messages = records.stream()
                .map(CardHistoryRecord::message)
                .collect(Collectors.toList());
        List<HistoryMessageVO> messageVOs = argumentResolver.resolveBatch(messages, cardTypeId);

        // 转换为 VO
        List<CardHistoryRecordVO> result = new java.util.ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            CardHistoryRecord record = records.get(i);
            HistoryMessageVO messageVO = i < messageVOs.size() ? messageVOs.get(i) : null;
            result.add(CardHistoryRecordVO.from(
                    record,
                    memberNameResolver.getName(record.operatorId(), nameMap),
                    messageVO
            ));
        }
        return result;
    }

    /**
     * 获取可用的筛选选项
     */
    public Result<CardHistoryFilters> getAvailableFilters(String cardTypeId, Long cardId) {
        try {
            // 检查表是否存在
            if (!tableManager.tableExists(cardTypeId)) {
                return Result.success(CardHistoryFilters.empty());
            }

            String tableName = tableManager.getTableName(cardTypeId);

            List<String> operatorIds = historyRepository.findDistinctOperatorIds(tableName, cardId);
            List<OperationType> operationTypes = historyRepository.findDistinctOperationTypes(tableName, cardId);
            List<String> sourceTypes = historyRepository.findDistinctSourceTypes(tableName, cardId);

            return Result.success(new CardHistoryFilters(operatorIds, operationTypes, sourceTypes));
        } catch (Exception e) {
            log.error("获取筛选选项失败", e);
            return Result.failure("HISTORY_FILTERS_FAILED", "获取筛选选项失败: " + e.getMessage());
        }
    }

    // ==================== 命令和查询对象 ====================

    /**
     * 记录历史命令
     */
    public record RecordHistoryCommand(
            String orgId,
            Long cardId,
            String cardTypeId,
            OperationType operationType,
            String operatorId,
            String operatorIp,
            OperationSource operationSource,
            HistoryMessage message,
            String traceId
    ) {}

    /**
     * 搜索历史查询
     *
     * @param sortAsc true=正序（最早的在前），false=倒序（最新的在前，默认）
     */
    public record SearchHistoryQuery(
            String cardTypeId,
            Long cardId,
            List<OperationType> operationTypes,
            List<String> operatorIds,
            List<String> sourceTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean sortAsc,
            int page,
            int size
    ) {}

    /**
     * 历史筛选选项
     */
    public record CardHistoryFilters(
            List<String> operatorIds,
            List<OperationType> operationTypes,
            List<String> sourceTypes
    ) {
        public static CardHistoryFilters empty() {
            return new CardHistoryFilters(List.of(), List.of(), List.of());
        }
    }
}
