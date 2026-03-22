package cn.planka.schema.controller;

import cn.planka.api.schema.vo.cardtype.CardTypeOptionVO;
import cn.planka.api.schema.vo.cardtype.CardTypeVO;
import cn.planka.common.result.Result;
import cn.planka.schema.service.cardtype.CardTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * __PLANKA_EINST__ REST 控制器
 * <p>
 * 提供__PLANKA_EINST__的列表查询接口，返回 VO 格式数据。
 */
@RestController
@RequestMapping("/api/v1/schemas/card-types")
@RequiredArgsConstructor
public class CardTypeController {

    private final CardTypeService cardTypeService;

    /**
     * 查询__PLANKA_EINST__列表
     *
     * @param orgId 组织 ID（从请求头获取）
     * @return __PLANKA_EINST__列表
     */
    @GetMapping
    public Result<List<CardTypeVO>> list(@RequestHeader("X-Org-Id") String orgId) {
        return cardTypeService.listCardTypes(orgId);
    }

    /**
     * 查询__PLANKA_EINST__选项列表（用于下拉框）
     *
     * @param orgId 组织 ID（从请求头获取）
     * @return __PLANKA_EINST__选项列表
     */
    @GetMapping("/options")
    public Result<List<CardTypeOptionVO>> listOptions(@RequestHeader("X-Org-Id") String orgId) {
        return cardTypeService.listCardTypeOptions(orgId);
    }

    /**
     * 根据 ID 获取__PLANKA_EINST__详情
     *
     * @param cardTypeId __PLANKA_EINST__ ID
     * @return __PLANKA_EINST__详情
     */
    @GetMapping("/{cardTypeId}")
    public Result<CardTypeVO> getById(
            @PathVariable("cardTypeId") String cardTypeId) {
        return cardTypeService.getCardTypeById(cardTypeId);
    }

    /**
     * 查询继承指定特征类型的__PLANKA_EINST__选项列表
     *
     * @param orgId        组织 ID（从请求头获取）
     * @param parentTypeId 父类型 ID（特征类型）
     * @return __PLANKA_EINST__选项列表
     */
    @GetMapping("/by-parent")
    public Result<List<CardTypeOptionVO>> listByParentTypeId(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam("parentTypeId") String parentTypeId) {
        return cardTypeService.listConcreteTypesByParentTypeId(parentTypeId);
    }
}
