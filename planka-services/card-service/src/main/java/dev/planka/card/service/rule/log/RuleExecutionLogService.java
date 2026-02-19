package dev.planka.card.service.rule.log;

import dev.planka.api.card.dto.RuleExecutionLogDTO;
import dev.planka.api.card.dto.RuleExecutionLogFiltersDTO;
import dev.planka.api.card.request.RuleExecutionLogSearchRequest;
import dev.planka.common.result.PageResult;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.BizRuleId;
import dev.planka.domain.schema.definition.rule.RuleExecutionLog;
import dev.planka.infra.cache.card.CardCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 规则执行日志服务
 * <p>
 * 负责记录和查询规则执行日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleExecutionLogService {

    private final RuleExecutionLogRepository repository;
    private final CardCacheService cardCacheService;

    /**
     * 保存执行日志（异步）
     */
    @Async("bizRuleExecutor")
    public void save(RuleExecutionLog executionLog) {
        try {
            repository.save(executionLog);
            log.debug("保存规则执行日志: id={}, ruleId={}, status={}",
                    executionLog.getId(), executionLog.getRuleId(), executionLog.getStatus());
        } catch (Exception e) {
            log.error("保存规则执行日志失败: ruleId={}, error={}",
                    executionLog.getRuleId(), e.getMessage(), e);
        }
    }

    /**
     * 同步保存执行日志
     */
    public void saveSync(RuleExecutionLog executionLog) {
        repository.save(executionLog);
    }

    /**
     * 批量保存执行日志
     */
    public void saveAll(List<RuleExecutionLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        try {
            repository.saveAll(logs);
            log.debug("批量保存规则执行日志: count={}", logs.size());
        } catch (Exception e) {
            log.error("批量保存规则执行日志失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 根据ID查询日志
     */
    public Optional<RuleExecutionLog> findById(String id, CardTypeId cardTypeId) {
        return repository.findById(id, cardTypeId);
    }

    /**
     * 根据规则ID查询日志
     */
    public List<RuleExecutionLog> findByRuleId(BizRuleId ruleId, CardTypeId cardTypeId, int limit) {
        return repository.findByRuleId(ruleId, cardTypeId, limit);
    }

    /**
     * 根据卡片ID查询日志
     */
    public List<RuleExecutionLog> findByCardId(CardId cardId, CardTypeId cardTypeId, int limit) {
        return repository.findByCardId(cardId, cardTypeId, limit);
    }

    /**
     * 根据时间范围查询日志
     */
    public List<RuleExecutionLog> findByTimeRange(CardTypeId cardTypeId, LocalDateTime startTime,
                                                   LocalDateTime endTime, int limit) {
        return repository.findByTimeRange(cardTypeId, startTime, endTime, limit);
    }

    /**
     * 查询最近的执行日志
     */
    public List<RuleExecutionLog> findRecent(CardTypeId cardTypeId, int limit) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(7);
        return repository.findByTimeRange(cardTypeId, startTime, endTime, limit);
    }

    /**
     * 查询失败的执行日志
     */
    public List<RuleExecutionLog> findFailed(CardTypeId cardTypeId, int limit) {
        return repository.findByStatus(cardTypeId, RuleExecutionLog.ExecutionStatus.FAILED, limit);
    }

    /**
     * 清理过期日志
     */
    public int cleanupOldLogs(CardTypeId cardTypeId, int retentionDays) {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(retentionDays);
        int deleted = repository.deleteByCreatedAtBefore(cardTypeId, beforeTime);
        log.info("清理过期日志: cardTypeId={}, retentionDays={}, deleted={}",
                cardTypeId, retentionDays, deleted);
        return deleted;
    }

    /**
     * 获取规则执行统计
     */
    public RuleExecutionStats getStats(BizRuleId ruleId, CardTypeId cardTypeId) {
        long totalCount = repository.countByRuleId(ruleId, cardTypeId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusDays(7);
        long failedCount = repository.countFailedByRuleId(ruleId, cardTypeId, startTime, now);

        return new RuleExecutionStats(totalCount, failedCount);
    }

    /**
     * 规则执行统计
     */
    public record RuleExecutionStats(long totalCount, long failedCountLast7Days) {
        public double getSuccessRate() {
            if (totalCount == 0) {
                return 100.0;
            }
            return (1 - (double) failedCountLast7Days / totalCount) * 100;
        }
    }

    /**
     * 分页搜索执行日志
     */
    public PageResult<RuleExecutionLogDTO> search(CardTypeId cardTypeId, RuleExecutionLogSearchRequest request) {
        PageResult<RuleExecutionLog> result = repository.search(cardTypeId, request);

        if (result.getContent().isEmpty()) {
            return PageResult.empty(result.getPage(), result.getSize());
        }

        // 收集所有需要查询名称的卡片ID
        Set<CardId> cardIds = new HashSet<>();
        for (RuleExecutionLog logItem : result.getContent()) {
            if (logItem.getCardId() != null) {
                cardIds.add(logItem.getCardId());
            }
            if (logItem.getOperatorId() != null) {
                cardIds.add(CardId.of(logItem.getOperatorId()));
            }
        }

        // 批量查询卡片名称
        Map<CardId, CardTitle> cardNames;
        if (!cardIds.isEmpty()) {
            cardNames = cardCacheService.queryCardNames(cardIds);
        } else {
            cardNames = Collections.emptyMap();
        }

        // 转换为DTO并填充名称
        List<RuleExecutionLogDTO> dtos = result.getContent().stream()
                .map(logItem -> toDTO(logItem, cardNames))
                .toList();

        return PageResult.of(dtos, result.getPage(), result.getSize(), result.getTotal());
    }

    /**
     * 获取过滤选项
     */
    public RuleExecutionLogFiltersDTO getFilters(CardTypeId cardTypeId) {
        return repository.getFilters(cardTypeId);
    }

    /**
     * 领域对象转DTO
     */
    private RuleExecutionLogDTO toDTO(RuleExecutionLog log, Map<CardId, CardTitle> cardNames) {
        RuleExecutionLogDTO dto = new RuleExecutionLogDTO();
        dto.setId(log.getId());
        dto.setRuleId(log.getRuleId() != null ? log.getRuleId().value() : null);
        dto.setRuleName(log.getRuleName());
        dto.setCardId(log.getCardId() != null ? log.getCardId().value() : null);
        dto.setTriggerEvent(log.getTriggerEvent() != null ? log.getTriggerEvent().name() : null);
        dto.setOperatorId(log.getOperatorId());
        dto.setExecutionTime(log.getExecutionTime());
        dto.setDurationMs(log.getDurationMs());
        dto.setStatus(log.getStatus() != null ? log.getStatus().name() : null);
        dto.setErrorMessage(log.getErrorMessage());
        dto.setAffectedCardIds(log.getAffectedCardIds());
        dto.setTraceId(log.getTraceId());

        // 填充卡片标题
        if (log.getCardId() != null && cardNames != null) {
            CardTitle cardTitle = cardNames.get(log.getCardId());
            if (cardTitle != null) {
                dto.setCardTitle(cardTitle.getDisplayValue());
            }
        }

        // 填充操作人名称
        if (log.getOperatorId() != null && cardNames != null) {
            CardTitle operatorTitle = cardNames.get(CardId.of(log.getOperatorId()));
            if (operatorTitle != null) {
                dto.setOperatorName(operatorTitle.getDisplayValue());
            }
        }

        if (log.getActionResults() != null) {
            List<RuleExecutionLogDTO.ActionResultDTO> actionResultDTOs = log.getActionResults().stream()
                    .map(ar -> {
                        RuleExecutionLogDTO.ActionResultDTO arDto = new RuleExecutionLogDTO.ActionResultDTO();
                        arDto.setActionType(ar.getActionType());
                        arDto.setSortOrder(ar.getSortOrder());
                        arDto.setSuccess(ar.isSuccess());
                        arDto.setDurationMs(ar.getDurationMs());
                        arDto.setErrorMessage(ar.getErrorMessage());
                        arDto.setAffectedCardIds(ar.getAffectedCardIds());
                        return arDto;
                    })
                    .toList();
            dto.setActionResults(actionResultDTOs);
        }

        return dto;
    }
}
