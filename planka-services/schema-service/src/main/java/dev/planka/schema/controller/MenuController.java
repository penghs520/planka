package dev.planka.schema.controller;

import dev.planka.api.schema.request.menu.AddViewToGroupRequest;
import dev.planka.api.schema.request.menu.ReorderViewsRequest;
import dev.planka.api.schema.vo.menu.MenuGroupVO;
import dev.planka.api.schema.vo.menu.MenuTreeVO;
import dev.planka.api.schema.vo.view.ViewListItemVO;
import dev.planka.common.result.Result;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.menu.MenuGroupDefinition;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.MenuService;
import dev.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单分组 REST 控制器
 * <p>
 * 提供菜单树查询和视图分组管理接口。
 * 分组的基础 CRUD 操作委托给 SchemaController 处理。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/schemas/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final SchemaRepository schemaRepository;
    private final SchemaQuery schemaQuery;

    /**
     * 获取完整菜单树（包含分组和视图）
     *
     * @param orgId  组织ID
     * @param userId 当前用户ID（可选，用于权限过滤）
     * @return 菜单树
     */
    @GetMapping("/tree")
    public Result<MenuTreeVO> getMenuTree(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(name = "userId", required = false) String userId) {
        return menuService.getMenuTree(orgId, userId);
    }

    /**
     * 获取所有菜单分组（平铺列表）
     *
     * @param orgId 组织ID
     * @return 分组列表
     */
    @GetMapping("/groups")
    public Result<List<MenuGroupVO>> listGroups(@RequestHeader("X-Org-Id") String orgId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.query(orgId, SchemaType.MENU);
        List<MenuGroupVO> groups = schemas.stream()
                .filter(s -> s instanceof MenuGroupDefinition)
                .map(s -> toGroupVO((MenuGroupDefinition) s))
                .toList();
        return Result.success(groups);
    }

    /**
     * 将视图添加到分组
     *
     * @param groupId 分组ID
     * @param operatorId 操作人ID（从请求头获取）
     * @param request 请求体
     * @return 操作结果
     */
    @PostMapping("/groups/{groupId}/views")
    public Result<Void> addViewToGroup(
            @PathVariable("groupId") String groupId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody AddViewToGroupRequest request) {
        return menuService.addViewToGroup(
                groupId,
                operatorId,
                request.getViewId(),
                request.getSortOrder(),
                request.getDisplayName()
        );
    }

    /**
     * 从分组移除视图
     *
     * @param groupId 分组ID
     * @param viewId  视图ID
     * @param operatorId 操作人ID（从请求头获取）
     * @return 操作结果
     */
    @DeleteMapping("/groups/{groupId}/views/{viewId}")
    public Result<Void> removeViewFromGroup(
            @PathVariable("groupId") String groupId,
            @PathVariable("viewId") String viewId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return menuService.removeViewFromGroup(groupId, viewId, operatorId);
    }

    /**
     * 重新排序分组内的视图
     *
     * @param groupId 分组ID
     * @param operatorId 操作人ID（从请求头获取）
     * @param request 请求体
     * @return 操作结果
     */
    @PutMapping("/groups/{groupId}/views/reorder")
    public Result<Void> reorderViews(
            @PathVariable("groupId") String groupId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody ReorderViewsRequest request) {
        return menuService.reorderViews(groupId, operatorId, request.getViewIds());
    }

    /**
     * 获取未分组的视图列表
     *
     * @param orgId 组织ID
     * @return 未分组视图列表
     */
    @GetMapping("/ungrouped-views")
    public Result<List<ViewListItemVO>> getUngroupedViews(@RequestHeader("X-Org-Id") String orgId) {
        return menuService.getUngroupedViews(orgId);
    }

    /**
     * 将 MenuGroupDefinition 转换为 VO
     */
    private MenuGroupVO toGroupVO(MenuGroupDefinition group) {
        return MenuGroupVO.builder()
                .id(group.getId().value())
                .orgId(group.getOrgId())
                .name(group.getName())
                .description(group.getDescription())
                .parentId(group.getParentId() != null ? group.getParentId().value() : null)
                .icon(group.getIcon())
                .sortOrder(group.getSortOrder())
                .viewCount(group.getViewItems() != null ? group.getViewItems().size() : 0)
                .enabled(group.isEnabled())
                .contentVersion(group.getContentVersion())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
}
