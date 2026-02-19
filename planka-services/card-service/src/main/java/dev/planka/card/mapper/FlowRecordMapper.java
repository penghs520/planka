package dev.planka.card.mapper;

import dev.planka.card.service.flowrecord.FlowRecordEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流动记录 Mapper
 * <p>
 * 使用动态SQL支持分表查询，表名通过参数传入
 */
@Mapper
public interface FlowRecordMapper {

    /**
     * 插入流动记录
     */
    @Insert("INSERT INTO ${tableName} " +
            "(id, card_id, card_type_id, stream_id, step_id, status_id, status_work_type, record_type, event_time, operator_id) " +
            "VALUES (#{record.id}, #{record.cardId}, #{record.cardTypeId}, #{record.streamId}, #{record.stepId}, " +
            "#{record.statusId}, #{record.statusWorkType}, #{record.recordType}, #{record.eventTime}, #{record.operatorId})")
    void insert(@Param("tableName") String tableName, @Param("record") FlowRecordEntity record);

    /**
     * 批量插入流动记录
     */
    @Insert("<script>" +
            "INSERT INTO ${tableName} " +
            "(id, card_id, card_type_id, stream_id, step_id, status_id, status_work_type, record_type, event_time, operator_id) VALUES " +
            "<foreach collection='records' item='r' separator=','>" +
            "(#{r.id}, #{r.cardId}, #{r.cardTypeId}, #{r.streamId}, #{r.stepId}, #{r.statusId}, #{r.statusWorkType}, #{r.recordType}, #{r.eventTime}, #{r.operatorId})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("tableName") String tableName, @Param("records") List<FlowRecordEntity> records);

    /**
     * 根据卡片ID查询流动记录（按事件时间升序）
     */
    @Select("SELECT * FROM ${tableName} WHERE card_id = #{cardId} ORDER BY event_time ASC")
    List<FlowRecordEntity> findByCardId(@Param("tableName") String tableName, @Param("cardId") Long cardId);

    /**
     * 根据卡片ID和状态ID查询流动记录（按事件时间升序）
     */
    @Select("SELECT * FROM ${tableName} WHERE card_id = #{cardId} AND status_id = #{statusId} ORDER BY event_time ASC")
    List<FlowRecordEntity> findByCardIdAndStatusId(
            @Param("tableName") String tableName,
            @Param("cardId") Long cardId,
            @Param("statusId") String statusId);

    /**
     * 根据时间范围查询流动记录
     */
    @Select("SELECT * FROM ${tableName} WHERE event_time >= #{startTime} AND event_time < #{endTime} ORDER BY event_time ASC")
    List<FlowRecordEntity> findByTimeRange(
            @Param("tableName") String tableName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据状态ID和时间范围查询流动记录
     */
    @Select("SELECT * FROM ${tableName} WHERE status_id = #{statusId} AND event_time >= #{startTime} AND event_time < #{endTime} ORDER BY event_time ASC")
    List<FlowRecordEntity> findByStatusIdAndTimeRange(
            @Param("tableName") String tableName,
            @Param("statusId") String statusId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 动态创建流动记录表
     */
    @Update("CREATE TABLE IF NOT EXISTS ${tableName} (" +
            "id BIGINT UNSIGNED NOT NULL COMMENT '唯一标识（雪花算法）', " +
            "card_id BIGINT NOT NULL COMMENT '卡片ID', " +
            "card_type_id VARCHAR(64) NOT NULL COMMENT '卡片类型ID', " +
            "stream_id VARCHAR(64) NOT NULL COMMENT '价值流ID', " +
            "step_id VARCHAR(64) NOT NULL COMMENT '阶段ID', " +
            "status_id VARCHAR(64) NOT NULL COMMENT '状态ID', " +
            "status_work_type TINYINT NOT NULL COMMENT '状态工作类型（0=等待，1=工作中）', " +
            "record_type VARCHAR(20) NOT NULL COMMENT '记录类型枚举', " +
            "event_time DATETIME(3) NOT NULL COMMENT '事件发生时间（毫秒精度）', " +
            "operator_id VARCHAR(64) NULL COMMENT '操作人ID', " +
            "PRIMARY KEY (id), " +
            "INDEX idx_card_id_time (card_id, event_time), " +
            "INDEX idx_event_time (event_time), " +
            "INDEX idx_status_time (status_id, event_time), " +
            "INDEX idx_step_time (step_id, event_time)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流动记录表'")
    void createTable(@Param("tableName") String tableName);

    /**
     * 检查表是否存在
     */
    @Select("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = #{tableName}")
    int tableExists(@Param("tableName") String tableName);
}
