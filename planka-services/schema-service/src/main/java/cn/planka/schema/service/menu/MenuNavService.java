package cn.planka.schema.service.menu;

import cn.planka.api.schema.vo.menu.MenuNavNodeVO;
import cn.planka.api.schema.vo.menu.MenuNavTreeVO;
import cn.planka.api.schema.vo.view.ViewListItemVO;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.menu.MenuGroupDefinition;
import cn.planka.schema.service.common.SchemaQuery;
import cn.planka.schema.service.view.ViewNavService;
import cn.planka.schema.service.view.ViewNavVisibilityChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * 侧栏菜单树：可见菜单分组 + 可见视图，未入树的视图归入 ungroupedViews。
 */
@Service
@RequiredArgsConstructor
public class MenuNavService {

    private final SchemaQuery schemaQuery;
    private final ViewNavService viewNavService;
    private final ViewNavVisibilityChecker viewNavVisibilityChecker;

    public MenuNavTreeVO buildNavTree(String orgId, String operatorMemberCardId, String cascadeRelationNodeId) {
        List<ViewListItemVO> nav = viewNavService.listNav(orgId, operatorMemberCardId, cascadeRelationNodeId);
        Map<String, ViewListItemVO> navById = nav.stream()
                .filter(v -> v.getId() != null)
                .collect(Collectors.toMap(ViewListItemVO::getId, v -> v, (a, b) -> a));

        List<MenuGroupDefinition> menuGroups = schemaQuery.query(orgId, SchemaType.MENU).stream()
                .filter(MenuGroupDefinition.class::isInstance)
                .map(MenuGroupDefinition.class::cast)
                .collect(Collectors.toList());

        Set<String> navVisibleGroupIds = menuGroups.stream()
                .filter(g -> viewNavVisibilityChecker.isVisibleForNav(g, orgId, operatorMemberCardId, cascadeRelationNodeId))
                .map(g -> g.getId().value())
                .collect(Collectors.toSet());

        Map<String, MenuGroupDefinition> visibleById = menuGroups.stream()
                .filter(g -> navVisibleGroupIds.contains(g.getId().value()))
                .filter(g -> g.getParentId() == null || navVisibleGroupIds.contains(g.getParentId().value()))
                .collect(Collectors.toMap(g -> g.getId().value(), g -> g, (a, b) -> a));

        Map<String, List<MenuGroupDefinition>> byParent = visibleById.values().stream()
                .filter(g -> g.getParentId() != null && visibleById.containsKey(g.getParentId().value()))
                .collect(Collectors.groupingBy(g -> g.getParentId().value()));

        Set<String> displayedViewIds = new HashSet<>();
        List<MenuNavNodeVO> roots = new ArrayList<>();
        for (MenuGroupDefinition g : sortGroups(
                visibleById.values().stream().filter(x -> x.getParentId() == null).collect(Collectors.toList()))) {
            MenuNavNodeVO node = buildGroupBranch(g, byParent, navById, displayedViewIds);
            if (node != null) {
                roots.add(node);
            }
        }

        List<MenuNavNodeVO> ungrouped = nav.stream()
                .filter(v -> !displayedViewIds.contains(v.getId()))
                .map(this::ungroupedViewNode)
                .collect(Collectors.toList());

        return MenuNavTreeVO.builder()
                .roots(roots)
                .ungroupedViews(ungrouped)
                .build();
    }

    private MenuNavNodeVO buildGroupBranch(
            MenuGroupDefinition g,
            Map<String, List<MenuGroupDefinition>> byParent,
            Map<String, ViewListItemVO> navById,
            Set<String> displayedViewIds) {
        List<MenuNavNodeVO> children = new ArrayList<>();
        List<MenuGroupDefinition> subgroups = sortGroups(byParent.getOrDefault(g.getId().value(), List.of()));
        for (MenuGroupDefinition sg : subgroups) {
            MenuNavNodeVO sub = buildGroupBranch(sg, byParent, navById, displayedViewIds);
            if (sub != null) {
                children.add(sub);
            }
        }
        List<MenuGroupDefinition.ViewMenuItem> items =
                g.getViewItems() == null ? List.of() : g.getViewItems();
        items.stream()
                .sorted(Comparator.comparingInt(a -> a.getSortOrder() != null ? a.getSortOrder() : Integer.MAX_VALUE))
                .forEach(item -> {
                    if (item.getViewId() == null) {
                        return;
                    }
                    String vid = item.getViewId().value();
                    ViewListItemVO vo = navById.get(vid);
                    if (vo != null) {
                        displayedViewIds.add(vid);
                        children.add(viewNode(vo, item));
                    }
                });
        if (children.isEmpty()) {
            return null;
        }
        return MenuNavNodeVO.builder()
                .id(g.getId().value())
                .type("GROUP")
                .name(g.getName())
                .icon(g.getIcon())
                .sortOrder(g.getSortOrder())
                .expanded(g.isExpanded())
                .enabled(g.isEnabled())
                .children(children)
                .build();
    }

    private static List<MenuGroupDefinition> sortGroups(List<MenuGroupDefinition> groups) {
        return groups.stream()
                .sorted(Comparator
                        .comparing((MenuGroupDefinition g) -> g.getSortOrder() != null ? g.getSortOrder() : Integer.MAX_VALUE)
                        .thenComparing(MenuGroupDefinition::getName, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    private MenuNavNodeVO viewNode(ViewListItemVO v, MenuGroupDefinition.ViewMenuItem item) {
        String name = item != null && item.getDisplayName() != null && !item.getDisplayName().isBlank()
                ? item.getDisplayName()
                : v.getName();
        return MenuNavNodeVO.builder()
                .id(Objects.requireNonNull(v.getId()))
                .type("VIEW")
                .name(name)
                .viewType(v.getViewType())
                .cardTypeId(v.getCardTypeId())
                .cardTypeName(v.getCardTypeName())
                .sortOrder(item != null ? item.getSortOrder() : null)
                .enabled(v.isEnabled())
                .children(new ArrayList<>())
                .build();
    }

    private MenuNavNodeVO ungroupedViewNode(ViewListItemVO v) {
        return MenuNavNodeVO.builder()
                .id(v.getId())
                .type("VIEW")
                .name(v.getName())
                .viewType(v.getViewType())
                .cardTypeId(v.getCardTypeId())
                .cardTypeName(v.getCardTypeName())
                .sortOrder(0)
                .enabled(v.isEnabled())
                .children(new ArrayList<>())
                .build();
    }
}
