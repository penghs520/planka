package dev.planka.api.schema.vo.menu;

import lombok.Data;

import java.util.List;

/**
 * 菜单树节点 VO
 * <p>
 * 可以是分组节点或视图节点。
 */
@Data
public class MenuTreeNodeVO {

    /**
     * 节点 ID（分组ID或视图ID）
     */
    private String id;

    /**
     * 节点类型：GROUP 或 VIEW
     */
    private String type;

    /**
     * 显示名称
     */
    private String name;

    /**
     * 图标（分组专用）
     */
    private String icon;

    /**
     * 视图类型（视图专用，如 LIST, planka）
     */
    private String viewType;

    /**
     * 关联的卡片类型ID（视图专用）
     */
    private String cardTypeId;

    /**
     * 关联的卡片类型名称（视图专用）
     */
    private String cardTypeName;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 是否展开（分组专用）
     */
    private Boolean expanded;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 子节点列表
     */
    private List<MenuTreeNodeVO> children;

    /**
     * 创建分组节点
     */
    public static MenuTreeNodeVO groupNode(String id, String name, String icon, Integer sortOrder) {
        MenuTreeNodeVO node = new MenuTreeNodeVO();
        node.setId(id);
        node.setType("GROUP");
        node.setName(name);
        node.setIcon(icon);
        node.setSortOrder(sortOrder);
        node.setExpanded(true);
        return node;
    }

    /**
     * 创建视图节点
     */
    public static MenuTreeNodeVO viewNode(String id, String name, String viewType, Integer sortOrder) {
        MenuTreeNodeVO node = new MenuTreeNodeVO();
        node.setId(id);
        node.setType("VIEW");
        node.setName(name);
        node.setViewType(viewType);
        node.setSortOrder(sortOrder);
        return node;
    }
}
