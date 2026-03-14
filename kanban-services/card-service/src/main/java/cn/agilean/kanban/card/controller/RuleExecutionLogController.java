package cn.agilean.kanban.card.controller;

import cn.agilean.kanban.api.card.dto.RuleExecutionLogDTO;
import cn.agilean.kanban.api.card.dto.RuleExecutionLogFiltersDTO;
import cn.agilean.kanban.api.card.request.RuleExecutionLogSearchRequest;
import cn.agilean.kanban.card.service.rule.log.RuleExecutionLogService;
import cn.agilean.kanban.common.result.PageResult;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.card.CardTypeId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 规则执行日志控制器
 */
@RestController
@RequestMapping("/api/v1/biz-rules/execution-logs")
@RequiredArgsConstructor
public class RuleExecutionLogController {

    private final RuleExecutionLogService logService;

    /**
     * 分页搜索执行日志
     */
    @PostMapping("/{cardTypeId}/search")
    public Result<PageResult<RuleExecutionLogDTO>> search(
            @PathVariable("cardTypeId") String cardTypeId,
            @RequestBody RuleExecutionLogSearchRequest request) {
        PageResult<RuleExecutionLogDTO> result = logService.search(CardTypeId.of(cardTypeId), request);
        return Result.success(result);
    }

    /**
     * 获取过滤选项
     */
    @GetMapping("/{cardTypeId}/filters")
    public Result<RuleExecutionLogFiltersDTO> getFilters(
            @PathVariable("cardTypeId") String cardTypeId) {
        RuleExecutionLogFiltersDTO filters = logService.getFilters(CardTypeId.of(cardTypeId));
        return Result.success(filters);
    }
}
