package cn.planka.card.controller;

import cn.planka.api.card.dto.StructureNodeDTO;
import cn.planka.api.card.request.StructureOptionsRequest;
import cn.planka.card.service.structure.StructureOptionsService;
import cn.planka.common.result.Result;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 架构属性可选项控制器
 * <p>
 * 提供架构属性编辑器所需的树形可选项查询 API
 */
@RestController
@RequestMapping("/api/v1/structure-options")
public class StructureOptionsController {

    private final StructureOptionsService structureOptionsService;

    public StructureOptionsController(StructureOptionsService structureOptionsService) {
        this.structureOptionsService = structureOptionsService;
    }

    /**
     * 查询架构树形可选项：{@code structureFieldId}（属性编辑器）与 {@code structureId}（侧栏等）二选一
     *
     * @param operatorId 操作人ID
     * @param orgId      组织ID
     * @param request    查询请求
     * @return 树形节点列表
     */
    @PostMapping
    public Result<List<StructureNodeDTO>> queryOptions(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestBody StructureOptionsRequest request) {
        return structureOptionsService.queryOptions(request, orgId, operatorId);
    }

}
