package cn.agilean.kanban.schema.controller;

import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.card.CardTypeId;
import cn.agilean.kanban.domain.schema.definition.rule.BizRuleDefinition;
import cn.agilean.kanban.schema.service.rule.BizRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务规则控制器
 * <p>
 * 提供业务规则的查询接口。
 * 创建、更新、删除操作通过 SchemaCommonController 统一处理。
 */
@RestController
@RequestMapping("/api/v1/schemas/biz-rules")
@RequiredArgsConstructor
public class BizRuleController {

    private final BizRuleService bizRuleService;

    /**
     * 根据卡片类型ID获取所有业务规则
     *
     * @param cardTypeId 卡片类型ID
     * @return 业务规则列表
     */
    @GetMapping("/by-card-type/{cardTypeId}")
    public Result<List<BizRuleDefinition>> getByCardTypeId(
            @PathVariable("cardTypeId") String cardTypeId) {
        return bizRuleService.getByCardTypeId(CardTypeId.of(cardTypeId));
    }

    /**
     * 获取组织下所有业务规则
     *
     * @param orgId 组织ID
     * @return 业务规则列表
     */
    @GetMapping
    public Result<List<BizRuleDefinition>> getByOrgId(
            @RequestHeader("X-Org-Id") String orgId) {
        return bizRuleService.getByOrgId(orgId);
    }
}
