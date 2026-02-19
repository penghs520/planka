package dev.planka.card.controller;

import dev.planka.api.card.dto.StructureNodeDTO;
import dev.planka.api.card.request.StructureOptionsRequest;
import dev.planka.card.service.structure.StructureOptionsService;
import dev.planka.common.result.Result;
import org.springframework.web.bind.annotation.*;

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
     * 查询架构属性的树形可选项
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
