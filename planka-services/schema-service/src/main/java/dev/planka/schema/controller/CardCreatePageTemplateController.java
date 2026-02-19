package dev.planka.schema.controller;

import dev.planka.common.result.Result;
import dev.planka.domain.schema.definition.template.CardCreatePageTemplateDefinition;
import dev.planka.schema.dto.CreatePageFormVO;
import dev.planka.schema.dto.CreatePageTemplateListItemVO;
import dev.planka.schema.service.cardtype.CardCreatePageTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 卡片新建页模板 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/schemas/card-create-page-templates")
@RequiredArgsConstructor
public class CardCreatePageTemplateController {

    private final CardCreatePageTemplateService templateService;

    /**
     * 查询模板列表
     *
     * @param orgId      组织ID
     * @param cardTypeId 卡片类型ID（可选，用于筛选）
     * @return 模板列表
     */
    @GetMapping
    public Result<List<CreatePageTemplateListItemVO>> list(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(name = "cardTypeId", required = false) String cardTypeId) {
        return templateService.list(orgId, cardTypeId);
    }

    /**
     * 根据卡片类型ID获取模板列表
     *
     * @param cardTypeId 卡片类型ID
     * @return 模板定义列表
     */
    @GetMapping("/by-card-type/{cardTypeId}")
    public Result<List<CardCreatePageTemplateDefinition>> getByCardType(
            @PathVariable("cardTypeId") String cardTypeId) {
        return templateService.getByCardType(cardTypeId);
    }

    /**
     * 获取卡片类型的默认新建页模板
     *
     * @param cardTypeId 卡片类型ID
     * @return 默认模板定义
     */
    @GetMapping("/by-card-type/{cardTypeId}/default")
    public Result<CardCreatePageTemplateDefinition> getDefaultByCardType(
            @PathVariable("cardTypeId") String cardTypeId) {
        return templateService.getDefaultByCardType(cardTypeId);
    }

    /**
     * 复制模板
     *
     * @param templateId 源模板ID
     * @param operatorId 操作人ID（从请求头获取）
     * @return 新模板定义
     */
    @PostMapping("/{templateId}/copy")
    public Result<CardCreatePageTemplateDefinition> copy(
            @PathVariable("templateId") String templateId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody CopyTemplateRequest request) {
        return templateService.copy(templateId, operatorId, request.newName());
    }

    /**
     * 设置为默认模板
     *
     * @param templateId 模板ID
     * @param operatorId 操作人ID（从请求头获取）
     * @return 操作结果
     */
    @PostMapping("/{templateId}/set-default")
    public Result<Void> setDefault(
            @PathVariable("templateId") String templateId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return templateService.setDefault(templateId, operatorId);
    }

    /**
     * 获取新建页表单配置（运行时 API）
     * <p>
     * 用于前端动态渲染卡片创建表单，返回模板布局、字段配置和默认值。
     *
     * @param cardTypeId 卡片类型ID
     * @return 表单配置 VO
     */
    @GetMapping("/form")
    public Result<CreatePageFormVO> getForm(@RequestParam("cardTypeId") String cardTypeId) {
        return templateService.getForm(cardTypeId);
    }

    /**
     * 复制模板请求
     */
    public record CopyTemplateRequest(String newName) {
    }
}

