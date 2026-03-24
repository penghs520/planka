package cn.planka.card.controller;

import cn.planka.api.card.dto.CascadeNodeDTO;
import cn.planka.api.card.request.CascadeFieldOptionsRequest;
import cn.planka.card.service.cascadefield.CascadeFieldOptionsService;
import cn.planka.common.result.Result;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 级联属性可选项控制器
 * <p>
 * 提供级联属性编辑器所需的树形可选项查询 API
 */
@RestController
@RequestMapping("/api/v1/cascade-field-options")
public class CascadeFieldOptionsController {

    private final CascadeFieldOptionsService cascadeFieldOptionsService;

    public CascadeFieldOptionsController(CascadeFieldOptionsService cascadeFieldOptionsService) {
        this.cascadeFieldOptionsService = cascadeFieldOptionsService;
    }

    /**
     * 查询级联树形可选项：{@code cascadeFieldId}（属性编辑器）与 {@code cascadeRelationId}（侧栏等）二选一
     *
     * @param operatorId 操作人ID
     * @param orgId      组织ID
     * @param request    查询请求
     * @return 树形节点列表
     */
    @PostMapping
    public Result<List<CascadeNodeDTO>> queryOptions(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestBody CascadeFieldOptionsRequest request) {
        return cascadeFieldOptionsService.queryOptions(request, orgId, operatorId);
    }

}
