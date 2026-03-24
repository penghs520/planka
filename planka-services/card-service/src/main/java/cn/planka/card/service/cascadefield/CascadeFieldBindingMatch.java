package cn.planka.card.service.cascadefield;

import cn.planka.domain.schema.definition.fieldconfig.CascadeFieldConfig;
import cn.planka.domain.schema.definition.cascaderelation.CascadeRelationLevelBinding;

/**
 * 级联绑定匹配结果
 * <p>
 * 当一个关联属性属于某个级联属性的层级绑定时，返回此匹配信息。
 *
 * @param cascadeFieldDef 级联属性定义
 * @param binding           匹配的层级绑定配置
 * @param levelIndex        层级索引
 */
public record CascadeFieldBindingMatch(
        CascadeFieldConfig cascadeFieldDef,
        CascadeRelationLevelBinding binding,
        int levelIndex
) {
}
