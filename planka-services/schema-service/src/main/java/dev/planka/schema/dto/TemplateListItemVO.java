package dev.planka.schema.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 模板列表项 VO
 * <p>
 * 用于模板列表展示的简化版本
 */
@Getter
@Setter
public class TemplateListItemVO {

    /** 模板ID */
    private String id;

    /** 组织ID */
    private String orgId;

    /** 模板名称 */
    private String name;

    /** 模板描述 */
    private String description;

    /** 所属卡片类型ID */
    private String cardTypeId;

    /** 所属卡片类型名称 */
    private String cardTypeName;

    /** 是否系统内置模板 */
    private boolean systemTemplate;

    /** 优先级 */
    private Integer priority;

    /** Tab 数量 */
    private int tabCount;

    /** 是否启用 */
    private boolean enabled;

    /** 是否默认模板 */
    private boolean isDefault;

    /** 内容版本号（用于乐观锁） */
    private Integer contentVersion;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
