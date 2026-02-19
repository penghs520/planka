package dev.planka.schema.controller;

import dev.planka.api.schema.request.UpdateSchemaRequest;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.definition.action.CardActionConfigDefinition;
import dev.planka.schema.service.action.CardActionConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 卡片动作配置控制器
 * <p>
 * 提供卡片动作配置的查询和更新接口。
 * 创建、删除操作通过 SchemaCommonController 统一处理。
 */
@RestController
@RequestMapping("/api/v1/schemas/card-actions")
@RequiredArgsConstructor
public class CardActionConfigController {

    private final CardActionConfigService cardActionConfigService;

    /**
     * 根据卡片类型ID获取所有动作配置
     *
     * @param cardTypeId 卡片类型ID
     * @return 动作配置列表
     */
    @GetMapping("/by-card-type/{cardTypeId}")
    public Result<List<CardActionConfigDefinition>> getByCardTypeId(
            @PathVariable("cardTypeId") String cardTypeId) {
        return cardActionConfigService.getByCardTypeId(CardTypeId.of(cardTypeId));
    }

    /**
     * 获取组织下所有卡片动作配置
     *
     * @param orgId 组织ID
     * @return 动作配置列表
     */
    @GetMapping
    public Result<List<CardActionConfigDefinition>> getByOrgId(
            @RequestHeader("X-Org-Id") String orgId) {
        return cardActionConfigService.getByOrgId(orgId);
    }

    /**
     * 更新动作配置
     * <p>
     * 支持更新内置动作和自定义动作。
     * 对于未持久化的内置动作（ID 以 builtin: 开头），首次编辑会自动创建持久化记录。
     *
     * @param id 动作ID
     * @param orgId 组织ID
     * @param operatorId 操作者ID
     * @param request 更新请求
     * @return 更新后的动作配置
     */
    @PutMapping("/{id}")
    public Result<CardActionConfigDefinition> update(
            @PathVariable("id") String id,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-User-Id") String operatorId,
            @RequestBody UpdateSchemaRequest request) {
        CardActionConfigDefinition definition = (CardActionConfigDefinition) request.getDefinition();
        return cardActionConfigService.updateAction(id, orgId, operatorId, definition, request.getExpectedVersion());
    }
}
