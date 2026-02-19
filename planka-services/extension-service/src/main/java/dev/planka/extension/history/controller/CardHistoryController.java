package dev.planka.extension.history.controller;

import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.domain.history.OperationType;
import dev.planka.extension.history.dto.CardHistoryRecordVO;
import dev.planka.extension.history.service.CardHistoryService;
import dev.planka.extension.history.service.CardHistoryService.CardHistoryFilters;
import dev.planka.extension.history.service.CardHistoryService.SearchHistoryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 卡片操作历史控制器
 */
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class CardHistoryController {

    private final CardHistoryService cardHistoryService;

    /**
     * 查询单卡片历史（简单查询）
     * GET /api/v1/history/cards/{cardTypeId}/{cardId}
     */
    @GetMapping("/cards/{cardTypeId}/{cardId}")
    public Result<PageResult<CardHistoryRecordVO>> getCardHistory(
            @PathVariable("cardTypeId") String cardTypeId,
            @PathVariable("cardId") Long cardId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return cardHistoryService.getCardHistory(cardTypeId, cardId, page, size);
    }

    /**
     * 查询单卡片历史（多维度搜索）
     * POST /api/v1/history/cards/{cardTypeId}/{cardId}/search
     */
    @PostMapping("/cards/{cardTypeId}/{cardId}/search")
    public Result<PageResult<CardHistoryRecordVO>> searchCardHistory(
            @PathVariable("cardTypeId") String cardTypeId,
            @PathVariable("cardId") Long cardId,
            @RequestBody CardHistorySearchRequest request) {

        SearchHistoryQuery query = new SearchHistoryQuery(
                cardTypeId,
                cardId,
                request.operationTypes(),
                request.operatorIds(),
                request.sourceTypes(),
                request.startTime(),
                request.endTime(),
                request.sortAsc() != null && request.sortAsc(),
                request.page() != null ? request.page() : 1,
                request.size() != null ? request.size() : 20
        );

        return cardHistoryService.searchCardHistory(query);
    }

    /**
     * 获取可用的筛选选项（用于前端下拉框）
     * GET /api/v1/history/cards/{cardTypeId}/{cardId}/filters
     */
    @GetMapping("/cards/{cardTypeId}/{cardId}/filters")
    public Result<CardHistoryFilters> getAvailableFilters(
            @PathVariable("cardTypeId") String cardTypeId,
            @PathVariable("cardId") Long cardId) {
        return cardHistoryService.getAvailableFilters(cardTypeId, cardId);
    }

    /**
     * 卡片历史搜索请求
     *
     * @param sortAsc true=正序（最早的在前），false/null=倒序（最新的在前，默认）
     */
    public record CardHistorySearchRequest(
            List<OperationType> operationTypes,
            List<String> operatorIds,
            List<String> sourceTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Boolean sortAsc,
            Integer page,
            Integer size
    ) {}
}
