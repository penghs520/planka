package dev.planka.schema.controller;

import dev.planka.api.schema.vo.cardtype.CardTypeOptionVO;
import dev.planka.api.schema.vo.cardtype.CardTypeVO;
import dev.planka.common.result.Result;
import dev.planka.schema.service.cardtype.CardTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 卡片类型 REST 控制器
 * <p>
 * 提供卡片类型的列表查询接口，返回 VO 格式数据。
 */
@RestController
@RequestMapping("/api/v1/schemas/card-types")
@RequiredArgsConstructor
public class CardTypeController {

    private final CardTypeService cardTypeService;

    /**
     * 查询卡片类型列表
     *
     * @param orgId 组织 ID（从请求头获取）
     * @return 卡片类型列表
     */
    @GetMapping
    public Result<List<CardTypeVO>> list(@RequestHeader("X-Org-Id") String orgId) {
        return cardTypeService.listCardTypes(orgId);
    }

    /**
     * 查询卡片类型选项列表（用于下拉框）
     *
     * @param orgId 组织 ID（从请求头获取）
     * @return 卡片类型选项列表
     */
    @GetMapping("/options")
    public Result<List<CardTypeOptionVO>> listOptions(@RequestHeader("X-Org-Id") String orgId) {
        return cardTypeService.listCardTypeOptions(orgId);
    }

    /**
     * 根据 ID 获取卡片类型详情
     *
     * @param cardTypeId 卡片类型 ID
     * @return 卡片类型详情
     */
    @GetMapping("/{cardTypeId}")
    public Result<CardTypeVO> getById(
            @PathVariable("cardTypeId") String cardTypeId) {
        return cardTypeService.getCardTypeById(cardTypeId);
    }

    /**
     * 查询继承指定属性集的实体类型选项列表
     *
     * @param orgId        组织 ID（从请求头获取）
     * @param parentTypeId 父类型 ID（属性集）
     * @return 卡片类型选项列表
     */
    @GetMapping("/by-parent")
    public Result<List<CardTypeOptionVO>> listByParentTypeId(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam("parentTypeId") String parentTypeId) {
        return cardTypeService.listConcreteTypesByParentTypeId(parentTypeId);
    }
}
