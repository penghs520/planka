package cn.planka.domain.schema.definition.view;

import cn.planka.common.util.AssertUtils;

import java.util.Collection;
import java.util.Objects;

/**
 * 视图可见性纯逻辑（无远程调用），供 schema-service / view-service 复用。
 */
public final class ViewVisibilityMath {

    private ViewVisibilityMath() {
    }

    /**
     * 是否应对当前调用者在导航或数据查询中可见。
     *
     * @param view                 视图定义
     * @param viewerMemberCardId   当前成员卡 ID（请求头 X-Member-Card-Id）
     * @param currentStructureNodeId 当前架构节点 ID；工作区全局导航传 null
     * @param memberInAnyTeam      当 scope=TEAMS 时，是否属于 visibleTeamCardIds 中任一团队
     */
    public static boolean isVisible(
            NavVisibilitySubject subject,
            String viewerMemberCardId,
            String currentStructureNodeId,
            boolean memberInAnyTeam) {
        AssertUtils.notBlank(viewerMemberCardId, "viewerMemberCardId");
        if (!subject.isEnabled() || subject.isDeleted()) {
            return false;
        }
        ViewVisibilityScope scope = subject.getEffectiveViewVisibilityScope();
        return switch (scope) {
            case PRIVATE -> Objects.equals(viewerMemberCardId, subject.getCreatedBy());
            case WORKSPACE -> true;
            case TEAMS -> memberInAnyTeam;
            case STRUCTURE_NODE -> isStructureNodeVisible(subject, currentStructureNodeId);
        };
    }

    /**
     * 工作区全局导航（无节点上下文）不应列出 STRUCTURE_NODE 作用域条目。
     */
    public static boolean includeInWorkspaceNav(NavVisibilitySubject subject) {
        return subject.getEffectiveViewVisibilityScope() != ViewVisibilityScope.STRUCTURE_NODE;
    }

    private static boolean isStructureNodeVisible(NavVisibilitySubject subject, String currentStructureNodeId) {
        Collection<String> nodes = subject.getVisibleStructureNodeIds();
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        if (currentStructureNodeId == null || currentStructureNodeId.isBlank()) {
            return false;
        }
        return nodes.contains(currentStructureNodeId);
    }
}
