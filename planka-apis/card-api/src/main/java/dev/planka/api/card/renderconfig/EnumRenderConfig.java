package dev.planka.api.card.renderconfig;

import lombok.*;

import java.util.List;

/**
 * 枚举类型渲染配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EnumRenderConfig extends FieldRenderConfig {

    /** 是否多选 */
    private boolean multiSelect;

    /** 枚举选项列表 */
    private List<EnumOptionDTO> options;
}
