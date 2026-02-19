package dev.planka.domain.schema.definition.cardtype;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaSubType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 属性集
 * <p>
 * 属性集的特点：
 * <ul>
 *     <li>只能被实体类型继承，不能直接创建卡片</li>
 *     <li>用于定义共同的属性定义和关联关系定义</li>
 * </ul>
 * <p>
 * 示例：可以定义一个"工作项"属性集，包含通用属性（优先级、计划开始时间、负责人等），
 * 然后让"需求"、"任务"、"缺陷"等实体类型继承它。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AbstractCardType extends CardTypeDefinition {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.TRAIT_CARD_TYPE;
    }

    @JsonCreator
    public AbstractCardType(
            @JsonProperty("id") CardTypeId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
    }
}
