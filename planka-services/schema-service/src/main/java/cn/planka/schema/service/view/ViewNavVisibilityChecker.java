package cn.planka.schema.service.view;

import cn.planka.domain.schema.definition.view.NavVisibilitySubject;
import cn.planka.domain.schema.definition.view.ViewVisibilityMath;
import cn.planka.domain.schema.definition.view.ViewVisibilityScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 工作区 / 架构节点导航条目的可见性（视图、菜单分组共用）
 */
@Component
@RequiredArgsConstructor
public class ViewNavVisibilityChecker {

    private final ViewTeamMembershipSupport viewTeamMembershipSupport;

    public boolean isVisibleForNav(
            NavVisibilitySubject subject,
            String orgId,
            String operatorMemberCardId,
            String structureNodeId) {
        if (subject == null || orgId == null || !Objects.equals(orgId, subject.getOrgId())) {
            return false;
        }
        if (structureNodeId == null || structureNodeId.isBlank()) {
            if (!ViewVisibilityMath.includeInWorkspaceNav(subject)) {
                return false;
            }
        }
        boolean inTeam = false;
        if (subject.getEffectiveViewVisibilityScope() == ViewVisibilityScope.TEAMS) {
            inTeam = viewTeamMembershipSupport.memberInAnyTeam(
                    orgId, operatorMemberCardId, subject.getVisibleTeamCardIds());
        }
        return ViewVisibilityMath.isVisible(subject, operatorMemberCardId, structureNodeId, inTeam);
    }
}
