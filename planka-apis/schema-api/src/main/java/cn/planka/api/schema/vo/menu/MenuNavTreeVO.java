package cn.planka.api.schema.vo.menu;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作区 / 架构节点侧栏菜单树（服务端已按可见性过滤）
 */
@Data
@Builder
public class MenuNavTreeVO {

    @Builder.Default
    private List<MenuNavNodeVO> roots = new ArrayList<>();

    @Builder.Default
    private List<MenuNavNodeVO> ungroupedViews = new ArrayList<>();
}
