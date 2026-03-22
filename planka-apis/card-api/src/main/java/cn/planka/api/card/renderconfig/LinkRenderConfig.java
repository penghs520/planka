package cn.planka.api.card.renderconfig;

import lombok.*;

/**
 * 关联类型渲染配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LinkRenderConfig extends FieldRenderConfig {

    /** 是否多选 */
    private boolean multiple;

    /** 目标实体类型 ID */
    private String targetCardTypeId;

    /** 目标实体类型名称 */
    private String targetCardTypeName;
}
