package dev.planka.card.service.rule.log;

import dev.planka.api.card.dto.RuleExecutionLogFiltersDTO;
import dev.planka.api.card.request.RuleExecutionLogSearchRequest;
import dev.planka.card.mapper.RuleExecutionLogMapper;
import dev.planka.common.result.PageResult;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.BizRuleId;
import dev.planka.domain.schema.definition.rule.BizRuleDefinition;
import dev.planka.domain.schema.definition.rule.RuleExecutionLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 规则执行日志仓储实现
 * <p>
 * 按卡片类型分表存储，使用 Mapper 操作数据库。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RuleExecutionLogRepositoryImpl implements RuleExecutionLogRepository {

    private final RuleExecutionLogMapper logMapper;
    private final RuleExecutionLogTableManager tableManager;
    private final ObjectMapper objectMapper;

    @Override
    public void save(RuleExecutionLog executionLog) {
        String tableName = tableManager.getOrCreateTable(executionLog.getCardTypeId());
        RuleExecutionLogEntity entity = toEntity(executionLog);
        logMapper.insert(tableName, entity);
    }

    @Override
    public void saveAll(List<RuleExecutionLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }

        // 按卡片类型分组批量插入
        logs.stream()
                .collect(java.util.stream.Collectors.groupingBy(RuleExecutionLog::getCardTypeId))
                .forEach((cardTypeId, groupedLogs) -> {
                    String tableName = tableManager.getOrCreateTable(cardTypeId);
                    List<RuleExecutionLogEntity> entities = groupedLogs.stream()
                            .map(this::toEntity)
                            .toList();
                    logMapper.batchInsert(tableName, entities);
                });
    }

    @Override
    public Optional<RuleExecutionLog> findById(String id, CardTypeId cardTypeId) {
        String tableName = tableManager.getTableName(cardTypeId);
        if (tableName == null) {
            return Optional.empty();
        }

        RuleExecutionLogEntity entity = logMapper.findById(tableName, id);
        return entity != null ? Optional.of(toDomain(entity, cardTypeId)) : Optional.empty();
    }

    @Override
    public List<RuleExecutionLog> findByRuleId(BizRuleId ruleId, CardTypeId cardTypeId, int limit) {
        String tableName = tableManager.getTableName(cardTypeId);
        if (tableName == null) {
            return List.of();
        }

        return logMapper.findByRuleId(tableName, ruleId.value(), limit).stream()
                .map(entity -> toDomain(entity, cardTypeId))
                .toList();
    }

    @Override
    public List<RuleExecutionLog> findByCardId(CardId cardId, CardTypeId cardTypeId, int limit) {
        String tableName = tableManager.getTableName(cardTypeId);
        if (tableName == null) {
            return List.of();
        }

        return logMapper.findByCardId(tableName, cardId.value(), limit).stream()
                .map(entity -> toDomain(entity, cardTypeId))
                .toList();
    }

    @Override
    public List<RuleExecutionLog> findByTimeRange(CardTypeId cardTypeId, LocalDateTime startTime,
                                                   LocalDateTime endTime, int limit) {
        String tableName = tableManager.getTableName(cardTypeId);
        if (tableName == null) {
            return List.of();
        }

        return logMapper.findByTimeRange(tableName, startTime, endTime, limit).stream()
                .map(entity -> toDomain(entity, cardTypeId))
                .toList();
    }

    @Override
    public List<RuleExecutionLog> findByStatus(CardTypeId cardTypeId,
                                                RuleExecutionLog.ExecutionStatus status, int limit) {
        String tableName = tableManager.getTableName(cardTypeId);
        if (tableName == null) {
            return List.of();
        }

        return logMapper.findByStatus(tableName, status.name(), limit).stream()
                .map(entity -> toDomain(entity, cardTypeId))
                .toList();
    }

    @Override
    public int deleteByCreatedAtBefore(CardTypeId cardTypeId, LocalDateTime beforeTime) {
        String tableName = tableManager.getTableName(cardTypeId);
        if (tableName == null) {
            return 0;
        }

        return logMapper.deleteByCreatedAtBefore(tableName, beforeTime);
    }

    @Override
    public long countByRuleId(BizRuleId ruleId, CardTypeId cardTypeId) {
        String tableName = tableManager.getTableName(cardTypeId);
        if (tableName == null) {
            return 0;
        }

        return logMapper.countByRuleId(tableName, ruleId.value());
    }

    @Override
    public long countFailedByRuleId(BizRuleId ruleId, CardTypeId cardTypeId,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        String tableName = tableManager.getTableName(cardTypeId);
        if (tableName == null) {
            return 0;
        }

        return logMapper.countFailedByRuleId(tableName, ruleId.value(), startTime, endTime);
    }

    /**
     * 领域对象转数据库实体
     */
    private RuleExecutionLogEntity toEntity(RuleExecutionLog log) {
        RuleExecutionLogEntity entity = new RuleExecutionLogEntity();
        entity.setId(log.getId());
        entity.setRuleId(log.getRuleId() != null ? log.getRuleId().value() : null);
        entity.setRuleName(log.getRuleName());
        entity.setCardId(log.getCardId() != null ? log.getCardId().value() : null);
        entity.setTriggerEvent(log.getTriggerEvent() != null ? log.getTriggerEvent().name() : null);
        entity.setOperatorId(log.getOperatorId());
        entity.setExecutionTime(log.getExecutionTime());
        entity.setDurationMs(log.getDurationMs());
        entity.setStatus(log.getStatus() != null ? log.getStatus().name() : null);
        entity.setAffectedCardIds(toJson(log.getAffectedCardIds()));
        entity.setActionResults(toJson(log.getActionResults()));
        entity.setErrorMessage(log.getErrorMessage());
        entity.setTraceId(log.getTraceId());
        entity.setCreatedAt(log.getCreatedAt());
        return entity;
    }

    /**
     * 数据库实体转领域对象
     */
    private RuleExecutionLog toDomain(RuleExecutionLogEntity entity, CardTypeId cardTypeId) {
        RuleExecutionLog domainLog = new RuleExecutionLog();
        domainLog.setId(entity.getId());
        domainLog.setRuleId(entity.getRuleId() != null ? BizRuleId.of(entity.getRuleId()) : null);
        domainLog.setRuleName(entity.getRuleName());
        domainLog.setCardTypeId(cardTypeId);
        domainLog.setCardId(entity.getCardId() != null ? CardId.of(entity.getCardId()) : null);

        if (entity.getTriggerEvent() != null) {
            try {
                domainLog.setTriggerEvent(BizRuleDefinition.TriggerEvent.valueOf(entity.getTriggerEvent()));
            } catch (IllegalArgumentException e) {
                log.warn("无效的触发事件类型: {}", entity.getTriggerEvent());
            }
        }

        domainLog.setOperatorId(entity.getOperatorId());
        domainLog.setExecutionTime(entity.getExecutionTime());
        domainLog.setDurationMs(entity.getDurationMs() != null ? entity.getDurationMs() : 0L);

        if (entity.getStatus() != null) {
            try {
                domainLog.setStatus(RuleExecutionLog.ExecutionStatus.valueOf(entity.getStatus()));
            } catch (IllegalArgumentException e) {
                log.warn("无效的执行状态: {}", entity.getStatus());
            }
        }

        domainLog.setAffectedCardIds(fromJson(entity.getAffectedCardIds(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
        domainLog.setActionResults(fromJson(entity.getActionResults(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, RuleExecutionLog.ActionResult.class)));
        domainLog.setErrorMessage(entity.getErrorMessage());
        domainLog.setTraceId(entity.getTraceId());
        domainLog.setCreatedAt(entity.getCreatedAt());
        return domainLog;
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private <T> T fromJson(String json, com.fasterxml.jackson.databind.JavaType type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public PageResult<RuleExecutionLog> search(CardTypeId cardTypeId, RuleExecutionLogSearchRequest request) {
        String tableName = tableManager.getTableName(cardTypeId);
        if (tableName == null) {
            return PageResult.empty(request.getPage() - 1, request.getSize());
        }

        // 查询总数
        long total = logMapper.countWithConditions(
                tableName,
                request.getRuleIds(),
                request.getStatuses(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (total == 0) {
            return PageResult.empty(request.getPage() - 1, request.getSize());
        }

        // 查询数据
        List<RuleExecutionLogEntity> entities = logMapper.searchWithPaging(
                tableName,
                request.getRuleIds(),
                request.getStatuses(),
                request.getStartTime(),
                request.getEndTime(),
                request.getOffset(),
                request.getSize(),
                request.isSortAsc()
        );

        List<RuleExecutionLog> logs = entities.stream()
                .map(entity -> toDomain(entity, cardTypeId))
                .toList();

        return PageResult.of(logs, request.getPage() - 1, request.getSize(), total);
    }

    @Override
    public RuleExecutionLogFiltersDTO getFilters(CardTypeId cardTypeId) {
        RuleExecutionLogFiltersDTO filters = new RuleExecutionLogFiltersDTO();

        String tableName = tableManager.getTableName(cardTypeId);
        if (tableName != null) {
            List<RuleExecutionLogEntity> distinctRules = logMapper.findDistinctRules(tableName);
            List<RuleExecutionLogFiltersDTO.RuleOption> ruleOptions = distinctRules.stream()
                    .map(e -> new RuleExecutionLogFiltersDTO.RuleOption(e.getRuleId(), e.getRuleName()))
                    .toList();
            filters.setRules(ruleOptions);
        } else {
            filters.setRules(List.of());
        }

        // 设置所有可用的状态
        filters.setStatuses(Arrays.asList("SUCCESS", "FAILED", "SKIPPED"));

        return filters;
    }
}
