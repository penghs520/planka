package dev.planka.card.controller;

import dev.planka.api.card.dto.CardDetailResponse;
import dev.planka.card.service.CardDetailService;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import org.springframework.web.bind.annotation.*;

/**
 * 卡片详情页接口
 */
@RestController
@RequestMapping("/api/v1/cards")
public class CardDetailController {

    private final CardDetailService cardDetailService;

    public CardDetailController(CardDetailService cardDetailService) {
        this.cardDetailService = cardDetailService;
    }

    /**
     * 获取卡片详情（含模板和字段配置）
     * <p>
     * 返回卡片数据、详情模板配置和基础信息标签页的字段配置
     */
    @GetMapping("/{cardId}/detail")
    public Result<CardDetailResponse> getCardDetail(
            @PathVariable("cardId") String cardId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return cardDetailService.getCardDetail(CardId.of(cardId), operatorId);
    }

}
