package dev.planka.card.service.structure;

import dev.planka.domain.schema.definition.fieldconfig.StructureFieldConfig;
import dev.planka.domain.schema.definition.structure.StructureLevelBinding;

/**
 * 架构绑定匹配结果
 * <p>
 * 当一个关联属性属于某个架构属性的层级绑定时，返回此匹配信息。
 *
 * @param structureFieldDef 架构属性定义
 * @param binding           匹配的层级绑定配置
 * @param levelIndex        层级索引
 */
public record StructureBindingMatch(
        StructureFieldConfig structureFieldDef,
        StructureLevelBinding binding,
        int levelIndex
) {
}
