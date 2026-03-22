package cn.planka.api.schema.request.linktype;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 更新关联类型请求
 */
@Getter
@Setter
public class UpdateLinkTypeRequest {

    /** 描述 */
    private String description;

    /** 源端名称 */
    private String sourceName;

    /** 源端编码 */
    private String sourceCode;

    /** 目标端名称 */
    private String targetName;

    /** 目标端编码 */
    private String targetCode;

    /** 源端是否显示 */
    private Boolean sourceVisible;

    /** 目标端是否显示 */
    private Boolean targetVisible;

    /** 源端允许的实体类型ID列表 */
    private List<String> sourceCardTypeIds;

    /** 目标端允许的实体类型ID列表 */
    private List<String> targetCardTypeIds;

    /** 源端是否多选 */
    private Boolean sourceMultiSelect;

    /** 目标端是否多选 */
    private Boolean targetMultiSelect;

    /** 是否启用 */
    private Boolean enabled;

    /** 期望的版本号（乐观锁） */
    private Integer expectedVersion;
}
