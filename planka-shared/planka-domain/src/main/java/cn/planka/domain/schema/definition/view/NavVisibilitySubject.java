package cn.planka.domain.schema.definition.view;

import java.util.List;

/**
 * 与导航/数据可见性求值相关的契约（视图、菜单分组等共用）。
 */
public interface NavVisibilitySubject {

    String getOrgId();

    boolean isEnabled();

    boolean isDeleted();

    ViewVisibilityScope getEffectiveViewVisibilityScope();

    String getCreatedBy();

    List<String> getVisibleStructureNodeIds();

    List<String> getVisibleTeamCardIds();
}
