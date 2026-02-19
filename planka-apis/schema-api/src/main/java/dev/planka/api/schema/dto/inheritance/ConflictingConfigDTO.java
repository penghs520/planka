package dev.planka.api.schema.dto.inheritance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 冲突配置来源 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictingConfigDTO {

    /** 来源卡片类型ID */
    private String cardTypeId;

    /** 来源卡片类型名称 */
    private String cardTypeName;

    /** 是否必填 */
    private Boolean required;

    /** 是否只读 */
    private Boolean readOnly;
}
