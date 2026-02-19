package dev.planka.api.schema.vo.menu;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单分组列表项 VO
 * <p>
 * 用于分组列表展示。
 */
@Data
@Builder
public class MenuGroupVO {

    /**
     * 分组 ID
     */
    private String id;

    /**
     * 组织 ID
     */
    private String orgId;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 分组描述
     */
    private String description;

    /**
     * 父分组 ID
     */
    private String parentId;

    /**
     * 分组图标
     */
    private String icon;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 包含的视图数量
     */
    private int viewCount;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 内容版本号
     */
    private int contentVersion;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
