package dev.planka.card.mapper;

import dev.planka.card.service.rule.log.RuleExecutionLogEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 规则执行日志 Mapper
 * <p>
 * 使用动态SQL支持分表查询，表名通过参数传入
 */
@Mapper
public interface RuleExecutionLogMapper {

    /**
     * 插入执行日志
     */
    @Insert("INSERT INTO ${tableName} " +
            "(id, rule_id, rule_name, card_id, trigger_event, operator_id, " +
            "execution_time, duration_ms, status, affected_card_ids, action_results, " +
            "error_message, trace_id, created_at) " +
            "VALUES (#{record.id}, #{record.ruleId}, #{record.ruleName}, #{record.cardId}, #{record.triggerEvent}, " +
            "#{record.operatorId}, #{record.executionTime}, #{record.durationMs}, #{record.status}, " +
            "#{record.affectedCardIds}, #{record.actionResults}, #{record.errorMessage}, #{record.traceId}, #{record.createdAt})")
    void insert(@Param("tableName") String tableName, @Param("record") RuleExecutionLogEntity record);

    /**
     * 批量插入执行日志
     */
    @Insert("<script>" +
            "INSERT INTO ${tableName} " +
            "(id, rule_id, rule_name, card_id, trigger_event, operator_id, " +
            "execution_time, duration_ms, status, affected_card_ids, action_results, " +
            "error_message, trace_id, created_at) VALUES " +
            "<foreach collection='records' item='r' separator=','>" +
            "(#{r.id}, #{r.ruleId}, #{r.ruleName}, #{r.cardId}, #{r.triggerEvent}, " +
            "#{r.operatorId}, #{r.executionTime}, #{r.durationMs}, #{r.status}, " +
            "#{r.affectedCardIds}, #{r.actionResults}, #{r.errorMessage}, #{r.traceId}, #{r.createdAt})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("tableName") String tableName, @Param("records") List<RuleExecutionLogEntity> records);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM ${tableName} WHERE id = #{id}")
    RuleExecutionLogEntity findById(@Param("tableName") String tableName, @Param("id") String id);

    /**
     * 根据规则ID查询（按执行时间降序）
     */
    @Select("SELECT * FROM ${tableName} WHERE rule_id = #{ruleId} ORDER BY execution_time DESC LIMIT #{limit}")
    List<RuleExecutionLogEntity> findByRuleId(
            @Param("tableName") String tableName,
            @Param("ruleId") String ruleId,
            @Param("limit") int limit);

    /**
     * 根据卡片ID查询（按执行时间降序）
     */
    @Select("SELECT * FROM ${tableName} WHERE card_id = #{cardId} ORDER BY execution_time DESC LIMIT #{limit}")
    List<RuleExecutionLogEntity> findByCardId(
            @Param("tableName") String tableName,
            @Param("cardId") String cardId,
            @Param("limit") int limit);

    /**
     * 根据时间范围查询（按执行时间降序）
     */
    @Select("SELECT * FROM ${tableName} WHERE execution_time BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY execution_time DESC LIMIT #{limit}")
    List<RuleExecutionLogEntity> findByTimeRange(
            @Param("tableName") String tableName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit);

    /**
     * 根据状态查询（按执行时间降序）
     */
    @Select("SELECT * FROM ${tableName} WHERE status = #{status} ORDER BY execution_time DESC LIMIT #{limit}")
    List<RuleExecutionLogEntity> findByStatus(
            @Param("tableName") String tableName,
            @Param("status") String status,
            @Param("limit") int limit);

