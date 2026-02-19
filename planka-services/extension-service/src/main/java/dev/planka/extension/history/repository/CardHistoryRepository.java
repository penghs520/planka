package dev.planka.extension.history.repository;

import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.extension.history.mapper.CardHistoryMapper;
import dev.planka.extension.history.model.CardHistoryEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.planka.domain.history.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 卡片操作历史 Repository
 * <p>
 * 防腐层：隔离数据访问细节，提供领域模型的存取接口
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CardHistoryRepository {

    private final CardHistoryMapper cardHistoryMapper;
    private final ObjectMapper objectMapper;

    /**
     * 保存历史记录
     */
    public CardHistoryId save(String tableName, CardHistoryRecord record) {
        CardHistoryEntity entity = toEntity(record);
        cardHistoryMapper.insert(tableName, entity);
        return CardHistoryId.of(String.valueOf(entity.getId()));
    }

    /**
     * 根据卡片ID查询历史记录（分页）
     */
    public List<CardHistoryRecord> findByCardId(String tableName, Long cardId, int page, int size) {
        int offset = (page - 1) * size;
        List<CardHistoryEntity> entities = cardHistoryMapper.findByCardId(tableName, cardId, offset, size);
        return entities.stream().map(this::toRecord).collect(Collectors.toList());
    }

    /**
     * 根据卡片ID统计总数
     */
    public long countByCardId(String tableName, Long cardId) {
        return cardHistoryMapper.countByCardId(tableName, cardId);
    }

    /**
     * 多条件查询历史记录（分页，支持排序）
     *
     * @param sortAsc true=正序（最早的在前），false=倒序（最新的在前）
     */
    public List<CardHistoryRecord> search(String tableName, Long cardId,
                                          List<OperationType> operationTypes,
                                          List<String> operatorIds,
                                          List<String> sourceTypes,
                                          LocalDateTime startTime,
                                          LocalDateTime endTime,
                                          boolean sortAsc,
                                          int page, int size) {
        int offset = (page - 1) * size;
        List<String> typeNames = operationTypes != null
                ? operationTypes.stream().map(OperationType::name).collect(Collectors.toList())
                : null;

        List<CardHistoryEntity> entities = cardHistoryMapper.search(
                tableName, cardId, typeNames, operatorIds, sourceTypes, startTime, endTime, sortAsc, offset, size);
        return entities.stream().map(this::toRecord).collect(Collectors.toList());
    }

    /**
     * 多条件统计总数
     */
    public long countBySearch(String tableName, Long cardId,
                              List<OperationType> operationTypes,
                              List<String> operatorIds,
                              List<String> sourceTypes,
                              LocalDateTime startTime,
                              LocalDateTime endTime) {
        List<String> typeNames = operationTypes != null
                ? operationTypes.stream().map(OperationType::name).collect(Collectors.toList())
                : null;

        return cardHistoryMapper.countBySearch(
                tableName, cardId, typeNames, operatorIds, sourceTypes, startTime, endTime);
    }

    /**
     * 获取某张卡片涉及的操作人列表
     */
    public List<String> findDistinctOperatorIds(String tableName, Long cardId) {
        return cardHistoryMapper.findDistinctOperatorIds(tableName, cardId);
    }

    /**
     * 获取某张卡片涉及的操作类型列表
     */
    public List<OperationType> findDistinctOperationTypes(String tableName, Long cardId) {
        return cardHistoryMapper.findDistinctOperationTypes(tableName, cardId).stream()
                .map(OperationType::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * 获取某张卡片涉及的操作来源类型列表
     */
    public List<String> findDistinctSourceTypes(String tableName, Long cardId) {
        return cardHistoryMapper.findDistinctSourceTypes(tableName, cardId).stream()
                .map(s -> s != null ? s.replace("\"", "") : null)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toList());
    }

    // ==================== Entity 与领域模型转换 ====================

    private CardHistoryEntity toEntity(CardHistoryRecord record) {
        CardHistoryEntity entity = new CardHistoryEntity();
        entity.setId(SnowflakeIdGenerator.generate());
        entity.setOrgId(record.orgId());
        entity.setCardId(record.cardId());
        entity.setCardTypeId(record.cardTypeId());
        entity.setOperationType(record.operationType().name());
        entity.setOperatorId(record.operatorId());
        entity.setOperatorIp(record.operatorIp());
        entity.setOperationSource(serializeJson(record.operationSource()));
        entity.setMessage(serializeJson(record.message()));
        entity.setTraceId(record.traceId());
        entity.setCreatedAt(record.createdAt() != null ? record.createdAt() : LocalDateTime.now());
        return entity;
    }

    private CardHistoryRecord toRecord(CardHistoryEntity entity) {
        return new CardHistoryRecord(
                CardHistoryId.of(String.valueOf(entity.getId())),
                entity.getOrgId(),
                entity.getCardId(),
                entity.getCardTypeId(),
                OperationType.valueOf(entity.getOperationType()),
                entity.getOperatorId(),
                entity.getOperatorIp(),
                deserializeOperationSource(entity.getOperationSource()),
                deserializeHistoryMessage(entity.getMessage()),
                entity.getTraceId(),
                entity.getCreatedAt()
        );
    }

    private String serializeJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    private OperationSource deserializeOperationSource(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, OperationSource.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize OperationSource from JSON: {}", json, e);
            return null;
        }
    }

    private HistoryMessage deserializeHistoryMessage(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, HistoryMessage.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize HistoryMessage from JSON: {}", json, e);
            return null;
        }
    }
}
