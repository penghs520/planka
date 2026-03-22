package cn.planka.schema.controller;

import cn.planka.api.schema.request.detailtemplate.CardDetailTemplateCopyRequest;
import cn.planka.common.result.Result;
import cn.planka.domain.schema.definition.template.CardDetailTemplateDefinition;
import cn.planka.schema.dto.TemplateListItemVO;
import cn.planka.schema.service.cardtype.CardDetailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 卡片详情页模板 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/schemas/card-detail-templates")
@RequiredArgsConstructor
public class CardDetailTemplateController {

    private final CardDetailTemplateService templateService;

    /**
     * 查询模板列表
     *
     * @param orgId      组织ID
     * @param cardTypeId __PLANKA_EINST__ID（可选，用于筛选）
     * @return 模板列表
     */
    @GetMapping
    public Result<List<TemplateListItemVO>> list(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(name = "cardTypeId", required = false) String cardTypeId) {
        return templateService.list(orgId, cardTypeId);
    }

    /**
     * 根据__PLANKA_EINST__ID获取模板列表
     *
     * @param cardTypeId __PLANKA_EINST__ID
     * @return 模板定义列表
     */
    @GetMapping("/by-card-type/{cardTypeId}")
    public Result<List<CardDetailTemplateDefinition>> getByCardType(
            @PathVariable("cardTypeId") String cardTypeId) {
        return templateService.getByCardType(cardTypeId);
    }

    /**
     * 复制模板
     *
     * @param templateId 源模板ID
     * @param operatorId 操作人ID（从请求头获取）
     * @return 新模板定义
     */
    @PostMapping("/{templateId}/copy")
    public Result<CardDetailTemplateDefinition> copy(
            @PathVariable("templateId") String templateId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody CardDetailTemplateCopyRequest request) {
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
}
