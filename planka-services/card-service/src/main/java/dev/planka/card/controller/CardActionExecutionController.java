package dev.planka.card.controller;

import dev.planka.card.controller.request.ExecuteActionRequest;
import dev.planka.card.service.action.ActionExecutionResult;
import dev.planka.card.service.action.CardActionExecutionService;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.schema.CardActionId;
import dev.planka.domain.schema.definition.action.CardActionConfigDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 卡片动作执行控制器
 * <p>
 * 提供卡片动作的执行接口。
 */
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardActionExecutionController {

    private final CardActionExecutionService cardActionExecutionService;

    /**
     * 获取卡片可用的动作列表
     *
     * @param cardId     卡片ID
     * @param operatorId 操作人ID
     * @return 可用的动作配置列表
     */
    @GetMapping("/{cardId}/available-actions")
    public Result<List<CardActionConfigDefinition>> getAvailableActions(
            @PathVariable("cardId") String cardId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return cardActionExecutionService.getAvailableActions(
                CardId.of(cardId),
                CardId.of(operatorId)
        );
    }

    /**
     * 执行卡片动作
     *
     * @param actionId   动作配置ID
     * @param cardId     卡片ID
     * @param request    执行请求（包含用户输入的字段值）
     * @param operatorId 操作人ID
     * @return 执行结果
     */
    @PostMapping("/actions/{actionId}/execute")
    public Result<ActionExecutionResult> execute(
            @PathVariable("actionId") String actionId,
            @RequestParam("cardId") String cardId,
            @RequestBody(required = false) ExecuteActionRequest request,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return cardActionExecutionService.execute(
                CardActionId.of(actionId),
                CardId.of(cardId),
                CardId.of(operatorId),
                request != null ? request.getUserInputs() : null
        );
    }
}
