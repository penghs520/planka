package cn.planka.domain.schema.definition.menu;

import cn.planka.domain.schema.MenuGroupId;
import cn.planka.domain.schema.definition.view.ViewVisibilityScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MenuGroupDefinitionValidateTest {

    @Test
    void rejectsTeamsScopeWithoutTeamIds() {
        MenuGroupDefinition g = new MenuGroupDefinition(MenuGroupId.of("mg-1"), "org-1", "G");
        g.setViewVisibilityScope(ViewVisibilityScope.TEAMS);
        assertThatThrownBy(g::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("visibleTeamCardIds");
    }

    @Test
    void rejectsStructureNodeScopeWithoutNodeIds() {
        MenuGroupDefinition g = new MenuGroupDefinition(MenuGroupId.of("mg-1"), "org-1", "G");
        g.setViewVisibilityScope(ViewVisibilityScope.CASCADE_RELATION_NODE);
        assertThatThrownBy(g::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("visibleCascadeRelationNodeIds");
    }
}
