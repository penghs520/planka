package cn.planka.schema.service.menu;

import cn.planka.api.schema.vo.view.ViewListItemVO;
import cn.planka.domain.schema.MenuGroupId;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.ViewId;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.menu.MenuGroupDefinition;
import cn.planka.schema.service.common.SchemaQuery;
import cn.planka.schema.service.view.ViewNavService;
import cn.planka.schema.service.view.ViewNavVisibilityChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuNavServiceTest {

    private static final String ORG = "org-1";
    private static final String MEMBER = "member-1";

    @Mock
    private SchemaQuery schemaQuery;
    @Mock
    private ViewNavService viewNavService;
    @Mock
    private ViewNavVisibilityChecker viewNavVisibilityChecker;

    private MenuNavService menuNavService;

    @BeforeEach
    void setUp() {
        menuNavService = new MenuNavService(schemaQuery, viewNavService, viewNavVisibilityChecker);
    }

    @Test
    void noMenuGroups_putsAllNavViewsInUngrouped() {
        when(schemaQuery.query(ORG, SchemaType.MENU)).thenReturn(List.of());
        when(viewNavService.listNav(ORG, MEMBER, null)).thenReturn(List.of(
                viewVo("v1", "A"),
                viewVo("v2", "B")));

        var tree = menuNavService.buildNavTree(ORG, MEMBER, null);

        assertThat(tree.getRoots()).isEmpty();
        assertThat(tree.getUngroupedViews()).hasSize(2);
        assertThat(tree.getUngroupedViews().get(0).getId()).isEqualTo("v1");
    }

    @Test
    void groupWithVisibleView_buildsRootAndUngroupedForRest() {
        MenuGroupDefinition g = new MenuGroupDefinition(MenuGroupId.of("g1"), ORG, "Group");
        g.setViewItems(List.of(new MenuGroupDefinition.ViewMenuItem(ViewId.of("v1"), 0)));

        when(schemaQuery.query(ORG, SchemaType.MENU)).thenReturn(List.<SchemaDefinition<?>>of(g));
        when(viewNavVisibilityChecker.isVisibleForNav(eq(g), eq(ORG), eq(MEMBER), eq(null))).thenReturn(true);
        when(viewNavService.listNav(ORG, MEMBER, null)).thenReturn(List.of(
                viewVo("v1", "One"),
                viewVo("v2", "Two")));

        var tree = menuNavService.buildNavTree(ORG, MEMBER, null);

        assertThat(tree.getRoots()).hasSize(1);
        assertThat(tree.getRoots().get(0).getType()).isEqualTo("GROUP");
        assertThat(tree.getRoots().get(0).getChildren()).hasSize(1);
        assertThat(tree.getRoots().get(0).getChildren().get(0).getId()).isEqualTo("v1");
        assertThat(tree.getUngroupedViews()).hasSize(1);
        assertThat(tree.getUngroupedViews().get(0).getId()).isEqualTo("v2");
    }

    @Test
    void invisibleGroup_yieldsOnlyUngrouped() {
        MenuGroupDefinition g = new MenuGroupDefinition(MenuGroupId.of("g1"), ORG, "Group");
        g.setViewItems(List.of(new MenuGroupDefinition.ViewMenuItem(ViewId.of("v1"), 0)));

        when(schemaQuery.query(ORG, SchemaType.MENU)).thenReturn(List.<SchemaDefinition<?>>of(g));
        when(viewNavVisibilityChecker.isVisibleForNav(any(MenuGroupDefinition.class), eq(ORG), eq(MEMBER), eq(null)))
                .thenReturn(false);
        when(viewNavService.listNav(ORG, MEMBER, null)).thenReturn(List.of(viewVo("v1", "One")));

        var tree = menuNavService.buildNavTree(ORG, MEMBER, null);

        assertThat(tree.getRoots()).isEmpty();
        assertThat(tree.getUngroupedViews()).hasSize(1);
    }

    @Test
    void groupWithOnlyMissingNavViews_isPruned() {
        MenuGroupDefinition g = new MenuGroupDefinition(MenuGroupId.of("g1"), ORG, "Group");
        g.setViewItems(List.of(new MenuGroupDefinition.ViewMenuItem(ViewId.of("ghost"), 0)));

        when(schemaQuery.query(ORG, SchemaType.MENU)).thenReturn(List.<SchemaDefinition<?>>of(g));
        when(viewNavVisibilityChecker.isVisibleForNav(eq(g), eq(ORG), eq(MEMBER), eq(null))).thenReturn(true);
        when(viewNavService.listNav(ORG, MEMBER, null)).thenReturn(List.of(viewVo("v1", "One")));

        var tree = menuNavService.buildNavTree(ORG, MEMBER, null);

        assertThat(tree.getRoots()).isEmpty();
        assertThat(tree.getUngroupedViews()).hasSize(1);
    }

    private static ViewListItemVO viewVo(String id, String name) {
        return ViewListItemVO.builder()
                .id(id)
                .name(name)
                .viewType("LIST")
                .enabled(true)
                .build();
    }
}
