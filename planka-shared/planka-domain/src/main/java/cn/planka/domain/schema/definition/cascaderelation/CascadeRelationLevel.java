package cn.planka.domain.schema.definition.cascaderelation;

import cn.planka.common.enums.SortDirection;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.field.FieldId;
import cn.planka.domain.link.LinkFieldId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 架构层级定义
 * <p>
 * 定义架构线中的一个层级，包含层级索引、名称、关联的__PLANKA_EINST__以及与上级的关联关系。
 *
 * @param index               层级索引（0为根层级）
 * @param name                层级名称（如"部落"、"小队"）
 * @param cardTypeId          关联的__PLANKA_EINST__ID（每层仅一个）
 * @param parentLinkFieldId   与上级的关联属性ID（根层级为null），格式为 "{linkTypeId}:{SOURCE|TARGET}"
 * @param ownerLinkFieldId    负责人关联属性ID（可选，用于标识当前架构节点的负责人）
 * @param sortFieldId         排序字段ID（可选，用于排序当前层级的节点）
 * @param sortDirection       排序方向（可选，默认升序）
 */
public record CascadeRelationLevel(
        @JsonProperty("index") int index,
        @JsonProperty("name") String name,
        @JsonProperty("cardTypeId") CardTypeId cardTypeId,
        @JsonProperty("parentLinkFieldId") LinkFieldId parentLinkFieldId,
        @JsonProperty("ownerLinkFieldId") LinkFieldId ownerLinkFieldId,
        @JsonProperty("sortFieldId") FieldId sortFieldId,
        @JsonProperty("sortDirection") SortDirection sortDirection
) {

    @JsonCreator
    public CascadeRelationLevel {
        if (index < 0) {
            throw new IllegalArgumentException("层级索引不能为负数");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("层级名称不能为空");
        }
        if (cardTypeId == null) {
            throw new IllegalArgumentException("__PLANKA_EINST__ID不能为空");
        }
        // 根层级（index=0）的 parentLinkFieldId 必须为 null
        if (index == 0 && parentLinkFieldId != null) {
            throw new IllegalArgumentException("根层级的 parentLinkFieldId 必须为 null");
        }
        // 非根层级的 parentLinkFieldId 不能为 null
        if (index > 0 && parentLinkFieldId == null) {
            throw new IllegalArgumentException("非根层级的 parentLinkFieldId 不能为 null");
        }
        // 如果指定了排序字段但没有指定方向，默认为升序
        if (sortFieldId != null && sortDirection == null) {
            sortDirection = SortDirection.ASC;
        }
    }
}
