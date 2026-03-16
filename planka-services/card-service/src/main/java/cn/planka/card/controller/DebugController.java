package cn.planka.card.controller;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.request.Yield;
import cn.planka.card.service.core.CardQueryService;
import cn.planka.common.result.Result;
import cn.planka.domain.card.CardId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Debug 控制器
 * <p>
 * 提供免认证的调试接口，用于开发和排查问题
 */
@RestController
@RequestMapping("/api/v1/cards/debug")
@RequiredArgsConstructor
public class DebugController {

    private final CardQueryService cardQueryService;

    /**
     * 根据 ID 查询卡片详情（免认证）
     * <p>
     * 用于调试场景，返回完整的卡片数据
     *
     * @param cardId 卡片 ID
     * @return 卡片详情
     */
    @GetMapping("/{cardId}")
    public Result<CardDTO> getCardById(@PathVariable("cardId") String cardId) {
        // 使用系统调试操作员 ID（免认证场景下使用）
        return cardQueryService.findById(CardId.of(cardId), Yield.all(), CardId.of("system-debug"));
    }
}
