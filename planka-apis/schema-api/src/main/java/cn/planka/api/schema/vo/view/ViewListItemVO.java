package cn.planka.api.schema.vo.view;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视图列表项 VO
 * <p>
 * 用于视图列表展示，仅包含必要字段，不包含完整的配置信息。
 */
@Data
@Builder
public class ViewListItemVO {

    /**
     * 视图 ID
     */
    private String id;

    /**
     * 组织 ID
     */
    private String orgId;


    /**
     * 视图名称
     */
    private String name;

    /**
     * 视图描述
     */
    private String description;

    /**
     * 视图类型（LIST、planka 等）
     */
    private String viewType;

    /**
     * Schema 子类型标识
     */
    private String schemaSubType;

    /**
     * 关联的实体类型 ID
     */
    private String cardTypeId;

    /**
     * 关联的实体类型名称
     */
    private String cardTypeName;

    /**
     * 列数量
     */
    private int columnCount;

    /**
     * 是否为默认视图
     */
    private boolean defaultView;

    /**
     * 是否共享
     */
    private boolean shared;

    /**
     * 可见性范围基数（PRIVATE/WORKSPACE/TEAMS/STRUCTURE_NODE）
     */
    private String viewVisibilityScope;

    /**
     * 团队可见时的团队卡 ID
     */
    private List<String> visibleTeamCardIds;

    /**
     * 架构节点可见时的节点 ID
     */
    private List<String> visibleCascadeRelationNodeIds;

    /**
     * 创建人成员卡 ID
     */
    private String createdBy;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 内容版本号（乐观锁）
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
