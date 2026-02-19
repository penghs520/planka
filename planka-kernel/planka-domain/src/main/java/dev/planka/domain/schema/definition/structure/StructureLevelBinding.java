package dev.planka.domain.schema.definition.structure;

import dev.planka.domain.link.LinkFieldId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 架构层级绑定配置
 * <p>
 * 定义卡片类型与架构线某一层级的关联关系。
 * 当卡片类型创建架构属性时，需要配置与架构线上每个层级的关联。
 *
 * @param levelIndex  层级索引，对应 StructureLevel 的 index
 * @param linkFieldId 与该层级卡片类型的关联属性ID，格式为 "{linkTypeId}:{SOURCE|TARGET}"
 * @param required    该层级是否必须填写，true 表示用户必须选择到此层级
 */
public record StructureLevelBinding(
        @JsonProperty("levelIndex") int levelIndex,
        @JsonProperty("linkFieldId") LinkFieldId linkFieldId,
        @JsonProperty("required") boolean required
) {

    @JsonCreator
    public StructureLevelBinding {
        if (levelIndex < 0) {
            throw new IllegalArgumentException("层级索引不能为负数");
        }
        Objects.requireNonNull(linkFieldId, "关联属性ID不能为空");
    }
}
