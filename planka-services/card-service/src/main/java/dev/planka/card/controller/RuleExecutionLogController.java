package dev.planka.card.controller;

import dev.planka.api.card.dto.RuleExecutionLogDTO;
import dev.planka.api.card.dto.RuleExecutionLogFiltersDTO;
import dev.planka.api.card.request.RuleExecutionLogSearchRequest;
import dev.planka.card.service.rule.log.RuleExecutionLogService;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
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
