package dev.planka.schema.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 新建页模板列表项 VO
 * <p>
 * 用于管理界面的模板列表展示。
 */
@Getter
@Setter
@NoArgsConstructor
public class CreatePageTemplateListItemVO {

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

    /** 是否系统内置模板 */
    private boolean systemTemplate;

    /** 区域数量 */
    private int sectionCount;

    /** 是否启用 */
    private boolean enabled;

    /** 是否默认模板 */
    private boolean isDefault;

    /** 内容版本号 */
    private int contentVersion;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
