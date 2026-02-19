package dev.planka.extension.history.mapper;

import dev.planka.extension.history.model.CardHistoryEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 卡片操作历史 Mapper
 * <p>
 * 使用动态SQL支持分表查询，表名通过参数传入
 */
@Mapper
public interface CardHistoryMapper {

    /**
     * 插入历史记录
     */
    @Insert("INSERT INTO ${tableName} " +
            "(id, org_id, card_id, card_type_id, operation_type, operator_id, operator_ip, operation_source, message, trace_id, created_at) " +
            "VALUES (#{record.id}, #{record.orgId}, #{record.cardId}, #{record.cardTypeId}, #{record.operationType}, " +
            "#{record.operatorId}, #{record.operatorIp}, #{record.operationSource}, #{record.message}, #{record.traceId}, #{record.createdAt})")
    void insert(@Param("tableName") String tableName, @Param("record") CardHistoryEntity record);

    /**
     * 根据卡片ID查询历史记录（分页，按时间倒序）
     */
    @Select("SELECT * FROM ${tableName} WHERE card_id = #{cardId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<CardHistoryEntity> findByCardId(
            @Param("tableName") String tableName,
            @Param("cardId") Long cardId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 根据卡片ID统计总数
     */
    @Select("SELECT COUNT(*) FROM ${tableName} WHERE card_id = #{cardId}")
    long countByCardId(@Param("tableName") String tableName, @Param("cardId") Long cardId);

    /**
     * 多条件查询历史记录（分页，支持正序/倒序）
     *
     * @param sortAsc true=正序（最早的在前），false=倒序（最新的在前）
     */
    @Select("<script>" +
            "SELECT * FROM ${tableName} WHERE card_id = #{cardId} " +
            "<if test='operationTypes != null and operationTypes.size() > 0'>" +
            "AND operation_type IN <foreach collection='operationTypes' item='type' open='(' separator=',' close=')'>#{type}</foreach> " +
            "</if>" +
            "<if test='operatorIds != null and operatorIds.size() > 0'>" +
            "AND operator_id IN <foreach collection='operatorIds' item='opId' open='(' separator=',' close=')'>#{opId}</foreach> " +
            "</if>" +
            "<if test='sourceTypes != null and sourceTypes.size() > 0'>" +
            "AND JSON_EXTRACT(operation_source, '$.type') IN <foreach collection='sourceTypes' item='st' open='(' separator=',' close=')'>#{st}</foreach> " +
            "</if>" +
            "<if test='startTime != null'>" +
            "AND created_at &gt;= #{startTime} " +
            "</if>" +
            "<if test='endTime != null'>" +
            "AND created_at &lt;= #{endTime} " +
            "</if>" +
            "<choose>" +
            "<when test='sortAsc == true'>ORDER BY created_at ASC</when>" +
            "<otherwise>ORDER BY created_at DESC</otherwise>" +
            "</choose>" +
            " LIMIT #{offset}, #{limit}" +
            "</script>")
    List<CardHistoryEntity> search(
            @Param("tableName") String tableName,
            @Param("cardId") Long cardId,
            @Param("operationTypes") List<String> operationTypes,
            @Param("operatorIds") List<String> operatorIds,
            @Param("sourceTypes") List<String> sourceTypes,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("sortAsc") boolean sortAsc,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 多条件统计总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM ${tableName} WHERE card_id = #{cardId} " +
            "<if test='operationTypes != null and operationTypes.size() > 0'>" +
            "AND operation_type IN <foreach collection='operationTypes' item='type' open='(' separator=',' close=')'>#{type}</foreach> " +
            "</if>" +
            "<if test='operatorIds != null and operatorIds.size() > 0'>" +
            "AND operator_id IN <foreach collection='operatorIds' item='opId' open='(' separator=',' close=')'>#{opId}</foreach> " +
            "</if>" +
            "<if test='sourceTypes != null and sourceTypes.size() > 0'>" +
            "AND JSON_EXTRACT(operation_source, '$.type') IN <foreach collection='sourceTypes' item='st' open='(' separator=',' close=')'>#{st}</foreach> " +
            "</if>" +
            "<if test='startTime != null'>" +
            "AND created_at &gt;= #{startTime} " +
            "</if>" +
            "<if test='endTime != null'>" +
            "AND created_at &lt;= #{endTime} " +
            "</if>" +
            "</script>")
    long countBySearch(
            @Param("tableName") String tableName,
            @Param("cardId") Long cardId,
            @Param("operationTypes") List<String> operationTypes,
            @Param("operatorIds") List<String> operatorIds,
            @Param("sourceTypes") List<String> sourceTypes,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 获取某张卡片涉及的操作人列表
     */
    @Select("SELECT DISTINCT operator_id FROM ${tableName} WHERE card_id = #{cardId}")
    List<String> findDistinctOperatorIds(@Param("tableName") String tableName, @Param("cardId") Long cardId);

    /**
     * 获取某张卡片涉及的操作类型列表
     */
    @Select("SELECT DISTINCT operation_type FROM ${tableName} WHERE card_id = #{cardId}")
    List<String> findDistinctOperationTypes(@Param("tableName") String tableName, @Param("cardId") Long cardId);

    /**
     * 获取某张卡片涉及的操作来源类型列表
     */
    @Select("SELECT DISTINCT JSON_EXTRACT(operation_source, '$.type') FROM ${tableName} WHERE card_id = #{cardId} AND operation_source IS NOT NULL")
    List<String> findDistinctSourceTypes(@Param("tableName") String tableName, @Param("cardId") Long cardId);

    /**
     * 动态创建历史记录表
     */
    @Update("CREATE TABLE IF NOT EXISTS ${tableName} (" +
            "id BIGINT UNSIGNED NOT NULL COMMENT '历史记录ID（雪花算法）', " +
            "org_id VARCHAR(64) NOT NULL COMMENT '组织ID', " +
            "card_id BIGINT NOT NULL COMMENT '卡片ID', " +
            "card_type_id VARCHAR(64) NOT NULL COMMENT '卡片类型ID', " +
            "operation_type VARCHAR(50) NOT NULL COMMENT '操作类型', " +
            "operator_id VARCHAR(64) NOT NULL COMMENT '操作人ID', " +
            "operator_ip VARCHAR(50) NULL COMMENT '操作人IP', " +
            "operation_source JSON NULL COMMENT '操作来源（JSON）', " +
            "message JSON NOT NULL COMMENT '历史消息（JSON）', " +
            "trace_id VARCHAR(64) NULL COMMENT '追踪ID', " +
            "created_at DATETIME(3) NOT NULL COMMENT '创建时间（毫秒精度）', " +
            "PRIMARY KEY (id), " +
            "INDEX idx_card_id_time (card_id, created_at), " +
            "INDEX idx_operator_time (operator_id, created_at), " +
            "INDEX idx_type_time (operation_type, created_at), " +
            "INDEX idx_created_at (created_at)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卡片操作历史表'")
    void createTable(@Param("tableName") String tableName);

    /**
     * 检查表是否存在
     */
    @Select("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = #{tableName}")
    int tableExists(@Param("tableName") String tableName);
}