    /**
     * 删除指定时间之前的日志
     */
    @Delete("DELETE FROM ${tableName} WHERE created_at < #{beforeTime}")
    int deleteByCreatedAtBefore(@Param("tableName") String tableName, @Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 统计规则执行次数
     */
    @Select("SELECT COUNT(*) FROM ${tableName} WHERE rule_id = #{ruleId}")
    long countByRuleId(@Param("tableName") String tableName, @Param("ruleId") String ruleId);

    /**
     * 统计时间范围内失败次数
     */
    @Select("SELECT COUNT(*) FROM ${tableName} WHERE rule_id = #{ruleId} AND status = 'FAILED' " +
            "AND execution_time BETWEEN #{startTime} AND #{endTime}")
    long countFailedByRuleId(
            @Param("tableName") String tableName,
            @Param("ruleId") String ruleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 动态创建执行日志表
     */
    @Update("CREATE TABLE IF NOT EXISTS ${tableName} (" +
            "id VARCHAR(32) NOT NULL COMMENT '日志ID', " +
            "rule_id VARCHAR(32) COMMENT '规则ID', " +
            "rule_name VARCHAR(200) COMMENT '规则名称', " +
            "card_id VARCHAR(32) COMMENT '触发卡片ID', " +
            "trigger_event VARCHAR(50) COMMENT '触发事件类型', " +
            "operator_id VARCHAR(32) COMMENT '操作人ID', " +
            "execution_time DATETIME(3) COMMENT '执行时间', " +
            "duration_ms INT COMMENT '执行耗时（毫秒）', " +
            "status VARCHAR(20) COMMENT '执行状态', " +
            "affected_card_ids TEXT COMMENT '受影响的卡片ID列表（JSON）', " +
            "action_results TEXT COMMENT '各动作执行结果（JSON）', " +
            "error_message TEXT COMMENT '错误信息', " +
            "trace_id VARCHAR(64) COMMENT '追踪ID', " +
            "created_at DATETIME COMMENT '创建时间', " +
            "PRIMARY KEY (id), " +
            "INDEX idx_rule_id (rule_id), " +
            "INDEX idx_card_id (card_id), " +
            "INDEX idx_execution_time (execution_time), " +
            "INDEX idx_status (status), " +
            "INDEX idx_trace_id (trace_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则执行日志表'")
    void createTable(@Param("tableName") String tableName);

    /**
     * 检查表是否存在
     */
    @Select("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = #{tableName}")
    int tableExists(@Param("tableName") String tableName);

    /**
     * 分页查询执行日志
     */
    @Select("<script>" +
            "SELECT * FROM ${tableName} WHERE 1=1 " +
            "<if test='ruleIds != null and ruleIds.size() > 0'>" +
            "AND rule_id IN <foreach collection='ruleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "</if>" +
            "<if test='statuses != null and statuses.size() > 0'>" +
            "AND status IN <foreach collection='statuses' item='s' open='(' separator=',' close=')'>#{s}</foreach> " +
            "</if>" +
            "<if test='startTime != null'>AND execution_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'>AND execution_time &lt;= #{endTime} </if>" +
            "<choose>" +
            "<when test='sortAsc'>ORDER BY execution_time ASC </when>" +
            "<otherwise>ORDER BY execution_time DESC </otherwise>" +
            "</choose>" +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<RuleExecutionLogEntity> searchWithPaging(
            @Param("tableName") String tableName,
            @Param("ruleIds") List<String> ruleIds,
            @Param("statuses") List<String> statuses,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortAsc") boolean sortAsc);

    /**
     * 统计符合条件的日志数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM ${tableName} WHERE 1=1 " +
            "<if test='ruleIds != null and ruleIds.size() > 0'>" +
            "AND rule_id IN <foreach collection='ruleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "</if>" +
            "<if test='statuses != null and statuses.size() > 0'>" +
            "AND status IN <foreach collection='statuses' item='s' open='(' separator=',' close=')'>#{s}</foreach> " +
            "</if>" +
            "<if test='startTime != null'>AND execution_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'>AND execution_time &lt;= #{endTime} </if>" +
            "</script>")
    long countWithConditions(
            @Param("tableName") String tableName,
            @Param("ruleIds") List<String> ruleIds,
            @Param("statuses") List<String> statuses,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询所有不同的规则ID和名称
     */
    @Select("SELECT DISTINCT rule_id, rule_name FROM ${tableName} WHERE rule_id IS NOT NULL ORDER BY rule_name")
    List<RuleExecutionLogEntity> findDistinctRules(@Param("tableName") String tableName);
}
