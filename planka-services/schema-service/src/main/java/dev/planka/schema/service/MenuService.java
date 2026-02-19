package dev.planka.schema.service;

import dev.planka.api.schema.vo.menu.MenuTreeNodeVO;
import dev.planka.api.schema.vo.menu.MenuTreeVO;
import dev.planka.api.schema.vo.view.ViewListItemVO;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.Result;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.ViewId;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.menu.MenuGroupDefinition;
import dev.planka.domain.schema.definition.view.AbstractViewDefinition;
import dev.planka.domain.schema.definition.view.ListViewDefinition;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单分组业务服务
 * <p>
 * 提供菜单树构建、视图分组管理等功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SchemaRepository schemaRepository;
    private final SchemaQuery schemaQuery;

    /**
     * 获取完整菜单树
     *
     * @param orgId  组织ID
     * @param userId 当前用户ID（用于权限过滤，可选）
     * @return 菜单树
     */
    public Result<MenuTreeVO> getMenuTree(String orgId, String userId) {
        // 1. 获取所有菜单分组
        List<SchemaDefinition<?>> menuSchemas = schemaQuery.query(orgId, SchemaType.MENU);
        List<MenuGroupDefinition> groups = menuSchemas.stream()
                .filter(s -> s instanceof MenuGroupDefinition)
                .map(s -> (MenuGroupDefinition) s)
                .filter(g -> isVisibleToUser(g, userId))
                .filter(MenuGroupDefinition::isEnabled)
                .toList();

        // 2. 获取所有视图
        List<SchemaDefinition<?>> viewSchemas = schemaQuery.query(orgId, SchemaType.VIEW);
        Map<String, AbstractViewDefinition> viewMap = viewSchemas.stream()
                .filter(s -> s instanceof AbstractViewDefinition)
                .map(s -> (AbstractViewDefinition) s)
                .filter(AbstractViewDefinition::isEnabled)
                .collect(Collectors.toMap(v -> v.getId().value(), v -> v));

        // 3. 获取卡片类型名称映射
        Map<String, String> cardTypeNames = getCardTypeNames(viewSchemas);

        // 4. 构建树形结构
        List<MenuTreeNodeVO> rootNodes = buildTree(groups, viewMap, cardTypeNames);

        // 5. 获取未分组视图
        Set<String> groupedViewIds = groups.stream()
                .flatMap(g -> g.getViewItems() != null ? g.getViewItems().stream() : java.util.stream.Stream.empty())
                .filter(item -> item.getViewId() != null)
                .map(item -> item.getViewId().value())
                .collect(Collectors.toSet());

        List<MenuTreeNodeVO> ungroupedViews = viewMap.values().stream()
                .filter(v -> !groupedViewIds.contains(v.getId().value()))
                .sorted(Comparator.comparing(v -> v.getSortOrder() != null ? v.getSortOrder() : 0))
                .map(v -> toViewNode(v, cardTypeNames))
                .toList();

        return Result.success(MenuTreeVO.builder()
                .roots(rootNodes)
                .ungroupedViews(ungroupedViews)
                .build());
    }

    /**
     * 将视图添加到分组
     */
    @Transactional
    public Result<Void> addViewToGroup(String groupId, String operatorId, String viewId, Integer sortOrder, String displayName) {
        Optional<SchemaDefinition<?>> groupOpt = schemaRepository.findById(groupId);
        if (groupOpt.isEmpty() || !(groupOpt.get() instanceof MenuGroupDefinition)) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "菜单分组不存在");
        }

        Optional<SchemaDefinition<?>> viewOpt = schemaRepository.findById(viewId);
        if (viewOpt.isEmpty() || !(viewOpt.get() instanceof AbstractViewDefinition)) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "视图不存在");
        }

        MenuGroupDefinition group = (MenuGroupDefinition) groupOpt.get();

        // 检查是否已存在
        if (group.getViewItems() != null &&
                group.getViewItems().stream().anyMatch(item ->
                        item.getViewId() != null && item.getViewId().value().equals(viewId))) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "视图已在该分组中");
        }

        // 添加视图项
        MenuGroupDefinition.ViewMenuItem item = new MenuGroupDefinition.ViewMenuItem();
        item.setViewId(ViewId.of(viewId));
        item.setSortOrder(sortOrder != null ? sortOrder : getNextSortOrder(group));
        item.setDisplayName(displayName);
        group.addViewItem(item);

        group.setUpdatedBy(operatorId);
        group.setUpdatedAt(java.time.LocalDateTime.now());
        schemaRepository.save(group);
        log.info("View {} added to group {} by {}", viewId, groupId, operatorId);
        return Result.success();
    }

    /**
     * 从分组移除视图
     */
    @Transactional
    public Result<Void> removeViewFromGroup(String groupId, String viewId, String operatorId) {
        Optional<SchemaDefinition<?>> groupOpt = schemaRepository.findById(groupId);
        if (groupOpt.isEmpty() || !(groupOpt.get() instanceof MenuGroupDefinition)) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "菜单分组不存在");
        }

        MenuGroupDefinition group = (MenuGroupDefinition) groupOpt.get();
        boolean removed = group.removeViewItem(viewId);

        if (!removed) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "视图不在该分组中");
        }

        group.setUpdatedBy(operatorId);
        group.setUpdatedAt(java.time.LocalDateTime.now());
        schemaRepository.save(group);
        log.info("View {} removed from group {} by {}", viewId, groupId, operatorId);
        return Result.success();
    }

    /**
     * 重新排序分组内的视图
     */
    @Transactional
    public Result<Void> reorderViews(String groupId, String operatorId, List<String> viewIds) {
        Optional<SchemaDefinition<?>> groupOpt = schemaRepository.findById(groupId);
        if (groupOpt.isEmpty() || !(groupOpt.get() instanceof MenuGroupDefinition)) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "菜单分组不存在");
        }

        MenuGroupDefinition group = (MenuGroupDefinition) groupOpt.get();

        if (group.getViewItems() == null || group.getViewItems().isEmpty()) {
            return Result.success();
        }

        // 重新排序
        Map<String, MenuGroupDefinition.ViewMenuItem> itemMap = group.getViewItems().stream()
                .filter(item -> item.getViewId() != null)
                .collect(Collectors.toMap(item -> item.getViewId().value(), item -> item));

        List<MenuGroupDefinition.ViewMenuItem> reordered = new ArrayList<>();
        for (int i = 0; i < viewIds.size(); i++) {
            MenuGroupDefinition.ViewMenuItem item = itemMap.get(viewIds.get(i));
            if (item != null) {
                item.setSortOrder(i);
                reordered.add(item);
            }
        }
        group.setViewItems(reordered);

        group.setUpdatedBy(operatorId);
        group.setUpdatedAt(java.time.LocalDateTime.now());
        schemaRepository.save(group);
        log.info("Views reordered in group {} by {}", groupId, operatorId);
        return Result.success();
    }

    /**
     * 获取未分组的视图列表
     *
     * @param orgId 组织ID
     * @return 未分组视图列表
     */
    public Result<List<ViewListItemVO>> getUngroupedViews(String orgId) {
        // 获取所有分组
        List<SchemaDefinition<?>> menuSchemas = schemaQuery.query(orgId, SchemaType.MENU);
        Set<String> groupedViewIds = menuSchemas.stream()
                .filter(s -> s instanceof MenuGroupDefinition)
                .map(s -> (MenuGroupDefinition) s)
                .flatMap(g -> g.getViewItems() != null ? g.getViewItems().stream() : java.util.stream.Stream.empty())
                .filter(item -> item.getViewId() != null)
                .map(item -> item.getViewId().value())
                .collect(Collectors.toSet());

        // 获取所有视图
        List<SchemaDefinition<?>> viewSchemas = schemaQuery.query(orgId, SchemaType.VIEW);
        Map<String, String> cardTypeNames = getCardTypeNames(viewSchemas);

        List<ViewListItemVO> ungroupedViews = viewSchemas.stream()
                .filter(s -> s instanceof ListViewDefinition)
                .map(s -> (ListViewDefinition) s)
                .filter(v -> !groupedViewIds.contains(v.getId().value()))
                .map(v -> toViewListItemVO(v, cardTypeNames))
                .toList();

        return Result.success(ungroupedViews);
    }

    // ==================== 私有方法 ====================

    private boolean isVisibleToUser(MenuGroupDefinition group, String userId) {
        if (userId == null || group.getVisibility() == null) {
            return true;
        }

        MenuGroupDefinition.VisibilityConfig visibility = group.getVisibility();
        return switch (visibility.getType()) {
            case ALL -> true;
            case SPECIFIED_USERS -> visibility.getAllowedUserIds() != null &&
                    visibility.getAllowedUserIds().contains(userId);
            case SPECIFIED_ROLES -> true; // TODO: 需要查询用户角色
        };
    }

    private List<MenuTreeNodeVO> buildTree(List<MenuGroupDefinition> groups,
                                           Map<String, AbstractViewDefinition> viewMap,
                                           Map<String, String> cardTypeNames) {
        // 按父ID分组
        Map<String, List<MenuGroupDefinition>> childrenMap = groups.stream()
                .filter(g -> g.getParentId() != null)
                .collect(Collectors.groupingBy(g -> g.getParentId().value()));

        // 构建根节点
        return groups.stream()
                .filter(MenuGroupDefinition::isRoot)
                .sorted(Comparator.comparing(g -> g.getSortOrder() != null ? g.getSortOrder() : 0))
                .map(g -> buildTreeNode(g, childrenMap, viewMap, cardTypeNames))
                .toList();
    }

    private MenuTreeNodeVO buildTreeNode(MenuGroupDefinition group,
                                         Map<String, List<MenuGroupDefinition>> childrenMap,
                                         Map<String, AbstractViewDefinition> viewMap,
                                         Map<String, String> cardTypeNames) {
        MenuTreeNodeVO node = MenuTreeNodeVO.groupNode(
                group.getId().value(),
                group.getName(),
                group.getIcon(),
                group.getSortOrder()
        );
        node.setExpanded(group.isExpanded());
        node.setEnabled(group.isEnabled());

        List<MenuTreeNodeVO> children = new ArrayList<>();

        // 添加子分组
        List<MenuGroupDefinition> subGroups = childrenMap.getOrDefault(group.getId().value(), List.of());
        for (MenuGroupDefinition subGroup : subGroups) {
            children.add(buildTreeNode(subGroup, childrenMap, viewMap, cardTypeNames));
        }

        // 添加视图项
        if (group.getViewItems() != null) {
            List<MenuTreeNodeVO> viewNodes = group.getViewItems().stream()
                    .filter(item -> item.getViewId() != null)
                    .sorted(Comparator.comparing(item -> item.getSortOrder() != null ? item.getSortOrder() : 0))
                    .map(item -> {
                        AbstractViewDefinition view = viewMap.get(item.getViewId().value());
                        if (view == null) return null;
                        MenuTreeNodeVO viewNode = toViewNode(view, cardTypeNames);
                        viewNode.setSortOrder(item.getSortOrder());
                        if (item.getDisplayName() != null && !item.getDisplayName().isBlank()) {
                            viewNode.setName(item.getDisplayName());
                        }
                        return viewNode;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            children.addAll(viewNodes);
        }

        // 排序所有子节点
        children.sort(Comparator.comparing(c -> c.getSortOrder() != null ? c.getSortOrder() : 0));
        node.setChildren(children);

        return node;
    }

    private MenuTreeNodeVO toViewNode(AbstractViewDefinition view, Map<String, String> cardTypeNames) {
        MenuTreeNodeVO node = MenuTreeNodeVO.viewNode(
                view.getId().value(),
                view.getName(),
                view.getViewType(),
                view.getSortOrder()
        );
        node.setEnabled(view.isEnabled());
        node.setChildren(List.of());

        // 设置卡片类型信息
        if (view instanceof ListViewDefinition listView && listView.getCardTypeId() != null) {
            String cardTypeId = listView.getCardTypeId().value();
            node.setCardTypeId(cardTypeId);
            node.setCardTypeName(cardTypeNames.get(cardTypeId));
        }

        return node;
    }

    private Map<String, String> getCardTypeNames(List<SchemaDefinition<?>> viewSchemas) {
        // 收集所有cardTypeId
        List<String> cardTypeIds = viewSchemas.stream()
                .filter(s -> s instanceof ListViewDefinition)
                .map(s -> ((ListViewDefinition) s).getCardTypeId())
                .filter(Objects::nonNull)
                .map(Object::toString)
                .distinct()
                .toList();

        if (cardTypeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量查询卡片类型
        List<SchemaDefinition<?>> cardTypes = schemaRepository.findByIds(new HashSet<>(cardTypeIds));
        return cardTypes.stream()
                .collect(Collectors.toMap(
                        s -> s.getId().value(),
                        SchemaDefinition::getName,
                        (a, b) -> a
                ));
    }

    private ViewListItemVO toViewListItemVO(ListViewDefinition view, Map<String, String> cardTypeNames) {
        String cardTypeId = view.getCardTypeId() != null ? view.getCardTypeId().value() : null;

        return ViewListItemVO.builder()
                .id(view.getId().value())
                .orgId(view.getOrgId())
                .name(view.getName())
                .description(view.getDescription())
                .viewType(view.getViewType())
                .schemaSubType(view.getSchemaSubType())
                .cardTypeId(cardTypeId)
                .cardTypeName(cardTypeId != null ? cardTypeNames.get(cardTypeId) : null)
                .columnCount(view.getColumnConfigs() != null ? view.getColumnConfigs().size() : 0)
                .defaultView(view.isDefaultView())
                .shared(view.isShared())
                .enabled(view.isEnabled())
                .contentVersion(view.getContentVersion())
                .createdAt(view.getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .build();
    }

    private int getNextSortOrder(MenuGroupDefinition group) {
        if (group.getViewItems() == null || group.getViewItems().isEmpty()) {
            return 0;
        }
        return group.getViewItems().stream()
                .mapToInt(item -> item.getSortOrder() != null ? item.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;
    }
}
