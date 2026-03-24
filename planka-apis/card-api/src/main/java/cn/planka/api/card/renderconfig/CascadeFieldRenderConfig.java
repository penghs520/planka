package cn.planka.api.card.renderconfig;

import lombok.*;

/**
 * 架构层级类型渲染配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CascadeFieldRenderConfig extends FieldRenderConfig {

    /** 架构线 ID */
    private String cascadeRelationId;

    /** 是否只能选择叶子节点 */
    private boolean leafOnly;
}
