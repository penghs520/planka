package cn.planka.api.card.renderconfig;

import lombok.*;

/**
 * 级联层级类型渲染配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CascadeFieldRenderConfig extends FieldRenderConfig {

    /** 级联关系 ID */
    private String cascadeRelationId;

    /** 是否只能选择叶子节点 */
    private boolean leafOnly;
}
