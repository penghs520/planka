package cn.planka.view.service;

import cn.planka.domain.schema.definition.view.AbstractViewDefinition;
import cn.planka.domain.schema.definition.view.ViewVisibilityMath;
import cn.planka.domain.schema.definition.view.ViewVisibilityScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViewDataVisibilityService {

    private final ViewTeamMembershipSupport viewTeamMembershipSupport;

    public boolean canQuery(AbstractViewDefinition view, String operatorMemberCardId, String structureNodeId) {
        String orgId = view.getOrgId();
        boolean inTeam = false;
        if (view.getEffectiveViewVisibilityScope() == ViewVisibilityScope.TEAMS) {
            inTeam = viewTeamMembershipSupport.memberInAnyTeam(orgId, operatorMemberCardId, view.getVisibleTeamCardIds());
        }
        return ViewVisibilityMath.isVisible(view, operatorMemberCardId, structureNodeId, inTeam);
    }
}
