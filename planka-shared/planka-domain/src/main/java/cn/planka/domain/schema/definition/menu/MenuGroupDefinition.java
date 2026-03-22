package cn.planka.domain.schema.definition.menu;

import cn.planka.domain.schema.*;
import cn.planka.domain.schema.definition.AbstractSchemaDefinition;
import cn.planka.domain.schema.definition.condition.Condition;
import cn.planka.domain.schema.definition.view.NavVisibilitySubject;
import cn.planka.domain.schema.definition.view.ViewVisibilityScope;
import cn.planka.domain.schema.definition.view.ViewVisibilityValidation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单分组定义
 * <p>
 * 用于组织视图的层级结构，支持：
 * <ul>
 *     <li>多级菜单分组（通过 parentId 实现）</li>
 *     <li>视图多重归属（同一视图可在多个分组中）</li>
 *     <li>与列表视图一致的可见性模型</li>
 *     <li>分组内独立排序</li>
 * </ul>
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class MenuGroupDefinition extends AbstractSchemaDefinition<MenuGroupId> implements NavVisibilitySubject {

    /** 父分组ID（根分组为null） */
    @JsonProperty("parentId")
    private MenuGroupId parentId;

    /** 分组图标（可选，如 "folder", "star", "home" 等） */
    @JsonProperty("icon")
    private String icon;

    /** 分组内的视图项列表（包含排序信息） */
    @JsonProperty("viewItems")
    private List<ViewMenuItem> viewItems;

    /** 是否展开（前端状态，可选存储） */
    @JsonProperty("expanded")
    private boolean expanded = true;

    /** 是否公开（其他用户可见），与视图定义语义一致 */
    @JsonProperty("shared")
    private boolean shared = true;

    /** 可见性范围（用户ID列表，空表示所有人可见） */
    @JsonProperty("visibleTo")
    private List<String> visibleTo;

    @JsonProperty("viewVisibilityScope")
    private ViewVisibilityScope viewVisibilityScope;

    @JsonProperty("visibleTeamCardIds")
    private List<String> visibleTeamCardIds;

    @JsonProperty("visibleStructureNodeIds")
    private List<String> visibleStructureNodeIds;

    @JsonProperty("visibilityAudienceCondition")
    private Condition visibilityAudienceCondition;

    @JsonCreator
    public MenuGroupDefinition(
            @JsonProperty("id") MenuGroupId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
        this.viewItems = new ArrayList<>();
    }

    public ViewVisibilityScope getEffectiveViewVisibilityScope() {
        if (viewVisibilityScope != null) {
            return viewVisibilityScope;
        }
        if (!shared) {
            return ViewVisibilityScope.PRIVATE;
        }
        return ViewVisibilityScope.WORKSPACE;
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.MENU;
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.MENU_GROUP;
    }

    @Override
    public SchemaId belongTo() {
        return parentId;
    }

    @Override
    public Set<SchemaId> secondKeys() {
        if (viewItems == null || viewItems.isEmpty()) {
            return Set.of();
        }
        return viewItems.stream()
                .filter(item -> item.getViewId() != null)
                .map(item -> (SchemaId) item.getViewId())
                .collect(Collectors.toSet());
    }

    @Override
    protected MenuGroupId newId() {
        return MenuGroupId.generate();
    }

    @Override
    public void validate() {
        super.validate();
        ViewVisibilityValidation.validateScopeAndAudience(
                getEffectiveViewVisibilityScope(),
                visibleTeamCardIds,
                visibleStructureNodeIds,
                visibilityAudienceCondition);
    }

    public void addViewItem(ViewMenuItem item) {
        if (this.viewItems == null) {
            this.viewItems = new ArrayList<>();
        }
        this.viewItems.add(item);
    }

    public boolean removeViewItem(String viewId) {
        if (this.viewItems == null) {
            return false;
        }
        return this.viewItems.removeIf(item ->
                item.getViewId() != null && item.getViewId().value().equals(viewId));
    }

    public boolean isRoot() {
        return parentId == null;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ViewMenuItem {
        @JsonProperty("viewId")
        private ViewId viewId;

        @JsonProperty("sortOrder")
        private Integer sortOrder;

        @JsonProperty("displayName")
        private String displayName;

        public ViewMenuItem() {
        }

        public ViewMenuItem(ViewId viewId, Integer sortOrder) {
            this.viewId = viewId;
            this.sortOrder = sortOrder;
        }

        public ViewMenuItem(ViewId viewId, Integer sortOrder, String displayName) {
            this.viewId = viewId;
            this.sortOrder = sortOrder;
            this.displayName = displayName;
        }
    }
}
