package dev.planka.card.controller;

import dev.planka.api.card.CardServiceClient;
import dev.planka.api.card.dto.BatchOperationResult;
import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.service.core.CardService;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTitle;
import dev.planka.api.card.request.*;
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

    public CardController(CardService cardService) {
        this.cardService = cardService;
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
        return cardService.findById(CardId.of(cardId), yield, CardId.of(operatorId));
    }

    @Override
    @PostMapping("/find-by-ids")
    public Result<List<CardDTO>> findByIds(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody FindByIdsRequest request) {
        return cardService.findByIds(request.getCardIds(), request.getYield(), CardId.of(operatorId));
    }

    @Override
    @PostMapping("/query")
    public Result<List<CardDTO>> query(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody CardQueryRequest request) {
        return cardService.query(request);
    }

    @Override
    @PostMapping("/page-query")
    public Result<PageResult<CardDTO>> pageQuery(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody CardPageQueryRequest request) {
        return cardService.pageQuery(request);
    }

    @Override
    @PostMapping("/query-ids")
    public Result<List<String>> queryIds(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody CardIdQueryRequest request) {
        return cardService.queryIds(request);
    }

    @Override
    @PostMapping("/count")
    public Result<Integer> count(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody CardCountRequest request) {
        return cardService.count(request);
    }

    @Override
    @PostMapping("/names")
    public Result<Map<String, CardTitle>> queryCardNames(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody List<String> cardIds) {
        return cardService.queryCardNames(cardIds, CardId.of(operatorId));
    }
}
