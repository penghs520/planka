package dev.planka.api.schema.vo.linktype;

import lombok.Builder;
import lombok.Data;

/**
 * 关联类型选项 VO（用于下拉框）
 */
@Data
@Builder
public class LinkTypeOptionVO {

    /** 关联类型 ID */
    private String id;

    /** 名称 */
    private String name;

    /** 源端名称 */
    private String sourceName;

    /** 目标端名称 */
    private String targetName;

    /** 源端是否显示 */
    private boolean sourceVisible;

    /** 目标端是否显示 */
    private boolean targetVisible;

    /** 源端是否多选 */
    private boolean sourceMultiSelect;

    /** 目标端是否多选 */
    private boolean targetMultiSelect;
}
