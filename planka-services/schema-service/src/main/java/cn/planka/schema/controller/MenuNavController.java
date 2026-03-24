package cn.planka.schema.controller;

import cn.planka.api.schema.vo.menu.MenuNavTreeVO;
import cn.planka.common.result.Result;
import cn.planka.schema.service.menu.MenuNavService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 侧栏菜单树（Schema 菜单分组 + 视图）
 */
@RestController
@RequestMapping("/api/v1/schemas/menus")
@RequiredArgsConstructor
public class MenuNavController {

    private final MenuNavService menuNavService;

    @GetMapping("/nav")
    public Result<MenuNavTreeVO> nav(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String operatorMemberCardId,
            @RequestParam(name = "cascadeRelationNodeId", required = false) String cascadeRelationNodeId) {
        return Result.success(menuNavService.buildNavTree(orgId, operatorMemberCardId, cascadeRelationNodeId));
    }
}
