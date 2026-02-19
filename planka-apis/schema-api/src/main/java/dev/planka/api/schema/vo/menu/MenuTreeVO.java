package dev.planka.api.schema.vo.menu;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 菜单树 VO
 * <p>
 * 包含完整的菜单树结构和未分组的视图列表。
 */
@Data
@Builder
public class MenuTreeVO {

    /**
     * 根节点列表（一级分组）
     */
    private List<MenuTreeNodeVO> roots;

    /**
     * 未分组的视图列表
     */
    private List<MenuTreeNodeVO> ungroupedViews;
}
