package cn.planka.api.schema.vo.menu;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 侧栏菜单树节点（分组或视图）
 */
@Data
@Builder
public class MenuNavNodeVO {

    private String id;
    /** GROUP 或 VIEW */
    private String type;
    private String name;
    private String icon;
    private String viewType;
    private String cardTypeId;
    private String cardTypeName;
    private Integer sortOrder;
    private Boolean expanded;
    private Boolean enabled;

    @Builder.Default
    private List<MenuNavNodeVO> children = new ArrayList<>();
}
