package dev.planka.card.service.rule.log;

import dev.planka.api.card.dto.RuleExecutionLogFiltersDTO;
import dev.planka.api.card.request.RuleExecutionLogSearchRequest;
import dev.planka.common.result.PageResult;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.BizRuleId;
import dev.planka.domain.schema.definition.rule.RuleExecutionLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 规则执行日志仓储接口
 */
public interface RuleExecutionLogRepository {

    /**
     * 保存执行日志
     */
    void save(RuleExecutionLog log);

    /**
     * 批量保存执行日志
     */
    void saveAll(List<RuleExecutionLog> logs);

    /**
     * 根据ID查询日志
     */
    Optional<RuleExecutionLog> findById(String id, CardTypeId cardTypeId);

    /**
     * 根据规则ID查询日志
     */
    List<RuleExecutionLog> findByRuleId(BizRuleId ruleId, CardTypeId cardTypeId, int limit);

    /**
     * 根据卡片ID查询日志
     */
    List<RuleExecutionLog> findByCardId(CardId cardId, CardTypeId cardTypeId, int limit);

    /**
     * 根据时间范围查询日志
     */
    List<RuleExecutionLog> findByTimeRange(CardTypeId cardTypeId, LocalDateTime startTime,
                                            LocalDateTime endTime, int limit);

    /**
     * 根据状态查询日志
     */
    List<RuleExecutionLog> findByStatus(CardTypeId cardTypeId,
                                         RuleExecutionLog.ExecutionStatus status, int limit);

    /**
     * 删除指定时间之前的日志
     */
    int deleteByCreatedAtBefore(CardTypeId cardTypeId, LocalDateTime beforeTime);

    /**
     * 统计规则执行次数
     */
    long countByRuleId(BizRuleId ruleId, CardTypeId cardTypeId);

    /**
     * 统计失败次数
     */
    long countFailedByRuleId(BizRuleId ruleId, CardTypeId cardTypeId,
                              LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 分页搜索执行日志
     */
    PageResult<RuleExecutionLog> search(CardTypeId cardTypeId, RuleExecutionLogSearchRequest request);

    /**
     * 获取过滤选项
     */
    RuleExecutionLogFiltersDTO getFilters(CardTypeId cardTypeId);
}
