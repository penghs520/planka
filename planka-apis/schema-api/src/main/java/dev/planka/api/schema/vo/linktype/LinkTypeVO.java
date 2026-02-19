package dev.planka.api.schema.vo.linktype;

import dev.planka.api.schema.vo.cardtype.CardTypeInfo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 关联类型 VO
 */
@Data
@Builder
public class LinkTypeVO {

    /** 关联类型 ID */
    private String id;

    /** 组织 ID */
    private String orgId;

    /** 名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 源端名称（如"父卡片"） */
    private String sourceName;

    /** 目标端名称（如"子卡片"） */
    private String targetName;

    /** 源端是否显示 */
    private boolean sourceVisible;

    /** 目标端是否显示 */
    private boolean targetVisible;

    /** 源端允许的卡片类型 */
    private List<CardTypeInfo> sourceCardTypes;

    /** 目标端允许的卡片类型 */
    private List<CardTypeInfo> targetCardTypes;

    /** 源端是否多选 */
    private boolean sourceMultiSelect;

    /** 目标端是否多选 */
    private boolean targetMultiSelect;

    /** 是否系统内置 */
    private boolean systemLinkType;

    /** 是否启用 */
    private boolean enabled;

    /** 内容版本号 */
    private int contentVersion;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
