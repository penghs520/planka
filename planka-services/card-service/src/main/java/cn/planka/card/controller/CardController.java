package cn.planka.card.controller;

import cn.planka.api.card.CardServiceClient;
import cn.planka.api.card.dto.BatchOperationResult;
import cn.planka.api.card.dto.CardDTO;
import cn.planka.card.service.core.CardQueryService;
import cn.planka.card.service.core.CardService;
import cn.planka.common.result.PageResult;
import cn.planka.common.result.Result;
import cn.planka.domain.card.CardId;
import cn.planka.domain.card.CardTitle;
import cn.planka.api.card.request.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 卡片控制器
 * <p>
 * 实现 CardServiceClient 接口，提供卡片 CRUD 和查询 REST API
 */
@RestController
@RequestMapping("/api/v1/cards")
public class CardController implements CardServiceClient {

    private final CardService cardService;
    private final CardQueryService cardQueryService;

    public CardController(CardService cardService, CardQueryService cardQueryService) {
        this.cardService = cardService;
        this.cardQueryService = cardQueryService;
    }

    // ==================== 写操作 ====================

    @Override
    @PostMapping
    public Result<CardId> create(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody CreateCardRequest request) {
        return cardService.create(request, CardId.of(operatorId));
    }

    @Override
    @PutMapping
    public Result<Void> update(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody UpdateCardRequest request) {
        return cardService.update(request, CardId.of(operatorId));
    }

    @Override
    @DeleteMapping("/{cardId}")
    public Result<Void> discard(
            @PathVariable("cardId") String cardId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestParam(value = "discardReason", required = false) String discardReason) {
        return cardService.discard(CardId.of(cardId), discardReason, CardId.of(operatorId));
    }

    @Override
    @PostMapping("/batch")
    public Result<BatchOperationResult> batchCreate(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody List<CreateCardRequest> requests) {
        return cardService.batchCreate(requests, CardId.of(operatorId));
    }

    @Override
    @DeleteMapping("/batch")
    public Result<Void> batchDiscard(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody BatchOperationRequest request) {
        return cardService.batchDiscard(request.getCardIds(), request.getDiscardReason(), CardId.of(operatorId));
    }

    @Override
    @PostMapping("/archive")
    public Result<Void> batchArchive(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody BatchOperationRequest request) {
        return cardService.batchArchive(request.getCardIds(), CardId.of(operatorId));
    }

    @Override
    @PostMapping("/restore")
    public Result<Void> batchRestore(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody BatchOperationRequest request) {
        return cardService.batchRestore(request.getCardIds(), CardId.of(operatorId));
    }

    // ==================== 价值流移动操作 ====================

    /**
     * 移动卡片到新状态
     */
    @PostMapping("/move")
    public Result<Void> move(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody MoveCardRequest request) {
        return cardService.move(request, CardId.of(operatorId));
    }

    /**
     * 批量移动卡片到新状态
     */
    @PostMapping("/batch-move")
    public Result<BatchOperationResult> batchMove(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody BatchMoveCardRequest request) {
        return cardService.batchMove(request, CardId.of(operatorId));
    }

    // ==================== 读操作 ====================

    @Override
    @PostMapping("/{cardId}")
    public Result<CardDTO> findById(
            @PathVariable("cardId") String cardId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody Yield yield) {
        return cardQueryService.findById(CardId.of(cardId), yield, CardId.of(operatorId));
    }

    @Override
    @PostMapping("/find-by-ids")
    public Result<List<CardDTO>> findByIds(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody FindByIdsRequest request) {
        return cardQueryService.findByIds(request.getCardIds(), request.getYield(), CardId.of(operatorId));
    }

    @Override
    @PostMapping("/query")
    public Result<List<CardDTO>> query(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody CardQueryRequest request) {
        ensureOperatorId(request, operatorId);
        return cardQueryService.query(request);
    }

    @Override
    @PostMapping("/page-query")
    public Result<PageResult<CardDTO>> pageQuery(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody CardPageQueryRequest request) {
        ensureOperatorId(request, operatorId);
        return cardQueryService.pageQuery(request);
    }

    @Override
    @PostMapping("/query-ids")
    public Result<List<String>> queryIds(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody CardIdQueryRequest request) {
        return cardQueryService.queryIds(request);
    }

    @Override
    @PostMapping("/count")
    public Result<Integer> count(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody CardCountRequest request) {
        return cardQueryService.count(request);
    }

    @Override
    @PostMapping("/names")
    public Result<Map<String, CardTitle>> queryCardNames(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody List<String> cardIds) {
        return cardQueryService.queryCardNames(cardIds, CardId.of(operatorId));
    }

    /**
     * 查询指定状态下的卡片数量
     */
    @GetMapping("/count-by-status")
    public Result<Integer> countByStatus(
            @RequestParam String statusId,
            @RequestParam String streamId,
            @RequestParam String cardTypeId,
            @RequestHeader("X-Org-Id") String orgId) {
        return cardQueryService.countCardsByStatus(orgId, statusId, streamId, cardTypeId);
    }

    /**
     * 批量更新卡片状态（用于状态迁移）
     */
    @PostMapping("/batch-update-status")
    public Result<Integer> batchUpdateCardStatus(
            @RequestParam String orgId,
            @RequestParam String sourceStatusId,
            @RequestParam String targetStatusId,
            @RequestParam String streamId,
            @RequestParam String cardTypeId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return cardService.batchUpdateCardStatus(orgId, sourceStatusId, targetStatusId, streamId, cardTypeId, CardId.of(operatorId));
    }

    private void ensureOperatorId(CardQueryRequest request, String operatorId) {
        if (request.getQueryContext() == null) {
            request.setQueryContext(new QueryContext());
        }
        request.getQueryContext().setOperatorId(operatorId);
    }
}
