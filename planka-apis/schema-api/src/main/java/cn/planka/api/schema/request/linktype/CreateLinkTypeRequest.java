package cn.planka.api.schema.request.linktype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 创建关联类型请求
 */
@Getter
@Setter
public class CreateLinkTypeRequest {

    /** 描述 */
    private String description;

    /** 源端名称（如"父卡片"） */
    @NotBlank(message = "源端名称不能为空")
    private String sourceName;

    /** 源端编码（如"parent"） */
    private String sourceCode;

    /** 目标端名称（如"子卡片"） */
    @NotBlank(message = "目标端名称不能为空")
    private String targetName;

    /** 目标端编码（如"children"） */
    private String targetCode;

    /** 源端是否显示 */
    private boolean sourceVisible = true;

    /** 目标端是否显示 */
    private boolean targetVisible = true;

    /** 源端允许的实体类型ID列表 */
    @NotEmpty(message = "源端实体类型不能为空")
    private List<String> sourceCardTypeIds;

    /** 目标端允许的实体类型ID列表 */
    @NotEmpty(message = "目标端实体类型不能为空")
    private List<String> targetCardTypeIds;

    /** 源端是否多选 */
    private boolean sourceMultiSelect = true;

    /** 目标端是否多选 */
    private boolean targetMultiSelect = true;
}
