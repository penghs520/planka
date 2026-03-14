package cn.agilean.kanban.card.controller;

import cn.agilean.kanban.card.controller.request.ExecuteActionRequest;
import cn.agilean.kanban.card.service.action.ActionExecutionResult;
import cn.agilean.kanban.card.service.action.CardActionExecutionService;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.domain.schema.CardActionId;
import cn.agilean.kanban.domain.schema.definition.action.CardActionConfigDefinition;
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
