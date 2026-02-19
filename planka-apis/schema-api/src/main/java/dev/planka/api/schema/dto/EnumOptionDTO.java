package dev.planka.api.schema.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 枚举选项 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnumOptionDTO {

    /** 选项 ID */
    private String id;

    /** 显示文本 */
    private String label;

    /** 选项颜色 */
    private String color;

    /** 是否启用 */
    private boolean enabled;
}
