package dev.planka.domain.schema.definition.menu;

import dev.planka.domain.schema.*;
import dev.planka.domain.schema.*;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
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
 *     <li>分组级别权限控制</li>
 *     <li>分组内独立排序</li>
 * </ul>
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class MenuGroupDefinition extends AbstractSchemaDefinition<MenuGroupId> {

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

    /** 可见性配置 */
    @JsonProperty("visibility")
    private VisibilityConfig visibility;

    @JsonCreator
    public MenuGroupDefinition(
            @JsonProperty("id") MenuGroupId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
        this.viewItems = new ArrayList<>();
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
        return parentId; // 子分组从属于父分组
    }

    @Override
    public Set<SchemaId> secondKeys() {
        // 收集所有引用的视图ID作为二级索引
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

    /**
     * 添加视图项
     */
    public void addViewItem(ViewMenuItem item) {
        if (this.viewItems == null) {
            this.viewItems = new ArrayList<>();
        }
        this.viewItems.add(item);
    }

    /**
     * 移除视图项
     */
    public boolean removeViewItem(String viewId) {
        if (this.viewItems == null) return false;
        return this.viewItems.removeIf(item ->
                item.getViewId() != null && item.getViewId().value().equals(viewId));
    }

    /**
     * 是否为根分组
     */
    public boolean isRoot() {
        return parentId == null;
    }

    // ==================== 内部类定义 ====================

    /**
     * 视图菜单项
     * 表示分组内的一个视图引用及其排序信息
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ViewMenuItem {
        /** 视图ID */
        @JsonProperty("viewId")
        private ViewId viewId;

        /** 在当前分组内的排序号 */
        @JsonProperty("sortOrder")
        private Integer sortOrder;

        /** 自定义显示名称（可选，不填则使用视图原名） */
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

    /**
     * 可见性配置
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VisibilityConfig {
        /** 可见性类型 */
        @JsonProperty("type")
        private VisibilityType type = VisibilityType.ALL;

        /** 允许的用户ID列表（当 type = SPECIFIED_USERS 时有效） */
        @JsonProperty("allowedUserIds")
        private Set<String> allowedUserIds;

        /** 允许的角色ID列表（当 type = SPECIFIED_ROLES 时有效） */
        @JsonProperty("allowedRoleIds")
        private Set<String> allowedRoleIds;

        public VisibilityConfig() {
        }

        public static VisibilityConfig forAll() {
            VisibilityConfig config = new VisibilityConfig();
            config.setType(VisibilityType.ALL);
            return config;
        }

        public static VisibilityConfig forUsers(Set<String> userIds) {
            VisibilityConfig config = new VisibilityConfig();
            config.setType(VisibilityType.SPECIFIED_USERS);
            config.setAllowedUserIds(userIds);
            return config;
        }

        public static VisibilityConfig forRoles(Set<String> roleIds) {
            VisibilityConfig config = new VisibilityConfig();
            config.setType(VisibilityType.SPECIFIED_ROLES);
            config.setAllowedRoleIds(roleIds);
            return config;
        }
    }

    /**
     * 可见性类型枚举
     */
    public enum VisibilityType {
        /** 所有用户可见 */
        ALL,
        /** 指定用户可见 */
        SPECIFIED_USERS,
        /** 指定角色可见 */
        SPECIFIED_ROLES
    }
}
