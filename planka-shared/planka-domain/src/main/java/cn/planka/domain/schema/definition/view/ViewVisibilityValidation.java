package cn.planka.domain.schema.definition.view;

import cn.planka.domain.schema.definition.condition.Condition;

import java.util.List;

/**
 * TEAMS / STRUCTURE_NODE / 受众条件等与 {@link ViewVisibilityScope} 相关的校验。
 */
public final class ViewVisibilityValidation {

    private ViewVisibilityValidation() {
    }

    public static void validateScopeAndAudience(
            ViewVisibilityScope scope,
            List<String> visibleTeamCardIds,
            List<String> visibleStructureNodeIds,
            Condition visibilityAudienceCondition) {
        if (scope == ViewVisibilityScope.TEAMS) {
            if (visibleTeamCardIds == null || visibleTeamCardIds.isEmpty()) {
                throw new IllegalArgumentException("TEAMS 可见性必须配置 visibleTeamCardIds");
            }
        }
        if (scope == ViewVisibilityScope.STRUCTURE_NODE) {
            if (visibleStructureNodeIds == null || visibleStructureNodeIds.isEmpty()) {
                throw new IllegalArgumentException("STRUCTURE_NODE 可见性必须配置 visibleStructureNodeIds");
            }
        }
        if (visibilityAudienceCondition != null && visibilityAudienceCondition.getRoot() != null) {
            throw new IllegalArgumentException("visibilityAudienceCondition 求值尚未实现，请暂勿配置");
        }
    }
}
