package dev.planka.card.service.flowrecord;

import dev.planka.card.mapper.FlowRecordMapper;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.stream.*;
import dev.planka.infra.cache.schema.query.ValueStreamCacheQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流动记录服务
 * <p>
 * 负责流动记录的写入和查询
 */
@Service
public class FlowRecordService {

    private static final Logger logger = LoggerFactory.getLogger(FlowRecordService.class);

    private final FlowRecordTableManager tableManager;
    private final FlowRecordGenerator generator;
    private final FlowRecordMapper flowRecordMapper;
    private final ValueStreamHelper valueStreamHelper;
    private final ValueStreamCacheQuery valueStreamCacheQuery;

    public FlowRecordService(FlowRecordTableManager tableManager,
                             FlowRecordGenerator generator,
                             FlowRecordMapper flowRecordMapper,
                             ValueStreamHelper valueStreamHelper, ValueStreamCacheQuery valueStreamCacheQuery) {
        this.tableManager = tableManager;
        this.generator = generator;
        this.flowRecordMapper = flowRecordMapper;
        this.valueStreamHelper = valueStreamHelper;
        this.valueStreamCacheQuery = valueStreamCacheQuery;
    }

    /**
     * 记录状态变更
     *
     * @param context 状态变更上下文
     */
    @Transactional
    public void recordStatusChange(CardStatusChangeContext context) {
        // 获取价值流定义
        var valueStreamOpt = valueStreamCacheQuery.getValueStreamByCardTypeId(
                context.getCardTypeId());

        if (valueStreamOpt.isEmpty()) {
            logger.warn("未找到价值流定义，跳过流动记录: cardTypeId={}", context.getCardTypeId());
            return;
        }

        // 获取状态路径
        List<ValueStreamHelper.StatusNode> statusPath = valueStreamHelper.getStatusPath(
                valueStreamOpt.get(), context.getFromStatusId(), context.getToStatusId());

        if (statusPath.isEmpty()) {
            logger.warn("状态路径为空，跳过流动记录: cardId={}, from={}, to={}",
                    context.getCardId(), context.getFromStatusId(), context.getToStatusId());
            return;
        }

        // 获取或创建表
        String tableName = tableManager.getOrCreateTable(context.getStreamId());

        // 生成流动记录
        List<FlowRecord> records = generator.generate(context, statusPath);

        if (records.isEmpty()) {
            logger.warn("未生成任何流动记录: cardId={}, from={}, to={}",
                    context.getCardId(), context.getFromStatusId(), context.getToStatusId());
            return;
        }

        // 转换为 Entity 并批量插入
        List<FlowRecordEntity> entities = records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        flowRecordMapper.batchInsert(tableName, entities);

        logger.debug("记录状态变更: cardId={}, from={}, to={}, pathSize={}, recordCount={}",
                context.getCardId(), context.getFromStatusId(), context.getToStatusId(),
                statusPath.size(), records.size());
    }

    /**
     * 查询卡片的流动历史
     *
     * @param streamId 价值流ID
     * @param cardId   卡片ID
     * @return 流动记录列表（按事件时间升序）
     */
    public List<FlowRecord> getCardFlowHistory(StreamId streamId, CardId cardId) {
        String tableName = tableManager.getTableName(streamId);
        if (tableName == null) {
            return List.of();
        }

        List<FlowRecordEntity> entities = flowRecordMapper.findByCardId(tableName, Long.parseLong(cardId.value()));
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 查询卡片在某状态的流动记录
     *
     * @param streamId 价值流ID
     * @param cardId   卡片ID
     * @param statusId 状态ID
     * @return 流动记录列表（按事件时间升序）
     */
    public List<FlowRecord> getCardStatusRecords(StreamId streamId, CardId cardId, StatusId statusId) {
        String tableName = tableManager.getTableName(streamId);
        if (tableName == null) {
            return List.of();
        }

        List<FlowRecordEntity> entities = flowRecordMapper.findByCardIdAndStatusId(
                tableName, Long.parseLong(cardId.value()), statusId.value());
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 计算卡片在某状态的停留时间
     * <p>
     * 通过匹配 ENTER 和 LEAVE（或 ROLLBACK_LEAVE）记录计算
     *
     * @param streamId 价值流ID
     * @param cardId   卡片ID
     * @param statusId 状态ID
     * @return 总停留时间，如果卡片仍在该状态则计算到当前时间
     */
    public Duration getStatusDuration(StreamId streamId, CardId cardId, StatusId statusId) {
        List<FlowRecord> records = getCardStatusRecords(streamId, cardId, statusId);

        Duration totalDuration = Duration.ZERO;
        LocalDateTime enterTime = null;

        for (FlowRecord record : records) {
            if (record.getRecordType().isEntry()) {
                // 进入状态
                enterTime = record.getEventTime();
            } else if (enterTime != null) {
                // 离开状态
                totalDuration = totalDuration.plus(Duration.between(enterTime, record.getEventTime()));
                enterTime = null;
            }
        }

        // 如果卡片仍在该状态（有进入但没有离开），计算到当前时间
        if (enterTime != null) {
            totalDuration = totalDuration.plus(Duration.between(enterTime, LocalDateTime.now()));
        }

        return totalDuration;
    }

    /**
     * 根据时间范围查询流动记录
     *
     * @param streamId  价值流ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 流动记录列表
     */
    public List<FlowRecord> getRecordsByTimeRange(StreamId streamId, LocalDateTime startTime, LocalDateTime endTime) {
        String tableName = tableManager.getTableName(streamId);
        if (tableName == null) {
            return List.of();
        }

        List<FlowRecordEntity> entities = flowRecordMapper.findByTimeRange(tableName, startTime, endTime);
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Entity 转 Domain
     */
    private FlowRecord toDomain(FlowRecordEntity entity) {
        return FlowRecord.builder()
                .id(FlowRecordId.of(entity.getId()))
                .cardId(CardId.of(String.valueOf(entity.getCardId())))
                .cardTypeId(CardTypeId.of(entity.getCardTypeId()))
                .streamId(StreamId.of(entity.getStreamId()))
                .stepId(StepId.of(entity.getStepId()))
                .statusId(StatusId.of(entity.getStatusId()))
                .statusWorkType(entity.getStatusWorkTypeEnum())
                .recordType(entity.getRecordTypeEnum())
                .eventTime(entity.getEventTime())
                .operatorId(entity.getOperatorId())
                .build();
    }

    /**
     * Domain 转 Entity
     */
    private FlowRecordEntity toEntity(FlowRecord record) {
        FlowRecordEntity entity = new FlowRecordEntity();
        entity.setId(record.getId().value());
        entity.setCardId(Long.parseLong(record.getCardId().value()));
        entity.setCardTypeId(record.getCardTypeId().value());
        entity.setStreamId(record.getStreamId().value());
        entity.setStepId(record.getStepId().value());
        entity.setStatusId(record.getStatusId().value());
        entity.setStatusWorkTypeEnum(record.getStatusWorkType());
        entity.setRecordTypeEnum(record.getRecordType());
        entity.setEventTime(record.getEventTime());
        entity.setOperatorId(record.getOperatorId());
        return entity;
    }
}
