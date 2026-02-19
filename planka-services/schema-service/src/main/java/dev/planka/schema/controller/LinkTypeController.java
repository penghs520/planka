package dev.planka.schema.controller;

import dev.planka.api.schema.request.linktype.CreateLinkTypeRequest;
import dev.planka.api.schema.request.linktype.UpdateLinkTypeRequest;
import dev.planka.api.schema.vo.linktype.LinkTypeOptionVO;
import dev.planka.api.schema.vo.linktype.LinkTypeVO;
import dev.planka.common.result.Result;
import dev.planka.domain.link.LinkPosition;
import dev.planka.schema.service.linktype.LinkTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关联类型 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/schemas/link-types")
@RequiredArgsConstructor
public class LinkTypeController {

    private final LinkTypeService linkTypeService;

    // ========== 关联类型 CRUD ==========

    /**
     * 查询关联类型列表
     *
     * @param orgId 组织 ID（从请求头获取）
     */
    @GetMapping
    public Result<List<LinkTypeVO>> list(@RequestHeader("X-Org-Id") String orgId) {
        return linkTypeService.listLinkTypes(orgId);
    }

    /**
     * 查询关联类型选项列表（用于下拉框）
     *
     * @param orgId 组织 ID（从请求头获取）
     */
    @GetMapping("/options")
    public Result<List<LinkTypeOptionVO>> listOptions(@RequestHeader("X-Org-Id") String orgId) {
        return linkTypeService.listLinkTypeOptions(orgId);
    }

    /**
     * 根据 ID 获取关联类型详情
     */
    @GetMapping("/{linkTypeId}")
    public Result<LinkTypeVO> getById(@PathVariable("linkTypeId") String linkTypeId) {
        return linkTypeService.getLinkTypeById(linkTypeId);
    }

    /**
     * 创建关联类型
     *
     * @param orgId      组织 ID
     * @param operatorId 操作人 ID
     */
    @PostMapping
    public Result<LinkTypeVO> create(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @Valid @RequestBody CreateLinkTypeRequest request) {
        return linkTypeService.createLinkType(orgId, operatorId, request);
    }

    /**
     * 更新关联类型
     *
     * @param linkTypeId 关联类型 ID
     * @param operatorId 操作人 ID
     */
    @PutMapping("/{linkTypeId}")
    public Result<LinkTypeVO> update(
            @PathVariable("linkTypeId") String linkTypeId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @Valid @RequestBody UpdateLinkTypeRequest request) {
        return linkTypeService.updateLinkType(linkTypeId, operatorId, request);
    }

    /**
     * 删除关联类型
     *
     * @param linkTypeId 关联类型 ID
     * @param operatorId 操作人 ID
     */
    @DeleteMapping("/{linkTypeId}")
    public Result<Void> delete(
            @PathVariable("linkTypeId") String linkTypeId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return linkTypeService.deleteLinkType(linkTypeId, operatorId);
    }

    // ========== 查询可用关联类型 ==========

    /**
     * 查询卡片类型可用的关联类型
     */
    @GetMapping("/available-for-card-type/{cardTypeId}")
    public Result<List<LinkTypeOptionVO>> getAvailableLinkTypes(
            @PathVariable("cardTypeId") String cardTypeId,
            @RequestParam(required = false) LinkPosition position) {
        return linkTypeService.getAvailableLinkTypes(cardTypeId, position);
    }
}
